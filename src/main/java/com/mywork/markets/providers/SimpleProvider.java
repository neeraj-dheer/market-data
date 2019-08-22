/**
 * 
 */
package com.mywork.markets.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mywork.markets.Instruments;
import com.mywork.markets.MarketUpdate;
import com.mywork.markets.Markets;
import com.mywork.markets.utils.ExecutorUtils;
import com.mywork.markets.utils.UpdatesProvider;

/**
 * Simple provider to test end-to-end functionality.
 * This provider calls its MD subscribers in its own thread (potentially pinned to a core)
 * and so it is expected that the subscribers do not hold up the thread.
 */
public class SimpleProvider implements ProviderWithLifecycle, Consumer<MarketUpdate> {

	private List<Function> [][] subscribers = new ArrayList[Instruments.SIZE][Markets.SIZE];
	
	private volatile UpdatesProvider provider;
	
	private final ExecutorService executor = ExecutorUtils.singleThreadExecutor();
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	public SimpleProvider() {

		for(int i = 0; i < Instruments.SIZE; i++){
			for(int j = 0; j < Markets.SIZE; j++) {
				subscribers[i][j] = new ArrayList<>();
			}
		}
		
		provider = new UpdatesProvider(this);

	}

	@Override
	public void subscribe(Instruments instrument, Markets market,
			Function<? extends MarketUpdate, ? extends MarketUpdate> consumer) throws SubscriptionException {
		try{
			lock.writeLock().lock();
		
			if(!subscribers[instrument.ordinal()][market.ordinal()].add(consumer)) {
				throw new SubscriptionException(String.format("Could not register subscription for [%s] for [%s]",
															instrument, market));
			}
		}finally {
			lock.writeLock().unlock();
		}
		
	}

	@Override
	public void unsubscribe(Instruments instrument, Markets market,
			Function<? extends MarketUpdate, ? extends MarketUpdate> consumer) throws SubscriptionException {
		try{
			lock.writeLock().lock();
	
			if(!subscribers[instrument.ordinal()][market.ordinal()].remove(consumer)) {
				throw new SubscriptionException(String.format("Could not unregister subscription for [%s] for [%s]",
															instrument, market));
			}
		}finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void accept(MarketUpdate update) {
		
		try{
			lock.readLock().lock();
	
			List<Function> subs = subscribers[update.getTick().getInstrument().ordinal()][update.getMarket().ordinal()];
			if(null == subs) {
				return;
			}
	
			for(int i = 0; i < subs.size(); i++){
				subs.get(i).apply(update);
			}
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public int getNumberOfSubscribers(Instruments ins, Markets market) {

		int size = 0;
		try {
			lock.readLock().lock();
			size = subscribers[ins.ordinal()][market.ordinal()].size();
		} finally {
			lock.readLock().unlock();
		}
		
		return size;
	}

	@Override
	public void start() {
		executor.execute(provider::run);
	}

	@Override
	public void stop() {
		
		//our updates provider tests for the interrupted flag 
		//so it will respond to the interruption sent to it by 
		//executor.shutdownNow and shut itself down.
		executor.shutdownNow();
		try {
			if(!executor.awaitTermination(1, TimeUnit.SECONDS)) {
				System.err.println("UpdatesProvider did not shut down");
			}
		} catch (InterruptedException e) {
			System.err.println("UpdatesProvider did not shut down");
		}
		
	}
}
