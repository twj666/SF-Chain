package com.tml.mosaic.install.support;

import com.tml.mosaic.core.annotation.MCube;
import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.cube.Cube;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 描述: Cube实例化工厂
 * @author suifeng
 * 日期: 2025/6/3
 */
public class CubeInstanceFactory {
    
    private final Map<Class<? extends Cube>, Cube.MetaData> metadataCache;
    
    public CubeInstanceFactory() {
        this.metadataCache = new ConcurrentHashMap<>();
    }
    
    /**
     * 创建Cube实例
     */
    public Cube createCubeInstance(Class<? extends Cube> cubeClass) throws CubeException {
        try {
            Cube.MetaData metadata = getCubeMetadata(cubeClass);
            Cube cube = instantiateCube(cubeClass);
            
            System.out.println("成功创建Cube实例: " + cube.getCubeId() + " [" + metadata.getDescription() + "]");
            return cube;
            
        } catch (Exception e) {
            throw new CubeException("创建Cube实例失败: " + cubeClass.getName(), e);
        }
    }
    
    /**
     * 获取Cube元数据
     */
    private Cube.MetaData getCubeMetadata(Class<? extends Cube> cubeClass) {
        return metadataCache.computeIfAbsent(cubeClass, this::extractMetadata);
    }
    
    /**
     * 提取Cube元数据
     */
    private Cube.MetaData extractMetadata(Class<? extends Cube> cubeClass) {
        MCube annotation = cubeClass.getAnnotation(MCube.class);
        String cubeId = annotation.value().isEmpty() ? cubeClass.getSimpleName() : annotation.value();
        
        return new Cube.MetaData(
            cubeId,
            annotation.version(),
            annotation.description(),
            annotation.model()
        );
    }
    
    /**
     * 实例化Cube对象
     */
    private Cube instantiateCube(Class<? extends Cube> cubeClass) throws Exception {
        Constructor<? extends Cube> constructor = cubeClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}