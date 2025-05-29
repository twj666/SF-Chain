package com.tml.mosaic.install.support;

import com.tml.mosaic.install.io.loader.ResourceLoader;

/**
 * 描述: 方块安装器抽象类实现
 * @author suifeng
 * 日期: 2025/5/29
 */
public abstract class AbstractCubeInstaller implements CubeInstaller {

    @Override
    public CubeRegistry getRegistry() {
        return null;
    }

    @Override
    public ResourceLoader getResourceLoader() {
        return null;
    }
}
