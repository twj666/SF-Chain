package com.tml.mosaic.core.frame;

import com.tml.mosaic.core.tools.guid.GUID;

import java.lang.reflect.Method;

/**
 * 描述: 基于方法的扩展点实现
 * @author suifeng
 * 日期: 2025/5/27
 */
public class MethodExtensionPoint extends AbstractExtensionPoint {

    private final Cube cube;
    private final Method method;

    public MethodExtensionPoint(Cube cube, Method method, String extensionId, String extensionName, String description) {
        super(extensionId, extensionName, description);
        this.cube = cube;
        this.method = method;
        this.method.setAccessible(true);
    }

    @Override
    public PointResult execute(PointParam input) {
        try {
            Object result = method.invoke(cube, input);

            if (result instanceof PointResult) {
                return (PointResult) result;
            } else {
                return PointResult.success().setValue(result);
            }
        } catch (Exception e) {
            String errorMsg = String.format("扩展点执行失败: %s, 方法: %s, 错误: %s",
                    extensionId, method.getName(), e.getMessage());
            System.err.println(errorMsg);
            return PointResult.failure("EXECUTION_ERROR", errorMsg);
        }
    }

    public GUID getCubeId() {
        return cube.getCubeId();
    }

    public String getMethodName() {
        return method.getName();
    }
}