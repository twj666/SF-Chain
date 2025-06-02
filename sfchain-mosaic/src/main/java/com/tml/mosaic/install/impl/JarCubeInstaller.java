package com.tml.mosaic.install.impl;

import com.tml.mosaic.core.CubeException;
import com.tml.mosaic.install.io.loader.ResourceLoader;
import com.tml.mosaic.install.io.resource.Resource;
import com.tml.mosaic.install.support.AbstractCubeInstaller;
import com.tml.mosaic.install.support.CubeRegistry;

import java.io.IOException;
import java.io.InputStream;

/**
 * 描述: 从Jar包安装Cube的安装器
 * @author suifeng
 * 日期: 2025/5/29 
 */
public class JarCubeInstaller extends AbstractCubeInstaller {

    public JarCubeInstaller(CubeRegistry registry) {
        super(registry);
    }

    public JarCubeInstaller(CubeRegistry registry, ResourceLoader resourceLoader) {
        super(registry, resourceLoader);
    }

    @Override
    public void installCube(Resource resource) throws CubeException {
        try {
            try (InputStream inputStream = resource.getInputStream()) {
                doInstallCubeByJar(inputStream);
            }
        } catch (IOException e) {
            throw new CubeException("IOException reading Jar Cube from " + resource, e);
        }
    }

    /**
     * 通过Jar包的方式加载Cube
     */
    protected void doInstallCubeByJar(InputStream inputStream) {

    }
}
