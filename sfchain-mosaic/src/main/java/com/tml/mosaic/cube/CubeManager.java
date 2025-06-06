package com.tml.mosaic.cube;

import com.tml.mosaic.core.annotation.MExtension;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.factory.config.CubeRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 描述: Cube管理器
 * @author suifeng
 * 日期: 2025/5/27
 */
@Slf4j
@Getter
public class CubeManager implements CubeRegistry {

    private static final CubeManager INSTANCE = new CubeManager();

    private final Map<GUID, Cube> cubes = new ConcurrentHashMap<>();
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
        int extensionCount = scanAndBuildCubeExtensions(cube);

        log.info("✓ 方块注册成功 | ID: {} | 名称: {} | 扩展点: {}个",
                cubeId, cube.getMetaData().getName(), extensionCount);
    }

    public void unregisterCube(GUID cubeId) {
        Cube cube = cubes.remove(cubeId);
        if (cube != null) {
            int removedMappings = removeInjectionPointsByCube(cubeId);
            cube.destroy();
            log.info("✓ 方块卸载成功 | ID: {} | 清理注入点: {}个", cubeId, removedMappings);
        } else {
            log.warn("⚠ 方块卸载失败 | ID: {} | 原因: 方块不存在", cubeId);
        }
    }

    public void bindInjectionPointToCube(String injectionPointId, GUID cubeId) {
        if (cubeId != null && !cubes.containsKey(cubeId)) {
            log.warn("⚠ 注入点绑定失败 | 注入点: {} | 原因: Cube[{}]不存在", injectionPointId, cubeId);
            return;
        }

        injectionPointToCubeMapping.put(injectionPointId, cubeId);
        log.info("✓ 注入点绑定成功 | {} → Cube[{}]", injectionPointId, cubeId);
    }

    public void unbindInjectionPoint(String injectionPointId) {
        GUID removedCubeId = injectionPointToCubeMapping.remove(injectionPointId);
        if (removedCubeId != null) {
            log.info("✓ 注入点解绑成功 | {} ← Cube[{}]", injectionPointId, removedCubeId);
        } else {
            log.warn("⚠ 注入点解绑失败 | {} | 原因: 未绑定", injectionPointId);
        }
    }

    public Cube getCubeByInjectionPoint(String injectionPointId) {
        GUID cubeId = injectionPointToCubeMapping.get(injectionPointId);
        return cubeId != null ? cubes.get(cubeId) : null;
    }

    public GUID getCubeIdByInjectionPoint(String injectionPointId) {
        return injectionPointToCubeMapping.get(injectionPointId);
    }

    /**
     * 扫描并构建Cube内部的扩展点
     */
    private int scanAndBuildCubeExtensions(Cube cube) {
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
                cube.getMetaData().addExtensionPoint(extensionPoint);
                extensionCount++;

                log.debug("  → 扫描扩展点 | ID: {} | 名称: {} | 优先级: {}",
                        mExtension.value(), extensionName, mExtension.priority());
            }
        }

        log.debug("✓ Cube扫描完成 | ID: {} | 发现扩展点: {}个", cube.getCubeId(), extensionCount);
        return extensionCount;
    }

    /**
     * 移除Cube相关的注入点映射
     */
    private int removeInjectionPointsByCube(GUID cubeId) {
        int removedCount = 0;
        var iterator = injectionPointToCubeMapping.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (cubeId.equals(entry.getValue())) {
                iterator.remove();
                removedCount++;
                log.debug("  → 清理注入点映射 | {} ← Cube[{}]", entry.getKey(), cubeId);
            }
        }
        return removedCount;
    }

    private void validateCubeRegistration(GUID cubeId, Cube cube) {
        if (cube == null) {
            throw new IllegalArgumentException("方块不能为空");
        }
        if (cubes.containsKey(cubeId)) {
            throw new IllegalStateException("方块ID已存在: " + cubeId);
        }
    }

    /**
     * 打印系统状态概览
     */
    public void printSystemOverview() {
        log.info("========== 系统状态概览 ==========");
        log.info("已注册方块数量: {}", cubes.size());
        log.info("注入点绑定数量: {}", injectionPointToCubeMapping.size());

        cubes.forEach((cubeId, cube) -> {
            log.info("Cube | ID: {} | 名称: {} | 版本: {} | 扩展点: {}个",
                    cubeId,
                    cube.getMetaData().getName(),
                    cube.getMetaData().getVersion(),
                    cube.getMetaData().getExtensionPoints().size());
        });

        if (!injectionPointToCubeMapping.isEmpty()) {
            log.info("--- 注入点绑定关系 ---");
            injectionPointToCubeMapping.forEach((injectionPoint, cubeId) -> {
                Cube cube = cubes.get(cubeId);
                log.info("绑定 | {} → Cube[{}] ({})",
                        injectionPoint, cubeId,
                        cube != null ? cube.getMetaData().getName() : "未知");
            });
        }
        log.info("================================");
    }
}