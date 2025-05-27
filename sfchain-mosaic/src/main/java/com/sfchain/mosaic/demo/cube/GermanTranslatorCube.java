package com.sfchain.mosaic.demo.cube;

import com.sfchain.mosaic.core.*;

/**
 * 描述: 德语翻译方块
 * @author suifeng
 * 日期: 2025/5/27
 */
@MCube(value = "germanTranslatorCube", version = "1.0.0", description = "德语翻译工具")
public class GermanTranslatorCube extends AbstractCube {

    public GermanTranslatorCube() {
        super("germanTranslatorCube", "1.0.0", "德语翻译工具");
    }

    @MExtension(value = "text.translate", name = "德语翻译", description = "将文本翻译为德语")
    public MOutput translate(MInput input) {
        String originalText = safeGetString(input, "param0", "");
        
        // 模拟德语翻译逻辑
        String translatedText = translateToGerman(originalText);

        return createSuccessOutput().setResult("original_text", originalText).setResult("translated_text", translatedText);
    }
    
    private String translateToGerman(String text) {
        // 简单的模拟翻译映射
        switch (text.toLowerCase()) {
            case "hello": return "Hallo";
            case "world": return "Welt";
            case "good morning": return "Guten Morgen";
            case "thank you": return "Danke";
            case "goodbye": return "Auf Wiedersehen";
            case "welcome": return "Willkommen";
            default: return "[DE]" + text;
        }
    }
}