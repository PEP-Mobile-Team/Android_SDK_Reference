/**
 * Copyright (C) 2003-2019, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.utils.thread;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AppLatch {
	private CountDownLatch mCountDownLatch;

	public AppLatch() {
		mCountDownLatch = new CountDownLatch(1);
	}

	public AppLatch(int count) {
		mCountDownLatch = new CountDownLatch(count);
	}
	
	public void setup(int count) {
		mCountDownLatch = new CountDownLatch(count);
	}
	
	public void countDown() {
		mCountDownLatch.countDown();
	}
	
	public void await() {
		try {
			mCountDownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void await(int timeout) {
		try {
			mCountDownLatch.await(timeout, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
