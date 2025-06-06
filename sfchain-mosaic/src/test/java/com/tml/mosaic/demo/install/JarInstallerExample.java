package com.tml.mosaic.demo.install;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.core.tools.guid.GUUID;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.factory.support.DefaultListableCubeFactory;
import com.tml.mosaic.install.impl.JarCubeInstaller;

/**
 * JAR安装器使用示例
 */
public class JarInstallerExample {
    
    public static void main(String[] args) {

        DefaultListableCubeFactory factory = new DefaultListableCubeFactory();

        // 创建安装器
        JarCubeInstaller installer = new JarCubeInstaller(factory);
        
        try {
            // 安装JAR包
            installer.installCube("F:\\soft-data\\mosic-test-dever-1.0-SNAPSHOT.jar");

            // 查看安装结果
            System.out.println("已安装的JAR包数量: " + installer.getInstalledJars().size());

            Cube germanTranslatorCube = factory.getCube(new GUUID("germanTranslatorCube"));
            System.out.println(germanTranslatorCube);

        } catch (CubeException e) {
            System.err.println("安装失败: " + e.getMessage());
        }
    }
}