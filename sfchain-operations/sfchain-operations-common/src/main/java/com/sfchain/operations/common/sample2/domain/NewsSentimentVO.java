package com.sfchain.operations.common.sample2.domain;

import lombok.Data;

/**
 * 新闻情感分析结果
 */
@Data
public class NewsSentimentVO {
    private String sentiment; // 积极、消极、中性
    private double score; // 情感得分 (-1.0 到 1.0)
    private String analysis; // 情感分析解释
}
