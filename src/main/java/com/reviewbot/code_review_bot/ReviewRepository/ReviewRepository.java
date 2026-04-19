package com.reviewbot.code_review_bot.ReviewRepository;


import com.reviewbot.code_review_bot.Entity.ReviewIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    List<ReviewIssue> findByPrNumber(Integer prNumber);

    // Find by repo and PR number
    List<ReviewIssue> findByRepoAndPrNumber(String repo, Integer prNumber);

    // Count by severity
    Long countBySeverity(String severity);

    // Stats query
    @Query("SELECT r.severity, COUNT(r) FROM ReviewIssue r GROUP BY r.severity")
    List<Object[]> countGroupedBySeverity();

    @Query("SELECT r.type, COUNT(r) FROM ReviewIssue r GROUP BY r.type")
    List<Object[]> countGroupedByType();
}
