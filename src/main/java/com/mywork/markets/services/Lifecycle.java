/**
 * 
 */
package com.mywork.markets.services;

/**
 * Interface to indicate implementing class is lifecycle-aware
 * and hence start/stop cna be called on it.
 */
public interface Lifecycle {

	public void start();
	public void stop();
}
