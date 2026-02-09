package com.suifeng.sfchain.constants;

/**
 * 描述: AI操作常量定义
 * 定义所有AI操作的标识符
 * 
 * @author suifeng
 * 日期: 2025/8/11
 */
public class AIOperationConstant {

    /**
     * JSON修复操作
     */
    public static final String JSON_REPAIR_OP = "JSON_REPAIR_OP";

    /**
     * 模型验证操作
     */
    public static final String MODEL_VALIDATION_OP = "MODEL_VALIDATION_OP";
    
    /**
     * 聊天对话操作
     */
    public static final String CHAT_OP = "CHAT";
    
    /**
     * 私有构造函数，防止实例化
     */
    private AIOperationConstant() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}