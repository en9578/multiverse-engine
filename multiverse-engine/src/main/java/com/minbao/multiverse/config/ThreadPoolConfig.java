package com.minbao.multiverse.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置：三个隔离池，均带 MdcTaskDecorator 实现 traceId 透传。
 */
@Configuration
public class ThreadPoolConfig {

    /** 多元宇宙编排池：核心 4，最大 8，队列 200 */
    @Bean("multiverseExecutor")
    public ThreadPoolTaskExecutor multiverseExecutor() {
        ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
        e.setCorePoolSize(4);
        e.setMaxPoolSize(8);
        e.setQueueCapacity(200);
        e.setKeepAliveSeconds(60);
        e.setThreadNamePrefix("multiverse-");
        e.setTaskDecorator(new MdcTaskDecorator());
        e.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        e.initialize();
        return e;
    }

    /** 百炼 API 调用池：核心 2，最大 4，队列 50 */
    @Bean("bailianExecutor")
    public ThreadPoolTaskExecutor bailianExecutor() {
        ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
        e.setCorePoolSize(2);
        e.setMaxPoolSize(4);
        e.setQueueCapacity(50);
        e.setKeepAliveSeconds(60);
        e.setThreadNamePrefix("bailian-");
        e.setTaskDecorator(new MdcTaskDecorator());
        e.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        e.initialize();
        return e;
    }

    /** 宇宙探索池：核心 2，最大 4，队列 100 */
    @Bean("explorationExecutor")
    public ThreadPoolTaskExecutor explorationExecutor() {
        ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
        e.setCorePoolSize(2);
        e.setMaxPoolSize(4);
        e.setQueueCapacity(100);
        e.setKeepAliveSeconds(60);
        e.setThreadNamePrefix("exploration-");
        e.setTaskDecorator(new MdcTaskDecorator());
        e.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        e.initialize();
        return e;
    }

    /** 将当前线程的 MDC 上下文复制到异步任务中 */
    private static class MdcTaskDecorator implements org.springframework.core.task.TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                if (context != null) MDC.setContextMap(context);
                try { runnable.run(); } finally { MDC.clear(); }
            };
        }
    }
}
