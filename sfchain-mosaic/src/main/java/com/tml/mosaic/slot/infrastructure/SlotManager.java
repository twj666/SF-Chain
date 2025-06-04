package com.tml.mosaic.slot.infrastructure;

import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.slot.Slot;

/**
 * 槽管理器，复杂对槽进行管理
 */
public interface SlotManager {

    Slot getSlot(GUID slotId);

    Slot getSlot(String slotId);

    boolean registerSlot(Slot slot);

    void removeSlot(GUID slotId);

    void removeSlot(String slotId);

    boolean setup(GUID slotId, Slot.SetupCubeInfo setupCubeInfo);

    boolean unSetup(GUID slotId);
}
