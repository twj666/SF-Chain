package com.tml.mosaic.install.io.resource;

import com.tml.mosaic.util.Assert;
import com.tml.mosaic.util.ClassUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * 描述: ClassPath下加载资源
 * @author suifeng
 * 日期: 2025/5/29 
 */
public class ClassPathResource implements Resource {

    private final String path;

    private ClassLoader classLoader;

    public ClassPathResource(String path) {
        this(path, (ClassLoader) null);
    }

    public ClassPathResource(String path, ClassLoader classLoader) {
        Assert.notNull(path, "Path 路径不能为空");
        this.path = path;
        this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream is = classLoader.getResourceAsStream(path);
        if (is == null) {
            throw new FileNotFoundException(
                    this.path + "不能打开此流，应该找不到此资源");
        }
        return is;
    }

    @Override
    public String getPath() {
        return path;
    }
}