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
        initializeMetaData();
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

    protected void initializeMetaData() {
        metaData.setName(this.getClass().getSimpleName());
        metaData.setDescription("默认Cube描述");
        metaData.setVersion("1.0.0");
    }

    public PointResult executeExtensionPoint(String injectionPointId, PointParam input) {
        try {
            ExtensionPoint extensionPoint = findBestMatchExtensionPoint(injectionPointId);

            if (extensionPoint == null) {
                log.debug("✗ 扩展点未找到 | Cube[{}] | 注入点: {}", getCubeId(), injectionPointId);
                return PointResult.failure("EXTENSION_NOT_FOUND", "未找到匹配的扩展点");
            }

            log.debug("  → 执行扩展点 | {} [{}] | 优先级: {}",
                    extensionPoint.getExtensionId(), extensionPoint.getExtensionName(), extensionPoint.getPriority());

            return extensionPoint.execute(input);

        } catch (Exception e) {
            log.error("✗ 扩展点执行异常 | Cube[{}] | 注入点: {} | 异常: {}",
                    getCubeId(), injectionPointId, e.getMessage(), e);
            return PointResult.failure("CUBE_EXECUTION_ERROR", "Cube执行异常: " + e.getMessage());
        }
    }

    protected ExtensionPoint findBestMatchExtensionPoint(String injectionPointId) {
        ExtensionPoint directMatch = metaData.findExtensionPoint(new DotNotationId(injectionPointId));
        if (directMatch != null) {
            return directMatch;
        }

        List<ExtensionPoint> availablePoints = metaData.getExtensionPoints();
        if (!availablePoints.isEmpty()) {
            return availablePoints.stream()
                    .min((ep1, ep2) -> Integer.compare(ep1.getPriority(), ep2.getPriority()))
                    .orElse(null);
        }

        return null;
    }

    public boolean hasExtensionPoint(String extensionId) {
        return metaData.findExtensionPoint(new DotNotationId(extensionId)) != null;
    }

    public GUID getCubeId() {
        return getId();
    }

    @Data
    public static class MetaData {
        private String name;
        private String version;
        private String description;
        private String model;
        private List<ExtensionPoint> extensionPoints = new CopyOnWriteArrayList<>();
        private Map<GUID, ExtensionPoint> extensionMap = new ConcurrentHashMap<>();

        public MetaData(String name, String version, String description, String model) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.model = model;
        }

        public MetaData() {}

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

        public void removeExtensionPoint(GUID extensionPointId) {
            ExtensionPoint removed = extensionMap.remove(extensionPointId);
            if (removed != null) {
                extensionPoints.remove(removed);
            }
        }
    }
}