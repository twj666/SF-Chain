package com.tml.mosaic.install.support;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.factory.CubeDefinition;
import com.tml.mosaic.install.CubeInstaller;
import com.tml.mosaic.factory.io.loader.DefaultResourceLoader;
import com.tml.mosaic.factory.io.loader.ResourceLoader;
import com.tml.mosaic.factory.io.resource.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述: 方块安装器抽象类实现
 * @author suifeng
 * 日期: 2025/5/29
 */
public abstract class AbstractCubeInstaller implements CubeInstaller {

    private final ResourceLoader resourceLoader;

    protected AbstractCubeInstaller() {
        this(new DefaultResourceLoader());
    }

    public AbstractCubeInstaller(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    @Override
    public List<CubeDefinition> installCube(Resource... resources) throws CubeException {
        List<CubeDefinition> allCubes = new ArrayList<>();
        for (Resource resource : resources) {
            allCubes.addAll(installCube(resource));
        }
        return allCubes;
    }

    @Override
    public List<CubeDefinition> installCube(String location) throws CubeException {
        ResourceLoader resourceLoader = getResourceLoader();
        Resource resource = resourceLoader.getResource(location);
        return installCube(resource);
    }
}
