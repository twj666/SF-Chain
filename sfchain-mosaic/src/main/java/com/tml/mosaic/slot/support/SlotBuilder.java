package com.tml.mosaic.slot.support;

import com.tml.mosaic.core.tools.guid.DotNotationId;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.slot.Slot;
import com.tml.mosaic.slot.infrastructure.GenericSlotManager;
import com.tml.mosaic.slot.infrastructure.SlotComponent;
import com.tml.mosaic.slot.infrastructure.SlotManager;

import java.util.Objects;
import java.util.Optional;

/**
 * 槽构建器
 */
public class SlotBuilder {

    public static SlotManager manager = new GenericSlotManager();

    public static BuilderContext builder(SlotManager manager) {
        return new BuilderContext(manager);
    }

   static class BuilderContext{

        private GUID slotId;

        private Slot.SetupCubeInfo setupCubeInfo;

        private SlotManager manager;

        protected BuilderContext(SlotManager manager) {
            this.setupCubeInfo = new Slot.SetupCubeInfo();
            this.manager = manager;
        }

        public BuilderContext slotId(String slotId) {
            this.slotId = new DotNotationId(slotId);
            return this;
        }

        public BuilderContext cubeId(GUID cubeId) {
            this.setupCubeInfo.setCubeId(cubeId);
            return this;
        }

        public BuilderContext methodName(String methodName) {
            this.setupCubeInfo.setMethodName(methodName);
            return this;
        }

        public Optional<Slot> build() {
           Slot slot = null;
           if(Objects.nonNull(slotId)) {
               slot = new Slot((DotNotationId) slotId);
               slot.Setup(setupCubeInfo);
               manager.registerSlot(slot);
           }
           return Optional.ofNullable(slot);
        }
    }
}
