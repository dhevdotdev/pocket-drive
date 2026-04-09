package dev.dhev.pocketdrive.file.dto;

import java.util.UUID;

public record UploadInitiateResponse(
    UUID fileId,
    String uploadUrl,
    int expiresIn
) {}
