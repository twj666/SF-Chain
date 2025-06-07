package com.tml.mosaic.factory.context.json;

import lombok.Data;

import java.util.List;

/**
 * 描述: 启动配置类
 * @author suifeng
 * 日期: 2025/6/7
 */
@Data
public class InstallationConfig {

    private List<InstallationItem> installations;
}
