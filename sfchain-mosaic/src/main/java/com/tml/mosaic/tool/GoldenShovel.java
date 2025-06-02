package com.tml.mosaic.tool;

import com.alibaba.fastjson2.JSONObject;
import com.tml.mosaic.actuator.CubeActuator;
import com.tml.mosaic.actuator.GenericCubeActuator;
import com.tml.mosaic.core.guid.GUID;
import com.tml.mosaic.slot.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 金铲铲
 * slot创建：使用slotBuilder进行槽的创建
 * cube执行：使用CubeActuator执行器执行
 */
public class GoldenShovel {

    // 方块执行器
    private static final CubeActuator actuator = new GenericCubeActuator();

    // 槽管理器
    private static final SlotManager slotManager = new GenericSlotManager();

    // 槽构建器
    private static final SlotBuilder slotBuilder = new GenericSlotBuilder();

    public static DigContext dig(){
        return new DigContext();
    }

    public static class DigContext{

        private Slot slot;

        private final JSONObject slotBuildParams = new JSONObject();

        private SetupCubeInfo setupCubeInfo = new SetupCubeInfo();


        public DigContext slotName(String slotName) {
            slotBuildParams.put("slotName", slotName);
            return this;
        }

        public DigContext slotParams(Map<String, Object> slotParams) {
            slotBuildParams.putAll(slotParams);
            return this;
        }

        public DigContext executeInfo(String methodId, Object...params) {
            setupCubeInfo.setMethodId(methodId);
            setupCubeInfo.setParams(params);
            return this;
        }

        public Object execute(){
            this.slot = slotBuilder.build(slotBuildParams);
            this.slot.Setup(setupCubeInfo);
            slotManager.registerSlot(this.slot);
            return actuator.execute(this.slot);
        }
    }
}
