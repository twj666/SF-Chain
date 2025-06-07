package com.tml.mosaic.factory.context;

import com.tml.mosaic.core.execption.CubeException;

/**
 * 描述: Cube上下文（提供刷新容器的操作）
 * @author suifeng
 * 日期: 2025/6/7
 */
public interface ConfigurableCubeContext extends CubeContext {

    /**
     * 刷新容器
     */
    void refresh() throws CubeException;
}
