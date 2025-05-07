package com.sfchain.operations.common.sample2.domain;

import lombok.Data;

/**
 * 新闻摘要结果
 */
@Data
public class NewsSummaryVO {
    private String summary;
    private int originalLength;
    private int summaryLength;
    private double compressionRatio;
}