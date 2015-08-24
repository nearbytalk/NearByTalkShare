package org.nearbytalk.test.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nearbytalk.datastore.IDataStore;
import org.nearbytalk.datastore.SQLiteDataStore;
import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.exception.DataStoreException;
import org.nearbytalk.exception.DuplicateMessageException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.query.PagedQuery.PagedInfo;
import org.nearbytalk.runtime.IMessageCache;
import org.nearbytalk.runtime.MemoryMessageCache;
import org.nearbytalk.runtime.UnsavedMessageQueue;
import org.nearbytalk.test.datastore.TestDataStore;
import org.nearbytalk.test.misc.RandomUtility;
import org.nearbytalk.test.misc.ThreadTest;
import org.nearbytalk.test.misc.ThreadTest.SingleTest;

import com.almworks.sqlite4java.SQLiteBusyException;

public class UnsavedMessageQueueTest extends TestCase {

	public void testExpiredTimeSave() throws Exception {

		TestDataStore testDataStore=new TestDataStore();

		MemoryMessageCache messageCache = new MemoryMessageCache(testDataStore);

		PlainTextMessage shouldSave = RandomUtility
				.randomNoRefTextMessage(null);

		messageCache.save(shouldSave);

		Thread.sleep(messageCache.getExpiredMillionSeconds()*2);

		assertEquals(testDataStore.container.size(), 1);

		assertEquals(testDataStore.container.values().iterator().next(), shouldSave);

	}

	private Collection<PlainTextMessage> threadPushRandom(
			final UnsavedMessageQueue queue) throws Exception {

		final ClientUserInfo userInfo = RandomUtility.randomUser();

		final IDataStore dataStore = queue.getMessageCache().getDataStore();

		dataStore.saveOrUpdateUser(userInfo);

		final ConcurrentLinkedQueue<PlainTextMessage> shouldSave = new ConcurrentLinkedQueue<PlainTextMessage>();

		assertTrue(ThreadTest.run(100, 20, 10, new SingleTest() {

			@Override
			public void singleTest() throws Exception {
				PlainTextMessage save = RandomUtility
						.randomNoRefTextMessage(userInfo);
				shouldSave.add(save);
				queue.addUnsaved(save);
			}
		}).isEmpty());

		return shouldSave;

	}

	public void testTestDataStoreThreadPush() throws Exception {

		final Set<AbstractMessage> memSave = new ConcurrentHashSet<AbstractMessage>();

		final int busyEmulate = 5000;

		TestMessageCache msgCache = new TestMessageCache(
				new TestDataStore() {
					@Override
					public boolean saveMessage(
							Collection<? extends AbstractMessage> messageList) {
						memSave.addAll(messageList);

						// emulate database busy
						try {
							Thread.sleep(busyEmulate * 2);
						} catch (InterruptedException e) {
						}
						return true;
					}
				});

		UnsavedMessageQueue queue = new UnsavedMessageQueue(2000, 40, msgCache);

		Collection<PlainTextMessage> shouldSave = threadPushRandom(queue);

		Thread.sleep(busyEmulate * 5);

		assertEquals(shouldSave.size(), memSave.size());

		assertTrue(memSave.containsAll(shouldSave));

	}

	public void testSQLiteDataStoreThreadPush() throws Exception {

		final SQLiteDataStore dataStore = new SQLiteDataStore();

		IMessageCache msgCache = new TestMessageCache(dataStore);

		UnsavedMessageQueue queue = new UnsavedMessageQueue(5000, 50, msgCache);

		Collection<PlainTextMessage> shouldSave = threadPushRandom(queue);

		queue.flushAndWait();

		for (PlainTextMessage mustHave : shouldSave) {

			int tryNumber = 0;
			while (tryNumber++ < 10) {
				try {

					List<PlainTextMessage> result = dataStore.queryMessage(
							PlainTextMessage.class, mustHave.getIdBytes(),
							new PagedInfo());

					assertEquals(1, result.size());

					assertEquals(mustHave, result.get(0));
					break;
				} catch (SQLiteBusyException ex) {
					Thread.sleep(30);
				}
			}

			assertTrue(tryNumber <= 10);
		}

	}

	private void recursiveSave(IMessageCache cache, AbstractMessage message)
			throws DataStoreException, BadReferenceException,
			InterruptedException, DuplicateMessageException {

		AbstractMessage ref = message.getReferenceMessage();

		if (ref != null) {
			recursiveSave(cache, ref);
		}

		cache.save(message);
	}

	public void testSaveDeepReference() throws Exception {
		SQLiteDataStore store = new SQLiteDataStore();

		MemoryMessageCache msgCache = new MemoryMessageCache(store);

		ClientUserInfo randomUser = RandomUtility.randomUser();

		assertTrue(store.saveOrUpdateUser(randomUser));

		PlainTextMessage root = RandomUtility.randomRefTextMessage(randomUser);

		recursiveSave(msgCache, root);
	}

	/**
	 * unsaved message queue should not explores memory
	 * 
	 * @throws Exception
	 */
	public void testMemLeaf() throws Exception {

		MemoryMessageCache msgCache = new MemoryMessageCache(
				new SQLiteDataStore());

		ClientUserInfo randomUser = RandomUtility.randomUser();

		msgCache.getDataStore().saveOrUpdateUser(randomUser);

		for (int i = 0; i < 10000; i++) {
			msgCache.save(RandomUtility.randomNoRefTextMessage(randomUser));
		}

		msgCache.stop();
		msgCache.stateConsist(MemoryMessageCacheTest.ALL_CHECKER);

	}

	public void testFlushAndWait() throws InterruptedException {

		final HashMap<String, AbstractMessage> take = new HashMap<String, AbstractMessage>();
		
		final AtomicBoolean stopSave=new AtomicBoolean(false);

		final TestDataStore testDataStore = new TestDataStore() {

			@Override
			public boolean saveMessage(AbstractMessage one) {
				
				if (stopSave.get()) {
					return true;
				}

				AbstractMessage prev = take.put(one.getIdBytes(), one);

				assertNull(prev);
				return true;
			}

			@Override
			public boolean saveMessage(
					Collection<? extends AbstractMessage> messageList) {

				for (AbstractMessage abstractMessage : messageList) {
					saveMessage(abstractMessage);
				}
				return true;
			}

		};

		UnsavedMessageQueue queue = new UnsavedMessageQueue(1000, 100,
				new TestMessageCache(testDataStore));
		
		
		List<AbstractMessage> toSave=new ArrayList<AbstractMessage>();
		
		for(int i=0;i<100;++i){
			toSave.add(RandomUtility.randomNoRefTextMessage(null));
		}
		
		for (AbstractMessage abstractMessage : toSave) {
			queue.addUnsaved(abstractMessage);
		}
		
		queue.flushAndWait();
		
		stopSave.set(true);
		
		for (AbstractMessage abstractMessage : toSave) {
			
			assertTrue(take.containsKey(abstractMessage.getIdBytes()));
			
			assertSame(take.get(abstractMessage.getIdBytes()), abstractMessage);
		}

	}

}
