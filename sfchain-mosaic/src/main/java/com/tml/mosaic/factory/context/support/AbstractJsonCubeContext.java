package com.tml.mosaic.factory.context.support;

import com.tml.mosaic.factory.context.json.JsonCubeDefinitionReader;
import com.tml.mosaic.factory.support.DefaultListableCubeFactory;
import com.tml.mosaic.install.support.registry.InstallerRegistry;

/**
 * 描述: 上下文中对配置信息的加载
 * @author suifeng
 * 日期: 2025/6/7
 */
public abstract class AbstractJsonCubeContext extends AbstractRefreshableCubeContext {

    @Override
    protected void loadCubeDefinitions(DefaultListableCubeFactory cubeFactory) {
        JsonCubeDefinitionReader cubeDefinitionReader = new JsonCubeDefinitionReader(cubeFactory, getInstallerRegistry());
        String[] configLocations = getConfigLocations();
        if (null != configLocations){
            cubeDefinitionReader.loadCubeDefinitions(configLocations);
        }
    }
    protected abstract String[] getConfigLocations();
    protected abstract InstallerRegistry getInstallerRegistry();
}
