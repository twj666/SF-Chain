package com.sfchain.operations.common.sample2;

import com.sfchain.core.AIService;
import com.sfchain.core.operation.AIOperationCoordinator;
import com.sfchain.operations.common.sample2.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.sfchain.core.constant.AIConstant.*;

/**
 * 描述: 新闻分析协调器
 * 负责协调多个AI操作并行处理新闻分析任务
 * @author suifeng
 * 日期: 2025/5/8
 */
@Component
@Slf4j
public class NewsAnalysisCoordinator extends AIOperationCoordinator {

    // 任务名称常量
    private static final String SUMMARY_TASK = "summary";
    private static final String SENTIMENT_TASK = "sentiment";
    private static final String KEYWORDS_TASK = "keywords";
    private static final String CATEGORY_TASK = "category";

    // 操作名称常量
    private static final String SUMMARY_OPERATION = "news-summary";
    private static final String SENTIMENT_OPERATION = "news-sentiment";
    private static final String KEYWORDS_OPERATION = "news-keywords";
    private static final String CATEGORY_OPERATION = "news-category";

    /**
     * 构造函数
     * @param aiService AI服务代理
     */
    public NewsAnalysisCoordinator(AIService aiService) {
        super(aiService, 4); // 使用4个线程的线程池
    }

    @Override
    public String getName() {
        return "新闻分析系统";
    }

    /**
     * 分析新闻
     * @param title 新闻标题
     * @param content 新闻内容
     * @param source 新闻来源
     * @param date 新闻日期
     * @return 新闻分析结果
     */
    public NewsAnalysisResult analyzeNews(String title, String content, String source, String date) {
        // 准备参数
        Map<String, Object> params = new HashMap<>();
        params.put("text", content);
        params.put("title", title);
        
        // 定义任务配置
        Map<String, TaskConfig<?>> taskConfigs = new HashMap<>();
        
        // 摘要任务
        taskConfigs.put(SUMMARY_TASK, new TaskConfig<>(
                SUMMARY_OPERATION,
                DEEP_SEEK_V3,
                p -> {
                    NewsSummaryVO fallback = new NewsSummaryVO();
                    fallback.setSummary("无法生成摘要");
                    fallback.setOriginalLength(content.length());
                    fallback.setSummaryLength(fallback.getSummary().length());
                    fallback.setCompressionRatio(0);
                    return fallback;
                }
        ));
        
        // 情感分析任务
        taskConfigs.put(SENTIMENT_TASK, new TaskConfig<>(
                SENTIMENT_OPERATION,
                QWEN_PLUS,
                p -> {
                    NewsSentimentVO fallback = new NewsSentimentVO();
                    fallback.setSentiment("中性");
                    fallback.setScore(0.0);
                    fallback.setAnalysis("无法进行情感分析");
                    return fallback;
                }
        ));
        
        // 关键词提取任务
        taskConfigs.put(KEYWORDS_TASK, new TaskConfig<>(
                KEYWORDS_OPERATION,
                THUDM,
                p -> {
                    NewsKeywordsVO fallback = new NewsKeywordsVO();
                    fallback.setKeywords(new String[]{"无关键词"});
                    fallback.setEntities(new String[0]);
                    fallback.setTopics(new String[0]);
                    return fallback;
                }
        ));
        
        // 分类任务
        taskConfigs.put(CATEGORY_TASK, new TaskConfig<>(
                CATEGORY_OPERATION,
                GPT_4O,
                p -> {
                    NewsCategoryVO fallback = new NewsCategoryVO();
                    fallback.setPrimaryCategory("未分类");
                    fallback.setSubCategories(new String[0]);
                    fallback.setConfidence(0.0);
                    return fallback;
                }
        ));
        
        // 执行并行任务
        return executeParallelTasks(
                params,
                taskConfigs,
                results -> processNewsAnalysisResults(results, title, source, date)
        );
    }

    /**
     * 处理新闻分析结果
     * @param results 各任务的结果
     * @param title 新闻标题
     * @param source 新闻来源
     * @param date 新闻日期
     * @return 完整的新闻分析结果
     */
    @SuppressWarnings("unchecked")
    private NewsAnalysisResult processNewsAnalysisResults(
            Map<String, Object> results, 
            String title, 
            String source, 
            String date) {
        
        // 获取各部分结果
        NewsSummaryVO summary = (NewsSummaryVO) results.get(SUMMARY_TASK);
        NewsSentimentVO sentiment = (NewsSentimentVO) results.get(SENTIMENT_TASK);
        NewsKeywordsVO keywords = (NewsKeywordsVO) results.get(KEYWORDS_TASK);
        NewsCategoryVO category = (NewsCategoryVO) results.get(CATEGORY_TASK);
        
        // 构建最终结果
        NewsAnalysisResult result = new NewsAnalysisResult();
        result.setTitle(title);
        result.setSource(source);
        result.setDate(date);
        
        // 设置各部分分析结果
        result.setSummary(summary);
        result.setSentiment(sentiment);
        result.setKeywords(keywords);
        result.setCategory(category);
        
        // 设置元数据
        result.setModelCount(4); // 使用了4个模型
        
        return result;
    }
}