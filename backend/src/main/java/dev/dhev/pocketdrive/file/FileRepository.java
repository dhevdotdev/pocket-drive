package dev.dhev.pocketdrive.file;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    Page<FileEntity> findByOwnerIdAndStatus(String ownerId, FileStatus status, Pageable pageable);

    Optional<FileEntity> findByIdAndOwnerId(UUID id, String ownerId);

    Slice<FileEntity> findByStatusAndCreatedAtBefore(FileStatus status, Instant cutoff, Pageable pageable);
}
