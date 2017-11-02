package com.github.xiao.lock.distributed.pcc;

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
 * mysql分布式锁-基于悲观锁
 * 
 * @mail xstojob@gamil.com
 * @author ShuaishuaiXiao
 */
public class DistributedPccLock {
	private static final String URL = "jdbc:mysql://172.16.5.205:7002/test?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai";
	private static final String USER = "root";
	private static final String PWD = "Password@1";
	private static final int THREADS_NUM = 10;// 线程长度10

	public static void main(String[] args) {
		ExecutorService es = Executors.newFixedThreadPool(THREADS_NUM);
		for (int i = 0; i < THREADS_NUM; i++) {
			es.submit(new Runnable() {
				public void run() {
					Connection connection = DistributedPccLock.getConnection();
					DistributedPccLock distribute = new DistributedPccLock();
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
					DistributedPccLock.close(connection);// 释放连接
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
	 * @param timeout
	 *            等待锁的超时时间,单位毫秒
	 * @return boolean
	 * @变更记录 2017年11月2日 下午3:33:37 ShuaishuaiXiao
	 */
	public boolean lock(Connection connection, DistributedLockEntity distributedLockEntity) {
		PreparedStatement ps = null;
		ResultSet rs = null;
		int num = 0;
		int max = 10;
		String selectSQL = "SELECT id FROM distributed_lock where lock_name=? for update";// lock_name建立了唯一索引，所以不会导致表锁
		String updateSQL = "update distributed_lock set taketime=?,clientId=? where lock_name=?";
		try {
			connection.setAutoCommit(false);// 关闭自动提交
			ps = connection.prepareStatement(selectSQL);
			ps.setString(1, distributedLockEntity.getLockName());
			if (ps.execute()) {// 获取到锁了
				PreparedStatement updatePs = connection.prepareStatement(updateSQL);
				distributedLockEntity.setClientId(MachineOnly.assemblyId());

				updatePs.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
				updatePs.setString(2, distributedLockEntity.getClientId());
				updatePs.setString(3, distributedLockEntity.getLockName());
				while (updatePs.executeUpdate() == 1 && num < 10) {
					num++;
					break;
				}
				if (num == max) {
					System.out.println(Thread.currentThread().getName() + "获取分布式锁失败，原因：更新客户端标识失败！！！");
					return false;
				}
				return true;
			} 
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackT(connection);
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
		String updateSQL = "update distributed_lock set releasetime=? where lock_name=?";
		try {
			ps = connection.prepareStatement(updateSQL);
			ps.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
			ps.setString(2, distributedLockEntity.getLockName());
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
		commitT(connection);// 事务提交锁释放
	}

	// 提交事务
	private void commitT(Connection connection) {
		try {
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackT(connection);
		}
	}

	// 回滚
	private void rollbackT(Connection connection) {
		try {
			connection.rollback();
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
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
