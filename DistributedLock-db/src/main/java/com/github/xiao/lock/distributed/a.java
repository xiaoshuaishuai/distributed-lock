package com.github.xiao.lock.distributed;

public class a {

	public static void main(String[] args) throws InterruptedException {
		long start = System.currentTimeMillis();
		Thread.sleep(2000);
		System.out.println(System.currentTimeMillis() - start);
		while (true) {
			System.out.println("---");
			return;
		}
	}
}
