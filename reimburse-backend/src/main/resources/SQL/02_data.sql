-- 初始化基础数据与示例报销单（执行前请先执行 01_schema.sql）

USE vetech_reimburse;

-- 费用归属公司
INSERT INTO reim_company (reim_company_id, reim_company_no, reim_company_name) VALUES
('1C54557F1782E000', '0407', '胜意科技北京分公司'),
('19218A262C976000', '0408', '胜意科技上海分公司'),
('1C61686865DA8000', '0409', '胜意科技武汉分公司'),
('1717271D1DA15000', '0410', '胜意科技杭州分公司'),
('16AE93CC7EF92002', '0411', '胜意科技荆州分公司');

-- 报销部门
INSERT INTO reim_department (reim_department_id, reim_department_no, reim_department_name) VALUES
('13AB8D7B52A9B002', '072001', '客户成功事业部'),
('13BFD31C6029A002', '072002', '企业消费事业部'),
('14515BB4BFB92003', '072003', '企业费控事业部'),
('19206611C47A6000', '072004', '集采事业部'),
('19D32F9FE9647000', '072005', '航旅事业部'),
('13C7E2BAE0393001', '072006', '运营事业部'),
('14055D22BB808001', '072007', '营销事业部');

-- 报销人
INSERT INTO reimburser (reimburser_id, reimburser_no, reimburser_name) VALUES
('13AB3A3F72409002', '74541', '徐年年'),
('13AB498CC6409002', '74008', '郑雨雪'),
('13AB4A56BB009002', '21552', '邹薇'),
('13AB591FE8009002', '80681', '王成军'),
('13AB77281A408001', '89899', '潘展飞'),
('13AB7925EB808001', '10503', '姜林');

-- 业务类型
INSERT INTO business_type (business_type_id, business_type_no, business_type_name, there_subordinate_node, superior_id) VALUES
('18F0916A8C2C4000', '1001001', '员工差旅活动', '1', 'none'),
('18F091913EEC4000', '100100101', '境内出差', '1', '18F0916A8C2C4000'),
('1B5FEB7DD4396000', '10010010101', '项目出差', '0', '18F091913EEC4000'),
('1A92E43082EFC000', '10010010102', '市场拓展出差', '0', '18F091913EEC4000'),
('13AB3A4138008001', '100100102', '境外出差', '1', '18F0916A8C2C4000'),
('13AB3A4248008002', '10010010201', '国外考察', '0', '13AB3A4138008001'),
('13AB3A4154008001', '10010010202', '售后维护出差', '0', '13AB3A4138008001'),
('13AB3A4172008001', '1001002', '人力资源', '1', 'none'),
('13AB3A418F808001', '100100201', '个人团队培训', '0', '13AB3A4172008001'),
('13AB3A41AC408001', '100100202', '招聘会', '0', '13AB3A4172008001'),
('13AB3A41CD808002', '1001003', '员工福利', '1', 'none'),
('13AB3A41ED408002', '100100301', '员工旅游', '0', '13AB3A41CD808002'),
('13AB3A420CC08002', '100100302', '员工团建', '0', '13AB3A41CD808002'),
('13AB3A422A808001', '100100303', '员工体检', '0', '13AB3A41CD808002');

-- 城市
INSERT INTO city (city_no, city_name, city_type) VALUES
('10119', '北京', '1'),
('10621', '上海', '1'),
('10458', '武汉', '2'),
('10216', '杭州', '2'),
('10455', '荆州', '3');

-- 项目
INSERT INTO project (project_id, project_no, project_name) VALUES
('12BC248B25083001', 'nonProjectRelated', '非项目类费用归集'),
('1C811ABF96195000', 'centralChina', '华中客户定制化项目'),
('1C5931735AC4A000', 'southChina', '华南客户定制化项目'),
('1771EC45F2443000', 'northChina', '华北客户定制化项目'),
('1762792DB4E9A002', 'eastChina', '华东客户定制化项目'),
('17071065FC29A002', 'southWest', '西南客户定制化项目'),
('162664EBE9ABE001', 'northWest', '西北客户定制化项目'),
('162664B8526BE002', 'northEast', '东北客户定制化项目');

-- 示例报销单（id=1 含行程与补助明细，供详情页演示）
INSERT INTO reimburse_doc (id, reimburse_no, doc_type, status, reimburser_id, department_id, company_id, business_type_id, title, reason, subsidy_amount, submit_date, create_time, remark) VALUES
(1, 'RCBX20260515002', '差旅费用报销单', 3, '13AB3A3F72409002', '14515BB4BFB92003', '1C61686865DA8000', '1B5FEB7DD4396000', '日常报销单模板 - 副本', '项目现场支持', 540, '2026-05-15', '2026-05-15', ''),
(2, 'RCBX202605140001', '日常报销单', 1, '13AB498CC6409002', '13AB8D7B52A9B002', '1C54557F1782E000', '1B5FEB7DD4396000', '徐年年项目出差', '客户拜访', 111, '2026-05-14', '2026-05-14', ''),
(3, 'RCBX202605130002', '日常报销单', 0, '13AB4A56BB009002', '13BFD31C6029A002', '19218A262C976000', '1A92E43082EFC000', '日常报销单模板 - 副本', '项目实施', 74, '2026-05-13', '2026-05-13', ''),
(4, 'RCBX202605120003', '差旅费用报销单', 2, '13AB591FE8009002', '14515BB4BFB92003', '1C61686865DA8000', '1B5FEB7DD4396000', '郑雨雪北京出差报销', '测试', 0, '2026-05-12', '2026-05-12', ''),
(5, 'RCBX202605110004', '日常报销单', 3, '13AB77281A408001', '19206611C47A6000', '1717271D1DA15000', '1A92E43082EFC000', '邹薇上海项目出差', '项目现场支持', 185, '2026-05-11', '2026-05-11', ''),
(6, 'RCBX202604150005', '日常报销单', 1, '13AB7925EB808001', '19D32F9FE9647000', '16AE93CC7EF92002', '1B5FEB7DD4396000', '市场拓展差旅报销', '客户拜访', 222, '2026-04-15', '2026-04-15', '');

INSERT INTO reimburse_itinerary (doc_id, traveler_id, depart_city_no, arrive_city_no, start_date, end_date, description) VALUES
(1, '13AB3A3F72409002', '10458', '10119', '2026-04-13', '2026-04-17', '行程说明'),
(2, '13AB498CC6409002', '10119', '10621', '2026-05-13', '2026-05-14', '客户拜访'),
(5, '13AB77281A408001', '10458', '10621', '2026-05-10', '2026-05-10', '项目现场支持');

-- 费用归属及分摊（doc_id=1 多行均摊，合计 540 与补助总金额一致）
INSERT INTO reimburse_allocation (doc_id, cost_attribution_id, project_id, ratio, amount, sort_order) VALUES
(1, '1C61686865DA8000', '1771EC45F2443000', 0.4000, 216.00, 0),
(1, '1C54557F1782E000', '1762792DB4E9A002', 0.3500, 189.00, 1),
(1, '19218A262C976000', '1C5931735AC4A000', 0.2500, 135.00, 2),
(2, '1C54557F1782E000', '1771EC45F2443000', 0.6000, 66.60, 0),
(2, '19218A262C976000', '1762792DB4E9A002', 0.4000, 44.40, 1),
(5, '1717271D1DA15000', '17071065FC29A002', 0.5000, 92.50, 0),
(5, '16AE93CC7EF92002', '162664EBE9ABE001', 0.5000, 92.50, 1);

-- 补助信息（须关联补录行程；删除行程时数据库级联删除对应补助）
INSERT INTO reimburse_subsidy (doc_id, itinerary_id, traveler_id, start_date, end_date, days, route, subsidy_city_no, apply_amount, subsidy_amount, meal_total, transport_total, comm_total, calendar_json) VALUES
(1, 1, '13AB3A3F72409002', '2026-04-13', '2026-04-17', 5, '武汉-北京', '10119', 540.00, 540.00, 300.00, 120.00, 120.00,
 '[{"date":"2026-04-13","weekday":"星期一","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":40},"comm":{"checked":true,"standard":40,"amount":40}},{"date":"2026-04-14","weekday":"星期二","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":40},"comm":{"checked":true,"standard":40,"amount":40}},{"date":"2026-04-15","weekday":"星期三","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":40},"comm":{"checked":true,"standard":40,"amount":40}},{"date":"2026-04-16","weekday":"星期四","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":false,"standard":100,"amount":100},"transport":{"checked":false,"standard":40,"amount":40},"comm":{"checked":false,"standard":40,"amount":40}},{"date":"2026-04-17","weekday":"星期五","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":false,"standard":100,"amount":100},"transport":{"checked":false,"standard":40,"amount":40},"comm":{"checked":false,"standard":40,"amount":40}}]'),
(5, 3, '13AB77281A408001', '2026-05-10', '2026-05-10', 1, '武汉-上海', '10621', 180.00, 185.00, 100.00, 40.00, 45.00,
 '[{"date":"2026-05-10","weekday":"星期日","cityNo":"10621","cityName":"上海","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":40},"comm":{"checked":true,"standard":40,"amount":45}}]'),
(2, 2, '13AB498CC6409002', '2026-05-13', '2026-05-14', 2, '北京-上海', '10621', 140.00, 111.00, 100.00, 11.00, 0.00,
 '[{"date":"2026-05-13","weekday":"星期三","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":11},"comm":{"checked":false,"standard":40,"amount":40}},{"date":"2026-05-14","weekday":"星期四","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":false,"standard":100,"amount":100},"transport":{"checked":false,"standard":40,"amount":40},"comm":{"checked":false,"standard":40,"amount":40}}]');
