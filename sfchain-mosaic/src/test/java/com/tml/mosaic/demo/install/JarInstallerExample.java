package com.tml.mosaic.demo.install;

import com.tml.mosaic.core.constant.InstallType;
import com.tml.mosaic.core.tools.guid.GUUID;
import com.tml.mosaic.cube.*;
import com.tml.mosaic.factory.ClassPathJsonCubeContext;
import com.tml.mosaic.install.impl.JarCubeInstaller;
import com.tml.mosaic.install.support.registry.DefaultInstallerRegistry;

/**
 * JAR安装器使用示例
 */
public class JarInstallerExample {
    
    public static void main(String[] args) {
        // 创建注册表
        DefaultInstallerRegistry installerRegistry = new DefaultInstallerRegistry();
        installerRegistry.registerInstaller(InstallType.JAR_INSTALL, new JarCubeInstaller());

        ClassPathJsonCubeContext context = new ClassPathJsonCubeContext("classpath:start1.json", installerRegistry);

        // 获取翻译Cube
        Cube translationCube = context.getCube(new GUUID("translationCube"));

        // 调用英语翻译
        PointParam input = new PointParam().set("text", "你好");

        ExtensionPackage englishTranslation = translationCube.getExtensionPackage(new GUUID("englishTranslation"));

        ExtensionPoint extensionPoint = englishTranslation.findExtensionPoint(new GUUID("text.translate"));

        PointResult result = extensionPoint.execute(input);

        System.out.println(result);
    }
}