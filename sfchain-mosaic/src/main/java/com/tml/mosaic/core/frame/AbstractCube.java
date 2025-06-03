package com.tml.mosaic.core.frame;

import com.tml.mosaic.core.tools.guid.GUID;
import lombok.Data;

/**
 * 描述: AbstractCube抽象类
 * @author suifeng
 * 日期: 2025/5/27
 */
@Data
public abstract class AbstractCube implements Cube {

    protected GUID cubeId;
    protected String version;
    protected String description;
    protected boolean initialized = false;

    public AbstractCube(GUID cubeId, String version, String description) {
        this.cubeId = cubeId;
        this.version = version;
        this.description = description;
    }

    @Override
    public void initialize() {
        this.initialized = true;
        System.out.println("方块初始化完成: " + cubeId);
    }

    @Override
    public void destroy() {
        this.initialized = false;
        System.out.println("方块销毁完成: " + cubeId);
    }

    /**
     * 创建输入参数
     */
    protected PointParam createInput() {
        return new PointParam();
    }

    /**
     * 创建成功输出
     */
    protected PointResult createSuccessOutput() {
        return PointResult.success();
    }

    /**
     * 创建成功输出，带值
     */
    protected PointResult createSuccessOutput(Object value) {
        return PointResult.success().setValue(value);
    }

    /**
     * 创建失败输出
     */
    protected PointResult createFailureOutput(String message) {
        return PointResult.failure(message);
    }

    /**
     * 参数验证
     */
    protected boolean validateRequired(PointParam input, String... requiredKeys) {
        for (String key : requiredKeys) {
            if (!input.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 安全获取整数参数
     */
    protected Integer safeGetInteger(PointParam input, String key, Integer defaultValue) {
        Integer value = input.getInteger(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 安全获取字符串参数
     */
    protected String safeGetString(PointParam input, String key, String defaultValue) {
        String value = input.getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 安全获取双精度参数
     */
    protected Double safeGetDouble(PointParam input, String key, Double defaultValue) {
        Double value = input.getDouble(key);
        return value != null ? value : defaultValue;
    }
}