package com.project.pdfmaker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "Madhav's do docs",
        version = "3.0.0",
        description = "API for manipulating PDF documents",
        contact = @Contact(
            name = "API Support",
            email = "support@pdfmaker.com",
            url = "https://pdfmaker.com/contact"
        )
    )
)
public class PdfmakerApplication {
	@Value("${spring.data.redis.host}")
	String test;

	public static void main(String[] args) {


		SpringApplication.run(PdfmakerApplication.class, args);
	}

}
