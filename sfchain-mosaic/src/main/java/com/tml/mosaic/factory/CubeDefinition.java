package com.tml.mosaic.factory;

import com.tml.mosaic.core.tools.guid.GUID;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
/**
 * 描述: Cube的定义信息
 * @author suifeng
 * 日期: 2025/6/6
 */
@Data
@Slf4j
public class CubeDefinition {
    private GUID id;
    private String name;
    private String version;
    private String description;
    private String model;
    private String className;
    private transient ClassLoader classLoader;
    private final List<ExtensionPointDefinition> extensionPoints = new ArrayList<>();

    public CubeDefinition(GUID id, String name, String version,
                          String description, String model,
                          String className, ClassLoader classLoader) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.model = model;
        this.className = className;
        this.classLoader = classLoader;
    }

    // 添加扩展点定义
    public void addExtensionPoint(ExtensionPointDefinition epd) {
        extensionPoints.add(epd);
    }
}