package com.tml.mosaic.core.frame;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述: 扩展点输入参数封装
 * @author suifeng
 * 日期: 2025/5/27
 */
public class PointParam {
    
    private final Map<String, Object> parameters = new HashMap<>();
    
    public PointParam() {}
    
    public PointParam(Map<String, Object> parameters) {
        if (parameters != null) {
            this.parameters.putAll(parameters);
        }
    }
    
    /**
     * 设置参数
     */
    public PointParam set(String key, Object value) {
        parameters.put(key, value);
        return this;
    }
    
    /**
     * 获取参数
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) parameters.get(key);
    }
    
    /**
     * 获取参数，带默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object value = parameters.get(key);
        return value != null ? (T) value : defaultValue;
    }
    
    /**
     * 获取字符串参数
     */
    public String getString(String key) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 获取整数参数
     */
    public Integer getInteger(String key) {
        Object value = parameters.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * 获取双精度参数
     */
    public Double getDouble(String key) {
        Object value = parameters.get(key);
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * 获取布尔参数
     */
    public Boolean getBoolean(String key) {
        Object value = parameters.get(key);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return false;
    }
    
    /**
     * 检查参数是否存在
     */
    public boolean containsKey(String key) {
        return parameters.containsKey(key);
    }
    
    /**
     * 获取所有参数
     */
    public Map<String, Object> getAllParameters() {
        return new HashMap<>(parameters);
    }
    
    /**
     * 参数数量
     */
    public int size() {
        return parameters.size();
    }
    
    @Override
    public String toString() {
        return "PointParam{参数=" + parameters + "}";
    }
}