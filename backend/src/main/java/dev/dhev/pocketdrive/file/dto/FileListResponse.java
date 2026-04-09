package dev.dhev.pocketdrive.file.dto;

import java.util.List;

public record FileListResponse(
    List<FileResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
