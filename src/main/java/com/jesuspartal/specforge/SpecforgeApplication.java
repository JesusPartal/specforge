package com.jesuspartal.specforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class SpecforgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpecforgeApplication.class, args);
	}

}
