# vetech-reimburse

差旅/日常报销单示例项目，前后端分离：列表查询、新建/编辑/复制/删除报销单，以及行程、补助、费用分摊等子表维护与提交前校验。

| 目录 | 说明 |
|------|------|
| `reimburse-backend` | Spring Boot REST API，MyBatis-Plus 持久化 |
| `reimburse-frontend` | Vue 3 单页应用（列表 + 表单） |
| `docs` | 接口与表结构 Word 文档（设计参考） |

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2.12、MyBatis-Plus 3.5、MySQL 8、JDK 17、Lombok |
| 前端 | Vite 7、Vue 3.5、Vue Router 5、Pinia、Element Plus、TypeScript |

## 功能概览

- **报销单列表**：分页、按单号/标题/事由及公司、部门、报销人、业务类型筛选
- **报销单表单**：主表信息、补录行程、补助信息（含日历明细）、费用归属分摊
- **业务操作**：新建、编辑、复制、删除；保存草稿与提交前校验（前端 + 后端）
- **主数据**：公司、部门、人员、业务类型、城市、项目等由前端 `masterData.ts` 写死（与 SQL 示例数据中的 ID 一致，不入库）

## 环境要求

| 组件 | 版本建议 |
|------|----------|
| JDK | 17+ |
| Maven | 3.8+（也可仅用 `reimburse-backend` 下的 `mvnw` / `mvnw.cmd`） |
| MySQL | 8.0+ |
| Node.js | `^20.19.0` 或 `>=22.12.0`（见 `reimburse-frontend/package.json` 的 `engines`） |

## 项目结构

```
vetech-reimburse/
├── reimburse-backend/
│   ├── src/main/java/org/dep/reimburse/
│   │   ├── controller/          # REST 接口
│   │   ├── service/             # 业务逻辑
│   │   ├── util/                # 表单校验、分摊金额等
│   │   └── ...
│   └── src/main/resources/
│       ├── application.yml      # 数据源、端口
│       └── SQL/                 # 建库建表与示例数据
├── reimburse-frontend/
│   └── src/
│       ├── views/               # 列表页、表单页
│       ├── api/                 # 调用 /api/reimburse/*
│       ├── data/masterData.ts   # 主数据（只读）
│       └── utils/               # 前端校验、数据规范化
└── docs/                        # API、表结构文档
```

---

## 一、数据库准备

### 1. 创建库并建表

脚本目录：`reimburse-backend/src/main/resources/SQL/`

在 MySQL 客户端中**按顺序**执行：

```bash
mysql -u root -p < reimburse-backend/src/main/resources/SQL/01_schema.sql
mysql -u root -p < reimburse-backend/src/main/resources/SQL/02_data.sql
```

Windows PowerShell（在项目根目录）：

```powershell
Get-Content reimburse-backend\src\main\resources\SQL\01_schema.sql | mysql -u root -p
Get-Content reimburse-backend\src\main\resources\SQL\02_data.sql | mysql -u root -p
```

也可使用 Navicat、DBeaver、MySQL Workbench 等工具依次执行上述文件。

| 脚本 | 作用 |
|------|------|
| `01_schema.sql` | 创建库 `vetech_reimburse` 及四张业务表 |
| `02_data.sql` | 插入示例报销单及行程、补助、分摊子表数据 |

### 2. 数据表说明

| 表名 | 说明 |
|------|------|
| `reimburse_doc` | 报销单主表（单号、状态、主数据 ID、标题、事由、补助合计等） |
| `reimburse_itinerary` | 补录行程 |
| `reimburse_subsidy` | 补助信息（可关联行程，`calendar_json` 存日历明细） |
| `reimburse_allocation` | 费用归属及分摊（比例、金额） |

主数据（公司、部门等）**不建表**，ID 须与前端 `reimburse-frontend/src/data/masterData.ts` 保持一致。

### 3. 配置数据源

编辑 `reimburse-backend/src/main/resources/application.yml`，使账号密码与本地 MySQL 一致：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/vetech_reimburse?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的密码
```

仓库默认用户名为 `root`、密码为 `rootpassword`。表结构以 SQL 脚本为准，应用启动时**不会**自动建表或迁移。

---

## 二、启动后端

工作目录：`reimburse-backend`。默认监听 **8080**。

### 方式 A：Maven Wrapper（推荐）

**Windows：**

```powershell
cd reimburse-backend
.\mvnw.cmd spring-boot:run
```

**macOS / Linux：**

```bash
cd reimburse-backend
./mvnw spring-boot:run
```

### 方式 B：本机 Maven

```bash
cd reimburse-backend
mvn spring-boot:run
```

### 方式 C：IDE

运行主类 `org.dep.reimburse.ReimburseApplication`。

### 验证是否启动成功

- 控制台出现 `Started ReimburseApplication`，且无数据源连接错误。
- 访问列表接口应返回 JSON（`code` 为 0）：

```bash
curl "http://localhost:8080/api/reimburse/list?page=1&size=10"
```

### 常见问题

| 现象 | 处理 |
|------|------|
| `Communications link failure` | 确认 MySQL 已启动；库名、端口、账号与 `application.yml` 一致 |
| `Table 'vetech_reimburse.xxx' doesn't exist` | 重新执行 `01_schema.sql` |
| 8080 被占用 | 修改 `application.yml` 的 `server.port`，并同步修改 `reimburse-frontend/vite.config.ts` 中 proxy 的 `target` |

### 后端测试

```bash
cd reimburse-backend
.\mvnw.cmd test    # Windows
./mvnw test        # macOS / Linux
```

---

## 三、启动前端

需先保证后端已在 8080（或你配置的端口）正常运行。

```bash
cd reimburse-frontend
npm install
npm run dev
```

开发服务器地址以终端输出为准（一般为 `http://localhost:5173`）。`vite.config.ts` 将 `/api` 代理到 `http://localhost:8080`。

### 前端路由

| 路径 | 页面 |
|------|------|
| `/` | 报销单列表 |
| `/reimburse/form` | 新建报销单 |
| `/reimburse/form/:id` | 编辑报销单 |

### 生产构建

```bash
cd reimburse-frontend
npm run build
npm run preview   # 本地预览构建结果
```

### 代码检查（可选）

```bash
cd reimburse-frontend
npm run lint
npm run format
```

---

## 四、推荐启动顺序

1. 执行 `01_schema.sql`、`02_data.sql`
2. 修改 `application.yml` 数据源
3. 启动 **reimburse-backend**（8080）
4. 启动 **reimburse-frontend**（`npm run dev`）
5. 浏览器打开前端地址，进入列表或表单页

---

## API 说明

基础路径：`http://localhost:8080/api/reimburse`（前端通过 `/api` 代理访问）。

统一响应：`{ "code": 0, "message": "success", "data": ... }`。`code !== 0` 表示业务或校验错误；未找到资源等场景 HTTP 状态码可能为 404/400，body 仍为上述结构。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 分页列表 |
| GET | `/detail/{id}` | 详情（含行程、补助、分摊） |
| POST | `/create` | 新建 |
| PUT | `/update/{id}` | 更新 |
| DELETE | `/delete/{id}` | 删除 |
| POST | `/copy/{id}` | 复制，返回新列表项 |
| POST | `/validate` | 提交前服务端校验 |

### 列表查询参数（GET `/list`）

| 参数 | 说明 |
|------|------|
| `reimburseNo` | 报销单号（模糊） |
| `title` | 标题（模糊） |
| `reason` | 事由（模糊） |
| `companyId` | 费用归属公司 ID |
| `departmentId` | 报销部门 ID |
| `reimburserId` | 报销人 ID |
| `businessTypeId` | 业务类型 ID（须为末级节点） |
| `page` | 页码，默认 1 |
| `size` | 每页条数，默认 10 |

### 校验接口（POST `/validate`）

请求体为报销单表单 JSON，并附带 `subsidyTotal`（补助合计）。响应 `data` 形如 `{ "valid": true/false, "message": "..." }`。

后端校验要点（与前端规则对齐）包括：必填主表字段、至少一条行程与补助且一一关联、分摊比例合计 100%、分摊金额合计等于补助总金额等。实现见 `ReimburseFormValidator.java`。

---

## 设计文档

更完整的字段与接口定义见仓库内：

- `docs/API接口定义文档.docx`
- `docs/表结构定义文档.docx`
