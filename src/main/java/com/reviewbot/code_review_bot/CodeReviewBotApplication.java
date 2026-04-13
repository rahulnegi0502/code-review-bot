package com.reviewbot.code_review_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class CodeReviewBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodeReviewBotApplication.class, args);
	}

}
