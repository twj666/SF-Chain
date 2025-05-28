package com.tml.mosaic.slot;

public class CommonSlot extends Slot{

    private SetupCubeInfo setupCubeInfo;

    public CommonSlot(String slotName) {
        super(slotName);
    }

    @Override
    public void Setup(SetupCubeInfo setupCubeInfo) {
        this.setupCubeInfo = setupCubeInfo;
    }
}
