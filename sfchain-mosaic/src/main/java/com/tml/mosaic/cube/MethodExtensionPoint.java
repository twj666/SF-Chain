package com.tml.mosaic.cube;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 描述: 基于方法的扩展点实现
 * @author suifeng
 * 日期: 2025/5/27
 */
public class MethodExtensionPoint extends ExtensionPoint {

    private final Object target;  // 执行目标（Cube或ExtensionPackage）
    private final Method method;

    public MethodExtensionPoint(Object target, Method method, String id, String name, String description) {
        super(id, name, description);
        this.target = target;
        this.method = method;
        this.method.setAccessible(true);
    }

    @Override
    public PointResult execute(PointParam input) {
        try {
            Object result = method.invoke(target, input);
            if (result instanceof PointResult) {
                return (PointResult) result;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return PointResult.failure("EXECUTION_ERROR", cause.getMessage());
        }
        return PointResult.failure("执行失败");
    }

    // 获取关联的Cube（如果目标是ExtensionPackage）
    public Cube getCube() {
        if (target instanceof ExtensionPackage) {
            return ((ExtensionPackage) target).getCube();
        }
        return (Cube) target;
    }
}