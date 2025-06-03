package com.tml.mosaic.core.tools.guid;

import lombok.Getter;

import java.util.Objects;

/**
 * 点分命名ID
 */
public class DotNotationId implements GUID{

    private final String[] notations;

    @Getter
    private final String dotNotationId;

    public DotNotationId(String str) {
        this.dotNotationId = str;
        this.notations = dotNotationId.split("\\.");
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DotNotationId){
            DotNotationId dotNotationId1 = (DotNotationId) o;
            return Objects.equals(dotNotationId, dotNotationId1.getDotNotationId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dotNotationId);
    }


}
