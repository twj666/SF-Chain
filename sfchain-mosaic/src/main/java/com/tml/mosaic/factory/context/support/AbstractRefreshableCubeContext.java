package com.tml.mosaic.factory.context.support;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.factory.support.ConfigurableListableCubeFactory;
import com.tml.mosaic.factory.support.DefaultListableCubeFactory;

/**
 * 描述: 获取Cube工厂和加载资源
 * @author suifeng
 * 日期: 2025/6/7
 */
public abstract class AbstractRefreshableCubeContext extends AbstractCubeContext {

    private DefaultListableCubeFactory cubeFactory;

    @Override
    protected void refreshCubeFactory() throws CubeException {
        DefaultListableCubeFactory cubeFactory = createBeanFactory();
        loadCubeDefinitions(cubeFactory);
        this.cubeFactory = cubeFactory;
    }

    private DefaultListableCubeFactory createBeanFactory() {
        return new DefaultListableCubeFactory();
    }

    protected abstract void loadCubeDefinitions(DefaultListableCubeFactory cubeFactory);

    @Override
    protected ConfigurableListableCubeFactory getBeanFactory() {
        return cubeFactory;
    }
}
