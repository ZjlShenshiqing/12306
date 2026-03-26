-- 创建 user-service 分片表：t_user、t_user_phone、t_user_mail（每库 0-15）
-- 在 12306_user_0 和 12306_user_1 中执行

-- ========== 12306_user_0 ==========
USE `12306_user_0`;

-- t_user_0 到 t_user_15（加密列用 VARCHAR(256) 存密文）
CREATE TABLE IF NOT EXISTS `t_user_0` (
  `id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `region` VARCHAR(100) DEFAULT NULL COMMENT '地区',
  `id_type` INT(11) NOT NULL DEFAULT 0 COMMENT '证件类型',
  `id_card` VARCHAR(256) DEFAULT NULL COMMENT '证件号',
  `phone` VARCHAR(256) DEFAULT NULL COMMENT '电话',
  `telephone` VARCHAR(20) DEFAULT NULL COMMENT '固定电话',
  `mail` VARCHAR(256) DEFAULT NULL COMMENT '邮箱',
  `user_type` INT(11) NOT NULL DEFAULT 1 COMMENT '旅客类型',
  `verify_status` INT(11) NOT NULL DEFAULT 0 COMMENT '审核状态',
  `post_code` VARCHAR(20) DEFAULT NULL COMMENT '邮编',
  `address` VARCHAR(256) DEFAULT NULL COMMENT '地址',
  `deletion_time` BIGINT(20) DEFAULT NULL COMMENT '注销时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `del_flag` INT(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_user_1` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_2` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_3` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_4` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_5` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_6` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_7` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_8` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_9` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_10` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_11` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_12` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_13` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_14` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_15` LIKE `t_user_0`;

-- t_user_phone_0 到 t_user_phone_15
SET @i = 0;
CREATE TABLE IF NOT EXISTS `t_user_phone_0` (
  `id` BIGINT(20) NOT NULL COMMENT '主键ID',
  `username` VARCHAR(256) NOT NULL COMMENT '用户名',
  `phone` VARCHAR(256) NOT NULL COMMENT '手机号',
  `deletion_time` BIGINT(20) DEFAULT NULL COMMENT '注销时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `del_flag` INT(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 批量创建 t_user_phone_1 到 t_user_phone_15
CREATE TABLE IF NOT EXISTS `t_user_phone_1` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_2` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_3` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_4` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_5` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_6` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_7` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_8` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_9` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_10` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_11` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_12` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_13` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_14` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_15` LIKE `t_user_phone_0`;

-- t_user_mail_0 到 t_user_mail_15
CREATE TABLE IF NOT EXISTS `t_user_mail_0` (
  `id` BIGINT(20) NOT NULL COMMENT '主键ID',
  `username` VARCHAR(256) NOT NULL COMMENT '用户名',
  `mail` VARCHAR(256) NOT NULL COMMENT '邮箱',
  `deletion_time` BIGINT(20) DEFAULT NULL COMMENT '注销时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `del_flag` INT(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mail` (`mail`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_user_mail_1` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_2` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_3` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_4` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_5` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_6` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_7` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_8` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_9` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_10` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_11` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_12` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_13` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_14` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_15` LIKE `t_user_mail_0`;

-- ========== 12306_user_1 ==========
USE `12306_user_1`;

-- t_user_0 到 t_user_15
CREATE TABLE IF NOT EXISTS `t_user_0` (
  `id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `region` VARCHAR(100) DEFAULT NULL COMMENT '地区',
  `id_type` INT(11) NOT NULL DEFAULT 0 COMMENT '证件类型',
  `id_card` VARCHAR(256) DEFAULT NULL COMMENT '证件号',
  `phone` VARCHAR(256) DEFAULT NULL COMMENT '电话',
  `telephone` VARCHAR(20) DEFAULT NULL COMMENT '固定电话',
  `mail` VARCHAR(256) DEFAULT NULL COMMENT '邮箱',
  `user_type` INT(11) NOT NULL DEFAULT 1 COMMENT '旅客类型',
  `verify_status` INT(11) NOT NULL DEFAULT 0 COMMENT '审核状态',
  `post_code` VARCHAR(20) DEFAULT NULL COMMENT '邮编',
  `address` VARCHAR(256) DEFAULT NULL COMMENT '地址',
  `deletion_time` BIGINT(20) DEFAULT NULL COMMENT '注销时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `del_flag` INT(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_user_1` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_2` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_3` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_4` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_5` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_6` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_7` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_8` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_9` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_10` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_11` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_12` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_13` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_14` LIKE `t_user_0`;
CREATE TABLE IF NOT EXISTS `t_user_15` LIKE `t_user_0`;

CREATE TABLE IF NOT EXISTS `t_user_phone_0` (
  `id` BIGINT(20) NOT NULL COMMENT '主键ID',
  `username` VARCHAR(256) NOT NULL COMMENT '用户名',
  `phone` VARCHAR(256) NOT NULL COMMENT '手机号',
  `deletion_time` BIGINT(20) DEFAULT NULL COMMENT '注销时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `del_flag` INT(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_user_phone_1` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_2` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_3` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_4` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_5` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_6` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_7` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_8` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_9` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_10` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_11` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_12` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_13` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_14` LIKE `t_user_phone_0`;
CREATE TABLE IF NOT EXISTS `t_user_phone_15` LIKE `t_user_phone_0`;

CREATE TABLE IF NOT EXISTS `t_user_mail_0` (
  `id` BIGINT(20) NOT NULL COMMENT '主键ID',
  `username` VARCHAR(256) NOT NULL COMMENT '用户名',
  `mail` VARCHAR(256) NOT NULL COMMENT '邮箱',
  `deletion_time` BIGINT(20) DEFAULT NULL COMMENT '注销时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `del_flag` INT(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mail` (`mail`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_user_mail_1` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_2` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_3` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_4` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_5` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_6` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_7` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_8` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_9` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_10` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_11` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_12` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_13` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_14` LIKE `t_user_mail_0`;
CREATE TABLE IF NOT EXISTS `t_user_mail_15` LIKE `t_user_mail_0`;
