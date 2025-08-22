package com.suifeng.sfchain.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * SF-Chain接口Authorization拦截器
 * 验证Authorization头是否为指定值
 * 
 * @author suifeng
 */
@Slf4j
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {
    
    @Value("${sf-chain.auth-token:suifeng666}")
    private String validToken;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // 跳过OPTIONS预检请求
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.debug("跳过OPTIONS预检请求: {}", requestURI);
            return true;
        }
        
        log.debug("拦截请求: {} [{}]", requestURI, method);
        
        // 获取Authorization头，同时检查大小写版本
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.trim().isEmpty()) {
            authorization = request.getHeader("authorization");
        }
        
        if (authorization == null || authorization.trim().isEmpty()) {
            log.warn("请求缺少Authorization头: {} [{}]", requestURI, method);
            sendUnauthorizedResponse(response, "缺少Authorization请求头");
            return false;
        }
        
        // 验证token值
        String trimmedAuth = authorization.trim();
        if (!validToken.equals(trimmedAuth)) {
            log.warn("Authorization验证失败: {} [{}] - 期望: [{}], 实际: [{}]", 
                requestURI, method, validToken, trimmedAuth);
            sendUnauthorizedResponse(response, "Authorization验证失败");
            return false;
        }
        
        log.debug("Authorization验证通过: {} [{}]", requestURI, method);
        return true;
    }
    
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
            "{\"error\":\"%s\",\"code\":%d,\"message\":\"%s\"}", 
            "Unauthorized", 
            HttpStatus.UNAUTHORIZED.value(), 
            message
        ));
    }
}