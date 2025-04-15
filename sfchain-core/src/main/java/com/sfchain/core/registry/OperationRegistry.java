package com.sfchain.core.registry;

import com.sfchain.core.annotation.AIOp;
import com.sfchain.core.exception.SFChainException;
import com.sfchain.core.operation.AIOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述: 操作注册中心，管理所有可用的AI操作
 * @author suifeng
 * 日期: 2025/4/15
 */
@Component
public class OperationRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(OperationRegistry.class);
    
    /**
     * 操作映射，键为操作名称，值为操作实例
     */
    private final Map<String, AIOperation<?, ?>> operationMap = new ConcurrentHashMap<>();
    
    /**
     * 构造函数，自动注册所有操作
     * 
     * @param operations Spring自动注入的操作列表
     */
    @Autowired
    public OperationRegistry(List<AIOperation<?, ?>> operations) {
        operations.forEach(op -> {
            AIOp annotation = op.getClass().getAnnotation(AIOp.class);
            if (annotation != null) {
                String operationName = annotation.value();
                operationMap.put(operationName, op);
                logger.info("Registered AI Operation: {}", operationName);
            } else {
                logger.warn("Skipped registering operation without @AIOp annotation: {}", op.getClass().getName());
            }
        });
    }
    
    /**
     * 获取指定名称的操作
     * 
     * @param operationName 操作名称
     * @return 操作实例
     * @throws SFChainException 如果操作未注册
     */
    public AIOperation<?, ?> getOperation(String operationName) {
        return Optional.ofNullable(operationMap.get(operationName)).orElseThrow(() -> new SFChainException("Unregistered operation: " + operationName));
    }
    
    /**
     * 获取所有注册的操作
     * 
     * @return 操作映射
     */
    public Map<String, AIOperation<?, ?>> getAllOperations() {
        return Map.copyOf(operationMap);
    }
}