package com.tml.mosaic.factory.json;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.factory.config.CubeDefinitionRegistry;
import com.tml.mosaic.factory.support.AbstractCubeDefinitionReader;
import com.tml.mosaic.factory.io.loader.ResourceLoader;
import com.tml.mosaic.factory.io.resource.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * 描述: 从Json中
 * @author suifeng
 * 日期: 2025/6/7
 */
public class JsonCubeDefinitionReader extends AbstractCubeDefinitionReader {

    public JsonCubeDefinitionReader(CubeDefinitionRegistry registry) {
        super(registry);
    }

    public JsonCubeDefinitionReader(CubeDefinitionRegistry registry, ResourceLoader resourceLoader) {
        super(registry, resourceLoader);
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

    private void doLoadCubeDefinitions(InputStream inputStream) {


    }
}
