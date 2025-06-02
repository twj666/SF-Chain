package com.tml.mosaic.slot;

import com.alibaba.fastjson2.JSONObject;

public class GenericSlotBuilder extends SlotBuilder<GenericSlot>{

    @Override
    public GenericSlot build(JSONObject initParams) {
        String slotName;
        try {
            slotName = initParams.getString("slotName");
        }catch (Exception e){
            return null;
        }
        return new GenericSlot(slotName);
    }
}
