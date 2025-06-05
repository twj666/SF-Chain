package com.tml.mosaic.cube;

import com.tml.mosaic.core.annotation.MExtension;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.install.support.CubeRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;

/**
 * 描述: Cube管理器
 * @author suifeng
 * 日期: 2025/5/27
 */
public class CubeManager implements CubeRegistry {

    private static final CubeManager INSTANCE = new CubeManager();

    private final Map<GUID, Cube> cubes = new ConcurrentHashMap<>();
    private final Map<String, List<ExtensionPoint>> extensionPoints = new ConcurrentHashMap<>();
    private final Map<String, String> injectionPointMapping = new ConcurrentHashMap<>();

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
        if (cube == null) {
            throw new IllegalArgumentException("方块不能为空");
        }
        if (cubes.containsKey(cubeId)) {
            throw new IllegalStateException("方块ID已存在: " + cubeId);
        }

        cubes.put(cubeId, cube);
        cube.initialize();
        scanAndRegisterExtensions(cube);

        System.out.println("方块注册成功: " + cubeId + " [" + cube.getMetaData().getDescription() + "]");
    }

    public void unregisterCube(GUID cubeId) {
        Cube cube = cubes.remove(cubeId);
        if (cube != null) {
            // 先移除扩展点
            removeExtensionsByCube(cubeId);
            // 再销毁Cube
            cube.destroy();
            System.out.println("方块卸载成功: " + cubeId);
        }
    }

    public Cube getCube(GUID cubeId) {
        return cubes.get(cubeId);
    }

    public void registerExtensionPoint(String extensionId, ExtensionPoint extensionPoint) {
        extensionPoints.computeIfAbsent(extensionId, k -> new CopyOnWriteArrayList<>())
                .add(extensionPoint);
        System.out.println("扩展点注册成功: " + extensionId + " [" + extensionPoint.getExtensionName() + "]");
    }

    public List<ExtensionPoint> getExtensionPoints(String extensionId) {
        return extensionPoints.getOrDefault(extensionId, new CopyOnWriteArrayList<>());
    }

    public void bindInjectionPoint(String injectionPointId, String extensionId) {
        injectionPointMapping.put(injectionPointId, extensionId);
    }

    public String getExtensionIdByInjectionPoint(String injectionPointId) {
        return injectionPointMapping.get(injectionPointId);
    }

    private void scanAndRegisterExtensions(Cube cube) {
        Class<?> cubeClass = cube.getClass();

        for (java.lang.reflect.Method method : cubeClass.getDeclaredMethods()) {
            MExtension mExtension = method.getAnnotation(MExtension.class);
            if (mExtension != null) {
                String extensionName = mExtension.name().isEmpty() ? method.getName() : mExtension.name();
                String description = mExtension.description().isEmpty() ? "无描述" : mExtension.description();

                MethodExtensionPoint extensionPoint = new MethodExtensionPoint(
                        cube, method, mExtension.value(), extensionName, description
                );

                // 设置优先级
                extensionPoint.setPriority(mExtension.priority());

                // 注册到CubeManager
                registerExtensionPoint(mExtension.value(), extensionPoint);

                // 同时添加到Cube的MetaData中
                cube.getMetaData().addExtensionPoint(extensionPoint);
            }
        }
    }

    private void removeExtensionsByCube(GUID cubeId) {
        extensionPoints.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(ep -> ep instanceof MethodExtensionPoint &&
                    ((MethodExtensionPoint) ep).getCubeId().equals(cubeId));
            return entry.getValue().isEmpty();
        });
    }

    public void printAllExtensions() {
        System.out.println("========== 扩展点信息 ==========");
        extensionPoints.forEach((extensionId, points) -> {
            System.out.println("扩展点ID: " + extensionId);
            points.forEach(point -> {
                System.out.println("  - 名称: " + point.getExtensionName());
                System.out.println("  - 描述: " + point.getDescription());
                System.out.println("  - 优先级: " + point.getPriority());
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