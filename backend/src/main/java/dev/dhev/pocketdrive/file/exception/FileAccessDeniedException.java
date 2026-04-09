package dev.dhev.pocketdrive.file.exception;

import java.util.UUID;

public class FileAccessDeniedException extends RuntimeException {

    public FileAccessDeniedException(UUID fileId) {
        super("Access denied to file: " + fileId);
    }
}
