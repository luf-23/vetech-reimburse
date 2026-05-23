-- 示例报销单（执行前请先执行 01_schema.sql；主数据 ID 与前端 masterData.ts 一致）

USE vetech_reimburse;

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

INSERT INTO reimburse_allocation (doc_id, cost_attribution_id, project_id, ratio, amount, sort_order) VALUES
(1, '1C61686865DA8000', '1771EC45F2443000', 0.4000, 216.00, 0),
(1, '1C54557F1782E000', '1762792DB4E9A002', 0.3500, 189.00, 1),
(1, '19218A262C976000', '1C5931735AC4A000', 0.2500, 135.00, 2),
(2, '1C54557F1782E000', '1771EC45F2443000', 0.6000, 66.60, 0),
(2, '19218A262C976000', '1762792DB4E9A002', 0.4000, 44.40, 1),
(5, '1717271D1DA15000', '17071065FC29A002', 0.5000, 92.50, 0),
(5, '16AE93CC7EF92002', '162664EBE9ABE001', 0.5000, 92.50, 1);

INSERT INTO reimburse_subsidy (doc_id, itinerary_id, traveler_id, start_date, end_date, days, route, subsidy_city_no, apply_amount, subsidy_amount, meal_total, transport_total, comm_total, calendar_json) VALUES
(1, 1, '13AB3A3F72409002', '2026-04-13', '2026-04-17', 5, '武汉-北京', '10119', 540.00, 540.00, 300.00, 120.00, 120.00,
 '[{"date":"2026-04-13","weekday":"星期一","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":40},"comm":{"checked":true,"standard":40,"amount":40}},{"date":"2026-04-14","weekday":"星期二","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":40},"comm":{"checked":true,"standard":40,"amount":40}},{"date":"2026-04-15","weekday":"星期三","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":40},"comm":{"checked":true,"standard":40,"amount":40}},{"date":"2026-04-16","weekday":"星期四","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":false,"standard":100,"amount":100},"transport":{"checked":false,"standard":40,"amount":40},"comm":{"checked":false,"standard":40,"amount":40}},{"date":"2026-04-17","weekday":"星期五","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":false,"standard":100,"amount":100},"transport":{"checked":false,"standard":40,"amount":40},"comm":{"checked":false,"standard":40,"amount":40}}]'),
(5, 3, '13AB77281A408001', '2026-05-10', '2026-05-10', 1, '武汉-上海', '10621', 180.00, 185.00, 100.00, 40.00, 45.00,
 '[{"date":"2026-05-10","weekday":"星期日","cityNo":"10621","cityName":"上海","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":40},"comm":{"checked":true,"standard":40,"amount":45}}]'),
(2, 2, '13AB498CC6409002', '2026-05-13', '2026-05-14', 2, '北京-上海', '10621', 140.00, 111.00, 100.00, 11.00, 0.00,
 '[{"date":"2026-05-13","weekday":"星期三","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":true,"standard":100,"amount":100},"transport":{"checked":true,"standard":40,"amount":11},"comm":{"checked":false,"standard":40,"amount":40}},{"date":"2026-05-14","weekday":"星期四","cityNo":"10119","cityName":"北京","cityType":"1","meal":{"checked":false,"standard":100,"amount":100},"transport":{"checked":false,"standard":40,"amount":40},"comm":{"checked":false,"standard":40,"amount":40}}]');
