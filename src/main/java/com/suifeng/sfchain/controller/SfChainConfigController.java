package com.suifeng.sfchain.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/sf-chain/config")
public class SfChainConfigController {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @GetMapping("/api-info")
    public Map<String, Object> getApiInfo() {
        Map<String, Object> config = new HashMap<>();
        
        // 构建基础URL
        String baseUrl = contextPath.isEmpty() ? "" : contextPath;
        
        config.put("baseUrl", baseUrl);
        config.put("port", serverPort);
        config.put("contextPath", contextPath);
        config.put("endpoints", Map.of(
            "AI_MODELS", baseUrl + "/sf-chain/models",
            "AI_OPERATIONS", baseUrl + "/sf-chain/operations",
            "AI_CALL_LOGS", baseUrl + "/sf-chain/call-logs",
            "AI_SYSTEM", baseUrl + "/sf-chain/system"
        ));
        
        return config;
    }
}