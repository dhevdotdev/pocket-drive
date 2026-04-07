package dev.dhev.pocketdrive.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "upload")
public class UploadProperties {

    private long maxSizeBytes = 104857600;
    private List<String> allowedTypes = List.of(
        "application/pdf", "image/png", "image/jpeg",
        "image/gif", "text/plain", "application/zip"
    );

    public long getMaxSizeBytes() { return maxSizeBytes; }
    public void setMaxSizeBytes(long maxSizeBytes) { this.maxSizeBytes = maxSizeBytes; }

    public List<String> getAllowedTypes() { return allowedTypes; }
    public void setAllowedTypes(List<String> allowedTypes) { this.allowedTypes = allowedTypes; }
}
