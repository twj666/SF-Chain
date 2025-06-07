package com.tml.mosaic.factory.config;

import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.factory.definition.CubeDefinition;

/**
 * 描述: Cube定义注册接口
 * @author suifeng
 * 日期: 2025/5/29
 */
public interface CubeDefinitionRegistry {

    /**
     * 注册方块定义信息 (指定id)
     * @param cubeId 指定方块Id
     * @param cubeDefinition 方块定义
     */
    void registerCubeDefinition(GUID cubeId, CubeDefinition cubeDefinition);
}
