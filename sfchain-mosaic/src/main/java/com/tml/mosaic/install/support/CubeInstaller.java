package com.tml.mosaic.install.support;

import com.tml.mosaic.core.CubeException;
import com.tml.mosaic.install.io.resource.Resource;
import com.tml.mosaic.install.io.loader.ResourceLoader;

/**
 * 描述: Cube安装器接口
 * @author suifeng
 * 日期: 2025/5/29
 */
public interface CubeInstaller {

    /**
     * 获取注册器
     */
    CubeRegistry getRegistry();

    /**
     * 获取资源解析器
     */
    ResourceLoader getResourceLoader();

    /**
     * 加载单个Cube资源
     */
    void loadCube(Resource resource) throws CubeException;

    /**
     * 加载多个Cube资源
     */
    void loadCube(Resource... resources) throws CubeException;

    /**
     * 加载location位置的Cube资源
     */
    void loadBean(String location) throws CubeException;
}
