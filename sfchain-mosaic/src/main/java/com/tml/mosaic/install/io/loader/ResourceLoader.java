package com.tml.mosaic.install.io.loader;

import com.tml.mosaic.install.io.resource.Resource;

/**
 * 描述: 资源加载器
 * @author suifeng
 * 日期: 2025/5/29
 */
public interface ResourceLoader {

    String CLASSPATH_URL_PREFIX = "classpath:";

    Resource getResource(String location);
}
