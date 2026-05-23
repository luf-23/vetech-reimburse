-- vetech_reimburse 数据库建表脚本（主数据由前端 5.3 写死，不入库）
CREATE DATABASE vetech_reimburse DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE vetech_reimburse;

-- 报销单主表
CREATE TABLE IF NOT EXISTS reimburse_doc (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    reimburse_no     VARCHAR(32)  NOT NULL COMMENT '报销单号',
    doc_type         VARCHAR(64)  NOT NULL DEFAULT '日常报销单' COMMENT '单据类型',
    status           TINYINT      NOT NULL DEFAULT 0 COMMENT '单据状态：0草稿 1审批通过 2已作废 3审批中',
    reimburser_id    VARCHAR(32)  NOT NULL COMMENT '报销人ID',
    department_id    VARCHAR(32)  NOT NULL COMMENT '报销部门ID',
    company_id       VARCHAR(32)  NOT NULL COMMENT '费用归属公司ID',
    business_type_id VARCHAR(32)  NOT NULL COMMENT '业务类型ID',
    title            VARCHAR(500) NOT NULL DEFAULT '' COMMENT '报销标题',
    reason           VARCHAR(500) NOT NULL DEFAULT '' COMMENT '报销事由',
    subsidy_amount   DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '补助合计金额',
    submit_date      DATE         NULL COMMENT '提交日期',
    create_time      DATE         NOT NULL COMMENT '创建日期',
    remark           VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '备注',
    UNIQUE KEY uk_reimburse_no (reimburse_no),
    KEY idx_reimburser (reimburser_id),
    KEY idx_department (department_id),
    KEY idx_company (company_id),
    KEY idx_business_type (business_type_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报销单主表';

-- 行程
CREATE TABLE IF NOT EXISTS reimburse_itinerary (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    doc_id          BIGINT       NOT NULL COMMENT '报销单ID',
    traveler_id     VARCHAR(32)  NOT NULL COMMENT '出行人ID',
    depart_city_no  VARCHAR(16)  NOT NULL COMMENT '出发城市编号',
    arrive_city_no  VARCHAR(16)  NOT NULL COMMENT '到达城市编号',
    start_date      DATE         NOT NULL COMMENT '行程开始日期',
    end_date        DATE         NOT NULL COMMENT '行程结束日期',
    description     VARCHAR(500) NOT NULL DEFAULT '' COMMENT '行程说明',
    KEY idx_doc_id (doc_id),
    CONSTRAINT fk_itinerary_doc FOREIGN KEY (doc_id) REFERENCES reimburse_doc (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报销行程明细';

-- 补助信息
CREATE TABLE IF NOT EXISTS reimburse_subsidy (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    doc_id           BIGINT        NOT NULL COMMENT '报销单ID',
    itinerary_id     BIGINT        NULL COMMENT '关联行程ID，为空表示未关联具体行程',
    traveler_id      VARCHAR(32)   NOT NULL COMMENT '出行人ID',
    start_date       DATE          NOT NULL COMMENT '补助开始日期',
    end_date         DATE          NOT NULL COMMENT '补助结束日期',
    days             INT           NOT NULL DEFAULT 0 COMMENT '补助天数',
    route            VARCHAR(200)  NOT NULL DEFAULT '' COMMENT '行程路线描述',
    subsidy_city_no  VARCHAR(16)   NOT NULL COMMENT '补助城市编号',
    apply_amount     DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '申请补助金额',
    subsidy_amount   DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '核定补助金额',
    meal_total       DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '餐补合计',
    transport_total  DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '交通补助合计',
    comm_total       DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '通讯补助合计',
    calendar_json    JSON          NULL COMMENT '日历明细JSON',
    KEY idx_subsidy_doc (doc_id),
    KEY idx_subsidy_itinerary (itinerary_id),
    CONSTRAINT fk_subsidy_doc FOREIGN KEY (doc_id) REFERENCES reimburse_doc (id) ON DELETE CASCADE,
    CONSTRAINT fk_subsidy_itinerary FOREIGN KEY (itinerary_id) REFERENCES reimburse_itinerary (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报销补助明细';

-- 费用分摊
CREATE TABLE IF NOT EXISTS reimburse_allocation (
    id                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    doc_id               BIGINT        NOT NULL COMMENT '报销单ID',
    cost_attribution_id  VARCHAR(32)   NOT NULL COMMENT '费用归属公司ID',
    project_id           VARCHAR(32)   NOT NULL DEFAULT '' COMMENT '项目ID',
    ratio                DECIMAL(8, 4) NOT NULL DEFAULT 0 COMMENT '分摊比例',
    amount               DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '分摊金额',
    sort_order           INT           NOT NULL DEFAULT 0 COMMENT '排序序号',
    KEY idx_allocation_doc (doc_id),
    CONSTRAINT fk_allocation_doc FOREIGN KEY (doc_id) REFERENCES reimburse_doc (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报销费用分摊明细';
