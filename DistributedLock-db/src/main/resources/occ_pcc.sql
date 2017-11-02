--- 构建表结构
CREATE TABLE `test_goods` (
  `id` int(11) NOT NULL,
  `goods_name` varchar(255) DEFAULT NULL,
  `stock` int(11) NOT NULL DEFAULT '0',
  `version` int(11) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF-8;

--- 插入数据
INSERT INTO `test`.`test_goods` (`id`, `goods_name`, `stock`, `version`) VALUES ('1', 'iPhoneX', '1000', '1');
INSERT INTO `test`.`test_goods` (`id`, `goods_name`, `stock`, `version`) VALUES ('2', 'thinkpad', '16', '1');
INSERT INTO `test`.`test_goods` (`id`, `goods_name`, `stock`, `version`) VALUES ('3', 'Nokia', '0', '1');
