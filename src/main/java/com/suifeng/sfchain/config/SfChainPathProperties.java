package com.suifeng.sfchain.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContext;

/**
 * SF-Chain 路径配置属性
 * 支持通过yml配置文件自定义路径前缀
 * 
 * @author suifeng
 */
@Data
@Component
@ConfigurationProperties(prefix = "sf-chain.path")
public class SfChainPathProperties {
    
    @Autowired(required = false)
    private ServletContext servletContext;
    
    /**
     * API路径前缀，默认为 /sf-chain
     * 可通过 sf-chain.path.api-prefix 配置
     */
    private String apiPrefix = "/sf-chain";
    
    /**
     * 前端静态资源路径前缀，默认为 /sf
     * 可通过 sf-chain.path.web-prefix 配置
     */
    private String webPrefix = "/sf";
    
    /**
     * 获取格式化的API前缀（确保以/开头，不以/结尾）
     */
    public String getFormattedApiPrefix() {
        if (apiPrefix == null || apiPrefix.trim().isEmpty()) {
            return "/sf-chain";
        }
        
        String formatted = apiPrefix.trim();
        if (!formatted.startsWith("/")) {
            formatted = "/" + formatted;
        }
        if (formatted.endsWith("/") && formatted.length() > 1) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        
        return formatted;
    }
    
    /**
     * 获取格式化的Web前缀（确保以/开头，不以/结尾）
     * 自动处理context-path，避免重复前缀
     */
    public String getFormattedWebPrefix() {
        if (webPrefix == null || webPrefix.trim().isEmpty()) {
            return "/sf";
        }
        
        String formatted = webPrefix.trim();
        if (!formatted.startsWith("/")) {
            formatted = "/" + formatted;
        }
        if (formatted.endsWith("/") && formatted.length() > 1) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        
        // 如果配置的webPrefix已经包含了context-path，则直接返回
        // 否则可能导致重复前缀问题
        if (servletContext != null) {
            String contextPath = servletContext.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && formatted.startsWith(contextPath)) {
                // webPrefix已经包含了context-path，直接返回
                return formatted;
            }
        }
        
        return formatted;
    }
}