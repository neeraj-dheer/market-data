package com.mywork.markets.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Can beused to create different Executors/threads
 * with pinned or normal threads etc.
 *
 */
public class ExecutorUtils {
	
	public static ExecutorService singleThreadExecutor() {
		return Executors.newSingleThreadExecutor();
	}

}
