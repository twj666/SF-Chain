# SF-Chain 渐进式重构实施手册（稳定升级版）

## 1. 文档目标与原则

本手册用于指导 `SF-Chain` 从“单体嵌入式框架”升级为“轻量 SDK + 中央配置中心”的渐进式重构，目标是：
- 不中断现有业务（尤其是 `poet-agent`）
- 每一步可验证、可回滚
- 逐步降低依赖耦合与部署复杂度
- 逐步建立配置治理与日志治理能力

核心原则：
1. 先兼容，后替换，再收敛。
2. 每阶段必须有可量化验收标准。
3. 所有核心变更提供回滚路径。
4. 不做“大爆炸式”一次性重写。

---

## 2. 当前基线（重构前冻结）

## 2.1 基线版本
- 仓库：`/Users/suifeng/Code/SF-Chain`
- 当前产物：`io.github.twj666:sf-chain`（单 jar）
- 典型使用方：`/Users/suifeng/Code/ai-poet/poet-agent`

## 2.2 基线能力快照（必须先记录）
在重构开始前，先输出一份“现状能力清单”（作为回归对照）：
- `AIService.execute` / `executeStream` 功能行为
- `@AIOp + BaseAIOperation` 注册行为
- 现有配置项行为（`sf-chain.*`, `ai.openai-models.*`, `ai.operations.*`）
- 管理接口可用性（当前 controller）
- 持久化行为（模型配置、操作配置、migration 初始化）

产出文件：
- `/Users/suifeng/Code/SF-Chain/docs/refactor/baseline-capabilities.md`
- `/Users/suifeng/Code/SF-Chain/docs/refactor/baseline-api-snapshots.md`

## 2.3 基线质量门禁
- 单元测试：记录当前通过率
- 集成测试：至少覆盖 `poet-agent` 关键调用链
- 性能基线：记录核心接口 p50/p95/p99 与错误率

---

## 3. 总体路线图（建议 6 个阶段）

- Phase 0：准备与护栏（1 周）
- Phase 1：代码内解耦，不改外部行为（1~2 周）
- Phase 2：拆分 Maven 模块，发布并行产物（2 周）
- Phase 3：接入配置中心客户端（MVP）（2~3 周）
- Phase 4：日志异步上报（1~2 周）
- Phase 5：消费者迁移与旧能力下线（2~4 周）

整个流程中，任何阶段不达标，不进入下一阶段。

---

## 4. Phase 0：准备与护栏

## 4.1 分支与发布策略
- 新建长期重构分支：`codex/refactor-sf-chain-core-split`
- 主干仅合入“小步可运行”改动
- 发布策略采用“双轨”：
  - 旧轨：`sf-chain 1.x`
  - 新轨：`sf-chain-core/starter-lite 2.x`

## 4.2 建立回归用例
必须先补齐最小回归集：
1. Operation 注册与执行（同步）
2. Operation 流式执行
3. 默认模型选择逻辑
4. 注解默认值回退逻辑
5. 配置覆盖优先级逻辑
6. 异常与超时处理

建议目录：
- `/Users/suifeng/Code/SF-Chain/src/test/java/.../core/`
- `/Users/suifeng/Code/SF-Chain/src/test/java/.../integration/`

## 4.3 引入重构守卫指标
每次 PR 必须输出：
- 依赖树 diff（确保“只减不增”）
- 启动日志关键片段（确保没有异常自动装配）
- 回归测试结果

---

## 5. Phase 1：代码内解耦（保持旧对外接口不变）

目标：不改 Maven 坐标、不改使用方代码，先在内部把耦合结构拆开。

## 5.1 拆分包边界（逻辑层）
将现有代码按责任明确分层：
- `core`：执行引擎与抽象
- `provider`：模型实现
- `config-client`：远程配置拉取（先空壳）
- `logging-client`：异步日志（先空壳）
- `control-plane-web`：管理接口
- `persistence-local`：本地 DB 持久化

注意：此阶段可先通过 package 重组 + facade 兼容，不必立即改 artifact。

## 5.2 删除全量扫描依赖（关键任务）
当前问题根源之一是 `@ComponentScan("com.suifeng.sfchain")`。

改造步骤：
1. 新增显式配置类：
   - `SfChainCoreAutoConfiguration`
   - `SfChainProviderAutoConfiguration`
   - `SfChainManagementAutoConfiguration`
   - `SfChainPersistenceAutoConfiguration`
2. 每个配置类用 `@ConditionalOnProperty` 控制开关。
3. 逐步移除全包扫描。

验收：
- 未开启管理/持久化开关时，不加载 controller、JPA、DataSource 相关 bean。

## 5.3 特性开关引入（强制）
新增配置开关（默认安全）：

```yaml
sf-chain:
  features:
    management-api: false
    local-persistence: false
    local-migration: false
    static-ui: false
```

验收：
- 默认配置下，框架只提供核心执行能力。

## 5.4 回归验证
- 旧项目引入 `sf-chain` 仍可运行
- 开启开关后旧能力可恢复
- 无破坏性行为变更

回滚：
- 回退到 Phase 0 tag
- 保留旧自动配置类并切回

---

## 6. Phase 2：Maven 模块拆分（并行发布）

目标：产物拆分，但旧产物继续可用。

## 6.1 目标模块结构
建议新增多模块聚合：
- `sf-chain-core`
- `sf-chain-openai`
- `sf-chain-config-client`
- `sf-chain-logging-client`
- `sf-chain-spring-boot-starter-lite`
- `sf-chain-control-plane-server`
- `sf-chain-legacy-starter`（兼容层，可选）

## 6.2 依赖治理规则
- `core` 禁止依赖 `spring-web`, `spring-data-jpa`, JDBC 驱动
- provider 模块与 core 解耦
- `starter-lite` 仅聚合最小运行时依赖
- `control-plane-server` 承担 Web+JPA+DB

## 6.3 发布策略
- 先发布 `2.0.0-alpha.1`（新模块）
- 保持 `1.x` 可维护
- `poet-agent` 先做灰度依赖切换（开发环境）

## 6.4 验收
- 使用方只引入 `starter-lite` 可执行 AI 调用
- `mvn dependency:tree` 明显减负（无 mysql/pgsql/jpa/web 管理依赖）

回滚：
- 使用方退回 `sf-chain 1.x`
- 新模块只在灰度环境使用，不影响生产

---

## 7. Phase 3：配置中心客户端（MVP）

目标：把“模型与 Operation 配置”迁移到中央服务。

## 7.1 最小接口协议（先固定）
服务端提供：
1. `POST /v1/auth/token/validate`
2. `GET /v1/config/snapshot?tenantId=...&appId=...&version=...`
3. `GET /v1/config/health`

返回结构至少包含：
- `version`
- `models[]`
- `operations[]`
- `policies`

## 7.2 客户端实现步骤
1. 启动首次拉取配置（失败走本地缓存）
2. 定时轮询（30s 可配置）
3. 版本无变更跳过
4. 版本变更原子更新
5. 更新后触发本地 registry refresh

## 7.3 本地缓存策略
缓存文件路径建议：
- `/var/lib/sf-chain/cache/config-snapshot.json`

规则：
- 拉取成功即覆盖缓存
- 拉取失败使用 last-known-good
- 超过容忍时间告警但不中断业务（fail-open）

## 7.4 配置优先级固化
- 运行时传参 > 远程配置 > 注解默认 > 框架默认

## 7.5 验收
- 在中心修改 Operation 对应模型，客户端 30s 内生效
- 客户端重启可从缓存恢复
- 中心不可用时服务不崩

回滚：
- 关闭 `sf-chain.config-sync.enabled`
- 回退到本地配置模式

---

## 8. Phase 4：AI 调用日志异步上报

目标：兼顾低延迟与中心可观测。

## 8.1 日志链路设计
- 调用完成后写入内存队列（非阻塞）
- 独立线程按 batch 上报
- 失败重试 + 指数退避
- 队列满时按策略丢弃低优先级日志并计数

## 8.2 上报内容分层
- 默认指标层：时延、状态、token、错误码
- 可选审计层：脱敏输入输出摘要

## 8.3 关键配置

```yaml
sf-chain:
  logging:
    upload-enabled: true
    queue-capacity: 10000
    batch-size: 200
    flush-interval-ms: 3000
    max-retry: 5
    sample-rate: 1.0
    upload-content: false
```

## 8.4 验收
- 业务主链路 p95 增量 < 5ms
- 中心可看到操作维度统计
- 中心不可用 30 分钟后恢复可补传（允许部分丢失，但可量化）

回滚：
- 关闭 `upload-enabled`
- 保留本地日志能力

---

## 9. Phase 5：消费者迁移与旧能力下线

目标：分批把现有项目迁移到新体系。

## 9.1 迁移批次建议
1. 批次 A：内部测试项目
2. 批次 B：`poet-agent` 预发环境
3. 批次 C：`poet-agent` 生产灰度（10% -> 50% -> 100%）

## 9.2 单项目迁移步骤（模板）
1. 依赖替换为 `starter-lite`
2. 新增配置中心连接配置
3. 关闭本地管理接口与本地持久化
4. 验证 Operation 调用一致性
5. 压测与回归
6. 灰度放量

## 9.3 旧能力下线条件
以下条件全部满足后，才允许下线旧模块：
- 100% 项目迁移完成
- 连续 2 个迭代未触发回滚
- 关键 SLO 稳定
- 运维与开发手册更新完成

---

## 10. 关键风险清单与应对

1. 风险：远程配置误推导致全局异常  
应对：配置发布走灰度、支持版本回滚、客户端可锁定版本。

2. 风险：模型切换导致输出质量波动  
应对：Operation 级灰度 + A/B 对比 + 质量评分回收。

3. 风险：日志系统背压影响业务  
应对：日志链路与业务线程彻底解耦，队列限流与丢弃策略。

4. 风险：依赖拆分后隐式兼容破坏  
应对：保留 `legacy-starter` 过渡期，发布迁移指南与 compatibility test。

5. 风险：多租户鉴权与数据隔离缺陷  
应对：服务端每请求强校验 `tenantId/appId/apiKey`，数据库按租户分区或逻辑隔离。

---

## 11. 每阶段“完成定义”（DoD）

每个 Phase 结束必须满足：
1. 代码合并 + tag 发布
2. 文档更新（配置说明、迁移说明、回滚说明）
3. 回归测试通过
4. 性能对比报告
5. 风险复盘记录

建议输出路径：
- `/Users/suifeng/Code/SF-Chain/docs/refactor/reports/phase-X-report.md`

---

## 12. 建议的执行节奏（周计划示例）

- Week 1：Phase 0 全部完成
- Week 2~3：Phase 1
- Week 4~5：Phase 2
- Week 6~8：Phase 3
- Week 9~10：Phase 4
- Week 11~14：Phase 5

每周固定节奏：
- 周一：需求冻结 + 任务拆分
- 周三：中期检查 + 风险扫描
- 周五：阶段验收 + 决策是否推进

---

## 13. 你可以马上执行的第一批任务（本周）

1. 在 `SF-Chain` 建立 `docs/refactor/` 文档骨架与基线清单。  
2. 增加 feature flags（management/persistence/migration/ui）并默认关闭。  
3. 移除全量 `@ComponentScan`，改为显式自动装配（先保证行为等价）。  
4. 补齐最小回归测试集（至少覆盖 6 个关键场景）。  
5. 产出第一份“依赖树减负对比报告”。

---

## 14. 最终目标状态（验收口径）

达到以下状态即视为重构成功：
- 使用方仅依赖 `starter-lite` 就能开发/运行 Operation。
- 配置中心可统一管理模型与 Operation 配置，并支持热更新。
- AI 日志具备异步汇聚能力，不显著增加业务延迟。
- 旧版能力已平滑下线或保留兼容层，不影响历史项目稳定运行。

