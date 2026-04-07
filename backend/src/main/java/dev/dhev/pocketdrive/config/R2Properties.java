package dev.dhev.pocketdrive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "r2")
public class R2Properties {

    private String endpoint;
    private String bucket;
    private String region = "auto";
    private String accessKey;
    private String secretKey;
    private int presignedUploadExpiry = 900;
    private int presignedDownloadExpiry = 300;

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public int getPresignedUploadExpiry() { return presignedUploadExpiry; }
    public void setPresignedUploadExpiry(int presignedUploadExpiry) { this.presignedUploadExpiry = presignedUploadExpiry; }

    public int getPresignedDownloadExpiry() { return presignedDownloadExpiry; }
    public void setPresignedDownloadExpiry(int presignedDownloadExpiry) { this.presignedDownloadExpiry = presignedDownloadExpiry; }
}
