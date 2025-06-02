package com.tml.mosaic.slot;

public class GenericSlot extends Slot{

    private SetupCubeInfo setupCubeInfo;

    public GenericSlot(String slotName) {
        super(slotName);
    }

    @Override
    public boolean Setup(SetupCubeInfo setupCubeInfo) {
        this.setupCubeInfo = setupCubeInfo;
        return true;
    }

    @Override
    public boolean UnSetup() {
        this.setupCubeInfo = null;
        return true;
    }
}
