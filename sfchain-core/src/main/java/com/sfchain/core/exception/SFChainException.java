package com.sfchain.core.exception;

/**
 * 描述: SFChain框架基础异常
 * @author suifeng
 * 日期: 2025/4/15
 */
public class SFChainException extends RuntimeException {
    
    public SFChainException(String message) {
        super(message);
    }
    
    public SFChainException(String message, Throwable cause) {
        super(message, cause);
    }
}
