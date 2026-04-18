package dev.dhev.pocketdrive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PocketdriveApplication {

	public static void main(String[] args) {
		SpringApplication.run(PocketdriveApplication.class, args);
	}

}
