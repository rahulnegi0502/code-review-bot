package com.reviewbot.code_review_bot.controller;

import com.reviewbot.code_review_bot.Entity.ReviewIssue;
import com.reviewbot.code_review_bot.ReviewRepository.ReviewRepository;
import com.reviewbot.code_review_bot.dto.ReviewStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;

    // ── 1. Get All Reviews or Filter by Repo/Severity ──────────────────

    @GetMapping
    public ResponseEntity<List<ReviewIssue>> getReviews(
            @RequestParam(required = false) String repo,
            @RequestParam(required = false) String severity) {

        log.info("GET /reviews repo={} severity={}", repo, severity);

        List<ReviewIssue> issues;

        if (repo != null && severity != null) {
            // Both filters
            issues = reviewRepository.findByRepoAndSeverity(repo, severity);

        } else if (repo != null) {
            // Only repo filter
            issues = reviewRepository.findByRepo(repo);

        } else if (severity != null) {
            // Only severity filter
            issues = reviewRepository.findBySeverity(severity);

        } else {
            // No filter — return all
            issues = reviewRepository.findAll();
        }

        log.info("Returning {} issues", issues.size());
        return ResponseEntity.ok(issues);
    }

    // ── 2. Get Reviews by PR Number ─────────────────────────────────────

    @GetMapping("/pr/{prNumber}")
    public ResponseEntity<List<ReviewIssue>> getByPrNumber(
            @PathVariable Integer prNumber,
            @RequestParam(required = false) String repo) {

        log.info("GET /reviews/pr/{} repo={}", prNumber, repo);

        List<ReviewIssue> issues;

        if (repo != null) {
            issues = reviewRepository.findByRepoAndPrNumber(repo, prNumber);
        } else {
            issues = reviewRepository.findByPrNumber(prNumber);
        }

        return ResponseEntity.ok(issues);
    }

    // ── 3. Get Stats ────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<ReviewStatsDTO> getStats() {

        log.info("GET /reviews/stats");

        long total = reviewRepository.count();
        long critical = reviewRepository.countBySeverity("CRITICAL");
        long major = reviewRepository.countBySeverity("MAJOR");
        long minor = reviewRepository.countBySeverity("MINOR");

        // Build severity map
        Map<String, Long> bySeverity = new HashMap<>();
        reviewRepository.countGroupedBySeverity()
                .forEach(row -> bySeverity.put(
                        (String) row[0],
                        (Long) row[1]
                ));

        // Build type map
        Map<String, Long> byType = new HashMap<>();
        reviewRepository.countGroupedByType()
                .forEach(row -> byType.put(
                        (String) row[0],
                        (Long) row[1]
                ));

        ReviewStatsDTO stats = ReviewStatsDTO.builder()
                .totalIssues(total)
                .criticalCount(critical)
                .majorCount(major)
                .minorCount(minor)
                .bySeverity(bySeverity)
                .byType(byType)
                .build();

        return ResponseEntity.ok(stats);
    }

    // ── 4. Get Single Review by ID ──────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<ReviewIssue> getById(@PathVariable Long id) {

        log.info("GET /reviews/{}", id);

        return reviewRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── 5. Delete Review by ID (Admin) ──────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteById(@PathVariable Long id) {

        log.info("DELETE /reviews/{}", id);

        if (!reviewRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        reviewRepository.deleteById(id);
        return ResponseEntity.ok("Review " + id + " deleted");
    }
}