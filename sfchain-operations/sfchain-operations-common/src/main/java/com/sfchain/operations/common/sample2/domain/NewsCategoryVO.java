package com.sfchain.operations.common.sample2.domain;

import lombok.Data;

/**
 * 新闻分类结果
 */
@Data
public class NewsCategoryVO {
    private String primaryCategory; // 主要分类
    private String[] subCategories; // 子分类
    private double confidence; // 分类置信度
}