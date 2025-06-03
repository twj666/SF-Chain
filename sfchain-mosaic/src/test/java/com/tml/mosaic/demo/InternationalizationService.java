package com.tml.mosaic.demo;

import com.tml.mosaic.core.frame.CodeInjector;
import com.tml.mosaic.core.frame.PointResult;

/**
 * 描述: 国际化业务服务 - 展示翻译插件的使用
 * @author suifeng
 * 日期: 2025/5/27
 */
public class InternationalizationService {
    
    /**
     * 处理用户消息 - 支持多语言翻译
     */
    public String processUserMessage(String message) {
        System.out.println("原始消息: " + message);

        // 注入点: 文本翻译增强
        PointResult translateResult = CodeInjector.executeInjectionPoint("business.message.translate", message);
        message = translateResult.getResult("translated_text");

        return message;
    }
}