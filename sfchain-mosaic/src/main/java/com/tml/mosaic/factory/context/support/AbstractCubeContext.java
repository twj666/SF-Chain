package com.tml.mosaic.factory.context.support;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.factory.context.ConfigurableCubeContext;
import com.tml.mosaic.factory.io.loader.DefaultResourceLoader;
import com.tml.mosaic.factory.support.ConfigurableListableCubeFactory;

/**
 * 描述: Cube上下文抽象类实现
 * @author suifeng
 * 日期: 2025/6/7
 */
public abstract class AbstractCubeContext extends DefaultResourceLoader implements ConfigurableCubeContext {

    @Override
    public void refresh() throws CubeException {
        // 1. 创建 CubeFactory，并加载 CubeDefinition
        refreshCubeFactory();

        // 2. 获取 CubeFactory
        ConfigurableListableCubeFactory cubeFactory = getBeanFactory();

        // 3. 提前实例化单例Bean对象
        cubeFactory.preInstantiateSingletons();
    }

    protected abstract void refreshCubeFactory() throws CubeException;

    protected abstract ConfigurableListableCubeFactory getBeanFactory();

    @Override
    public Cube getCube(GUID cubeId) throws CubeException {
        return getBeanFactory().getCube(cubeId);
    }

    @Override
    public Cube getCube(GUID cubeId, Object... args) throws CubeException {
        return getBeanFactory().getCube(cubeId, args);
    }
}
