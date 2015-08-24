package org.nearbytalk.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

import org.nearbytalk.datastore.IDataStore;
import org.nearbytalk.datastore.SQLiteDataStore;
import org.nearbytalk.exception.NearByTalkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * a singleton object holds most global variable,
 * DataStore,and related cache,queue. for simple use
 * it do not throw Exception in constructor,instead 
 * getInitException gives first exception. 
 * 
 */
public class UniqueObject {
	
	private Logger log=LoggerFactory.getLogger(UniqueObject.class);
	
	public final OnlineUserInfoMap onlineUserInfoMap=new OnlineUserInfoMap();
	
	private final IDataStore dataStore=new SQLiteDataStore();

	private IMessageCache messageCache;
	
	public final UserCache userCache=new UserCache(onlineUserInfoMap,dataStore);
	
	private final AtomicBoolean messageCacheCreated=new AtomicBoolean(false);
	
	private NearByTalkException initException;
	
	private MessageDispatcher messageDispatcher=new MessageDispatcher();
	
	private static UniqueObject instance=new UniqueObject();
	
	
	private UniqueObject() {
	}
	
	public static UniqueObject getInstance(){
		return instance;
	}

	public NearByTalkException getInitException() {
		return initException;
	}
	
	/**
	 * re init instance
	 * 
	 */
	public static void reset(){
		//TODO resource leak
		instance=new UniqueObject();
	}

	private Object messageCacheLock=new Object();

	public IMessageCache getMessageCache() {
		if (messageCache!=null) {
			return messageCache;
		}
		
		synchronized (messageCacheLock) {
			try {
				messageCache=new MemoryMessageCache(dataStore);
			} catch (Exception e) {
				initException=new NearByTalkException(e);
			}
		};
		
		return messageCache;
	}
	
	

	public IDataStore getDataStore() {
		return dataStore;
	}

	public MessageDispatcher getMessageDispatcher() {
		return messageDispatcher;
	}
	
	
}
