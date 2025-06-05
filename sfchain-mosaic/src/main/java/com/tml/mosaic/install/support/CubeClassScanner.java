package com.tml.mosaic.install.support;

import com.tml.mosaic.core.annotation.MCube;
import com.tml.mosaic.cube.Cube;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * 描述: Cube类发现器
 * @author suifeng
 * 日期: 2025/6/3
 */
public class CubeClassScanner {
    
    private final ClassLoader classLoader;
    
    public CubeClassScanner(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    
    /**
     * 从JAR输入流中扫描Cube类
     */
    public List<Class<? extends Cube>> scanCubeClasses(InputStream jarInputStream) throws IOException {
        List<Class<? extends Cube>> cubeClasses = new ArrayList<>();
        
        try (JarInputStream jis = new JarInputStream(jarInputStream)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (isClassFile(entry)) {
                    String className = extractClassName(entry.getName());
                    Class<? extends Cube> cubeClass = loadAndValidateCubeClass(className);
                    if (cubeClass != null) {
                        cubeClasses.add(cubeClass);
                        System.out.println("发现Cube类: " + className);
                    }
                }
            }
        }
        
        return cubeClasses;
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