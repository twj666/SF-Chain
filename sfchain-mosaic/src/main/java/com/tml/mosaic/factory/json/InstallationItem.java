package com.tml.mosaic.factory.json;

import lombok.Data;

import java.util.Map;

/**
 * 描述: 安装配置类
 * @author suifeng
 * 日期: 2025/6/7
 */
@Data
public class InstallationItem {

    private String type;
    private String location;
    private Map<String, Object> properties;
}