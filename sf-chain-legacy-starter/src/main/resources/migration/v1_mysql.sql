-- 创建AI相关数据表 (MySQL版本)
-- 作者: suifeng
-- 日期: 2025/1/27

-- 创建模型配置表
CREATE TABLE IF NOT EXISTS sfchain_model_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_name VARCHAR(100) NOT NULL UNIQUE,
    provider VARCHAR(50) NOT NULL,
    api_key VARCHAR(500),
    base_url VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT true,
    description VARCHAR(1000),
    custom_params JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建操作配置表（直接包含模型名称字段）
CREATE TABLE IF NOT EXISTS sfchain_operation_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_type VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(1000),
    enabled BOOLEAN NOT NULL DEFAULT true,
    max_tokens INTEGER,
    temperature DOUBLE,
    json_output BOOLEAN NOT NULL DEFAULT false,
    thinking_mode BOOLEAN NOT NULL DEFAULT false,
    custom_params JSON,
    model_name VARCHAR(100), -- 直接在操作配置表中存储关联的模型名称
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建索引
CREATE INDEX idx_sfchain_model_configs_model_name ON sfchain_model_configs(model_name);
CREATE INDEX idx_sfchain_model_configs_provider ON sfchain_model_configs(provider);
CREATE INDEX idx_sfchain_model_configs_enabled ON sfchain_model_configs(enabled);
CREATE INDEX idx_sfchain_operation_configs_operation_type ON sfchain_operation_configs(operation_type);
CREATE INDEX idx_sfchain_operation_configs_enabled ON sfchain_operation_configs(enabled);
CREATE INDEX idx_sfchain_operation_configs_model ON sfchain_operation_configs(model_name);

-- 添加外键约束
ALTER TABLE sfchain_operation_configs 
    ADD CONSTRAINT fk_operation_model 
    FOREIGN KEY (model_name) REFERENCES sfchain_model_configs(model_name) 
    ON DELETE SET NULL;

-- 仅在表为空时插入示例数据
INSERT INTO sfchain_model_configs (model_name, provider, enabled, description) 
SELECT * FROM (
    SELECT 'deepseek-chat' as model_name, 'deepseek' as provider, true as enabled, 'DeepSeek Chat模型' as description
    UNION ALL
    SELECT 'gpt-4o', 'openai', true, 'OpenAI GPT-4o模型'
    UNION ALL
    SELECT 'siliconflow-qwen', 'siliconflow', true, 'SiliconFlow Qwen模型'
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM sfchain_model_configs LIMIT 1
);

-- 提交事务
COMMIT;