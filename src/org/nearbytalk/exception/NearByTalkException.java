package org.nearbytalk.exception;

public class NearByTalkException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NearByTalkException() {
	}

	public NearByTalkException(String str) {
		super(str);
	}
	
	public NearByTalkException(Throwable ex){
		super(ex);
	}
}
