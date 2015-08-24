package org.nearbytalk.exception;

public class DataStoreException extends NearByTalkException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public DataStoreException(Throwable ex){
		super(ex);
	}
	
	public DataStoreException(){}
	
	public DataStoreException(String error){
		super(error);
	}

}
