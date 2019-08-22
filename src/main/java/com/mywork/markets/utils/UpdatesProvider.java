package com.mywork.markets.utils;

import java.util.Random;
import java.util.function.Consumer;

import com.mywork.markets.DefaultMarketUpdate;
import com.mywork.markets.Instruments;
import com.mywork.markets.MarketUpdate;
import com.mywork.markets.Markets;
import com.mywork.markets.Side;
import com.mywork.markets.Tick;

/**
 * Sample implementation of how this provider
 * can be plugged to a real MD provider.
 * This class creates updates with random values and sleeps 
 * randomly after every few updates to simulate real conditions.
 * The actual values of the ticks will not make commercial sense.
 *
 */
public class UpdatesProvider {
	
	private volatile boolean stop = false;
	
	private Random random = new Random();
	
	private final Consumer<MarketUpdate> consumer;
	
	public UpdatesProvider(Consumer<MarketUpdate> consumer){
		this.consumer = consumer;
	}
	
	public void stop(){
		stop = true;
	}
	
	//send random updates periodically
	public void run() {
		
		while(!stop && !Thread.currentThread().isInterrupted()) {
			try{
				int burst = random.nextInt(1000);
				//will send burst before checking if we have to stop
				//rather than checking after every update. will also test if we can 
				//handle data while shutting down
				for(int i = 0; i < burst; i++) {
					consumer.accept(createRandomUpdate());
				}
				Thread.sleep(random.nextInt(100));
			}catch(InterruptedException iex){
				System.err.println("Update thread interrupted, quitting.");
				Thread.currentThread().interrupt();
			}catch(Exception ex){
				System.err.println("Caught exception, quitting - " + ex);
				stop = true;
			}
		}
		System.out.println("Updates Provider shut down");
		
	}
	

	public MarketUpdate createRandomUpdate() {
		return new DefaultMarketUpdate(Markets.getMarketById(random.nextInt(Markets.SIZE-1)), 
										new Tick(Instruments.getInstrumentById(random.nextInt(Instruments.SIZE-1)), 
												Side.getSideById(random.nextInt(Side.SIZE)), 
												random.nextDouble(), random.nextDouble() * 1_00_000, 
												random.nextDouble(), random.nextDouble() * 1_00_000));
	}
}
