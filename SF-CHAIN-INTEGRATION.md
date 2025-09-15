# SF-Chain 框架集成指南

## 概述

SF-Chain 是一个可嵌入的 AI 管理框架，类似于 Swagger，可以直接集成到其他 Spring Boot 项目中，提供 AI 模型配置、操作管理和调用日志等功能。

## 快速集成

### 1. 添加依赖

将 SF-Chain 相关的 jar 包添加到你的项目依赖中。

### 2. 配置路径前缀

在你的项目的 `application.yml` 中添加以下配置：

```yaml
# SF-Chain 路径配置
sf-chain:
  path:
    # API 接口路径前缀（后端接口）
    api-prefix: /your-project/sf-chain
    # 前端静态资源路径前缀
    web-prefix: /your-project/sf
```

### 3. 配置示例

#### 示例 1：项目前缀为 `/api/v1`

```yaml
sf-chain:
  path:
    api-prefix: /api/v1/sf-chain
    web-prefix: /api/v1/sf
```

访问地址：
- 前端界面：`http://localhost:8080/api/v1/sf/`
- API 接口：`http://localhost:8080/api/v1/sf-chain/models`

#### 示例 2：项目前缀为 `/myapp`

```yaml
sf-chain:
  path:
    api-prefix: /myapp/sf-chain
    web-prefix: /myapp/sf
```

访问地址：
- 前端界面：`http://localhost:8080/myapp/sf/`
- API 接口：`http://localhost:8080/myapp/sf-chain/models`

#### 示例 3：使用默认配置

如果不配置，将使用默认值：

```yaml
sf-chain:
  path:
    api-prefix: /sf-chain  # 默认值
    web-prefix: /sf        # 默认值
```

## 前端构建配置

如果你需要自定义构建前端资源，可以设置环境变量：

```bash
# 设置前端基础路径
export VITE_BASE_PATH=/your-project/sf/
# 设置 API 前缀
export VITE_API_PREFIX=/your-project/sf-chain

# 构建前端
npm run build
```

## 访问方式

集成完成后，启动你的 Spring Boot 应用，然后访问：

- **SF-Chain 管理界面**：`http://localhost:8080{web-prefix}/`
- **API 接口**：`http://localhost:8080{api-prefix}/models`

## 注意事项

1. **路径一致性**：确保 `api-prefix` 和 `web-prefix` 的配置与你的项目路径规范一致
2. **静态资源**：前端资源会自动映射到 `{web-prefix}/**` 路径下
3. **API 拦截**：SF-Chain 的 API 会自动应用到 `{api-prefix}/**` 路径下
4. **根路径处理**：如果你的项目根路径 `/` 有特殊处理，SF-Chain 不会影响它

## 完整配置示例

```yaml
server:
  port: 8080
  servlet:
    context-path: /myapp

sf-chain:
  path:
    api-prefix: /myapp/sf-chain
    web-prefix: /myapp/sf

# 其他项目配置...
spring:
  application:
    name: my-application
```

使用上述配置，SF-Chain 将在以下地址可用：
- 管理界面：`http://localhost:8080/myapp/sf/`
- API 接口：`http://localhost:8080/myapp/sf-chain/*`

## 故障排除

### 1. 页面无法访问
- 检查 `web-prefix` 配置是否正确
- 确认静态资源是否正确打包

### 2. API 调用失败
- 检查 `api-prefix` 配置是否正确
- 确认前端的 API 前缀配置是否与后端一致

### 3. 路径冲突
- 确保 SF-Chain 的路径前缀不与你的项目现有路径冲突
- 可以通过修改配置来避免冲突

## 技术支持

如有问题，请检查：
1. Spring Boot 版本兼容性
2. 配置文件语法正确性
3. 路径前缀的唯一性