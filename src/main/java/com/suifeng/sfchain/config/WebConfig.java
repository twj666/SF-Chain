package com.suifeng.sfchain.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源处理
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(3600);
                
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico")
                .setCachePeriod(3600);
                
        registry.addResourceHandler("/icons/**")
                .addResourceLocations("classpath:/static/icons/")
                .setCachePeriod(3600);
        
        // 配置SPA路由支持
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // 如果请求的资源存在，直接返回
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // 排除API请求，对于SPA路由返回index.html
                        if (!resourcePath.startsWith("api/") && 
                            !resourcePath.startsWith("sf-chain/") &&
                            !resourcePath.contains(".")) {  // 不包含文件扩展名的请求视为路由
                            Resource indexHtml = new ClassPathResource("/static/index.html");
                            if (indexHtml.exists()) {
                                return indexHtml;
                            }
                        }
                        
                        return null;
                    }
                });
    }
    
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 显式配置根路径映射到index.html
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}