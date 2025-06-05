package com.tml.mosaic.install.support;

import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.Cube;

/**
 * 描述: 方块注册接口
 * @author suifeng
 * 日期: 2025/5/29
 */
public interface CubeRegistry {

    /**
     * 注册方块
     * @param cube 方块对象
     */
    void registerCube(Cube cube);

    /**
     * 注册方块 (指定id)
     * @param cubeId 指定方块Id
     * @param cube 方块对象
     */
    void registerCube(GUID cubeId, Cube cube);
}
