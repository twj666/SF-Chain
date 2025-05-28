package com.tml.mosaic.core.guid;

import java.util.UUID;

public class GuuidAllocator implements GuidAllocator{

    @Override
    public GUID nextGUID() {
        return new GUUID(UUID.randomUUID().toString());
    }
}
