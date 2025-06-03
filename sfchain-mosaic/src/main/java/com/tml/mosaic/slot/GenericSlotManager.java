package com.tml.mosaic.slot;

import com.tml.mosaic.core.tools.guid.GUID;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GenericSlotManager implements SlotManager{

    private Map<GUID, Slot> slotMap = new HashMap<>();

    @Override
    public Slot getSlot(GUID slotId) {
        return slotMap.get(slotId);
    }

    @Override
    public void registerSlot(Slot slot) {
        // TODO 日志
        slotMap.put(slot.getSlotId(), slot);
    }

    @Override
    public void removeSlot(GUID slotId) {
        slotMap.remove(slotId);
    }

    @Override
    public boolean setup(GUID slotId, SetupCubeInfo setupCubeInfo) {
        Slot slot = getSlot(slotId);
        if(Objects.nonNull(slot)){
            return slot.Setup(setupCubeInfo);
        }
        return false;
    }

    @Override
    public boolean unSetup(GUID slotId) {
        Slot slot = getSlot(slotId);
        if(Objects.nonNull(slot)){
            return slot.UnSetup();
        }
        return false;
    }
}
