package com.tml.mosaic.install.support.jar;

import com.tml.mosaic.cube.MCube;
import com.tml.mosaic.core.tools.guid.GUUID;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.factory.CubeDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * 描述: Jar包下Cube类扫描器（扫描Cube类，返回CubeDefinition列表）
 * @author suifeng
 * 日期: 2025/6/3
 */
public class JarCubeClassScanner {
    
    private final ClassLoader classLoader;
    
    public JarCubeClassScanner(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 从JAR输入流中扫描Cube类，并返回CubeDefinition列表
     */
    public List<CubeDefinition> scanCubeDefinitions(InputStream jarInputStream) throws IOException {
        List<CubeDefinition> cubeDefinitions = new ArrayList<>();

        try (JarInputStream jis = new JarInputStream(jarInputStream)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (isClassFile(entry)) {
                    String className = extractClassName(entry.getName());
                    Class<?> clazz = loadAndValidateCubeClass(className);
                    if (clazz != null) {
                        // 创建CubeDefinition
                        MCube mCube = clazz.getAnnotation(MCube.class);
                        String id = mCube.value();
                        String name = mCube.name().isEmpty() ? clazz.getSimpleName() : mCube.value();
                        String version = mCube.version();
                        String description = mCube.description();
                        String model = mCube.model();

                        CubeDefinition cubeDefinition = new CubeDefinition(
                                new GUUID(id),
                                name,
                                version,
                                description,
                                model,
                                className,
                                this.classLoader   // 当前类加载器
                        );

                        cubeDefinitions.add(cubeDefinition);
                        System.out.println("发现Cube: " + className);
                    }
                }
            }
        }
        return cubeDefinitions;
    }

    /**
     * 判断是否是Class文件
     */
    private boolean isClassFile(JarEntry entry) {
        String name = entry.getName();
        return !entry.isDirectory() && 
               name.endsWith(".class") && 
               !name.contains("$"); // 排除内部类
    }
    
    /**
     * 提取类名
     */
    private String extractClassName(String entryName) {
        return entryName.replace('/', '.')
                       .replace(".class", "");
    }
    
    /**
     * 加载并验证Cube类
     */
    @SuppressWarnings("unchecked")
    private Class<? extends Cube> loadAndValidateCubeClass(String className) {
        try {
            Class<?> clazz = classLoader.loadClass(className);
            
            // 验证是否是有效的Cube类
            if (isValidCubeClass(clazz)) {
                return (Class<? extends Cube>) clazz;
            }
        } catch (Exception e) {
            // 忽略无法加载或验证失败的类
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
        
        // 4. 必须有无参构造函数
        try {
            clazz.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            System.out.println("Cube类缺少无参构造函数: " + clazz.getName());
            return false;
        }
    }
}