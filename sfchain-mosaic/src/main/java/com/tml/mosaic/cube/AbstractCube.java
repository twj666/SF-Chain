package com.tml.mosaic.cube;

import com.tml.mosaic.core.tools.guid.GUID;
import lombok.Data;

/**
 * 描述: AbstractCube抽象类
 * @author suifeng
 * 日期: 2025/5/27
 */
@Data
public abstract class AbstractCube extends Cube {

    public AbstractCube(GUID cubeId, String version, String description) {
        super(cubeId);
        // 初始化MetaData
        getMetaData().setName(this.getClass().getSimpleName());
        getMetaData().setVersion(version);
        getMetaData().setDescription(description);
    }

    // 参数工具方法
    protected PointParam createInput() {
        return new PointParam();
    }

    protected PointResult createSuccessOutput() {
        return PointResult.success();
    }

    protected PointResult createSuccessOutput(Object value) {
        return PointResult.success().setValue(value);
    }

    protected PointResult createFailureOutput(String message) {
        return PointResult.failure(message);
    }

    protected boolean validateRequired(PointParam input, String... requiredKeys) {
        for (String key : requiredKeys) {
            if (!input.containsKey(key) || input.get(key) == null) {
                return false;
            }
        }
        return true;
    }

    // 安全获取参数的方法保持不变
    protected Integer safeGetInteger(PointParam input, String key, Integer defaultValue) {
        Integer value = input.getInteger(key);
        return value != null ? value : defaultValue;
    }

    protected String safeGetString(PointParam input, String key, String defaultValue) {
        String value = input.getString(key);
        return value != null ? value : defaultValue;
    }

    protected Double safeGetDouble(PointParam input, String key, Double defaultValue) {
        Double value = input.getDouble(key);
        return value != null ? value : defaultValue;
    }
}