package com.suifeng.sfchain.persistence.config;

/**
 * 数据库类型枚举
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
public enum DatabaseType {

    MYSQL("mysql"),
    POSTGRESQL("postgresql");
    
    private final String value;
    
    DatabaseType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * 从字符串转换为枚举
     */
    public static DatabaseType fromString(String value) {
        if (value == null) {
            return MYSQL; // 默认值
        }
        
        for (DatabaseType type : DatabaseType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("不支持的数据库类型: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}