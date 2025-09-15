package com.suifeng.sfchain.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import javax.servlet.ServletContext;
import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final SfChainPathProperties pathProperties;
    
    @Autowired(required = false)
    private ServletContext servletContext;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String webPrefix = pathProperties.getFormattedWebPrefix();
        
        // 如果webPrefix包含context-path，需要提取相对路径部分
        // 例如：webPrefix=/jeecg-boot/sf，context-path=/jeecg-boot，则使用/sf
        String relativePath = webPrefix;
        if (servletContext != null) {
            String contextPath = servletContext.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && webPrefix.startsWith(contextPath + "/")) {
                relativePath = webPrefix.substring(contextPath.length());
            }
        }
        
        // 配置静态资源处理 - 支持动态前缀
        registry.addResourceHandler(relativePath + "/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(3600);
                
        registry.addResourceHandler(relativePath + "/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico")
                .setCachePeriod(3600);
                
        registry.addResourceHandler(relativePath + "/icons/**")
                .addResourceLocations("classpath:/static/icons/")
                .setCachePeriod(3600);
        
        // SPA路由支持现在由IndexController处理，避免路径冲突
    }
    
    // 前端页面路由现在由IndexController处理，支持动态路径替换
}