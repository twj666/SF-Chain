# Persistence Package - 持久化包文档

## 概述
`persistence`包负责SF-Chain框架中所有数据的持久化管理，包括模型配置、操作配置、调用日志和会话上下文的存储。支持多种存储后端，包括MySQL、PostgreSQL和内存存储。

## 架构设计

### 1. 分层架构
```
persistence/
├── config/          # 持久化配置
├── context/         # 会话上下文管理
├── entity/          # 数据库实体
├── repository/      # 数据访问层
├── PersistenceService.java      # 持久化服务接口
├── PersistenceManager.java    # 持久化管理器
└── PersistenceServiceFactory.java  # 工厂类
```

### 2. 存储类型支持
- **MySQL**: 生产环境推荐
- **PostgreSQL**: 企业级应用
- **内存存储**: 测试和开发环境

## 核心组件详解

### 1. PersistenceManager - 持久化管理器
**文件**: `PersistenceManager.java`

#### 功能概述
- 统一管理所有持久化操作
- 提供配置同步和备份功能
- 处理配置的热加载和版本管理

#### 核心功能

##### 模型配置管理
```java
// 添加模型配置
public void addModelConfig(String modelName, ModelConfigData config)

// 更新模型配置
public void updateModelConfig(String modelName, ModelConfigData config)

// 删除模型配置
public void deleteModelConfig(String modelName)

// 获取模型配置
public Optional<ModelConfigData> getModelConfig(String modelName)

// 获取所有模型配置
public Map<String, ModelConfigData> getAllModelConfigs()
```

##### 操作配置管理
```java
// 保存操作配置
public void saveOperationConfig(String operationType, OperationConfigData config)

// 获取操作配置
public Optional<OperationConfigData> getOperationConfig(String operationType)

// 获取所有操作配置
public Map<String, OperationConfigData> getAllOperationConfigs()
```

##### 配置同步
```java
// 创建配置备份
public String createBackup(String backupName)

// 从备份恢复
public void restoreFromBackup(String backupName)

// 刷新配置
public void flushConfigurations()

// 重新加载配置
public void reloadConfigurations()
```

### 2. PersistenceService - 持久化服务接口
**文件**: `PersistenceService.java`

#### 接口定义
```java
public interface PersistenceService {
    
    // 模型配置相关
    void saveModelConfig(String modelName, ModelConfigData config);
    Optional<ModelConfigData> getModelConfig(String modelName);
    Map<String, ModelConfigData> getAllModelConfigs();
    boolean existsModelConfig(String modelName);
    void deleteModelConfig(String modelName);
    
    // 操作配置相关
    void saveOperationConfig(String operationType, OperationConfigData config);
    Optional<OperationConfigData> getOperationConfig(String operationType);
    Map<String, OperationConfigData> getAllOperationConfigs();
    void deleteOperationConfig(String operationType);
    
    // 调用日志相关
    void saveCallLog(AICallLog log);
    List<AICallLog> getCallLogs(LocalDateTime startTime, LocalDateTime endTime, String operationType);
    void cleanupOldLogs(int daysToKeep);
    
    // 会话上下文相关
    void saveChatContext(String sessionId, List<ChatMessage> messages);
    List<ChatMessage> getChatContext(String sessionId);
    void deleteChatContext(String sessionId);
    boolean existsChatContext(String sessionId);
    
    // 系统管理
    void createBackup(String backupName);
    void restoreFromBackup(String backupName);
    List<String> listBackups();
}
```

### 3. ChatContextService - 会话上下文服务
**文件**: `context/ChatContextService.java`

#### 功能概述
- 管理用户会话的对话历史
- 支持会话的持久化和恢复
- 提供上下文清理和管理功能

#### 接口定义
```java
public interface ChatContextService {
    
    // 添加用户消息
    void addUserMessage(String sessionId, String message);
    
    // 添加AI回复
    void addAiResponse(String sessionId, String response);
    
    // 设置系统提示词
    void setSystemPrompt(String sessionId, String systemPrompt);
    
    // 获取上下文
    List<ChatMessage> getContext(String sessionId);
    
    // 获取上下文字符串
    String getContextAsString(String sessionId, boolean includeSystemPrompt);
    
    // 清理对话历史
    void clearConversation(String sessionId);
    
    // 完全清理会话
    void clearSession(String sessionId);
    
    // 检查会话是否存在
    boolean sessionExists(String sessionId);
}
```

#### 实现类
- **MapBasedChatContextService**: 基于内存的实现，适合测试环境
- **PersistentChatContextService**: 基于持久化的实现，适合生产环境

### 4. 数据实体

#### ModelConfigEntity - 模型配置实体
**文件**: `entity/ModelConfigEntity.java`

```java
@Entity
@Table(name = "sf_model_config")
public class ModelConfigEntity {
    
    @Id
    private String modelName;
    
    @Column(columnDefinition = "TEXT")
    private String configData;  // JSON格式的配置数据
    
    private Boolean enabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;  // 乐观锁版本号
}
```

#### OperationConfigEntity - 操作配置实体
**文件**: `entity/OperationConfigEntity.java`

```java
@Entity
@Table(name = "sf_operation_config")
public class OperationConfigEntity {
    
    @Id
    private String operationType;
    
    @Column(columnDefinition = "TEXT")
    private String configData;  // JSON格式的配置数据
    
    private String modelName;
    private Boolean enabled;
    private Integer maxTokens;
    private Double temperature;
    private Boolean requireJsonOutput;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
}
```

### 5. 存储实现

#### MySQLPersistenceService - MySQL实现
**文件**: `MySQLPersistenceService.java`

##### 数据库表结构
```sql
-- 模型配置表
CREATE TABLE sf_model_config (
    model_name VARCHAR(100) PRIMARY KEY,
    config_data JSON,
    enabled BOOLEAN DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- 操作配置表
CREATE TABLE sf_operation_config (
    operation_type VARCHAR(100) PRIMARY KEY,
    config_data JSON,
    model_name VARCHAR(100),
    enabled BOOLEAN DEFAULT TRUE,
    max_tokens INT,
    temperature DOUBLE,
    require_json_output BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- 调用日志表
CREATE TABLE sf_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_type VARCHAR(100),
    model_name VARCHAR(100),
    request_data TEXT,
    response_data TEXT,
    success BOOLEAN,
    duration_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_at (created_at)
);

-- 会话上下文表
CREATE TABLE sf_chat_context (
    session_id VARCHAR(100) PRIMARY KEY,
    context_data JSON,
    system_prompt TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 配置备份表
CREATE TABLE sf_config_backup (
    backup_name VARCHAR(100) PRIMARY KEY,
    backup_data JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);
```

#### PostgreSQLPersistenceService - PostgreSQL实现
**文件**: `PostgreSQLPersistenceService.java`

##### PostgreSQL特有特性
- 使用JSONB类型存储配置数据
- 支持全文搜索
- 支持数组类型
- 更好的JSON查询性能

```sql
-- PostgreSQL特有的JSONB支持
CREATE TABLE sf_model_config (
    model_name VARCHAR(100) PRIMARY KEY,
    config_data JSONB,
    enabled BOOLEAN DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- 创建GIN索引优化JSON查询
CREATE INDEX idx_model_config_gin ON sf_model_config USING GIN (config_data);
```

#### MemoryPersistenceService - 内存实现
**文件**: 内存实现（未单独列出）

##### 特点
- 基于ConcurrentHashMap实现
- 适合测试环境
- 数据不会持久化到磁盘
- 重启后数据丢失

## 配置管理

### 1. 配置数据结构

#### ModelConfigData - 模型配置数据
**文件**: `ModelConfigData.java`

```java
@Data
public class ModelConfigData {
    private String modelName;
    private String apiKey;
    private String baseUrl;
    private String description;
    private Boolean enabled;
    private Map<String, String> headers;
    private Integer timeout;
    private Integer maxRetries;
    
    public boolean isValid() {
        return modelName != null && !modelName.trim().isEmpty()
            && apiKey != null && !apiKey.trim().isEmpty()
            && baseUrl != null && !baseUrl.trim().isEmpty();
    }
}
```

#### OperationConfigData - 操作配置数据
**文件**: `OperationConfigData.java`

```java
@Data
public class OperationConfigData {
    private String operationType;
    private String modelName;
    private Boolean enabled;
    private Integer maxTokens;
    private Double temperature;
    private Boolean requireJsonOutput;
    private Boolean supportThinking;
    private Integer timeoutSeconds;
    private Integer retryCount;
    
    public boolean isValid() {
        return operationType != null && !operationType.trim().isEmpty()
            && modelName != null && !modelName.trim().isEmpty();
    }
}
```

### 2. 配置版本管理

#### 版本控制策略
- 使用乐观锁（@Version）防止并发更新
- 每次更新自动增加版本号
- 支持配置的历史记录查询

#### 配置变更通知
```java
@Service
public class ConfigurationChangeNotifier {
    
    @EventListener
    public void handleConfigChange(ConfigurationUpdatedEvent event) {
        // 通知相关组件配置已变更
        applicationEventPublisher.publishEvent(
            new ConfigurationReloadEvent(event.getConfigType(), event.getConfigKey())
        );
    }
}
```

## 数据访问层

### 1. Repository接口

#### ModelConfigRepository - 模型配置仓库
**文件**: `repository/ModelConfigRepository.java`

```java
@Repository
public interface ModelConfigRepository extends JpaRepository<ModelConfigEntity, String> {
    
    List<ModelConfigEntity> findByEnabledTrue();
    
    @Query("SELECT m FROM ModelConfigEntity m WHERE m.modelName LIKE %:keyword%")
    List<ModelConfigEntity> searchByName(@Param("keyword") String keyword);
    
    @Modifying
    @Query("UPDATE ModelConfigEntity m SET m.enabled = :enabled WHERE m.modelName = :modelName")
    int updateEnabledStatus(@Param("modelName") String modelName, @Param("enabled") boolean enabled);
}
```

#### OperationConfigRepository - 操作配置仓库
**文件**: `repository/OperationConfigRepository.java`

```java
@Repository
public interface OperationConfigRepository extends JpaRepository<OperationConfigEntity, String> {
    
    List<OperationConfigEntity> findByEnabledTrue();
    
    List<OperationConfigEntity> findByModelName(String modelName);
    
    @Query("SELECT o FROM OperationConfigEntity o WHERE o.modelName IS NOT NULL")
    List<OperationConfigEntity> findConfiguredOperations();
}
```

### 2. 数据迁移

#### 数据库初始化
**文件**: `DatabaseInitializationService.java`

```java
@Service
public class DatabaseInitializationService {
    
    @PostConstruct
    public void initializeDatabase() {
        // 创建必要的表和索引
        createTablesIfNotExist();
        
        // 插入默认配置
        insertDefaultConfigurations();
        
        // 创建必要的索引
        createIndexes();
    }
    
    private void insertDefaultConfigurations() {
        // 插入默认的OpenAI模型配置
        ModelConfigData gpt4 = new ModelConfigData();
        gpt4.setModelName("gpt-4");
        gpt4.setApiKey("${OPENAI_API_KEY}");
        gpt4.setBaseUrl("https://api.openai.com");
        gpt4.setEnabled(true);
        gpt4.setDescription("OpenAI GPT-4模型");
        
        persistenceService.saveModelConfig("gpt-4", gpt4);
    }
}
```

## 备份与恢复

### 1. 备份策略

#### 自动备份
- 每日定时备份
- 保留最近30天的备份
- 支持增量备份

#### 手动备份
```java
@RestController
@RequestMapping("/sf-chain/backup")
public class BackupController {
    
    @PostMapping("/create")
    public ResponseEntity<String> createBackup(@RequestParam String name) {
        persistenceManager.createBackup(name);
        return ResponseEntity.ok("备份创建成功: " + name);
    }
    
    @PostMapping("/restore/{name}")
    public ResponseEntity<String> restoreBackup(@PathVariable String name) {
        persistenceManager.restoreFromBackup(name);
        return ResponseEntity.ok("备份恢复成功: " + name);
    }
}
```

### 2. 备份格式

#### JSON格式备份
```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "version": "1.0.0",
  "model_configs": {
    "gpt-4": {
      "apiKey": "***",
      "baseUrl": "https://api.openai.com",
      "enabled": true,
      "description": "GPT-4模型"
    }
  },
  "operation_configs": {
    "jsonRepair": {
      "modelName": "gpt-4",
      "enabled": true,
      "maxTokens": 2000,
      "temperature": 0.3
    }
  }
}
```

## 性能优化

### 1. 数据库优化

#### 索引策略
```sql
-- 为查询频繁的字段创建索引
CREATE INDEX idx_call_log_operation_time ON sf_call_log(operation_type, created_at);
CREATE INDEX idx_call_log_success ON sf_call_log(success);

-- 复合索引优化查询
CREATE INDEX idx_operation_config_model_enabled ON sf_operation_config(model_name, enabled);
```

#### 连接池配置
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
```

### 2. 缓存策略

#### 配置缓存
- 模型配置缓存：TTL 1小时
- 操作配置缓存：TTL 30分钟
- 会话上下文缓存：TTL 24小时

#### 缓存实现
```java
@Service
public class CachedPersistenceService implements PersistenceService {
    
    @Cacheable(value = "model-config", key = "#modelName")
    public Optional<ModelConfigData> getModelConfig(String modelName) {
        return delegate.getModelConfig(modelName);
    }
    
    @CacheEvict(value = "model-config", key = "#modelName")
    public void saveModelConfig(String modelName, ModelConfigData config) {
        delegate.saveModelConfig(modelName, config);
    }
}
```

## 监控与诊断

### 1. 数据库监控

#### 查询性能监控
```java
@Component
public class DatabaseMetrics {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @EventListener
    public void handleQueryExecution(QueryExecutionEvent event) {
        meterRegistry.timer("db.query.duration", "query", event.getQueryName())
            .record(event.getDuration().toMillis(), TimeUnit.MILLISECONDS);
    }
}
```

#### 连接池监控
```java
@Configuration
public class DataSourceMetrics {
    
    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMetricRegistry(meterRegistry);
        return dataSource;
    }
}
```