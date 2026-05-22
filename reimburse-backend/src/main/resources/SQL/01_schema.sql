-- vetech_reimburse 数据库建表脚本
CREATE DATABASE vetech_reimburse DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE vetech_reimburse;

-- 费用归属公司
CREATE TABLE IF NOT EXISTS reim_company (
    reim_company_id   VARCHAR(32)  NOT NULL PRIMARY KEY,
    reim_company_no   VARCHAR(16)  NOT NULL,
    reim_company_name VARCHAR(128) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 报销部门
CREATE TABLE IF NOT EXISTS reim_department (
    reim_department_id   VARCHAR(32)  NOT NULL PRIMARY KEY,
    reim_department_no   VARCHAR(16)  NOT NULL,
    reim_department_name VARCHAR(128) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 报销人
CREATE TABLE IF NOT EXISTS reimburser (
    reimburser_id   VARCHAR(32) NOT NULL PRIMARY KEY,
    reimburser_no   VARCHAR(16) NOT NULL,
    reimburser_name VARCHAR(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 业务类型（树形）
CREATE TABLE IF NOT EXISTS business_type (
    business_type_id        VARCHAR(32)  NOT NULL PRIMARY KEY,
    business_type_no        VARCHAR(32)  NOT NULL,
    business_type_name      VARCHAR(128) NOT NULL,
    there_subordinate_node  CHAR(1)      NOT NULL COMMENT '1有下级 0叶子',
    superior_id             VARCHAR(32)  NOT NULL COMMENT 'none 表示根节点'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 城市
CREATE TABLE IF NOT EXISTS city (
    city_no   VARCHAR(16) NOT NULL PRIMARY KEY,
    city_name VARCHAR(64) NOT NULL,
    city_type CHAR(1)     NOT NULL COMMENT '1一线 2二线 3三线'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 项目
CREATE TABLE IF NOT EXISTS project (
    project_id   VARCHAR(32)  NOT NULL PRIMARY KEY,
    project_no   VARCHAR(64)  NOT NULL,
    project_name VARCHAR(256) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 报销单主表
CREATE TABLE IF NOT EXISTS reimburse_doc (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reimburse_no     VARCHAR(32)  NOT NULL,
    doc_type         VARCHAR(64)  NOT NULL DEFAULT '日常报销单',
    status           TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1审批通过 2已作废 3审批中',
    reimburser_id    VARCHAR(32)  NOT NULL,
    department_id    VARCHAR(32)  NOT NULL,
    company_id       VARCHAR(32)  NOT NULL,
    business_type_id VARCHAR(32)  NOT NULL,
    title            VARCHAR(500) NOT NULL DEFAULT '',
    reason           VARCHAR(500) NOT NULL DEFAULT '',
    subsidy_amount   DECIMAL(12, 2) NOT NULL DEFAULT 0,
    submit_date      DATE         NULL,
    create_time      DATE         NOT NULL,
    remark           VARCHAR(1000) NOT NULL DEFAULT '',
    UNIQUE KEY uk_reimburse_no (reimburse_no),
    KEY idx_reimburser (reimburser_id),
    KEY idx_department (department_id),
    KEY idx_company (company_id),
    KEY idx_business_type (business_type_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 行程
CREATE TABLE IF NOT EXISTS reimburse_itinerary (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    doc_id          BIGINT       NOT NULL,
    traveler_id     VARCHAR(32)  NOT NULL,
    depart_city_no  VARCHAR(16)  NOT NULL,
    arrive_city_no  VARCHAR(16)  NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    description     VARCHAR(500) NOT NULL DEFAULT '',
    KEY idx_doc_id (doc_id),
    CONSTRAINT fk_itinerary_doc FOREIGN KEY (doc_id) REFERENCES reimburse_doc (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 补助信息
CREATE TABLE IF NOT EXISTS reimburse_subsidy (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    doc_id           BIGINT        NOT NULL,
    itinerary_id     BIGINT        NULL,
    traveler_id      VARCHAR(32)   NOT NULL,
    start_date       DATE          NOT NULL,
    end_date         DATE          NOT NULL,
    days             INT           NOT NULL DEFAULT 0,
    route            VARCHAR(200)  NOT NULL DEFAULT '',
    subsidy_city_no  VARCHAR(16)   NOT NULL,
    apply_amount     DECIMAL(12, 2) NOT NULL DEFAULT 0,
    subsidy_amount   DECIMAL(12, 2) NOT NULL DEFAULT 0,
    meal_total       DECIMAL(12, 2) NOT NULL DEFAULT 0,
    transport_total  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    comm_total       DECIMAL(12, 2) NOT NULL DEFAULT 0,
    calendar_json    JSON          NULL,
    KEY idx_subsidy_doc (doc_id),
    KEY idx_subsidy_itinerary (itinerary_id),
    CONSTRAINT fk_subsidy_doc FOREIGN KEY (doc_id) REFERENCES reimburse_doc (id) ON DELETE CASCADE,
    CONSTRAINT fk_subsidy_itinerary FOREIGN KEY (itinerary_id) REFERENCES reimburse_itinerary (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 费用分摊
CREATE TABLE IF NOT EXISTS reimburse_allocation (
    id                   BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    doc_id               BIGINT        NOT NULL,
    cost_attribution_id  VARCHAR(32)   NOT NULL,
    project_id           VARCHAR(32)   NOT NULL DEFAULT '',
    ratio                DECIMAL(8, 4) NOT NULL DEFAULT 0,
    amount               DECIMAL(12, 2) NOT NULL DEFAULT 0,
    sort_order           INT           NOT NULL DEFAULT 0,
    KEY idx_allocation_doc (doc_id),
    CONSTRAINT fk_allocation_doc FOREIGN KEY (doc_id) REFERENCES reimburse_doc (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
