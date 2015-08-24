package org.nearbytalk.test.runtime;

import java.util.Calendar;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.nearbytalk.datastore.IDataStore;
import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.exception.DataStoreException;
import org.nearbytalk.exception.DuplicateMessageException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.ChatBuildMessage;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.identity.VoteOfMeMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.runtime.MemoryMessageCache;
import org.nearbytalk.runtime.SlotLock;
import org.nearbytalk.runtime.UnsavedMessageQueue;
import org.nearbytalk.runtime.MemoryMessageCache.AccessRecord;
import org.nearbytalk.runtime.MemoryMessageCache.StateConsistChecker;
import org.nearbytalk.test.TestUtil;
import org.nearbytalk.test.datastore.TestDataStore;
import org.nearbytalk.test.misc.RandomUtility;
import org.nearbytalk.test.misc.ThreadTest;
import org.nearbytalk.test.misc.ThreadTest.SingleTest;

import junit.framework.TestCase;


public class MemoryMessageCacheTestDataStoreTest extends TestCase {

	IDataStore testStore = new TestDataStore(); 

	MemoryMessageCache messageCache; 
	
	@Override
	public void setUp(){
		
		try {
			messageCache=new MemoryMessageCache(testStore);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void tearDown(){
		messageCache.stop();
		
	}

	private void check() {

		messageCache.stateConsist(MemoryMessageCacheTest.ALL_CHECKER);
	}

	public void testIncreaseReferencedCounter() throws Exception {

		PlainTextMessage origin = RandomUtility.randomNoRefTextMessage(null);

		messageCache.save(origin);

		assertEquals("first saved message referencedCounter should be 0", 0,
				origin.getReferencedCounter());

		PlainTextMessage refOtherMessage = new PlainTextMessage(
				RandomUtility.randomIdBytesString(),
				RandomUtility.randomUser(), RandomUtility.nextString(),
				Calendar.getInstance().getTime(), origin.getIdBytes(), 2, 0, 0,
				0);

		messageCache.save(refOtherMessage);

		assertEquals("referenced counter should increase", 1,
				origin.getReferencedCounter());
	}

	public void testThreadIncreaseReferencedCounter() throws Exception {
		final PlainTextMessage origin = RandomUtility.randomNoRefTextMessage(null);

		messageCache.save(origin);
		
		final int threadNumber=100;
		final int loopNumber=100;
		
		check();

		ThreadTest.run(threadNumber, loopNumber, 0, new SingleTest() {

			@Override
			public void singleTest() throws Exception {
				PlainTextMessage refOtherMessage = new PlainTextMessage(
						RandomUtility.randomIdBytesString(), RandomUtility
								.randomUser(), RandomUtility.nextString(),
						Calendar.getInstance().getTime(), origin.getIdBytes(),
						2, 0, 0, 0);

				messageCache.save(refOtherMessage);

			}
		});
		
		messageCache.getUnsavedMessageQueue().stop();
		
		check();
		
		AbstractMessage getBack=messageCache.get(origin.getIdBytes());
		
		assertEquals(threadNumber*loopNumber, getBack.getReferencedCounter());

	}

	public void testAddAndGet() throws Exception {

		PlainTextMessage randomMessage = RandomUtility
				.randomNoRefTextMessage(null);

		check();

		testStore.saveMessage(randomMessage);
		messageCache.save(randomMessage);

		check();

		AbstractMessage get = messageCache.get(randomMessage.getIdBytes());

		assertNotNull(get);

		check();

		assertTrue(randomMessage.sameStrippedUser(get));

	}

	public void testThreadAddAndGet() throws InterruptedException {
		ThreadTest.run(20, 100, 10, new SingleTest() {

			@Override
			public void singleTest() throws Exception {
				testAddAndGet();
			}
		});
	}

	public void testOutOfCache() throws Exception {

		PlainTextMessage first = RandomUtility.randomNoRefTextMessage(null);

		testStore.saveMessage(first);

		messageCache.save(first);

		check();

		for (int i = 0; i < messageCache.getSizeLimit() * 2; i++) {

			messageCache.save(RandomUtility.randomNoRefTextMessage(null));

			check();
		}
		// first should out of cache

		AbstractMessage reload = messageCache.get(first.getIdBytes());

		assertTrue(first.sameStrippedUser(reload));
	}
	
	public void testPreload() throws Exception{
		
		List<ChatBuildMessage> longChain=RandomUtility.randomChatBuildMessage(null, 10);

		testStore.saveMessage(longChain);
		
		TestNewMessageListener listener=new TestNewMessageListener();
		
		//must renew one , or datastore's content will not be included
		messageCache=new MemoryMessageCache(testStore);
		
		messageCache.pushLatestMessages(listener);
		
		assertEquals(longChain.size(), listener.list.size());
		
		for (ChatBuildMessage chatBuildMessage : longChain) {
			TestUtil.assertContains(listener.list, chatBuildMessage);
		}

	}

	public void testRecycleLogic() throws DataStoreException,
			BadReferenceException, InterruptedException, DuplicateMessageException {

		final VoteTopicMessage topic = RandomUtility.randomVoteTopicMessage(null);

		messageCache.save(topic);

		messageCache.getUnsavedMessageQueue().flushAndWait();
		
		messageCache.stateConsist(MemoryMessageCacheTest.ALL_CHECKER);

		messageCache.stateConsist(new StateConsistChecker() {
			@Override
			public void check(ConcurrentHashMap<String, SlotLock> slotLocks,
					ConcurrentHashMap<String, AccessRecord> cache,
					TreeMap<Long, String> lRUList,
					UnsavedMessageQueue unsavedMessageQueue) {
				
				assertTrue("not recycled one should be in cache",cache.containsKey(topic.getIdBytes()));
				
				assertTrue("after flush unsaved, the topic should be in LRUList",lRUList.containsValue(topic.getIdBytes()));
				
				assertEquals("back ref should be zero",0, cache.get(topic.getIdBytes()).backReference.get());
			}
		});

		final VoteOfMeMessage me = RandomUtility.randomVoteOfMeMessage(null, topic);
		
		messageCache.save(me);
		
		messageCache.getUnsavedMessageQueue().flushAndWait();
		
		messageCache.stateConsist(MemoryMessageCacheTest.ALL_CHECKER);

		messageCache.stateConsist(new StateConsistChecker() {
			
			@Override
			public void check(ConcurrentHashMap<String, SlotLock> slotLocks,
					ConcurrentHashMap<String, AccessRecord> cache,
					TreeMap<Long, String> lRUList,
					UnsavedMessageQueue unsavedMessageQueue) {
				
				assertTrue(cache.containsKey(topic.getIdBytes()));
				
				assertEquals(1, cache.get(topic.getIdBytes()).backReference.get());
				
				assertTrue(cache.containsKey(me.getIdBytes()));

				assertEquals(0, cache.get(me.getIdBytes()).backReference.get());
				
				assertFalse("backref >0 msg should not in LRU",lRUList.containsValue(topic.getIdBytes()));
				
				assertTrue("VoteOfMe message should be in LRU",lRUList.containsValue(me.getIdBytes()));
			}
		});
	}

}
