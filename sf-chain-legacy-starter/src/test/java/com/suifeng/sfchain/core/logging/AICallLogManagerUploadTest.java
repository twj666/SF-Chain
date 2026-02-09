package com.suifeng.sfchain.core.logging;

import com.suifeng.sfchain.config.SfChainLoggingProperties;
import com.suifeng.sfchain.core.logging.upload.AICallLogUploadGateway;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AICallLogManagerUploadTest {

    @Test
    void shouldPublishToUploadGatewayWhenLogAdded() {
        SfChainLoggingProperties properties = new SfChainLoggingProperties();
        AtomicReference<String> publishedCallId = new AtomicReference<>();
        AICallLogUploadGateway gateway = callLog -> publishedCallId.set(callLog.getCallId());

        AICallLogManager manager = new AICallLogManager(properties, gateway);
        AICallLog log = AICallLog.builder()
                .callId("call-1")
                .callTime(LocalDateTime.now())
                .status(AICallLog.CallStatus.SUCCESS)
                .duration(10L)
                .frequency(1)
                .lastAccessTime(LocalDateTime.now())
                .build();

        manager.addLog(log);

        assertThat(publishedCallId.get()).isEqualTo("call-1");
    }
}
