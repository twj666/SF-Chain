package com.sfchain.spring.service;

import com.sfchain.core.exception.OperationException;
import com.sfchain.core.model.AIModel;
import com.sfchain.core.operation.AIOperation;
import com.sfchain.core.registry.ModelRegistry;
import com.sfchain.core.registry.OperationRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 描述: AI服务类，提供执行AI操作的主要入口
 * @author suifeng
 * 日期: 2025/4/15
 */
@RequiredArgsConstructor
@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private final OperationRegistry operationRegistry;
    private final ModelRegistry modelRegistry;

    /**
     * 执行AI操作
     *
     * @param operationType 操作类型
     * @param modelType 模型类型
     * @param params 操作参数
     * @param <P> 参数类型
     * @param <R> 结果类型
     * @return 操作结果
     */
    public <P, R> R execute(String operationType, String modelType, P params) {
        logger.debug("Executing operation: {} with model: {}", operationType, modelType);

        try {
            // 获取操作和模型
            @SuppressWarnings("unchecked")
            AIOperation<P, R> operation = (AIOperation<P, R>) operationRegistry.getOperation(operationType);

            // 验证模型是否被支持
            if (!operation.supportedModels().contains(modelType)) {
                throw new OperationException(operationType,
                        "Operation does not support model: " + modelType);
            }

            // 验证参数
            operation.validate(params);

            // 获取模型
            AIModel model = modelRegistry.getModel(modelType);

            // 构建提示词
            String prompt = operation.buildPrompt(params);
            logger.debug("Generated prompt: {}", prompt);

            // 生成内容
            String response = model.generate(prompt);
            logger.debug("Received response: {}", response);

            // 解析响应
            return operation.parseResponse(response);
        } catch (Exception e) {
            if (e instanceof OperationException) {
                throw e;
            }
            throw new OperationException(operationType, "Failed to execute operation", e);
        }
    }
}