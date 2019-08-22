/**
 * 
 */
package com.mywork.markets.providers;

import com.mywork.markets.services.Lifecycle;

/**
 * interface to be used for providers that also use Lifecycle methods
 *
 */
public interface ProviderWithLifecycle extends Provider, Lifecycle {

}
