package com.tml.mosaic.demo.cube;

import com.tml.mosaic.core.*;
import com.tml.mosaic.core.guid.GUUID;

/**
 * 描述: 英语翻译方块
 * @author suifeng
 * 日期: 2025/5/27
 */
@MCube(value = "englishTranslatorCube", version = "1.0.0", description = "英语翻译工具")
public class EnglishTranslatorCube extends AbstractCube {

    public EnglishTranslatorCube() {
        super(new GUUID("englishTranslatorCube"), "1.0.0", "英语翻译工具");
    }

    @MExtension(value = "text.translate", name = "英语翻译", description = "将文本翻译为英语")
    public MOutput translate(MInput input) {
        String originalText = safeGetString(input, "param0", "");

        // 模拟英语翻译逻辑
        String translatedText = translateToEnglish(originalText);

        return createSuccessOutput().setResult("original_text", originalText).setResult("translated_text", translatedText);
    }
    
    private String translateToEnglish(String text) {
        // 简单的模拟翻译映射（假设输入是中文）
        switch (text) {
            case "你好": return "Hello";
            case "世界": return "World";
            case "早上好": return "Good Morning";
            case "谢谢": return "Thank You";
            case "再见": return "Goodbye";
            case "欢迎": return "Welcome";
            default: return "[EN]" + text;
        }
    }
}