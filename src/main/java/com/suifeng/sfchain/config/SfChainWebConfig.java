package com.suifeng.sfchain.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SF-Chain Web配置
 * 注册Authorization拦截器
 * 
 * @author suifeng
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sf-chain", name = "authEnabled", havingValue = "true")
public class SfChainWebConfig implements WebMvcConfigurer {
    
    private final AuthorizationInterceptor authorizationInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("注册SF-Chain Authorization拦截器");
        
        registry.addInterceptor(authorizationInterceptor)
                .addPathPatterns("/sf-chain/**")
                .excludePathPatterns(
                    "/sf-chain/config/**",  // 排除配置接口
                    "/**/*.js",             // 排除JS文件
                    "/**/*.css",            // 排除CSS文件
                    "/**/*.html",           // 排除HTML文件
                    "/**/*.ico",            // 排除图标文件
                    "/**/*.svg",            // 排除SVG文件
                    "/**/*.png",            // 排除PNG文件
                    "/**/*.jpg",            // 排除JPG文件
                    "/**/*.jpeg",           // 排除JPEG文件
                    "/**/*.gif",            // 排除GIF文件
                    "/**/*.woff",           // 排除字体文件
                    "/**/*.woff2",          // 排除字体文件
                    "/**/*.ttf",            // 排除字体文件
                    "/assets/**",           // 排除静态资源
                    "/static/**",           // 排除静态资源
                    "/",                    // 排除根路径
                    "/index.html"           // 排除首页
                )
                .order(1);
    }
}