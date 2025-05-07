package com.sfchain.operations.common.sample2.operation;

import com.alibaba.fastjson2.JSON;
import com.sfchain.core.annotation.AIOp;

import com.sfchain.core.operation.BaseAIOperation;
import com.sfchain.operations.common.sample2.domain.NewsKeywordsVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.sfchain.core.constant.AIConstant.DEEP_SEEK_V3;
import static com.sfchain.core.constant.AIConstant.THUDM;

/**
 * 描述: 新闻关键词提取操作
 * 负责从新闻文本中提取关键词和实体
 * @author suifeng
 * 日期: 2025/5/8
 */
@AIOp("news-keywords")
@Component
public class NewsKeywordsOperation extends BaseAIOperation<Map<String, Object>, NewsKeywordsVO> {

    @Override
    public List<String> supportedModels() {
        return List.of(DEEP_SEEK_V3, THUDM);
    }

    @Override
    public String buildPrompt(Map<String, Object> params) {
        String newsText = (String) params.get("text");
        String title = params.containsKey("title") ? (String) params.get("title") : "未知标题";

        // 使用提示词构建器创建结构化提示词
        return createPromptBuilder("新闻关键词提取")
                .addRole("你是一位专业的文本分析专家，擅长从文本中提取关键词、实体和主题。")
                .addSection("任务说明", "请从以下新闻中提取三类关键信息：\n" +
                        "1. 关键词：文本中最重要的词语，按重要性排序\n" +
                        "2. 实体：文本中出现的人物、组织、地点等命名实体\n" +
                        "3. 主题：文本涉及的主要主题或话题")
                .addSection("新闻标题", title)
                .addSection("新闻正文", newsText)
                .addJsonOutput("{\n" +
                        "  \"keywords\": [\"关键词1\", \"关键词2\", \"关键词3\", ...],\n" +
                        "  \"entities\": [\"实体1\", \"实体2\", \"实体3\", ...],\n" +
                        "  \"topics\": [\"主题1\", \"主题2\", \"主题3\", ...]\n" +
                        "}")
                .build();
    }

    @Override
    public NewsKeywordsVO parseResponse(String aiResponse) {
        try {
            // 提取JSON
            String jsonStr = extractJsonFromResponse(aiResponse);

            // 解析为NewsKeywordsVO对象
            NewsKeywordsVO keywordsVO = JSON.parseObject(jsonStr, NewsKeywordsVO.class);

            // 验证结果
            if (keywordsVO.getKeywords() == null || keywordsVO.getKeywords().length == 0) {
                keywordsVO.setKeywords(new String[]{"无关键词"});
            }

            if (keywordsVO.getEntities() == null) {
                keywordsVO.setEntities(new String[0]);
            }

            if (keywordsVO.getTopics() == null) {
                keywordsVO.setTopics(new String[0]);
            }

            return keywordsVO;
        } catch (Exception e) {
            throw new RuntimeException("解析关键词提取结果失败: " + e.getMessage(), e);
        }
    }
}