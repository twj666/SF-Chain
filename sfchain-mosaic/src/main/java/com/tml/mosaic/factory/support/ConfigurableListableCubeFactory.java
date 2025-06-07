package com.tml.mosaic.factory.support;


import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.factory.CubeFactory;

public interface ConfigurableListableCubeFactory extends CubeFactory {

    void preInstantiateSingletons() throws CubeException;
}