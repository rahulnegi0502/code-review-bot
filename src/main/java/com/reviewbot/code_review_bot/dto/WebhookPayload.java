package com.reviewbot.code_review_bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)  // ignore fields we don't need
public class WebhookPayload {

    private String action;  // "opened", "synchronize", "closed"

    @JsonProperty("pull_request")
    private PullRequest pullRequest;

    private Repository repository;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequest {
        private Integer number;
        private String title;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Repository {
        private String name;

        @JsonProperty("full_name")
        private String fullName;    // "rahulnegi0502/code-review-bot"

        private Owner owner;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Owner {
        private String login;       // "rahulnegi0502"
    }
}