package com.mywork.markets;

/**
 * Event used by a disruptor, so making it mutable
 */
public interface MarketUpdate {
	Markets 	getMarket();
	Tick getTick();
	void setMarket(Markets market);
	void setTick(Tick tick);
}
