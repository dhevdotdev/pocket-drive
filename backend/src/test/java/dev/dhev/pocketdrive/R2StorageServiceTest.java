package dev.dhev.pocketdrive;

import dev.dhev.pocketdrive.storage.R2StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(
    locations = "file:.env",
    properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri="
)
class R2StorageServiceTest {

    @Autowired
    R2StorageService r2;

    @Test
    void presignUpload_generatesSignedUrl() {
        String url = r2.presignUpload("test/hello.txt", "text/plain", 12);

        System.out.println("\n--- Presigned PUT URL ---");
        System.out.println(url);
        System.out.println("curl it: curl -X PUT -H \"Content-Type: text/plain\" -d \"hello world!\" \"" + url + "\"");
        System.out.println("---\n");

        assertThat(url).contains("X-Amz-Signature");
        assertThat(url).contains("test/hello.txt");
    }

    @Test
    void presignDownload_generatesSignedUrl() {
        String url = r2.presignDownload("test/hello.txt", "hello.txt");

        assertThat(url).contains("X-Amz-Signature");
        assertThat(url).contains("test/hello.txt");
    }
}
