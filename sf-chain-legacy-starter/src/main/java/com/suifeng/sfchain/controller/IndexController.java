package com.suifeng.sfchain.controller;

import com.suifeng.sfchain.config.SfChainPathProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 前端页面控制器
 * 处理index.html的动态路径替换
 * 
 * @author suifeng
 */
@Controller
@RequiredArgsConstructor
public class IndexController {
    
    private final SfChainPathProperties pathProperties;
    
    /**
     * 处理前端首页请求，动态替换HTML中的资源路径
     * 排除静态资源文件（.js, .css, .ico, .png, .jpg, .svg等）
     */
    @GetMapping(value = {
        "${sf-chain.path.web-prefix:/sf}",
        "${sf-chain.path.web-prefix:/sf}/"
    })
    public ResponseEntity<String> index() {
        
        try {
            // 读取静态的index.html文件
            ClassPathResource resource = new ClassPathResource("static/index.html");
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // 读取文件内容
            String content;
            try (Scanner scanner = new Scanner(resource.getInputStream(), StandardCharsets.UTF_8)) {
                content = scanner.useDelimiter("\\A").next();
            }
            
            // 获取配置的前缀
            String webPrefix = pathProperties.getFormattedWebPrefix();
            
            // 动态替换HTML中的资源路径
            // 替换 href="/sf/xxx" 为 href="{webPrefix}/xxx"
            content = content.replaceAll("href=\"/sf/([^\"]*)\"", "href=\"" + webPrefix + "/$1\"");
            // 替换 src="/sf/xxx" 为 src="{webPrefix}/xxx"
            content = content.replaceAll("src=\"/sf/([^\"]*)\"", "src=\"" + webPrefix + "/$1\"");
            
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    

}