package com.tml.mosaic.slot;

import com.tml.mosaic.core.guid.GUID;

/**
 * 槽管理器，复杂对槽进行管理
 */
public interface SlotManager {

    Slot getSlot(GUID slotId);

    void registerSlot(Slot slot);

    void removeSlot(GUID slotId);

    boolean setup(GUID slotId, SetupCubeInfo setupCubeInfo);

    boolean unSetup(GUID slotId);
}
