package com.tml.mosaic.factory;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.Cube;

/**
 * 描述: Cube工厂接口
 * @author suifeng
 * 日期: 2025/6/6
 */
public interface CubeFactory {

    // 核心方法，根据cubeId获取到一个可以直接用的Cube对象
    Cube getCube(GUID cubeId) throws CubeException;

    // 需要传一些参数
    Cube getCube(GUID cubeId, Object... args) throws CubeException;
}
