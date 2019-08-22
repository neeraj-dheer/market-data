/**
 * 
 */
package com.mywork.markets.providers;

/**
 * Exception thrown when subscribe/unsubscribe fails
 *
 */
public class SubscriptionException extends Exception {

	private static final long serialVersionUID = 3942571677946836660L;

	public SubscriptionException() {
	}

	/**
	 * @param message exception message
	 */
	public SubscriptionException(String message) {
		super(message);

	}

	/**
	 * @param cause underlying cause of this exception
	 */
	public SubscriptionException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SubscriptionException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public SubscriptionException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
