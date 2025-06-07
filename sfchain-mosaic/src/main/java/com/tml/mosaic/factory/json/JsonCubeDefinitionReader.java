package com.tml.mosaic.factory.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.factory.CubeDefinition;
import com.tml.mosaic.factory.config.CubeDefinitionRegistry;
import com.tml.mosaic.factory.support.AbstractCubeDefinitionReader;
import com.tml.mosaic.factory.io.loader.ResourceLoader;
import com.tml.mosaic.factory.io.resource.Resource;
import com.tml.mosaic.install.CubeInstaller;
import com.tml.mosaic.install.support.InstallerRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 描述: 从Json的配置文件中去加载
 * @author suifeng
 * 日期: 2025/6/7
 */
public class JsonCubeDefinitionReader extends AbstractCubeDefinitionReader {

    private final InstallerRegistry installerRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonCubeDefinitionReader(CubeDefinitionRegistry registry, InstallerRegistry installerRegistry) {
        super(registry);
        this.installerRegistry = installerRegistry;
    }

    public JsonCubeDefinitionReader(CubeDefinitionRegistry registry, ResourceLoader resourceLoader, InstallerRegistry installerRegistry) {
        super(registry, resourceLoader);
        this.installerRegistry = installerRegistry;
    }

    @Override
    public void loadCubeDefinitions(Resource resource) throws CubeException {
        try {
            try (InputStream inputStream = resource.getInputStream()) {
                doLoadCubeDefinitions(inputStream);
            }
        } catch (IOException e) {
            throw new CubeException("IOException parsing JSON document from " + resource, e);
        }
    }

    private void doLoadCubeDefinitions(InputStream inputStream) throws CubeException {
        try {
            // 解析JSON配置
            InstallationConfig config = objectMapper.readValue(inputStream, InstallationConfig.class);

            // 处理每个安装项
            for (InstallationItem item : config.getInstallations()) {
                processInstallationItem(item);
            }
        } catch (IOException e) {
            throw new CubeException("Failed to parse installation config", e);
        }
    }

    private void processInstallationItem(InstallationItem item) throws CubeException {
        // 获取安装器
        CubeInstaller installer = installerRegistry.getInstaller(item.getType());
        if (installer == null) {
            throw new CubeException("No installer found for type: " + item.getType());
        }

        // 执行安装
        List<CubeDefinition> definitions = installer.installCube(item.getLocation());

        // 注册CubeDefinition
        CubeDefinitionRegistry registry = getRegistry();
        for (CubeDefinition definition : definitions) {
            registry.registerCubeDefinition(definition.getId(), definition);
        }
    }
}
