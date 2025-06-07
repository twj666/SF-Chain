package com.tml.mosaic.factory.support;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.factory.config.CubeDefinitionRegistry;
import com.tml.mosaic.factory.io.loader.DefaultResourceLoader;
import com.tml.mosaic.factory.io.loader.ResourceLoader;
import com.tml.mosaic.factory.io.resource.Resource;

/**
 * 描述: Cube定义读取抽象类
 * @author suifeng
 * 日期: 2025/6/7
 */
public abstract class AbstractCubeDefinitionReader implements CubeDefinitionReader {

    private final CubeDefinitionRegistry registry;

    private final ResourceLoader resourceLoader;

    protected AbstractCubeDefinitionReader(CubeDefinitionRegistry registry) {
        this(registry, new DefaultResourceLoader());
    }

    public AbstractCubeDefinitionReader(CubeDefinitionRegistry registry, ResourceLoader resourceLoader) {
        this.registry = registry;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public CubeDefinitionRegistry getRegistry() {
        return registry;
    }

    @Override
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    @Override
    public void loadCubeDefinitions(Resource... resources) throws CubeException {
        for (Resource resource : resources) {
            loadCubeDefinitions(resource);
        }
    }

    @Override
    public void loadCubeDefinitions(String location) throws CubeException {
        ResourceLoader resourceLoader = getResourceLoader();
        Resource resource = resourceLoader.getResource(location);
        loadCubeDefinitions(resource);
    }
}
