mysql分布式锁-基于悲观锁

问题：
1.重入(clientId解决)
2.等待超时(目前依赖于数据库的innodb_lock_wait_timeout超时等待)

高并发情况下会出现：
1.连接被占用
2.系统等待
2.如果业务逻辑执行时间较长，斟酌innodb_lock_wait_timeout


使用场景：
高并发下慎用，增加数据库压力！
适合的场景为并发小，业务执行时间较短的情况。