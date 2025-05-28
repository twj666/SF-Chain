package com.tml.mosaic.core.guid;

import java.util.Objects;

public class GUUID implements GUID{

    private final String uuid;

    public GUUID(String uuid){
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GUUID){
            GUUID uuid1 = (GUUID) o;
            return Objects.equals(uuid, uuid1.getUuid());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
}
