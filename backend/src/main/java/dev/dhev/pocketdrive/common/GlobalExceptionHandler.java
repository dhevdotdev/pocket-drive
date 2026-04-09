package dev.dhev.pocketdrive.common;

import dev.dhev.pocketdrive.file.exception.FileAccessDeniedException;
import dev.dhev.pocketdrive.file.exception.FileNotFoundException;
import dev.dhev.pocketdrive.file.exception.FileStateConflictException;
import dev.dhev.pocketdrive.file.exception.InvalidFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse handleNotFound(FileNotFoundException ex) {
        return ErrorResponse.of("FILE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(FileAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ErrorResponse handleAccessDenied(FileAccessDeniedException ex) {
        return ErrorResponse.of("FILE_ACCESS_DENIED", ex.getMessage());
    }

    @ExceptionHandler(InvalidFileException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleInvalidFile(InvalidFileException ex) {
        return ErrorResponse.of("INVALID_FILE", ex.getMessage());
    }

    @ExceptionHandler(FileStateConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ErrorResponse handleStateConflict(FileStateConflictException ex) {
        return ErrorResponse.of("FILE_STATE_CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .findFirst()
            .orElse("Invalid request");
        return ErrorResponse.of("VALIDATION_ERROR", message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ErrorResponse handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred");
    }
}
