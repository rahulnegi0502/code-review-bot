package com.reviewbot.code_review_bot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsDTO {

    private long totalIssues;
    private long criticalCount;
    private long majorCount;
    private long minorCount;
    private Map<String, Long> byType;      // BUG→5, SECURITY→3
    private Map<String, Long> bySeverity;  // CRITICAL→2, MAJOR→5
}