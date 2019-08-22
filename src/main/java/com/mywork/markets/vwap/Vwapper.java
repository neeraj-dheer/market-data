/**
 * 
 */
package com.mywork.markets.vwap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mywork.markets.Instruments;
import com.mywork.markets.MarketUpdate;
import com.mywork.markets.Markets;
import com.mywork.markets.Tick;
import com.mywork.markets.providers.Provider;
import com.mywork.markets.providers.SubscriptionException;
import com.mywork.markets.services.Lifecycle;


/**This class computes a running vwap of instruments from different markets
 * Assumptions:
 * 1. VWAP is calculated on a per instrument per market basis. 
 * 2. Side is ignored in the calculations at this point.
 * 3. A known number of instruments/markets (as is the case typically in FX) will be used.
 *   Hence the use of Instruments and Markets Enums - and an array to hold VWAPs. 
 *   This can be changed to an EnumMap or HashMap(Instrument as key) of HashMaps(Market as key)
 * 4. The Function functional interface has been explicitly implemented to show that each update 
 *   can be run through multiple functions where each can implement a standard interface.
 *   
 *This class is NOT thread-safe intentionally. Prices per instrument are independent and so for scalability,
 *multiple instances on this class can be created, each calculating for a specific set of instruments.
 *The class can be made thread-safe by storing the internal state - the current running vwap of instruments in a
 *thread-safe data structure like a ConcurrentHashMap if required.
 */
public class Vwapper implements Function<MarketUpdate, VwapMarketUpdate>, Lifecycle {

	private final Logger LOG = LoggerFactory.getLogger(Vwapper.class);	
	
	//stores state of this class - current running vwap per instrument
	//not thread-safe
	private final VwapEntry vwaps[][];
	
	private final Provider provider;
	
	private final Map<Instruments, List<Markets>> allSubscriptions = new HashMap<>();
	
	public Vwapper(Provider provider) {
		//initialize entire array to prevent null/exists checks when getting updates
		vwaps = new VwapEntry[Instruments.SIZE][Markets.SIZE];
		for(int i = 0; i < Instruments.SIZE; i++){
			for(int j = 0; j < Markets.SIZE; j++) {
				vwaps[i][j] = new VwapEntry();
			}
		}
		this.provider = provider;
	}

	/**
	 * start the Vwapper service
	 * subscribe to MD from all markets
	 */
	public void start() {
		Instruments [] allInstruments = Instruments.values();
		Markets [] allMarkets = Markets.values();
		
		for(int i = 0; i < allInstruments.length; i++){
			for(int j = 0; j < allMarkets.length; j++) {
				try {
					LOG.info("Subscribing to [%] for [%]", allInstruments[i], allMarkets[j]);
					provider.subscribe(allInstruments[i], allMarkets[j], this);
					final Markets currMarket = allMarkets[j];
					allSubscriptions.compute(allInstruments[i], ((Instruments key, List<Markets> value) -> { 
														if(null == value) {
															List<Markets> list = new ArrayList<>();
															list.add(currMarket);
															return list;
														} else {
															value.add(currMarket);
															return value;
														}}));
				//catch subscription exception only. other exceptions can be propagated up	
				}catch(SubscriptionException sExc) {
					//log exception and continue
					//one specific subscription failed - maybe the provider does not provide MD for a specific instrument/market
					LOG.warn("Could not subscribe to [%s] for [%s]", allInstruments[i], allMarkets[i]);
				}
			}
		}
		
	}

	/**
	 * stop the Vwapper service
	 * unsubscribe from MD from all markets
	 */
	public void stop() {

		//using streams here since this would ideally happen once when the 
		//application is shutting down. else would use old style iteration
		allSubscriptions.forEach((Instruments key, List<Markets> markets) -> {
				markets.forEach(market -> {
				try {
					LOG.info("Unsubscribing from [%] for [%]", key, market);
					provider.unsubscribe(key, market, this);
					
				//catch subscription exception only. other exceptions can be propagated up
				}catch(SubscriptionException sExc) { 
					//log exception and continue
					LOG.warn("Could not unsubscribe from [%s] for [%s]", key, market);
				}
			});
		});
	}
	
	/**
	 * Calculates the VWAP from given update and returns the vwap-ed update for the instrument/market pair
	 * As of now, creates and returns a new object every time - but typically can return a pooled object
	 * or update a shared object between this class and the subscriber.
	 * 
	 * @param update - the update to process
	 * @return the vwap-ed update for this instrument-market pair. 
	 *        This class returns instance of {@link VwapMarketUpdate VwapMarketUpdate} to indicate update is vwap-ed
	 *@throws IllegalArgumentException if the update, market, tick or instrument is null
	 */
	public VwapMarketUpdate apply(MarketUpdate update) {

		if(null == update) {
			throw new IllegalArgumentException("Cannot process null market update");
		}
		
		if(null == update.getMarket()) {
			throw new IllegalArgumentException("Cannot process update with null market");
		}
		
		if(null == update.getTick()) {
			throw new IllegalArgumentException("Cannot process update with null tick");
		}

		if(null == update.getTick().getInstrument()) {
			throw new IllegalArgumentException("Cannot process update with null instrument");
		}
		
		int instrumentIndex	= update.getTick().getInstrument().ordinal();
		int marketIndex 	= update.getMarket().ordinal();

		VwapEntry entry 	 = vwaps[instrumentIndex][marketIndex];
		
		//checking to take care of one-sided ticks - checking both amount and price to ensure validity.
		if((update.getTick().getBidAmount()) > 0.0 && (update.getTick().getBidPrice() > 0.0) ) {
			entry.setBidTotal(entry.getBidTotal() + (update.getTick().getBidAmount() * update.getTick().getBidPrice()));
			entry.setBidAmount(entry.getBidAmount() + update.getTick().getBidAmount()); 
		}
		if((update.getTick().getOfferAmount()) > 0.0 && (update.getTick().getOfferPrice() > 0.0) ) {
			entry.setOfferTotal(entry.getOfferTotal()+ (update.getTick().getOfferAmount() * update.getTick().getOfferPrice()));
			entry.setOfferAmount(entry.getOfferAmount() + update.getTick().getOfferAmount()); 
		}
		
		//returning a new update each time for now - can be changed to reusing shared/pooled objects
		VwapMarketUpdate vwapUpdate = new VwapMarketUpdate(update.getMarket(), 
											new Tick (	update.getTick().getInstrument(),
														update.getTick().getSide(),
														computeVwap(entry.getBidTotal(), entry.getBidAmount()),
														entry.getBidAmount(),
														computeVwap(entry.getOfferTotal(), entry.getOfferAmount()),
														entry.getOfferAmount()
													));
		
		//do whatever work is required with the vwap here.
		System.out.println(vwapUpdate);
		return vwapUpdate;
	}
	
	/**
	 * calculate the vwap.
	 * @param total total PV
	 * @param amount total amount
	 * @return vwap price
	 */
	protected double computeVwap(double total, double amount) {
		if(amount == 0.0) {
			return 0;
		}

		return total/amount;
	}

	/**
	 * Holder for running VWAP totals.
	 * In this case, since each entry is associated with the vwap array,
	 * we dont need to store additional meta-data like markets, instruments in this class. 
	 */
	class VwapEntry {
		private double bidTotal;
		private double bidAmount;
		private double offerTotal;
		private double offerAmount;
		
		public double getBidTotal() {
			return bidTotal;
		}
		public void setBidTotal(double bidTotal) {
			this.bidTotal = bidTotal;
		}
		public double getBidAmount() {
			return bidAmount;
		}
		public void setBidAmount(double bidAmount) {
			this.bidAmount = bidAmount;
		}
		public double getOfferTotal() {
			return offerTotal;
		}
		public void setOfferTotal(double offerTotal) {
			this.offerTotal = offerTotal;
		}
		public double getOfferAmount() {
			return offerAmount;
		}
		public void setOfferAmount(double offerAmount) {
			this.offerAmount = offerAmount;
		}

	}

}
