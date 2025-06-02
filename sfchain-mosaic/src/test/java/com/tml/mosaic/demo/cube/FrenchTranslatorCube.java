package com.tml.mosaic.demo.cube;

import com.tml.mosaic.core.*;
import com.tml.mosaic.core.guid.GUUID;

/**
 * 描述: 法语翻译方块
 * @author suifeng
 * 日期: 2025/5/27
 */
@MCube(value = "frenchTranslatorCube", version = "1.0.0", description = "法语翻译工具")
public class FrenchTranslatorCube extends AbstractCube {

    public FrenchTranslatorCube() {
        super(new GUUID("frenchTranslatorCube"), "1.0.0", "法语翻译工具");
    }

    @MExtension(value = "text.translate", name = "法语翻译", description = "将文本翻译为法语")
    public MOutput translate(MInput input) {
        String originalText = safeGetString(input, "param0", "");
        
        // 模拟法语翻译逻辑
        String translatedText = translateToFrench(originalText);

        return createSuccessOutput().setResult("original_text", originalText).setResult("translated_text", translatedText);
    }
    
    private String translateToFrench(String text) {
        // 简单的模拟翻译映射
        switch (text.toLowerCase()) {
            case "hello": return "Bonjour";
            case "world": return "Monde";
            case "good morning": return "Bonjour";
            case "thank you": return "Merci";
            case "goodbye": return "Au revoir";
            case "welcome": return "Bienvenue";
            default: return "[FR]" + text;
        }
    }
}