package com.visualbuilder.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置 — 替代 Spring 默认的 SimpleAsyncTaskExecutor（每次创建新线程，无上限）。
 *
 * <p>核心参数：
 * <ul>
 *   <li>corePoolSize=4: 常驻线程数</li>
 *   <li>maxPoolSize=16: 最大线程数（防止 OOM）</li>
 *   <li>queueCapacity=100: 等待队列容量</li>
 *   <li>rejectedExecutionHandler=CallerRunsPolicy: 队列满时由调用线程执行（降级而非丢弃）</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @org.springframework.beans.factory.annotation.Value("${business.async.core-pool-size:4}")
    private int corePoolSize;

    @org.springframework.beans.factory.annotation.Value("${business.async.max-pool-size:16}")
    private int maxPoolSize;

    @org.springframework.beans.factory.annotation.Value("${business.async.queue-capacity:100}")
    private int queueCapacity;

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("异步线程池初始化: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), 100);
        return executor;
    }
}
