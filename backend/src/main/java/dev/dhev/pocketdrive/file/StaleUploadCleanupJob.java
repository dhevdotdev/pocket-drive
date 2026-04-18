package dev.dhev.pocketdrive.file;

import dev.dhev.pocketdrive.storage.R2StorageService;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StaleUploadCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(StaleUploadCleanupJob.class);
    private static final Duration STALE_AFTER = Duration.ofHours(1);
    private static final int PAGE_SIZE = 100;

    private final FileRepository fileRepository;
    private final R2StorageService r2;

    public StaleUploadCleanupJob(FileRepository fileRepository, R2StorageService r2) {
        this.fileRepository = fileRepository;
        this.r2 = r2;
    }

    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void run() {
        Instant cutoff = Instant.now().minus(STALE_AFTER);
        int processed = 0;

        Slice<FileEntity> page;
        do {
            page = fileRepository.findByStatusAndCreatedAtBefore(
                FileStatus.PENDING, cutoff, PageRequest.of(0, PAGE_SIZE));

            for (FileEntity file : page) {
                try {
                    r2.deleteObject(file.getObjectKey());
                } catch (Exception e) {
                    log.warn("Failed to delete orphaned R2 object {}: {}", file.getObjectKey(), e.getMessage());
                }
                file.setStatus(FileStatus.FAILED);
                processed++;
            }
        } while (page.hasNext());

        if (processed > 0) {
            log.info("Stale upload cleanup: marked {} files as FAILED", processed);
        }
    }
}
