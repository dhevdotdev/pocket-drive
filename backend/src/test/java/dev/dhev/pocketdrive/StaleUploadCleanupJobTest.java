package dev.dhev.pocketdrive;

import dev.dhev.pocketdrive.file.FileEntity;
import dev.dhev.pocketdrive.file.FileRepository;
import dev.dhev.pocketdrive.file.FileStatus;
import dev.dhev.pocketdrive.file.StaleUploadCleanupJob;
import dev.dhev.pocketdrive.storage.R2StorageService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "r2.endpoint=https://test.r2.cloudflarestorage.com",
    "r2.bucket=test-bucket",
    "r2.access-key=test-key",
    "r2.secret-key=test-secret"
})
class StaleUploadCleanupJobTest {

    @Autowired
    StaleUploadCleanupJob job;

    @Autowired
    FileRepository fileRepository;

    @PersistenceContext
    EntityManager em;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    R2StorageService r2;

    @Test
    void run_marksStalePendingAsFailed_andDeletesFromR2() {
        FileEntity stale = pendingFileCreatedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        FileEntity fresh = pendingFileCreatedAt(Instant.now().minus(10, ChronoUnit.MINUTES));

        job.run();

        em.flush();
        em.clear();

        assertThat(fileRepository.findById(stale.getId()).orElseThrow().getStatus())
            .isEqualTo(FileStatus.FAILED);
        assertThat(fileRepository.findById(fresh.getId()).orElseThrow().getStatus())
            .isEqualTo(FileStatus.PENDING);
        verify(r2, times(1)).deleteObject(stale.getObjectKey());
    }

    @Test
    void run_marksFailedEvenWhenR2DeleteThrows() {
        doThrow(new RuntimeException("R2 unavailable")).when(r2).deleteObject(anyString());
        FileEntity stale = pendingFileCreatedAt(Instant.now().minus(2, ChronoUnit.HOURS));

        job.run();

        em.flush();
        em.clear();

        assertThat(fileRepository.findById(stale.getId()).orElseThrow().getStatus())
            .isEqualTo(FileStatus.FAILED);
    }

    @Test
    void run_doesNothing_whenNoPendingFiles() {
        job.run();
        verify(r2, times(0)).deleteObject(anyString());
    }

    private FileEntity pendingFileCreatedAt(Instant createdAt) {
        FileEntity file = new FileEntity();
        file.setOwnerId("user-test");
        file.setObjectKey("user-test/" + UUID.randomUUID() + "/file.pdf");
        file.setOriginalName("file.pdf");
        file.setContentType("application/pdf");
        file.setSizeBytes(1024);
        FileEntity saved = fileRepository.save(file);
        em.flush();

        em.createNativeQuery("UPDATE files SET created_at = :ts WHERE id = :id")
            .setParameter("ts", java.sql.Timestamp.from(createdAt))
            .setParameter("id", saved.getId())
            .executeUpdate();
        em.clear();

        return fileRepository.findById(saved.getId()).orElseThrow();
    }
}
