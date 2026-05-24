package com.example.wmbservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WmbserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WmbserviceApplication.class, args);
	}

}
