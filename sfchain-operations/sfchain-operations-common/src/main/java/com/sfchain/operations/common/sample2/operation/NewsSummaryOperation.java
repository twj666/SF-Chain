package com.sfchain.operations.common.sample2.operation;

import com.alibaba.fastjson2.JSON;
import com.sfchain.core.annotation.AIOp;

import com.sfchain.core.operation.BaseAIOperation;
import com.sfchain.operations.common.sample2.domain.NewsSummaryVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.sfchain.core.constant.AIConstant.DEEP_SEEK_V3;
import static com.sfchain.core.constant.AIConstant.GPT_4O;

/**
 * 描述: 新闻摘要操作
 * 负责从新闻文本中生成简洁的摘要
 * @author suifeng
 * 日期: 2025/5/8
 */
@AIOp("news-summary")
@Component
public class NewsSummaryOperation extends BaseAIOperation<Map<String, Object>, NewsSummaryVO> {

    @Override
    public List<String> supportedModels() {
        return List.of(DEEP_SEEK_V3, GPT_4O);
    }

    @Override
    public String buildPrompt(Map<String, Object> params) {
        String newsText = (String) params.get("text");
        String title = params.containsKey("title") ? (String) params.get("title") : "未知标题";
        
        // 使用提示词构建器创建结构化提示词
        return createPromptBuilder("新闻摘要生成")
                .addRole("你是一位专业的新闻编辑，善于提炼新闻核心内容，生成简洁明了的摘要。")
                .addSection("任务说明", "请为以下新闻生成一个简洁的摘要，突出最重要的信息点。摘要应当：\n" +
                        "1. 包含新闻的核心事实和关键信息\n" +
                        "2. 保持客观中立的语气\n" +
                        "3. 控制在原文的20%长度以内\n" +
                        "4. 避免添加原文中不存在的信息")
                .addSection("新闻标题", title)
                .addSection("新闻正文", newsText)
                .addJsonOutput("{\n" +
                        "  \"summary\": \"生成的新闻摘要内容\",\n" +
                        "  \"originalLength\": 原文字符数,\n" +
                        "  \"summaryLength\": 摘要字符数,\n" +
                        "  \"compressionRatio\": 压缩比例(小数)\n" +
                        "}")
                .build();
    }

    @Override
    public NewsSummaryVO parseResponse(String aiResponse) {
        try {
            // 提取JSON
            String jsonStr = extractJsonFromResponse(aiResponse);
            
            // 解析为NewsSummaryVO对象
            NewsSummaryVO summaryVO = JSON.parseObject(jsonStr, NewsSummaryVO.class);
            
            // 验证结果
            if (summaryVO.getSummary() == null || summaryVO.getSummary().isEmpty()) {
                throw new RuntimeException("摘要内容为空");
            }
            
            return summaryVO;
        } catch (Exception e) {
            throw new RuntimeException("解析摘要结果失败: " + e.getMessage(), e);
        }
    }
}