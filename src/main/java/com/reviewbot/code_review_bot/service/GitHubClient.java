package com.reviewbot.code_review_bot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GitHubClient {

    private final RestClient restClient;

    private static final String GITHUB_API_URL = "https://api.github.com";

    public GitHubClient(@Value("${reviewbot.github.token}") String githubToken) {
        this.restClient = RestClient.builder()
                .baseUrl(GITHUB_API_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    // ── 1. Fetch PR Diff ────────────────────────────────────────────────

    public String fetchPullRequestDiff(String owner, String repo, int prNumber) {
        log.info("Fetching diff for PR #{} in {}/{}", prNumber, owner, repo);

        try {
            String diff = restClient.get()
                    .uri("/repos/{owner}/{repo}/pulls/{prNumber}",
                            owner, repo, prNumber)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3.diff")
                    .retrieve()
                    .body(String.class);

            log.info("Successfully fetched diff for PR #{} ({} chars)",
                    prNumber, diff != null ? diff.length() : 0);
            return diff;

        } catch (Exception e) {
            log.error("Failed to fetch diff for PR #{}: {}", prNumber, e.getMessage());
            return null;
        }
    }

    // ── 2. Post Review Comment on PR ────────────────────────────────────

    public void postReviewComment(String owner, String repo,
                                  int prNumber, String commitId,
                                  String filePath, int line,
                                  String comment) {
        log.info("Posting comment on PR #{} line {}", prNumber, line);

        try {
            Map<String, Object> requestBody = Map.of(
                    "body", comment,
                    "commit_id", commitId,
                    "path", filePath,
                    "line", line,
                    "side", "RIGHT"   // RIGHT = new version of file
            );

            restClient.post()
                    .uri("/repos/{owner}/{repo}/pulls/{prNumber}/comments",
                            owner, repo, prNumber)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully posted comment on line {}", line);

        } catch (Exception e) {
            log.error("Failed to post comment on line {}: {}", line, e.getMessage());
        }
    }

    // ── 3. Post General PR Review Summary ───────────────────────────────

    public void postReviewSummary(String owner, String repo,
                                  int prNumber, int totalIssues,
                                  long criticalCount) {
        log.info("Posting review summary for PR #{}", prNumber);

        try {
            String summary = buildSummary(totalIssues, criticalCount);

            Map<String, Object> requestBody = Map.of(
                    "body", summary,
                    "event", "COMMENT"   // COMMENT, APPROVE, REQUEST_CHANGES
            );

            restClient.post()
                    .uri("/repos/{owner}/{repo}/pulls/{prNumber}/reviews",
                            owner, repo, prNumber)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully posted review summary for PR #{}", prNumber);

        } catch (Exception e) {
            log.error("Failed to post review summary: {}", e.getMessage());
        }
    }

    // ── 4. Get Latest Commit ID of PR ───────────────────────────────────

    public String getLatestCommitId(String owner, String repo, int prNumber) {
        log.info("Fetching latest commit id for PR #{}", prNumber);

        try {
            Map response = restClient.get()
                    .uri("/repos/{owner}/{repo}/pulls/{prNumber}",
                            owner, repo, prNumber)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("head")) {
                Map head = (Map) response.get("head");
                return (String) head.get("sha");
            }

        } catch (Exception e) {
            log.error("Failed to fetch commit id: {}", e.getMessage());
        }
        return null;
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private String buildSummary(int totalIssues, long criticalCount) {
        if (totalIssues == 0) {
            return """
                ## ✅ AI Code Review Complete
                No issues found. Looks good to merge!
                > *Reviewed by AI Code Review Bot*
                """;
        }

        return String.format("""
                ## 🔍 AI Code Review Complete
                Found **%d issue(s)** including **%d critical**.
                
                Please address the critical issues before merging.
                > *Reviewed by AI Code Review Bot*
                """, totalIssues, criticalCount);
    }
}