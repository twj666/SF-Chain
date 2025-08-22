-- 创建AI相关数据表
-- 作者: suifeng
-- 日期: 2025/1/27

-- 创建模型配置表
CREATE TABLE IF NOT EXISTS sfchain_model_configs (
    id BIGSERIAL PRIMARY KEY,
    model_name VARCHAR(100) NOT NULL UNIQUE,
    provider VARCHAR(50) NOT NULL,
    api_key VARCHAR(500),
    base_url VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT true,
    description VARCHAR(1000),
    custom_params JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建操作配置表（直接包含模型名称字段）
CREATE TABLE IF NOT EXISTS sfchain_operation_configs (
    id BIGSERIAL PRIMARY KEY,
    operation_type VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(1000),
    enabled BOOLEAN NOT NULL DEFAULT true,
    max_tokens INTEGER,
    temperature DOUBLE PRECISION,
    json_output BOOLEAN NOT NULL DEFAULT false,
    thinking_mode BOOLEAN NOT NULL DEFAULT false,
    custom_params JSONB,
    model_name VARCHAR(100), -- 直接在操作配置表中存储关联的模型名称
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_sfchain_model_configs_model_name ON sfchain_model_configs(model_name);
CREATE INDEX IF NOT EXISTS idx_sfchain_model_configs_provider ON sfchain_model_configs(provider);
CREATE INDEX IF NOT EXISTS idx_sfchain_model_configs_enabled ON sfchain_model_configs(enabled);
CREATE INDEX IF NOT EXISTS idx_sfchain_operation_configs_operation_type ON sfchain_operation_configs(operation_type);
CREATE INDEX IF NOT EXISTS idx_sfchain_operation_configs_enabled ON sfchain_operation_configs(enabled);
CREATE INDEX IF NOT EXISTS idx_sfchain_operation_configs_model ON sfchain_operation_configs(model_name);

-- 添加外键约束
ALTER TABLE sfchain_operation_configs 
    ADD CONSTRAINT fk_operation_model 
    FOREIGN KEY (model_name) REFERENCES sfchain_model_configs(model_name) 
    ON DELETE SET NULL;

-- 创建更新时间戳的触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为每个表创建更新时间戳触发器
CREATE TRIGGER update_sfchain_model_configs_updated_at 
    BEFORE UPDATE ON sfchain_model_configs 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sfchain_operation_configs_updated_at 
    BEFORE UPDATE ON sfchain_operation_configs 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 插入一些示例数据
INSERT INTO sfchain_model_configs (model_name, provider, enabled, description) VALUES 
('deepseek-chat', 'deepseek', true, 'DeepSeek Chat模型'),
('gpt-4o', 'openai', true, 'OpenAI GPT-4o模型'),
('siliconflow-qwen', 'siliconflow', true, 'SiliconFlow Qwen模型')
ON CONFLICT (model_name) DO NOTHING;

-- 提交事务
COMMIT;