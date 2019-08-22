package com.mywork.markets.vwap;

import com.mywork.markets.DefaultMarketUpdate;
import com.mywork.markets.Markets;
import com.mywork.markets.Tick;

/**
 * Marker class to indicate that this update is a VWAP price.
 * Should ideally implement the interface {@link MarketUpdate MarketUpdate}
 * but has just extended {@link DefaultMarketUpdate DefaultMarketUpdate}
 * for simplicity.
 */
public class VwapMarketUpdate extends DefaultMarketUpdate{

	public VwapMarketUpdate(Markets market, Tick tick) {
		super(market, tick);
	}

}
