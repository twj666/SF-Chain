package com.suifeng.sfchain.core.logging.upload;

import com.suifeng.sfchain.config.SfChainLoggingProperties;
import com.suifeng.sfchain.core.logging.AICallLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncAICallLogUploaderTest {

    @Test
    void shouldRetryUntilSuccess() {
        SfChainLoggingProperties properties = new SfChainLoggingProperties();
        properties.setSampleRate(1.0);
        properties.setBatchSize(10);
        properties.setMaxRetry(2);

        AtomicInteger uploadAttempts = new AtomicInteger();
        AICallLogUploadClient uploadClient = items -> uploadAttempts.incrementAndGet() >= 2;
        AsyncAICallLogUploader uploader = new AsyncAICallLogUploader(properties, uploadClient);

        uploader.publish(sampleLog("call-retry"));
        uploader.stop();

        assertThat(uploadAttempts.get()).isEqualTo(2);
        assertThat(uploader.stats().getSuccessCount()).isEqualTo(1);
        assertThat(uploader.stats().getFailedCount()).isZero();
    }

    @Test
    void shouldCountSampledOutLogs() {
        SfChainLoggingProperties properties = new SfChainLoggingProperties();
        properties.setSampleRate(0.0);

        AtomicInteger uploadAttempts = new AtomicInteger();
        AICallLogUploadClient uploadClient = items -> {
            uploadAttempts.incrementAndGet();
            return true;
        };
        AsyncAICallLogUploader uploader = new AsyncAICallLogUploader(properties, uploadClient);

        uploader.publish(sampleLog("call-sampled"));
        uploader.stop();

        assertThat(uploadAttempts.get()).isZero();
        assertThat(uploader.stats().getSampledOutCount()).isEqualTo(1);
    }

    private static AICallLog sampleLog(String callId) {
        return AICallLog.builder()
                .callId(callId)
                .operationType("TEST_OP")
                .modelName("test-model")
                .callTime(LocalDateTime.now())
                .duration(10L)
                .status(AICallLog.CallStatus.SUCCESS)
                .requestParams(AICallLog.AIRequestParams.builder().build())
                .build();
    }
}
