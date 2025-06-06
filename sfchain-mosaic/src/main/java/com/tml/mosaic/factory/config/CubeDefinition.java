package com.tml.mosaic.factory.config;

import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.ExtensionPoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 描述: Cube的定义信息
 * @author suifeng
 * 日期: 2025/6/6
 */
@AllArgsConstructor
@Data
@Slf4j
public class CubeDefinition {

    private GUID id;
    private String name;
    private String version;
    private String description;
    private String model;
    private final List<ExtensionPoint> extensionPoints = new CopyOnWriteArrayList<>();

    // Cube类的全限定名
    private String className;
    // 类加载器
    private transient ClassLoader classLoader;

    public CubeDefinition(GUID id, String className, ClassLoader classLoader) {
        this.id = id;
        this.className = className;
        this.classLoader = classLoader;
    }

    public void setMetadata(String name, String version, String description, String model) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.model = model;
    }
}