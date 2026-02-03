package com.example.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration
 * 
 * Configures async method execution with a custom thread pool.
 * This is used for background tasks like sending emails via Lambda.
 * 
 * Without this configuration, Spring uses a default SimpleAsyncTaskExecutor
 * which creates a new thread for each task (not efficient).
 * 
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * Custom Async Thread Pool Executor
     * 
     * Configuration:
     * - Core Pool Size: 5 threads always running
     * - Max Pool Size: 10 threads maximum
     * - Queue Capacity: 100 tasks can wait
     * - Thread Name Prefix: "async-" for easy identification
     * 
     * How it works:
     * 1. First 5 tasks → Use core threads
     * 2. Tasks 6-105 → Queued (waiting)
     * 3. Tasks 106-115 → Create new threads (up to max 10)
     * 4. Task 116+ → Rejected (throws exception)
     * 
     * When threads are idle for 60 seconds, they're terminated
     * down to the core pool size.
     * 
     * @return Configured ThreadPoolTaskExecutor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core number of threads (always kept alive)
        executor.setCorePoolSize(5);
        
        // Maximum number of threads
        executor.setMaxPoolSize(10);
        
        // Queue capacity (tasks waiting for a thread)
        executor.setQueueCapacity(100);
        
        // Thread name prefix (helps with debugging)
        executor.setThreadNamePrefix("async-email-");
        
        // Allow core threads to timeout when idle
        executor.setAllowCoreThreadTimeOut(true);
        
        // Timeout for idle threads (seconds)
        executor.setKeepAliveSeconds(60);
        
        // Initialize the executor
        executor.initialize();
        
        return executor;
    }
}

/**
 * USAGE:
 * ======
 * 
 * 1. In Service Methods:
 * 
 *    @Service
 *    public class EmailService {
 *        
 *        @Async("taskExecutor")  // Use this executor
 *        public void sendEmail(OrderEmailEvent event) {
 *            // This runs in background thread
 *            lambdaClient.invoke(...);
 *        }
 *    }
 * 
 * 
 * 2. Default Executor (if no name specified):
 * 
 *    @Async  // Uses default SimpleAsyncTaskExecutor
 *    public void someMethod() {
 *        // Runs in background
 *    }
 *    
 *    vs
 *    
 *    @Async("taskExecutor")  // Uses our custom executor
 *    public void someMethod() {
 *        // Runs in background with thread pool
 *    }
 * 
 * 
 * THREAD POOL SIZING GUIDE:
 * =========================
 * 
 * For I/O-bound tasks (like Lambda invocations):
 * - Core Pool Size: 2 * number of CPU cores
 * - Max Pool Size: 4 * number of CPU cores
 * 
 * For CPU-bound tasks:
 * - Core Pool Size: number of CPU cores
 * - Max Pool Size: number of CPU cores + 1
 * 
 * For light workload (emails):
 * - Core: 2-5
 * - Max: 10-20
 * 
 * For heavy workload:
 * - Core: 10-20
 * - Max: 50-100
 * 
 * Current configuration (5 core, 10 max) is good for:
 * - Low to medium email volume
 * - Less than 100 orders per minute
 * 
 * 
 * MONITORING:
 * ===========
 * 
 * To monitor thread pool:
 * 
 * @Autowired
 * private ThreadPoolTaskExecutor taskExecutor;
 * 
 * public void logThreadPoolStats() {
 *     log.info("Active threads: {}", taskExecutor.getActiveCount());
 *     log.info("Pool size: {}", taskExecutor.getPoolSize());
 *     log.info("Queue size: {}", taskExecutor.getThreadPoolExecutor().getQueue().size());
 * }
 * 
 * 
 */
