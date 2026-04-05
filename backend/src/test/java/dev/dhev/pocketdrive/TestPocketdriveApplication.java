package dev.dhev.pocketdrive;

import org.springframework.boot.SpringApplication;

public class TestPocketdriveApplication {

	public static void main(String[] args) {
		SpringApplication.from(PocketdriveApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
