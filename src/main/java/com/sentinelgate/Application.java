package com.sentinelgate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Application {

	@RequestMapping("/health")
	public ResponseEntity<String> healthCheck(){
		return ResponseEntity.ok("Welcome");
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
