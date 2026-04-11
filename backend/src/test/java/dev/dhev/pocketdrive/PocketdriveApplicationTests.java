package dev.dhev.pocketdrive;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = {
    "r2.endpoint=https://test.r2.cloudflarestorage.com",
    "r2.bucket=test-bucket",
    "r2.access-key=test-key",
    "r2.secret-key=test-secret"
})
class PocketdriveApplicationTests {

	@MockitoBean
	JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {
	}

}
