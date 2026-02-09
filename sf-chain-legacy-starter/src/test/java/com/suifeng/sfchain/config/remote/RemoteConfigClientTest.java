package com.suifeng.sfchain.config.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suifeng.sfchain.config.SfChainServerProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteConfigClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldAcceptSignedSnapshotResponse() throws Exception {
        String secret = "sign-secret";
        String body = "{\"version\":\"v-signed-1\"}";

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/config/snapshot", exchange -> {
            long ts = System.currentTimeMillis();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("X-SF-SIGNATURE-TS", String.valueOf(ts));
            exchange.getResponseHeaders().add("X-SF-SIGNATURE", sign(secret, ts, body));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        serverProperties.setApiKey("center-key");
        serverProperties.setResponseSignatureEnabled(true);
        serverProperties.setResponseSigningSecret(secret);

        RemoteConfigClient client = new RemoteConfigClient(new ObjectMapper(), serverProperties);
        Optional<RemoteConfigSnapshot> snapshotOpt = client.fetchSnapshot(null);

        assertThat(snapshotOpt).isPresent();
        assertThat(snapshotOpt.get().getVersion()).isEqualTo("v-signed-1");
    }

    @Test
    void shouldRejectReplaySignedResponse() throws Exception {
        String secret = "sign-secret";
        String body = "{\"version\":\"v-replay-1\"}";
        long fixedTs = System.currentTimeMillis();
        String signature = sign(secret, fixedTs, body);
        AtomicInteger requestCount = new AtomicInteger();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/config/snapshot", exchange -> {
            requestCount.incrementAndGet();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("X-SF-SIGNATURE-TS", String.valueOf(fixedTs));
            exchange.getResponseHeaders().add("X-SF-SIGNATURE", signature);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        SfChainServerProperties serverProperties = new SfChainServerProperties();
        serverProperties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        serverProperties.setApiKey("center-key");
        serverProperties.setResponseSignatureEnabled(true);
        serverProperties.setResponseSigningSecret(secret);

        RemoteConfigClient client = new RemoteConfigClient(new ObjectMapper(), serverProperties);
        assertThat(client.fetchSnapshot(null)).isPresent();

        assertThatThrownBy(() -> client.fetchSnapshot("v-replay-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重放");
        assertThat(requestCount.get()).isEqualTo(2);
    }

    private static String sign(String secret, long ts, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((ts + "\n" + body).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
