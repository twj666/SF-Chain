package com.tml.mosaic.factory.support;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.cube.CubeDefinition;

/**
 * 描述: 默认的Cube实例化策略
 * @author suifeng
 * 日期: 2025/6/6
 */
public class DefaultInstantiationStrategy implements InstantiationStrategy {

    @Override
    public Cube instantiate(CubeDefinition cubeDefinition, GUID cubeId, Object[] args) throws CubeException {
        return null;
    }
}
