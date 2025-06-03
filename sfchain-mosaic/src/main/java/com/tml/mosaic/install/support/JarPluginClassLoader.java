package com.tml.mosaic.install.support;

import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.core.tools.guid.GuidAllocator;
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
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (closed) {
            throw new ClassNotFoundException("类加载器已关闭: " + loaderId);
        }
        
        synchronized (getClassLoadingLock(name)) {
            // 检查缓存
            Class<?> clazz = loadedClasses.get(name);
            if (clazz != null) {
                if (resolve) resolveClass(clazz);
                return clazz;
            }
            
            // 框架核心类必须由父类加载器加载，确保一致性
            if (isFrameworkCoreClass(name)) {
                clazz = getParent().loadClass(name);
            } else {
                // 插件类优先从当前JAR加载
                try {
                    clazz = findClass(name);
                } catch (ClassNotFoundException e) {
                    clazz = getParent().loadClass(name);
                }
            }
            
            if (clazz != null) {
                loadedClasses.put(name, clazz);
                if (resolve) resolveClass(clazz);
            }
            
            return clazz;
        }
    }
    
    /**
     * 判断是否是框架核心类
     */
    private boolean isFrameworkCoreClass(String className) {
        return className.startsWith("com.tml.mosaic.core.") ||
               className.startsWith("com.tml.mosaic.install.") ||
               className.startsWith("com.tml.mosaic.slot.") ||
               className.startsWith("com.tml.mosaic.actuator.") ||
               className.startsWith("java.") ||
               className.startsWith("javax.") ||
               className.startsWith("lombok.");
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

    public int getLoadedClassCount() { return loadedClasses.size(); }
}