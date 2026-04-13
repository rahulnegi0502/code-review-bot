package com.reviewbot.code_review_bot.controller;

import com.reviewbot.code_review_bot.agent.CodeReviewAgent;
import com.reviewbot.code_review_bot.dto.ReviewResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private CodeReviewAgent codeReviewAgent;

    @PostMapping("/review")
    public ReviewResult testReview(@RequestBody String code) {
        return codeReviewAgent.review(code);
    }
}