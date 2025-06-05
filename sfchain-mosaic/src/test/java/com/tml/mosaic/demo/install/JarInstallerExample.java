package com.tml.mosaic.demo.install;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.cube.CubeManager;
import com.tml.mosaic.core.tools.guid.GUUID;
import com.tml.mosaic.demo.InternationalizationService;
import com.tml.mosaic.install.impl.JarCubeInstaller;

import static com.tml.mosaic.demo.TranslatorDemo.switchToCube;

/**
 * JAR安装器使用示例
 */
public class JarInstallerExample {
    
    public static void main(String[] args) {
        // 创建安装器
        JarCubeInstaller installer = new JarCubeInstaller(CubeManager.getInstance());
        
        try {
            // 安装JAR包
            installer.installCube("F:\\Code\\mosic-test-dever-1.0-SNAPSHOT.jar");
            
            // 查看安装结果
            System.out.println("已安装的JAR包数量: " + installer.getInstalledJars().size());
            
            // 打印所有扩展点信息
            CubeManager.getInstance().printAllCubesAndExtensions();

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
            
        } catch (CubeException e) {
            System.err.println("安装失败: " + e.getMessage());
        } finally {
            // 清理资源
            installer.cleanup();
        }
    }
}