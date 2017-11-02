package com.github.xiao.lock.distributed;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 机器唯一标识的生成方式
 * 
 * 机器hostname+机器ip+序号(启动时生成原子的序号)+线程name+线程id
 */
public class MachineOnly {

	public static final String SEGMENT = "/";// 分割标识

	private static final String URL = "jdbc:mysql://172.16.5.205:7002/test?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai";
	private static final String USER = "root";
	private static final String PWD = "Password@1";

	/**
	 * 
	 * 方法描述 拼接机器唯一标识
	 * 
	 * @return
	 * @throws UnknownHostException
	 *             String
	 * @变更记录 2017年11月2日 下午3:00:51 ShuaishuaiXiao
	 */
	public static String assemblyId() throws UnknownHostException {

		StringBuilder identification = new StringBuilder(60);
		identification.append(InetAddress.getLocalHost().getHostName() + MachineOnly.SEGMENT);
		identification.append(InetAddress.getLocalHost().getHostAddress() + MachineOnly.SEGMENT);
		identification.append(MachineOnly.produceSeq() + MachineOnly.SEGMENT);// 数据库生成
		identification.append(Thread.currentThread().getName() + MachineOnly.SEGMENT);
		identification.append(Thread.currentThread().getId() + MachineOnly.SEGMENT);
		return identification.toString();
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

	/**
	 * 基于悲观锁生成序列号(考虑机器启动时并发量) 方法描述
	 * 
	 * @return String
	 * @变更记录 2017年11月2日 下午3:06:48 ShuaishuaiXiao
	 */
	public static int produceSeq() {
		Connection connection = MachineOnly.getConnection();
		PreparedStatement old_ps = null;
		PreparedStatement update_ps = null;
		PreparedStatement new_ps = null;
		ResultSet old_rs = null;
		ResultSet new_rs = null;
		int seq = 0;
		try {
			connection.setAutoCommit(false);
			old_ps = connection.prepareStatement("select seq from test_seq where id =1 for update");
			old_rs = old_ps.executeQuery();
			while (old_rs.next()) {
//				System.out.println("【old】序列号:" + old_rs.getInt("seq"));
			}
			update_ps = connection.prepareStatement("update test_seq set seq=seq+1 where id =1");
			if (update_ps.executeUpdate() == 1) {
//				System.out.println("生成序列号成功啦。。。");
			}
			new_ps = connection.prepareStatement("select seq from test_seq where id =1");
			new_rs = new_ps.executeQuery();
			while (new_rs.next()) {
				seq = new_rs.getInt("seq");
//				System.out.println("【new】序列号:" + seq);
			}
			connection.commit();
			return seq;
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} finally {
			if (null != old_ps) {
				try {
					old_ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (null != update_ps) {
				try {
					update_ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (null != new_ps) {
				try {
					new_ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (null != old_rs) {
				try {
					old_rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (null != new_rs) {
				try {
					new_rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			MachineOnly.close(connection);
		}
		return seq;
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

	public static void main(String[] args) throws UnknownHostException {
		System.out.println(MachineOnly.assemblyId());
	}

}
