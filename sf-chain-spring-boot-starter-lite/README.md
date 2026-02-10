# SF-Chain Starter Lite

`sf-chain-spring-boot-starter-lite` provides SF-Chain core runtime without forcing web/JPA/JDBC/database driver dependencies into consumer projects.

Use this starter when you only need:
- `@AIOp`
- `BaseAIOperation`
- `AIService`
- model dispatch and operation execution

Management API, static UI, and local persistence are not included.

## Maven

```xml
<dependency>
  <groupId>io.github.twj666</groupId>
  <artifactId>sf-chain-spring-boot-starter-lite</artifactId>
  <version>1.0.11</version>
</dependency>
```

## Minimal Configuration

```yaml
sf-chain:
  enabled: true
  config-sync:
    enabled: true
    interval-seconds: 30
    startup-check-enabled: true
    startup-max-attempts: 3
    startup-retry-interval-ms: 2000
  server:
    base-url: http://127.0.0.1:19090
    api-key: ${SF_CHAIN_SERVER_API_KEY}
    tenant-id: default
    app-id: your-app
  logging:
    upload-enabled: true
```

- `startup-check-enabled=true`: startup requires first successful remote sync (with retry).
- `startup-check-enabled=false`: startup does not block on first sync; periodic sync keeps running.

## What This Starter Auto-Configures

- Core runtime from `sf-chain-core`
- Remote config sync from `sf-chain-config-client`
- Async AI-call log upload client (HTTP) when `sf-chain.logging.upload-enabled=true`
