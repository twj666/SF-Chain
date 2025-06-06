package com.tml.mosaic.factory.support;

import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.factory.config.SingletonCubeRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述: 单例Cube注册默认实现类
 * @author suifeng
 * 日期: 2025/6/6
 */
public class DefaultSingletonCubeRegistry implements SingletonCubeRegistry {

    private final Map<GUID, Cube> singletonCubes = new HashMap<>();

    @Override
    public Cube getSingleton(GUID cubeId) {
        return singletonCubes.get(cubeId);
    }

    protected void addSingleton(GUID cubeId, Cube cube) {
        singletonCubes.put(cubeId, cube);
    }
}
