package com.mywork.markets;

public class DefaultMarketUpdate implements MarketUpdate{
	
	private Markets market;
	private Tick tick;
	
	public DefaultMarketUpdate() {}
	public DefaultMarketUpdate(final Markets market, final Tick tick) {
		this.market = market;
		this.tick = tick;
	}
	
	public Markets getMarket() {
		return market;
	}
	
	public Tick getTick() {
		return tick;
	}
	
	public String toString() {
		return "Market : [" + market + " Tick : [" + tick + "]";
	}
	@Override
	public void setMarket(Markets market) {
		this.market = market;
		
	}
	@Override
	public void setTick(Tick tick) {
		this.tick = tick;
		
	}
}
