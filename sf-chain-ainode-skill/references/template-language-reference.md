# SF-Chain 模板语法参考

## 1. 变量

- `{{ input.field }}`
- `{{ operationType }}`
- `{{ fn.defaultValue(input.topic, '未命名主题') }}`

## 2. 条件块

```text
{{#if fn.present(input.description)}}
描述：{{ input.description }}
{{else}}
描述为空
{{/if}}
```

## 3. 循环块

```text
{{#each input.materialLines}}
- {{ item }}
{{/each}}
```

## 4. 常用函数

- 空值：`defaultValue` `coalesce` `blank` `present`
- 集合：`len` `join` `get`
- 类型：`toInt` `toLong` `toDouble` `toBoolean`
- 时间：`now` `nowMillis` `nowSeconds` `formatDate` `dateAdd` `dateSub`
- 文本：`trim` `upper` `lower` `replace` `substring`
- JSON：`json`

## 5. 严格渲染建议

- 生产模板按严格渲染思维编写。
- 表达式出错要视为模板缺陷，不依赖吞错回退。
