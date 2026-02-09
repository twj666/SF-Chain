# SF-Chain 解耦升级与配置中心方案（深度调研版）

## 1. 背景与目标

当前 `SF-Chain` 在使用侧存在显著耦合：
- 引入一个依赖，实际引入了 Web、JPA、JDBC、MySQL/PostgreSQL 驱动、前端静态资源、控制器等整套能力。
- 使用方如果只是想“写 Operation 并调用 AI”，也会被动承载数据库初始化、Web 路由、管理端接口等。
- 配置变更（模型、Operation 参数）和 AI 执行面耦合在同一进程内，扩展和多租户治理困难。

目标是升级为“最小执行 SDK + 中央配置中心”的双层架构：
1. 使用方只引入最小能力包，专注 Operation 编写与调用。
2. 配置中心独立部署，管理模型配置、Operation 配置、策略、密钥发放。
3. 使用方通过 `sf-chain.server` + `api-key` 获取并定时同步配置。
4. AI 调用日志具备可观测与可回传能力，同时不显著增加调用延迟。

---

## 2. 现状调研结论（基于代码）

### 2.1 `SF-Chain` 当前耦合根因

#### A. 依赖层耦合（POM）
`/Users/suifeng/Code/SF-Chain/pom.xml` 当前直接包含：
- `spring-boot-starter-web`
- `spring-boot-starter-webflux`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-jdbc`
- `mysql-connector-j`（runtime）
- `postgresql`（runtime）

这意味着任何消费者引入 `sf-chain`，都会被动引入以上能力与版本约束。

#### B. 自动装配耦合（全量扫描）
`/Users/suifeng/Code/SF-Chain/src/main/java/com/suifeng/sfchain/config/SfChainAutoConfiguration.java`
包含：
- `@ComponentScan(basePackages = "com.suifeng.sfchain")`
- `@EnableJpaRepositories`
- `@EntityScan`

该模式会将 controller / web config / persistence / 日志组件整体注入，无法按需裁剪。

#### C. Web 管理面耦合
`controller` 包下多个 `@RestController` 会随 starter 进入宿主：
- `AIOperationController`
- `AIModelController`
- `AISystemController`
- `AICallLogController`
- `SfChainConfigController`

且路径受 `sf-chain.path.api-prefix` 影响，实质是把“管理中心能力”嵌进了使用方应用。

#### D. DB 初始化耦合
`DatabaseInitializationService` 在 `sf-chain.persistence.database-type` 配置出现时会执行 `migration/v1_mysql.sql` / `migration/v1_postgresql.sql`。

虽然你提到 `poet-agent/src/main/resources/db/migration`，但从代码看 SF-Chain 读取的是**自身 classpath** 的 `migration/*.sql`。问题本质是：
- 使用方为了启用 SF-Chain 配置管理，不得不带上 DataSource/JPA/SQL 脚本路径和驱动语义。

### 2.2 `poet-agent` 现有接入方式

`/Users/suifeng/Code/ai-poet/poet-agent` 中：
- `pom.xml` 引入 `io.github.twj666:sf-chain:1.0.8`
- 业务 Operation（`AnnotationAnalysisOperation` 等）继承 `BaseAIOperation`
- 业务服务注入 `com.suifeng.sfchain.core.AIService` 调用

这说明你的核心价值已经很清晰：
- 用户要的是 `AIOp + BaseAIOperation + AIService` 这套“编排执行能力”
- 不是要把模型管理后台和数据库初始化都嵌进业务系统

---

## 3. 新架构建议：Control Plane / Data Plane 分离

### 3.1 总体分层

- **Data Plane（本地执行面）**：运行在使用方服务中
  - Operation 注册与执行
  - Prompt 构建
  - 模型调用（直连 AI 厂商）
  - 本地缓存配置 + 热更新

- **Control Plane（中央控制面）**：独立部署
  - 模型配置中心
  - Operation 参数配置中心
  - API Key / 租户鉴权
  - 策略下发、版本管理、审计
  - 日志汇聚与分析

### 3.2 推荐 Maven 模块拆分

建议拆成多模块（可逐步迁移）：

1. `sf-chain-core`
- 仅保留：`AIOp`、`BaseAIOperation`、`AIService`、`AIOperationRegistry`、`AIPromptBuilder`、Model 抽象
- 不包含 Spring MVC、JPA、Controller、SQL、静态资源
- 依赖尽量收敛到 `spring-context` + `jackson` + 可选 `reactor`

2. `sf-chain-openai`（或 provider-*）
- OpenAI Compatible 具体实现：`OpenAIModelFactory` 等
- 将模型提供商适配与 core 解耦

3. `sf-chain-config-client`
- 配置中心客户端（HTTP/gRPC）
- 周期拉取 + ETag/版本号 + 本地缓存
- API Key 鉴权头注入

4. `sf-chain-logging-client`
- 异步日志上报（批量、压缩、重试、降级本地）

5. `sf-chain-spring-boot-starter-lite`（推荐给使用方）
- 只自动装配 core + provider + config-client
- 默认不启 Web 管理端、不启 DB 持久化

6. `sf-chain-control-plane`（独立服务）
- 管理 API + 控制台 UI + 存储层（MySQL/PG二选一即可）

### 3.3 自动配置策略（关键）

不要再使用全包 `@ComponentScan("com.suifeng.sfchain")`。
改为**显式 @Bean + 条件装配**：
- `@ConditionalOnClass`
- `@ConditionalOnProperty`
- `@ConditionalOnMissingBean`

拆分配置类：
- `SfChainCoreAutoConfiguration`
- `SfChainOpenAIAutoConfiguration`
- `SfChainConfigClientAutoConfiguration`
- `SfChainLoggingClientAutoConfiguration`
- `SfChainControlPlaneServerAutoConfiguration`（仅中心服务启用）

---

## 4. 配置中心协议设计（建议）

### 4.1 使用方配置（目标形态）

```yaml
sf-chain:
  enabled: true
  tenant-id: poet-prod
  api-key: ${SF_CHAIN_API_KEY}
  server:
    base-url: http://sf-chain-control-plane:8080
    connect-timeout-ms: 3000
    read-timeout-ms: 5000
  config-sync:
    enabled: true
    mode: pull
    interval-seconds: 30
    fail-open: true
  logging:
    upload-enabled: true
    batch-size: 100
    flush-interval-seconds: 5
```

### 4.2 配置下发模型

按租户返回一个“配置快照”即可：
- `version`
- `models[]`
- `operations[]`
- `policies`（限流、超时、重试等）

客户端逻辑：
1. 启动先拉取一次。
2. 周期拉取（携带当前版本）。
3. 无变化返回 `304` 或 `not_modified=true`。
4. 有变化原子替换本地配置。

### 4.3 配置生效优先级

建议统一优先级，避免混乱：
1. 代码强制参数（调用时显式传入）
2. 远程配置中心
3. 注解默认值 `@AIOp`
4. 框架内置默认值

---

## 5. AI 调用日志方案（你关心的重点）

你提出了核心矛盾：
- 调用方本地直连模型 => 延迟低，但日志如何统一汇聚？
- 控制中心代调用模型 => 日志天然集中，但引入额外跳数和延迟。

### 5.1 三种可选模式

#### 模式 A：本地调用 + 异步上报日志（推荐默认）
- 使用方执行 AI 调用。
- 产生结构化日志事件，进入本地队列。
- 独立线程批量上报到控制中心。

优点：低延迟、中心可观测、可降级。  
缺点：日志最终一致，非强实时。

#### 模式 B：中心代理调用（网关模式）
- 使用方请求先到控制中心，再由中心调用模型。

优点：审计统一、策略统一、密钥不下发到使用方。  
缺点：延迟上升明显，中心成为性能与可用性瓶颈。

#### 模式 C：混合模式（推荐长期形态）
- 实时高敏链路：本地调用（A）
- 强审计/强风控链路：中心代理（B）

### 5.2 日志字段规范（最小集）

- `traceId` / `callId`
- `tenantId` / `appId`
- `operationType`
- `modelName`
- `startTime` / `durationMs`
- `status` / `errorCode`
- `inputTokens` / `outputTokens`
- `cost`（可选）
- `promptHash`（建议，不直接上传明文 prompt）

### 5.3 隐私与安全建议

- 默认不上报原文输入输出，只上报摘要/哈希/长度。
- 对敏感字段可配置脱敏规则。
- 采样上报（例如 10% 全量，90% 指标化）。

---

## 6. 是否演进成 n8n / Dify 式远程智能体平台？

结论：**可以演进，但不建议在本次解耦阶段一起做重平台化**。

建议路线：
1. 先完成“解耦 + 配置中心 + 日志汇聚”的基础设施化。
2. 稳定后再增量加“工作流编排/Agent工具调用/UI编排器”。

原因：
- 当前最痛点是依赖与部署耦合，不是编排能力不足。
- 一步到位做平台会显著拉长交付周期和风险。

---

## 7. 分阶段迁移方案（可执行）

### Phase 1：止血（1~2周）
- 拆 `sf-chain` 为 `sf-chain-core` 与 `sf-chain-starter-lite`。
- `starter-lite` 去掉：JPA/JDBC/MySQL/PG/Web Controller/静态资源。
- `poet-agent` 改为依赖 `starter-lite`。

验收标准：
- 使用方只引入最小依赖即可跑通 Operation 调用。
- 不再自动暴露 SF-Chain 管理端接口。

### Phase 2：配置中心 MVP（2~4周）
- 上线 `sf-chain-control-plane`：
  - API Key 管理
  - 模型配置管理
  - Operation 配置管理
  - 配置版本下发
- 客户端实现周期拉取 + 热更新。

验收标准：
- 修改中心配置后，30s 内客户端可自动生效。
- 使用方无需重启即可切换 Operation 模型。

### Phase 3：日志中心（2~3周）
- 上线日志异步上报 SDK。
- 控制中心增加查询与统计面板。

验收标准：
- 业务调用 p95 延迟增量 < 5ms（本地模式）。
- 控制中心日志完整率 > 99%。

### Phase 4：高级能力（按需）
- 灰度发布（按租户/应用/Operation）
- 配置审批流与审计
- 可选代理调用链路
- 工作流/Agent 编排能力

---

## 8. 对你当前构想的评估与补强

你的方向是正确的，建议补三点：

1. **API Key 之外加租户与应用维度**
- 至少 `tenantId + appId + apiKey` 三元组，便于隔离与统计。

2. **配置要版本化而非“覆盖式”**
- 任何变更都应有 `version` 与变更记录，支持回滚。

3. **日志优先异步事件化**
- 不要让主调用链同步依赖中心可用性。
- 中心不可达时本地降级缓存/落盘，恢复后补传。

---

## 9. 对 `poet-agent` 的改造示例

### 9.1 依赖
将：
- `io.github.twj666:sf-chain`

替换为：
- `io.github.twj666:sf-chain-spring-boot-starter-lite`
- （可选）`io.github.twj666:sf-chain-config-client`

### 9.2 配置
保留极简：

```yaml
sf-chain:
  enabled: true
  api-key: ${SF_CHAIN_API_KEY}
  server:
    base-url: http://sf-chain-config-center:8080
  config-sync:
    interval-seconds: 30
```

### 9.3 业务代码
`AnnotationAnalysisOperation` / `ContentModificationOperation` / `CopywritingGenerationOperation` 代码基本不用变：
- 继续 `@AIOp`
- 继续继承 `BaseAIOperation`
- 继续通过 `AIService` 调用

变更主要在：配置来源从本地/DB迁移到远程中心。

---

## 10. 技术风险与规避

- 风险：远程配置不可用导致启动失败  
  规避：`fail-open + 本地最后成功快照`。

- 风险：配置热更新与执行并发冲突  
  规避：不可变快照 + 原子引用替换。

- 风险：日志上报挤占业务线程  
  规避：无锁队列 + 批量发送 + 背压丢弃策略。

- 风险：多租户误配  
  规避：租户级命名空间 + 服务端签名校验。

---

## 11. 本次调研结论（一句话）

`SF-Chain` 应从“嵌入式全家桶”升级为“轻量执行 SDK + 独立控制中心”，日志采用“本地执行、异步上报”为主、代理调用为辅的混合模式；这样可以同时解决耦合、可运维和性能问题。

