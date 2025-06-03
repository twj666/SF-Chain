package com.tml.mosaic.install.impl;

import com.tml.mosaic.core.constant.InstallType;
import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.core.frame.Cube;
import com.tml.mosaic.install.io.loader.ResourceLoader;
import com.tml.mosaic.install.io.resource.Resource;
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

    private final CubeInstanceFactory instanceFactory;
    private final Map<String, JarPluginClassLoader> classLoaderRegistry;

    public JarCubeInstaller(CubeRegistry registry) {
        super(registry);
        this.instanceFactory = new CubeInstanceFactory();
        this.classLoaderRegistry = new ConcurrentHashMap<>();
    }

    public JarCubeInstaller(CubeRegistry registry, ResourceLoader resourceLoader) {
        super(registry, resourceLoader);
        this.instanceFactory = new CubeInstanceFactory();
        this.classLoaderRegistry = new ConcurrentHashMap<>();
    }

    @Override
    public String getInstallerType() {
        return InstallType.JAR_INSTALL;
    }

    @Override
    public void installCube(Resource resource) throws CubeException {
        try {
            try (InputStream inputStream = resource.getInputStream()) {
                doInstallCubeByJar(resource, inputStream);
            }
        } catch (IOException e) {
            throw new CubeException("IOException reading Jar Cube from " + resource, e);
        }
    }

    /**
     * 通过Jar包的方式加载Cube
     */
    protected void doInstallCubeByJar(Resource resource, InputStream inputStream) throws CubeException {
        String jarPath = resource.getPath();
        System.out.println("开始安装JAR包Cube: " + jarPath);

        JarPluginClassLoader classLoader = null;
        try {
            // 1. 创建专用类加载器
            classLoader = createJarClassLoader(jarPath);

            // 2. 扫描Cube类
            CubeClassScanner scanner = new CubeClassScanner(classLoader);
            List<Class<? extends Cube>> cubeClasses = scanner.scanCubeClasses(inputStream);

            if (cubeClasses.isEmpty()) {
                throw new CubeException("JAR包中未发现有效的Cube类: " + jarPath);
            }

            // 3. 批量实例化和注册Cube
            installCubeClasses(cubeClasses);

            // 4. 注册类加载器
            classLoaderRegistry.put(jarPath, classLoader);

            System.out.println("JAR包安装完成: " + jarPath +
                    ", 共安装 " + cubeClasses.size() + " 个Cube");

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
     * 批量安装Cube类
     */
    private void installCubeClasses(List<Class<? extends Cube>> cubeClasses) throws CubeException {
        int successCount = 0;
        int failureCount = 0;

        for (Class<? extends Cube> cubeClass : cubeClasses) {
            try {
                Cube cube = instanceFactory.createCubeInstance(cubeClass);
                getRegistry().registerCube(cube);
                successCount++;

            } catch (Exception e) {
                failureCount++;
                System.err.println("安装Cube失败: " + cubeClass.getName() +
                        ", 错误: " + e.getMessage());
                // 继续安装其他Cube，不中断整个过程
            }
        }

        if (successCount == 0) {
            throw new CubeException("所有Cube安装失败，成功: " + successCount +
                    ", 失败: " + failureCount);
        }

        if (failureCount > 0) {
            System.out.println("部分Cube安装失败，成功: " + successCount +
                    ", 失败: " + failureCount);
        }
    }

    /**
     * 卸载JAR包
     */
    public void uninstallJar(String jarPath) {
        JarPluginClassLoader classLoader = classLoaderRegistry.remove(jarPath);
        if (classLoader != null) {
            classLoader.close();
            System.out.println("JAR包卸载完成: " + jarPath);
        }
    }

    /**
     * 获取已安装的JAR包信息
     */
    public Map<String, JarPluginClassLoader> getInstalledJars() {
        return new ConcurrentHashMap<>(classLoaderRegistry);
    }

    /**
     * 清理所有资源
     */
    public void cleanup() {
        classLoaderRegistry.values().forEach(JarPluginClassLoader::close);
        classLoaderRegistry.clear();
        System.out.println("JAR安装器资源清理完成");
    }
}
