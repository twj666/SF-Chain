package com.sfchain.operations.common.sample2.operation;

import com.alibaba.fastjson2.JSON;
import com.sfchain.core.annotation.AIOp;
import com.sfchain.core.operation.BaseAIOperation;
import com.sfchain.operations.common.sample2.domain.NewsSentimentVO;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

import static com.sfchain.core.constant.AIConstant.*;


/**
 * 描述: 新闻情感分析操作
 * 负责分析新闻文本的情感倾向
 * @author suifeng
 * 日期: 2025/5/8
 */
@AIOp("news-sentiment")
@Component
public class NewsSentimentOperation extends BaseAIOperation<Map<String, Object>, NewsSentimentVO> {

    @Override
    public List<String> supportedModels() {
        return List.of(SILI_QWEN, SILI_DEEP_SEEK_V3, SILI_THUDM);
    }

    @Override
    public String buildPrompt(Map<String, Object> params) {
        String newsText = (String) params.get("text");
        String title = params.containsKey("title") ? (String) params.get("title") : "未知标题";
        
        // 使用提示词构建器创建结构化提示词
        return createPromptBuilder("新闻情感分析")
                .addRole("你是一位专业的文本情感分析专家，擅长分析文本的情感倾向和语气。")
                .addSection("任务说明", "请分析以下新闻的整体情感倾向，判断其是积极、消极还是中性的。分析应当：\n" +
                        "1. 考虑新闻的整体语气和用词\n" +
                        "2. 评估新闻报道的事件性质\n" +
                        "3. 给出情感倾向的评分(-1.0表示极度消极，0表示中性，1.0表示极度积极)\n" +
                        "4. 简要解释你的判断理由")
                .addSection("新闻标题", title)
                .addSection("新闻正文", newsText)
                .addJsonOutput("{\n" +
                        "  \"sentiment\": \"积极|消极|中性\",\n" +
                        "  \"score\": 情感得分(-1.0到1.0之间的小数),\n" +
                        "  \"analysis\": \"情感分析解释\"\n" +
                        "}")
                .build();
    }

    @Override
    public NewsSentimentVO parseResponse(String aiResponse) {
        try {
            // 提取JSON
            String jsonStr = extractJsonFromResponse(aiResponse);
            
            // 解析为NewsSentimentVO对象
            NewsSentimentVO sentimentVO = JSON.parseObject(jsonStr, NewsSentimentVO.class);
            
            // 验证结果
            if (sentimentVO.getSentiment() == null || sentimentVO.getSentiment().isEmpty()) {
                throw new RuntimeException("情感分析结果为空");
            }
            
            // 确保情感得分在有效范围内
            if (sentimentVO.getScore() < -1.0 || sentimentVO.getScore() > 1.0) {
                sentimentVO.setScore(Math.max(-1.0, Math.min(1.0, sentimentVO.getScore())));
            }
            
            return sentimentVO;
        } catch (Exception e) {
            throw new RuntimeException("解析情感分析结果失败: " + e.getMessage(), e);
        }
    }
}