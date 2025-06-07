package com.tml.mosaic.install.support;

import com.tml.mosaic.install.CubeInstaller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface InstallerRegistry {
    void registerInstaller(String installerType, CubeInstaller installer);
    CubeInstaller getInstaller(String installerType);
}