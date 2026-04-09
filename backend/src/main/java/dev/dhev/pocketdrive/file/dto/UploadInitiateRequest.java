package dev.dhev.pocketdrive.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UploadInitiateRequest(
    @NotBlank(message = "Filename is required")
    @Size(max = 255, message = "Filename must not exceed 255 characters")
    String filename,

    @NotBlank(message = "Content type is required")
    String contentType,

    @Positive(message = "Size must be positive")
    long sizeBytes
) {}
