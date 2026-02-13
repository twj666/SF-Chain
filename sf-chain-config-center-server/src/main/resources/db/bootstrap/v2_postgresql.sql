CREATE TABLE IF NOT EXISTS sfchain_cp_tenants (
  tenant_id VARCHAR(64) PRIMARY KEY,
  tenant_name VARCHAR(128) NOT NULL,
  description VARCHAR(512),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sfchain_cp_apps (
  id BIGSERIAL PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  app_id VARCHAR(128) NOT NULL,
  app_name VARCHAR(128) NOT NULL,
  description VARCHAR(512),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_sfchain_cp_apps_tenant_app UNIQUE (tenant_id, app_id)
);

CREATE TABLE IF NOT EXISTS sfchain_cp_api_keys (
  id BIGSERIAL PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  app_id VARCHAR(128) NOT NULL,
  key_name VARCHAR(128) NOT NULL,
  key_prefix VARCHAR(32) NOT NULL,
  secret_hash VARCHAR(128) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  expires_at TIMESTAMP,
  last_used_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sfchain_cp_model_configs (
  id BIGSERIAL PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  app_id VARCHAR(128) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  provider VARCHAR(64) NOT NULL,
  base_url VARCHAR(512),
  api_key_ref BIGINT,
  config_json JSONB,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_sfchain_cp_model UNIQUE (tenant_id, app_id, model_name)
);

CREATE TABLE IF NOT EXISTS sfchain_cp_operation_configs (
  id BIGSERIAL PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  app_id VARCHAR(128) NOT NULL,
  operation_type VARCHAR(128) NOT NULL,
  model_name VARCHAR(128),
  config_json JSONB,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_sfchain_cp_operation UNIQUE (tenant_id, app_id, operation_type)
);

CREATE TABLE IF NOT EXISTS sfchain_cp_config_releases (
  id BIGSERIAL PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  app_id VARCHAR(128) NOT NULL,
  version VARCHAR(64) NOT NULL,
  snapshot_json JSONB NOT NULL,
  published_by VARCHAR(128),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_sfchain_cp_release UNIQUE (tenant_id, app_id, version)
);

CREATE INDEX IF NOT EXISTS idx_sfchain_cp_api_keys_tenant_app ON sfchain_cp_api_keys(tenant_id, app_id);
CREATE INDEX IF NOT EXISTS idx_sfchain_cp_models_tenant_app ON sfchain_cp_model_configs(tenant_id, app_id);
CREATE INDEX IF NOT EXISTS idx_sfchain_cp_ops_tenant_app ON sfchain_cp_operation_configs(tenant_id, app_id);

DROP TABLE IF EXISTS sfchain_cp_agent_instances;
