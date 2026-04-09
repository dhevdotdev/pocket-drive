package dev.dhev.pocketdrive.file;

import dev.dhev.pocketdrive.file.dto.DownloadResponse;
import dev.dhev.pocketdrive.file.dto.FileListResponse;
import dev.dhev.pocketdrive.file.dto.FileResponse;
import dev.dhev.pocketdrive.file.dto.UploadInitiateRequest;
import dev.dhev.pocketdrive.file.dto.UploadInitiateResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    // TODO Phase 4: replace with jwt.getSubject() from @AuthenticationPrincipal Jwt jwt
    private static final String DEV_OWNER_ID = "dev-user";

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    UploadInitiateResponse initiateUpload(@Valid @RequestBody UploadInitiateRequest request) {
        return fileService.initiateUpload(DEV_OWNER_ID, request);
    }

    @PostMapping("/{fileId}/confirm")
    FileResponse confirmUpload(@PathVariable UUID fileId) {
        return fileService.confirmUpload(fileId, DEV_OWNER_ID);
    }

    @GetMapping
    FileListResponse listFiles(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return fileService.listFiles(DEV_OWNER_ID, pageable);
    }

    @GetMapping("/{fileId}/download")
    DownloadResponse getDownloadUrl(@PathVariable UUID fileId) {
        return fileService.getDownloadUrl(fileId, DEV_OWNER_ID);
    }

    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteFile(@PathVariable UUID fileId) {
        fileService.deleteFile(fileId, DEV_OWNER_ID);
    }
}
