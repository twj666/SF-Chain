package com.tml.mosaic.core.infrastructure;

import com.tml.mosaic.core.guid.GuidAllocator;
import com.tml.mosaic.core.guid.GuuidAllocator;

/**
 * 公共基础基建 组件
 */
public class CommonComponent {

    protected static final GuidAllocator GUID_ALLOCATOR;

    static{
        GUID_ALLOCATOR = new GuuidAllocator();
    }

    public static GuidAllocator GuidAllocator(){
        return GUID_ALLOCATOR;
    }
}
