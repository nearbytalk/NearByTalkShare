package org.nearbytalk.query;

public enum SearchType {
/**
		 * if query by idBytes(must be valid idBytes), use EXACTLY
		 * 
		 */
		EXACTLY, BY_RATE,
		/**
		 * if query by text contents,use FUZZY
		 * 
		 */
		FUZZY,
		TOPIC_EXACTLY,
		TOPIC_FUZZY,
		/**
		 *message sent by user  
		 */
		BY_USER
}
