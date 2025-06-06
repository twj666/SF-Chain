package com.tml.mosaic.install.support;

import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.core.infrastructure.CommonComponent;
import lombok.Data;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 描述: JAR包类加载器
 * @author suifeng
 * 日期: 2025/6/3
 */
@Data
public class JarPluginClassLoader extends URLClassLoader {
    
    private final GUID loaderId;
    private final String jarPath;
    private final Map<String, Class<?>> loadedClasses;
    private volatile boolean closed = false;
    
    public JarPluginClassLoader(String jarPath, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.loaderId = CommonComponent.GuidAllocator().nextGUID();
        this.jarPath = jarPath;
        this.loadedClasses = new ConcurrentHashMap<>();
    }
    
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            loadedClasses.clear();
            try {
                super.close();
                System.out.println("JAR类加载器已关闭: " + jarPath);
            } catch (Exception e) {
                System.err.println("关闭类加载器失败: " + e.getMessage());
            }
        }
    }
}