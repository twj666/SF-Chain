package com.suifeng.sfchain.core.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI调用日志记录切面
 */
@Slf4j
@Aspect
@Component
public class AICallLogAspect {
    
    @Autowired
    private AICallLogManager logManager;
    
    /**
     * 拦截BaseAIOperation的execute方法
     */
    @Around("execution(* com.suifeng.sfchain.core.BaseAIOperation.execute(..))")
    public Object logAIOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String callId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();
        long startMillis = System.currentTimeMillis();
        
        AICallLog.AICallLogBuilder logBuilder = AICallLog.builder()
                .callId(callId)
                .callTime(startTime)
                .frequency(1)
                .lastAccessTime(startTime);
        
        try {
            // 获取输入参数
            Object[] args = joinPoint.getArgs();
            Object input = args.length > 0 ? args[0] : null;
            String modelName = args.length > 1 ? (String) args[1] : null;
            
            logBuilder.input(input).modelName(modelName);
            
            // 执行原方法
            Object result = joinPoint.proceed();
            
            // 记录成功日志
            long duration = System.currentTimeMillis() - startMillis;
            AICallLog log = logBuilder
                    .status(AICallLog.CallStatus.SUCCESS)
                    .duration(duration)
                    .output(result)
                    .build();
            
            logManager.addLog(log);
            
            return result;
            
        } catch (Exception e) {
            // 记录失败日志
            long duration = System.currentTimeMillis() - startMillis;
            AICallLog log = logBuilder
                    .status(AICallLog.CallStatus.FAILED)
                    .duration(duration)
                    .errorMessage(e.getMessage())
                    .build();
            
            logManager.addLog(log);
            
            throw e;
        }
    }
}