package com.tml.mosaic.factory.definition;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ExtensionPackageDefinition {

    private final String id;
    private final String name;
    private final String description;
    private final String version;
    private final String className;
    private final String cubeId;
    private final List<ExtensionPointDefinition> extensionPoints = new ArrayList<>();
    
    public void addExtensionPoint(ExtensionPointDefinition epd) {
        extensionPoints.add(epd);
    }
}