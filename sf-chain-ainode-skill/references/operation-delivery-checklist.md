# Operation Delivery Checklist

## A. 编码完成前

- [ ] `@AIOp.value` 已定义常量且唯一
- [ ] `promptTemplate()` 已实现且模板完整
- [ ] `requireJsonOutput` 与输出类型一致
- [ ] 必要输入字段已校验

## B. 模板校验

- [ ] 每个 `input.xxx` 均可解析
- [ ] 每个 `fn.xxx` 为引擎支持函数
- [ ] `if/each` 块闭合正确
- [ ] 远程模板也能通过预览

## C. 运行校验

- [ ] 正常样例可执行
- [ ] 边界样例可执行（空值/极值）
- [ ] 日志中能看到 operationType/modelName/duration
- [ ] 输出可被业务层正确消费

## D. 配置中心联动

- [ ] operation catalog 同步成功
- [ ] localPromptTemplate 已上传
- [ ] 远程覆盖可切换并生效
- [ ] strictRender 策略符合预期
