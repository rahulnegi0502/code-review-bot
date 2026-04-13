package com.reviewbot.code_review_bot.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryableAgentCaller {

    private final AgentContinuationHandler continuationHandler;

    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 2000,
                    multiplier = 2.0,
                    maxDelay = 10000
            )
    )
    public String callAgent(String diff) {
        log.info("Calling AI agent...");

        String response = continuationHandler.getCompleteResponse(diff);

        if (response == null || response.isBlank()) {
            throw new RuntimeException("Agent returned blank response — triggering retry");
        }

        return response;
    }

    @Recover
    public String recover(Exception e, String diff) {
        log.error("All retry attempts exhausted. Reason: {}", e.getMessage());
        return null;
    }
}
