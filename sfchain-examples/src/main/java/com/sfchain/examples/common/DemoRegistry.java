package com.sfchain.examples.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 演示模块注册表
 * 管理所有可用的演示模块
 */
@Component
public class DemoRegistry {

    @Autowired
    private List<DemoRunner> allDemos;

    private final List<DemoRunner> sortedDemos = new ArrayList<>();
    private final Map<String, List<DemoRunner>> demosByCategory = new HashMap<>();

    @PostConstruct
    public void initialize() {
        // 按照order排序所有演示模块
        sortedDemos.addAll(allDemos);
        sortedDemos.sort(Comparator.comparingInt(DemoRunner::getOrder));

        // 按分类组织演示模块
        for (DemoRunner demo : sortedDemos) {
            String category = demo.getCategory();
            demosByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(demo);
        }
    }

    /**
     * 获取所有可用的演示模块
     * @return 演示模块列表
     */
    public List<DemoRunner> getAvailableDemos() {
        return Collections.unmodifiableList(sortedDemos);
    }

    /**
     * 获取指定索引的演示模块
     * @param index 索引
     * @return 演示模块
     */
    public DemoRunner getDemoByIndex(int index) {
        if (index < 0 || index >= sortedDemos.size()) {
            throw new IndexOutOfBoundsException("演示模块索引超出范围");
        }
        return sortedDemos.get(index);
    }

    /**
     * 获取指定分类的演示模块
     * @param category 分类名称
     * @return 演示模块列表
     */
    public List<DemoRunner> getDemosByCategory(String category) {
        return demosByCategory.getOrDefault(category, Collections.emptyList());
    }

    /**
     * 获取所有可用的分类
     * @return 分类列表
     */
    public List<String> getCategories() {
        return new ArrayList<>(demosByCategory.keySet());
    }

    /**
     * 根据标签搜索演示模块
     * @param tag 标签
     * @return 匹配的演示模块列表
     */
    public List<DemoRunner> searchByTag(String tag) {
        return sortedDemos.stream()
                .filter(demo -> Arrays.asList(demo.getTags()).contains(tag))
                .collect(Collectors.toList());
    }
}