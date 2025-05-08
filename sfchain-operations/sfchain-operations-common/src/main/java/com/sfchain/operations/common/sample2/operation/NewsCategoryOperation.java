package com.sfchain.operations.common.sample2.operation;

import com.alibaba.fastjson2.JSON;
import com.sfchain.core.annotation.AIOp;
import com.sfchain.core.operation.BaseAIOperation;
import com.sfchain.operations.common.sample2.domain.NewsCategoryVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.sfchain.core.constant.AIConstant.*;

/**
 * 描述: 新闻分类操作
 * 负责对新闻进行分类
 * @author suifeng
 * 日期: 2025/5/8
 */
@AIOp("news-category")
@Component
public class NewsCategoryOperation extends BaseAIOperation<Map<String, Object>, NewsCategoryVO> {

    @Override
    public List<String> supportedModels() {
        return List.of(SILI_QWEN, SILI_DEEP_SEEK_V3, SILI_THUDM);
    }

    @Override
    public String buildPrompt(Map<String, Object> params) {
        String newsText = (String) params.get("text");
        String title = params.containsKey("title") ? (String) params.get("title") : "未知标题";
        
        // 使用提示词构建器创建结构化提示词
        return createPromptBuilder("新闻分类")
                .addRole("你是一位专业的新闻分类专家，擅长将新闻准确分类到合适的类别。")
                .addSection("任务说明", "请对以下新闻进行分类，确定其主要类别和子类别。分类应当：\n" +
                        "1. 确定一个最适合的主要类别（如政治、经济、科技、体育、文化、社会、国际等）\n" +
                        "2. 确定1-3个相关的子类别，更具体地描述新闻内容\n" +
                        "3. 给出分类的置信度（0.0-1.0之间）")
                .addSection("新闻标题", title)
                .addSection("新闻正文", newsText)
                .addSection("分类参考", "主要类别参考：政治、经济、科技、体育、文化、教育、健康、社会、国际、环境、军事、娱乐\n" +
                        "子类别应更具体，如：人工智能、股市、足球、电影、疫情、气候变化等")
                .addJsonOutput("{\n" +
                        "  \"primaryCategory\": \"主要分类\",\n" +
                        "  \"subCategories\": [\"子分类1\", \"子分类2\", ...],\n" +
                        "  \"confidence\": 分类置信度(0.0到1.0之间的小数)\n" +
                        "}")
                .build();
    }

    @Override
    public NewsCategoryVO parseResponse(String aiResponse) {
        try {
            // 提取JSON
            String jsonStr = extractJsonFromResponse(aiResponse);
            
            // 解析为NewsCategoryVO对象
            NewsCategoryVO categoryVO = JSON.parseObject(jsonStr, NewsCategoryVO.class);
            
            // 验证结果
            if (categoryVO.getPrimaryCategory() == null || categoryVO.getPrimaryCategory().isEmpty()) {
                throw new RuntimeException("分类结果为空");
            }
            
            if (categoryVO.getSubCategories() == null) {
                categoryVO.setSubCategories(new String[0]);
            }
            
            // 确保置信度在有效范围内
            if (categoryVO.getConfidence() < 0.0 || categoryVO.getConfidence() > 1.0) {
                categoryVO.setConfidence(Math.max(0.0, Math.min(1.0, categoryVO.getConfidence())));
            }
            
            return categoryVO;
        } catch (Exception e) {
            throw new RuntimeException("解析分类结果失败: " + e.getMessage(), e);
        }
    }
}