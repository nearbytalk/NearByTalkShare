package org.nearbytalk.test.runtime;

import org.nearbytalk.datastore.IDataStore;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.runtime.IMessageCache;
import org.nearbytalk.runtime.PassthroughMessageCache;

public class PassthroughMessageCacheTest extends MessageCacheSQLiteDataStoreTest{

	@Override
	protected IMessageCache messageCacheImpl(IDataStore dataStore,
			int preloadSize) {
		return new PassthroughMessageCache(dataStore);
	}

	@Override
	protected void checkSame(AbstractMessage lhs, AbstractMessage rhs) {
		assertEquals(lhs, rhs);
	}

}
