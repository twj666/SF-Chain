package com.tml.mosaic.install.support;

import com.tml.mosaic.core.annotation.MCube;
import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.core.infrastructure.CommonComponent;
import com.tml.mosaic.cube.Cube;
import lombok.Data;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 描述: Cube实例化工厂
 * @author suifeng
 * 日期: 2025/6/3
 */
public class CubeInstanceFactory {
    
    private final Map<Class<? extends Cube>, CubeMetadata> metadataCache;
    
    public CubeInstanceFactory() {
        this.metadataCache = new ConcurrentHashMap<>();
    }
    
    /**
     * 创建Cube实例
     */
    public Cube createCubeInstance(Class<? extends Cube> cubeClass) throws CubeException {
        try {
            CubeMetadata metadata = getCubeMetadata(cubeClass);
            Cube cube = instantiateCube(cubeClass);
            
            // 如果Cube没有预设ID，则分配一个新的ID
            if (cube.getCubeId() == null) {
                setCubeId(cube, metadata.generateId());
            }
            
            System.out.println("成功创建Cube实例: " + cube.getCubeId() + 
                             " [" + metadata.getDescription() + "]");
            return cube;
            
        } catch (Exception e) {
            throw new CubeException("创建Cube实例失败: " + cubeClass.getName(), e);
        }
    }
    
    /**
     * 获取Cube元数据
     */
    private CubeMetadata getCubeMetadata(Class<? extends Cube> cubeClass) {
        return metadataCache.computeIfAbsent(cubeClass, this::extractMetadata);
    }
    
    /**
     * 提取Cube元数据
     */
    private CubeMetadata extractMetadata(Class<? extends Cube> cubeClass) {
        MCube annotation = cubeClass.getAnnotation(MCube.class);
        String cubeId = annotation.value().isEmpty() ? cubeClass.getSimpleName() : annotation.value();
        
        return new CubeMetadata(
            cubeId,
            annotation.version(),
            annotation.description(),
            annotation.autoRegister()
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
    
    /**
     * 设置Cube ID（通过反射）
     */
    private void setCubeId(Cube cube, GUID cubeId) {
        try {
            // 这里需要根据你的AbstractCube实现来设置ID
            // 假设有setCubeId方法或者cubeId字段
            java.lang.reflect.Field field = cube.getClass().getDeclaredField("cubeId");
            field.setAccessible(true);
            field.set(cube, cubeId);
        } catch (Exception e) {
            System.out.println("警告: 无法设置Cube ID，使用默认ID: " + e.getMessage());
        }
    }
    
    /**
     * Cube元数据内部类
     */
    @Data
    private static class CubeMetadata {
        private final String cubeId;
        private final String version;
        private final String description;
        private final boolean autoRegister;
        
        public CubeMetadata(String cubeId, String version, String description, boolean autoRegister) {
            this.cubeId = cubeId;
            this.version = version;
            this.description = description;
            this.autoRegister = autoRegister;
        }
        public GUID generateId() {
            return CommonComponent.GuidAllocator().nextGUID();
        }
    }
}