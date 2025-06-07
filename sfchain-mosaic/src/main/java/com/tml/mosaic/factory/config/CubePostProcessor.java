package com.tml.mosaic.factory.config;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.Cube;

/**
 * 描述: Cube对象使用前处理器
 * @author suifeng
 * 日期: 2025/6/7
 */
public interface CubePostProcessor {

    /**
     * 在 cube 对象执行初始化方法之前，执行此方法
     */
    Object postProcessBeforeInitialization(Cube cube, GUID cubeId) throws CubeException;

    /**
     * 在 cube 对象执行初始化方法之后，执行此方法
     */
    Object postProcessAfterInitialization(Cube cube, GUID cubeId) throws CubeException;
}
