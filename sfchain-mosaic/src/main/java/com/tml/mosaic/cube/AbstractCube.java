package com.tml.mosaic.cube;

import com.tml.mosaic.core.tools.guid.GUID;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 描述: AbstractCube抽象类
 * @author suifeng
 * 日期: 2025/5/27
 */
@Slf4j
@Data
public abstract class AbstractCube extends Cube {

    public AbstractCube(GUID cubeId) {
        super(cubeId);
        getMetaData().setName(this.getClass().getSimpleName());
    }

    public AbstractCube(GUID cubeId, String version, String description) {
        super(cubeId);
        getMetaData().setName(this.getClass().getSimpleName());
        getMetaData().setVersion(version);
        getMetaData().setDescription(description);
    }

    @Override
    protected void doInitialize() {
        log.debug("→ 初始化AbstractCube | 名称: {} | 版本: {}",
                getMetaData().getName(), getMetaData().getVersion());
    }

    @Override
    protected void doDestroy() {
        log.debug("→ 销毁AbstractCube | 名称: {}", getMetaData().getName());
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
                log.warn("⚠ 参数验证失败 | 缺少必需参数: {}", key);
                return false;
            }
        }
        return true;
    }

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