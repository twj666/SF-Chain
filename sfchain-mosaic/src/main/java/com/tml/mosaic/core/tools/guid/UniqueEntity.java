package com.tml.mosaic.core.tools.guid;

import com.tml.mosaic.slot.Slot;
import lombok.Getter;

import java.util.Objects;

/**
 * 实体
 */
public abstract class UniqueEntity {

    @Getter
    protected final GUID id;

    public UniqueEntity(GUID id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object target) {
        if(target instanceof UniqueEntity){
            if (target.getClass().equals(this.getClass())){
                GUID targetId = ((UniqueEntity) target).id;
                return Objects.equals(id, targetId);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
