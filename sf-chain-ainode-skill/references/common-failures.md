# Common Failures & Fix

## 1. 模板变量未替换（原样输出 {{ ... }}）

常见原因：
- 表达式写错（字段名、函数名）
- 使用了不存在的 `input.xxx`
- 远程模板关闭严格渲染导致错误被吞

修复：
- 对照 DTO + `buildPromptInputExtensions` 校对字段
- 在配置中心开启 `promptStrictRender`
- 优先使用 `fn.defaultValue` 处理可空字段

## 2. JSON 解析失败

常见原因：
- `requireJsonOutput=true`，但模板未强约束 JSON 输出
- 示例 JSON 与 DTO 字段不一致

修复：
- 模板末尾给出唯一 JSON 输出协议
- DTO 字段名与模板示例完全一致

## 3. 远程模板覆盖后行为异常

常见原因：
- promptMode 被改为 `TEMPLATE_OVERRIDE`
- 远程模板缺字段或语法错误

修复：
- 先在配置中心模板预览
- 回退到本地模板（`LOCAL_ONLY`）
- 打开严格渲染定位具体表达式

## 4. 扩展字段不生效

常见原因：
- `buildPromptInputExtensions` 返回字段与 input 原字段同名
- 合并策略 `putIfAbsent` 导致未覆盖

修复：
- 改扩展字段名
- 不依赖覆盖原字段，使用独立派生字段

## 5. 模型参数不符合预期

原因：
- 运行时会合并 registry config 与 `@AIOp` 默认值

修复：
- 同时检查：
  - `@AIOp` 注解参数
  - 配置中心 operation config
  - 节点模型映射
