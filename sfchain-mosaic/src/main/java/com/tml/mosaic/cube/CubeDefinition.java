package com.tml.mosaic.cube;

import com.tml.mosaic.core.tools.guid.GUID;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 描述: Cube的定义信息
 * @author suifeng
 * 日期: 2025/6/6
 */
@Slf4j
public class CubeDefinition {

    private GUID id;
    private String name;
    private String version;
    private String description;
    private String model;
    private final List<ExtensionPoint> extensionPoints = new CopyOnWriteArrayList<>();

    public Cube getBean() {
        return null;
    }
}