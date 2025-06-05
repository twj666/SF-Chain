package com.tml.mosaic.cube;

import com.tml.mosaic.core.infrastructure.CommonComponent;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.core.tools.guid.UniqueEntity;
import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 方块抽象基类 - 所有插件必须继承此类
 */
public abstract class Cube extends UniqueEntity {

    @Getter
    private MetaData metaData;

    protected volatile boolean initialized = false;

    public Cube(GUID id) {
        super(id);
        this.metaData = new MetaData();
    }

    public Cube() {
        this(CommonComponent.GuidAllocator().nextGUID());
    }

    // 生命周期方法
    public void initialize() {
        if (!initialized) {
            doInitialize();
            initialized = true;
            System.out.println("方块初始化完成: " + getId() + " [" + metaData.getDescription() + "]");
        }
    }

    public void destroy() {
        if (initialized) {
            doDestroy();
            initialized = false;
            System.out.println("方块销毁完成: " + getId());
        }
    }

    // 子类可覆盖的初始化和销毁方法
    protected void doInitialize() {}
    protected void doDestroy() {}

    // 获取Cube ID的便捷方法
    public GUID getCubeId() {
        return getId();
    }

    @Data
    public static class MetaData {

        private String name;
        private String version;
        private String description;
        private List<ExtensionPoint> extensionPoints = new CopyOnWriteArrayList<>();
        private Map<GUID, ExtensionPoint> extensionMap = new ConcurrentHashMap<>();

        public ExtensionPoint findExtensionPoint(GUID guid) {
            return extensionMap.computeIfAbsent(guid, key ->
                    extensionPoints.stream()
                            .filter(ep -> ep.getId().equals(key))
                            .findFirst()
                            .orElse(null)
            );
        }

        public void addExtensionPoint(ExtensionPoint extensionPoint) {
            extensionPoints.add(extensionPoint);
            extensionMap.put(extensionPoint.getId(), extensionPoint);
        }
    }
}