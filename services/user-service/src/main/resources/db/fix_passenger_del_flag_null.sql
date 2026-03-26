-- 修复 t_passenger 分表中 del_flag 为 NULL 导致查询不到数据的问题
-- 背景：系统查询会自动追加 WHERE del_flag = 0，但你历史 INSERT 的 del_flag 为 NULL，导致总是查不到
-- 作用：将所有分表里 del_flag IS NULL 的记录修正为 0，并补齐 create_time/update_time（可选但建议）
--
-- 执行方式：在 MySQL 客户端中直接执行本脚本即可

-- ---------- 库 12306_user_0 (ds_0) ----------
USE 12306_user_0;
UPDATE t_passenger_0  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_1  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_2  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_3  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_4  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_5  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_6  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_7  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_8  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_9  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_10 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_11 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_12 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_13 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_14 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_15 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_16 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_17 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_18 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_19 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_20 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_21 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_22 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_23 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_24 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_25 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_26 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_27 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_28 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_29 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_30 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_31 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;

-- ---------- 库 12306_user_1 (ds_1) ----------
USE 12306_user_1;
UPDATE t_passenger_0  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_1  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_2  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_3  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_4  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_5  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_6  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_7  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_8  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_9  SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_10 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_11 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_12 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_13 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_14 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_15 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_16 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_17 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_18 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_19 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_20 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_21 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_22 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_23 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_24 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_25 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_26 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_27 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_28 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_29 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_30 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;
UPDATE t_passenger_31 SET del_flag = 0, create_time = IFNULL(create_time, NOW()), update_time = IFNULL(update_time, NOW()) WHERE del_flag IS NULL;

