package com.tml.mosaic.cube;

import com.tml.mosaic.core.tools.guid.DotNotationId;
import com.tml.mosaic.core.tools.guid.UniqueEntity;
import lombok.Getter;

public class ExtensionPoint extends UniqueEntity {

    @Getter
    private String methodName;

    // 返回类型
    @Getter
    private Class<?> returnType;

    // 参数类型
    @Getter
    private Class<?>[] parameterTypes;

    // 方法说明
    @Getter
    private String description;

    @Getter
    private boolean asyncFlag;

    public ExtensionPoint(String id) {
        super(new DotNotationId(id));
    }
}
