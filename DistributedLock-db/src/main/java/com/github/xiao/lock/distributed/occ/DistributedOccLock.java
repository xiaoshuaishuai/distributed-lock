package com.github.xiao.lock.distributed.occ;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.github.xiao.lock.distributed.DistributedLockEntity;
import com.github.xiao.lock.distributed.MachineOnly;

/**
 * mysql分布式锁-基于乐观锁
 * 
 * @mail xstojob@gamil.com
 * @author ShuaishuaiXiao
 */
public class DistributedOccLock {
	private static final String URL = "jdbc:mysql://172.16.5.205:7002/test?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai";
	private static final String USER = "root";
	private static final String PWD = "Password@1";
	private static final int THREADS_NUM = 10;// 线程长度10
	private static final String UNLOCKED = "0";
	private static final String LOCKED = "1";

	public static void main(String[] args) {
		ExecutorService es = Executors.newFixedThreadPool(THREADS_NUM);
		for (int i = 0; i < THREADS_NUM; i++) {
			es.submit(new Runnable() {
				public void run() {
					Connection connection = DistributedOccLock.getConnection();
					DistributedOccLock distribute = new DistributedOccLock();
					DistributedLockEntity entity = new DistributedLockEntity();
					entity.setLockName("Test_Job");
					if (distribute.lock(connection, entity)) {
						System.out.println(Thread.currentThread().getName() + "修改分布式锁创建时间，客户端标识【成功>>>>>>>>>>>】");
						System.out.println(
								Thread.currentThread().getName() + "******************执行业务操作start*****************");
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						System.out.println(
								Thread.currentThread().getName() + "******************执行业务操作end*****************");
					}
					distribute.unlock(connection, entity);
					DistributedOccLock.close(connection);// 释放连接
				}
			});
			// Thread.sleep(300);
		}
		es.shutdown();

	}

	/**
	 * 
	 * 方法描述
	 * 
	 * @param connection
	 * @param distributedLockEntity
	 *            锁标识名字
	 * @return boolean
	 * @变更记录 2017年11月2日17:04:53 ShuaishuaiXiao
	 */
	public boolean lock(Connection connection, DistributedLockEntity distributedLockEntity) {
		PreparedStatement ps = null;
		ResultSet rs = null;
		PreparedStatement updatePs = null;
		String selectSQL = "SELECT id,version FROM distributed_lock where lock_name=? and locked=?";
		String updateSQL = "update distributed_lock set taketime=?,clientId=? ,version = version+1 ,locked=? where lock_name=? and version =?";
		int oldVersion = 0;
		try {
			ps = connection.prepareStatement(selectSQL);
			ps.setString(1, distributedLockEntity.getLockName());
			ps.setString(2, UNLOCKED);// 没有上锁的记录
			rs = ps.executeQuery();
			while (rs.next()) {
				oldVersion = rs.getInt("version");
			}
			updatePs = connection.prepareStatement(updateSQL);
			distributedLockEntity.setClientId(MachineOnly.assemblyId());

			updatePs.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
			updatePs.setString(2, distributedLockEntity.getClientId());
			updatePs.setString(3, LOCKED);// 上锁
			updatePs.setString(4, distributedLockEntity.getLockName());
			updatePs.setInt(5, oldVersion);
			if (updatePs.executeUpdate() != 1) {
				System.out.println(Thread.currentThread().getName() + "获取锁【失败】了！！！！");
				return false;
			}

			// while (updatePs.executeUpdate() != 1) {
			// System.out.println(Thread.currentThread().getName() +
			// "获取锁【失败】了！！！！正在进行重试===========");
			// if(lock(connection, distributedLockEntity)) {//
			// break;
			// }
			// }
			return true;

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} finally {
			if (null != rs) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (null != ps) {
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}

		return false;
	}

	/**
	 * 
	 * 方法描述 释放锁
	 * 
	 * @param connection
	 * @param distributedLockEntity
	 *            void
	 * @变更记录 2017年11月2日 下午3:32:05 ShuaishuaiXiao
	 */
	public void unlock(Connection connection, DistributedLockEntity distributedLockEntity) {
		/**
		 * 记录释放锁的时间。。。。。。。。。。。。。
		 */
		PreparedStatement ps = null;
		String updateSQL = "update distributed_lock set releasetime=? , locked = ? where lock_name=?";
		try {
			ps = connection.prepareStatement(updateSQL);
			ps.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
			ps.setString(2, UNLOCKED);// 释放锁
			ps.setString(3, distributedLockEntity.getLockName());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (null != ps) {
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		/**
		 * 。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。
		 */
	}

	private static Connection getConnection() {
		Connection connection = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection(URL, USER, PWD);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return connection;
	}

	// 关闭连接
	public static void close(Connection connection) {
		if (null != connection) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
/**
 * 
 * 
 * 
 * 
pool-1-thread-10获取锁【失败】了！！！！
pool-1-thread-3修改分布式锁创建时间，客户端标识【成功>>>>>>>>>>>】
pool-1-thread-3******************执行业务操作start*****************
pool-1-thread-6获取锁【失败】了！！！！
pool-1-thread-5获取锁【失败】了！！！！
pool-1-thread-4获取锁【失败】了！！！！
pool-1-thread-7获取锁【失败】了！！！！
pool-1-thread-1获取锁【失败】了！！！！
pool-1-thread-8获取锁【失败】了！！！！
pool-1-thread-9获取锁【失败】了！！！！
pool-1-thread-2获取锁【失败】了！！！！
pool-1-thread-3******************执行业务操作end*****************

 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */
