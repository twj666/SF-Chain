package com.tml.mosaic.slot;

import com.tml.mosaic.core.tools.guid.DotNotationId;
import com.tml.mosaic.core.tools.guid.GUID;
import lombok.Getter;

import java.util.Objects;

/**
 * 放置Cube的槽，用于引入Cube插件来进行部署和使用
 */
public abstract class Slot {

    // 槽唯一 id
    @Getter
    protected final GUID slotId;

    protected Slot(String slotName) {
        slotId = new DotNotationId(slotName);
    }

    /**
     * 安装方块信息
     * @param setupCubeInfo
     */
    public abstract boolean Setup(SetupCubeInfo setupCubeInfo);

    /**
     * 取消方块安装
     */
    public abstract boolean UnSetup();

    @Override
    public boolean equals(Object o) {
        if (o instanceof Slot){
            Slot slot = (Slot) o;
            return Objects.equals(slot.getSlotId(), this.slotId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return slotId.hashCode();
    }
}
