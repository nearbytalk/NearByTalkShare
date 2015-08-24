package org.nearbytalk.query;

public class PollQuery extends AbstractQuery{
	
	public static enum PollType{
		/**
		 *polling in lazy ,will wait long time for batch notify 
		 */
		LAZY,
		/**
		 * 
		 * like IM talk,any new message will flush immediately
		 */
		EAGER,
		/**
		 *force refresh cached message to client immediately 
		 */
		REFRESH,
		
		/**
		 *query messages currently in unsavedMessageQueue.
		 *if client refresh page , any js value is cleared,
		 *and this type is useful 
		 */
		HISTORY
	}
	
	public PollType pollType;

	@Override
	public String toString() {
		return pollType.toString();
	}

	
}
