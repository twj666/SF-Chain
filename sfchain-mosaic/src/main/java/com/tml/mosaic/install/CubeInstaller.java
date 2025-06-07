package com.tml.mosaic.install;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.factory.CubeDefinition;
import com.tml.mosaic.factory.io.resource.Resource;
import com.tml.mosaic.factory.io.loader.ResourceLoader;

import java.util.List;

/**
 * 描述: Cube安装器接口
 * @author suifeng
 * 日期: 2025/5/29
 */
public interface CubeInstaller {

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
    List<CubeDefinition> installCube(Resource resource) throws CubeException;

    /**
     * 安装多个Cube资源
     */
    List<CubeDefinition> installCube(Resource... resources) throws CubeException;

    /**
     * 安装location位置下的Cube资源
     */
    List<CubeDefinition> installCube(String location) throws CubeException;
}
