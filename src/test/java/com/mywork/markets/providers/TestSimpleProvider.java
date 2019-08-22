package com.mywork.markets.providers;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import com.mywork.markets.DefaultMarketUpdate;
import com.mywork.markets.Instruments;
import com.mywork.markets.MarketUpdate;
import com.mywork.markets.Markets;
import com.mywork.markets.Side;
import com.mywork.markets.Tick;
import com.mywork.markets.vwap.Vwapper;

public class TestSimpleProvider {

	private SimpleProvider provider;
	private Vwapper vwapper;
	
	@Before
	public void setup() {
		provider = new SimpleProvider();
		vwapper = spy(new Vwapper(provider));
	}

	@Test
	public void testSubscriptionCount() {
		vwapper.start();
		checkSubscriptionCounts(1);
	}

	@Test
	public void testUpdateDelivery(){
		vwapper.start();
		MarketUpdate update = createTestUpdate(Markets.MARKET0, Instruments.GBPUSD,
												1.1, 100.0, 1.2, 200.0);
		provider.accept(update);
		
		verify(vwapper, times(1)).apply(update);
	}

	@Test
	public void testTwoUpdates(){
		
		vwapper = spy(new Vwapper(provider));
		vwapper.start();
		MarketUpdate update1 = createTestUpdate(Markets.MARKET0, Instruments.GBPUSD,
												1.1, 100.0, 1.2, 200.0);
		
		MarketUpdate update2 = createTestUpdate(Markets.MARKET1, Instruments.GBPUSD,
				1.1, 100.0, 1.2, 200.0);

		
		provider.accept(update1);
		
		provider.accept(update2);
		verify(vwapper, times(2)).apply(any(MarketUpdate.class));	
	}
	
	@Test
	public void testUnsubscribe(){
		
		vwapper = spy(new Vwapper(provider));
		vwapper.start();
		MarketUpdate update = createTestUpdate(Markets.MARKET0, Instruments.GBPUSD,
												1.1, 100.0, 1.2, 200.0);
		provider.accept(update);
		
		vwapper.stop();
	
		provider.accept(update);
		verify(vwapper, times(1)).apply(update);	
	}
	
	private void checkSubscriptionCounts(int count) {

		Instruments [] allInstruments = Instruments.values();
		Markets [] allMarkets = Markets.values();

		for(int i = 0; i < Instruments.SIZE; i++){
			for(int j = 0; j < Markets.SIZE; j++) {
				System.out.println(allInstruments[i] + "," + allMarkets[j]);
				System.out.println(allInstruments[i].ordinal() + "," + allMarkets[j].ordinal());
				assertEquals(count, provider.getNumberOfSubscribers(allInstruments[i], allMarkets[j]));
			}
		}
		
	}
	
	public MarketUpdate createTestUpdate(Markets market, Instruments ins,
			double bidPrice, double bidAmt, double offPrice, double offAmt) {
		
		return new DefaultMarketUpdate(market, 
					new Tick (ins, Side.BID, 
								bidPrice,
								bidAmt,
								offPrice,
								offAmt
							)
		);
}
	
	
}
