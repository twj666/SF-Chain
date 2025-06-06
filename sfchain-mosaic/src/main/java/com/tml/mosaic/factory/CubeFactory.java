package com.tml.mosaic.factory;

import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.CubeDefinition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述: Cube工厂
 * @author suifeng
 * 日期: 2025/6/6
 */
public class CubeFactory {

    private Map<GUID, CubeDefinition> cubeDefinitionMap = new ConcurrentHashMap<>();


}
