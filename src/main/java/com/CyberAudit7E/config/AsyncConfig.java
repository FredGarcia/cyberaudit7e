package com.CyberAudit7E.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration asynchrone et scheduling.
 *
 * Active @Async (pour FeedbackLoopListener) et @Scheduled
 * (pour de futurs audits périodiques).
 *
 * Le thread pool est dimensionné pour un POC :
 * - 2 threads core (suffisant pour la boucle de rétroaction)
 * - 4 threads max (montée en charge modérée)
 * - Préfixe "7e-async-" pour identifier les threads dans les logs
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("7e-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
