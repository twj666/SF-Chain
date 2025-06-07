package com.tml.mosaic.cube;

import com.tml.mosaic.core.infrastructure.CommonComponent;
import com.tml.mosaic.core.tools.guid.DotNotationId;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.core.tools.guid.UniqueEntity;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 方块抽象基类 - 所有插件必须继承此类
 */
@Slf4j
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

    public void initialize() {
        if (!initialized) {
            doInitialize();
            initialized = true;
            log.info("✓ 方块初始化 | ID: {} | 名称: {}", getId(), metaData.getName());
        }
    }

    public void destroy() {
        if (initialized) {
            doDestroy();
            initialized = false;
            log.info("✓ 方块销毁 | ID: {} | 名称: {}", getId(), metaData.getName());
        }
    }

    protected void doInitialize() {}
    protected void doDestroy() {}

    public GUID getCubeId() {
        return getId();
    }

    @Data
    public static class MetaData {
        private String name;
        private String version;
        private String description;
        private String model;

        // 扩展包元数据
        private final List<ExtensionPackage> extensionPackages = new CopyOnWriteArrayList<>();
        private final Map<GUID, ExtensionPackage> extensionPackageMap = new ConcurrentHashMap<>();
    }

    public void addExtensionPackage(ExtensionPackage extensionPackage) {
        getMetaData().extensionPackages.add(extensionPackage);
        getMetaData().extensionPackageMap.put(extensionPackage.getId(), extensionPackage);
    }

    public ExtensionPackage getExtensionPackage(GUID packageId) {
        return getMetaData().extensionPackageMap.get(packageId);
    }
}