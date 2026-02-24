---
name: sf-chain-ainode-builder
description: 编写或重构 SF-Chain 的 AI Operation 节点。用于新增/修改 BaseAIOperation 子类、定义输入输出 DTO、编写提示词模板、接入配置中心远程模板覆盖、排查模板渲染与输出解析问题。
---

# SF-Chain AI Node Builder

按下面流程执行，不跳步。

## 1. 输入确认（最小集）

先确认：
- `operationType`（唯一）
- INPUT DTO（必填、可选、默认值）
- OUTPUT DTO（字段和类型）
- `@AIOp.requireJsonOutput`（true/false）
- 默认模型和参数（maxTokens、temperature、thinking）

如果缺信息，先追问 1-3 个关键字段，再编码。

## 2. 强约束实现规范

- 必须继承：`BaseAIOperation<INPUT, OUTPUT>`
- 必须标注：`@Component` + `@AIOp(...)`
- 必须实现：`promptTemplate()`
- 仅在需要派生字段时实现：`buildPromptInputExtensions(INPUT input)`
- 禁止使用旧式 `buildPrompt(INPUT)` 手工拼接
- 模板变量统一用 `input`（不引入 `ctx`）

## 3. 与框架源码一致的关键行为

### 3.1 提示词来源
- `LOCAL_ONLY`：使用 `promptTemplate()` 本地模板
- `TEMPLATE_OVERRIDE`：优先用配置中心模板（`promptTemplate`）
- 远程模板为空会直接失败（非降级）

### 3.2 严格渲染
- 本地模板渲染走严格模式（字段/表达式异常直接报错）
- 远程模板是否严格由 `promptStrictRender` 控制

### 3.3 input 扩展合并规则
- `buildPromptInputExtensions` 返回值会合并到模板 `input`
- 合并使用 `putIfAbsent`：不会覆盖原始 input 同名字段
- 结论：扩展字段命名不要和 DTO 字段重名

### 3.4 配置中心同步
- 启动时会同步 Operation catalog
- catalog 会带 `localPromptTemplate` + checksum
- 这用于配置中心展示本地模板与远程覆盖管理

## 4. 模板语法

可用对象：
- `input.xxx`
- `fn.xxx(...)`
- `operationType`
- `localPrompt`（远程模板调试）

支持块：
- `{{#if expr}}...{{else}}...{{/if}}`
- `{{#each input.list}}...{{ item }}...{{/each}}`

常用函数：
- 空值：`defaultValue` `coalesce` `blank` `present`
- 集合：`len` `join` `get`
- 类型：`toInt` `toLong` `toDouble` `toBoolean`
- 时间：`now` `nowMillis` `nowSeconds` `formatDate` `dateAdd` `dateSub`
- 文本：`trim` `upper` `lower` `replace` `substring`
- JSON：`json`

## 5. 标准骨架

```java
@Component
@AIOp(
    value = AINodeConstant.XXX_OP,
    description = "节点用途说明",
    requireJsonOutput = true
)
public class XxxOperation extends BaseAIOperation<XxxInput, XxxOutput> {

    private static final String LOCAL_PROMPT_TEMPLATE = """
            任务描述...
            输入：{{ fn.defaultValue(input.field, '') }}
            输出 JSON：{ "result": "..." }
            """;

    @Override
    public String promptTemplate() {
        return LOCAL_PROMPT_TEMPLATE;
    }

    @Override
    protected Map<String, Object> buildPromptInputExtensions(XxxInput input) {
        return Map.of();
    }
}
```

## 6. 交付门禁

交付前必须满足：
- `operationType` 唯一
- 模板字段全部可解析（DTO 或 extensions）
- 输出 JSON 与 OUTPUT DTO 完全一致
- 至少 1 个正常样例 + 1 个边界样例
- 无冗余方法、无无用兼容分支

## 7. 参考资料加载顺序（按需）

1. `references/operation-authoring-standard.md`
2. `references/runtime-behavior.md`
3. `references/common-failures.md`
4. `references/template-language-reference.md`
5. `references/framework/` + `references/real-nodes/`
