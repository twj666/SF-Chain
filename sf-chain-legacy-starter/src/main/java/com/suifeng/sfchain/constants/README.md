# Constants Package - 常量包文档

## 概述
`constants`包定义了SF-Chain框架中所有核心的常量、枚举和配置键，为整个框架提供统一的配置管理和标准化定义。

## 包结构
```
constants/
├── SfChainConstants.java      # 核心常量定义
├── SfChainConfigKeys.java     # 配置键定义
├── SfChainExceptionCode.java  # 异常码定义
├── ModelProvider.java         # 模型提供商枚举
├── OperationType.java         # 操作类型枚举
└── ResponseCode.java        # 响应码枚举
```

## 核心常量详解

### 1. SfChainConstants - 核心常量
**文件**: `SfChainConstants.java`

#### 框架基础常量
```java
public class SfChainConstants {
    
    // 框架版本信息
    public static final String VERSION = "1.0.0";
    public static final String BUILD_DATE = "2024-01-01";
    
    // 系统标识
    public static final String FRAMEWORK_NAME = "sf-chain";
    public static final String SYSTEM_NAME = "SF-Chain AI Framework";
    
    // 默认配置
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String DEFAULT_CONTENT_TYPE = "application/json";
    
    // 时间相关常量
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;
    public static final int DEFAULT_RETRY_COUNT = 3;
    public static final int DEFAULT_RETRY_DELAY_MS = 1000;
    
    // 大小限制
    public static final int MAX_REQUEST_SIZE = 10 * 1024 * 1024; // 10MB
    public static final int MAX_RESPONSE_SIZE = 50 * 1024 * 1024; // 50MB
    public static final int MAX_PROMPT_LENGTH = 10000;
    
    // 缓存相关
    public static final int DEFAULT_CACHE_TTL_SECONDS = 3600; // 1小时
    public static final int DEFAULT_CACHE_MAX_SIZE = 1000;
    
    // 线程池配置
    public static final int CORE_POOL_SIZE = 10;
    public static final int MAX_POOL_SIZE = 50;
    public static final int QUEUE_CAPACITY = 1000;
    public static final int KEEP_ALIVE_SECONDS = 60;
    
    // 日志配置
    public static final String LOG_FORMAT = "[{}] [{}] {}";
    public static final String LOG_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    
    // 文件路径配置
    public static final String CONFIG_FILE_NAME = "sf-chain.yml";
    public static final String DEFAULT_CONFIG_PATH = "/config/sf-chain.yml";
    public static final String BACKUP_PATH = "/backups";
    public static final String LOGS_PATH = "/logs";
    
    // HTTP相关常量
    public static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
    public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HTTP_HEADER_USER_AGENT = "User-Agent";
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_GET = "GET";
    
    // 错误消息模板
    public static final String ERROR_INVALID_CONFIG = "Invalid configuration: {}";
    public static final String ERROR_OPERATION_FAILED = "Operation {} failed: {}";
    public static final String ERROR_MODEL_NOT_FOUND = "Model {} not found";
    public static final String ERROR_TIMEOUT = "Request timeout after {} seconds";
    
    // 成功消息模板
    public static final String SUCCESS_OPERATION_COMPLETED = "Operation {} completed successfully";
    public static final String SUCCESS_MODEL_REGISTERED = "Model {} registered successfully";
    public static final String SUCCESS_CONFIG_UPDATED = "Configuration updated successfully";
}
```

### 2. SfChainConfigKeys - 配置键定义
**文件**: `SfChainConfigKeys.java`

#### 配置键分层结构
```java
public class SfChainConfigKeys {
    
    // 根配置键
    public static final String ROOT = "sf-chain";
    
    // 模型配置键
    public static final String MODELS = ROOT + ".models";
    public static final String MODEL_ENABLED = ".enabled";
    public static final String MODEL_API_KEY = ".api-key";
    public static final String MODEL_BASE_URL = ".base-url";
    public static final String MODEL_TIMEOUT = ".timeout";
    public static final String MODEL_MAX_RETRIES = ".max-retries";
    public static final String MODEL_DESCRIPTION = ".description";
    
    // OpenAI特定配置
    public static final String OPENAI = MODELS + ".openai";
    public static final String OPENAI_GPT4 = OPENAI + ".gpt-4";
    public static final String OPENAI_GPT35 = OPENAI + ".gpt-3.5-turbo";
    public static final String OPENAI_DALLE3 = OPENAI + ".dall-e-3";
    
    // 操作配置键
    public static final String OPERATIONS = ROOT + ".operations";
    public static final String OPERATION_MODEL = ".model";
    public static final String OPERATION_ENABLED = ".enabled";
    public static final String OPERATION_MAX_TOKENS = ".max-tokens";
    public static final String OPERATION_TEMPERATURE = ".temperature";
    public static final String OPERATION_TIMEOUT = ".timeout";
    public static final String OPERATION_RETRY_COUNT = ".retry-count";
    public static final String OPERATION_REQUIRE_JSON = ".require-json-output";
    
    // 具体操作配置
    public static final String JSON_REPAIR = OPERATIONS + ".json-repair";
    public static final String CODE_GENERATION = OPERATIONS + ".code-generation";
    public static final String DOCUMENT_SUMMARY = OPERATIONS + ".document-summary";
    public static final String TRANSLATION = OPERATIONS + ".translation";
    
    // 持久化配置键
    public static final String PERSISTENCE = ROOT + ".persistence";
    public static final String PERSISTENCE_TYPE = PERSISTENCE + ".type";
    public static final String PERSISTENCE_URL = PERSISTENCE + ".url";
    public static final String PERSISTENCE_USERNAME = PERSISTENCE + ".username";
    public static final String PERSISTENCE_PASSWORD = PERSISTENCE + ".password";
    public static final String PERSISTENCE_DRIVER = PERSISTENCE + ".driver-class-name";
    
    // 缓存配置键
    public static final String CACHE = ROOT + ".cache";
    public static final String CACHE_ENABLED = CACHE + ".enabled";
    public static final String CACHE_TTL = CACHE + ".ttl-seconds";
    public static final String CACHE_MAX_SIZE = CACHE + ".max-size";
    
    // 日志配置键
    public static final String LOGGING = ROOT + ".logging";
    public static final String LOGGING_LEVEL = LOGGING + ".level";
    public static final String LOGGING_FILE = LOGGING + ".file";
    public static final String LOGGING_MAX_SIZE = LOGGING + ".max-file-size";
    public static final String LOGGING_MAX_FILES = LOGGING + ".max-backup-files";
    
    // 监控配置键
    public static final String MONITORING = ROOT + ".monitoring";
    public static final String MONITORING_ENABLED = MONITORING + ".enabled";
    public static final String MONITORING_ENDPOINTS = MONITORING + ".endpoints";
    public static final String MONITORING_METRICS = MONITORING + ".metrics";
    
    // 安全配置键
    public static final String SECURITY = ROOT + ".security";
    public static final String SECURITY_ENABLED = SECURITY + ".enabled";
    public static final String SECURITY_API_KEY = SECURITY + ".api-key";
    public static final String SECURITY_RATE_LIMIT = SECURITY + ".rate-limit";
    
    // 线程池配置键
    public static final String THREAD_POOL = ROOT + ".thread-pool";
    public static final String THREAD_POOL_CORE_SIZE = THREAD_POOL + ".core-size";
    public static final String THREAD_POOL_MAX_SIZE = THREAD_POOL + ".max-size";
    public static final String THREAD_POOL_QUEUE_CAPACITY = THREAD_POOL + ".queue-capacity";
    public static final String THREAD_POOL_KEEP_ALIVE = THREAD_POOL + ".keep-alive-seconds";
}
```

### 3. ModelProvider - 模型提供商枚举
**文件**: `ModelProvider.java`

#### 支持的模型提供商
```java
public enum ModelProvider {
    
    OPENAI("OpenAI", "https://api.openai.com", "openai"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com", "anthropic"),
    GOOGLE("Google", "https://generativelanguage.googleapis.com", "google"),
    MICROSOFT("Microsoft", "https://api.openai.com", "microsoft"),
    HUGGINGFACE("Hugging Face", "https://api-inference.huggingface.co", "huggingface"),
    COHERE("Cohere", "https://api.cohere.ai", "cohere"),
    STABILITY_AI("Stability AI", "https://api.stability.ai", "stability"),
    LOCAL("Local", "http://localhost:8080", "local"),
    CUSTOM("Custom", "", "custom");
    
    private final String displayName;
    private final String defaultBaseUrl;
    private final String configKey;
    
    ModelProvider(String displayName, String defaultBaseUrl, String configKey) {
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.configKey = configKey;
    }
    
    public static ModelProvider fromString(String provider) {
        for (ModelProvider mp : values()) {
            if (mp.name().equalsIgnoreCase(provider) || 
                mp.configKey.equalsIgnoreCase(provider)) {
                return mp;
            }
        }
        return CUSTOM;
    }
}
```

### 4. OperationType - 操作类型枚举
**文件**: `OperationType.java`

#### 预定义操作类型
```java
public enum OperationType {
    
    // 文本处理
    TEXT_GENERATION("text-generation", "文本生成"),
    TEXT_SUMMARY("text-summary", "文本摘要"),
    TEXT_TRANSLATION("text-translation", "文本翻译"),
    TEXT_CLASSIFICATION("text-classification", "文本分类"),
    SENTIMENT_ANALYSIS("sentiment-analysis", "情感分析"),
    
    // 代码相关
    CODE_GENERATION("code-generation", "代码生成"),
    CODE_REVIEW("code-review", "代码审查"),
    CODE_EXPLANATION("code-explanation", "代码解释"),
    CODE_REFACTORING("code-refactoring", "代码重构"),
    BUG_FIXING("bug-fixing", "Bug修复"),
    
    // 数据格式
    JSON_REPAIR("json-repair", "JSON修复"),
    JSON_VALIDATION("json-validation", "JSON验证"),
    XML_PROCESSING("xml-processing", "XML处理"),
    CSV_PROCESSING("csv-processing", "CSV处理"),
    
    // 创意生成
    CREATIVE_WRITING("creative-writing", "创意写作"),
    STORY_GENERATION("story-generation", "故事生成"),
    POEM_GENERATION("poem-generation", "诗歌生成"),
    
    // 问答系统
    QUESTION_ANSWERING("question-answering", "问答系统"),
    KNOWLEDGE_BASE("knowledge-base", "知识库问答"),
    CHAT_CONVERSATION("chat-conversation", "对话聊天"),
    
    // 图像处理
    IMAGE_GENERATION("image-generation", "图像生成"),
    IMAGE_ANALYSIS("image-analysis", "图像分析"),
    IMAGE_EDITING("image-editing", "图像编辑"),
    
    // 文档处理
    DOCUMENT_SUMMARY("document-summary", "文档摘要"),
    DOCUMENT_ANALYSIS("document-analysis", "文档分析"),
    DOCUMENT_TRANSLATION("document-translation", "文档翻译"),
    
    // 数据提取
    DATA_EXTRACTION("data-extraction", "数据提取"),
    ENTITY_EXTRACTION("entity-extraction", "实体提取"),
    KEYWORD_EXTRACTION("keyword-extraction", "关键词提取"),
    
    // 验证和测试
    MODEL_VALIDATION("model-validation", "模型验证"),
    PROMPT_TESTING("prompt-testing", "提示测试"),
    PERFORMANCE_TESTING("performance-testing", "性能测试"),
    
    // 自定义操作
    CUSTOM_OPERATION("custom", "自定义操作");
    
    private final String code;
    private final String description;
    
    OperationType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public static OperationType fromCode(String code) {
        for (OperationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return CUSTOM_OPERATION;
    }
}
```

### 5. ResponseCode - 响应码枚举
**文件**: `ResponseCode.java`

#### 标准化响应码
```java
public enum ResponseCode {
    
    // 成功响应
    SUCCESS(200, "成功"),
    CREATED(201, "创建成功"),
    ACCEPTED(202, "已接受"),
    
    // 客户端错误
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "方法不允许"),
    CONFLICT(409, "资源冲突"),
    UNPROCESSABLE_ENTITY(422, "无法处理的实体"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    
    // 服务器错误
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),
    
    // 业务错误
    MODEL_NOT_FOUND(1001, "模型不存在"),
    OPERATION_NOT_FOUND(1002, "操作不存在"),
    CONFIG_INVALID(1003, "配置无效"),
    API_KEY_INVALID(1004, "API密钥无效"),
    RATE_LIMIT_EXCEEDED(1005, "超出频率限制"),
    TIMEOUT_EXCEEDED(1006, "请求超时"),
    RETRY_EXCEEDED(1007, "重试次数超出限制"),
    
    // AI相关错误
    AI_MODEL_ERROR(2001, "AI模型错误"),
    AI_RESPONSE_INVALID(2002, "AI响应无效"),
    AI_SERVICE_UNAVAILABLE(2003, "AI服务不可用"),
    AI_TOKEN_LIMIT_EXCEEDED(2004, "超出Token限制"),
    
    // 数据相关错误
    DATA_PERSISTENCE_ERROR(3001, "数据持久化错误"),
    DATA_VALIDATION_ERROR(3002, "数据验证错误"),
    DATA_NOT_FOUND(3003, "数据不存在"),
    DATA_CONFLICT(3004, "数据冲突"),
    
    // 系统错误
    SYSTEM_INITIALIZATION_ERROR(4001, "系统初始化错误"),
    SYSTEM_CONFIG_ERROR(4002, "系统配置错误"),
    SYSTEM_RESOURCE_EXHAUSTED(4003, "系统资源耗尽");
    
    private final int code;
    private final String message;
    
    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public boolean isSuccess() {
        return this.code >= 200 && this.code < 300;
    }
    
    public boolean isClientError() {
        return this.code >= 400 && this.code < 500;
    }
    
    public boolean isServerError() {
        return this.code >= 500;
    }
}
```

### 6. SfChainExceptionCode - 异常码定义
**文件**: `SfChainExceptionCode.java`

#### 异常码规范
```java
public class SfChainExceptionCode {
    
    // 基础异常码范围：1000-1999
    public static final int BASE_EXCEPTION = 1000;
    public static final int CONFIG_EXCEPTION = 1001;
    public static final int VALIDATION_EXCEPTION = 1002;
    public static final int NETWORK_EXCEPTION = 1003;
    
    // AI相关异常码：2000-2999
    public static final int AI_EXCEPTION = 2000;
    public static final int AI_CONNECTION_EXCEPTION = 2001;
    public static final int AI_TIMEOUT_EXCEPTION = 2002;
    public static final int AI_RESPONSE_EXCEPTION = 2003;
    public static final int AI_RATE_LIMIT_EXCEPTION = 2004;
    public static final int AI_AUTHENTICATION_EXCEPTION = 2005;
    public static final int AI_MODEL_NOT_FOUND_EXCEPTION = 2006;
    
    // 数据相关异常码：3000-3999
    public static final int DATA_EXCEPTION = 3000;
    public static final int DATA_PERSISTENCE_EXCEPTION = 3001;
    public static final int DATA_RETRIEVAL_EXCEPTION = 3002;
    public static final int DATA_VALIDATION_EXCEPTION = 3003;
    public static final int DATA_CONFLICT_EXCEPTION = 3004;
    
    // 系统相关异常码：4000-4999
    public static final int SYSTEM_EXCEPTION = 4000;
    public static final int SYSTEM_INITIALIZATION_EXCEPTION = 4001;
    public static final int SYSTEM_RESOURCE_EXCEPTION = 4002;
    public static final int SYSTEM_CONFIG_EXCEPTION = 4003;
    
    // 操作相关异常码：5000-5999
    public static final int OPERATION_EXCEPTION = 5000;
    public static final int OPERATION_NOT_FOUND_EXCEPTION = 5001;
    public static final int OPERATION_EXECUTION_EXCEPTION = 5002;
    public static final int OPERATION_VALIDATION_EXCEPTION = 5003;
    
    // 用户相关异常码：6000-6999
    public static final int USER_EXCEPTION = 6000;
    public static final int USER_AUTHENTICATION_EXCEPTION = 6001;
    public static final int USER_AUTHORIZATION_EXCEPTION = 6002;
    public static final int USER_INPUT_EXCEPTION = 6003;
    
    // 工具方法
    public static String getExceptionMessage(int code) {
        switch (code) {
            case AI_CONNECTION_EXCEPTION:
                return "无法连接到AI服务";
            case AI_TIMEOUT_EXCEPTION:
                return "AI服务响应超时";
            case AI_RESPONSE_EXCEPTION:
                return "AI服务返回无效响应";
            case DATA_PERSISTENCE_EXCEPTION:
                return "数据持久化失败";
            case SYSTEM_INITIALIZATION_EXCEPTION:
                return "系统初始化失败";
            default:
                return "未知错误";
        }
    }
}
```

## 使用示例

### 1. 配置键使用
```java
// 获取OpenAI GPT-4配置
String gpt4ApiKey = config.getString(SfChainConfigKeys.OPENAI_GPT4 + SfChainConfigKeys.MODEL_API_KEY);
Integer gpt4Timeout = config.getInteger(SfChainConfigKeys.OPENAI_GPT4 + SfChainConfigKeys.MODEL_TIMEOUT);

// 检查JSON修复操作配置
boolean jsonRepairEnabled = config.getBoolean(
    SfChainConfigKeys.JSON_REPAIR + SfChainConfigKeys.OPERATION_ENABLED);
```

### 2. 枚举使用
```java
// 使用ModelProvider枚举
ModelProvider provider = ModelProvider.OPENAI;
String baseUrl = provider.getDefaultBaseUrl();

// 使用OperationType枚举
OperationType operationType = OperationType.JSON_REPAIR;
String operationCode = operationType.getCode();

// 使用ResponseCode枚举
ResponseCode code = ResponseCode.SUCCESS;
if (code.isSuccess()) {
    // 处理成功响应
}
```

### 3. 异常码使用
```java
// 抛出带异常码的异常
throw new SfChainException(SfChainExceptionCode.AI_TIMEOUT_EXCEPTION, 
    "AI服务响应超时");

// 根据异常码获取消息
String errorMessage = SfChainExceptionCode.getExceptionMessage(errorCode);
```

## 扩展指南

### 1. 添加新的模型提供商
```java
public enum ModelProvider {
    // 现有枚举...
    NEW_PROVIDER("New Provider", "https://api.new-provider.com", "new-provider");
    
    // 构造函数和getter保持不变
}
```

### 2. 添加新的操作类型
```java
public enum OperationType {
    // 现有枚举...
    NEW_OPERATION("new-operation", "新操作类型");
    
    // 构造函数和getter保持不变
}
```

### 3. 添加新的配置键
```java
public class SfChainConfigKeys {
    // 在适当的位置添加新配置键
    public static final String NEW_FEATURE = ROOT + ".new-feature";
    public static final String NEW_FEATURE_ENABLED = NEW_FEATURE + ".enabled";
}
```

## 最佳实践

### 1. 配置管理
- 所有配置键都应该在`SfChainConfigKeys`中定义
- 使用分层结构组织配置键
- 避免硬编码字符串

### 2. 枚举使用
- 使用枚举代替字符串常量
- 为枚举提供`fromString`或`fromCode`方法
- 考虑提供国际化支持

### 3. 异常处理
- 使用标准化的异常码
- 提供清晰的异常消息
- 记录异常上下文信息

### 4. 版本控制
- 在添加新的常量时保持向后兼容
- 废弃的常量使用`@Deprecated`标记
- 提供迁移指南文档