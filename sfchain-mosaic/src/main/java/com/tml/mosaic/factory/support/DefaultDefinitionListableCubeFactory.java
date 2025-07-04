package com.tml.mosaic.factory.support;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.factory.definition.CubeDefinition;
import com.tml.mosaic.factory.config.CubeDefinitionRegistry;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 描述: Cube工厂的核心实现类
 * @author suifeng
 * 日期: 2025/6/6
 */
public class DefaultDefinitionListableCubeFactory extends ListableCubeFactory{

    private Map<GUID, CubeDefinition> cubeDefinitionMap = new HashMap<>();

    @Override
    public void registerCubeDefinition(GUID cubeId, CubeDefinition cubeDefinition) {
        cubeDefinitionMap.put(cubeId, cubeDefinition);
    }

    @Override
    protected CubeDefinition getCubeDefinition(GUID cubeId) throws CubeException {
        CubeDefinition cubeDefinition = cubeDefinitionMap.get(cubeId);
        if (cubeDefinition == null) throw new CubeException("No cubeId '" + cubeId + "' is defined");
        return cubeDefinition;
    }

    @Override
    public void preInstantiateSingletons() throws CubeException {
        cubeDefinitionMap.keySet().forEach(this::getCube);
    }
}
