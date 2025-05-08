package com.sfchain.operations.common.sample2.domain;

import lombok.Data;

/**
 * 综合新闻分析结果
 */
@Data
public class NewsAnalysisResult {
    private String title;
    private String source;
    private String date;
    
    private NewsSummaryVO summary;
    private NewsSentimentVO sentiment;
    private NewsKeywordsVO keywords;
    private NewsCategoryVO category;
    
    // 元数据
    private long processingTime; // 处理时间(毫秒)
    private int modelCount; // 使用的模型数量
}