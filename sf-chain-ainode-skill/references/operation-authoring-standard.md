# SF-Chain Operation 编写标准

## 1. 设计目标

Operation 要做到：
- 可直接运行（本地模板可独立渲染）
- 可被配置中心覆盖（远程模板可替换）
- 可维护（模板结构清晰、字段命名稳定）

## 2. 实现步骤

1. 定义 operation 常量。
2. 定义 INPUT/OUTPUT DTO（先定输出结构再反推模板）。
3. 创建 `BaseAIOperation` 子类并加 `@AIOp`。
4. 编写 `LOCAL_PROMPT_TEMPLATE`。
5. 如有派生字段，实现 `buildPromptInputExtensions`。
6. 用真实输入进行模板渲染验证。

## 3. AIOp 配置建议

- `value`：必填，且全局唯一。
- `description`：简洁说明用途。
- `requireJsonOutput`：输出为对象时设为 `true`。
- `defaultMaxTokens`、`defaultTemperature`：按任务复杂度给默认值。
- `supportThinking`：仅在模型与任务都需要时开启。

## 4. 常见问题

- 模板字段丢失：`input.xxx` 未出现在 DTO 或扩展字段里。
- 输出解析失败：提示词中声明的 JSON 与 DTO 不一致。
- 模板不可维护：把大量业务判断写进模板表达式，建议下沉到 `buildPromptInputExtensions`。

## 5. 推荐实践

- 模板里主要写任务语义与输出约束。
- Java 里主要做输入清洗、枚举归一化、默认值补齐。
- 模板中的示例 JSON 尽量最小闭环，避免歧义。
