package com.tml.mosaic.demo;


import com.tml.mosaic.core.frame.CodeInjector;
import com.tml.mosaic.core.frame.Cube;
import com.tml.mosaic.core.frame.CubeManager;
import com.tml.mosaic.demo.cube.EnglishTranslatorCube;
import com.tml.mosaic.demo.cube.FrenchTranslatorCube;
import com.tml.mosaic.demo.cube.GermanTranslatorCube;
import com.tml.mosaic.demo.cube.JapaneseTranslatorCube;

/**
 * 描述: 翻译插件演示程序
 * @author suifeng
 * 日期: 2025/5/27
 */
public class TranslatorDemo {

    public static void main(String[] args) {
        System.out.println("========== 翻译插件演示 ==========");

        // 1. 初始化翻译插件
        initializeTranslators();
        demonstrateDynamicSwitching();

        System.out.println("========== 演示完成 ==========");
    }

    private static void initializeTranslators() {
        System.out.println("\n--- 初始化翻译插件 ---");

        CubeManager cubeManager = CubeManager.getInstance();

        // 注册所有翻译插件
        cubeManager.registerCube(new FrenchTranslatorCube());
        cubeManager.registerCube(new JapaneseTranslatorCube());
        cubeManager.registerCube(new EnglishTranslatorCube());
        cubeManager.registerCube(new GermanTranslatorCube());

        // 显示所有扩展点
        cubeManager.printAllExtensions();
    }


    private static void demonstrateDynamicSwitching() {
        System.out.println("\n--- 动态切换演示 ---");

        InternationalizationService service = new InternationalizationService();
        String testMessage = "hello";

        CodeInjector injector = CodeInjector.getInstance();

        // 翻译器扩展点
        injector.bindInjectionPoint("business.message.translate", "text.translate");

        // 快速切换不同的翻译器
        System.out.println("同一消息的不同翻译:");

        switchToTranslator("frenchTranslatorCube");
        String french = service.processUserMessage(testMessage);
        System.out.println("法语: " + french);

        switchToTranslator("japaneseTranslatorCube");
        String japanese = service.processUserMessage(testMessage);
        System.out.println("日语: " + japanese);

        switchToTranslator("germanTranslatorCube");
        String german = service.processUserMessage(testMessage);
        System.out.println("德语: " + german);

        switchToTranslator("englishTranslatorCube");
        String english = service.processUserMessage(testMessage);
        System.out.println("英语: " + english);
    }


    /**
     * 辅助方法：切换到指定的翻译器
     * 这里通过重新注册来模拟切换，实际应用中应该有更优雅的机制
     */
    private static void switchToTranslator(String translatorCubeId) {
        CubeManager cubeManager = CubeManager.getInstance();

        // 清除现有的扩展点映射
        cubeManager.getExtensionPoints("text.translate").clear();

        // 重新注册指定的翻译器的扩展点
        Cube targetCube = cubeManager.getCube(translatorCubeId);
        if (targetCube != null) {
            // 重新扫描并注册扩展点
            try {
                java.lang.reflect.Method scanMethod = CubeManager.class.getDeclaredMethod("scanAndRegisterExtensions", Cube.class);
                scanMethod.setAccessible(true);
                scanMethod.invoke(cubeManager, targetCube);
            } catch (Exception e) {
                System.err.println("切换翻译器失败: " + e.getMessage());
            }
        }
    }
}