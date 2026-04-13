package com.reviewbot.code_review_bot.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReviewResult {

    private List<Issue> issues;

    @Data
    public static class Issue {
        private int lineNumber;
        private String severity;   // CRITICAL, MAJOR, MINOR
        private String type;       // BUG, PERFORMANCE, SECURITY, STYLE
        private String suggestion;
    }
}