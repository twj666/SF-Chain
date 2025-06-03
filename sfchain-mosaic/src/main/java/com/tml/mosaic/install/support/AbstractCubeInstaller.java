package com.tml.mosaic.install.support;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.install.io.loader.DefaultResourceLoader;
import com.tml.mosaic.install.io.loader.ResourceLoader;
import com.tml.mosaic.install.io.resource.Resource;

/**
 * 描述: 方块安装器抽象类实现
 * @author suifeng
 * 日期: 2025/5/29
 */
public abstract class AbstractCubeInstaller implements CubeInstaller {

    private final CubeRegistry registry;

    private ResourceLoader resourceLoader;

    protected AbstractCubeInstaller(CubeRegistry registry) {
        this(registry, new DefaultResourceLoader());
    }

    public AbstractCubeInstaller(CubeRegistry registry, ResourceLoader resourceLoader) {
        this.registry = registry;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public CubeRegistry getRegistry() {
        return registry;
    }

    @Override
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    @Override
    public void installCube(Resource... resources) throws CubeException {
        for (Resource resource : resources) {
            installCube(resource);
        }
    }

    @Override
    public void installCube(String location) throws CubeException {
        ResourceLoader resourceLoader = getResourceLoader();
        Resource resource = resourceLoader.getResource(location);
        installCube(resource);
    }
}
