package com.tml.mosaic.core;

import lombok.Data;

/**
 * 描述: 抽象扩展点
 * @author suifeng
 * 日期: 2025/5/27
 */
@Data
public abstract class AbstractExtensionPoint implements ExtensionPoint {

    protected String extensionId;
    protected String extensionName;
    protected String description;

    public AbstractExtensionPoint(String extensionId, String extensionName, String description) {
        this.extensionId = extensionId;
        this.extensionName = extensionName;
        this.description = description;
    }
}