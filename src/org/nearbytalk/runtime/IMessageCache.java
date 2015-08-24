package org.nearbytalk.runtime;

import org.nearbytalk.datastore.IDataStore;
import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.exception.DataStoreException;
import org.nearbytalk.exception.DuplicateMessageException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.BaseUserInfo;

public interface IMessageCache {

	public abstract AbstractMessage get(String idBytes)
			throws DataStoreException, InterruptedException,
			BadReferenceException;

	public void judgeMessage(String idBytes, boolean positive);

	public AbstractMessage save(AbstractMessage message)
			throws DataStoreException, InterruptedException,
			BadReferenceException, DuplicateMessageException;

	public void releaseMessage(AbstractMessage message);

	public IDataStore getDataStore();

	public boolean deleteMessage(BaseUserInfo user, String idBytes);
	
	public void pushLatestMessages(NewMessageListener listener);
	
	public void stop();
	
}
