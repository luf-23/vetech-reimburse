## vetech-reimburse

报销系统示例项目，包含前后端两个子工程：

| 目录 | 技术栈 |
|------|--------|
| `reimburse-backend` | Spring Boot 3.2.12、Spring Data JPA、MySQL |
| `reimburse-frontend` | Vite、Vue 3、Element Plus |

### 环境要求

| 组件 | 版本建议 |
|------|----------|
| JDK | 17+ |
| Maven | 3.8+（也可仅用项目自带的 `mvnw` / `mvnw.cmd`） |
| MySQL | 8.0+ |
| Node.js | 18+（前端开发） |

---

### 一、数据库准备

#### 1. 创建库并建表

脚本目录：`reimburse-backend/src/main/resources/SQL/`

`01_schema.sql` 已包含 `CREATE DATABASE` 与全部表结构，在 MySQL 客户端中**按顺序**执行：

```bash
# 示例：命令行（将 -u/-p 换成你的账号）
mysql -u root -p < reimburse-backend/src/main/resources/SQL/01_schema.sql
mysql -u root -p < reimburse-backend/src/main/resources/SQL/02_data.sql
```

Windows PowerShell（在项目根目录）：

```powershell
Get-Content reimburse-backend\src\main\resources\SQL\01_schema.sql | mysql -u root -p
Get-Content reimburse-backend\src\main\resources\SQL\02_data.sql | mysql -u root -p
```

也可使用 Navicat、DBeaver、MySQL Workbench 等工具打开上述两个文件依次执行。

| 脚本 | 作用 |
|------|------|
| `01_schema.sql` | 创建库 `vetech_reimburse`、建表 |
| `02_data.sql` | 初始化主数据（公司、部门、报销人等）及示例报销单 |

#### 2. 配置数据源

编辑 `reimburse-backend/src/main/resources/application.properties`，使账号密码与本地 MySQL 一致：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vetech_reimburse?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的密码
```

仓库默认密码为 `rootpassword`（用户名 `root`）。`spring.jpa.hibernate.ddl-auto=none`，表结构以 SQL 脚本为准，启动时不会自动改表。

---

### 二、启动后端

工作目录：`reimburse-backend`。默认监听 **8080**。

#### 方式 A：Maven Wrapper（推荐，无需本机安装 Maven）

**Windows（CMD / PowerShell）：**

```powershell
cd reimburse-backend
.\mvnw.cmd spring-boot:run
```

**macOS / Linux：**

```bash
cd reimburse-backend
./mvnw spring-boot:run
```

首次运行 Wrapper 会下载 Maven，需保持网络畅通。

#### 方式 B：本机 Maven

```bash
cd reimburse-backend
mvn spring-boot:run
```

#### 方式 C：IDE

用 IntelliJ IDEA / Eclipse 打开 `reimburse-backend`，运行主类：

`org.dep.reimbursebackend.ReimburseBackendApplication`

#### 验证是否启动成功

- 控制台出现 `Started ReimburseBackendApplication`，且无数据源连接错误。
- 浏览器或 curl 访问主数据接口应返回 JSON：

```bash
curl http://localhost:8080/api/master
```

#### 常见问题

| 现象 | 处理 |
|------|------|
| `Communications link failure` / 无法连接数据库 | 确认 MySQL 已启动；库名、端口、用户名密码与 `application.properties` 一致 |
| `Table 'vetech_reimburse.xxx' doesn't exist` | 未执行或未完成 `01_schema.sql`，请重新执行建表脚本 |
| 端口 8080 被占用 | 修改 `application.properties` 中的 `server.port`，并同步修改前端 `vite.config.ts` 里 proxy 的 `target` 端口 |

---

### 三、启动前端

需先保证后端已在 8080（或你配置的端口）正常运行。

```bash
cd reimburse-frontend
npm install
npm run dev
```

开发服务器默认地址一般为 `http://localhost:5173`（以终端输出为准）。`vite.config.ts` 已将 `/api` 代理到 `http://localhost:8080`，前端请求 `/api/...` 会转发到后端，无需单独配置 CORS。

生产构建：

```bash
cd reimburse-frontend
npm run build
npm run preview   # 本地预览构建结果
```

---

### 四、推荐启动顺序

1. 执行 `01_schema.sql`、`02_data.sql`
2. 修改 `application.properties` 数据源
3. 启动 **reimburse-backend**（8080）
4. 启动 **reimburse-frontend**（`npm run dev`）
5. 浏览器打开前端地址，进入报销列表 / 表单页

---

### API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/master` | 基础数据（公司、部门、报销人、业务类型、城市、项目） |
| GET | `/api/reimburse` | 报销单分页列表（支持 `reimburseNo`、`title`、`page`、`size` 等查询参数） |
| GET | `/api/reimburse/{id}` | 报销单详情 |
| POST | `/api/reimburse` | 新建报销单 |
| PUT | `/api/reimburse/{id}` | 更新报销单 |
| DELETE | `/api/reimburse/{id}` | 删除报销单 |
| POST | `/api/reimburse/{id}/copy` | 复制报销单 |

统一响应包装为 `{ "code": 0, "message": "ok", "data": ... }`，`code !== 0` 表示业务或校验错误。
