package com.tml.mosaic.actuator;

import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.slot.Slot;

/**
 * 方块执行器
 */
public interface CubeActuator {
    // TODO execute方法改写
    Object execute(Slot slot, Object...args);
}
