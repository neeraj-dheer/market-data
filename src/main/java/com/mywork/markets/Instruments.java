package com.mywork.markets;

public enum Instruments {

	GBPUSD,
	EURUSD,
	EURSEK,
	USDJPY;
	
	public static final int SIZE = Instruments.values().length;
	
	private static final Instruments[] instruments = Instruments.values();
	
	public static Instruments getInstrumentById(int index) {
		//will throw ArrayIndexOutOfBounds if index < 0 or index > instruments.length
		return instruments[index];
	}
}
