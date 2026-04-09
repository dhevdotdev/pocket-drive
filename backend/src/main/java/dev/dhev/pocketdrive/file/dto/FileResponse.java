package dev.dhev.pocketdrive.file.dto;

import dev.dhev.pocketdrive.file.FileEntity;
import dev.dhev.pocketdrive.file.FileStatus;
import java.time.Instant;
import java.util.UUID;

public record FileResponse(
    UUID fileId,
    String originalName,
    String contentType,
    long sizeBytes,
    FileStatus status,
    Instant createdAt
) {
    public static FileResponse from(FileEntity entity) {
        return new FileResponse(
            entity.getId(),
            entity.getOriginalName(),
            entity.getContentType(),
            entity.getSizeBytes(),
            entity.getStatus(),
            entity.getCreatedAt()
        );
    }
}
