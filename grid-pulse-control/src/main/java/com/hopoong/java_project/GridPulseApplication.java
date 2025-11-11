package com.hopoong.java_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GridPulseApplication {
	public static void main(String[] args) {
		SpringApplication.run(GridPulseApplication.class, args);
	}
}
