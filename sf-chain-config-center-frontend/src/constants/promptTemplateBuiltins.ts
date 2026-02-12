export interface PromptFunctionDoc {
  name: string
  signature: string
  description: string
}

// Must stay aligned with PromptTemplateEngine.PromptFunctions in sf-chain-core.
export const PROMPT_TEMPLATE_FUNCTIONS: PromptFunctionDoc[] = [
  { name: 'defaultValue', signature: 'fn.defaultValue(value, fallback)', description: 'value为空时返回fallback' },
  { name: 'coalesce', signature: 'fn.coalesce(v1, v2, ...)', description: '返回第一个非空值' },
  { name: 'blank', signature: 'fn.blank(value)', description: '判断是否为空（null/空字符串/空集合）' },
  { name: 'present', signature: 'fn.present(value)', description: '是否非空' },
  { name: 'len', signature: 'fn.len(value)', description: '返回长度（字符串/集合/Map/数组）' },
  { name: 'join', signature: 'fn.join(value, separator)', description: '拼接集合或数组' },
  { name: 'get', signature: "fn.get(root, \"path.to[0].value\")", description: '按路径读取值' },
  { name: 'replace', signature: 'fn.replace(text, target, replacement)', description: '字符串替换' },
  { name: 'substring', signature: 'fn.substring(text, begin, endExclusive)', description: '字符串截取' },
  { name: 'toInt', signature: 'fn.toInt(value, defaultValue)', description: '转Int' },
  { name: 'toLong', signature: 'fn.toLong(value, defaultValue)', description: '转Long' },
  { name: 'toDouble', signature: 'fn.toDouble(value, defaultValue)', description: '转Double' },
  { name: 'toBoolean', signature: 'fn.toBoolean(value, defaultValue)', description: '转Boolean' },
  { name: 'now', signature: "fn.now('yyyy-MM-dd HH:mm:ss')", description: '当前时间格式化字符串' },
  { name: 'nowMillis', signature: 'fn.nowMillis()', description: '当前毫秒时间戳' },
  { name: 'nowSeconds', signature: 'fn.nowSeconds()', description: '当前秒时间戳' },
  { name: 'formatDate', signature: "fn.formatDate(value, 'yyyy-MM-dd')", description: '格式化时间' },
  { name: 'dateAdd', signature: "fn.dateAdd(value, 3, 'days', 'yyyy-MM-dd')", description: '日期增加' },
  { name: 'dateSub', signature: "fn.dateSub(value, 3, 'days', 'yyyy-MM-dd')", description: '日期减少' },
  { name: 'json', signature: 'fn.json(value)', description: '对象转JSON字符串' },
  { name: 'trim', signature: 'fn.trim(value)', description: '去除首尾空白' },
  { name: 'upper', signature: 'fn.upper(value)', description: '转大写' },
  { name: 'lower', signature: 'fn.lower(value)', description: '转小写' }
]

export const PROMPT_TEMPLATE_FUNCTION_NAMES = PROMPT_TEMPLATE_FUNCTIONS.map((item) => item.name)
