package com.tml.mosaic.demo;


import com.tml.mosaic.cube.CodeInjector;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.cube.CubeManager;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.core.tools.guid.GUUID;

/**
 * 描述: 翻译插件演示程序
 * @author suifeng
 * 日期: 2025/5/27
 */
public class TranslatorDemo {

    public static void main(String[] args) {
        System.out.println("========== 翻译插件演示 ==========");

        initializeTranslators();
        demonstrateDynamicSwitching();

        System.out.println("========== 演示完成 ==========");
    }

    private static void initializeTranslators() {
        System.out.println("\n--- 初始化翻译插件 ---");
        CubeManager cubeManager = CubeManager.getInstance();

        // 这里应该注册具体的翻译器Cube
        // cubeManager.registerCube(new FrenchTranslatorCube());
        // cubeManager.registerCube(new JapaneseTranslatorCube());
        // cubeManager.registerCube(new EnglishTranslatorCube());
        // cubeManager.registerCube(new GermanTranslatorCube());

        cubeManager.printSystemOverview();
    }

    private static void demonstrateDynamicSwitching() {
        System.out.println("\n--- 动态切换演示 ---");

        InternationalizationService service = new InternationalizationService();
        String testMessage = "hello";

        System.out.println("同一消息的不同翻译:");

        // 切换到不同的翻译器Cube
        switchToCube(new GUUID("frenchTranslatorCube"));
        String french = service.processUserMessage(testMessage);
        System.out.println("法语: " + french);

        switchToCube(new GUUID("japaneseTranslatorCube"));
        String japanese = service.processUserMessage(testMessage);
        System.out.println("日语: " + japanese);

        switchToCube(new GUUID("germanTranslatorCube"));
        String german = service.processUserMessage(testMessage);
        System.out.println("德语: " + german);

        switchToCube(new GUUID("englishTranslatorCube"));
        String english = service.processUserMessage(testMessage);
        System.out.println("英语: " + english);
    }

    /**
     * 切换到指定的翻译器Cube
     */
    public static void switchToCube(GUID cubeId) {
        CodeInjector injector = CodeInjector.getInstance();

        // 直接绑定注入点到指定的Cube
        injector.bindInjectionPointToCube("business.message.translate", cubeId);

        System.out.println("已切换到翻译器: " + cubeId);
    }
}