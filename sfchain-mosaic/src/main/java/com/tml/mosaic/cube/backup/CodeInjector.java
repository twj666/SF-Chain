package com.tml.mosaic.cube.backup;

import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.cube.PointParam;
import com.tml.mosaic.cube.PointResult;
import lombok.extern.slf4j.Slf4j;

/**
 * 描述: 代码注入器
 * @author suifeng
 * 日期: 2025/5/27
 */
@Slf4j
public class CodeInjector {

    private static final CodeInjector INSTANCE = new CodeInjector();

    private CodeInjector() {}

    public static CodeInjector getInstance() {
        return INSTANCE;
    }

    private static PointResult executeInjectionPointInternal(String injectionPointId, PointParam input) {
        CubeManager cubeManager = CubeManager.getInstance();

        try {
            Cube targetCube = cubeManager.getCubeByInjectionPoint(injectionPointId);
            if (targetCube == null) {
                log.warn("✗ 注入点执行失败 | {} | 原因: 未绑定到任何Cube", injectionPointId);
                return PointResult.failure("UNBOUND_INJECTION_POINT", "注入点未绑定到任何Cube");
            }

            log.debug("→ 执行注入点 | {} → Cube[{}] ({})",
                    injectionPointId, targetCube.getCubeId(), targetCube.getMetaData().getName());

            PointResult result = targetCube.executeExtensionPoint(injectionPointId, input);

            if (result.isSuccess()) {
                log.debug("✓ 注入点执行成功 | {} | 耗时: 完成", injectionPointId);
            } else {
                log.warn("✗ 注入点执行失败 | {} | 错误: {}", injectionPointId, result.getMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("✗ 注入点执行异常 | {} | 异常: {}", injectionPointId, e.getMessage(), e);
            return PointResult.failure("INJECTION_EXECUTION_ERROR", "注入点执行异常: " + e.getMessage());
        }
    }

    public static PointResult executeInjectionPoint(String injectionPointId, Object... params) {
        PointParam input = createPointParam(params);
        return executeInjectionPointInternal(injectionPointId, input);
    }

    public void bindInjectionPointToCube(String injectionPointId, GUID cubeId) {
        CubeManager.getInstance().bindInjectionPointToCube(injectionPointId, cubeId);
    }

    public void bindInjectionPoint(String injectionPointId, String extensionId) {
        CubeManager cubeManager = CubeManager.getInstance();

        Cube targetCube = findCubeByExtensionId(extensionId);
        if (targetCube != null) {
            cubeManager.bindInjectionPointToCube(injectionPointId, targetCube.getCubeId());
            log.info("✓ 扩展点绑定成功 | {} → 扩展点[{}] → Cube[{}]",
                    injectionPointId, extensionId, targetCube.getCubeId());
        } else {
            log.warn("⚠ 扩展点绑定失败 | {} | 原因: 未找到包含扩展点[{}]的Cube", injectionPointId, extensionId);
        }
    }

    public void unbindInjectionPoint(String injectionPointId) {
        CubeManager.getInstance().unbindInjectionPoint(injectionPointId);
    }

    private Cube findCubeByExtensionId(String extensionId) {
        CubeManager cubeManager = CubeManager.getInstance();

        return cubeManager.getCubes().values().stream()
                .filter(cube -> cube.hasExtensionPoint(extensionId))
                .findFirst()
                .orElse(null);
    }

    private static PointParam createPointParam(Object... params) {
        PointParam input = new PointParam();

        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                input.set("param" + i, params[i]);
            }

            if (params.length >= 1) input.set("value", params[0]);
            if (params.length >= 2) input.set("second", params[1]);
            if (params.length >= 3) input.set("third", params[2]);
        }

        return input;
    }

    public void printInjectionPointInfo(String injectionPointId) {
        CubeManager cubeManager = CubeManager.getInstance();
        Cube cube = cubeManager.getCubeByInjectionPoint(injectionPointId);

        log.info("========== 注入点详情 ==========");
        log.info("注入点ID: {}", injectionPointId);

        if (cube != null) {
            log.info("绑定Cube: {} [{}]", cube.getCubeId(), cube.getMetaData().getName());
            log.info("Cube描述: {}", cube.getMetaData().getDescription());
            log.info("可用扩展点数量: {}", cube.getMetaData().getExtensionPoints().size());

            cube.getMetaData().getExtensionPoints().forEach(ep -> {
                log.info("  → {} [{}] (优先级: {})", ep.getExtensionId(), ep.getExtensionName(), ep.getPriority());
            });
        } else {
            log.info("绑定状态: 未绑定");
        }
        log.info("==============================");
    }
}