package com.suifeng.sfchain.controller;

import com.suifeng.sfchain.config.SfChainPathProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 基础控制器类
 * 提供动态路径前缀支持
 * 
 * @author suifeng
 */
@RequiredArgsConstructor
public abstract class BaseController {
    
    @Autowired
    protected SfChainPathProperties pathProperties;
    
    /**
     * 获取API路径前缀
     */
    protected String getApiPrefix() {
        return pathProperties.getFormattedApiPrefix();
    }
    
    /**
     * 获取Web路径前缀
     */
    protected String getWebPrefix() {
        return pathProperties.getFormattedWebPrefix();
    }
}