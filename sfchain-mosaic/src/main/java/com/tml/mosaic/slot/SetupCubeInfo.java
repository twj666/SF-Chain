package com.tml.mosaic.slot;

import com.tml.mosaic.core.Cube;
import com.tml.mosaic.core.guid.GUID;
import lombok.Getter;
import lombok.Setter;

/**
 * 安装的Cube信息
 */
public class SetupCubeInfo {

    private Cube cube;

    // TODO 调用的方法结构化
    // 调用的方法 id
    @Getter
    @Setter
    private String methodId;

    // 调用的方法参数
    @Getter
    @Setter
    private Object[] params;
}
