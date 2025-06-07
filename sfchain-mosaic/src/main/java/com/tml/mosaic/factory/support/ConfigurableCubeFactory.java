package com.tml.mosaic.factory.support;


import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.factory.CubeFactory;

// TODO 有点歧义
public interface ConfigurableCubeFactory extends CubeFactory {

    void preInstantiateSingletons() throws CubeException;
}