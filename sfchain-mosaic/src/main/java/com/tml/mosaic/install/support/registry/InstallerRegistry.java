package com.tml.mosaic.install.support.registry;

import com.tml.mosaic.install.CubeInstaller;

public interface InstallerRegistry {
    void registerInstaller(String installerType, CubeInstaller installer);
    CubeInstaller getInstaller(String installerType);
}