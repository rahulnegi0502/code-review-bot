package com.reviewbot.code_review_bot.agent;

import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
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

        // First call
        Response<String> response = codeReviewAgent.review(diff);
        String fullContent = response.content();
        FinishReason finishReason = response.finishReason();

        log.info("First call finish reason: {}", finishReason);

        // Continue if response was cut
        int continuationCount = 0;
        while (isIncomplete(finishReason, fullContent)
                && continuationCount < MAX_CONTINUATIONS) {

            continuationCount++;
            log.warn("Response cut. Requesting continuation {}/{}",
                    continuationCount, MAX_CONTINUATIONS);

            Response<String> continuation = codeReviewAgent.continueResponse(
                    "Continue the JSON exactly from where you stopped. " +
                            "Do not repeat anything already written. " +
                            "Just continue the JSON:\n" + fullContent
            );

            fullContent = mergeResponses(fullContent, continuation.content());
            finishReason = continuation.finishReason();

            log.info("Continuation {} finish reason: {}", continuationCount, finishReason);
        }

        if (isIncomplete(finishReason, fullContent)) {
            log.error("Response still incomplete after {} continuations", MAX_CONTINUATIONS);
        }

        return fullContent;
    }

    private boolean isIncomplete(FinishReason reason, String content) {
        if (FinishReason.LENGTH.equals(reason)) return true;
        if (content != null && !content.trim().endsWith("]}")) return true;
        return false;
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
