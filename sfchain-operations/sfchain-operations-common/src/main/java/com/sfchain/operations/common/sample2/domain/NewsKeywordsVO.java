package com.sfchain.operations.common.sample2.domain;

import lombok.Data;

/**
 * 新闻关键词提取结果
 */
@Data
public class NewsKeywordsVO {
    private String[] keywords;
    private String[] entities; // 实体名称（人物、组织、地点等）
    private String[] topics; // 主题词
}