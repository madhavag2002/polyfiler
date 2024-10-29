package com.project.pdfmaker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PdfmakerApplication {
	@Value("${spring.data.redis.host}")
	String test;

	public static void main(String[] args) {


		SpringApplication.run(PdfmakerApplication.class, args);
	}

}
