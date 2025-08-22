package com.suifeng.sfchain.operations;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.suifeng.sfchain.annotation.AIOp;
import com.suifeng.sfchain.core.BaseAIOperation;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.suifeng.sfchain.constants.AIOperationConstant.JSON_REPAIR_OP;


/**
 * 描述: JSON修复操作
 * 专门用于修复AI返回的格式错误的JSON字符串
 * @author suifeng
 * 日期: 2025/8/11
 */
@AIOp(
        value = JSON_REPAIR_OP,
        description = "修复格式错误的JSON字符串",
        autoRepairJson = false
)
@Component
public class JSONRepairOperation extends BaseAIOperation<String, JSONObject> {

    /**
     * 当前输入的JSON字符串
     */
    private String currentInput;

    @Override
    public String buildPrompt(String brokenJson) {
        this.currentInput = brokenJson; // 保存当前输入
        
        // 如果输入已经是有效JSON，尝试直接解析
        if (isValidJson(brokenJson)) {
            return ""; // 返回空字符串表示不需要AI修复
        }

        return String.format("""
                你是一位专业的JSON格式修复专家，需要将格式错误的JSON字符串修复为有效的JSON格式。
                
                ## 任务描述
                我将提供一个可能包含格式错误的JSON字符串。你的任务是：
                1. 识别并修复所有格式错误，包括但不限于：
                   - 缺失或多余的引号、逗号、括号
                   - 非法的转义字符
                   - 重复的键
                   - 不符合JSON规范的值格式
                2. 保留原始JSON的所有数据和结构
                3. 返回修复后的有效JSON字符串
                
                ## 需要修复的JSON
                ```
                %s
                ```
                
                ## 输出要求
                1. 只返回修复后的JSON字符串，不要有任何解释或额外文字
                2. 确保输出是有效的JSON格式
                3. 保持原始数据的完整性，除非格式错误导致数据冗余
                4. 如果某部分无法修复，使用最合理的猜测进行修复
                
                ## 注意事项
                - 不要给我任何多余的东西，只需要正确的JSON对象
                - 不要添加原始JSON中不存在的键或值
                - 如果原始字符串中包含多个JSON对象，只修复第一个完整的JSON对象
                - 确保所有字符串值使用双引号包围
                - 确保数字、布尔值和null值不使用引号
                - 移除任何注释或非JSON元素
                """, brokenJson);
    }

    @Override
    protected String preprocessResponse(String aiResponse, String brokenJson) {
        // 如果提示为空（表示原始输入已是有效JSON），直接返回原始输入
        if (aiResponse.isEmpty()) {
            return this.currentInput;
        }
        return aiResponse;
    }
    
    @Override
    protected String preprocessJson(String jsonContent, String brokenJson) {
        // 如果提取失败，尝试本地修复
        if (!isValidJson(jsonContent)) {
            return localJsonRepair(jsonContent);
        }
        return jsonContent;
    }
    
    /**
     * 检查字符串是否为有效的JSON
     */
    private boolean isValidJson(String jsonStr) {
        try {
            JSON.parseObject(jsonStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 本地JSON修复逻辑，用于简单错误的修复
     */
    private String localJsonRepair(String brokenJson) {
        // 1. 移除可能的代码块标记
        String json = brokenJson.replaceAll("```json|```", "").trim();
        
        // 2. 确保对象以 { 开始，以 } 结束
        Pattern objectPattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
        Matcher objectMatcher = objectPattern.matcher(json);
        if (objectMatcher.find()) {
            json = objectMatcher.group();
        }
        
        // 3. 修复常见引号问题
        json = json.replaceAll("(?<!\\\\)(')(\\w+)(?<!\\\\)('\\s*:)", "\"$2\":");  // 修复键的单引号
        json = json.replaceAll("(:\\s*)(?<!\\\\)'([^']*?)(?<!\\\\)'", "$1\"$2\""); // 修复值的单引号
        
        // 4. 修复缺失的引号
        json = json.replaceAll("([{,])\\s*(\\w+)\\s*:", "$1\"$2\":");
        
        // 5. 修复多余或缺失的逗号
        json = json.replaceAll(",\\s*}", "}");  // 移除对象末尾多余的逗号
        json = json.replaceAll(",\\s*]", "]");  // 移除数组末尾多余的逗号
        
        // 6. 修复错误的布尔值和null值格式
        json = json.replaceAll("\"(true|false|null)\"", "$1");
        
        // 7. 修复数字值的引号
        json = json.replaceAll("\"(-?\\d+(\\.\\d+)?)\"", "$1");
        
        return json;
    }
}