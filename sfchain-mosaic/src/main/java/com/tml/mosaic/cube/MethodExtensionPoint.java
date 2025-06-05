package com.tml.mosaic.cube;

import com.tml.mosaic.core.tools.guid.GUID;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * 描述: 基于方法的扩展点实现
 * @author suifeng
 * 日期: 2025/5/27
 */
@Slf4j
public class MethodExtensionPoint extends ExtensionPoint {

    private final Cube cube;
    private final Method method;

    public MethodExtensionPoint(Cube cube, Method method, String extensionId, String extensionName, String description) {
        super(extensionId);
        this.cube = cube;
        this.method = method;
        this.method.setAccessible(true);

        setMethodName(method.getName());
        setReturnType(method.getReturnType());
        setParameterTypes(method.getParameterTypes());
        setExtensionName(extensionName);
        setDescription(description);
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
            log.error("✗ 方法扩展点执行失败 | 扩展点: {} | 方法: {} | Cube: {} | 异常: {}",
                    getExtensionId(), method.getName(), cube.getCubeId(), e.getMessage());
            log.debug("方法执行异常详情", e);
            return PointResult.failure("EXECUTION_ERROR", "扩展点执行失败: " + e.getMessage());
        }
    }

    public GUID getCubeId() {
        return cube.getCubeId();
    }
}