/**
 * 
 */
package com.mywork.markets.runners;

import com.mywork.markets.providers.ProviderWithLifecycle;
import com.mywork.markets.providers.SimpleProvider;
import com.mywork.markets.vwap.Vwapper;

/**
 *Bootstrapper for the MD system
 *
 */
public class ProviderRunner {

	private volatile ProviderWithLifecycle provider;
	private volatile Vwapper vwapper;
	
	public ProviderRunner(ProviderWithLifecycle provider) {
		this.provider = provider;
		
	}
	public void run() {
		
		vwapper = new Vwapper(provider);
		vwapper.start();
		provider.start();
	}
	
	public void stop() {
		vwapper.stop();
		provider.stop();

	}
	
	public static void main(String[] args) throws InterruptedException {

//uncomment the line with whichever Provider needs to be tested.
//if not auto-imported, please import the respective provider class.
// no other changes sbhould be required to run either.
		
		ProviderRunner runner = new ProviderRunner(new SimpleProvider());
//		ProviderRunner runner = new ProviderRunner(new MultiThreadProvider());
		runner.run();
		Thread.sleep(10_000); //run for about 10s then stop
		runner.stop();
		
	}

}
