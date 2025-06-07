package com.tml.mosaic.demo.install;

import com.tml.mosaic.core.constant.InstallType;
import com.tml.mosaic.core.tools.guid.GUUID;
import com.tml.mosaic.cube.Cube;
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

        Cube frenchTranslatorCube = context.getCube(new GUUID("frenchTranslatorCube"));
        System.out.println(frenchTranslatorCube);
    }
}