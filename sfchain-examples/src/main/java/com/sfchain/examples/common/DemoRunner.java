package com.sfchain.examples.common;

import java.util.Scanner;

/**
 * 演示模块接口
 * 所有演示场景都需要实现此接口
 */
public interface DemoRunner {
    
    /**
     * 获取演示名称
     * @return 演示名称
     */
    String getName();
    
    /**
     * 获取演示描述
     * @return 演示描述
     */
    String getDescription();
    
    /**
     * 获取演示图标
     * @return 图标字符
     */
    String getIcon();
    
    /**
     * 运行演示
     * @param scanner 用于接收用户输入
     * @param ui 用于显示界面
     */
    void run(Scanner scanner, ConsoleUI ui);
    
    /**
     * 获取演示分类
     * @return 分类名称
     */
    default String getCategory() {
        return "通用";
    }
    
    /**
     * 获取演示排序权重（数字越小越靠前）
     * @return 排序权重
     */
    default int getOrder() {
        return 100;
    }
    
    /**
     * 获取演示标签
     * @return 标签数组
     */
    default String[] getTags() {
        return new String[0];
    }
    
    /**
     * 清理资源
     * 在演示结束时调用
     */
    default void cleanup() {
        // 默认不做任何操作
    }
}