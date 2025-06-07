package com.tml.mosaic.factory.definition;

import lombok.Data;

@Data
public class ExtensionPointDefinition {

    private final String id;
    private final String methodName;
    private final String extensionName;
    private final int priority;
    private final String description;
    private final boolean asyncFlag;
    private final Class<?> returnType;
    private final Class<?>[] parameterTypes;
    
    @Override
    public String toString() {
        return "ExtensionPointDefinition{" +
                "id='" + id + '\'' +
                ", methodName='" + methodName + '\'' +
                ", extensionName='" + extensionName + '\'' +
                ", priority=" + priority +
                '}';
    }
}