package dev.dhev.pocketdrive.storage;

import dev.dhev.pocketdrive.config.R2Properties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class R2StorageService {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final R2Properties props;

    public R2StorageService(S3Client s3Client, S3Presigner presigner, R2Properties props) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.props = props;
    }

    public String presignUpload(String objectKey, String contentType, long sizeBytes) {
        var request = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(props.getPresignedUploadExpiry()))
            .putObjectRequest(PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .contentLength(sizeBytes)
                .build())
            .build();
        return presigner.presignPutObject(request).url().toString();
    }

    public String presignDownload(String objectKey, String originalName) {
        var request = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(props.getPresignedDownloadExpiry()))
            .getObjectRequest(GetObjectRequest.builder()
                .bucket(props.getBucket())
                .key(objectKey)
                .responseContentDisposition("attachment; filename*=UTF-8''" +
                    URLEncoder.encode(originalName, StandardCharsets.UTF_8).replace("+", "%20"))
                .build())
            .build();
        return presigner.presignGetObject(request).url().toString();
    }

    public boolean objectExists(String objectKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                .bucket(props.getBucket())
                .key(objectKey)
                .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    public void deleteObject(String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(props.getBucket())
            .key(objectKey)
            .build());
    }
}
