package com.github.xiao.lock.distributedLock.redis.redlock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * redisson的redlock算法实现
 */
public class RedissonLockDemo {
	private static final int THREADS_NUM = 10;// 线程长度10

	public static void main(String[] args) throws InterruptedException {
		Config config = new Config();
		// config.setUseLinuxNativeEpoll(true);
		config.useClusterServers().addNodeAddress("redis://172.29.150.1:6390", "redis://172.29.150.2:6393",
				"redis://172.29.150.3:40010");
		RedissonClient client = Redisson.create(config);
		final RLock lock = client.getLock("anyLock");

		ExecutorService es = Executors.newFixedThreadPool(THREADS_NUM);
		for (int i = 0; i < THREADS_NUM; i++) { // 多个线程修改库存10次
			es.submit(new Runnable() {
				public void run() {
					boolean res;
					try {
						res = lock.tryLock(100, 10, TimeUnit.SECONDS);		// 尝试加锁，最多等待100秒，上锁以后60秒自动解锁
						if (res) {
							System.out.println(Thread.currentThread().getName() + "，拿到了锁");
							System.out.println("开始执行业务逻辑====================start");
							Thread.sleep(2000);
							System.out.println("开始执行业务逻辑====================end");
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					} finally {
						lock.unlock();
					}
				}
			});
			// Thread.sleep(300);
		}
		
		
		Thread.sleep(30000);
		es.shutdown();
		client.shutdown();
		System.out.println("****************************threads&redisson关闭****************************");

	}
}
