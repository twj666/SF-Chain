package com.tml.mosaic.factory.support;

import com.tml.mosaic.core.execption.CubeException;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.core.tools.guid.GUUID;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.cube.ExtensionPackage;
import com.tml.mosaic.cube.MethodExtensionPoint;
import com.tml.mosaic.cube.PointParam;
import com.tml.mosaic.factory.definition.CubeDefinition;
import com.tml.mosaic.factory.definition.ExtensionPackageDefinition;
import com.tml.mosaic.factory.definition.ExtensionPointDefinition;
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

        // 实例化并注入扩展包
        populateExtensionPackages(cube, cubeDefinition);
    }

    private void populateExtensionPackages(Cube cube, CubeDefinition cubeDefinition) {
        for (ExtensionPackageDefinition pkgDef : cubeDefinition.getExtensionPackages()) {
            try {
                // 加载扩展包类
                Class<?> pkgClass = cubeDefinition.getClassLoader().loadClass(pkgDef.getClassName());

                // 实例化扩展包
                ExtensionPackage extensionPackage = (ExtensionPackage) pkgClass.getDeclaredConstructor(Cube.class).newInstance(cube);
                extensionPackage.setCube(cube);

                // 创建扩展包元数据对象
                ExtensionPackage packageMeta = new ExtensionPackage() {};
                packageMeta.setName(pkgDef.getName());
                packageMeta.setDescription(pkgDef.getDescription());
                packageMeta.setVersion(pkgDef.getVersion());
                packageMeta.setId(new GUUID(pkgDef.getId()));
                packageMeta.setCube(cube);

                // 注册扩展包到Cube元数据
                cube.addExtensionPackage(packageMeta);

                // 注册扩展点
                for (ExtensionPointDefinition epd : pkgDef.getExtensionPoints()) {
                    Method method = pkgClass.getDeclaredMethod(epd.getMethodName(), PointParam.class);

                    MethodExtensionPoint extensionPoint = new MethodExtensionPoint(
                            extensionPackage,
                            method,
                            epd.getId(),
                            epd.getExtensionName(),
                            epd.getDescription()
                    );

                    // 设置扩展点属性
                    extensionPoint.setPriority(epd.getPriority());
                    extensionPoint.setAsyncFlag(epd.isAsyncFlag());

                    // 注册扩展点到扩展包
                    packageMeta.addExtensionPoint(extensionPoint);
                }

            } catch (Exception e) {
                log.error("扩展包初始化失败 | Cube: {} | 扩展包: {} | 错误: {}",
                        cube.getCubeId(), pkgDef.getClassName(), e.getMessage());
            }
        }
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
}
