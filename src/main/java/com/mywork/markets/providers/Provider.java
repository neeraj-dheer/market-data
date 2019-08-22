package com.mywork.markets.providers;

import java.util.function.Function;

import com.mywork.markets.Instruments;
import com.mywork.markets.MarketUpdate;
import com.mywork.markets.Markets;

/**
 * Market Data Provider interface.
 * This interface provides methods for MD consumers to subscribe/unsubscribe
 * for dkifferent instruments and markets.
 * This interface imposes no threading/delivery restrictions on the actual provider.
 * No lifecycle methods for the actual provider are defined but it is expected that
 * the provider will shut-down cleanly after notifying consumers of shutdown - possibly 
 * unsubscribing all consumers first. 
 */
public interface Provider {
	
	/**
	 * subscribe to one instrument from one market
	 * @param instrument - the instrument to subscribe to
	 * @param market - the market for which this instrument is being subscribed
	 * @param consumer - the callback for this subscription
	 * @throws NullPointerException if any of the parameters are null
	 */
	void subscribe(Instruments instrument, Markets market, 
			Function<? extends MarketUpdate, ? extends MarketUpdate> consumer) throws SubscriptionException;

	/**
	 * unsubscribe from one instrument from one market
	 * @param instrument - the instrument to unsubscribe from
	 * @param market - the market for which this instrument is being unsubscribed
	 * @param consumer - the callback that needs to unsubscribe
	 * @throws NullPointerException if any of the parameters are null
	 */
	void unsubscribe(Instruments instrument, Markets market, 
			Function<? extends MarketUpdate, ? extends MarketUpdate> consumer) throws SubscriptionException;

}
