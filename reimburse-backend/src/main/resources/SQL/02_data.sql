-- 示例报销单（执行前请先执行 01_schema.sql；主数据 ID 与前端 masterData.ts 一致）
-- 状态：草稿 3 条，审批通过/已作废/审批中各 2 条；每单行程/补助/费用公司 0～4 条
-- 业务类型须为末级（masterData thereSubordinateNode='0'），如 1B5FEB7DD4396000 项目出差、1A92E43082EFC000 市场拓展出差

USE vetech_reimburse;

INSERT INTO reimburse_doc (id, reimburse_no, doc_type, status, reimburser_id, department_id, company_id, business_type_id, title, reason, subsidy_amount, submit_date, create_time, remark) VALUES
(1, 'RCBX20260515002', '差旅费用报销单', 3, '13AB3A3F72409002', '14515BB4BFB92003', '1C61686865DA8000', '1B5FEB7DD4396000', '日常报销单模板 - 副本', '项目现场支持', 1120.00, '2026-05-15', '2026-05-15', ''),
(2, 'RCBX202605140001', '日常报销单', 1, '13AB498CC6409002', '13AB8D7B52A9B002', '1C54557F1782E000', '1B5FEB7DD4396000', '徐年年项目出差', '客户拜访', 241.00, '2026-05-14', '2026-05-14', ''),
(3, 'RCBX202605130002', '日常报销单', 0, '13AB4A56BB009002', '13BFD31C6029A002', '19218A262C976000', '1A92E43082EFC000', '日常报销单模板 - 副本', '项目实施', 0.00, NULL, '2026-05-13', '草稿-无子表明细'),
(4, 'RCBX202605120003', '差旅费用报销单', 2, '13AB591FE8009002', '14515BB4BFB92003', '1C61686865DA8000', '1B5FEB7DD4396000', '郑雨雪北京出差报销', '测试作废', 100.00, '2026-05-12', '2026-05-12', ''),
(5, 'RCBX202605110004', '日常报销单', 3, '13AB77281A408001', '19206611C47A6000', '1717271D1DA15000', '1A92E43082EFC000', '邹薇上海项目出差', '项目现场支持', 365.00, '2026-05-11', '2026-05-11', ''),
(6, 'RCBX202604150005', '日常报销单', 1, '13AB7925EB808001', '19D32F9FE9647000', '16AE93CC7EF92002', '1B5FEB7DD4396000', '市场拓展差旅报销', '客户拜访', 430.00, '2026-04-15', '2026-04-15', ''),
(7, 'RCBX202605200006', '日常报销单', 0, '13AB4A56BB009002', '13BFD31C6029A002', '19218A262C976000', '1A92E43082EFC000', '邹薇杭州出差草稿', '市场调研', 280.00, NULL, '2026-05-20', ''),
(8, 'RCBX202605190007', '差旅费用报销单', 0, '13AB4A56BB009002', '14055D22BB808001', '1C54557F1782E000', '1A92E43082EFC000', '北京考察差旅草稿', '客户拜访', 180.00, NULL, '2026-05-19', ''),
(9, 'RCBX202605100008', '日常报销单', 2, '13AB591FE8009002', '13C7E2BAE0393001', '16AE93CC7EF92002', '1B5FEB7DD4396000', '王成军作废报销单', '已取消', 0.00, '2026-05-10', '2026-05-10', '作废-无子表明细');

-- 补录行程（按插入顺序自增 id 1～17，供补助 itinerary_id 引用）
INSERT INTO reimburse_itinerary (doc_id, traveler_id, depart_city_no, arrive_city_no, start_date, end_date, description) VALUES
(1, '13AB3A3F72409002', '10458', '10216', '2026-04-12', '2026-04-12', '出发前杭州衔接'),
(1, '13AB3A3F72409002', '10458', '10119', '2026-04-13', '2026-04-15', '武汉赴北京项目支持'),
(1, '13AB3A3F72409002', '10119', '10621', '2026-04-16', '2026-04-17', '北京转上海'),
(1, '13AB591FE8009002', '10621', '10458', '2026-04-17', '2026-04-17', '上海返程'),
(1, '13AB3A3F72409002', '10216', '10119', '2026-04-18', '2026-04-19', '杭州赴北京复盘'),
(2, '13AB498CC6409002', '10119', '10621', '2026-05-13', '2026-05-14', '客户拜访'),
(2, '13AB498CC6409002', '10621', '10216', '2026-05-14', '2026-05-14', '上海赴杭州'),
(4, '13AB591FE8009002', '10458', '10119', '2026-05-11', '2026-05-11', '作废单行程'),
(5, '13AB77281A408001', '10458', '10621', '2026-05-10', '2026-05-10', '项目现场支持'),
(5, '13AB77281A408001', '10621', '10216', '2026-05-11', '2026-05-11', '上海赴杭州'),
(6, '13AB7925EB808001', '10458', '10119', '2026-04-10', '2026-04-12', '市场拓展第一段'),
(6, '13AB7925EB808001', '10119', '10621', '2026-04-13', '2026-04-14', '北京赴上海'),
(6, '13AB7925EB808001', '10621', '10216', '2026-04-15', '2026-04-15', '上海赴杭州'),
(6, '13AB7925EB808001', '10216', '10455', '2026-04-16', '2026-04-16', '杭州赴荆州'),
(7, '13AB4A56BB009002', '10458', '10216', '2026-05-20', '2026-05-21', '草稿-杭州出差'),
(7, '13AB4A56BB009002', '10216', '10458', '2026-05-22', '2026-05-22', '返程武汉'),
(8, '13AB4A56BB009002', '10458', '10119', '2026-05-18', '2026-05-19', '草稿-北京考察');

-- 费用公司分摊（doc 3、9 无；其余 1～4 条）
INSERT INTO reimburse_allocation (doc_id, cost_attribution_id, project_id, ratio, amount, sort_order) VALUES
(1, '1C61686865DA8000', '1771EC45F2443000', 0.2500, 170.00, 0),
(1, '1C54557F1782E000', '1762792DB4E9A002', 0.2500, 170.00, 1),
(1, '19218A262C976000', '1C5931735AC4A000', 0.2500, 170.00, 2),
(1, '1717271D1DA15000', '17071065FC29A002', 0.2500, 170.00, 3),
(2, '1C54557F1782E000', '1771EC45F2443000', 0.6000, 66.60, 0),
(2, '19218A262C976000', '1762792DB4E9A002', 0.4000, 44.40, 1),
(4, '1C61686865DA8000', '12BC248B25083001', 1.0000, 0.00, 0),
(5, '1717271D1DA15000', '17071065FC29A002', 0.5000, 182.50, 0),
(5, '16AE93CC7EF92002', '162664EBE9ABE001', 0.5000, 182.50, 1),
(6, '16AE93CC7EF92002', '162664EBE9ABE001', 0.4000, 168.00, 0),
(6, '1C54557F1782E000', '1771EC45F2443000', 0.3500, 147.00, 1),
(6, '19218A262C976000', '1762792DB4E9A002', 0.2500, 105.00, 2),
(7, '19218A262C976000', '1C5931735AC4A000', 0.6000, 120.00, 0),
(7, '1717271D1DA15000', '17071065FC29A002', 0.4000, 80.00, 1),
(8, '1C54557F1782E000', '1771EC45F2443000', 1.0000, 180.00, 0);

-- 补助信息：每条补录行程对应一条补助（route=出发城市名-到达城市名，与 masterData 城市一致；subsidy_city_no=到达城市）
INSERT INTO reimburse_subsidy (doc_id, itinerary_id, traveler_id, start_date, end_date, days, route, subsidy_city_no, apply_amount, subsidy_amount, meal_total, transport_total, comm_total, calendar_json) VALUES
(1, 1, '13AB3A3F72409002', '2026-04-12', '2026-04-12', 1, '武汉-杭州', '10216', 80.00, 80.00, 80.00, 0.00, 0.00,
 '[{"date":"2026-04-12","weekday":"星期六","cityNo":"10216","cityName":"杭州","cityType":"2","meal":{"checked":true,"standard":80,"amount":80},"transport":{"checked":false,"standard":50,"amount":0},"comm":{"checked":false,"standard":50,"amount":0}}]'),
(1, 2, '13AB3A3F72409002', '2026-04-13', '2026-04-15', 3, '武汉-北京', '10119', 300.00, 300.00, 200.00, 60.00, 40.00,
 '[{"date":"2026-04-13","weekday":"星期一","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":20},"comm":{"checked":true,"standard":40,"amount":20}},{"date":"2026-04-14","weekday":"星期二","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":20},"comm":{"checked":true,"standard":40,"amount":20}},{"date":"2026-04-15","weekday":"星期三","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":false,"standard":100,"amount":0},"transport":{"checked":true,"standard":40,"amount":20},"comm":{"checked":false,"standard":40,"amount":0}}]'),
(1, 3, '13AB3A3F72409002', '2026-04-16', '2026-04-17', 2, '北京-上海', '10621', 380.00, 380.00, 200.00, 80.00, 100.00,
 '[{"date":"2026-04-16","weekday":"星期四","cityNo":"10621","cityName":"上海","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":40},"comm":{"checked":true,"standard":40,"amount":50}},{"date":"2026-04-17","weekday":"星期五","cityNo":"10621","cityName":"上海","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":40},"comm":{"checked":true,"standard":40,"amount":50}}]'),
(1, 4, '13AB591FE8009002', '2026-04-17', '2026-04-17', 1, '上海-武汉', '10458', 80.00, 80.00, 80.00, 0.00, 0.00,
 '[{"date":"2026-04-17","weekday":"星期五","cityNo":"10458","cityName":"武汉","cityType":"2","meal":{"checked":true,"standard":80,"amount":80},"transport":{"checked":false,"standard":50,"amount":0},"comm":{"checked":false,"standard":50,"amount":0}}]'),
(1, 5, '13AB3A3F72409002', '2026-04-18', '2026-04-19', 2, '杭州-北京', '10119', 280.00, 280.00, 200.00, 40.00, 40.00,
 '[{"date":"2026-04-18","weekday":"星期六","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":20},"comm":{"checked":true,"standard":40,"amount":20}},{"date":"2026-04-19","weekday":"星期日","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":20},"comm":{"checked":true,"standard":40,"amount":20}}]'),
(2, 6, '13AB498CC6409002', '2026-05-13', '2026-05-14', 2, '北京-上海', '10621', 140.00, 111.00, 100.00, 11.00, 0.00,
 '[{"date":"2026-05-13","weekday":"星期三","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":11},"comm":{"checked":false,"standard":40,"amount":0}},{"date":"2026-05-14","weekday":"星期四","cityNo":"10621","cityName":"上海","cityType":"1","meal":{"checked":false,"standard":100,"amount":0},"transport":{"checked":false,"standard":40,"amount":0},"comm":{"checked":false,"standard":40,"amount":0}}]'),
(2, 7, '13AB498CC6409002', '2026-05-14', '2026-05-14', 1, '上海-杭州', '10216', 130.00, 130.00, 80.00, 50.00, 0.00,
 '[{"date":"2026-05-14","weekday":"星期四","cityNo":"10216","cityName":"杭州","cityType":"2","meal":{"checked":true,"standard":80,"amount":80},"transport":{"checked":true,"standard":50,"amount":50},"comm":{"checked":false,"standard":50,"amount":0}}]'),
(4, 8, '13AB591FE8009002', '2026-05-11', '2026-05-11', 1, '武汉-北京', '10119', 100.00, 100.00, 100.00, 0.00, 0.00,
 '[{"date":"2026-05-11","weekday":"星期一","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":false,"standard":40,"amount":0},"comm":{"checked":false,"standard":40,"amount":0}}]'),
(5, 9, '13AB77281A408001', '2026-05-10', '2026-05-10', 1, '武汉-上海', '10621', 180.00, 185.00, 100.00, 40.00, 45.00,
 '[{"date":"2026-05-10","weekday":"星期日","cityNo":"10621","cityName":"上海","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":40},"comm":{"checked":true,"standard":40,"amount":45}}]'),
(5, 10, '13AB77281A408001', '2026-05-11', '2026-05-11', 1, '上海-杭州', '10216', 175.00, 180.00, 80.00, 50.00, 50.00,
 '[{"date":"2026-05-11","weekday":"星期一","cityNo":"10216","cityName":"杭州","cityType":"2","meal":{"checked":true,"standard":80,"amount":80},"transport":{"checked":true,"standard":50,"amount":50},"comm":{"checked":true,"standard":50,"amount":50}}]'),
(6, 11, '13AB7925EB808001', '2026-04-10', '2026-04-12', 3, '武汉-北京', '10119', 150.00, 150.00, 90.00, 30.00, 30.00,
 '[{"date":"2026-04-10","weekday":"星期四","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":30},"transport":{"checked":true,"standard":40,"amount":10},"comm":{"checked":true,"standard":40,"amount":10}},{"date":"2026-04-11","weekday":"星期五","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":30},"transport":{"checked":true,"standard":40,"amount":10},"comm":{"checked":true,"standard":40,"amount":10}},{"date":"2026-04-12","weekday":"星期六","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":30},"transport":{"checked":true,"standard":40,"amount":10},"comm":{"checked":true,"standard":40,"amount":10}}]'),
(6, 12, '13AB7925EB808001', '2026-04-13', '2026-04-14', 2, '北京-上海', '10621', 140.00, 140.00, 80.00, 30.00, 30.00,
 '[{"date":"2026-04-13","weekday":"星期日","cityNo":"10621","cityName":"上海","cityType":"1","meal":{"checked":true,"standard":100,"amount":40},"transport":{"checked":true,"standard":40,"amount":15},"comm":{"checked":true,"standard":40,"amount":15}},{"date":"2026-04-14","weekday":"星期一","cityNo":"10621","cityName":"上海","cityType":"1","meal":{"checked":true,"standard":100,"amount":40},"transport":{"checked":true,"standard":40,"amount":15},"comm":{"checked":true,"standard":40,"amount":15}}]'),
(6, 13, '13AB7925EB808001', '2026-04-15', '2026-04-15', 1, '上海-杭州', '10216', 80.00, 80.00, 80.00, 0.00, 0.00,
 '[{"date":"2026-04-15","weekday":"星期二","cityNo":"10216","cityName":"杭州","cityType":"2","meal":{"checked":true,"standard":80,"amount":80},"transport":{"checked":false,"standard":50,"amount":0},"comm":{"checked":false,"standard":50,"amount":0}}]'),
(6, 14, '13AB7925EB808001', '2026-04-16', '2026-04-16', 1, '杭州-荆州', '10455', 60.00, 60.00, 60.00, 0.00, 0.00,
 '[{"date":"2026-04-16","weekday":"星期三","cityNo":"10455","cityName":"荆州","cityType":"3","meal":{"checked":true,"standard":60,"amount":60},"transport":{"checked":false,"standard":30,"amount":0},"comm":{"checked":false,"standard":30,"amount":0}}]'),
(7, 15, '13AB4A56BB009002', '2026-05-20', '2026-05-21', 2, '武汉-杭州', '10216', 200.00, 200.00, 120.00, 40.00, 40.00,
 '[{"date":"2026-05-20","weekday":"星期二","cityNo":"10216","cityName":"杭州","cityType":"2","meal":{"checked":true,"standard":80,"amount":60},"transport":{"checked":true,"standard":50,"amount":20},"comm":{"checked":true,"standard":50,"amount":20}},{"date":"2026-05-21","weekday":"星期三","cityNo":"10216","cityName":"杭州","cityType":"2","meal":{"checked":true,"standard":80,"amount":60},"transport":{"checked":true,"standard":50,"amount":20},"comm":{"checked":true,"standard":50,"amount":20}}]'),
(7, 16, '13AB4A56BB009002', '2026-05-22', '2026-05-22', 1, '杭州-武汉', '10458', 80.00, 80.00, 80.00, 0.00, 0.00,
 '[{"date":"2026-05-22","weekday":"星期五","cityNo":"10458","cityName":"武汉","cityType":"2","meal":{"checked":true,"standard":80,"amount":80},"transport":{"checked":false,"standard":50,"amount":0},"comm":{"checked":false,"standard":50,"amount":0}}]'),
(8, 17, '13AB4A56BB009002', '2026-05-18', '2026-05-19', 2, '武汉-北京', '10119', 180.00, 180.00, 100.00, 40.00, 40.00,
 '[{"date":"2026-05-18","weekday":"星期日","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":50},"transport":{"checked":true,"standard":40,"amount":20},"comm":{"checked":true,"standard":40,"amount":20}},{"date":"2026-05-19","weekday":"星期一","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":50},"transport":{"checked":true,"standard":40,"amount":20},"comm":{"checked":true,"standard":40,"amount":20}}]');
