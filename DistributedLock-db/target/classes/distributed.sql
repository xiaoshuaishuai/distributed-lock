-- 机器唯一标识递增序列
CREATE TABLE `test_seq` (
  `id` int(11) NOT NULL,
  `seq` int(11) DEFAULT '1000' COMMENT '序列',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF-8;

INSERT INTO `test`.`test_seq` (`id`, `seq`) VALUES ('1', '1000');


CREATE TABLE `distributed_lock` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lock_name` varchar(255) CHARACTER SET utf8 NOT NULL DEFAULT '' COMMENT '锁标识',
  `clientId` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT '客户端信息',
  `taketime` datetime DEFAULT NULL COMMENT '获取锁时间',
  `releasetime` datetime DEFAULT NULL COMMENT '释放锁时间',
  `version` int(11) NOT NULL DEFAULT '0' COMMENT '版本号：乐观锁使用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `lock_name_index` (`lock_name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=UTF-8;

INSERT INTO `test`.`distributed_lock` (`id`, `lock_name`, `clientId`, `taketime`, `releasetime`, `version`) VALUES ('1', 'Test_Job', NULL, NULL, NULL, '0');
INSERT INTO `test`.`distributed_lock` (`id`, `lock_name`, `clientId`, `taketime`, `releasetime`, `version`) VALUES ('2', 'UpdateCache_Lock', NULL, NULL, NULL, '0');
INSERT INTO `test`.`distributed_lock` (`id`, `lock_name`, `clientId`, `taketime`, `releasetime`, `version`) VALUES ('3', 'Default_Lock', NULL, NULL, NULL, '0');
