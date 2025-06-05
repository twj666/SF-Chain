package com.tml.mosaic.cube;

import com.tml.mosaic.core.infrastructure.CommonComponent;
import com.tml.mosaic.core.tools.guid.DotNotationId;
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
        initializeMetaData();
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

    /**
     * 初始化元数据，子类可覆盖
     */
    protected void initializeMetaData() {
        metaData.setName(this.getClass().getSimpleName());
        metaData.setDescription("默认Cube描述");
        metaData.setVersion("1.0.0");
    }

    /**
     * 核心方法：执行扩展点
     * 注入点通过此方法委托给具体的扩展点执行
     */
    public PointResult executeExtensionPoint(String injectionPointId, PointParam input) {
        try {
            // 查找最匹配的扩展点
            ExtensionPoint extensionPoint = findBestMatchExtensionPoint(injectionPointId);

            if (extensionPoint == null) {
                String errorMsg = "Cube[" + getCubeId() + "]中未找到匹配的扩展点，注入点: " + injectionPointId;
                System.out.println(errorMsg);
                return PointResult.failure("EXTENSION_NOT_FOUND", errorMsg);
            }

            System.out.println("  执行扩展点: " + extensionPoint.getExtensionId() + " [" + extensionPoint.getExtensionName() + "]");
            return extensionPoint.execute(input);

        } catch (Exception e) {
            String errorMsg = "Cube[" + getCubeId() + "]执行扩展点异常，注入点: " + injectionPointId + ", 错误: " + e.getMessage();
            System.err.println(errorMsg);
            return PointResult.failure("CUBE_EXECUTION_ERROR", errorMsg);
        }
    }

    /**
     * 查找最佳匹配的扩展点
     */
    protected ExtensionPoint findBestMatchExtensionPoint(String injectionPointId) {
        // 默认实现：直接通过扩展点ID查找
        ExtensionPoint directMatch = metaData.findExtensionPoint(new DotNotationId(injectionPointId));
        if (directMatch != null) {
            return directMatch;
        }

        // 如果直接匹配失败，尝试模糊匹配或返回第一个可用的扩展点
        List<ExtensionPoint> availablePoints = metaData.getExtensionPoints();
        if (!availablePoints.isEmpty()) {
            // 按优先级排序，返回优先级最高的
            return availablePoints.stream()
                    .min((ep1, ep2) -> Integer.compare(ep1.getPriority(), ep2.getPriority()))
                    .orElse(null);
        }

        return null;
    }

    /**
     * 检查是否包含指定的扩展点
     */
    public boolean hasExtensionPoint(String extensionId) {
        return metaData.findExtensionPoint(new DotNotationId(extensionId)) != null;
    }

    // 获取Cube ID的便捷方法
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