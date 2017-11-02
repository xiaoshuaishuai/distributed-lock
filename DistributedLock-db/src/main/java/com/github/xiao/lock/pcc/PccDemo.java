package com.github.xiao.lock.pcc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * mysql悲观锁 
 * @mail xstojob@gamil.com
 * @author ShuaishuaiXiao
 */
public class PccDemo {
	private static final String URL="jdbc:mysql://172.16.5.205:7002/test?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai";
	private static final String USER="root";
	private static final String PWD="Password@1";
	
	private static final int THREADS_NUM=10;//线程长度10
	public static void main(String[] args) {
		ExecutorService es = Executors.newFixedThreadPool(THREADS_NUM);
		for (int i=0;i<THREADS_NUM;i++) { //多个线程修改库存10次
			es.submit(new Runnable() {
				public void run() {
					PccDemo pcc = new PccDemo();
					pcc.pcc_lock(1);
				}
			});
		}
		es.shutdown();
	
	}
	/**
	 * 悲观锁应用 
	 */
	public void pcc_lock(int id) {
		Connection connection = PccDemo.getConnection();
		//queryAllGoods(connection);
		
		/**
		 * 1-4步骤开始
		 */
		//1111111111111111111111111111111111111111111111111
		beginT(connection);
		//2222222222222222222222222222222222222222222222222
		int goon=0;
		try {
			queryGoods(connection,id);
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackT(connection);//这里事务超时后，回滚事务
			goon++;
		}
		//如果出现事务超时现象，将不在执行第三四步骤！！！
		if(goon==0) {
			//3333333333333333333333333333333333333333333333333
			updateStock(connection,id);
			//4444444444444444444444444444444444444444444444444
			commitT(connection);
		}
		
		//--------------------------------------……………………………………………………………………………………………………………………
		queryAllGoods(connection);//这里查询下商品库存
		close(connection);
	}
	
	//一、开启事务
	private void beginT(Connection connection) {
		try {
			connection.setAutoCommit(false);//不设置自动提交
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	//二、查询指定商品
	private void queryGoods(Connection connection, int id) throws SQLException {
		PreparedStatement ps =null;
		ResultSet rs =null;
		try {
			ps = connection.prepareStatement("select id,goods_name,stock from test_goods where id =?  for update");//for update悲观锁 这里id是主键所以不会锁表
			ps.setInt(1, id);
			rs = ps.executeQuery();
			while(rs.next()) {
				System.out.println("指定商品【for update】 =======id:"+rs.getString("id")+"name:"+rs.getString("goods_name")+"库存:"+rs.getString("stock"));
			}
		} 
		finally {
			if(null!=rs) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if(null!=ps) {
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	//三、修改商品库存
	private void updateStock(Connection connection, int id) {
		PreparedStatement ps =null;
		try {
			ps = connection.prepareStatement("update test_goods set stock=stock-1 where id =?");
			ps.setInt(1, id);
			if(ps.executeUpdate()==1) {
				System.out.println("修改库存成功了！！！！");
			}else {
				System.out.println("修改库存失败了！！！！");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			if(null!=ps) {
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	//四、提交事务
	private void commitT(Connection connection) {
		try {
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			rollbackT(connection);
		}
	}
	//回滚
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
		Connection connection =null;
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
	//关闭连接
	public static void close(Connection connection) {
		if(null!=connection) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	//查询所有商品
	private void queryAllGoods(Connection connection) {
		PreparedStatement ps =null;
		ResultSet rs =null;
		try {
			ps = connection.prepareStatement("select id,goods_name,stock from test_goods");
			rs = ps.executeQuery();
			while(rs.next()) {
				System.out.println("主键id:"+rs.getString("id")+",商品name:"+rs.getString("goods_name")+",库存:"+rs.getString("stock"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		finally {
			if(null!=rs) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if(null!=ps) {
				try {
					ps.close();
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
	 * 商品id为1，库存100，执行10次，最终库存90
	 * 
	 * 
	 * 
	 * console：
	 *  指定商品【for update】 =======id:1name:iPhoneX库存:100
		修改库存成功了！！！！
		指定商品【for update】 =======id:1name:iPhoneX库存:99
		主键id:1,商品name:iPhoneX,库存:99
		主键id:2,商品name:thinkpad,库存:16
		主键id:3,商品name:Nokia,库存:0
		修改库存成功了！！！！
		指定商品【for update】 =======id:1name:iPhoneX库存:98
		修改库存成功了！！！！
		主键id:1,商品name:iPhoneX,库存:98
		主键id:2,商品name:thinkpad,库存:16
		主键id:3,商品name:Nokia,库存:0
		指定商品【for update】 =======id:1name:iPhoneX库存:97
		主键id:1,商品name:iPhoneX,库存:97
		主键id:2,商品name:thinkpad,库存:16
		主键id:3,商品name:Nokia,库存:0
		修改库存成功了！！！！
		指定商品【for update】 =======id:1name:iPhoneX库存:96
		主键id:1,商品name:iPhoneX,库存:96
		主键id:2,商品name:thinkpad,库存:16
		主键id:3,商品name:Nokia,库存:0
		修改库存成功了！！！！
		指定商品【for update】 =======id:1name:iPhoneX库存:95
		修改库存成功了！！！！
		主键id:1,商品name:iPhoneX,库存:95
		主键id:2,商品name:thinkpad,库存:16
		主键id:3,商品name:Nokia,库存:0
		指定商品【for update】 =======id:1name:iPhoneX库存:94
		主键id:1,商品name:iPhoneX,库存:94
		主键id:2,商品name:thinkpad,库存:16
		主键id:3,商品name:Nokia,库存:0
		修改库存成功了！！！！
		指定商品【for update】 =======id:1name:iPhoneX库存:93
		主键id:1,商品name:iPhoneX,库存:93
		主键id:2,商品name:thinkpad,库存:16
		主键id:3,商品name:Nokia,库存:0
		修改库存成功了！！！！
		指定商品【for update】 =======id:1name:iPhoneX库存:92
		修改库存成功了！！！！
		主键id:1,商品name:iPhoneX,库存:92
		主键id:2,商品name:thinkpad,库存:16
		主键id:3,商品name:Nokia,库存:0
		指定商品【for update】 =======id:1name:iPhoneX库存:91
		修改库存成功了！！！！
		主键id:1,商品name:iPhoneX,库存:91
		主键id:2,商品name:thinkpad,库存:16
		主键id:3,商品name:Nokia,库存:0
		主键id:1,商品name:iPhoneX,库存:90
		主键id:2,商品name:thinkpad,库存:16
		主键id:3,商品name:Nokia,库存:0
	 * 
	 * 
	 * 
	 */
}
