package com.tml.mosaic.factory.config;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.factory.support.DefaultListableCubeFactory;

/**
 * 描述: 用于处理修改CubeFactory里的CubeDefinition
 * @author suifeng
 * 日期: 2025/6/7
 */
public interface CubeFactoryPostProcessor {

    /**
     * 在所有的 CubeDefinition 加载完成后，实例化 Cube 对象之前，提供修改 CubeDefinition 属性的机制
     */
    void postProcessBeanFactory(DefaultListableCubeFactory cubeFactory) throws CubeException;
}
