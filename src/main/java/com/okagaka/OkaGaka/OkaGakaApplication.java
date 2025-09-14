package com.okagaka.OkaGaka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class OkaGakaApplication {

	public static void main(String[] args) {
		SpringApplication.run(OkaGakaApplication.class, args);
	}

}
