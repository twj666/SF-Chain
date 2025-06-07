package com.tml.mosaic.factory.support;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.factory.config.CubeDefinitionRegistry;
import com.tml.mosaic.factory.io.loader.ResourceLoader;
import com.tml.mosaic.factory.io.resource.Resource;

/**
 * 描述: Cube定义读取接口
 * @author suifeng
 * 日期: 2025/6/7 
 */
public interface CubeDefinitionReader {

    CubeDefinitionRegistry getRegistry();

    ResourceLoader getResourceLoader();

    void loadCubeDefinitions(Resource resource) throws CubeException;

    void loadCubeDefinitions(Resource... resources) throws CubeException;

    void loadCubeDefinitions(String location) throws CubeException;
}
