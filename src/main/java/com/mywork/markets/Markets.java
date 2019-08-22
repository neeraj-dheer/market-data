package com.mywork.markets;

public enum Markets {
	MARKET0,
	MARKET1,
	MARKET2,
	MARKET3;
	// ...
	
	public static final int SIZE = Markets.values().length;
	
	private static final Markets[] markets = Markets.values();
	
	public static Markets getMarketById(int index) {
		//will throw ArrayIndexOutOfBounds if index < 0 or index > markets.length
		return markets[index]; 
	}
	
}
