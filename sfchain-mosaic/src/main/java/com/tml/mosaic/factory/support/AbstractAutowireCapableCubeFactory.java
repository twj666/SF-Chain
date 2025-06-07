package com.tml.mosaic.factory.support;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.cube.MethodExtensionPoint;
import com.tml.mosaic.cube.PointParam;
import com.tml.mosaic.factory.CubeDefinition;
import com.tml.mosaic.factory.ExtensionPointDefinition;
import com.tml.mosaic.factory.config.InstantiationStrategy;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * 描述: 用于实例化Cube的类
 * @author suifeng
 * 日期: 2025/6/6
 */
@Slf4j
public abstract class AbstractAutowireCapableCubeFactory extends AbstractCubeFactory {

    private final InstantiationStrategy instantiationStrategy = new DefaultInstantiationStrategy();

    @Override
    protected Cube createCube(GUID cubeId, CubeDefinition cubeDefinition, Object[] args) throws CubeException {
        Cube cube = null;
        try {
            cube = createCubeInstance(cubeDefinition, cubeId, args);
            // 给 Cube 填充属性
            applyPropertyValues(cubeId, cube, cubeDefinition);
            // 执行 Cube 的初始化方法和 CubePostProcessor 的前置和后置处理方法
            cube = initializeCube(cubeId, cube, cubeDefinition);
        } catch (Exception e) {
            throw new CubeException("Instantiation of cube failed", e);
        }
        // TODO 加入到单例池中
        addSingleton(cubeId, cube);
        return cube;
    }

    private Cube initializeCube(GUID cubeId, Cube cube, CubeDefinition cubeDefinition) {
        return cube;
    }

    // 内部实例化方法
    protected Cube createCubeInstance(CubeDefinition cubeDefinition, GUID cubeId, Object[] args) {
        return instantiationStrategy.instantiate(cubeDefinition, cubeId, args);
    }

    /**
     * Cube 属性填充
     */
    protected void applyPropertyValues(GUID cubeId, Cube cube, CubeDefinition cubeDefinition) {
        // 填充Cube核心元数据
        populateCubeMetadata(cube, cubeDefinition);

        // 填充扩展点元数据
        populateExtensionPoints(cube, cubeDefinition);
    }

    /**
     * 填充Cube核心元数据
     */
    private void populateCubeMetadata(Cube cube, CubeDefinition cubeDefinition) {
        Cube.MetaData metaData = cube.getMetaData();

        // 设置基础元数据
        metaData.setName(cubeDefinition.getName());
        metaData.setVersion(cubeDefinition.getVersion());
        metaData.setDescription(cubeDefinition.getDescription());
        metaData.setModel(cubeDefinition.getModel());

        // 日志记录元数据填充
        log.debug("填充Cube元数据 | ID: {} | 名称: {} | 版本: {}",
                cube.getCubeId(), metaData.getName(), metaData.getVersion());
    }

    /**
     * 填充扩展点元数据
     */
    private void populateExtensionPoints(Cube cube, CubeDefinition cubeDefinition) {
        for (ExtensionPointDefinition epd : cubeDefinition.getExtensionPoints()) {
            try {
                // 获取扩展点方法
                Method method = cube.getClass().getDeclaredMethod(
                        epd.getMethodName(),
                        PointParam.class
                );

                // 创建扩展点实例
                MethodExtensionPoint extensionPoint = new MethodExtensionPoint(
                        cube,
                        method,
                        epd.getId(),
                        epd.getExtensionName(),
                        epd.getDescription()
                );

                // 设置扩展点属性
                extensionPoint.setPriority(epd.getPriority());
                extensionPoint.setAsyncFlag(epd.isAsyncFlag());
                extensionPoint.setReturnType(epd.getReturnType());
                extensionPoint.setParameterTypes(epd.getParameterTypes());

                // 注册到Cube元数据
                cube.getMetaData().addExtensionPoint(extensionPoint);

                // 日志记录扩展点注册
                log.debug("注册扩展点 | Cube: {} | 扩展点: {} | 方法: {}",
                        cube.getCubeId(), epd.getId(), epd.getMethodName());

            } catch (NoSuchMethodException e) {
                log.error("扩展点方法未找到 | Cube: {} | 方法: {} | 错误: {}",
                        cube.getCubeId(), epd.getMethodName(), e.getMessage());
            }
        }
    }
}
