package com.tml.mosaic.slot;

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 槽构建器
 */
public abstract class SlotBuilder<T extends Slot> {

    public abstract T build(JSONObject initParams);

    @Getter
    private final Class<T> slotType;


    public SlotBuilder() {
        slotType = initSlotType();
    }

    private Class<T> initSlotType() {
        // 获取当前类的泛型父类（GenericClass<T>）
        Type genericSuperclass = this.getClass().getGenericSuperclass();

        if (genericSuperclass instanceof ParameterizedType) {
            // 获取泛型参数（T 的实际类型）
            ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

            if (actualTypeArguments.length > 0) {
                return (Class<T>) actualTypeArguments[0];
            } else {
                throw new IllegalStateException("No generic type found.");
            }
        } else {
            throw new IllegalStateException("Not a ParameterizedType.");
        }
    }


}
