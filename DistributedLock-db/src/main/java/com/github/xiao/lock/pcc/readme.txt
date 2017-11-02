基于InnoDB存储引擎行锁的悲观锁
select ... for update

/**
 * 数据库悲观锁实例
 * Pessimistic Concurrency Control，缩写“PCC”
 * 【*************商品减库存案例*****************】
 * 1.begin 开启事物 //设置antocommit=false
 * 2.====查询指定商品
 * 2.select * from test_goods where id=1 for update
 * 3.===修改商品库存
 * update test_goods set stock -1 where id=1
 * 4.====commit提交事务
 * 
 * 
 * 不足：
 * -----------------------------
 * -悲观并发控制实际上是“先取锁再访问”的保守策略，-
 * -为数据处理的安全提供了保证。但是在效率方面，-
 * -处理加锁的机制会让数据库产生额外的开销，-
 * -还有增加产生死锁的机会；-
 * -另外，在只读型事务处理中由于不会产生冲突，-
 * -也没必要使用锁，这样做只能增加系统负载；-
 * -还有会降低了并行性，一个事务如果锁定了某行数据-
 * -其他事务就必须等待该事务处理完才可以处理那行数-
 * -----------------------------
 */
 
 * mysql参数：
 * innodb_rollback_on_timeout=OFF
 * autocommit=ON
 * innodb_lock_wait_timeout=420s
 * 7分钟
 * ===================
 * 1、关闭innodb_rollback_on_timeout后，一旦以begin;start transaction;等语句开启一个事务，当锁等待超时后，该事务请求的锁将不释放，直到事务提交或回滚或会话超时；

	所以autocommit参数建议设置成ON，只要程序没有显示开启事务，就可以避免上述锁未释放问题。
	
	2、开启innodb_rollback_on_timeout后，一旦锁等待超时，是事务内sql将全部回滚，且释放之前请求的锁。
	
	3、当autocommit=on，只要不显示开启事务，将不存在上面2个问题，即锁的问题和回滚的问题。
 总结：
 1.并发等待问题
 2.如果并发过高，会出现很多连接占用问题
 3.死锁(正常commit，异常提交rollback，意外连接中断有可能出现死锁)