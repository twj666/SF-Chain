package com.sfchain.mosaic.core;

import java.util.List;

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
     * 核心执行方法 - 使用ExtensionInput
     */
    private static MOutput executeInjectionPointInternal(String injectionPointId, MInput input) {
        CubeManager cubeManager = CubeManager.getInstance();

        try {
            String extensionId = cubeManager.getExtensionIdByInjectionPoint(injectionPointId);
            if (extensionId == null) {
                System.out.println("注入点未绑定扩展点: " + injectionPointId);
                return MOutput.failure("UNBOUND_INJECTION_POINT", "注入点未绑定扩展点");
            }

            List<ExtensionPoint> extensionPoints = cubeManager.getExtensionPoints(extensionId);
            if (extensionPoints.isEmpty()) {
                System.out.println("扩展点不存在: " + extensionId);
                return MOutput.failure("EXTENSION_NOT_FOUND", "扩展点不存在");
            }

            ExtensionPoint extensionPoint = extensionPoints.get(0);

            System.out.println("执行注入点: " + injectionPointId + " -> 扩展点: " + extensionId);

            return extensionPoint.execute(input);
        } catch (Exception e) {
            String errorMsg = "注入点执行异常: " + injectionPointId + ", 错误: " + e.getMessage();
            System.err.println(errorMsg);
            return MOutput.failure("INJECTION_EXECUTION_ERROR", errorMsg);
        }
    }

    /**
     * 主要调用方法 - 支持可变参数
     */
    public static MOutput executeInjectionPoint(String injectionPointId, Object... params) {
        MInput input = new MInput();

        // 自动封装参数
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                input.set("param" + i, params[i]);
            }

            // 同时提供一些常用的参数名映射，方便扩展点使用
            if (params.length >= 1) input.set("value", params[0]);
            if (params.length >= 2) input.set("second", params[1]);
            if (params.length >= 3) input.set("third", params[2]);
        }

        return executeInjectionPointInternal(injectionPointId, input);
    }

    /**
     * 绑定注入点到扩展点
     */
    public void bindInjectionPoint(String injectionPointId, String extensionId) {
        CubeManager cubeManager = CubeManager.getInstance();

        if (extensionId != null && cubeManager.getExtensionPoints(extensionId).isEmpty()) {
            System.err.println("警告: 扩展点不存在 " + extensionId);
        }

        cubeManager.bindInjectionPoint(injectionPointId, extensionId);
        System.out.println("绑定成功: " + injectionPointId + " -> " + extensionId);
    }

    /**
     * 解绑注入点
     */
    public void unbindInjectionPoint(String injectionPointId) {
        CubeManager.getInstance().bindInjectionPoint(injectionPointId, null);
        System.out.println("解绑成功: " + injectionPointId);
    }

    /**
     * 获取注入点信息
     */
    public void printInjectionPointInfo(String injectionPointId) {
        CubeManager cubeManager = CubeManager.getInstance();
        String extensionId = cubeManager.getExtensionIdByInjectionPoint(injectionPointId);

        System.out.println("========== 注入点信息 ==========");
        System.out.println("注入点ID: " + injectionPointId);
        System.out.println("绑定扩展点: " + (extensionId != null ? extensionId : "未绑定"));

        if (extensionId != null) {
            List<ExtensionPoint> extensionPoints = cubeManager.getExtensionPoints(extensionId);
            for (ExtensionPoint ep : extensionPoints) {
                System.out.println("扩展点名称: " + ep.getExtensionName());
                System.out.println("扩展点描述: " + ep.getDescription());
                if (ep instanceof MethodExtensionPoint) {
                    MethodExtensionPoint mep = (MethodExtensionPoint) ep;
                    System.out.println("所属方块: " + mep.getCubeId());
                    System.out.println("方法名: " + mep.getMethodName());
                }
            }
        }
        System.out.println("===============================");
    }
}