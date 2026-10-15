package com.placementos.tpms.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@Slf4j
@Getter
public class AsyncConfig implements AsyncConfigurer {

    @Value("${app.resume.thread-pool.core-size:2}")
    private int corePoolSize;

    @Value("${app.resume.thread-pool.max-size:4}")
    private int maxPoolSize;

    @Value("${app.resume.thread-pool.queue-capacity:10}")
    private int queueCapacity;

    @Value("${app.resume.thread-pool.thread-name-prefix:TPMS-Resume-Async-}")
    private String threadNamePrefix;

    @Bean(name = {"resumeTaskExecutor", "asyncTaskExecutor"})
    @Primary
    public ThreadPoolTaskExecutor resumeTaskExecutor() {
        log.info("[AsyncConfig] Initializing bounded ThreadPoolTaskExecutor: coreSize={}, maxSize={}, queueCapacity={}, prefix={}",
                corePoolSize, maxPoolSize, queueCapacity, threadNamePrefix);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        // Explicit rejection policy: immediately abort with RejectedExecutionException when queue and pool are full
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return resumeTaskExecutor();
    }
}
