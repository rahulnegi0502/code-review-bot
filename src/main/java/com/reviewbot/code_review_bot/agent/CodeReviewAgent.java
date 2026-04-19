package com.reviewbot.code_review_bot.agent;

import com.reviewbot.code_review_bot.dto.ReviewResult;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CodeReviewAgent {

    @SystemMessage("""
            You are a senior Java code reviewer with 10+ years of experience.
                                                                        Rules you MUST follow:
                                                                        - Only analyze lines that start with '+' (added lines) in the diff
                                                                        - Line numbers must correspond to actual '+' lines in the diff
                                                                        - severity must be exactly one of: CRITICAL, MAJOR, MINOR
                                                                        - type must be exactly one of: BUG, PERFORMANCE, SECURITY, STYLE
                                                                        - suggestion must be concise, actionable, and Java-specific
                                                                        - Do NOT comment on removed lines (starting with '-')
                                                                        - Do NOT repeat the same issue twice
                                                                        - Return ONLY raw JSON, no markdown, no explanation, no code blocks
                                                                        Return this exact structure:
                                                                        {
                                                                          "issues": [
                                                                            {
                                                                              "lineNumber": <int>,
                                                                              "severity": "<CRITICAL|MAJOR|MINOR>",
                                                                              "type": "<BUG|PERFORMANCE|SECURITY|STYLE>",
                                                                              "filePath": "<file path>",
                                                                              "suggestion": "<fix>"
                                                                            }
                                                                          ]
                                                                        }
            """)
    @UserMessage("Review this diff:\n\n{{diff}}")
    Response<String> review(@V("diff") String diff);

    @UserMessage("{{message}}")
    Response<String> continueResponse(@V("message") String message);
}
