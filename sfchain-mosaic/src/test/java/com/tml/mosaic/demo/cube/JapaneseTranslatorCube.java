package com.tml.mosaic.demo.cube;

import com.tml.mosaic.core.annotation.MCube;
import com.tml.mosaic.core.annotation.MExtension;
import com.tml.mosaic.core.frame.AbstractCube;
import com.tml.mosaic.core.frame.PointParam;
import com.tml.mosaic.core.frame.PointResult;
import com.tml.mosaic.core.tools.guid.GUUID;

/**
 * 描述: 日语翻译方块
 * @author suifeng
 * 日期: 2025/5/27
 */
@MCube(value = "japaneseTranslatorCube", version = "1.0.0", description = "日语翻译工具")
public class JapaneseTranslatorCube extends AbstractCube {

    public JapaneseTranslatorCube() {
        super(new GUUID("japaneseTranslatorCube"), "1.0.0", "日语翻译工具");
    }

    @MExtension(value = "text.translate", name = "日语翻译", description = "将文本翻译为日语")
    public PointResult translate(PointParam input) {
        String originalText = safeGetString(input, "param0", "");
        
        // 模拟日语翻译逻辑
        String translatedText = translateToJapanese(originalText);
        
        return createSuccessOutput().setResult("original_text", originalText).setResult("translated_text", translatedText);
    }
    
    private String translateToJapanese(String text) {
        // 简单的模拟翻译映射
        switch (text.toLowerCase()) {
            case "hello": return "こんにちは";
            case "world": return "世界";
            case "good morning": return "おはようございます";
            case "thank you": return "ありがとうございます";
            case "goodbye": return "さようなら";
            case "welcome": return "いらっしゃいませ";
            default: return "[JP]" + text;
        }
    }
}