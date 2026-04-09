package dev.dhev.pocketdrive;

import dev.dhev.pocketdrive.file.FileService;
import dev.dhev.pocketdrive.file.FileStatus;
import dev.dhev.pocketdrive.file.dto.DownloadResponse;
import dev.dhev.pocketdrive.file.dto.FileListResponse;
import dev.dhev.pocketdrive.file.dto.FileResponse;
import dev.dhev.pocketdrive.file.dto.UploadInitiateRequest;
import dev.dhev.pocketdrive.file.dto.UploadInitiateResponse;
import dev.dhev.pocketdrive.file.exception.FileNotFoundException;
import dev.dhev.pocketdrive.file.exception.FileStateConflictException;
import dev.dhev.pocketdrive.file.exception.InvalidFileException;
import dev.dhev.pocketdrive.storage.R2StorageService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "r2.endpoint=https://test.r2.cloudflarestorage.com",
    "r2.bucket=test-bucket",
    "r2.access-key=test-key",
    "r2.secret-key=test-secret",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri="
})
class FileServiceTest {

    @Autowired
    FileService fileService;

    @MockitoBean
    R2StorageService r2;

    private static final String OWNER = "user-1";
    private static final String OTHER_OWNER = "user-2";
    private static final String UPLOAD_URL = "https://r2.example.com/upload";
    private static final String DOWNLOAD_URL = "https://r2.example.com/download";

    @BeforeEach
    void setUp() {
        when(r2.presignUpload(anyString(), anyString(), anyLong())).thenReturn(UPLOAD_URL);
        when(r2.objectExists(anyString())).thenReturn(true);
        when(r2.presignDownload(anyString(), anyString())).thenReturn(DOWNLOAD_URL);
    }

    private UUID givenUploadedFile(String owner) {
        UUID fileId = fileService.initiateUpload(owner,
            new UploadInitiateRequest("doc.pdf", "application/pdf", 1024L)).fileId();
        fileService.confirmUpload(fileId, owner);
        return fileId;
    }

    @Test
    void initiateUpload_createsPendingRow_andReturnsPresignedUrl() {
        UploadInitiateResponse response = fileService.initiateUpload(OWNER,
            new UploadInitiateRequest("report.pdf", "application/pdf", 1024L));

        assertThat(response.fileId()).isNotNull();
        assertThat(response.uploadUrl()).isEqualTo(UPLOAD_URL);
        assertThat(response.expiresIn()).isPositive();
    }

    @Test
    void initiateUpload_rejects_fileTooLarge() {
        assertThatThrownBy(() -> fileService.initiateUpload(OWNER,
            new UploadInitiateRequest("big.pdf", "application/pdf", 999_999_999L)))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("size");
    }

    @Test
    void initiateUpload_rejects_unsupportedContentType() {
        assertThatThrownBy(() -> fileService.initiateUpload(OWNER,
            new UploadInitiateRequest("virus.exe", "application/octet-stream", 512L)))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("Content type not allowed");
    }

    @Test
    void confirmUpload_flipsToUploaded_whenObjectExistsInR2() {
        UUID fileId = fileService.initiateUpload(OWNER,
            new UploadInitiateRequest("doc.pdf", "application/pdf", 1024L)).fileId();

        FileResponse response = fileService.confirmUpload(fileId, OWNER);

        assertThat(response.status()).isEqualTo(FileStatus.UPLOADED);
        assertThat(response.fileId()).isEqualTo(fileId);
    }

    @Test
    void confirmUpload_isIdempotent_whenAlreadyUploaded() {
        UUID fileId = givenUploadedFile(OWNER);

        FileResponse response = fileService.confirmUpload(fileId, OWNER);

        assertThat(response.status()).isEqualTo(FileStatus.UPLOADED);
        // objectExists must not be called again on the second confirm
        verify(r2, times(1)).objectExists(anyString());
    }

    @Test
    void confirmUpload_throws_whenObjectMissingInR2() {
        when(r2.objectExists(anyString())).thenReturn(false);
        UUID fileId = fileService.initiateUpload(OWNER,
            new UploadInitiateRequest("doc.pdf", "application/pdf", 1024L)).fileId();

        assertThatThrownBy(() -> fileService.confirmUpload(fileId, OWNER))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("Object not found in storage");
    }

    @Test
    void confirmUpload_throws_whenFileNotFound() {
        assertThatThrownBy(() -> fileService.confirmUpload(UUID.randomUUID(), OWNER))
            .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void confirmUpload_throws_forWrongOwner() {
        UUID fileId = fileService.initiateUpload(OWNER,
            new UploadInitiateRequest("doc.pdf", "application/pdf", 1024L)).fileId();

        assertThatThrownBy(() -> fileService.confirmUpload(fileId, OTHER_OWNER))
            .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void confirmUpload_throws_forDeletedFile() {
        UUID fileId = givenUploadedFile(OWNER);
        fileService.deleteFile(fileId, OWNER);

        assertThatThrownBy(() -> fileService.confirmUpload(fileId, OWNER))
            .isInstanceOf(FileStateConflictException.class)
            .hasMessageContaining("DELETED");
    }

    @Test
    void listFiles_returnsOnlyUploadedFilesForOwner() {
        UUID fileId = givenUploadedFile(OWNER);
        fileService.initiateUpload(OWNER, new UploadInitiateRequest("b.pdf", "application/pdf", 512L));
        givenUploadedFile(OTHER_OWNER);

        FileListResponse result = fileService.listFiles(OWNER, PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().fileId()).isEqualTo(fileId);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getDownloadUrl_returnsPresignedUrl_forUploadedFile() {
        UUID fileId = givenUploadedFile(OWNER);

        DownloadResponse response = fileService.getDownloadUrl(fileId, OWNER);

        assertThat(response.downloadUrl()).isEqualTo(DOWNLOAD_URL);
        assertThat(response.expiresIn()).isPositive();
    }

    @Test
    void getDownloadUrl_throws_forPendingFile() {
        UUID fileId = fileService.initiateUpload(OWNER,
            new UploadInitiateRequest("img.png", "image/png", 2048L)).fileId();

        assertThatThrownBy(() -> fileService.getDownloadUrl(fileId, OWNER))
            .isInstanceOf(FileStateConflictException.class)
            .hasMessageContaining("PENDING");
    }

    @Test
    void getDownloadUrl_throws_forDeletedFile() {
        UUID fileId = givenUploadedFile(OWNER);
        fileService.deleteFile(fileId, OWNER);

        assertThatThrownBy(() -> fileService.getDownloadUrl(fileId, OWNER))
            .isInstanceOf(FileStateConflictException.class)
            .hasMessageContaining("DELETED");
    }

    @Test
    void getDownloadUrl_throws_forWrongOwner() {
        UUID fileId = givenUploadedFile(OWNER);

        assertThatThrownBy(() -> fileService.getDownloadUrl(fileId, OTHER_OWNER))
            .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void deleteFile_marksDeleted_andCallsR2() {
        UUID fileId = givenUploadedFile(OWNER);

        fileService.deleteFile(fileId, OWNER);

        verify(r2).deleteObject(anyString());
        assertThatThrownBy(() -> fileService.getDownloadUrl(fileId, OWNER))
            .isInstanceOf(FileStateConflictException.class);
    }

    @Test
    void deleteFile_marksDeleted_evenWhenR2Fails() {
        doThrow(new RuntimeException("R2 unavailable")).when(r2).deleteObject(anyString());
        UUID fileId = givenUploadedFile(OWNER);

        fileService.deleteFile(fileId, OWNER);

        assertThatThrownBy(() -> fileService.getDownloadUrl(fileId, OWNER))
            .isInstanceOf(FileStateConflictException.class);
    }

    @Test
    void deleteFile_throws_forPendingFile() {
        UUID fileId = fileService.initiateUpload(OWNER,
            new UploadInitiateRequest("doc.pdf", "application/pdf", 1024L)).fileId();

        assertThatThrownBy(() -> fileService.deleteFile(fileId, OWNER))
            .isInstanceOf(FileStateConflictException.class)
            .hasMessageContaining("PENDING");

        verify(r2, never()).deleteObject(any());
    }

    @Test
    void deleteFile_throws_forAlreadyDeletedFile() {
        UUID fileId = givenUploadedFile(OWNER);
        fileService.deleteFile(fileId, OWNER);

        assertThatThrownBy(() -> fileService.deleteFile(fileId, OWNER))
            .isInstanceOf(FileStateConflictException.class)
            .hasMessageContaining("DELETED");
    }

    @Test
    void deleteFile_throws_forWrongOwner() {
        UUID fileId = givenUploadedFile(OWNER);

        assertThatThrownBy(() -> fileService.deleteFile(fileId, OTHER_OWNER))
            .isInstanceOf(FileNotFoundException.class);

        verify(r2, never()).deleteObject(any());
    }
}
