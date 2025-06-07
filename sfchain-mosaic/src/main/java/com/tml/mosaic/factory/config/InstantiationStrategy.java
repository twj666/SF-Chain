package com.tml.mosaic.factory.config;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.factory.CubeDefinition;

/**
 * 描述: Cube实例化策略
 * @author suifeng
 * 日期: 2025/6/6
 */
public interface InstantiationStrategy {

    Cube instantiate(CubeDefinition cubeDefinition, GUID cubeId, Object[] args) throws CubeException;
}
