package com.tml.mosaic.factory.context.support;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.factory.context.ConfigurableCubeContext;
import com.tml.mosaic.factory.support.DefaultDefinitionListableCubeFactory;
import com.tml.mosaic.factory.support.ListableCubeFactory;

/**
 * 描述: 获取Cube工厂和加载资源
 * @author suifeng
 * 日期: 2025/6/7
 */
public abstract class AbstractRefreshableCubeContext extends AbstractCubeContext implements ConfigurableCubeContext {

    private DefaultDefinitionListableCubeFactory cubeFactory;

    @Override
    public void refresh() throws CubeException {
        // 1. 创建 CubeFactory，并加载 CubeDefinition
        refreshCubeFactory();

        // 2. 获取 CubeFactory
        ListableCubeFactory cubeFactory = getBeanFactory();

        // 3. 提前实例化单例Bean对象
        cubeFactory.preInstantiateSingletons();
    }

    protected void refreshCubeFactory() throws CubeException {
        DefaultDefinitionListableCubeFactory cubeFactory = createBeanFactory();
        loadCubeDefinitions(cubeFactory);
        this.cubeFactory = cubeFactory;
    }

    private DefaultDefinitionListableCubeFactory createBeanFactory() {
        return new DefaultDefinitionListableCubeFactory();
    }

    @Override
    protected ListableCubeFactory getBeanFactory() {
        return cubeFactory;
    }
}
