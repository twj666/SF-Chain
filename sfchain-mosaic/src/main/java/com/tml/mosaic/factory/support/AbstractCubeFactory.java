package com.tml.mosaic.factory.support;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.factory.config.CubeDefinition;
import com.tml.mosaic.factory.CubeFactory;

/**
 * 描述: Cube工厂抽象类（定义getCube的模板方法）
 * @author suifeng
 * 日期: 2025/6/6
 */
public abstract class AbstractCubeFactory extends DefaultSingletonCubeRegistry implements CubeFactory {

    /**
     * 实现getCube方法，定义主流程
     * @param cubeId id
     * @return Cube 对象
     * @throws CubeException e
     */
    @Override
    public Cube getCube(GUID cubeId, Object[] args) throws CubeException {
        Cube cube = getSingleton(cubeId);
        if (cube != null) {
            return cube;
        }
        CubeDefinition cubeDefinition = getCubeDefinition(cubeId);
        return createCube(cubeId, cubeDefinition, args);
    }

    @Override
    public Cube getCube(GUID cubeId) throws CubeException {
        return getCube(cubeId, null);
    }

    // 获取Cube定义，由子类实现
    protected abstract CubeDefinition getCubeDefinition(GUID cubeId) throws CubeException;

    // 创建Cube，由子类实现
    protected abstract Cube createCube(GUID cubeId, CubeDefinition cubeDefinition, Object[] args) throws CubeException;
}
