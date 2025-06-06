package com.tml.mosaic.factory.support;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.cube.CubeDefinition;

/**
 * 描述: 用于实例化Cube的类
 * @author suifeng
 * 日期: 2025/6/6
 */
public abstract class AbstractAutowireCapableCubeFactory extends AbstractCubeFactory {

    private InstantiationStrategy instantiationStrategy = new DefaultInstantiationStrategy();

    @Override
    protected Cube createCube(GUID cubeId, CubeDefinition cubeDefinition, Object[] args) throws CubeException {
        Cube cube = null;
        try {
            cube = createCubeInstance(cubeDefinition, cubeId, args);
        } catch (Exception e) {
            throw new CubeException("Instantiation of cube failed", e);
        }
        // 加入到单例池中
        addSingleton(cubeId, cube);
        return cube;
    }

    // 内部实例化方法
    protected Cube createCubeInstance(CubeDefinition cubeDefinition, GUID cubeId, Object[] args) {
        return instantiationStrategy.instantiate(cubeDefinition, cubeId, args);
    }
}
