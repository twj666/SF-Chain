package com.tml.mosaic.factory.context;

import com.tml.mosaic.core.execption.CubeException;

public interface ConfigurableCubeApplicationContext extends CubeApplicationContext {

    /**
     * 刷新容器
     */
    void refresh() throws CubeException;
}
