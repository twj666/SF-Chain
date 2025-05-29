package com.tml.mosaic.core;

import com.tml.mosaic.core.guid.GUID;
import com.tml.mosaic.core.guid.GUUID;
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
    protected MInput createInput() {
        return new MInput();
    }

    /**
     * 创建成功输出
     */
    protected MOutput createSuccessOutput() {
        return MOutput.success();
    }

    /**
     * 创建成功输出，带值
     */
    protected MOutput createSuccessOutput(Object value) {
        return MOutput.success().setValue(value);
    }

    /**
     * 创建失败输出
     */
    protected MOutput createFailureOutput(String message) {
        return MOutput.failure(message);
    }

    /**
     * 参数验证
     */
    protected boolean validateRequired(MInput input, String... requiredKeys) {
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
    protected Integer safeGetInteger(MInput input, String key, Integer defaultValue) {
        Integer value = input.getInteger(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 安全获取字符串参数
     */
    protected String safeGetString(MInput input, String key, String defaultValue) {
        String value = input.getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 安全获取双精度参数
     */
    protected Double safeGetDouble(MInput input, String key, Double defaultValue) {
        Double value = input.getDouble(key);
        return value != null ? value : defaultValue;
    }
}