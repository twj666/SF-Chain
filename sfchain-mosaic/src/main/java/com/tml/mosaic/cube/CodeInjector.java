package com.tml.mosaic.cube;

import com.tml.mosaic.core.tools.guid.GUID;

/**
 * 描述: 代码注入器
 * @author suifeng
 * 日期: 2025/5/27
 */
public class CodeInjector {

    private static final CodeInjector INSTANCE = new CodeInjector();

    private CodeInjector() {}

    public static CodeInjector getInstance() {
        return INSTANCE;
    }

    /**
     * 核心执行方法 - 通过注入点找到Cube，再执行扩展点
     */
    private static PointResult executeInjectionPointInternal(String injectionPointId, PointParam input) {
        CubeManager cubeManager = CubeManager.getInstance();

        try {
            // 第一步：通过注入点找到对应的Cube
            Cube targetCube = cubeManager.getCubeByInjectionPoint(injectionPointId);
            if (targetCube == null) {
                String errorMsg = "注入点未绑定到任何Cube: " + injectionPointId;
                System.out.println(errorMsg);
                return PointResult.failure("UNBOUND_INJECTION_POINT", errorMsg);
            }

            // 第二步：让Cube执行对应的扩展点
            System.out.println("执行注入点: " + injectionPointId + " -> Cube[" + targetCube.getCubeId() + "]");

            return targetCube.executeExtensionPoint(injectionPointId, input);

        } catch (Exception e) {
            String errorMsg = "注入点执行异常: " + injectionPointId + ", 错误: " + e.getMessage();
            System.err.println(errorMsg);
            e.printStackTrace();
            return PointResult.failure("INJECTION_EXECUTION_ERROR", errorMsg);
        }
    }

    /**
     * 主要调用方法 - 支持可变参数
     */
    public static PointResult executeInjectionPoint(String injectionPointId, Object... params) {
        PointParam input = createPointParam(params);
        return executeInjectionPointInternal(injectionPointId, input);
    }

    /**
     * 绑定注入点到Cube
     */
    public void bindInjectionPointToCube(String injectionPointId, GUID cubeId) {
        CubeManager.getInstance().bindInjectionPointToCube(injectionPointId, cubeId);
    }

    /**
     * 便捷方法：绑定注入点到扩展点（通过Cube查找）
     */
    public void bindInjectionPoint(String injectionPointId, String extensionId) {
        CubeManager cubeManager = CubeManager.getInstance();

        // 查找包含指定扩展点的Cube
        Cube targetCube = findCubeByExtensionId(extensionId);
        if (targetCube != null) {
            cubeManager.bindInjectionPointToCube(injectionPointId, targetCube.getCubeId());
            System.out.println("通过扩展点绑定成功: " + injectionPointId + " -> 扩展点[" + extensionId + "] -> Cube[" + targetCube.getCubeId() + "]");
        } else {
            System.err.println("警告: 未找到包含扩展点[" + extensionId + "]的Cube");
        }
    }

    /**
     * 解绑注入点
     */
    public void unbindInjectionPoint(String injectionPointId) {
        CubeManager.getInstance().unbindInjectionPoint(injectionPointId);
    }

    /**
     * 查找包含指定扩展点的Cube
     */
    private Cube findCubeByExtensionId(String extensionId) {
        CubeManager cubeManager = CubeManager.getInstance();

        return cubeManager.getCubes().values().stream()
                .filter(cube -> cube.hasExtensionPoint(extensionId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 创建参数对象
     */
    private static PointParam createPointParam(Object... params) {
        PointParam input = new PointParam();

        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                input.set("param" + i, params[i]);
            }

            // 提供常用参数名映射
            if (params.length >= 1) input.set("value", params[0]);
            if (params.length >= 2) input.set("second", params[1]);
            if (params.length >= 3) input.set("third", params[2]);
        }

        return input;
    }

    /**
     * 获取注入点详细信息
     */
    public void printInjectionPointInfo(String injectionPointId) {
        CubeManager cubeManager = CubeManager.getInstance();
        Cube cube = cubeManager.getCubeByInjectionPoint(injectionPointId);

        System.out.println("========== 注入点信息 ==========");
        System.out.println("注入点ID: " + injectionPointId);

        if (cube != null) {
            System.out.println("绑定Cube: " + cube.getCubeId() + " [" + cube.getMetaData().getName() + "]");
            System.out.println("Cube描述: " + cube.getMetaData().getDescription());
            System.out.println("可用扩展点:");
            cube.getMetaData().getExtensionPoints().forEach(ep -> {
                System.out.println("  - " + ep.getExtensionId() + " [" + ep.getExtensionName() + "]");
            });
        } else {
            System.out.println("绑定状态: 未绑定");
        }
        System.out.println("===============================");
    }
}