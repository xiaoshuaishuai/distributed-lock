基于数据库实现的分布式锁
需要考虑的问题：
锁的可重入
1.可重入(应用的唯一标识-如果是微服务应用有在一台机器部署多个应用的情况就设计到应用唯一标识的问题)
2.锁的自动释放
3.阻塞锁

-------------------------------------------------------------------------
机器唯一标识

机器hostname+机器ip+序号(启动时生成原子的序号)+线程name+线程id

序号(启动时生成原子的序号):
目前有三种可以选择的方式：

1.基于数据库的悲观锁
set auto_commit = false;
begin;
select * from table where id = 1 for update;
update table set num = num + 1 where id = 1;
commit;
set auto_commit = true;

2.基于数据库的乐观锁
select * as rs from table where id = 1;
update table set num = num + 1 where id = 1 and num = rs.num;
如果没有更新成功;可以采用重试机制

3. 将类似的字段放到 Redis 中维护,规避这种问题

Redis 自带的  incr 和 decr 操作具有原子性,可以保证数据一致性