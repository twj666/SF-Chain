package com.tml.mosaic.install.support.jar;

import com.tml.mosaic.cube.MCube;
import com.tml.mosaic.cube.MExtension;
import com.tml.mosaic.cube.MExtensionPackage;
import com.tml.mosaic.core.tools.guid.GUUID;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.factory.definition.CubeDefinition;
import com.tml.mosaic.factory.definition.ExtensionPackageDefinition;
import com.tml.mosaic.factory.definition.ExtensionPointDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * 描述: Jar包下Cube类扫描器（扫描Cube类，返回CubeDefinition列表）
 * @author suifeng
 * 日期: 2025/6/3
 */
public class JarCubeClassScanner {

    private final ClassLoader classLoader;
    private final Map<String, Class<?>> classMap = new HashMap<>();

    public JarCubeClassScanner(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 从JAR输入流中扫描Cube类，并返回CubeDefinition列表
     */
    public List<CubeDefinition> scanCubeDefinitions(InputStream jarInputStream) throws IOException {
        List<CubeDefinition> cubeDefinitions = new ArrayList<>();
        scanAllClasses(jarInputStream); // 第一步：扫描所有类

        // 查找Cube类
        for (Class<?> clazz : classMap.values()) {
            if (isValidCubeClass(clazz)) {
                cubeDefinitions.add(createCubeDefinition(clazz));
            }
        }

        return cubeDefinitions;
    }

    /**
     * 扫描JAR中所有类并缓存
     */
    private void scanAllClasses(InputStream jarInputStream) throws IOException {
        try (JarInputStream jis = new JarInputStream(jarInputStream)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (isClassFile(entry)) {
                    String className = extractClassName(entry.getName());
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        classMap.put(className, clazz);
                    } catch (ClassNotFoundException e) {
                        System.out.println("类加载失败: " + className);
                    }
                }
            }
        }
    }

    private CubeDefinition createCubeDefinition(Class<?> cubeClass) {
        // 创建CubeDefinition
        MCube mCube = cubeClass.getAnnotation(MCube.class);
        String id = mCube.value();
        String name = mCube.name().isEmpty() ? cubeClass.getSimpleName() : mCube.name();

        CubeDefinition definition = new CubeDefinition(
                new GUUID(id),
                name,
                mCube.version(),
                mCube.description(),
                mCube.model(),
                cubeClass.getName(),
                classLoader
        );

        // 扫描扩展包
        scanExtensionPackages(cubeClass, definition);
        return definition;
    }

    private void scanExtensionPackages(Class<?> cubeClass, CubeDefinition cubeDef) {
        // 获取Cube类所在包
        String basePackage = cubeClass.getPackage().getName();

        // 扫描同包下的类
        for (Class<?> clazz : classMap.values()) {
            // 检查是否在同一个包下
            if (clazz.getPackage().getName().startsWith(basePackage)) {
                MExtensionPackage pkgAnno = clazz.getAnnotation(MExtensionPackage.class);
                if (pkgAnno != null && pkgAnno.cubeId().equals(cubeDef.getId().toString())) {
                    // 创建扩展包定义
                    ExtensionPackageDefinition pkgDef = new ExtensionPackageDefinition(
                            pkgAnno.value(),
                            pkgAnno.name(),
                            pkgAnno.description(),
                            pkgAnno.version(),
                            clazz.getName(),
                            pkgAnno.cubeId()
                    );

                    // 扫描扩展点
                    scanExtensionPoints(clazz, pkgDef);
                    cubeDef.addExtensionPackage(pkgDef);
                }
            }
        }
    }

    private void scanExtensionPoints(Class<?> pkgClass, ExtensionPackageDefinition pkgDef) {
        for (Method method : pkgClass.getDeclaredMethods()) {
            MExtension extension = method.getAnnotation(MExtension.class);
            if (extension != null) {
                ExtensionPointDefinition epd = new ExtensionPointDefinition(
                        extension.value(),
                        method.getName(),
                        extension.name(),
                        extension.priority(),
                        extension.description(),
                        false,
                        method.getReturnType(),
                        method.getParameterTypes()
                );
                pkgDef.addExtensionPoint(epd);
            }
        }
    }

    private boolean isClassFile(JarEntry entry) {
        return !entry.isDirectory() && entry.getName().endsWith(".class");
    }

    private String extractClassName(String entryName) {
        return entryName.replace('/', '.').substring(0, entryName.length() - 6);
    }

    /**
     * 加载并验证Cube类
     */
    @SuppressWarnings("unchecked")
    private Class<? extends Cube> loadAndValidateCubeClass(String className) {
        try {
            Class<?> clazz = classLoader.loadClass(className);
            if (isValidCubeClass(clazz)) {
                return (Class<? extends Cube>) clazz;
            }
        } catch (Exception e) {
            System.out.println("跳过类: " + className + " (原因: " + e.getMessage() + ")");
        }
        return null;
    }

    /**
     * 验证是否是有效的Cube类
     */
    private boolean isValidCubeClass(Class<?> clazz) {
        // 1. 必须实现Cube接口
        if (!Cube.class.isAssignableFrom(clazz)) {
            return false;
        }

        // 2. 必须有@MCube注解
        if (!clazz.isAnnotationPresent(MCube.class)) {
            return false;
        }

        // 3. 不能是抽象类或接口
        if (clazz.isInterface() ||
                java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
            return false;
        }
        return true;
    }
}