package com.reviewbot.code_review_bot.service;

import com.reviewbot.code_review_bot.agent.RetryableAgentCaller;
import com.reviewbot.code_review_bot.dto.ReviewResult;
import com.reviewbot.code_review_bot.helper.ReviewResultParser;
import com.reviewbot.code_review_bot.Entity.ReviewIssue;
import com.reviewbot.code_review_bot.ReviewRepository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeReviewService {

    private final RetryableAgentCaller retryableAgentCaller;
    private final ReviewRepository reviewRepository;
    private final GitHubClient gitHubClient;

    private static final int MAX_DIFF_CHARS = 12000;
    private static final List<String> VALID_SEVERITIES = List.of("CRITICAL", "MAJOR", "MINOR");
    private static final List<String> VALID_TYPES = List.of("BUG", "PERFORMANCE", "SECURITY", "STYLE");

    @Async("reviewTaskExecutor")
    public void reviewPullRequest(String owner, String repo,
                                  Integer prNumber, String diff) {

        log.info("Starting review for PR #{} in repo {}", prNumber, repo);

        // Step 1 — Truncate diff
        String safeDiff = truncateDiff(diff);

        // Step 2 — Call agent with retry
        String rawResponse;
        try {
            rawResponse = retryableAgentCaller.callAgent(safeDiff);
        } catch (Exception e) {
            log.error("Agent failed after all retries: {}", e.getMessage());
            return;    // ← just return, no value
        }

        // Step 3 — Parse response
        ReviewResult result = ReviewResultParser.parse(rawResponse);

        if (result.getIssues().isEmpty()) {
            log.info("No issues found for PR #{} in {}", prNumber, repo);
            return;    // ← just return, no value
        }

        // Step 4 — Map to entities
        List<ReviewIssue> issues = result.getIssues().stream()
                .filter(this::isValid)
                .map(issue -> ReviewIssue.builder()
                        .repo(repo)
                        .prNumber(prNumber)
                        .lineNumber(issue.getLineNumber())
                        .severity(issue.getSeverity())
                        .type(issue.getType())
                        .suggestion(issue.getSuggestion())
                        .filePath(issue.getFilePath())
                        .build())
                .toList();

        // Step 5 — Save to MySQL
        List<ReviewIssue> saved = reviewRepository.saveAll(issues);
        log.info("Saved {} issues for PR #{} in {}",
                saved.size(), prNumber, repo);

        // Step 6 — Post comments to GitHub
        postCommentsToGitHub(owner, repo, prNumber, saved);
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

    private void postCommentsToGitHub(String owner, String repo,
                                      int prNumber, List<ReviewIssue> issues) {

        if (issues.isEmpty()) {
            log.info("No issues to post for PR #{}", prNumber);
            return;
        }

        // Step 1 — Get latest commit id
        String commitId = gitHubClient.getLatestCommitId(owner, repo, prNumber);
        if (commitId == null) {
            log.warn("Could not get commit id — skipping GitHub comments");
            // Still post summary even without line comments
            gitHubClient.postReviewSummary(owner, repo, prNumber,
                    issues.size(), countCritical(issues));
            return;
        }

        // Step 2 — Post individual line comments
        issues.forEach(issue -> {
            try {
                // Skip line comment if no filePath
                if (issue.getFilePath() == null
                        || issue.getFilePath().isBlank()) {
                    log.warn("No filePath for line {} — skipping line comment",
                            issue.getLineNumber());
                    return;   // continue to next issue
                }

                gitHubClient.postReviewComment(
                        owner,
                        repo,
                        prNumber,
                        commitId,
                        issue.getFilePath(),
                        issue.getLineNumber(),
                        formatComment(issue)
                );
            } catch (Exception e) {
                log.warn("Failed to post comment for line {}: {}",
                        issue.getLineNumber(), e.getMessage());
            }
        });

        // Step 3 — Post summary comment
        gitHubClient.postReviewSummary(
                owner,
                repo,
                prNumber,
                issues.size(),
                countCritical(issues)
        );

        log.info("Posted {} comments to PR #{}", issues.size(), prNumber);
    }

    private String formatComment(ReviewIssue issue) {
        return String.format("""
            **[%s/%s]** %s
            """,
                issue.getSeverity(),
                issue.getType(),
                issue.getSuggestion()
        );
    }

    private long countCritical(List<ReviewIssue> issues) {
        return issues.stream()
                .filter(i -> "CRITICAL".equals(i.getSeverity()))
                .count();
    }
}