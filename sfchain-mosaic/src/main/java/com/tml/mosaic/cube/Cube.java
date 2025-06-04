package com.tml.mosaic.cube;

import com.tml.mosaic.core.infrastructure.CommonComponent;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.core.tools.guid.UniqueEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 方块抽象类
 */
public abstract class Cube extends UniqueEntity {

    @Getter
    private MetaData metaData;

    public Cube() {
        super(CommonComponent.GuidAllocator().nextGUID());
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class MetaData{

        // cube名称
        private String name;

        // cube版本
        private String version;

        // cube说明
        private String description;

        // 扩展点
        private List<ExtensionPoint> extensionPoints;

        // 扩展点查询Map，不初始化
        private Map<GUID, ExtensionPoint> extensionMap;

        // 查询扩展点
        // TODO 并发问题
        public ExtensionPoint findExtensionPoint(GUID guid){
            if (Objects.isNull(extensionMap)){
                extensionMap = extensionPoints.stream().collect(Collectors.toMap(ExtensionPoint::getId, x -> x));
            }
            return extensionMap.get(guid);
        };
    }

}
