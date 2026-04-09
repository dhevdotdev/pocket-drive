package dev.dhev.pocketdrive.file;

import dev.dhev.pocketdrive.config.R2Properties;
import dev.dhev.pocketdrive.config.UploadProperties;
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
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final Pattern UNSAFE_FILENAME_CHARS = Pattern.compile("[/\\\\:*?\"<>|]");

    private final FileRepository fileRepository;
    private final R2StorageService r2;
    private final UploadProperties uploadProperties;
    private final R2Properties r2Properties;

    public FileService(FileRepository fileRepository, R2StorageService r2,
                       UploadProperties uploadProperties, R2Properties r2Properties) {
        this.fileRepository = fileRepository;
        this.r2 = r2;
        this.uploadProperties = uploadProperties;
        this.r2Properties = r2Properties;
    }

    @Transactional
    public UploadInitiateResponse initiateUpload(String ownerId, UploadInitiateRequest request) {
        validate(request);

        String objectKey = ownerId + "/" + UUID.randomUUID() + "/" + sanitize(request.filename());
        String uploadUrl = r2.presignUpload(objectKey, request.contentType(), request.sizeBytes());

        FileEntity file = new FileEntity();
        file.setOwnerId(ownerId);
        file.setObjectKey(objectKey);
        file.setOriginalName(request.filename());
        file.setContentType(request.contentType());
        file.setSizeBytes(request.sizeBytes());
        fileRepository.save(file);

        return new UploadInitiateResponse(
            file.getId(),
            uploadUrl,
            r2Properties.getPresignedUploadExpiry()
        );
    }

    @Transactional
    public FileResponse confirmUpload(UUID fileId, String ownerId) {
        FileEntity file = requireOwned(fileId, ownerId);

        if (file.getStatus() == FileStatus.UPLOADED) {
            return FileResponse.from(file);
        }
        if (file.getStatus() != FileStatus.PENDING) {
            throw new FileStateConflictException(
                "Cannot confirm file in state: " + file.getStatus());
        }
        if (!r2.objectExists(file.getObjectKey())) {
            throw new InvalidFileException("Object not found in storage — upload may have failed");
        }

        file.setStatus(FileStatus.UPLOADED);
        return FileResponse.from(file);
    }

    @Transactional(readOnly = true)
    public FileListResponse listFiles(String ownerId, Pageable pageable) {
        Page<FileEntity> page = fileRepository.findByOwnerIdAndStatus(
            ownerId, FileStatus.UPLOADED, pageable);
        return new FileListResponse(
            page.getContent().stream().map(FileResponse::from).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public DownloadResponse getDownloadUrl(UUID fileId, String ownerId) {
        FileEntity file = requireOwned(fileId, ownerId);

        if (file.getStatus() != FileStatus.UPLOADED) {
            throw new FileStateConflictException(
                "Cannot download file in state: " + file.getStatus());
        }

        String url = r2.presignDownload(file.getObjectKey(), file.getOriginalName());
        return new DownloadResponse(url, r2Properties.getPresignedDownloadExpiry());
    }

    @Transactional
    public void deleteFile(UUID fileId, String ownerId) {
        FileEntity file = requireOwned(fileId, ownerId);

        if (file.getStatus() != FileStatus.UPLOADED) {
            throw new FileStateConflictException(
                "Cannot delete file in state: " + file.getStatus());
        }

        try {
            r2.deleteObject(file.getObjectKey());
        } catch (Exception e) {
            log.warn("R2 delete failed for {}, marking deleted anyway: {}", file.getObjectKey(), e.getMessage());
        }

        file.setStatus(FileStatus.DELETED);
    }

    private FileEntity requireOwned(UUID fileId, String ownerId) {
        return fileRepository.findByIdAndOwnerId(fileId, ownerId)
            .orElseThrow(() -> new FileNotFoundException(fileId));
    }

    private void validate(UploadInitiateRequest request) {
        if (request.sizeBytes() > uploadProperties.getMaxSizeBytes()) {
            throw new InvalidFileException(
                "File size exceeds limit of " + uploadProperties.getMaxSizeBytes() + " bytes");
        }
        if (!uploadProperties.getAllowedTypes().contains(request.contentType())) {
            throw new InvalidFileException("Content type not allowed: " + request.contentType());
        }
    }

    private String sanitize(String filename) {
        return UNSAFE_FILENAME_CHARS.matcher(filename).replaceAll("_");
    }
}
