package com.tml.mosaic.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;

/**
 * 描述: Cube管理器
 * @author suifeng
 * 日期: 2025/5/27
 */
public class CubeManager {

    private static final CubeManager INSTANCE = new CubeManager();

    private final Map<String, Cube> cubes = new ConcurrentHashMap<>();
    private final Map<String, List<ExtensionPoint>> extensionPoints = new ConcurrentHashMap<>();
    private final Map<String, String> injectionPointMapping = new ConcurrentHashMap<>();

    private CubeManager() {}

    public static CubeManager getInstance() {
        return INSTANCE;
    }

    /**
     * 注册Cube
     */
    public void registerCube(Cube cube) {
        if (cube == null) {
            throw new IllegalArgumentException("方块不能为空");
        }

        String cubeId = cube.getCubeId();
        if (cubes.containsKey(cubeId)) {
            throw new IllegalStateException("方块ID已存在: " + cubeId);
        }

        cubes.put(cubeId, cube);
        cube.initialize();

        scanAndRegisterExtensions(cube);

        System.out.println("方块注册成功: " + cubeId + " [" + cube.getDescription() + "]");
    }

    /**
     * 卸载Cube
     */
    public void unregisterCube(String cubeId) {
        Cube cube = cubes.remove(cubeId);
        if (cube != null) {
            cube.destroy();
            removeExtensionsByCube(cubeId);
            System.out.println("方块卸载成功: " + cubeId);
        }
    }

    /**
     * 获取Cube
     */
    public Cube getCube(String cubeId) {
        return cubes.get(cubeId);
    }

    /**
     * 注册扩展点
     */
    public void registerExtensionPoint(String extensionId, ExtensionPoint extensionPoint) {
        extensionPoints.computeIfAbsent(extensionId, k -> new CopyOnWriteArrayList<>())
                .add(extensionPoint);
        System.out.println("扩展点注册成功: " + extensionId + " [" + extensionPoint.getExtensionName() + "]");
    }

    /**
     * 获取扩展点
     */
    public List<ExtensionPoint> getExtensionPoints(String extensionId) {
        return extensionPoints.getOrDefault(extensionId, new CopyOnWriteArrayList<>());
    }

    /**
     * 绑定注入点到扩展点
     */
    public void bindInjectionPoint(String injectionPointId, String extensionId) {
        injectionPointMapping.put(injectionPointId, extensionId);
    }

    /**
     * 获取注入点绑定的扩展点ID
     */
    public String getExtensionIdByInjectionPoint(String injectionPointId) {
        return injectionPointMapping.get(injectionPointId);
    }

    /**
     * 扫描并注册Cube中的扩展点
     */
    private void scanAndRegisterExtensions(Cube cube) {
        Class<?> cubeClass = cube.getClass();

        for (java.lang.reflect.Method method : cubeClass.getDeclaredMethods()) {
            MExtension MExtension = method.getAnnotation(MExtension.class);
            if (MExtension != null) {
                String extensionName = MExtension.name().isEmpty() ? method.getName() : MExtension.name();
                String description = MExtension.description().isEmpty() ? "无描述" : MExtension.description();

                ExtensionPoint extensionPoint = new MethodExtensionPoint(
                        cube, method, MExtension.value(), extensionName, description
                );
                registerExtensionPoint(MExtension.value(), extensionPoint);
            }
        }
    }

    /**
     * 移除Cube相关的扩展点
     */
    private void removeExtensionsByCube(String cubeId) {
        extensionPoints.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(ep -> ep instanceof MethodExtensionPoint &&
                    ((MethodExtensionPoint) ep).getCubeId().equals(cubeId));
            return entry.getValue().isEmpty();
        });
    }

    /**
     * 获取所有已注册的Cube
     */
    public Map<String, Cube> getAllCubes() {
        return new ConcurrentHashMap<>(cubes);
    }

    /**
     * 获取所有扩展点信息
     */
    public void printAllExtensions() {
        System.out.println("========== 扩展点信息 ==========");
        extensionPoints.forEach((extensionId, points) -> {
            System.out.println("扩展点ID: " + extensionId);
            points.forEach(point -> {
                System.out.println("  - 名称: " + point.getExtensionName());
                System.out.println("  - 描述: " + point.getDescription());
                if (point instanceof MethodExtensionPoint) {
                    MethodExtensionPoint mep = (MethodExtensionPoint) point;
                    System.out.println("  - 所属方块: " + mep.getCubeId());
                    System.out.println("  - 方法名: " + mep.getMethodName());
                }
                System.out.println();
            });
        });
        System.out.println("===============================");
    }
}