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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    UploadInitiateResponse initiateUpload(
        @Valid @RequestBody UploadInitiateRequest request,
        @AuthenticationPrincipal Jwt jwt) {
        return fileService.initiateUpload(jwt.getSubject(), request);
    }

    @PostMapping("/{fileId}/confirm")
    FileResponse confirmUpload(@PathVariable UUID fileId, @AuthenticationPrincipal Jwt jwt) {
        return fileService.confirmUpload(fileId, jwt.getSubject());
    }

    @GetMapping
    FileListResponse listFiles(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @AuthenticationPrincipal Jwt jwt) {
        return fileService.listFiles(jwt.getSubject(), pageable);
    }

    @GetMapping("/{fileId}/download")
    DownloadResponse getDownloadUrl(@PathVariable UUID fileId, @AuthenticationPrincipal Jwt jwt) {
        return fileService.getDownloadUrl(fileId, jwt.getSubject());
    }

    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteFile(@PathVariable UUID fileId, @AuthenticationPrincipal Jwt jwt) {
        fileService.deleteFile(fileId, jwt.getSubject());
    }
}
