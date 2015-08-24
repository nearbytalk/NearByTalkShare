package org.nearbytalk.test.runtime;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.nearbytalk.datastore.IDataStore;
import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.exception.DataStoreException;
import org.nearbytalk.exception.DuplicateMessageException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.identity.VoteOfMeMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.runtime.IMessageCache;
import org.nearbytalk.runtime.MemoryMessageCache;
import org.nearbytalk.runtime.SlotLock;
import org.nearbytalk.runtime.UnsavedMessageQueue;
import org.nearbytalk.runtime.MemoryMessageCache.AccessRecord;
import org.nearbytalk.runtime.MemoryMessageCache.StateConsistChecker;
import org.nearbytalk.test.datastore.SQLiteDataStoreShareTest;
import org.nearbytalk.test.misc.RandomUtility;
import org.nearbytalk.test.misc.ThreadTest;

import com.almworks.sqlite4java.SQLiteBusyException;

public abstract class MessageCacheSQLiteDataStoreTest extends
		SQLiteDataStoreShareTest {

	private IMessageCache messageCache = messageCacheImpl(super.dataStore, 1000);

	protected abstract IMessageCache messageCacheImpl(IDataStore dataStore,
			int preloadSize);

	protected abstract void checkSame(AbstractMessage lhs, AbstractMessage rhs);

	

	public void testMultiThreadGetSame() throws SQLiteBusyException,
			ParseException, InterruptedException, DataStoreException,
			BadReferenceException {

		final AbstractMessage toGet = super.saveNoRefMessage(null);

		int size = 100;

		final AbstractMessage[] putResult = new AbstractMessage[size];

		Collection<Exception> errors = ThreadTest.run(size, 1, 0,
				new ThreadTest.SingleTest() {

					@Override
					public void singleTest(int threadIdx, int threadNumber)
							throws DataStoreException, BadReferenceException,
							InterruptedException {
						AbstractMessage get = messageCache.get(toGet
								.getIdBytes());

						assertEquals(get, toGet);

						putResult[threadIdx] = get;
					}
				});

		assertTrue(errors.isEmpty());

		AbstractMessage onlyOne = messageCache.get(toGet.getIdBytes());

		for (AbstractMessage abstractMessage : putResult) {
			checkSame(onlyOne, abstractMessage);
			assertEquals(toGet, abstractMessage);
		}

	}

	public void testMultiThreadAddSameRef() throws SQLiteBusyException,
			ParseException, InterruptedException, DataStoreException,
			BadReferenceException {
		AbstractMessage leaf = super.saveNoRefMessage(null);

		int size = 100;
		final List<AbstractMessage> lots = new ArrayList<AbstractMessage>(size);

		for (int i = 0; i < size; i++) {
			lots.add(new PlainTextMessage(leaf.getSender(), RandomUtility
					.nextString(), leaf));
		}

		final ConcurrentHashMap<String, AbstractMessage> addResult = new ConcurrentHashMap<String, AbstractMessage>();

		Collection<Exception> errors = ThreadTest.run(size, 1, 0,
				new ThreadTest.SingleTest() {

					@Override
					public void singleTest(int threadIndex, int threadNumber)
							throws Exception {

						AbstractMessage putResult = messageCache.save(lots
								.get(threadIndex));

						checkSame(putResult, lots.get(threadIndex));

						addResult.put(putResult.getIdBytes(), putResult);
					}
				});

		assertTrue(errors.isEmpty());

		AbstractMessage shouldBeSame = messageCache.get(leaf.getIdBytes());

		for (AbstractMessage abstractMessage : addResult.values()) {

			checkSame(abstractMessage.getReferenceMessage(), shouldBeSame);

		}

	}

	public void testRejectNoExistRefMsg() throws SQLiteBusyException,
			DataStoreException, InterruptedException, DuplicateMessageException {

		ClientUserInfo randomUser = saveRandomUser();

		PlainTextMessage root = new PlainTextMessage(
				RandomUtility.randomIdBytesString(), randomUser,
				RandomUtility.nextString(), Calendar.getInstance().getTime(),
				RandomUtility.randomIdBytesString(), 2, 0, 0, 0);

		root.parseTopics();

		try {
			messageCache.save(root);
		} catch (BadReferenceException e) {
			return;
		}

		fail("should throw");
	}

	public void testUpdateVoteTopicMsg() throws SQLiteBusyException,
			ParseException, BadReferenceException, DataStoreException,
			InterruptedException, DuplicateMessageException {

		VoteTopicMessage saved = saveVoteTopicMessage();

		HashSet<String> myoptions = new HashSet<String>();

		myoptions.add(saved.getOptions().iterator().next());

		VoteOfMeMessage voteOfMeMsg = new VoteOfMeMessage(saved.getSender(),
				"comment", myoptions, saved);

		messageCache.save(voteOfMeMsg);

		// assume it's flushed to datastore
		messageCache.stop();

		VoteTopicMessage readBack = (VoteTopicMessage) dataStore
				.loadWithDependency(saved.getIdBytes());

		VoteTopicMessage inCache = (VoteTopicMessage) messageCache.get(saved
				.getIdBytes());

		assertEquals(inCache, readBack);
	}

	public void testThreadUpdateVoteTopicMsg() throws SQLiteBusyException,
			ParseException, InterruptedException, DataStoreException, BadReferenceException {
		final VoteTopicMessage saved = saveVoteTopicMessage();
		
		int threadNumber=10;
		
		int loopNumber=10;
		
		final ConcurrentHashMap<String, AbstractMessage> voteOfMes=new ConcurrentHashMap<String, AbstractMessage>();
		
		int totalUser=threadNumber*loopNumber;
		
		final ClientUserInfo users[]=new ClientUserInfo[totalUser];
		
		for(int i=0;i<totalUser;++i){
			users[i]=saveRandomUser();
			//avoid too fast insert 
			Thread.sleep(100);
		}

		Collection<Exception> errors = ThreadTest.run(threadNumber, loopNumber, 0,
				new ThreadTest.SingleTest() {

					@Override
					public void singleTest(int threadIdx,int threadNumber, int loopIdx,int loopNumber) throws Exception {
						HashSet<String> myoptions = new HashSet<String>();

						myoptions.add(saved.getOptions().iterator().next());
						
						ClientUserInfo thisUser=users[threadIdx*loopNumber+loopIdx];

						VoteOfMeMessage voteOfMeMsg = new VoteOfMeMessage(thisUser, RandomUtility.nextString(), myoptions, saved);
						
						
						AbstractMessage prev=voteOfMes.put(voteOfMeMsg.getIdBytes(), voteOfMeMsg);
						
						assertNull(prev);

						messageCache.save(voteOfMeMsg);

					}

					@Override
					public void threadLeaveCallback() {
						dataStore.threadRecycle();
					}

				});

		assertTrue(errors.isEmpty());
		
		messageCache.stop();
		
		if (messageCache instanceof MemoryMessageCache) {
			((MemoryMessageCache) messageCache).stateConsist(
					new StateConsistChecker() {

						@Override
						public void check(
								ConcurrentHashMap<String, SlotLock> slotLocks,
								ConcurrentHashMap<String, AccessRecord> cache,
								TreeMap<Long, String> lRUList,
								UnsavedMessageQueue unsavedMessageQueue) {
							MemoryMessageCacheTest.ALL_CHECKER.check(slotLocks, cache, lRUList, unsavedMessageQueue);
						
							AccessRecord record=cache.get(saved.getIdBytes());
							assertNotNull(record);
						
							assertFalse(lRUList.containsKey(record.serial));

							for (Long serial : lRUList.keySet()) {
								assertEquals(0,cache.get(lRUList.get(serial)).backReference.get());
							}
						}
						
					});
		}
		
		VoteTopicMessage readBack = (VoteTopicMessage) dataStore
				.loadWithDependency(saved.getIdBytes());

		VoteTopicMessage inCache = (VoteTopicMessage) messageCache.get(saved.getIdBytes());

		assertEquals(inCache, readBack);	
		
		Map<String, Long> results=inCache.getResults();
		
		String firstOption=inCache.getOptions().iterator().next();
		
		assertEquals(results.get(firstOption), Long.valueOf(threadNumber*loopNumber));
		
		for (AbstractMessage voteOfMe: voteOfMes.values()) {
			
			AbstractMessage loadBack=messageCache.get(voteOfMe.getIdBytes());
			
			assertEquals(loadBack, voteOfMe);
			
			AbstractMessage loadFromDb=messageCache.getDataStore().loadWithDependency(voteOfMe.getIdBytes());
			
			assertEquals(loadFromDb, loadBack);
			
		}
		
	}
}
