# SF-Chain Config Center Server

独立部署的 SF-Chain 配置中心启动工程。

## 1. 前置条件

- JDK 17
- PostgreSQL 或 MySQL

## 2. 必要环境变量

```bash
export SF_CHAIN_AUTH_TOKEN='your-token'
export SF_CHAIN_DB_TYPE='postgresql' # 可选: mysql / postgresql
export SF_CHAIN_DB_URL='jdbc:postgresql://127.0.0.1:5432/sf_chain'
export SF_CHAIN_DB_USERNAME='postgres'
export SF_CHAIN_DB_PASSWORD='postgres'
```

MySQL 示例：

```bash
export SF_CHAIN_DB_TYPE='mysql'
export SF_CHAIN_DB_URL='jdbc:mysql://127.0.0.1:3306/sf_chain?useSSL=false&characterEncoding=utf8&serverTimezone=UTC'
export SF_CHAIN_DB_DRIVER='com.mysql.cj.jdbc.Driver'
```

## 3. 构建前端静态资源

该服务 UI 静态资源来自 `sf-chain-legacy-starter`，首次部署前执行：

```bash
cd /Users/suifeng/Code/SF-Chain/sf-chain-legacy-starter
./build-frontend.sh
```

## 4. 启动

### 4.1 本地快速启动（内存数据库 H2）

```bash
cd /Users/suifeng/Code/SF-Chain
mvn -pl sf-chain-config-center-server -am spring-boot:run \
  -Dspring-boot.run.profiles=local
```

说明：
- `local` profile 使用 H2 内存数据库。
- 该模式可直接使用模型/Operation/系统管理接口，适合本地联调前端。

### 4.2 完整启动（推荐，含模型/Operation 持久化能力）

```bash
cd /Users/suifeng/Code/SF-Chain
mvn -pl sf-chain-config-center-server -am spring-boot:run
```

或打包后运行：

```bash
cd /Users/suifeng/Code/SF-Chain
mvn -pl sf-chain-config-center-server -am package -DskipTests
java -jar sf-chain-config-center-server/target/sf-chain-config-center-server-1.0.11.jar
```

## 5. 访问

- 前端 UI: `http://127.0.0.1:19090/sf`
- 管理 API 前缀: `http://127.0.0.1:19090/sf-chain`
- API 信息: `http://127.0.0.1:19090/sf-chain/config/api-info`

Header `Authorization` 值使用 `SF_CHAIN_AUTH_TOKEN`。

## 6. 租户与 API Key 管理接口（MVP）

- `GET /sf-chain/control/tenants`：查询租户
- `POST /sf-chain/control/tenants`：创建租户
- `PATCH /sf-chain/control/tenants/{tenantId}/status`：启停租户
- `GET /sf-chain/control/tenants/{tenantId}/api-keys`：查询租户下密钥
- `POST /sf-chain/control/tenants/{tenantId}/api-keys`：申请密钥（返回明文，仅一次）
- `PATCH /sf-chain/control/api-keys/{keyId}/revoke`：吊销密钥
- `POST /v1/auth/token/validate`：校验 API Key（供客户端调用）
