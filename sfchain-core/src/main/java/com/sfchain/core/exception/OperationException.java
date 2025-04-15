package com.sfchain.core.exception;

import lombok.Getter;

/**
 * 描述: 操作执行异常
 * @author suifeng
 * 日期: 2025/4/15
 */
@Getter
public class OperationException extends SFChainException {
    
    private final String operationName;
    
    public OperationException(String operationName, String message) {
        super(String.format("Error executing operation '%s': %s", operationName, message));
        this.operationName = operationName;
    }
    
    public OperationException(String operationName, String message, Throwable cause) {
        super(String.format("Error executing operation '%s': %s", operationName, message), cause);
        this.operationName = operationName;
    }
}