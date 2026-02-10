# SF-Chain

SF-Chain 是一个面向 Spring Boot 的 AI 调度框架，当前采用“配置中心 + 轻量客户端”架构：

- 配置中心独立部署：`sf-chain-config-center-server`
- 租户应用仅引入轻量依赖：`sf-chain-spring-boot-starter-lite`

## 模块说明

- `sf-chain-core`：运行时核心能力（`@AIOp`、`BaseAIOperation`、`AIService`、模型/操作注册、日志基础能力）
- `sf-chain-config-client`：远程配置拉取与同步能力
- `sf-chain-spring-boot-starter-lite`：租户侧 starter（组合 core + config-client + 日志上报客户端）
- `sf-chain-config-center-server`：配置中心服务端（租户/应用/API Key/模型/操作配置管理与日志接入）

## 租户侧接入（推荐）

### 1. Maven 依赖

```xml
<dependency>
  <groupId>io.github.twj666</groupId>
  <artifactId>sf-chain-spring-boot-starter-lite</artifactId>
  <version>1.0.11</version>
</dependency>
```

### 2. 最小配置

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

`startup-check-enabled=true` 时，应用启动会先完成远程配置同步（失败按重试策略，超限后启动失败）。

`startup-check-enabled=false` 时，应用不会因为首轮同步失败而中断，后续按定时任务继续同步。

## 配置中心部署

配置中心详细说明见：

- `/Users/suifeng/Code/SF-Chain/sf-chain-config-center-server/README.md`

## 迁移说明

历史 `legacy` 模块已移除。迁移指引见：

- `/Users/suifeng/Code/SF-Chain/docs/migration-to-config-center.md`

## 许可证

Apache License 2.0
