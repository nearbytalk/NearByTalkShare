package org.nearbytalk.identity;


public interface Identifiable {

	String getIdBytes();

	/**
	 * 
	 * if a object needs multi field set and then do a full digest
	 * this function should be called for the final object
	 */
	void digestId();

}
