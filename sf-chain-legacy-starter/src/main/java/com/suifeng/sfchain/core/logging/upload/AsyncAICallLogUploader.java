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

/**
 * AI调用日志异步批量上报器
 */
@Slf4j
public class AsyncAICallLogUploader implements AICallLogUploadGateway {

    private final SfChainLoggingProperties loggingProperties;
    private final AICallLogUploadClient uploadClient;
    private final LinkedBlockingQueue<AICallLogUploadItem> queue;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public AsyncAICallLogUploader(
            SfChainLoggingProperties loggingProperties,
            AICallLogUploadClient uploadClient) {
        this.loggingProperties = loggingProperties;
        this.uploadClient = uploadClient;
        this.queue = new LinkedBlockingQueue<>(Math.max(loggingProperties.getQueueCapacity(), 100));
    }

    @PostConstruct
    public void start() {
        long interval = Math.max(loggingProperties.getFlushIntervalMs(), 1000L);
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
        if (Math.random() > clampSampleRate(loggingProperties.getSampleRate())) {
            return;
        }

        AICallLogUploadItem item = AICallLogUploadItem.from(callLog, loggingProperties.isUploadContent());
        boolean offered = queue.offer(item);
        if (!offered) {
            log.debug("AI调用日志队列已满，丢弃 callId={}", callLog.getCallId());
        }
    }

    private void flushSafely() {
        try {
            flushBatch();
        } catch (Exception e) {
            log.warn("AI调用日志批量上报失败: {}", e.getMessage());
        }
    }

    private void flushBatch() throws InterruptedException {
        int batchSize = Math.max(loggingProperties.getBatchSize(), 1);
        List<AICallLogUploadItem> batch = new ArrayList<>(batchSize);
        queue.drainTo(batch, batchSize);
        if (batch.isEmpty()) {
            return;
        }

        int maxRetry = Math.max(loggingProperties.getMaxRetry(), 0);
        for (int attempt = 0; attempt <= maxRetry; attempt++) {
            if (uploadClient.upload(batch)) {
                return;
            }
            if (attempt < maxRetry) {
                long backoffMs = Math.min(1000L * (1L << attempt), 8000L);
                Thread.sleep(backoffMs);
            }
        }

        log.warn("AI调用日志上报失败且超过重试次数，丢弃 {} 条", batch.size());
    }

    private static double clampSampleRate(double sampleRate) {
        if (sampleRate < 0) {
            return 0;
        }
        return Math.min(sampleRate, 1.0);
    }
}
