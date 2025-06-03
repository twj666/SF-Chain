package com.tml.mosaic.install.support;

import com.tml.mosaic.core.execption.CubeException;
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
     * 获取安装器类型
     */
    String getInstallerType();

    /**
     * 安装单个Cube资源
     */
    void installCube(Resource resource) throws CubeException;

    /**
     * 安装多个Cube资源
     */
    void installCube(Resource... resources) throws CubeException;

    /**
     * 安装location位置下的Cube资源
     */
    void installCube(String location) throws CubeException;
}
