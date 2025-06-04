package com.tml.mosaic.cube;

import com.tml.mosaic.core.infrastructure.CommonComponent;
import com.tml.mosaic.core.tools.guid.UniqueEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 方块抽象类
 */
public abstract class Cube extends UniqueEntity {

    private MetaData metaData;

    public Cube() {
        super(CommonComponent.GuidAllocator().nextGUID());
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class MetaData{

        // cube名称
        private String name;

        // cube版本
        private String version;

        // cube说明
        private String description;

        // 扩展点
        List<ExtensionPoint> extensionPoints;
    }

}
