# SF-Chain 迁移指南（Legacy -> Config Center）

本指南用于将旧版单体接入方式迁移到新版“配置中心 + 轻量客户端”架构。

## 1. 依赖迁移

- 删除旧依赖（如历史 `sf-chain` / `legacy` 相关依赖）
- 新增：

```xml
<dependency>
  <groupId>io.github.twj666</groupId>
  <artifactId>sf-chain-spring-boot-starter-lite</artifactId>
  <version>1.0.11</version>
</dependency>
```

## 2. 配置迁移

删除旧配置项（例如本地管理端、local persistence、auth-token 等旧模式配置），替换为：

```yaml
sf-chain:
  enabled: true
  config-sync:
    enabled: true
    interval-seconds: 30
  server:
    base-url: http://127.0.0.1:19090
    api-key: ${SF_CHAIN_SERVER_API_KEY}
    tenant-id: default
    app-id: your-app
  logging:
    upload-enabled: true
```

## 3. 配置中心部署

在独立服务部署配置中心模块：`sf-chain-config-center-server`。

必要环境变量：

- `SF_CHAIN_AUTH_TOKEN`
- `SF_CHAIN_INGESTION_API_KEY`
- `SF_CHAIN_DB_TYPE`
- `SF_CHAIN_DB_URL`
- `SF_CHAIN_DB_USERNAME`
- `SF_CHAIN_DB_PASSWORD`

## 4. 启动顺序建议

1. 启动配置中心
2. 在配置中心创建 tenant / app / api key
3. 启动租户应用并启用 `config-sync`
4. 观察租户同步日志与配置中心日志上报接口

## 5. 验证要点

- 配置同步接口：`GET /v1/config/snapshot`
- 日志上报接口：`POST /v1/logs/ai-calls/batch`
- 管理面接口：`/sf-chain/control/**`
