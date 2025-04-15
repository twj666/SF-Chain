package com.sfchain.core.exception;

import lombok.Getter;

/**
 * 描述: 模型调用异常
 * @author suifeng
 * 日期: 2025/4/15
 */
@Getter
public class ModelException extends SFChainException {
    
    private final String modelName;
    
    public ModelException(String modelName, String message) {
        super(String.format("Error invoking model '%s': %s", modelName, message));
        this.modelName = modelName;
    }
    
    public ModelException(String modelName, String message, Throwable cause) {
        super(String.format("Error invoking model '%s': %s", modelName, message), cause);
        this.modelName = modelName;
    }
}