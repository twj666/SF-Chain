package com.tml.mosaic.factory;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.factory.context.support.AbstractJsonCubeContext;
import com.tml.mosaic.install.support.registry.InstallerRegistry;
import lombok.Data;


/**
 * 描述: 核心应用上下文实现类
 * @author suifeng
 * 日期: 2025/6/7
 */
@Data
public class ClassPathJsonCubeContext extends AbstractJsonCubeContext {

    private InstallerRegistry installerRegistry;

    private String[] configLocations;

    public ClassPathJsonCubeContext() {}

    /**
     * 从 JSON 中加载 CubeDefinition，并刷新上下文
     */
    public ClassPathJsonCubeContext(String configLocations, InstallerRegistry installerRegistry) throws CubeException {
        this(new String[]{configLocations}, installerRegistry);
    }

    public ClassPathJsonCubeContext(String[] configLocations, InstallerRegistry installerRegistry) throws CubeException {
        this.configLocations = configLocations;
        this.installerRegistry = installerRegistry;
        refresh();
    }

    @Override
    protected String[] getConfigLocations() {
        return configLocations;
    }

    @Override
    protected InstallerRegistry getInstallerRegistry() {
        return installerRegistry;
    }
}
