package com.reviewbot.code_review_bot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewbot.code_review_bot.dto.WebhookPayload;
import com.reviewbot.code_review_bot.service.CodeReviewService;
import com.reviewbot.code_review_bot.service.GitHubClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final CodeReviewService codeReviewService;
    private final GitHubClient gitHubClient;
    private final ObjectMapper objectMapper;

    @Value("${reviewbot.github.webhook-secret}")
    private String webhookSecret;

    private static final List<String> TRIGGER_ACTIONS =
            List.of("opened", "synchronize", "reopened");

    // ── Main Webhook Endpoint ────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestBody String rawPayload) {

        log.info("Received GitHub webhook event: {}", event);

        // Step 1 — Validate signature
        if (!isValidSignature(rawPayload, signature)) {
            log.warn("Invalid webhook signature — rejecting request");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid signature");
        }

        // Step 2 — Only handle PR events
        if (!"pull_request".equals(event)) {
            log.info("Ignoring non-PR event: {}", event);
            return ResponseEntity.ok("Event ignored");
        }

        // Step 3 — Parse payload
        WebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawPayload, WebhookPayload.class);
        } catch (Exception e) {
            log.error("Failed to parse webhook payload: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid payload");
        }

        // Step 4 — Only process relevant actions
        String action = payload.getAction();
        if (!TRIGGER_ACTIONS.contains(action)) {
            log.info("Ignoring PR action: {}", action);
            return ResponseEntity.ok("Action ignored: " + action);
        }

        // Step 5 — Extract PR details
        String owner = payload.getRepository().getOwner().getLogin();
        String repo = payload.getRepository().getName();
        Integer prNumber = payload.getPullRequest().getNumber();

        log.info("Processing PR #{} in {}/{} action={}",
                prNumber, owner, repo, action);

        // Step 6 — Fetch diff from GitHub
        String diff = gitHubClient.fetchPullRequestDiff(owner, repo, prNumber);
        if (diff == null || diff.isBlank()) {
            log.warn("Empty diff for PR #{} — skipping review", prNumber);
            return ResponseEntity.ok("Empty diff — skipped");
        }

        // Step 7 — Trigger AI review (async so webhook returns fast)
        try {
            codeReviewService.reviewPullRequest(owner, repo, prNumber, diff);
        } catch (Exception e) {
            log.error("Review failed for PR #{}: {}", prNumber, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Review failed");
        }

        return ResponseEntity.ok("Review triggered for PR #" + prNumber);
    }

    // ── HMAC Signature Validation ────────────────────────────────────────

    private boolean isValidSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(), "HmacSHA256"
            );
            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload.getBytes());
            String expectedSignature = "sha256=" + bytesToHex(hash);

            log.debug("Expected: {}", expectedSignature);
            log.debug("Received: {}", signature);

            return expectedSignature.equals(signature);

        } catch (Exception e) {
            log.error("Signature validation failed: {}", e.getMessage());
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}