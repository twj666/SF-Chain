package com.suifeng.sfchain.core.logging.upload;

import com.suifeng.sfchain.config.SfChainLoggingProperties;
import com.suifeng.sfchain.core.logging.AICallLog;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI调用日志异步批量上报器
 */
@Slf4j
public class AsyncAICallLogUploader implements AICallLogUploadGateway {

    private static final int MAX_BATCHES_PER_CYCLE = 8;
    private final SfChainLoggingProperties loggingProperties;
    private final AICallLogUploadClient uploadClient;
    private final LinkedBlockingQueue<AICallLogUploadItem> queue;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong sampledOutCount = new AtomicLong();
    private final AtomicLong droppedCount = new AtomicLong();
    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failedCount = new AtomicLong();

    public AsyncAICallLogUploader(
            SfChainLoggingProperties loggingProperties,
            AICallLogUploadClient uploadClient) {
        this.loggingProperties = loggingProperties;
        this.uploadClient = uploadClient;
        this.queue = new LinkedBlockingQueue<>(Math.max(loggingProperties.getQueueCapacity(), 100));
    }

    @PostConstruct
    public void start() {
        long interval = Math.max(loggingProperties.getUploadIntervalSeconds(), 1) * 1000L;
        scheduler.scheduleWithFixedDelay(this::flushSafely, interval, interval, TimeUnit.MILLISECONDS);
        log.info("AI调用日志异步上报已启动, interval={}ms, batchSize={}", interval, loggingProperties.getBatchSize());
    }

    @PreDestroy
    public void stop() {
        flushSafely();
        scheduler.shutdownNow();
    }

    @Override
    public void publish(AICallLog callLog) {
        if (ThreadLocalRandom.current().nextDouble() > clampSampleRate(loggingProperties.getSampleRate())) {
            sampledOutCount.incrementAndGet();
            return;
        }

        AICallLogUploadItem item = AICallLogUploadItem.from(callLog, loggingProperties.isUploadContent());
        boolean offered = queue.offer(item);
        if (!offered) {
            droppedCount.incrementAndGet();
            log.debug("AI调用日志队列已满，丢弃 callId={}", callLog.getCallId());
        }
    }

    @Override
    public AICallLogUploadStats stats() {
        return AICallLogUploadStats.builder()
                .queueSize(queue.size())
                .sampledOutCount(sampledOutCount.get())
                .droppedCount(droppedCount.get())
                .successCount(successCount.get())
                .failedCount(failedCount.get())
                .build();
    }

    private void flushSafely() {
        try {
            flushBatch();
        } catch (Exception e) {
            log.warn("AI调用日志批量上报失败: {}", e.getMessage());
        }
    }

    private void flushBatch() {
        int batchSize = Math.max(loggingProperties.getBatchSize(), 1);
        for (int i = 0; i < MAX_BATCHES_PER_CYCLE; i++) {
            List<AICallLogUploadItem> batch = new ArrayList<>(batchSize);
            queue.drainTo(batch, batchSize);
            if (batch.isEmpty()) {
                return;
            }
            if (uploadWithRetry(batch)) {
                successCount.addAndGet(batch.size());
                continue;
            }
            failedCount.addAndGet(batch.size());
            log.warn("AI调用日志上报失败且超过重试次数，丢弃 {} 条", batch.size());
        }
    }

    private boolean uploadWithRetry(List<AICallLogUploadItem> batch) {
        int maxRetry = Math.max(loggingProperties.getMaxRetry(), 0);
        for (int attempt = 0; attempt <= maxRetry; attempt++) {
            if (uploadClient.upload(batch)) {
                return true;
            }
        }
        return false;
    }

    private static double clampSampleRate(double sampleRate) {
        if (sampleRate < 0) {
            return 0;
        }
        return Math.min(sampleRate, 1.0);
    }
}
