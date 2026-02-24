# Runtime Behavior（基于 SF-Chain 源码）

## 1. Operation 生命周期

1. Spring 初始化 Operation Bean
2. `BaseAIOperation.init()` 读取 `@AIOp` 并注册到 `AIOperationRegistry`
3. 如果 `defaultModel` 存在且当前未映射，会自动绑定默认模型

## 2. 执行链路

`execute()` / `executeStream()` 关键步骤：
1. 检查节点是否启用（registry config）
2. 解析模型（入参 modelName 或映射）
3. 构建 prompt（本地模板或远程模板）
4. 合并参数：`maxTokens/temperature/jsonOutput/thinking`
5. 调模型生成
6. 解析响应（String 直出或 JSON 解析）
7. 记录 AI 调用日志

## 3. Prompt 解析逻辑

- 本地模板来源：`promptTemplate()`
- 远程模板来源：`OperationConfig.promptTemplate`
- 模式字段：`OperationConfig.promptMode`
  - `LOCAL_ONLY`
  - `TEMPLATE_OVERRIDE`
- 严格渲染字段：`OperationConfig.promptStrictRender`

## 4. input 扩展字段

- 方法：`buildPromptInputExtensions(INPUT input)`
- 合并行为：合并到 `input` 且不覆盖已有字段（`putIfAbsent`）
- 建议：
  - 扩展字段使用语义化命名
  - 避免和 DTO 原字段重名

## 5. 配置中心联动

- 启动会进行 operation catalog sync
- catalog payload 包含：
  - `operationType`
  - `description/defaultModel/enabled/...`
  - `localPromptTemplate`
  - `localPromptTemplateChecksum`
- 作用：配置中心可展示本地模板并管理远程覆盖
