package com.sfchain.core.operation;

import com.sfchain.core.AIService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * AI操作协调器抽象类
 * 用于协调多个AI操作并行执行，并提供性能监控和统计
 * @author suifeng
 * 日期: 2025/04/22
 */
@Slf4j
public abstract class AIOperationCoordinator {

    /**
     * AI服务代理
     */
    protected final AIService aiService;

    /**
     * 线程池执行器
     */
    protected final ExecutorService executor;

    /**
     * 日志格式常量 - 边框样式
     */
    private static final String MAIN_BORDER_H = "━";
    private static final String MAIN_BORDER_V = "┃";
    private static final String MAIN_BORDER_TL = "┏";
    private static final String MAIN_BORDER_TR = "┓";
    private static final String MAIN_BORDER_BL = "┗";
    private static final String MAIN_BORDER_BR = "┛";
    private static final String MAIN_BORDER_ML = "┣";
    private static final String MAIN_BORDER_MR = "┫";

    private static final String SUB_BORDER_H = "─";
    private static final String SUB_BORDER_V = "│";
    private static final String SUB_BORDER_TL = "╭";
    private static final String SUB_BORDER_TR = "╮";
    private static final String SUB_BORDER_BL = "╰";
    private static final String SUB_BORDER_BR = "╯";

    /**
     * 日志格式常量 - 颜色代码（ANSI）
     */
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_PURPLE = "\u001B[35m";

    /**
     * 日志格式常量 - 图标
     */
    private static final String ICON_START = "🚀";
    private static final String ICON_COMPLETE = "✅";
    private static final String ICON_FAILED = "❌";
    private static final String ICON_PROCESSING = "⏳";
    private static final String ICON_STATS = "📊";
    private static final String ICON_TIME = "⏱️";
    private static final String ICON_MERGE = "🔄";
    private static final String ICON_TASK = "▶️";
    private static final String ICON_DETAIL = "•";

    /**
     * 日志格式常量 - 宽度设置
     */
    private static final int MAIN_BOX_WIDTH = 110;  // 主框宽度
    private static final int SUB_BOX_WIDTH = 106;   // 子框宽度
    private static final int TASK_NAME_WIDTH = 12;  // 任务名宽度
    private static final int OPERATION_NAME_WIDTH = 25; // 操作名宽度
    private static final int MODEL_NAME_WIDTH = 25; // 模型名宽度
    private static final int DURATION_WIDTH = 10;   // 耗时宽度
    private static final int NUMBER_WIDTH = 10;     // 数字宽度

    /**
     * 表格列宽度预设
     */
    private static final int[] TABLE_COLUMN_WIDTHS = {
            10,  // 任务名称
            10,  // 耗时
            25,  // 模型
            8,  // 输入字符
            8,  // 输出字符
            8,   // 条目数
            15   // 吞吐率
    };

    /**
     * 构造函数
     * @param aiService AI服务代理
     */
    public AIOperationCoordinator(AIService aiService) {
        this.aiService = aiService;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setName("ai-operation-executor-" + thread.getId());
            return thread;
        });
    }

    public abstract String getName();

    /**
     * 构造函数
     * @param aiService AI服务代理
     * @param threadPoolSize 线程池大小
     */
    public AIOperationCoordinator(AIService aiService, int threadPoolSize) {
        this.aiService = aiService;
        this.executor = Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread thread = new Thread(r);
            thread.setName("ai-operation-executor-" + thread.getId());
            return thread;
        });
    }

    /**
     * 定义任务配置类
     */
    protected static class TaskConfig<T> {
        private final String operationName;
        private final String modelName;
        private final Function<Map<String, Object>, T> fallbackFunction;

        public TaskConfig(String operationName, String modelName, Function<Map<String, Object>, T> fallbackFunction) {
            this.operationName = operationName;
            this.modelName = modelName;
            this.fallbackFunction = fallbackFunction;
        }

        public String getOperationName() {
            return operationName;
        }

        public String getModelName() {
            return modelName;
        }

        public Function<Map<String, Object>, T> getFallbackFunction() {
            return fallbackFunction;
        }
    }

    /**
     * 执行并行任务并整合结果
     * @param params 操作参数
     * @param taskConfigs 任务配置映射表
     * @param resultProcessor 结果处理函数
     * @param <R> 返回结果类型
     * @return 处理结果
     */
    protected <R> R executeParallelTasks(Map<String, Object> params,
                                         Map<String, TaskConfig<?>> taskConfigs,
                                         Function<Map<String, Object>, R> resultProcessor) {
        // 记录总体开始时间
        long totalStartTime = System.currentTimeMillis();

        // 估算输入长度
        String mainInputText = estimateInputText(params);
        int inputLength = mainInputText != null ? mainInputText.length() : 0;

        // 打印任务开始日志
        log.info(createMainBoxStart(
                String.format("%s %s正在执行%s [%s%s%s] %s并行AI操作%s | %s任务数:%s %d | %s输入:%s %s 字符",
                        ANSI_BOLD, ANSI_BLUE, ANSI_RESET,
                        ANSI_BOLD, getName(), ANSI_RESET,
                        ANSI_BLUE, ANSI_RESET,
                        ANSI_YELLOW, ANSI_RESET, taskConfigs.size(),
                        ANSI_YELLOW, ANSI_RESET, formatNumber(inputLength)
                ),
                ICON_START
        ));

        // 创建性能统计数据结构
        Map<String, Map<String, Object>> performanceStats = new HashMap<>();
        taskConfigs.keySet().forEach(key -> performanceStats.put(key, new HashMap<>()));

        // 创建任务映射
        Map<String, CompletableFuture<Object>> taskFutures = new HashMap<>();

        // 为每个任务创建CompletableFuture
        for (Map.Entry<String, TaskConfig<?>> entry : taskConfigs.entrySet()) {
            String taskKey = entry.getKey();
            TaskConfig<?> config = entry.getValue();

            CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // 记录操作开始时间
                    long startTime = System.currentTimeMillis();
                    String operationName = config.getOperationName();
                    String modelName = config.getModelName();

                    // 打印任务开始日志
                    log.info(createSubBoxMessage(
                            String.format("%s %s开始任务%s [%s%-" + TASK_NAME_WIDTH + "s%s] | %s操作:%s %-" +
                                            OPERATION_NAME_WIDTH + "s | %s模型:%s %-" + MODEL_NAME_WIDTH + "s",
                                    ANSI_BOLD, ANSI_BLUE, ANSI_RESET,
                                    ANSI_BOLD, taskKey, ANSI_RESET,
                                    ANSI_CYAN, ANSI_RESET, operationName,
                                    ANSI_PURPLE, ANSI_RESET, modelName
                            ),
                            ICON_TASK
                    ));

                    // 执行操作
                    Object result = aiService.execute(operationName, modelName, params);

                    // 计算执行时间
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;

                    // 估算输出大小
                    String resultJson = result != null ? result.toString() : "null";
                    int outputLength = resultJson.length();

                    // 记录性能统计
                    Map<String, Object> stats = performanceStats.get(taskKey);
                    stats.put("operation", operationName);
                    stats.put("model", modelName);
                    stats.put("duration", duration);
                    stats.put("inputLength", inputLength);
                    stats.put("outputLength", outputLength);
                    stats.put("throughput", calculateThroughput(inputLength, outputLength, duration));

                    // 构建完成日志
                    StringBuilder completeMsg = new StringBuilder();
                    completeMsg.append(String.format("%s %s完成任务%s [%s%-" + TASK_NAME_WIDTH + "s%s] | ",
                            ANSI_BOLD, ANSI_GREEN, ANSI_RESET,
                            ANSI_BOLD, taskKey, ANSI_RESET));

                    // 添加耗时信息
                    completeMsg.append(String.format("%s%s%s %-" + DURATION_WIDTH + "s | ",
                            ANSI_YELLOW, ICON_TIME, ANSI_RESET, formatDuration(duration)));

                    // 添加输入输出信息
                    completeMsg.append(String.format("%s输入:%s %-" + NUMBER_WIDTH + "s | %s输出:%s %-" + NUMBER_WIDTH + "s 字符",
                            ANSI_CYAN, ANSI_RESET, formatNumber(inputLength),
                            ANSI_CYAN, ANSI_RESET, formatNumber(outputLength)));

                    // 如果结果是集合类型，添加条目数
                    if (result instanceof Collection) {
                        int itemCount = ((Collection<?>) result).size();
                        stats.put("itemCount", itemCount);
                        completeMsg.append(String.format(" | %s条目:%s %d", ANSI_CYAN, ANSI_RESET, itemCount));
                    }

                    // 打印任务完成日志
                    log.info(createSubBoxMessage(completeMsg.toString(), ICON_COMPLETE));

                    return result;
                } catch (Exception e) {
                    // 打印任务失败日志
                    log.error(createSubBoxMessage(
                            String.format("%s %s任务失败%s [%s%-" + TASK_NAME_WIDTH + "s%s] | %s操作:%s %-" +
                                            OPERATION_NAME_WIDTH + "s | %s错误:%s %s",
                                    ANSI_BOLD, ANSI_RED, ANSI_RESET,
                                    ANSI_BOLD, taskKey, ANSI_RESET,
                                    ANSI_CYAN, ANSI_RESET, config.getOperationName(),
                                    ANSI_RED, ANSI_RESET, e.getMessage()
                            ),
                            ICON_FAILED
                    ));

                    // 记录错误信息
                    Map<String, Object> stats = performanceStats.get(taskKey);
                    stats.put("operation", config.getOperationName());
                    stats.put("model", config.getModelName());
                    stats.put("status", "failed");
                    stats.put("error", e.getMessage());

                    // 使用回退函数生成默认结果
                    return config.getFallbackFunction().apply(params);
                }
            }, executor);

            taskFutures.put(taskKey, future);
        }

        try {
            // 等待所有任务完成
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    taskFutures.values().toArray(new CompletableFuture[0])
            );

            // 处理所有结果
            CompletableFuture<R> resultFuture = allFutures.thenApply(v -> {
                try {
                    long mergeStartTime = System.currentTimeMillis();

                    // 打印开始整合结果日志
                    log.info(createSubBoxMessage(
                            String.format("%s %s开始整合AI操作结果%s", ANSI_BOLD, ANSI_BLUE, ANSI_RESET),
                            ICON_MERGE
                    ));

                    // 收集所有任务结果
                    Map<String, Object> results = new HashMap<>();
                    for (Map.Entry<String, CompletableFuture<Object>> entry : taskFutures.entrySet()) {
                        results.put(entry.getKey(), entry.getValue().get());
                    }

                    // 处理结果
                    R finalResult = resultProcessor.apply(results);

                    // 计算合并时间
                    long mergeEndTime = System.currentTimeMillis();
                    long mergeDuration = mergeEndTime - mergeStartTime;

                    // 打印整合完成日志
                    log.info(createSubBoxMessage(
                            String.format("%s %s完成整合AI操作结果%s | %s%s%s %s",
                                    ANSI_BOLD, ANSI_GREEN, ANSI_RESET,
                                    ANSI_YELLOW, ICON_TIME, ANSI_RESET, formatDuration(mergeDuration)
                            ),
                            ICON_COMPLETE
                    ));

                    return finalResult;
                } catch (Exception e) {
                    // 打印整合失败日志
                    log.error(createSubBoxMessage(
                            String.format("%s %s整合AI操作结果失败%s | %s错误:%s %s",
                                    ANSI_BOLD, ANSI_RED, ANSI_RESET,
                                    ANSI_RED, ANSI_RESET, e.getMessage()
                            ),
                            ICON_FAILED
                    ));
                    throw new CompletionException(e);
                }
            });

            // 获取最终结果
            R result = resultFuture.get();

            // 计算总体执行时间
            long totalEndTime = System.currentTimeMillis();
            long totalDuration = totalEndTime - totalStartTime;

            // 生成性能报告
            generatePerformanceReport(performanceStats, totalDuration, inputLength);

            return result;
        } catch (Exception e) {
            // 打印执行失败日志
            log.error(createMainBoxEnd(
                    String.format("%s %s执行并行AI操作失败%s: %s",
                            ANSI_BOLD, ANSI_RED, ANSI_RESET, e.getMessage()
                    ),
                    ICON_FAILED
            ));
            throw new RuntimeException("执行并行AI操作失败", e);
        }
    }

    /**
     * 计算吞吐率 (字符/秒)
     */
    private double calculateThroughput(int inputLength, int outputLength, long durationMs) {
        if (durationMs == 0) return 0;
        return (inputLength + outputLength) / (durationMs / 1000.0);
    }

    /**
     * 估算输入文本长度（用于日志记录）
     */
    private String estimateInputText(Map<String, Object> params) {
        String[] commonTextParamNames = {"text", "content", "input", "inputText", "jobText", "resumeText", "query", "question"};

        for (String paramName : commonTextParamNames) {
            if (params.containsKey(paramName) && params.get(paramName) instanceof String) {
                return (String) params.get(paramName);
            }
        }

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() instanceof String) {
                return (String) entry.getValue();
            }
        }

        return null;
    }

    /**
     * 生成性能报告
     */
    private void generatePerformanceReport(Map<String, Map<String, Object>> performanceStats,
                                           long totalDuration,
                                           int inputLength) {
        StringBuilder report = new StringBuilder();

        // 报告标题
        String title = String.format("%s %s性能报告%s | %s操作:%s %s | %s总耗时:%s %s",
                ANSI_BOLD, ANSI_CYAN, ANSI_RESET,
                ANSI_BLUE, ANSI_RESET, getName(),
                ANSI_YELLOW, ANSI_RESET, formatDuration(totalDuration)
        );

        report.append(createMainBoxStart(title, ICON_STATS));

        // 收集所有操作的耗时，用于计算关键路径
        List<Map.Entry<String, Long>> operationDurations = new ArrayList<>();

        // 计算总输入和输出字符数
        long totalInputChars = 0;
        long totalOutputChars = 0;

        // 准备表格数据
        List<String[]> tableRows = new ArrayList<>();
        String[] headerColumns = {"任务", "耗时", "模型", "输入", "输出", "条目数", "吞吐率"};

        // 收集表格数据
        for (Map.Entry<String, Map<String, Object>> entry : performanceStats.entrySet()) {
            String taskKey = entry.getKey();
            Map<String, Object> stats = entry.getValue();

            if (stats.containsKey("status") && "failed".equals(stats.get("status"))) {
                // 失败任务的表格行
                tableRows.add(new String[]{
                        taskKey + " " + ICON_FAILED,
                        "失败",
                        getStringValue(stats, "model", ""),
                        "-",
                        "-",
                        "-",
                        "错误: " + getStringValue(stats, "error", "未知错误")
                });
            } else if (stats.containsKey("duration")) {
                // 成功任务的表格行
                long duration = (long) stats.get("duration");
                operationDurations.add(Map.entry(taskKey, duration));

                int inLen = (int) stats.getOrDefault("inputLength", 0);
                int outLen = (int) stats.getOrDefault("outputLength", 0);
                totalInputChars += inLen;
                totalOutputChars += outLen;

                double throughput = (double) stats.getOrDefault("throughput", 0.0);

                String itemCountStr = stats.containsKey("itemCount") ?
                        String.valueOf(stats.get("itemCount")) : "-";

                tableRows.add(new String[]{
                        taskKey,
                        formatDuration(duration),
                        getStringValue(stats, "model", ""),
                        formatNumber(inLen),
                        formatNumber(outLen),
                        itemCountStr,
                        String.format("%.2f 字符/秒", throughput)
                });
            }
        }

        // 计算最佳列宽
        int[] columnWidths = calculateOptimalColumnWidths(headerColumns, tableRows);

        // 输出表格标题行
        report.append(createTableHeader(headerColumns, columnWidths));

        // 输出表格内容行
        for (String[] row : tableRows) {
            boolean isError = "失败".equals(row[1]);
            report.append(createTableRow(row, columnWidths, isError));
        }

        // 添加表格底部分隔线
        report.append(createTableFooter());

        // 并行效率分析
        if (!operationDurations.isEmpty()) {
            // 计算串行总耗时
            long serialDuration = operationDurations.stream()
                    .mapToLong(Map.Entry::getValue)
                    .sum();

            // 找出耗时最长的操作（关键路径）
            Map.Entry<String, Long> criticalPath = operationDurations.stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            double parallelSpeedup = (double) serialDuration / totalDuration;
            double parallelEfficiency = parallelSpeedup / operationDurations.size() * 100;

            // 计算总吞吐率
            double totalThroughput = (totalInputChars + totalOutputChars) / (totalDuration / 1000.0);

            // 添加总体统计部分
            report.append(createSectionTitle("总体统计"));

            // 输入输出统计
            report.append(createDetailLine(String.format(
                    "%s总输入:%s %s 字符 | %s总输出:%s %s 字符 | %s总吞吐率:%s %.2f 字符/秒",
                    ANSI_CYAN, ANSI_RESET, formatNumber(totalInputChars),
                    ANSI_CYAN, ANSI_RESET, formatNumber(totalOutputChars),
                    ANSI_CYAN, ANSI_RESET, totalThroughput
            )));

            // 并行效率
            report.append(createDetailLine(String.format(
                    "%s串行耗时估计:%s %s | %s加速比:%s %.2fx | %s并行效率:%s %.2f%%",
                    ANSI_CYAN, ANSI_RESET, formatDuration(serialDuration),
                    ANSI_CYAN, ANSI_RESET, parallelSpeedup,
                    ANSI_CYAN, ANSI_RESET, parallelEfficiency
            )));

            // 关键路径
            double percentOfTotal = (double) criticalPath.getValue() / totalDuration * 100;
            report.append(createDetailLine(String.format(
                    "%s关键路径:%s %s (%s, 占比 %.2f%%)",
                    ANSI_CYAN, ANSI_RESET, criticalPath.getKey(),
                    formatDuration(criticalPath.getValue()), percentOfTotal
            )));
        }

        report.append(createMainBoxEnd("", null));

        log.info(report.toString());
    }

    /**
     * 计算最佳列宽度
     */
    private int[] calculateOptimalColumnWidths(String[] headers, List<String[]> rows) {
        // 初始化列宽数组
        int[] columnWidths = Arrays.copyOf(TABLE_COLUMN_WIDTHS, headers.length);

        // 根据标题调整列宽
        for (int i = 0; i < headers.length; i++) {
            int headerLength = headers[i].length();
            if (headerLength > columnWidths[i]) {
                columnWidths[i] = headerLength;
            }
        }

        // 根据内容调整列宽
        for (String[] row : rows) {
            for (int i = 0; i < row.length && i < columnWidths.length; i++) {
                if (row[i] != null) {
                    int contentLength = row[i].length() - countAnsiChars(row[i]);
                    if (contentLength > columnWidths[i]) {
                        // 限制最大列宽，避免表格过宽
                        columnWidths[i] = Math.min(contentLength, TABLE_COLUMN_WIDTHS[i] * 2);
                    }
                }
            }
        }

        return columnWidths;
    }

    /**
     * 创建主盒子开始部分
     */
    private String createMainBoxStart(String title, String icon) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");

        // 顶部边框
        sb.append(MAIN_BORDER_TL).append(repeat(MAIN_BORDER_H, MAIN_BOX_WIDTH - 2)).append(MAIN_BORDER_TR).append("\n");

        // 标题行
        String titleWithIcon = icon != null ? icon + " " + title : title;
        int padding = MAIN_BOX_WIDTH - 2 - titleWithIcon.length() + countAnsiChars(titleWithIcon);
        sb.append(MAIN_BORDER_V).append(" ").append(titleWithIcon);
        sb.append(repeat(" ", padding > 0 ? padding : 1)).append("\n");

        return sb.toString();
    }

    /**
     * 创建主盒子结束部分
     */
    private String createMainBoxEnd(String message, String icon) {
        StringBuilder sb = new StringBuilder();

        // 如果有消息，添加消息行
        if (message != null && !message.isEmpty()) {
            String msgWithIcon = icon != null ? icon + " " + message : message;
            int padding = MAIN_BOX_WIDTH - 2 - msgWithIcon.length() + countAnsiChars(msgWithIcon);
            sb.append(MAIN_BORDER_V).append(" ").append(msgWithIcon);
            sb.append(repeat(" ", padding > 0 ? padding : 1)).append("\n");
        }

        // 底部边框
        sb.append(MAIN_BORDER_BL).append(repeat(MAIN_BORDER_H, MAIN_BOX_WIDTH - 2)).append(MAIN_BORDER_BR);

        return sb.toString();
    }

    /**
     * 创建子盒子消息
     */
    private String createSubBoxMessage(String message, String icon) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");

        // 顶部边框
        sb.append(SUB_BORDER_TL).append(repeat(SUB_BORDER_H, SUB_BOX_WIDTH - 2)).append(SUB_BORDER_TR).append("\n");

        // 消息行
        String msgWithIcon = icon != null ? icon + " " + message : message;
        int padding = SUB_BOX_WIDTH - 2 - msgWithIcon.length() + countAnsiChars(msgWithIcon);
        sb.append(SUB_BORDER_V).append(" ").append(msgWithIcon);
        sb.append(repeat(" ", padding > 0 ? padding : 1)).append("\n");

        // 底部边框
        sb.append(SUB_BORDER_BL).append(repeat(SUB_BORDER_H, SUB_BOX_WIDTH - 2)).append(SUB_BORDER_BR);

        return sb.toString();
    }

    /**
     * 创建表格标题行
     */
    private String createTableHeader(String[] columns, int[] columnWidths) {
        StringBuilder sb = new StringBuilder();

        // 表头分隔线
        sb.append(MAIN_BORDER_ML).append(repeat(MAIN_BORDER_H, MAIN_BOX_WIDTH - 2)).append(MAIN_BORDER_MR).append("\n");

        // 表头内容
        sb.append(MAIN_BORDER_V).append(" ");
        for (int i = 0; i < columns.length; i++) {
            sb.append(ANSI_BOLD).append(padRight(columns[i], columnWidths[i])).append(ANSI_RESET);
            // 最后一列不添加分隔符
            if (i < columns.length - 1) {
                sb.append(" | ");
            }
        }

        // 添加填充和右边框
        int contentLength = sb.length() - countAnsiChars(sb.toString());
        int padding = MAIN_BOX_WIDTH - contentLength;
        sb.append(repeat(" ", padding > 0 ? padding : 1)).append("\n");

        // 表头下分隔线
        sb.append(MAIN_BORDER_ML).append(repeat(MAIN_BORDER_H, MAIN_BOX_WIDTH - 2)).append(MAIN_BORDER_MR).append("\n");

        return sb.toString();
    }

    /**
     * 创建表格行
     */
    private String createTableRow(String[] values, int[] columnWidths, boolean isError) {
        StringBuilder sb = new StringBuilder();

        sb.append(MAIN_BORDER_V).append(" ");
        for (int i = 0; i < values.length; i++) {
            if (isError && i == 0) {
                sb.append(ANSI_RED).append(padRight(values[i], columnWidths[i])).append(ANSI_RESET);
            } else if (isError && i == values.length - 1) {
                sb.append(ANSI_RED).append(values[i]).append(ANSI_RESET);
            } else {
                sb.append(padRight(values[i], columnWidths[i]));
            }

            // 最后一列不添加分隔符
            if (i < values.length - 1) {
                sb.append(" | ");
            }
        }

        // 添加填充和右边框
        int contentLength = sb.length() - countAnsiChars(sb.toString());
        int padding = MAIN_BOX_WIDTH - contentLength;
        sb.append(repeat(" ", padding > 0 ? padding : 1)).append("\n");

        return sb.toString();
    }

    /**
     * 创建表格底部
     */
    private String createTableFooter() {
        return MAIN_BORDER_ML + repeat(MAIN_BORDER_H, MAIN_BOX_WIDTH - 2) + MAIN_BORDER_MR + "\n";
    }

    /**
     * 创建章节标题
     */
    private String createSectionTitle(String title) {
        StringBuilder sb = new StringBuilder();

        sb.append(MAIN_BORDER_V).append(" ").append(ANSI_BOLD).append(title).append(ANSI_RESET);
        int padding = MAIN_BOX_WIDTH - 2 - title.length();
        sb.append(repeat(" ", padding > 0 ? padding : 1)).append("\n");

        return sb.toString();
    }

    /**
     * 创建详情行
     */
    private String createDetailLine(String detail) {
        StringBuilder sb = new StringBuilder();

        sb.append(MAIN_BORDER_V).append("   ").append(ICON_DETAIL).append(" ").append(detail);
        int padding = MAIN_BOX_WIDTH - 5 - detail.length() + countAnsiChars(detail);
        sb.append(repeat(" ", padding > 0 ? padding : 1)).append("\n");

        return sb.toString();
    }

    /**
     * 右填充字符串到指定长度
     */
    private String padRight(String s, int n) {
        if (s == null) {
            return repeat(" ", n);
        }

        int visibleLength = s.length() - countAnsiChars(s);
        if (visibleLength >= n) {
            return s;
        }

        return s + repeat(" ", n - visibleLength);
    }

    /**
     * 重复字符串n次
     */
    private String repeat(String str, int times) {
        if (times <= 0) return "";
        return str.repeat(times);
    }

    /**
     * 计算ANSI转义序列的字符数
     * 用于正确计算字符串长度（排除颜色代码）
     */
    private int countAnsiChars(String str) {
        if (str == null) return 0;

        int count = 0;
        boolean inEscape = false;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '\u001B') {
                inEscape = true;
                count++;
            } else if (inEscape) {
                count++;
                if (str.charAt(i) == 'm') {
                    inEscape = false;
                }
            }
        }
        return count;
    }

    /**
     * 格式化数字，添加千位分隔符
     */
    private String formatNumber(long number) {
        return String.format("%,d", number);
    }

    /**
     * 格式化持续时间
     */
    private String formatDuration(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else if (durationMs < 60000) {
            return String.format("%.2f秒", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60000;
            long seconds = (durationMs % 60000) / 1000;
            return String.format("%d分%d秒", minutes, seconds);
        }
    }

    /**
     * 关闭协调器，释放资源
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 从Map中安全获取字符串值
     */
    protected String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        if (map.containsKey(key) && map.get(key) != null) {
            String value = map.get(key).toString();
            return value.isEmpty() ? defaultValue : value;
        }
        return defaultValue;
    }

    /**
     * 从Map中安全获取整数值
     */
    protected Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        if (map == null) {
            return defaultValue;
        }

        if (map.containsKey(key) && map.get(key) != null) {
            try {
                if (map.get(key) instanceof Integer) {
                    return (Integer) map.get(key);
                } else if (map.get(key) instanceof Number) {
                    return ((Number) map.get(key)).intValue();
                } else {
                    String strValue = map.get(key).toString().trim();
                    if (strValue.isEmpty() || strValue.equalsIgnoreCase("未知")) {
                        return defaultValue;
                    }
                    return Integer.parseInt(strValue);
                }
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 从Map中安全获取布尔值
     */
    protected boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        if (map == null) {
            return defaultValue;
        }

        if (map.containsKey(key) && map.get(key) != null) {
            if (map.get(key) instanceof Boolean) {
                return (Boolean) map.get(key);
            } else {
                String strValue = map.get(key).toString().trim().toLowerCase();
                return strValue.equals("true") || strValue.equals("yes") || strValue.equals("1");
            }
        }
        return defaultValue;
    }

    /**
     * 从Map中安全获取双精度浮点值
     */
    protected Double getDoubleValue(Map<String, Object> map, String key, Double defaultValue) {
        if (map == null) {
            return defaultValue;
        }

        if (map.containsKey(key) && map.get(key) != null) {
            try {
                if (map.get(key) instanceof Double) {
                    return (Double) map.get(key);
                } else if (map.get(key) instanceof Number) {
                    return ((Number) map.get(key)).doubleValue();
                } else {
                    String strValue = map.get(key).toString().trim();
                    if (strValue.isEmpty()) {
                        return defaultValue;
                    }
                    return Double.parseDouble(strValue);
                }
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 从Map中安全获取列表值
     */
    protected <T> List<T> getListValue(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return new ArrayList<>();
        }

        if (map.get(key) instanceof List) {
            return (List<T>) map.get(key);
        }

        return new ArrayList<>();
    }
}