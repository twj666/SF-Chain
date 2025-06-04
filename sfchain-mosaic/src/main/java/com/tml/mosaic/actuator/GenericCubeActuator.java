package com.tml.mosaic.actuator;

import com.tml.mosaic.core.frame.Cube;
import com.tml.mosaic.core.frame.CubeManager;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.slot.Slot;

public class GenericCubeActuator implements CubeActuator{

    @Override
    public Object execute(Slot slot) {
        Slot.SetupCubeInfo setupCubeInfo = slot.getSetupCubeInfo();
        if(setupCubeInfo != null){
            GUID cubeId = setupCubeInfo.getCubeId();
            Cube cube = CubeManager.getInstance().getCube(cubeId);
            // TODO 从cube中获取方法
        }
        return null;
    }
}
