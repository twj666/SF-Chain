# Controller Package - 控制器包文档

## 概述
`controller`包提供了RESTful API接口，用于管理AI系统、模型配置、操作配置和调用日志。所有控制器都遵循Spring Boot的REST API设计规范。

## 控制器详解

### 1. AISystemController - 系统管理控制器
**文件**: `AISystemController.java`
**路径**: `/sf-chain/system`

#### 功能概述
- 提供AI系统的整体概览信息
- 支持系统配置的备份、刷新和重置
- 监控系统运行状态

#### API端点

##### 获取系统概览
```
GET /sf-chain/system/overview
```
**响应示例**:
```json
{
  "totalModels": 5,
  "enabledModels": 3,
  "totalOperations": 8,
  "configuredOperations": 6,
  "totalConfigs": 8,
  "systemStatus": "running",
  "lastUpdate": 1700000000000
}
```

##### 创建配置备份
```
POST /sf-chain/system/backup
```
**响应示例**:
```json
{
  "success": true,
  "message": "配置备份创建成功",
  "backupName": "backup_1700000000000"
}
```

##### 刷新系统配置
```
POST /sf-chain/system/refresh
```
**响应示例**:
```json
{
  "success": true,
  "message": "系统配置刷新成功",
  "timestamp": 1700000000000
}
```

##### 重置系统配置
```
POST /sf-chain/system/reset
```
**响应示例**:
```json
{
  "success": true,
  "message": "系统配置重置成功",
  "timestamp": 1700000000000
}
```

### 2. AIModelController - 模型管理控制器
**文件**: `AIModelController.java`
**路径**: `/sf-chain/models`

#### 功能概述
- 管理AI模型的配置信息
- 支持模型的CRUD操作
- 提供模型可用性检查

#### API端点

##### 获取所有模型
```
GET /sf-chain/models
```

##### 获取指定模型配置
```
GET /sf-chain/models/{modelName}
```

##### 添加新模型
```
POST /sf-chain/models
```
**请求体**:
```json
{
  "modelName": "gpt-4",
  "apiKey": "your-api-key",
  "baseUrl": "https://api.openai.com",
  "enabled": true,
  "description": "GPT-4模型"
}
```

##### 更新模型配置
```
PUT /sf-chain/models/{modelName}
```

##### 删除模型配置
```
DELETE /sf-chain/models/{modelName}
```

##### 检查模型可用性
```
GET /sf-chain/models/{modelName}/available
```

### 3. AIOperationController - 操作管理控制器
**文件**: `AIOperationController.java`
**路径**: `/sf-chain/operations`

#### 功能概述
- 管理AI操作的配置
- 支持操作与模型的绑定
- 提供操作状态的查询

#### API端点

##### 获取所有操作
```
GET /sf-chain/operations
```

##### 获取指定操作配置
```
GET /sf-chain/operations/{operationType}
```

##### 保存操作配置
```
POST /sf-chain/operations/{operationType}
```
**请求体**:
```json
{
  "modelName": "gpt-4",
  "enabled": true,
  "maxTokens": 2000,
  "temperature": 0.7,
  "requireJsonOutput": true,
  "timeoutSeconds": 30,
  "retryCount": 2
}
```

##### 更新操作配置
```
PUT /sf-chain/operations/{operationType}
```

##### 删除操作配置
```
DELETE /sf-chain/operations/{operationType}
```

### 4. AICallLogController - 调用日志控制器
**文件**: `AICallLogController.java`
**路径**: `/sf-chain/logs`

#### 功能概述
- 管理AI调用的日志记录
- 提供日志查询和统计功能
- 支持日志的清理和导出

#### API端点

##### 获取调用日志列表
```
GET /sf-chain/logs
```
**查询参数**:
- `page`: 页码（默认0）
- `size`: 每页大小（默认20）
- `operationType`: 操作类型过滤
- `startTime`: 开始时间
- `endTime`: 结束时间
- `success`: 是否成功

##### 获取日志详情
```
GET /sf-chain/logs/{logId}
```

##### 获取日志统计
```
GET /sf-chain/logs/stats
```

##### 清理过期日志
```
DELETE /sf-chain/logs/cleanup
```
**查询参数**:
- `daysToKeep`: 保留天数（默认30）

##### 导出日志
```
GET /sf-chain/logs/export
```
**查询参数**:
- `format`: 导出格式（csv/json）
- `startTime`: 开始时间
- `endTime`: 结束时间

### 5. SfChainConfigController - 配置管理控制器
**文件**: `SfChainConfigController.java`
**路径**: `/sf-chain/config`

#### 功能概述
- 提供统一的配置管理接口
- 支持配置的导入导出
- 管理配置版本

#### API端点

##### 导出完整配置
```
GET /sf-chain/config/export
```

##### 导入配置
```
POST /sf-chain/config/import
```

##### 获取配置版本历史
```
GET /sf-chain/config/versions
```

##### 回滚到指定版本
```
POST /sf-chain/config/rollback/{versionId}
```

## 统一响应格式

所有控制器都遵循统一的响应格式：

### 成功响应
```json
{
  "success": true,
  "data": { /* 响应数据 */ },
  "message": "操作成功",
  "timestamp": 1700000000000
}
```

### 错误响应
```json
{
  "success": false,
  "error": "错误描述",
  "code": "ERROR_CODE",
  "timestamp": 1700000000000
}
```

## 异常处理

### 全局异常处理
所有控制器都通过Spring的`@ControllerAdvice`进行统一异常处理：

- `IllegalArgumentException`: 400 Bad Request
- `IllegalStateException`: 409 Conflict
- `RuntimeException`: 500 Internal Server Error

### 自定义异常码
```java
public enum ErrorCode {
    INVALID_MODEL_CONFIG("MODEL_001"),
    MODEL_NOT_FOUND("MODEL_002"),
    OPERATION_NOT_FOUND("OP_001"),
    INVALID_OPERATION_CONFIG("OP_002"),
    DUPLICATE_CONFIG("CONFIG_001"),
    BACKUP_FAILED("SYSTEM_001")
}
```

## 分页查询

支持分页查询的控制器使用统一的分页参数：

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| page | int | 0 | 页码，从0开始 |
| size | int | 20 | 每页大小，最大100 |
| sort | string | id,desc | 排序字段和方向 |

### 分页响应格式
```json
{
  "success": true,
  "data": {
    "content": [ /* 数据列表 */ ],
    "totalElements": 100,
    "totalPages": 5,
    "page": 0,
    "size": 20,
    "first": true,
    "last": false
  }
}
```

## 安全配置

### 认证方式
- API Key认证（通过Header: `X-API-Key`）
- JWT Token认证（通过Header: `Authorization: Bearer {token}`）

### 权限控制
基于角色的访问控制（RBAC）：

| 角色 | 权限 |
|------|------|
| ADMIN | 所有操作 |
| OPERATOR | 查看配置、执行操作 |
| VIEWER | 仅查看配置 |

### 敏感信息处理
- API密钥在响应中会被掩码处理
- 密码字段不会出现在日志中
- 支持配置加密存储

## 使用示例

### 1. 添加新模型
```bash
curl -X POST http://localhost:8080/sf-chain/models \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "modelName": "gpt-4-turbo",
    "apiKey": "sk-xxx",
    "baseUrl": "https://api.openai.com",
    "enabled": true,
    "description": "GPT-4 Turbo模型"
  }'
```

### 2. 配置操作
```bash
curl -X POST http://localhost:8080/sf-chain/operations/codeReview \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "modelName": "gpt-4-turbo",
    "enabled": true,
    "maxTokens": 2000,
    "temperature": 0.7,
    "requireJsonOutput": true,
    "timeoutSeconds": 30
  }'
```

### 3. 查询调用日志
```bash
curl "http://localhost:8080/sf-chain/logs?operationType=codeReview&page=0&size=10" \
  -H "X-API-Key: your-api-key"
```

## 监控指标

### 系统指标
- API调用次数
- 平均响应时间
- 错误率
- 活跃会话数

### 业务指标
- 各操作使用频率
- 模型调用分布
- 成功率统计
- 平均token消耗

### 监控端点
```
GET /sf-chain/system/metrics
GET /sf-chain/system/health
```