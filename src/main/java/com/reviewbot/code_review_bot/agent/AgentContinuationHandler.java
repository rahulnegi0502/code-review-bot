package com.reviewbot.code_review_bot.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentContinuationHandler {

    private final CodeReviewAgent codeReviewAgent;
    private static final int MAX_CONTINUATIONS = 2;

    public String getCompleteResponse(String diff) {

        // First call — returns String directly now
        String fullContent = codeReviewAgent.review(diff);
        log.info("First call completed");

        // Guard — if first call returned nothing
        if (fullContent == null || fullContent.isBlank()) {
            log.warn("First call returned blank response");
            return null;
        }

        log.debug("First call response: {}", fullContent);

        // Continue if response was cut
        int continuationCount = 0;
        while (isIncomplete(fullContent)
                && continuationCount < MAX_CONTINUATIONS) {

            continuationCount++;
            log.warn("Response incomplete. Requesting continuation {}/{}",
                    continuationCount, MAX_CONTINUATIONS);

            try {
                log.info("Waiting 10s before continuation to avoid rate limit...");
                Thread.sleep(10000);  // wait 10 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            String continuation = codeReviewAgent.continueResponse(
                    "Continue the JSON exactly from where you stopped. " +
                            "Do not repeat anything already written. " +
                            "Just continue the JSON:\n" + fullContent
            );

            // Guard — if continuation returned nothing
            if (continuation == null || continuation.isBlank()) {
                log.warn("Continuation {} returned blank — stopping",
                        continuationCount);
                break;
            }

            fullContent = mergeResponses(fullContent, continuation);
            log.info("Continuation {} completed", continuationCount);
            log.debug("Merged response: {}", fullContent);
        }

        if (isIncomplete(fullContent)) {
            log.error("Response still incomplete after {} continuations",
                    MAX_CONTINUATIONS);
        }

        return fullContent;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private boolean isIncomplete(String content) {
        if (content == null || content.isBlank()) return true;
        // Valid complete JSON must end with ]}
        return !content.trim().endsWith("]}");
    }

    private String mergeResponses(String first, String continuation) {
        if (continuation == null || continuation.isBlank()) return first;

        first = first.trim();
        continuation = continuation.trim();

        if (first.endsWith(",")) {
            return first + continuation;
        } else if (!first.endsWith("}") && !first.endsWith("]")) {
            int lastCompleteIssue = first.lastIndexOf("},");
            if (lastCompleteIssue != -1) {
                first = first.substring(0, lastCompleteIssue + 1);
            }
            return first + continuation;
        }

        return first + continuation;
    }
}