package com.mywork.markets.vwap;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import com.mywork.markets.DefaultMarketUpdate;
import com.mywork.markets.Instruments;
import com.mywork.markets.MarketUpdate;
import com.mywork.markets.Markets;
import com.mywork.markets.Side;
import com.mywork.markets.Tick;
import com.mywork.markets.providers.Provider;
import com.mywork.markets.providers.SubscriptionException;

public class TestVwapper {
	
	private Vwapper vwapper;
	private Provider provider;
	
	@Before
	public void setup() {
		provider = mock(Provider.class);
		vwapper = new Vwapper(provider);
	}

	@Test
	public void testVwapperStartMethod() throws SubscriptionException {
		doNothing().when(provider).subscribe(any(Instruments.class), any(Markets.class), 
				ArgumentMatchers.<Function<? extends MarketUpdate, ? extends MarketUpdate>>any());
		vwapper.start();
		verify(provider, times(Instruments.SIZE * Markets.SIZE)).subscribe(any(Instruments.class), any(Markets.class), 
				ArgumentMatchers.<Function<? extends MarketUpdate, ? extends MarketUpdate>>any());
	}

	@Test
	public void testVwapperStopMethod() throws SubscriptionException {
		doNothing().when(provider).subscribe(any(Instruments.class), any(Markets.class), 
				ArgumentMatchers.<Function<? extends MarketUpdate, ? extends MarketUpdate>>any());
		doNothing().when(provider).unsubscribe(any(Instruments.class), any(Markets.class), 
				ArgumentMatchers.<Function<? extends MarketUpdate, ? extends MarketUpdate>>any());

		vwapper.start();
		vwapper.stop();
		verify(provider, times(Instruments.SIZE * Markets.SIZE)).unsubscribe(any(Instruments.class), any(Markets.class), 
				ArgumentMatchers.<Function<? extends MarketUpdate, ? extends MarketUpdate>>any());
	}
	
	
	/**
	 * 
	 * Throw exception during a subscription.
	 * Unsubscribe should be called one time lesser to account for the exception
	 */
	@Test
	public void testExceptionInSubscription() throws SubscriptionException {
		
		doThrow(new SubscriptionException("Test Exception")).doNothing().when(provider).subscribe(any(Instruments.class), any(Markets.class), 
				ArgumentMatchers.<Function<? extends MarketUpdate, ? extends MarketUpdate>>any());
		doNothing().when(provider).unsubscribe(any(Instruments.class), any(Markets.class), 
				ArgumentMatchers.<Function<? extends MarketUpdate, ? extends MarketUpdate>>any());

		vwapper.start();

		verify(provider, times(Instruments.SIZE * Markets.SIZE)).subscribe(any(Instruments.class), any(Markets.class), 
				ArgumentMatchers.<Function<? extends MarketUpdate, ? extends MarketUpdate>>any());

		vwapper.stop();
		verify(provider, times((Instruments.SIZE * Markets.SIZE) - 1)).unsubscribe(any(Instruments.class), any(Markets.class), 
				ArgumentMatchers.<Function<? extends MarketUpdate, ? extends MarketUpdate>>any());
	}

	
	@Test
	public void testComputeVwap() {
		double vwapPrice = vwapper.computeVwap(110.0, 100.0);
		assertEquals(1.1, vwapPrice, 0.0);
	}

	@Test
	public void testComputeVwapZeroAmount() {
		double vwapPrice = vwapper.computeVwap(110.0, 0.0);
		assertEquals(0.0, vwapPrice, 0.0);
	}
	
	
	@Test
	public void testOneOneWayOfferZeroPriceUpdate() {

		MarketUpdate update = createTestUpdate(Markets.MARKET0, Instruments.GBPUSD,
												1.1, 100.0, 0.0, 200.0);
		MarketUpdate result = vwapper.apply(update);
		
		assertEqualMarketUpdates(result, Markets.MARKET0, Instruments.GBPUSD,
									1.1, 100.0, 0.0, 0.0);
		
	}

	
	@Test
	public void testOneOneWayBidZeroAmountUpdate() {
		
		MarketUpdate update = createTestUpdate(Markets.MARKET0, Instruments.GBPUSD,
												1.1, 0.0, 1.2, 200.0);
		MarketUpdate result = vwapper.apply(update);
		
		assertEqualMarketUpdates(result, Markets.MARKET0, Instruments.GBPUSD,
									0.0, 0.0, 1.2, 200.0);
		
	}
	
	
	@Test
	public void testOneTwoWayUpdate() {
		
		MarketUpdate update = createTestUpdate(Markets.MARKET0, Instruments.GBPUSD,
												1.1, 100.0, 1.2, 200.0);
		MarketUpdate result = vwapper.apply(update);
		
		assertEqualMarketUpdates(result, Markets.MARKET0, Instruments.GBPUSD,
									1.1, 100.0, 1.2, 200.0);
		
	}

	@Test
	public void testTwoTwoWayUpdate() {
		
		MarketUpdate update1 = createTestUpdate(Markets.MARKET0, Instruments.GBPUSD,
												1.1, 100.0, 1.2, 200.0);
		MarketUpdate update2 = createTestUpdate(Markets.MARKET0, Instruments.GBPUSD,
												1.2, 150.0, 1.3, 300.0);

		MarketUpdate result = vwapper.apply(update1);
		assertEqualMarketUpdates(result, Markets.MARKET0, Instruments.GBPUSD,
									1.1, 100.0, 1.2, 200.0);

		result = vwapper.apply(update2);
		
		assertEqualMarketUpdates(result, Markets.MARKET0, Instruments.GBPUSD,
									1.16, 250.0, 1.26, 500.0);
		
	}
	
	@Test
	public void testOneWayBidThenOneWayOffer() {
	
		MarketUpdate update1 = createTestUpdate(Markets.MARKET0, Instruments.GBPUSD,
												1.1, 100.0, 0.0, 200.0);
		
		MarketUpdate update2 = createTestUpdate(Markets.MARKET0, Instruments.GBPUSD,
												1.1, 0.0, 1.2, 200.0);
		
		MarketUpdate result = vwapper.apply(update1);

		assertEqualMarketUpdates(result, Markets.MARKET0, Instruments.GBPUSD,
									1.1, 100.0, 0.0, 0.0);


		result = vwapper.apply(update2);

		assertEqualMarketUpdates(result, Markets.MARKET0, Instruments.GBPUSD,
									1.1, 100.0, 1.2, 200.0);

		
	}

	@Test
	public void testTwoWayUpdateDifferentMarkets() {
		
		MarketUpdate update1 = createTestUpdate(Markets.MARKET0, Instruments.GBPUSD,
												1.1, 100.0, 1.2, 200.0);
		MarketUpdate update2 = createTestUpdate(Markets.MARKET1, Instruments.GBPUSD,
												1.2, 150.0, 1.3, 300.0);

		MarketUpdate result = vwapper.apply(update1);
		assertEqualMarketUpdates(result, Markets.MARKET0, Instruments.GBPUSD,
									1.1, 100.0, 1.2, 200.0);

		result = vwapper.apply(update2);
		
		assertEqualMarketUpdates(result, Markets.MARKET1, Instruments.GBPUSD,
									1.2, 150.0, 1.3, 300.0);
		
	}

	
	@Test(expected=IllegalArgumentException.class)
	public void testNullMarketUpdate() {
		vwapper.apply(null);
	}
	
	public MarketUpdate createTestUpdate(Markets market, Instruments ins,
										double bidPrice, double bidAmt, 
										double offPrice, double offAmt) {
		return new DefaultMarketUpdate(market, 
								new Tick (ins, Side.BID, 
										bidPrice,
										bidAmt,
										offPrice,
										offAmt
										)
									);
	}

	public void assertEqualMarketUpdates(MarketUpdate update, Markets market, 
										Instruments ins, double bidTot, 
										double bidAmt, double offTot, double offAmt) {
		assertEquals(market, update.getMarket());
		assertEquals(ins, update.getTick().getInstrument());
		assertEquals(offTot, update.getTick().getOfferPrice(), 0.0);
		assertEquals(offAmt, update.getTick().getOfferAmount(), 0.0);
		assertEquals(bidTot, update.getTick().getBidPrice(), 0.0);
		assertEquals(bidAmt, update.getTick().getBidAmount(), 0.0);
		
	}


}
