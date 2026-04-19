package com.reviewbot.code_review_bot.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_issues")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String repo;            // e.g. "myorg/myapp"

    @Column(nullable = false)
    private Integer prNumber;       // PR number from GitHub

    @Column(nullable = false)
    private Integer lineNumber;     // line in the diff

    @Column(nullable = false)
    private String severity;        // CRITICAL, MAJOR, MINOR

    @Column(nullable = false)
    private String type;            // BUG, PERFORMANCE, SECURITY, STYLE

    @Column(nullable = false, length = 1000)
    private String suggestion;      // AI generated fix

    @Column(nullable = false)
    private LocalDateTime reviewedAt;

    @Column(nullable = true)
    private String filePath;

    @PrePersist
    public void prePersist() {
        reviewedAt = LocalDateTime.now();
    }
}
