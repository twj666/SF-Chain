# Prompt 模板化改造（Operation 保持签名不变）

本文档用于落地 SF-Chain 模板化能力：在不改 Operation 输入输出签名的前提下，启用配置中心可编辑的远程提示词模板。

Operation 若需要在运行时补充模板变量，请重写：

- `buildPromptInputExtensions(input)`：返回要追加到 `input` 的字段（同名不覆盖）

## 1. 运行时规则

Operation 配置新增字段：

- `promptMode`：`LOCAL_ONLY` / `TEMPLATE_OVERRIDE`
- `promptTemplate`：远程模板正文
- `promptStrictRender`：是否严格渲染

执行规则：

1. `LOCAL_ONLY`：始终使用本地 `buildPrompt(input)`。
2. `TEMPLATE_OVERRIDE`：
   - 模板渲染成功：使用远程模板结果。
   - `promptStrictRender=false`：表达式失败时保留原占位符文本，继续执行。
   - `promptStrictRender=true`：表达式失败直接抛错，阻止本次调用。

## 2. 模板变量

支持以下变量（Mustache 风格）：

- `{{ input.xxx }}`：读取 Operation 输入对象字段
- `{{ localPrompt }}`：本地 `buildPrompt(input)` 结果
- `{{ operationType }}`：当前操作类型
- `{{ fn.xxx(...) }}`：调用模板内置函数

支持结构语法：

- `{{#if condition}} ... {{else}} ... {{/if}}`
- `{{#each list}} ... {{/each}}`，循环内可用 `{{ item }}`、`{{ this }}`、`{{ index }}`

内置函数（常用）：

- `fn.defaultValue(value, fallback)` / `fn.coalesce(a,b,c...)`
- `fn.get(obj, "a.b[0].c")`（支持对象路径访问）
- `fn.join(list, sep)` / `fn.len(value)` / `fn.json(value)`
- `fn.replace(text, target, replacement)` / `fn.substring(text, start, endExclusive)`
- `fn.toInt(x, default)` / `fn.toLong(x, default)` / `fn.toDouble(x, default)` / `fn.toBoolean(x, default)`
- `fn.now("yyyy-MM-dd HH:mm:ss")` / `fn.nowMillis()` / `fn.nowSeconds()`
- `fn.formatDate(value, "yyyy-MM-dd")` / `fn.dateAdd(value, 3, "days", "yyyy-MM-dd")` / `fn.dateSub(value, 2, "hours", "yyyy-MM-dd HH:mm")`

示例：

```text
主题：{{ fn.defaultValue(input.topic, '未提供') }}
操作：{{ operationType }}
本地兜底提示词：
{{ localPrompt }}
```

## 3. ai-poet 三个核心 Operation 示例模板

### 3.1 `CONTENT_MODIFICATION_OP`

```text
你是一个专业的文档编辑助手，擅长根据批注内容精准修改文档。

原始文档：
<原文开始>
{{ input.originalText }}
<原文结束>

批注内容：{{ input.annotationContent }}
选中文本：{{ input.selectedText }}
保持原文风格：{{ input.keepOriginalStyle }}

请返回 JSON：
{
  "reason": "本次修改的总体原因和目标",
  "changes": [
    {
      "line": "行号",
      "content": "该行修改后的完整内容"
    }
  ]
}
```

### 3.2 `COPYWRITING_GENERATION`

```text
你是一位擅长抖音科普的内容创作者，请围绕主题生成口播文案。

主题：{{ input.topic }}
项目描述：{{ input.description }}
参考素材：{{ input.materials }}
目标字数：{{ input.wordLimit }}
额外要求：{{ input.additionalRequirements }}

输出要求：
1. 只输出正文，不要解释过程
2. 语言口语化、节奏紧凑
3. 保持信息准确
```

### 3.3 `ANNOTATION_ANALYSIS_OP`

```text
你是一位资深内容策划师，请分析文案并给出可执行优化建议。

平台风格：{{ input.contentStyle }}
分析类型：{{ input.analysisType }}
分析深度：{{ input.analysisDepth }}

待分析文案：
{{ input.content }}

返回 JSON：
{
  "overallScore": 85,
  "analysisSummary": "整体评价",
  "suggestions": [
    {
      "selectedText": "原文片段",
      "content": "具体建议",
      "type": "OPTIMIZATION",
      "severity": "MEDIUM",
      "color": "#4ECDC4"
    }
  ]
}
```

## 4. 推荐上线步骤

1. 在配置中心将目标 Operation 切换为：
   - `promptMode=TEMPLATE_OVERRIDE`
   - `promptStrictRender=false`
   - 填入模板内容
2. 观察租户日志确认生效（模板渲染成功/失败回退）。
3. 模板稳定后，将 `promptStrictRender` 改为 `true`，进入严格模式。

## 5. 回滚策略

任意时刻可将 `promptMode` 改回 `LOCAL_ONLY`，立即恢复原有本地 `buildPrompt(input)` 行为。
