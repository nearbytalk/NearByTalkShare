package org.nearbytalk.test.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.nearbytalk.datastore.IDataStore;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.runtime.IMessageCache;
import org.nearbytalk.runtime.MemoryMessageCache;
import org.nearbytalk.runtime.SlotLock;
import org.nearbytalk.runtime.UnsavedMessageQueue;
import org.nearbytalk.runtime.MemoryMessageCache.AccessRecord;
import org.nearbytalk.runtime.MemoryMessageCache.StateConsistChecker;
import org.nearbytalk.test.TestUtil;


public class MemoryMessageCacheTest extends MessageCacheSQLiteDataStoreTest{

	@Override
	protected IMessageCache messageCacheImpl(IDataStore dataStore,
			int preloadSize) {
		try {
			return new MemoryMessageCache(dataStore);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected void checkSame(AbstractMessage lhs, AbstractMessage rhs) {
		assertSame(lhs, rhs);
		
	}
	
	public static final StateConsistChecker BackRefNoneZeroNotInLRUChecker=new StateConsistChecker() {
		
		@Override
		public void check(ConcurrentHashMap<String, SlotLock> slotLocks,
				ConcurrentHashMap<String, AccessRecord> cache,
				TreeMap<Long, String> lRUList,
				UnsavedMessageQueue unsavedMessageQueue) {
			
			for(String idBytes:cache.keySet()){
				AccessRecord record=cache.get(idBytes);
				
				if(record.backReference.get()>0){
					assertTrue(!lRUList.containsKey(record.serial));
				}
			}
		}
	};
	
	public static final StateConsistChecker LRUBackRefAlwaysZeroChecker=new StateConsistChecker() {
		
		@Override
		public void check(ConcurrentHashMap<String, SlotLock> slotLocks,
				ConcurrentHashMap<String, AccessRecord> cache,
				TreeMap<Long, String> lRUList,
				UnsavedMessageQueue unsavedMessageQueue) {
			
			for (Long serial: lRUList.keySet()) {
				
				assertEquals(0,cache.get(lRUList.get(serial)).backReference.get());
			}
		}
	};
	
	public static final StateConsistChecker LRUSerialAllInCacheChecker=new StateConsistChecker() {
		
		@Override
		public void check(ConcurrentHashMap<String, SlotLock> slotLocks,
				ConcurrentHashMap<String, AccessRecord> cache,
				TreeMap<Long, String> lRUList,
				UnsavedMessageQueue unsavedMessageQueue) {
			
			for (Long serial: lRUList.keySet()) {
				assertTrue(cache.containsKey(lRUList.get(serial)));
			}
		}
	};
	
	public static final StateConsistChecker LRUNoDuplicateIdBytesChecker=new StateConsistChecker() {
		
		@Override
		public void check(ConcurrentHashMap<String, SlotLock> slotLocks,
				ConcurrentHashMap<String, AccessRecord> cache,
				TreeMap<Long, String> lRUList,
				UnsavedMessageQueue unsavedMessageQueue) {
			// LRU has no duplicate entry

			HashSet<String> dedup = new HashSet<String>();

			for (Long thisSerial : lRUList.keySet()) {

				String canBeRecycled= lRUList.get(thisSerial);
				
				assertTrue(!dedup.contains(canBeRecycled));

			}
		}
	};
	
	public static final StateConsistChecker ALL_CHECKER = new StateConsistChecker() {
		
		@Override
		public void check(ConcurrentHashMap<String, SlotLock> slotLocks,
				ConcurrentHashMap<String, AccessRecord> cache,
				TreeMap<Long, String> lRUList,
				UnsavedMessageQueue unsavedMessageQueue) {
			
			LRUBackRefAlwaysZeroChecker.check(slotLocks, cache, lRUList, unsavedMessageQueue);
			
			LRUNoDuplicateIdBytesChecker.check(slotLocks, cache, lRUList, unsavedMessageQueue);
			
			BackRefNoneZeroNotInLRUChecker.check(slotLocks, cache, lRUList, unsavedMessageQueue);
			
			LRUSerialAllInCacheChecker.check(slotLocks, cache, lRUList, unsavedMessageQueue);
			
		}
	};
public void testPreloadSimple2Ref() throws Exception {

		// prevent previous save date confuse this test (createDate only did
		// hh,mm,ss)
		Thread.sleep(2000);

		PlainTextMessage save = super.saveRefMessage();

		MemoryMessageCache cache=new MemoryMessageCache(dataStore);
		
		TestNewMessageListener listener=new TestNewMessageListener();

		cache.pushLatestMessages(listener);
		
		List<AbstractMessage> preload=listener.list;

		assertTrue(preload.contains(save));

		assertTrue(preload.contains(save.getReferenceMessage()));

		int topIdx = preload.indexOf(save);

		int refIdx = preload.indexOf(save.getReferenceMessage());

		AbstractMessage readbackTop = preload.get(topIdx);
		AbstractMessage readbackRef = preload.get(refIdx);

		checkSame(readbackTop.getReferenceMessage(), readbackRef);
	}

	public void testPreloadComplexChatBuildChain() throws Exception {

		Thread.sleep(2000);

		SaveChatBuildMessageResult result = super.saveChatBuildMessage(10);
		
		MemoryMessageCache cache=new MemoryMessageCache(dataStore);
		
		TestNewMessageListener listener=new TestNewMessageListener();
		
		cache.pushLatestMessages(listener);

		List<AbstractMessage> preload = listener.list;

		for (AbstractMessage top : result.topMost) {
			TestUtil.assertContains(preload, top);
		}

		HashMap<String, AbstractMessage> checkDuplicate = new HashMap<String, AbstractMessage>();

		for (AbstractMessage abstractMessage : preload) {

			AbstractMessage leaf = abstractMessage;

			while (leaf != null) {

				if (checkDuplicate.containsKey(leaf.getIdBytes())) {
					checkSame(checkDuplicate.get(leaf.getIdBytes()), leaf);
				} else {
					checkDuplicate.put(leaf.getIdBytes(), leaf);
				}

				leaf = leaf.getReferenceMessage();
			}
		}

	}
}
