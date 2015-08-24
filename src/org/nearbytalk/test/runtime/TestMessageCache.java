package org.nearbytalk.test.runtime;

import java.text.ParseException;

import org.nearbytalk.datastore.IDataStore;
import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.exception.DataStoreException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.BaseUserInfo;
import org.nearbytalk.runtime.IMessageCache;
import org.nearbytalk.runtime.NewMessageListener;

import com.almworks.sqlite4java.SQLiteBusyException;

public class TestMessageCache implements IMessageCache{
	
	private IDataStore dataStore;

	public TestMessageCache(IDataStore dataStore){
		this.dataStore=dataStore;
	}

	@Override
	public AbstractMessage get(String idBytes) throws DataStoreException,
			InterruptedException, BadReferenceException {
		try {
			return dataStore.loadWithDependency(idBytes);
		} catch (SQLiteBusyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void judgeMessage(String idBytes, boolean positive) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AbstractMessage save(AbstractMessage message)
			throws DataStoreException, InterruptedException,
			BadReferenceException {
		try {
			dataStore.saveMessage(message);
			return message;
		} catch (Exception e) {
			throw new DataStoreException(e);
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void pushLatestMessages(NewMessageListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

}
