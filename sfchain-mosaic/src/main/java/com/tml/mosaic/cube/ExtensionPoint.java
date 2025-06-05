package com.tml.mosaic.cube;

import com.tml.mosaic.core.tools.guid.DotNotationId;
import com.tml.mosaic.core.tools.guid.UniqueEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 扩展点抽象基类
 */
public abstract class ExtensionPoint extends UniqueEntity {

    @Getter @Setter
    private String methodName;

    @Getter @Setter
    private Class<?> returnType;

    @Getter @Setter
    private Class<?>[] parameterTypes;

    @Getter @Setter
    private String description;

    @Getter @Setter
    private boolean asyncFlag;

    @Getter @Setter
    private String extensionName;

    @Getter @Setter
    private int priority = 100;

    public ExtensionPoint(String id) {
        super(new DotNotationId(id));
    }

    // 核心执行方法
    public abstract PointResult execute(PointParam input);

    // 获取扩展点ID的便捷方法
    public String getExtensionId() {
        return getId().toString();
    }
}