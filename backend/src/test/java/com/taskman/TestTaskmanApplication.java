package com.taskman;

import org.springframework.boot.SpringApplication;

public class TestTaskmanApplication {

	public static void main(String[] args) {
		SpringApplication.from(TaskmanApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
