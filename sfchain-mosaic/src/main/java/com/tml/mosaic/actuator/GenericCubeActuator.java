package com.tml.mosaic.actuator;

import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.cube.Cube;
import com.tml.mosaic.cube.ExtensionPoint;
import com.tml.mosaic.slot.Slot;

public class GenericCubeActuator implements CubeActuator{

    @Override
    public Object execute(Slot slot, Object...args) {
        Slot.SetupCubeInfo setupCubeInfo = slot.getSetupCubeInfo();
        if(setupCubeInfo != null){
            GUID cubeId = setupCubeInfo.getCubeId();

            // TODO 从cube中获取方法
            Cube cube = new Cube() {
            };

            Cube.MetaData metaData = cube.getMetaData();
//            ExtensionPoint extensionPoint = metaData.findExtensionPoint(setupCubeInfo.getMethodId());

            // TODO 反射执行方法
        }
        return null;
    }
}
