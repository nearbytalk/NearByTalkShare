package org.nearbytalk.runtime;

import java.text.ParseException;

import org.nearbytalk.datastore.IDataStore;
import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.exception.DataStoreException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.BaseUserInfo;

import com.almworks.sqlite4java.SQLiteBusyException;

public class PassthroughMessageCache implements IMessageCache{
	
	private IDataStore dataStore;

	public PassthroughMessageCache(IDataStore dataStore) {
		this.dataStore = dataStore;
	}

	@Override
	public AbstractMessage get(String idBytes) throws DataStoreException,
			InterruptedException, BadReferenceException {

		try {
			return dataStore.loadWithDependency(idBytes);
		} catch (SQLiteBusyException e) {
			throw new DataStoreException(e);
		} catch (ParseException e) {
			throw new DataStoreException(e);
		}
	}

	@Override
	public void judgeMessage(String idBytes, boolean positive) {
		
		try {
			dataStore.judgeMessage(idBytes, positive);
		} catch (SQLiteBusyException e) {
			e.printStackTrace();
		}
	}
	
	

	@Override
	public AbstractMessage save(AbstractMessage message){
		
		try {
			dataStore.saveMessage(message);
			return message;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void releaseMessage(AbstractMessage message) {
		
		//do nothing
	}


	@Override
	public IDataStore getDataStore() {
		return dataStore;
	}


	@Override
	public boolean deleteMessage(BaseUserInfo user, String idBytes) {
		return dataStore.delete(user, idBytes, false);
	}

	@Override
	public void pushLatestMessages(NewMessageListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
		//do nothing	
	}

}
