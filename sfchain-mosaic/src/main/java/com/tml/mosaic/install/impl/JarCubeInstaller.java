package com.tml.mosaic.install.impl;

import com.tml.mosaic.core.constant.InstallType;
import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.factory.CubeDefinition;
import com.tml.mosaic.factory.config.CubeDefinitionRegistry;
import com.tml.mosaic.factory.io.loader.ResourceLoader;
import com.tml.mosaic.factory.io.resource.Resource;
import com.tml.mosaic.install.support.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述: 从Jar包安装Cube的安装器
 * @author suifeng
 * 日期: 2025/5/29 
 */
public class JarCubeInstaller extends AbstractCubeInstaller {

    private final Map<String, JarPluginClassLoader> classLoaderRegistry;

    public JarCubeInstaller() {
        super();
        this.classLoaderRegistry = new ConcurrentHashMap<>();
    }

    public JarCubeInstaller(ResourceLoader resourceLoader) {
        super(resourceLoader);
        this.classLoaderRegistry = new ConcurrentHashMap<>();
    }

    @Override
    public String getInstallerType() {
        return InstallType.JAR_INSTALL;
    }

    @Override
    public List<CubeDefinition> installCube(Resource resource) throws CubeException {
        try {
            try (InputStream inputStream = resource.getInputStream()) {
                return doInstallCubeByJar(resource, inputStream);
            }
        } catch (IOException e) {
            throw new CubeException("IOException reading Jar Cube from " + resource, e);
        }
    }

    protected List<CubeDefinition> doInstallCubeByJar(Resource resource, InputStream inputStream) throws CubeException {
        String jarPath = resource.getPath();
        System.out.println("开始安装JAR包Cube: " + jarPath);

        JarPluginClassLoader classLoader = null;
        try {
            // 1. 创建专用类加载器
            classLoader = createJarClassLoader(jarPath);

            // 2. 扫描Cube定义
            JarCubeClassScanner scanner = new JarCubeClassScanner(classLoader);
            List<CubeDefinition> cubeDefinitions = scanner.scanCubeDefinitions(inputStream);

            if (cubeDefinitions.isEmpty()) {
                throw new CubeException("JAR包中未发现有效的Cube定义: " + jarPath);
            }

            // 3. 注册类加载器
            classLoaderRegistry.put(jarPath, classLoader);

            System.out.println("JAR包安装完成: " + jarPath + ", 共安装 " + cubeDefinitions.size() + " 个Cube定义");

            return cubeDefinitions;

        } catch (Exception e) {
            // 异常时清理资源
            if (classLoader != null) {
                classLoader.close();
            }
            throw new CubeException("安装JAR包失败: " + jarPath, e);
        }
    }

    /**
     * 创建JAR专用类加载器
     */
    private JarPluginClassLoader createJarClassLoader(String jarPath) throws CubeException {
        try {
            File jarFile = new File(jarPath);
            if (!jarFile.exists() || !jarFile.isFile()) {
                throw new CubeException("JAR文件不存在或不是有效文件: " + jarPath);
            }

            URL jarUrl = jarFile.toURI().toURL();
            return new JarPluginClassLoader(
                    jarPath,
                    new URL[]{jarUrl},
                    Thread.currentThread().getContextClassLoader()
            );

        } catch (Exception e) {
            throw new CubeException("创建JAR类加载器失败: " + jarPath, e);
        }
    }

    /**
     * 获取已安装的JAR包信息
     */
    public Map<String, JarPluginClassLoader> getInstalledJars() {
        return new ConcurrentHashMap<>(classLoaderRegistry);
    }
}
