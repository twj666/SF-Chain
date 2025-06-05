package com.tml.mosaic.cube;

import com.tml.mosaic.core.annotation.MExtension;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.install.support.CubeRegistry;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 描述: Cube管理器
 * @author suifeng
 * 日期: 2025/5/27
 */
@Getter
public class CubeManager implements CubeRegistry {

    private static final CubeManager INSTANCE = new CubeManager();

    // Cube容器
    private final Map<GUID, Cube> cubes = new ConcurrentHashMap<>();

    // 注入点到Cube的映射关系
    private final Map<String, GUID> injectionPointToCubeMapping = new ConcurrentHashMap<>();

    private CubeManager() {}

    public static CubeManager getInstance() {
        return INSTANCE;
    }

    @Override
    public void registerCube(Cube cube) {
        registerCube(cube.getCubeId(), cube);
    }

    @Override
    public void registerCube(GUID cubeId, Cube cube) {
        validateCubeRegistration(cubeId, cube);

        cubes.put(cubeId, cube);
        cube.initialize();
        scanAndBuildCubeExtensions(cube);

        System.out.println("方块注册成功: " + cubeId + " [" + cube.getMetaData().getDescription() + "]");
    }

    public void unregisterCube(GUID cubeId) {
        Cube cube = cubes.remove(cubeId);
        if (cube != null) {
            // 清理注入点映射
            removeInjectionPointsByCube(cubeId);
            // 销毁Cube
            cube.destroy();
            System.out.println("方块卸载成功: " + cubeId);
        }
    }

    /**
     * 绑定注入点到指定Cube
     */
    public void bindInjectionPointToCube(String injectionPointId, GUID cubeId) {
        if (cubeId != null && !cubes.containsKey(cubeId)) {
            System.err.println("警告: Cube不存在 " + cubeId);
            return;
        }

        injectionPointToCubeMapping.put(injectionPointId, cubeId);
        System.out.println("注入点绑定成功: " + injectionPointId + " -> Cube[" + cubeId + "]");
    }

    /**
     * 解绑注入点
     */
    public void unbindInjectionPoint(String injectionPointId) {
        GUID removedCubeId = injectionPointToCubeMapping.remove(injectionPointId);
        System.out.println("注入点解绑成功: " + injectionPointId +
                (removedCubeId != null ? " (原绑定Cube: " + removedCubeId + ")" : ""));
    }

    /**
     * 通过注入点获取绑定的Cube
     */
    public Cube getCubeByInjectionPoint(String injectionPointId) {
        GUID cubeId = injectionPointToCubeMapping.get(injectionPointId);
        return cubeId != null ? cubes.get(cubeId) : null;
    }

    /**
     * 获取注入点绑定的CubeId
     */
    public GUID getCubeIdByInjectionPoint(String injectionPointId) {
        return injectionPointToCubeMapping.get(injectionPointId);
    }

    /**
     * 扫描并构建Cube内部的扩展点
     */
    private void scanAndBuildCubeExtensions(Cube cube) {
        Class<?> cubeClass = cube.getClass();
        int extensionCount = 0;

        for (Method method : cubeClass.getDeclaredMethods()) {
            MExtension mExtension = method.getAnnotation(MExtension.class);
            if (mExtension != null) {
                String extensionName = mExtension.name().isEmpty() ? method.getName() : mExtension.name();
                String description = mExtension.description().isEmpty() ? "无描述" : mExtension.description();

                MethodExtensionPoint extensionPoint = new MethodExtensionPoint(
                        cube, method, mExtension.value(), extensionName, description
                );

                extensionPoint.setPriority(mExtension.priority());

                // 添加到Cube的MetaData中
                cube.getMetaData().addExtensionPoint(extensionPoint);
                extensionCount++;

                System.out.println("  扫描到扩展点: " + mExtension.value() + " [" + extensionName + "]");
            }
        }

        System.out.println("Cube[" + cube.getCubeId() + "]扫描完成，共发现 " + extensionCount + " 个扩展点");
    }

    /**
     * 移除Cube相关的注入点映射
     */
    private void removeInjectionPointsByCube(GUID cubeId) {
        injectionPointToCubeMapping.entrySet().removeIf(entry ->
                cubeId.equals(entry.getValue())
        );
    }

    /**
     * 验证Cube注册参数
     */
    private void validateCubeRegistration(GUID cubeId, Cube cube) {
        if (cube == null) {
            throw new IllegalArgumentException("方块不能为空");
        }
        if (cubes.containsKey(cubeId)) {
            throw new IllegalStateException("方块ID已存在: " + cubeId);
        }
    }

    /**
     * 打印所有Cube和扩展点信息
     */
    public void printAllCubesAndExtensions() {
        System.out.println("========== Cube和扩展点信息 ==========");
        cubes.forEach((cubeId, cube) -> {
            System.out.println("Cube ID: " + cubeId);
            System.out.println("  名称: " + cube.getMetaData().getName());
            System.out.println("  描述: " + cube.getMetaData().getDescription());
            System.out.println("  版本: " + cube.getMetaData().getVersion());
            System.out.println("  扩展点数量: " + cube.getMetaData().getExtensionPoints().size());

            cube.getMetaData().getExtensionPoints().forEach(ep -> {
                System.out.println("    - 扩展点ID: " + ep.getExtensionId());
                System.out.println("      名称: " + ep.getExtensionName());
                System.out.println("      描述: " + ep.getDescription());
                System.out.println("      优先级: " + ep.getPriority());
            });
            System.out.println();
        });

        System.out.println("========== 注入点绑定信息 ==========");
        injectionPointToCubeMapping.forEach((injectionPoint, cubeId) -> {
            Cube cube = cubes.get(cubeId);
            System.out.println("注入点: " + injectionPoint + " -> Cube[" + cubeId + "]" +
                    (cube != null ? " (" + cube.getMetaData().getName() + ")" : " (Cube不存在)"));
        });
        System.out.println("=====================================");
    }
}