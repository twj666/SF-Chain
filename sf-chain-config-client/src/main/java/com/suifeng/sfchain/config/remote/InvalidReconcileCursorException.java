package com.suifeng.sfchain.config.remote;

/**
 * finalize 对账游标无效异常
 */
public class InvalidReconcileCursorException extends RuntimeException {

    public InvalidReconcileCursorException(String message) {
        super(message);
    }
}
