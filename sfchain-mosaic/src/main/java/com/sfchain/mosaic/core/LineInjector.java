package com.sfchain.mosaic.core;

import javassist.*;

/**
 * 描述: 代码行注入器
 * @author suifeng
 * 日期: 2025/5/27
 */
public class LineInjector {

    /**
     * 在指定方法的指定行号插入代码
     */
    public static void injectAtLine(String className, String methodName,
                                    int lineNumber, String code) {
        try {
            ClassPool pool = ClassPool.getDefault();

            // 关键修复：确保所有相关类都在ClassPool中
            setupClassPool(pool);

            CtClass ctClass = pool.get(className);
            CtMethod method = ctClass.getDeclaredMethod(methodName);

            // 在指定行号插入代码
            method.insertAt(lineNumber, code);

            // 重新加载类
            ctClass.toClass();
            ctClass.detach();

            System.out.println("代码注入成功: " + className + "." + methodName + " 第" + lineNumber + "行");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("代码注入失败: " + e.getMessage());
        }
    }

    /**
     * 设置ClassPool，确保所有必要的类都可用
     */
    private static void setupClassPool(ClassPool pool) {
        try {
            pool.appendSystemPath();

            ensureClassInPool(pool, "com.sfchain.mosaic.core.MOutput");
            ensureClassInPool(pool, "com.sfchain.mosaic.core.CodeInjector");
            ensureClassInPool(pool, "com.sfchain.mosaic.core.MInput");

        } catch (Exception e) {
            System.err.println("设置ClassPool失败: " + e.getMessage());
        }
    }

    /**
     * 确保类在ClassPool中可用
     */
    private static void ensureClassInPool(ClassPool pool, String className) {
        try {
            CtClass ctClass = pool.get(className);
            System.out.println("类已加载到ClassPool: " + className);
        } catch (NotFoundException e) {
            System.err.println("警告: 无法加载类到ClassPool: " + className);
            // 尝试通过反射加载
            try {
                Class.forName(className);
                System.out.println("通过反射确认类存在: " + className);
            } catch (ClassNotFoundException ex) {
                System.err.println("错误: 类不存在: " + className);
            }
        }
    }
}