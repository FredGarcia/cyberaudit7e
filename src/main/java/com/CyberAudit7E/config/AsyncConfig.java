package com.cyberaudit7e.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration asynchrone et scheduling.
 *
 * M5 : thread pool dimensionné pour le batch (jusqu'à 10 audits parallèles)
 * et gestion globale des exceptions async.
 *
 * Implémente AsyncConfigurer pour :
 * - Définir l'executor par défaut pour @Async
 * - Intercepter les exceptions non gérées dans les threads async
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);           // 4 threads permanents
        executor.setMaxPoolSize(10);           // 10 threads max (batch de 10)
        executor.setQueueCapacity(50);         // File d'attente de 50 jobs
        executor.setThreadNamePrefix("7e-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // Politique de rejet : exécuter dans le thread appelant si la queue est pleine
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        log.info("[ASYNC] Thread pool initialisé — core: {}, max: {}, queue: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(),
                executor.getQueueCapacity());
        return executor;
    }

    /**
     * Gestion globale des exceptions dans les méthodes @Async.
     * Sans ce handler, les exceptions sont silencieusement avalées.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
                log.error("[ASYNC-ERROR] Exception dans {} : {}",
                        method.getName(), throwable.getMessage(), throwable);
    }
}
