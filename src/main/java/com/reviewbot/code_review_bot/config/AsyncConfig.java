package com.reviewbot.code_review_bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "reviewTaskExecutor")
    public Executor reviewTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);      // 5 threads always running
        executor.setMaxPoolSize(10);      // max 10 threads under load
        executor.setQueueCapacity(50);    // queue 50 tasks if all threads busy
        executor.setThreadNamePrefix("review-");  // thread name in logs
        executor.setWaitForTasksToCompleteOnShutdown(true); // finish on shutdown
        executor.setAwaitTerminationSeconds(60);  // wait max 60s on shutdown
        executor.initialize();

        return executor;
    }
}