package com.tml.mosaic.demo.install;

import com.tml.mosaic.core.constant.InstallType;
import com.tml.mosaic.factory.json.JsonCubeDefinitionReader;
import com.tml.mosaic.factory.support.DefaultListableCubeFactory;
import com.tml.mosaic.install.impl.JarCubeInstaller;
import com.tml.mosaic.install.support.DefaultInstallerRegistry;

/**
 * JAR安装器使用示例
 */
public class JarInstallerExample {
    
    public static void main(String[] args) {

        // 创建注册表
        DefaultInstallerRegistry installerRegistry = new DefaultInstallerRegistry();
        installerRegistry.registerInstaller(InstallType.JAR_INSTALL, new JarCubeInstaller());

        // 创建工厂
        DefaultListableCubeFactory factory = new DefaultListableCubeFactory();

        // 创建JSON读取器
        JsonCubeDefinitionReader reader = new JsonCubeDefinitionReader(factory, installerRegistry);

        // 加载配置
        reader.loadCubeDefinitions("D:\\mosic-data-test\\start.json");
    }
}