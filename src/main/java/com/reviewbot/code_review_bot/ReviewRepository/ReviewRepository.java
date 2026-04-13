package com.reviewbot.code_review_bot.ReviewRepository;


import com.reviewbot.code_review_bot.Entity.ReviewIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewIssue, Long> {

    // GET /reviews?repo=myorg/myapp
    List<ReviewIssue> findByRepo(String repo);

    // GET /reviews?severity=CRITICAL
    List<ReviewIssue> findBySeverity(String severity);

    // GET /reviews?repo=myorg/myapp&severity=CRITICAL
    List<ReviewIssue> findByRepoAndSeverity(String repo, String severity);

    // GET /reviews?repo=myorg/myapp&prNumber=42
    List<ReviewIssue> findByRepoAndPrNumber(String repo, Integer prNumber);
}
