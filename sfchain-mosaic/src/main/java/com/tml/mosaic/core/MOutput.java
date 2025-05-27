package com.tml.mosaic.core;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述: 扩展点输出结果封装
 * @author suifeng
 * 日期: 2025/5/27
 */
@Data
public class MOutput {
    
    private final Map<String, Object> results = new HashMap<>();
    private boolean success = true;
    private String message = "执行成功";
    private String errorCode;
    
    public MOutput() {}
    
    public MOutput(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    /**
     * 创建成功结果
     */
    public static MOutput success() {
        return new MOutput(true, "执行成功");
    }
    
    /**
     * 创建成功结果，带消息
     */
    public static MOutput success(String message) {
        return new MOutput(true, message);
    }
    
    /**
     * 创建失败结果
     */
    public static MOutput failure(String message) {
        return new MOutput(false, message);
    }
    
    /**
     * 创建失败结果，带错误码
     */
    public static MOutput failure(String errorCode, String message) {
        MOutput output = new MOutput(false, message);
        output.setErrorCode(errorCode);
        return output;
    }
    
    /**
     * 设置结果数据
     */
    public MOutput setResult(String key, Object value) {
        results.put(key, value);
        return this;
    }
    
    /**
     * 获取结果数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getResult(String key) {
        return (T) results.get(key);
    }
    
    /**
     * 获取结果数据，带默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getResult(String key, T defaultValue) {
        Object value = results.get(key);
        return value != null ? (T) value : defaultValue;
    }
    
    /**
     * 设置主要返回值（便捷方法）
     */
    public MOutput setValue(Object value) {
        results.put("value", value);
        return this;
    }
    
    /**
     * 获取主要返回值
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) results.get("value");
    }
    
    /**
     * 获取主要返回值，带默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(T defaultValue) {
        Object value = results.get("value");
        return value != null ? (T) value : defaultValue;
    }

    
    public MOutput setSuccess(boolean success) {
        this.success = success;
        return this;
    }
    
    public MOutput setMessage(String message) {
        this.message = message;
        return this;
    }
    
    public MOutput setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }
    
    public Map<String, Object> getAllResults() {
        return new HashMap<>(results);
    }
    
    @Override
    public String toString() {
        return "MOutput{" +
                "成功=" + success +
                ", 消息='" + message + '\'' +
                ", 错误码='" + errorCode + '\'' +
                ", 结果=" + results +
                '}';
    }
}