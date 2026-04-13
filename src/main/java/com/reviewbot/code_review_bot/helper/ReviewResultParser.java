package com.reviewbot.code_review_bot.helper;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewbot.code_review_bot.dto.ReviewResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ReviewResultParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern JSON_PATTERN = Pattern.compile(
            "\\{[\\s\\S]*\"issues\"[\\s\\S]*\\}",
            Pattern.MULTILINE
    );

    public static ReviewResult parse(String rawResponse) {

        if (rawResponse == null || rawResponse.isBlank()) {
            log.warn("Raw response is null or blank");
            return emptyResult();
        }

        // Step 1 — Strip markdown
        String cleaned = stripMarkdown(rawResponse);

        // Step 2 — Try direct parse
        ReviewResult result = tryParse(cleaned);
        if (result != null) return result;

        // Step 3 — Extract JSON block using regex
        String extracted = extractJson(cleaned);
        if (extracted != null) {
            result = tryParse(extracted);
            if (result != null) return result;
        }

        log.error("Could not parse response. Raw: {}", rawResponse);
        return emptyResult();
    }

    private static String stripMarkdown(String response) {
        return response
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();
    }

    private static String extractJson(String response) {
        Matcher matcher = JSON_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group();
        }
        log.warn("No JSON block found in response");
        return null;
    }

    private static ReviewResult tryParse(String json) {
        try {
            ReviewResult result = objectMapper.readValue(json, ReviewResult.class);
            if (result != null && result.getIssues() != null) {
                return result;
            }
        } catch (Exception e) {
            log.debug("Parse attempt failed: {}", e.getMessage());
        }
        return null;
    }

    private static ReviewResult emptyResult() {
        ReviewResult result = new ReviewResult();
        result.setIssues(Collections.emptyList());
        return result;
    }
}
