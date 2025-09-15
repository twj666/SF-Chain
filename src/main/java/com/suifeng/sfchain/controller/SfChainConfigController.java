package com.suifeng.sfchain.controller;

import com.suifeng.sfchain.config.SfChainPathProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("${sf-chain.path.api-prefix:/sf-chain}/config")
public class SfChainConfigController {

    @Autowired
    private SfChainPathProperties pathProperties;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @GetMapping("/api-info")
    public Map<String, Object> getApiInfo() {
        Map<String, Object> config = new HashMap<>();
        
        // 构建基础URL
        String baseUrl = contextPath.isEmpty() ? "" : contextPath;
        
        // 获取配置的API前缀
        String apiPrefix = pathProperties.getFormattedApiPrefix();
        
        config.put("baseUrl", baseUrl);
        config.put("port", serverPort);
        config.put("contextPath", contextPath);
        config.put("endpoints", Map.of(
            "AI_MODELS", apiPrefix + "/models",
            "AI_OPERATIONS", apiPrefix + "/operations",
            "AI_CALL_LOGS", apiPrefix + "/call-logs",
            "AI_SYSTEM", apiPrefix + "/system"
        ));
        
        return config;
    }
}