package com.reviewbot.code_review_bot.config;


import com.reviewbot.code_review_bot.agent.CodeReviewAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    @Bean
    public CodeReviewAgent codeReviewAgent() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(4000)
                .temperature(0.1)
                .maxRetries(2)
                .build();

        return AiServices.builder(CodeReviewAgent.class)
                .chatLanguageModel(model)
                .build();
    }
}
