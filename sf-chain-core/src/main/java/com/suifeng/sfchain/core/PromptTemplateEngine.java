package com.suifeng.sfchain.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PromptTemplateEngine {
    private static final Pattern TEMPLATE_PLACEHOLDER = Pattern.compile("\\{\\{\\s*(.+?)\\s*}}");
    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();
    private static final String BLOCK_IF_OPEN = "{{#if ";
    private static final String BLOCK_EACH_OPEN = "{{#each ";
    private static final String BLOCK_IF_CLOSE = "{{/if}}";
    private static final String BLOCK_EACH_CLOSE = "{{/each}}";
    private static final String BLOCK_ELSE = "{{else}}";
    private final ObjectMapper objectMapper;

    public PromptTemplateEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String render(String template, Map<String, Object> context, boolean strictRender) {
        Map<String, Object> runtimeContext = new HashMap<>(context == null ? Map.of() : context);
        runtimeContext.putIfAbsent("fn", new PromptFunctions(objectMapper));
        return renderTemplateBlocks(template == null ? "" : template, runtimeContext, strictRender);
    }

    private String renderTemplateBlocks(String template, Map<String, Object> context, boolean strictRender) {
        StringBuilder output = new StringBuilder();
        int cursor = 0;
        while (cursor < template.length()) {
            int blockStart = findNextBlockStart(template, cursor);
            if (blockStart < 0) {
                output.append(renderTemplateExpressions(template.substring(cursor), context, strictRender));
                break;
            }
            output.append(renderTemplateExpressions(template.substring(cursor, blockStart), context, strictRender));
            BlockHeader header = parseBlockHeader(template, blockStart);
            BlockMatch match = findBlockMatch(template, header);
            String renderedBlock = renderBlock(header, match, context, strictRender);
            output.append(renderedBlock);
            cursor = match.closeEnd;
        }
        return output.toString();
    }

    private String renderBlock(BlockHeader header, BlockMatch match, Map<String, Object> context, boolean strictRender) {
        Object evaluated = evaluateExpression(context, header.expression, strictRender);
        if ("if".equals(header.type)) {
            boolean condition = toBoolean(evaluated);
            String selected = condition ? match.trueContent : match.falseContent;
            return renderTemplateBlocks(selected, context, strictRender);
        }
        List<?> items = toIterable(evaluated);
        if (items.isEmpty()) {
            return "";
        }
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            Map<String, Object> child = new HashMap<>(context);
            child.put("item", item);
            child.put("this", item);
            child.put("index", i);
            output.append(renderTemplateBlocks(match.trueContent, child, strictRender));
        }
        return output.toString();
    }

    private String renderTemplateExpressions(String text, Map<String, Object> context, boolean strictRender) {
        EvaluationContext evaluationContext = createEvaluationContext(context);
        Matcher matcher = TEMPLATE_PLACEHOLDER.matcher(text);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            if (expression.startsWith("#") || expression.startsWith("/") || "else".equals(expression)) {
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            Object value = evaluateExpression(evaluationContext, expression, strictRender);
            if (value == null) {
                if (strictRender) {
                    throw new TemplateRenderException("EXPRESSION_NULL", expression, "模板表达式结果为空: " + expression);
                }
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private int findNextBlockStart(String template, int from) {
        int ifIndex = template.indexOf(BLOCK_IF_OPEN, from);
        int eachIndex = template.indexOf(BLOCK_EACH_OPEN, from);
        if (ifIndex < 0) {
            return eachIndex;
        }
        if (eachIndex < 0) {
            return ifIndex;
        }
        return Math.min(ifIndex, eachIndex);
    }

    private BlockHeader parseBlockHeader(String template, int start) {
        if (template.startsWith(BLOCK_IF_OPEN, start)) {
            int close = template.indexOf("}}", start);
            if (close < 0) {
                throw new IllegalArgumentException("模板语法错误: if 块未闭合");
            }
            String expression = template.substring(start + BLOCK_IF_OPEN.length(), close).trim();
            return new BlockHeader("if", expression, close + 2);
        }
        if (template.startsWith(BLOCK_EACH_OPEN, start)) {
            int close = template.indexOf("}}", start);
            if (close < 0) {
                throw new IllegalArgumentException("模板语法错误: each 块未闭合");
            }
            String expression = template.substring(start + BLOCK_EACH_OPEN.length(), close).trim();
            return new BlockHeader("each", expression, close + 2);
        }
        throw new IllegalArgumentException("模板语法错误: 未知块起始");
    }

    private BlockMatch findBlockMatch(String template, BlockHeader header) {
        int cursor = header.openEnd;
        List<String> stack = new ArrayList<>();
        stack.add(header.type);
        int elseStart = -1;
        int elseEnd = -1;
        while (cursor < template.length()) {
            int tokenIndex = findNextTokenIndex(template, cursor);
            if (tokenIndex < 0) {
                break;
            }
            if (template.startsWith(BLOCK_IF_OPEN, tokenIndex)) {
                stack.add("if");
                cursor = tokenEnd(template, tokenIndex, "if");
                continue;
            }
            if (template.startsWith(BLOCK_EACH_OPEN, tokenIndex)) {
                stack.add("each");
                cursor = tokenEnd(template, tokenIndex, "each");
                continue;
            }
            if (template.startsWith(BLOCK_ELSE, tokenIndex)) {
                if ("if".equals(header.type) && stack.size() == 1 && elseStart < 0) {
                    elseStart = tokenIndex;
                    elseEnd = tokenIndex + BLOCK_ELSE.length();
                }
                cursor = tokenIndex + BLOCK_ELSE.length();
                continue;
            }
            if (template.startsWith(BLOCK_IF_CLOSE, tokenIndex)) {
                cursor = tokenIndex + BLOCK_IF_CLOSE.length();
                if (!"if".equals(stack.get(stack.size() - 1))) {
                    throw new IllegalArgumentException("模板语法错误: if 块闭合不匹配");
                }
                stack.remove(stack.size() - 1);
                if (stack.isEmpty()) {
                    String trueContent = elseStart >= 0
                            ? template.substring(header.openEnd, elseStart)
                            : template.substring(header.openEnd, tokenIndex);
                    String falseContent = elseStart >= 0
                            ? template.substring(elseEnd, tokenIndex)
                            : "";
                    return new BlockMatch(trueContent, falseContent, cursor);
                }
                continue;
            }
            if (template.startsWith(BLOCK_EACH_CLOSE, tokenIndex)) {
                cursor = tokenIndex + BLOCK_EACH_CLOSE.length();
                if (!"each".equals(stack.get(stack.size() - 1))) {
                    throw new IllegalArgumentException("模板语法错误: each 块闭合不匹配");
                }
                stack.remove(stack.size() - 1);
                if (stack.isEmpty()) {
                    return new BlockMatch(template.substring(header.openEnd, tokenIndex), "", cursor);
                }
            }
        }
        throw new IllegalArgumentException("模板语法错误: 块未正确闭合, type=" + header.type);
    }

    private int tokenEnd(String template, int tokenIndex, String blockType) {
        int close = template.indexOf("}}", tokenIndex);
        if (close < 0) {
            throw new IllegalArgumentException("模板语法错误: " + blockType + " 块未闭合");
        }
        return close + 2;
    }

    private int findNextTokenIndex(String template, int from) {
        int min = -1;
        String[] tokens = new String[]{BLOCK_IF_OPEN, BLOCK_EACH_OPEN, BLOCK_IF_CLOSE, BLOCK_EACH_CLOSE, BLOCK_ELSE};
        for (String token : tokens) {
            int idx = template.indexOf(token, from);
            if (idx >= 0 && (min < 0 || idx < min)) {
                min = idx;
            }
        }
        return min;
    }

    private EvaluationContext createEvaluationContext(Map<String, Object> context) {
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext(context);
        evaluationContext.addPropertyAccessor(new MapAccessor());
        if (context != null) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                evaluationContext.setVariable(entry.getKey(), entry.getValue());
            }
        }
        return evaluationContext;
    }

    private Object evaluateExpression(Map<String, Object> context, String expression, boolean strictRender) {
        return evaluateExpression(createEvaluationContext(context), expression, strictRender);
    }

    private Object evaluateExpression(EvaluationContext evaluationContext, String expression, boolean strictRender) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }
        String normalized = expression.trim();
        try {
            return EXPRESSION_PARSER.parseExpression(normalized).getValue(evaluationContext);
        } catch (Exception ex) {
            if (normalized.contains("fn.")) {
                String fallbackExpr = normalized.replaceAll("\\bfn\\.", "#fn.");
                try {
                    return EXPRESSION_PARSER.parseExpression(fallbackExpr).getValue(evaluationContext);
                } catch (Exception ignored) {
                    // keep original exception behavior below
                }
            }
            if (!strictRender) {
                return null;
            }
            throw new TemplateRenderException(
                    "EXPRESSION_PARSE_ERROR",
                    expression,
                    "模板表达式解析失败: " + expression + ", err=" + ex.getMessage(),
                    ex);
        }
    }

    private boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }
        if (value instanceof String) {
            String s = ((String) value).trim();
            return !s.isEmpty() && !"false".equalsIgnoreCase(s) && !"0".equals(s);
        }
        if (value instanceof Collection) {
            return !((Collection<?>) value).isEmpty();
        }
        return true;
    }

    private List<?> toIterable(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List) {
            return (List<?>) value;
        }
        if (value instanceof Collection) {
            return new ArrayList<>((Collection<?>) value);
        }
        if (value instanceof Iterable) {
            List<Object> list = new ArrayList<>();
            Iterator<?> iterator = ((Iterable<?>) value).iterator();
            while (iterator.hasNext()) {
                list.add(iterator.next());
            }
            return list;
        }
        if (value instanceof Map) {
            return new ArrayList<>(((Map<?, ?>) value).entrySet());
        }
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            List<Object> list = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                list.add(Array.get(value, i));
            }
            return list;
        }
        return Collections.singletonList(value);
    }

    private static final class BlockHeader {
        private final String type;
        private final String expression;
        private final int openEnd;

        private BlockHeader(String type, String expression, int openEnd) {
            this.type = type;
            this.expression = expression;
            this.openEnd = openEnd;
        }
    }

    private static final class BlockMatch {
        private final String trueContent;
        private final String falseContent;
        private final int closeEnd;

        private BlockMatch(String trueContent, String falseContent, int closeEnd) {
            this.trueContent = trueContent;
            this.falseContent = falseContent;
            this.closeEnd = closeEnd;
        }
    }

    public static final class PromptFunctions {
        private final ObjectMapper objectMapper;

        private PromptFunctions(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        public Object defaultValue(Object value, Object fallback) {
            if (value == null) {
                return fallback;
            }
            if (value instanceof String && ((String) value).trim().isEmpty()) {
                return fallback;
            }
            return value;
        }

        public Object coalesce(Object... values) {
            if (values == null || values.length == 0) {
                return null;
            }
            for (Object value : values) {
                if (!blank(value)) {
                    return value;
                }
            }
            return null;
        }

        public boolean blank(Object value) {
            if (value == null) {
                return true;
            }
            if (value instanceof String) {
                return ((String) value).trim().isEmpty();
            }
            if (value instanceof Collection) {
                return ((Collection<?>) value).isEmpty();
            }
            if (value instanceof Map) {
                return ((Map<?, ?>) value).isEmpty();
            }
            if (value.getClass().isArray()) {
                return Array.getLength(value) == 0;
            }
            return false;
        }

        public boolean present(Object value) {
            return !blank(value);
        }

        public int len(Object value) {
            if (value == null) {
                return 0;
            }
            if (value instanceof CharSequence) {
                return ((CharSequence) value).length();
            }
            if (value instanceof Collection) {
                return ((Collection<?>) value).size();
            }
            if (value instanceof Map) {
                return ((Map<?, ?>) value).size();
            }
            if (value.getClass().isArray()) {
                return Array.getLength(value);
            }
            if (value instanceof Iterable) {
                int count = 0;
                for (Object ignored : (Iterable<?>) value) {
                    count++;
                }
                return count;
            }
            return 1;
        }

        public String join(Object value, String separator) {
            List<?> items = toList(value);
            if (items.isEmpty()) {
                return "";
            }
            StringJoiner joiner = new StringJoiner(separator == null ? "" : separator);
            items.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .forEach(joiner::add);
            return joiner.toString();
        }

        public Object get(Object root, String path) {
            if (root == null || path == null || path.trim().isEmpty()) {
                return null;
            }
            Object current = root;
            for (Object segment : parsePath(path.trim())) {
                if (current == null) {
                    return null;
                }
                if (segment instanceof Integer) {
                    current = resolveByIndex(current, (Integer) segment);
                    continue;
                }
                current = resolveByKey(current, String.valueOf(segment));
            }
            return current;
        }

        public String replace(Object value, String target, String replacement) {
            String text = value == null ? "" : String.valueOf(value);
            if (target == null || target.isEmpty()) {
                return text;
            }
            return text.replace(target, replacement == null ? "" : replacement);
        }

        public String substring(Object value, int begin, int endExclusive) {
            String text = value == null ? "" : String.valueOf(value);
            if (text.isEmpty()) {
                return "";
            }
            int start = Math.max(0, Math.min(begin, text.length()));
            int end = Math.max(start, Math.min(endExclusive, text.length()));
            return text.substring(start, end);
        }

        public Integer toInt(Object value, Integer defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (Exception ex) {
                return defaultValue;
            }
        }

        public Long toLong(Object value, Long defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            try {
                return Long.parseLong(String.valueOf(value).trim());
            } catch (Exception ex) {
                return defaultValue;
            }
        }

        public Double toDouble(Object value, Double defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            try {
                return Double.parseDouble(String.valueOf(value).trim());
            } catch (Exception ex) {
                return defaultValue;
            }
        }

        public Boolean toBoolean(Object value, Boolean defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue() != 0;
            }
            String s = String.valueOf(value).trim();
            if (s.isEmpty()) {
                return defaultValue;
            }
            if ("true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s)) {
                return true;
            }
            if ("false".equalsIgnoreCase(s) || "0".equals(s) || "no".equalsIgnoreCase(s)) {
                return false;
            }
            return defaultValue;
        }

        public String now(String pattern) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(defaultPattern(pattern));
            return LocalDateTime.now().format(formatter);
        }

        public long nowMillis() {
            return System.currentTimeMillis();
        }

        public long nowSeconds() {
            return System.currentTimeMillis() / 1000;
        }

        public String formatDate(Object value, String pattern) {
            if (value == null) {
                return "";
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(defaultPattern(pattern));
            TemporalAccessor temporal = parseTemporal(value);
            if (temporal == null) {
                return "";
            }
            if (temporal instanceof Instant) {
                return formatter.format(LocalDateTime.ofInstant((Instant) temporal, ZoneId.systemDefault()));
            }
            return formatter.format(temporal);
        }

        public String dateAdd(Object value, long amount, String unit, String pattern) {
            LocalDateTime dateTime = toDateTime(value);
            if (dateTime == null) {
                return "";
            }
            LocalDateTime shifted = shiftDateTime(dateTime, amount, unit);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(defaultPattern(pattern));
            return shifted.format(formatter);
        }

        public String dateSub(Object value, long amount, String unit, String pattern) {
            return dateAdd(value, -amount, unit, pattern);
        }

        public String json(Object value) {
            if (value == null) {
                return "null";
            }
            try {
                return objectMapper.writeValueAsString(value);
            } catch (JsonProcessingException ex) {
                throw new IllegalArgumentException("JSON序列化失败: " + ex.getMessage(), ex);
            }
        }

        public String trim(Object value) {
            return value == null ? "" : String.valueOf(value).trim();
        }

        public String upper(Object value) {
            return value == null ? "" : String.valueOf(value).toUpperCase();
        }

        public String lower(Object value) {
            return value == null ? "" : String.valueOf(value).toLowerCase();
        }

        private String defaultPattern(String pattern) {
            return pattern == null || pattern.trim().isEmpty() ? "yyyy-MM-dd HH:mm:ss" : pattern.trim();
        }

        private TemporalAccessor parseTemporal(Object value) {
            if (value instanceof TemporalAccessor) {
                return (TemporalAccessor) value;
            }
            if (value instanceof Number) {
                long epochMillis = ((Number) value).longValue();
                return Instant.ofEpochMilli(epochMillis);
            }
            String raw = String.valueOf(value).trim();
            if (raw.isEmpty()) {
                return null;
            }
            try {
                return Instant.parse(raw);
            } catch (Exception ignored) {
            }
            try {
                return LocalDateTime.parse(raw);
            } catch (Exception ignored) {
            }
            try {
                return LocalDate.parse(raw);
            } catch (Exception ignored) {
            }
            return null;
        }

        private LocalDateTime toDateTime(Object value) {
            TemporalAccessor temporal = parseTemporal(value);
            if (temporal == null) {
                return null;
            }
            if (temporal instanceof LocalDateTime) {
                return (LocalDateTime) temporal;
            }
            if (temporal instanceof LocalDate) {
                return ((LocalDate) temporal).atStartOfDay();
            }
            if (temporal instanceof Instant) {
                return LocalDateTime.ofInstant((Instant) temporal, ZoneId.systemDefault());
            }
            try {
                return LocalDateTime.from(temporal);
            } catch (Exception ex) {
                try {
                    return LocalDate.from(temporal).atTime(LocalTime.MIN);
                } catch (Exception ignore) {
                    return null;
                }
            }
        }

        private LocalDateTime shiftDateTime(LocalDateTime value, long amount, String unit) {
            String normalized = unit == null ? "DAYS" : unit.trim().toUpperCase();
            switch (normalized) {
                case "SECOND":
                case "SECONDS":
                    return value.plusSeconds(amount);
                case "MINUTE":
                case "MINUTES":
                    return value.plusMinutes(amount);
                case "HOUR":
                case "HOURS":
                    return value.plusHours(amount);
                case "WEEK":
                case "WEEKS":
                    return value.plusWeeks(amount);
                case "MONTH":
                case "MONTHS":
                    return value.plusMonths(amount);
                case "YEAR":
                case "YEARS":
                    return value.plusYears(amount);
                case "DAY":
                case "DAYS":
                default:
                    return value.plusDays(amount);
            }
        }

        private Object resolveByIndex(Object value, int index) {
            if (index < 0 || value == null) {
                return null;
            }
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                return index < list.size() ? list.get(index) : null;
            }
            if (value instanceof Collection) {
                int cursor = 0;
                for (Object item : (Collection<?>) value) {
                    if (cursor == index) {
                        return item;
                    }
                    cursor++;
                }
                return null;
            }
            if (value.getClass().isArray()) {
                return index < Array.getLength(value) ? Array.get(value, index) : null;
            }
            return null;
        }

        private Object resolveByKey(Object value, String key) {
            if (value == null || key == null) {
                return null;
            }
            if (value instanceof Map) {
                return ((Map<?, ?>) value).get(key);
            }
            Map<String, Object> asMap = objectMapper.convertValue(value, Map.class);
            return asMap.get(key);
        }

        private List<Object> parsePath(String path) {
            List<Object> segments = new ArrayList<>();
            StringBuilder token = new StringBuilder();
            for (int i = 0; i < path.length(); i++) {
                char c = path.charAt(i);
                if (c == '.') {
                    if (token.length() > 0) {
                        segments.add(token.toString());
                        token.setLength(0);
                    }
                    continue;
                }
                if (c == '[') {
                    if (token.length() > 0) {
                        segments.add(token.toString());
                        token.setLength(0);
                    }
                    int close = path.indexOf(']', i + 1);
                    if (close < 0) {
                        throw new IllegalArgumentException("路径语法错误: " + path);
                    }
                    String inner = path.substring(i + 1, close).trim();
                    if (inner.startsWith("'") && inner.endsWith("'") && inner.length() >= 2) {
                        segments.add(inner.substring(1, inner.length() - 1));
                    } else if (inner.startsWith("\"") && inner.endsWith("\"") && inner.length() >= 2) {
                        segments.add(inner.substring(1, inner.length() - 1));
                    } else {
                        segments.add(Integer.parseInt(inner));
                    }
                    i = close;
                    continue;
                }
                token.append(c);
            }
            if (token.length() > 0) {
                segments.add(token.toString());
            }
            return segments;
        }

        private List<?> toList(Object value) {
            if (value == null) {
                return List.of();
            }
            if (value instanceof List) {
                return (List<?>) value;
            }
            if (value instanceof Collection) {
                return new ArrayList<>((Collection<?>) value);
            }
            if (value instanceof Iterable) {
                List<Object> list = new ArrayList<>();
                for (Object item : (Iterable<?>) value) {
                    list.add(item);
                }
                return list;
            }
            if (value instanceof Map) {
                return new ArrayList<>(((Map<?, ?>) value).entrySet());
            }
            if (value.getClass().isArray()) {
                int len = Array.getLength(value);
                List<Object> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    list.add(Array.get(value, i));
                }
                return list;
            }
            return List.of(value);
        }
    }

    public static final class TemplateRenderException extends RuntimeException {
        private final String errorType;
        private final String expression;

        public TemplateRenderException(String errorType, String expression, String message) {
            super(message);
            this.errorType = errorType;
            this.expression = expression;
        }

        public TemplateRenderException(String errorType, String expression, String message, Throwable cause) {
            super(message, cause);
            this.errorType = errorType;
            this.expression = expression;
        }

        public String getErrorType() {
            return errorType;
        }

        public String getExpression() {
            return expression;
        }
    }
}
