package com.reviewbot.code_review_bot.service;

import com.reviewbot.code_review_bot.agent.RetryableAgentCaller;
import com.reviewbot.code_review_bot.dto.ReviewResult;
import com.reviewbot.code_review_bot.helper.ReviewResultParser;
import com.reviewbot.code_review_bot.Entity.ReviewIssue;
import com.reviewbot.code_review_bot.ReviewRepository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeReviewService {

    private final RetryableAgentCaller retryableAgentCaller;
    private final ReviewRepository reviewRepository;

    private static final int MAX_DIFF_CHARS = 12000;
    private static final List<String> VALID_SEVERITIES = List.of("CRITICAL", "MAJOR", "MINOR");
    private static final List<String> VALID_TYPES = List.of("BUG", "PERFORMANCE", "SECURITY", "STYLE");

    public List<ReviewIssue> reviewPullRequest(String repo, Integer prNumber, String diff) {

        log.info("Starting review for PR #{} in repo {}", prNumber, repo);

        // Step 1 — Truncate diff if too large
        String safeDiff = truncateDiff(diff);

        // Step 2 — Call agent with retry + continuation
        String rawResponse;
        try {
            rawResponse = retryableAgentCaller.callAgent(safeDiff);
        } catch (Exception e) {
            log.error("Agent failed after all retries: {}", e.getMessage());
            return Collections.emptyList();
        }

        // Step 3 — Parse response safely
        ReviewResult result = ReviewResultParser.parse(rawResponse);

        if (result.getIssues().isEmpty()) {
            log.info("No issues found for PR #{} in {}", prNumber, repo);
            return Collections.emptyList();
        }

        // Step 4 — Map to entities + filter invalid
        List<ReviewIssue> issues = result.getIssues().stream()
                .filter(this::isValid)
                .map(issue -> ReviewIssue.builder()
                        .repo(repo)
                        .prNumber(prNumber)
                        .lineNumber(issue.getLineNumber())
                        .severity(issue.getSeverity())
                        .type(issue.getType())
                        .suggestion(issue.getSuggestion())
                        .build())
                .toList();

        // Step 5 — Save to MySQL
        List<ReviewIssue> saved = reviewRepository.saveAll(issues);
        log.info("Saved {} issues for PR #{} in {}", saved.size(), prNumber, repo);

        return saved;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private String truncateDiff(String diff) {
        if (diff.length() > MAX_DIFF_CHARS) {
            log.warn("Diff truncated from {} to {} chars", diff.length(), MAX_DIFF_CHARS);
            return diff.substring(0, MAX_DIFF_CHARS) + "\n... [diff truncated]";
        }
        return diff;
    }

    private boolean isValid(ReviewResult.Issue issue) {
        return issue.getSuggestion() != null
                && issue.getLineNumber() > 0
                && VALID_SEVERITIES.contains(issue.getSeverity())
                && VALID_TYPES.contains(issue.getType());
    }
}