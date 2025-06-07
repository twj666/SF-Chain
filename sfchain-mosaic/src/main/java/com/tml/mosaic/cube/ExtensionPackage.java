package com.tml.mosaic.cube;

import com.tml.mosaic.core.tools.guid.GUID;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 描述: 扩展包抽象基类
 * @author suifeng
 * 日期: 2025/6/7
 */
@Getter
@Setter
public abstract class ExtensionPackage {

    private GUID id;
    private String name;
    private String description;
    private String version;
    private Cube cube; // 关联的Cube实例

    // 扩展点元数据
    private final List<ExtensionPoint> extensionPoints = new CopyOnWriteArrayList<>();
    private final Map<GUID, ExtensionPoint> extensionPointMap = new ConcurrentHashMap<>();

    public ExtensionPackage(Cube cube) {
        this.id = cube.getId();
        this.cube = cube;
    }

    public ExtensionPackage() {
    }

    public void addExtensionPoint(ExtensionPoint extensionPoint) {
        extensionPoints.add(extensionPoint);
        extensionPointMap.put(extensionPoint.getExtensionId(), extensionPoint);
    }

    public ExtensionPoint findExtensionPoint(GUID extensionPointId) {
        return extensionPointMap.get(extensionPointId);
    }
}