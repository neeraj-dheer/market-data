package com.mywork.markets;

public enum Side {
	BID,
	OFFER;
	
	public static final int SIZE = Side.values().length;
	
	private static final Side[] sides = Side.values();
	
	public static Side getSideById(int index) {
		//will throw ArrayIndexOutOfBounds if index < 0 or index > instruments.length
		return sides[index];
	}
	
}
