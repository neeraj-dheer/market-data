/**
 * 
 */
package com.mywork.markets.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.mywork.markets.DefaultMarketUpdate;
import com.mywork.markets.Instruments;
import com.mywork.markets.MarketUpdate;
import com.mywork.markets.Markets;
import com.mywork.markets.utils.ExecutorUtils;
import com.mywork.markets.utils.UpdatesProvider;

/**
 * Provider uses a disruptor to hand-off updates to subscribers.
 * Uses one disruptor/hand-off queue per subscriber based on the following:
 * 1. Number of subscribers is expected to be low.
 * 2. Having one hand-off queue per subscriber enables the queue to be customized per subscriber,
 * ie, queue depth, if conflation is required etc can be controlled on a per subscriber basis.
 * 3. Each subscriber only gets the data it needs and doesnt need to do any filtering etc.
 * 
 * The other alternative would be to have just one disruptor where each subscriber is a consumer off 
 * that one disruptor.
 * 
 *  Note: This class has NOT been tested - and so might have bugs in it. This is just to outline
 *  how a producer-consumer hand-off based implementation using a disruptor will look like.
 */
public class MultiThreadProvider implements ProviderWithLifecycle, Consumer<MarketUpdate> {

//	private Map<Instruments, Map<Markets List<Function>> subscribers;

	private List<Function<? extends MarketUpdate,? extends MarketUpdate>> [][] subscribers = new ArrayList[Instruments.SIZE][Markets.SIZE];
	
	//a HashMap is fine here - since synchronization is achieved by the Lock object.
	private final Map<Function<? extends MarketUpdate, ? extends MarketUpdate>, 
						Disruptor<? extends MarketUpdate>> subscriberQueues = new HashMap<>();
	
	private final UpdatesProvider provider;
	
	private final ExecutorService executor = ExecutorUtils.singleThreadExecutor();
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	public MultiThreadProvider() {

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
			
			Disruptor<? extends MarketUpdate> disruptor = subscriberQueues.get(consumer);
			if(null == disruptor) {
				disruptor = createSubscriberDisruptor(consumer);
				disruptor.start();
				subscriberQueues.put(consumer, disruptor);
			}

		
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
	public void accept(final MarketUpdate update) {
		
		try{
			lock.readLock().lock();
	
			List<Function<? extends MarketUpdate, ? extends MarketUpdate>> subs = 
								subscribers[update.getTick().getInstrument().ordinal()][update.getMarket().ordinal()];
			if(null == subs) {
				return;
			}
	
			for(int i = 0; i < subs.size(); i++){
				Function<? extends MarketUpdate, ? extends MarketUpdate> consumer = subs.get(i);
				Disruptor<? extends MarketUpdate> disruptor = subscriberQueues.get(consumer);
				if(null == disruptor) {
					System.err.println("Could not find queue for consumer - " + consumer);
				}else {
					disruptor.getRingBuffer().publishEvent( (event, seq) -> 
											{
												event.setMarket(update.getMarket());
												event.setTick(update.getTick());
											});
				
				}
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
		
		//sync this
		subscriberQueues.forEach((consumer, disruptor) -> disruptor.halt());
		
	}
	
	/**
	 * Using the following:
	 * 1. disruptor size of 1024 - keeping it to 1k to try and keep size of disruptor 
	 * to L3 cache atleast
	 * 2. Using BlockingWaitStrategy because of laptop configuration. 
	 * Would use BusySpinStrategy or YieldingWaitStrategy or similar in real-life
	 * 3. Using DaemonThreadFactory here - would typically use a different pool
	 * (or ThreadFactory) that returns pinned threads
	 */
	private <T extends MarketUpdate, U extends MarketUpdate> Disruptor<? extends MarketUpdate> 
										createSubscriberDisruptor(final Function<T, U> consumer) {
	
		Disruptor<T> disruptor = new Disruptor<>(new DefaultMarketUpdateFactory(), 1024, DaemonThreadFactory.INSTANCE,
				ProducerType.SINGLE, new BlockingWaitStrategy());
		disruptor.handleEventsWith( (event, sequence, batch) -> consumer.apply(event));
		
		return disruptor;
	}

	
	private class DefaultMarketUpdateFactory<T extends MarketUpdate> implements EventFactory<DefaultMarketUpdate> {

		@Override
		public DefaultMarketUpdate newInstance() {
			return new DefaultMarketUpdate();
		}
		
	}
}
