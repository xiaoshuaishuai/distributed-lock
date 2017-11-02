package com.github.xiao.lock.occ;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * mysql乐观锁 
 * @mail xstojob@gamil.com
 * @author ShuaishuaiXiao
 */
public class OccDemo {
	private static final String URL = "jdbc:mysql://172.16.5.205:7002/test?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai";
	private static final String USER = "root";
	private static final String PWD = "Password@1";

	private static final int THREADS_NUM = 10;// 线程长度10

	public static void main(String[] args) throws InterruptedException {
		ExecutorService es = Executors.newFixedThreadPool(THREADS_NUM);
		for (int i = 0; i < THREADS_NUM; i++) { // 多个线程修改库存10次
			es.submit(new Runnable() {
				public void run() {
					OccDemo occ = new OccDemo();
					occ.occ_lock(1);// 操作id为1的商品库存
				}
			});
			// Thread.sleep(300);
		}
		es.shutdown();

	}

	/**
	 * 乐观锁
	 */
	public void occ_lock(int id) {
		Connection connection = OccDemo.getConnection();

		/**
		 * 步骤开始
		 */
		// 1111111111111111111111111111111111111111111111111
		Map<String, Integer> maps = queryGoods(connection, id);
		// 2222222222222222222222222222222222222222222222222
		updateStock(connection, id, maps);

		// --------------------------------------……………………………………………………………………………………………………………………
		queryAllGoods(connection);// 这里查询下商品库存
		close(connection);
	}

	// 一、查询指定商品,包括版本号
	private Map<String, Integer> queryGoods(Connection connection, int id) {
		PreparedStatement ps = null;
		ResultSet rs = null;
		Map<String, Integer> maps = new HashMap<String, Integer>();
		try {
			ps = connection.prepareStatement("select id,goods_name,stock,version from test_goods where id =?");//
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while (rs.next()) {
				System.out.println("指定商品【乐观锁】 =======id:" + rs.getString("id") + "name:" + rs.getString("goods_name")
						+ "库存:" + rs.getString("stock") + "版本号:" + rs.getString("version"));
				// 把版本号和库存放入map
				maps.put("version", rs.getInt("version"));
				maps.put("stock", rs.getInt("stock"));
			}
		} catch (SQLException e) {
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
		return maps;
	}

	// 二、修改商品库存
	private boolean updateStock(Connection connection, int id, Map<String, Integer> maps) {
		// 先判断下库存，如果为0的话，就不在执行修改库存的操作了
		if (maps.get("stock") == 0) {
			System.out.println("亲。商品库存没有了，购买失败，请联系我们进行补货，欢迎下次光临！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！");
			return true;
		}
		PreparedStatement ps = null;

		try {
			ps = connection.prepareStatement(
					"update test_goods set stock= stock-1,version= version+1 where id =? and version=?");
			ps.setInt(1, id);
			ps.setInt(2, maps.get("version"));
			while (ps.executeUpdate() != 1) {
				System.out.println(Thread.currentThread().getName() + "修改库存【失败】了！！！！正在进行重试===========");
				Map<String, Integer> maps_retry = new HashMap<String, Integer>();
				/**
				 * 这里如果失败了，重试。。。
				 * 
				 * 重新执行 第 1，第2步。。。
				 */
				// 1111111111111111111111111111111111111111111111111
				maps_retry = queryGoods(connection, id);
				// 2222222222222222222222222222222222222222222222222
				if (updateStock(connection, id, maps_retry)) {// 如果修改库存成功了，就退出重试
					break;
				}
			}
			System.out.println(Thread.currentThread().getName() + "修改库存【成功】了！！！！");
			return true;

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (null != ps) {
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
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

	// 查询所有商品
	private void queryAllGoods(Connection connection) {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = connection.prepareStatement("select id,goods_name,stock,version from test_goods");
			rs = ps.executeQuery();
			while (rs.next()) {
				System.out.println("主键id:" + rs.getString("id") + ",商品name:" + rs.getString("goods_name") + ",库存:"
						+ rs.getString("stock") + ",版本号" + rs.getString("version"));
			}
		} catch (SQLException e) {
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
	}
}
/*****
 * 
 * console: 1000库存，卖出10件，预期库存990.。。。。
 * 
 * 
 * 
 * 指定商品【乐观锁】 =======id:1name:iPhoneX库存:1000版本号:1 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:1000版本号:1 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:1000版本号:1 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:1000版本号:1 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:1000版本号:1 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:1000版本号:1 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:1000版本号:1 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:1000版本号:1 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:1000版本号:1 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:1000版本号:1
 * pool-1-thread-7修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-2修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-9修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-8修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-4修改库存【失败】了！！！！正在进行重试=========== pool-1-thread-6修改库存【成功】了！！！！
 * pool-1-thread-5修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-3修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-1修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-10修改库存【失败】了！！！！正在进行重试=========== 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:999版本号:2 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:999版本号:2 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:999版本号:2 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:999版本号:2 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:999版本号:2 主键id:1,商品name:iPhoneX,库存:999,版本号2
 * 指定商品【乐观锁】 =======id:1name:iPhoneX库存:999版本号:2
 * 主键id:2,商品name:thinkpad,库存:16,版本号1 主键id:3,商品name:Nokia,库存:0,版本号1 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:999版本号:2 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:999版本号:2 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:999版本号:2 pool-1-thread-9修改库存【成功】了！！！！
 * pool-1-thread-2修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-7修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-3修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-4修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-1修改库存【失败】了！！！！正在进行重试=========== pool-1-thread-9修改库存【成功】了！！！！
 * pool-1-thread-8修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-5修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-10修改库存【失败】了！！！！正在进行重试=========== 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:998版本号:3 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:998版本号:3 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:998版本号:3 主键id:1,商品name:iPhoneX,库存:998,版本号3
 * 主键id:2,商品name:thinkpad,库存:16,版本号1 主键id:3,商品name:Nokia,库存:0,版本号1 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:998版本号:3 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:998版本号:3 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:998版本号:3 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:998版本号:3 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:998版本号:3
 * pool-1-thread-1修改库存【失败】了！！！！正在进行重试=========== pool-1-thread-2修改库存【成功】了！！！！
 * pool-1-thread-5修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-7修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-3修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-8修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-10修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-4修改库存【失败】了！！！！正在进行重试=========== pool-1-thread-2修改库存【成功】了！！！！
 * pool-1-thread-2修改库存【成功】了！！！！ 指定商品【乐观锁】 =======id:1name:iPhoneX库存:997版本号:4
 * 指定商品【乐观锁】 =======id:1name:iPhoneX库存:997版本号:4
 * 主键id:1,商品name:iPhoneX,库存:997,版本号4 主键id:2,商品name:thinkpad,库存:16,版本号1
 * 主键id:3,商品name:Nokia,库存:0,版本号1 指定商品【乐观锁】 =======id:1name:iPhoneX库存:997版本号:4
 * 指定商品【乐观锁】 =======id:1name:iPhoneX库存:997版本号:4 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:997版本号:4 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:997版本号:4 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:997版本号:4
 * pool-1-thread-10修改库存【失败】了！！！！正在进行重试=========== pool-1-thread-8修改库存【成功】了！！！！
 * pool-1-thread-8修改库存【成功】了！！！！ pool-1-thread-8修改库存【成功】了！！！！
 * pool-1-thread-8修改库存【成功】了！！！！ pool-1-thread-7修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-5修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-1修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-4修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-3修改库存【失败】了！！！！正在进行重试=========== 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:996版本号:5 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:996版本号:5 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:996版本号:5 主键id:1,商品name:iPhoneX,库存:996,版本号5
 * 主键id:2,商品name:thinkpad,库存:16,版本号1 主键id:3,商品name:Nokia,库存:0,版本号1 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:996版本号:5 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:996版本号:5 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:996版本号:5
 * pool-1-thread-1修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-10修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-4修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-7修改库存【失败】了！！！！正在进行重试=========== pool-1-thread-5修改库存【成功】了！！！！
 * pool-1-thread-3修改库存【失败】了！！！！正在进行重试=========== pool-1-thread-5修改库存【成功】了！！！！
 * pool-1-thread-5修改库存【成功】了！！！！ pool-1-thread-5修改库存【成功】了！！！！
 * pool-1-thread-5修改库存【成功】了！！！！ 指定商品【乐观锁】 =======id:1name:iPhoneX库存:995版本号:6
 * 主键id:1,商品name:iPhoneX,库存:995,版本号6 主键id:2,商品name:thinkpad,库存:16,版本号1
 * 主键id:3,商品name:Nokia,库存:0,版本号1 指定商品【乐观锁】 =======id:1name:iPhoneX库存:995版本号:6
 * 指定商品【乐观锁】 =======id:1name:iPhoneX库存:995版本号:6 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:995版本号:6 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:995版本号:6 pool-1-thread-7修改库存【成功】了！！！！
 * pool-1-thread-3修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-1修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-10修改库存【失败】了！！！！正在进行重试=========== pool-1-thread-7修改库存【成功】了！！！！
 * pool-1-thread-7修改库存【成功】了！！！！ pool-1-thread-7修改库存【成功】了！！！！
 * pool-1-thread-7修改库存【成功】了！！！！ pool-1-thread-7修改库存【成功】了！！！！
 * pool-1-thread-4修改库存【失败】了！！！！正在进行重试=========== 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:994版本号:7 主键id:1,商品name:iPhoneX,库存:994,版本号7
 * 主键id:2,商品name:thinkpad,库存:16,版本号1 主键id:3,商品name:Nokia,库存:0,版本号1 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:994版本号:7 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:994版本号:7 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:994版本号:7
 * pool-1-thread-1修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-10修改库存【失败】了！！！！正在进行重试=========== pool-1-thread-3修改库存【成功】了！！！！
 * pool-1-thread-3修改库存【成功】了！！！！ pool-1-thread-3修改库存【成功】了！！！！
 * pool-1-thread-4修改库存【失败】了！！！！正在进行重试=========== pool-1-thread-3修改库存【成功】了！！！！
 * pool-1-thread-3修改库存【成功】了！！！！ pool-1-thread-3修改库存【成功】了！！！！
 * pool-1-thread-3修改库存【成功】了！！！！ 指定商品【乐观锁】 =======id:1name:iPhoneX库存:993版本号:8
 * 指定商品【乐观锁】 =======id:1name:iPhoneX库存:993版本号:8
 * 主键id:1,商品name:iPhoneX,库存:993,版本号8 主键id:2,商品name:thinkpad,库存:16,版本号1
 * 主键id:3,商品name:Nokia,库存:0,版本号1 指定商品【乐观锁】 =======id:1name:iPhoneX库存:993版本号:8
 * pool-1-thread-1修改库存【成功】了！！！！ pool-1-thread-10修改库存【失败】了！！！！正在进行重试===========
 * pool-1-thread-1修改库存【成功】了！！！！ pool-1-thread-1修改库存【成功】了！！！！
 * pool-1-thread-1修改库存【成功】了！！！！ pool-1-thread-1修改库存【成功】了！！！！
 * pool-1-thread-1修改库存【成功】了！！！！ pool-1-thread-1修改库存【成功】了！！！！
 * pool-1-thread-1修改库存【成功】了！！！！ pool-1-thread-4修改库存【失败】了！！！！正在进行重试===========
 * 主键id:1,商品name:iPhoneX,库存:992,版本号9 主键id:2,商品name:thinkpad,库存:16,版本号1
 * 主键id:3,商品name:Nokia,库存:0,版本号1 指定商品【乐观锁】 =======id:1name:iPhoneX库存:992版本号:9
 * 指定商品【乐观锁】 =======id:1name:iPhoneX库存:992版本号:9 pool-1-thread-10修改库存【成功】了！！！！
 * pool-1-thread-4修改库存【失败】了！！！！正在进行重试=========== pool-1-thread-10修改库存【成功】了！！！！
 * pool-1-thread-10修改库存【成功】了！！！！ pool-1-thread-10修改库存【成功】了！！！！
 * pool-1-thread-10修改库存【成功】了！！！！ pool-1-thread-10修改库存【成功】了！！！！
 * pool-1-thread-10修改库存【成功】了！！！！ pool-1-thread-10修改库存【成功】了！！！！
 * pool-1-thread-10修改库存【成功】了！！！！ 主键id:1,商品name:iPhoneX,库存:991,版本号10 指定商品【乐观锁】
 * =======id:1name:iPhoneX库存:991版本号:10 主键id:2,商品name:thinkpad,库存:16,版本号1
 * 主键id:3,商品name:Nokia,库存:0,版本号1 pool-1-thread-4修改库存【成功】了！！！！
 * pool-1-thread-4修改库存【成功】了！！！！ pool-1-thread-4修改库存【成功】了！！！！
 * pool-1-thread-4修改库存【成功】了！！！！ pool-1-thread-4修改库存【成功】了！！！！
 * pool-1-thread-4修改库存【成功】了！！！！ pool-1-thread-4修改库存【成功】了！！！！
 * pool-1-thread-4修改库存【成功】了！！！！ pool-1-thread-4修改库存【成功】了！！！！
 * pool-1-thread-4修改库存【成功】了！！！！ 主键id:1,商品name:iPhoneX,库存:990,版本号11
 * 主键id:2,商品name:thinkpad,库存:16,版本号1 主键id:3,商品name:Nokia,库存:0,版本号1
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
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 **/
