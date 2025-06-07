package com.tml.mosaic.factory.io.loader;

import com.tml.mosaic.factory.io.resource.Resource;
import com.tml.mosaic.factory.io.resource.UrlResource;
import com.tml.mosaic.factory.io.resource.ClassPathResource;
import com.tml.mosaic.factory.io.resource.FileSystemResource;
import com.tml.mosaic.util.Assert;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * 描述: 默认资源加载器
 * @author suifeng
 * 日期: 2025/5/29
 */
public class DefaultResourceLoader implements ResourceLoader {

    @Override
    public Resource getResource(String location) {
        Assert.notNull(location, "Location 不得为空");
        if (location.startsWith(CLASSPATH_URL_PREFIX)) {
            return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()));
        }
        else {
            try {
                URL url = new URL(location);
                return new UrlResource(url);
            } catch (MalformedURLException e) {
                return new FileSystemResource(location);
            }
        }
    }
}
