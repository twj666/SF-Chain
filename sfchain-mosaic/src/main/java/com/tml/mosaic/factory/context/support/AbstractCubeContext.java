package com.tml.mosaic.factory.context.support;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.factory.context.CubeContext;
import com.tml.mosaic.factory.definition.CubeDefinition;
import com.tml.mosaic.factory.io.loader.DefaultResourceLoader;
import com.tml.mosaic.factory.support.ListableCubeFactory;

/**
 * 描述: Cube上下文抽象类实现
 * @author suifeng
 * 日期: 2025/6/7
 */
public abstract class AbstractCubeContext extends DefaultResourceLoader implements CubeContext {

    protected abstract void refreshCubeFactory() throws CubeException;

    protected abstract ListableCubeFactory getBeanFactory();

    @Override
    public Cube getCube(GUID cubeId) throws CubeException {
        return getBeanFactory().getCube(cubeId);
    }

    @Override
    public Cube getCube(GUID cubeId, Object... args) throws CubeException {
        return getBeanFactory().getCube(cubeId, args);
    }

    public void registerCubeDefinition(GUID cubeId, CubeDefinition cubeDefinition){
        getBeanFactory().registerCubeDefinition(cubeId, cubeDefinition);
    }

    protected abstract void loadCubeDefinitions(ListableCubeFactory cubeFactory);
}
