package org.nearbytalk.runtime;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.nearbytalk.datastore.IDataStore;
import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.exception.DataStoreException;
import org.nearbytalk.exception.DuplicateMessageException;
import org.nearbytalk.identity.AbstractFileMessage;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.BaseUserInfo;
import org.nearbytalk.identity.VoteOfMeMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.query.PagedQuery.PagedInfo;
import org.nearbytalk.runtime.SlotLock.LockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteBusyException;
import com.almworks.sqlite4java.SQLiteException;

/**
 * cache writed message
 * 
 * TODO need to check recursive meanings
 * 
 * <pre>
 * 
 *    slotted lock design: when message needs to be access/remove from cache
 *                         their's idbytes must hold the slotted lock to prevent race condition
 *                         slotted lock is ConcurrentHashMap based (with some mutex internal)
 *     		  3 action on message:
 *     						1.  get from cache
 *     						2.  loading to cache
 *     						3.  recycle from cache
 *     		  rules on 3 action : (when operating the same idbytes)
 *     						recycle will skip if getting/loadding in slot (putIfAbuse)
 *     						getting/loading will wait until recycle finished (waiting on condition in this slot)
 *     						getting will wait until loadding finished
 *     						
 *     		  detail of 3 action : 
 *     						loading: hold slot(putIfAbuse): 	
 *     							    (prefer hold slot first instead of get from cache 
 *     							     since recycle in other thread can be skipped)
 *     								success->create new AccessRecord->put to cache (putIfAbuse)
 *     										 -> success -> release slot (remove) ->update LRU
 *     										 -> failed  -> update existing serial/backref ->update LRU
 *     										 (we may load a message which may already in cache, since a message chain
 *     										  load from db may have some leaf parts already in cache)
 *     
 *     							    	release slot. 
 *     								failed-> waiting then check 
 *     										 if is waiting after recycle (recycle thread must remove from cache before notify ),
 *     											   just taking message from slot(it may newer than the one load from db, since
 *     											   saving thread may not save the runtime message  to db).
 *     							 LABEL:ref:if is waiting after loading, getting message from slot,
 *     										 (if increasing backRef ,must in sync block ,prevent recycle just after )
 *     										 if not increasing backRef, just return this message, since outside just 
 *     										 need a message with no modify ,so not consider it's referenced message change
 *     										 
 *     								after add self to cache, add this message's reference message too (recurisve)	
 *     								recursive result is the actually in cache's winner result, set this result to self message
 *     								
 *     						        now self message is final
 *     
 *     						getting: try to hold slot 
 *     								  success->(no recycle thread will kick in)-> 
 *     									  lookup in cache update serial ->update LRU
 *     
 *     								      release slot
 *     								      return result
 *     
 *     								      if not in cache ,return loading 
 *     								      (must re-race to ensure Loading, 
 *     										since we can not translate from getting->loading directly(
 *     									  other Getting thread can not be notified as this change))
 *     
 *     								  failed->wait then check
 *     									  if waiting after recycling, readd it to cache add update LRU
 *     									  if waiting after loading ,same as LABEL:ref
 *     
 *                          recycle:  sync on LRUList ,get first entry (oldest access record)
 *                          	try to hold slot
 *                          		  success : in sync block ,remove slot lock
 *                          		 
 *                          		  failed: try next entry until finish
 * 
 * </pre>
 * 
 */
public class MemoryMessageCache implements IMessageCache {

	private static Logger log = LoggerFactory
			.getLogger(MemoryMessageCache.class);

	/**
	 * time based message access record
	 * 
	 */
	public static class AccessRecord {
		AbstractMessage message;
		/**
		 * access time, less is older, greater is later
		 * 
		 */
		public long serial;

		/**
		 * how many other message reference this, only if it decreased to 0,
		 * means no other message reference this, and this becomes recyclable
		 * 
		 */
		public AtomicInteger backReference = new AtomicInteger();

		public AccessRecord(AbstractMessage message, long serial) {
			this.message = message;
			this.serial = serial;
		}

	}

	/**
	 * every message in cache is a complete chain, and any message in cache will
	 * has it's all dependency in cache , this makes any new message with
	 * reference will getting a one-time lookup if it's dependency in cache (or
	 * once loadWithDependency db access)
	 * 
	 */
	private ConcurrentHashMap<String, AccessRecord> cache = new ConcurrentHashMap<String, AccessRecord>();

	/**
	 * Least Recently Used record
	 */
	private TreeMap<Long, String> LRUList = new TreeMap<Long, String>();

	private ConcurrentHashMap<String, SlotLock> slotLocks = new ConcurrentHashMap<String, SlotLock>();

	private AtomicLong serial = new AtomicLong();

	private int sizeLimit = 200;

	private IDataStore dataStore;

	public MemoryMessageCache(IDataStore dataStore) throws Exception{
		
		this(dataStore, 500, 5000, 500);
	}

	public MemoryMessageCache(IDataStore dataStore, int cacheSizeLimit,int expiredTimeMS,int expiredLength) throws Exception
			{
		this.dataStore = dataStore;
		this.sizeLimit = cacheSizeLimit;
		this.unsavedMessageQueue = new UnsavedMessageQueue(expiredTimeMS, expiredLength, this);
		unsavedMessageQueue.preload(preload(cacheSizeLimit));
	}

	private AbstractMessage recursiveAddAndGet(AbstractMessage alreadyLoaded,
			boolean lockAlreadyHold) throws InterruptedException {
		
		log.trace("recursive add and get for {}",alreadyLoaded.getIdBytes());

		String idBytes = alreadyLoaded.getIdBytes();

		boolean currentThreadHoldLock = lockAlreadyHold;

		SlotLock prevLock = null;
		SlotLock tryLock = null;
		if (!lockAlreadyHold) {
			tryLock = new SlotLock(LRUList, slotLocks, idBytes,
					LockState.Loading);

			prevLock = slotLocks.putIfAbsent(idBytes, tryLock);

			currentThreadHoldLock = (prevLock == null);
		}

		if (!currentThreadHoldLock) {
			
			log.trace("other thread hold slot lock of {}, return tryGetAsWait",idBytes);
			assert prevLock!=null;

			// other thread getting the lock, but may also a getting lock.
			// if this is situation ,just return it (no need to blocking)

			return tryGetAsWait(prevLock);
			
		}


		log.trace("{},lockAlreadyHold: {}, hold the slot lock",idBytes,lockAlreadyHold);

		// current thread hold slot lock

		// check cache first

		AccessRecord inCache = cache.get(idBytes);

		if (inCache != null) {

			log.trace("{} already in cache",idBytes);
			// update existing

			if (!lockAlreadyHold) {
				tryLock.accessRecord = inCache;
			} else {
				// slot lock is hold by caller
				slotLocks.get(idBytes).accessRecord = inCache;
			}

			long backReference = inCache.backReference.incrementAndGet();

			if (backReference == 1) {
				// this message must be in LRUList previously, but now it
				// should be removed
				// from LRUList


				synchronized (LRUList) {

					String removeDebug = LRUList.remove(inCache.serial);

					assert removeDebug != null;

					assert removeDebug.equals(idBytes);
				}

				// update existing's serial
				long newSerial=serial.incrementAndGet();
				log.trace("remove {} from LRU, update serial {} to {}",idBytes,inCache.serial,newSerial);
				inCache.serial = newSerial;
			}

			if (!lockAlreadyHold) {

				assert prevLock == null;
				assert tryLock != null;

				tryLock.removeSelfFromLocks();
			}

			// use existing ,give up already loaded message chain,
			// since it may not newer than existing
			
			assert inCache.message!=null;
			return inCache.message;
		}

		// not in cache ,no other thread will set this idbytes key to cache

		AbstractMessage ref = alreadyLoaded.getReferenceMessage();
		if (ref != null) {

			log.trace("recursive add and get {}",ref.getIdBytes());

			// this call makes a lock chain , message is DAG
			// so not possible to get dead lock
			AbstractMessage actualRef = null;

			// spin try
			for (int i = 0; i < TRY_MAX_TIMES; i++) {
				actualRef = recursiveAddAndGet(ref, false);
				if (actualRef != null) {
					break;
				}
			}

			if (actualRef == null) {
				// ref get failed, not a complete chain
				// add failed

				log.warn("recursiveAndAndGet {} as {}'s ref failed, return NULL",idBytes,ref.getIdBytes());
				if (!lockAlreadyHold) {
					SlotLock removeDebug = tryLock.removeSelfFromLocks();
					assert removeDebug == tryLock;
				}
				return null;
			}

			alreadyLoaded.replaceReferenceMessage(actualRef);
		}

		AccessRecord newAccessRecord = new AccessRecord(alreadyLoaded,
				serial.incrementAndGet());
		// this will always as dependency , since it's parent always exists
		newAccessRecord.backReference.incrementAndGet();

		log.trace("new accessRecord of {} with serial {}",idBytes,newAccessRecord.serial);

		if (!lockAlreadyHold) {
			assert tryLock != null;
			assert tryLock.accessRecord == null;

			tryLock.accessRecord = newAccessRecord;
		} else {
			assert tryLock == null;
			assert slotLocks.get(idBytes).accessRecord == null;

			//no need to protect accessRecord in mutex block
			//since Getting thread just read it's value .
			slotLocks.get(idBytes).accessRecord = newAccessRecord;
		}

		AccessRecord previousDebug = cache.put(idBytes, newAccessRecord);

		assert previousDebug == null;

		if (!lockAlreadyHold) {
			assert tryLock != null;
			SlotLock removeDebug = tryLock.removeSelfFromLocks();

			assert removeDebug == tryLock;
		}
		
		assert alreadyLoaded!=null;
		
		log.trace("return already loaded {}",alreadyLoaded);

		return alreadyLoaded;



	}

	/**
	 * try get from cache. 
	 * @param idBytes
	 * @return message if already in cache, 
	 * 		    null if not in cache and no other thread hold slotlock, 
	 * 			other thread's SlotLock if other thread holding SlotLock
	 */
	private Object tryGetFromCache(String idBytes) {
		SlotLock tryLock = new SlotLock(LRUList, slotLocks, idBytes,
				LockState.Getting);

		SlotLock prevLock = slotLocks.putIfAbsent(idBytes, tryLock);

		if (prevLock != null) {
			log.trace("other thread hold slot lock for {} while current thread try get from cache",idBytes);
			return prevLock;
		}

		// we holding this lock
		// only other thread Getting action can enter here

		AccessRecord fromCache = cache.get(idBytes);

		// this entry will not be recycled ,since Recycle thread
		// can not enter here while Getting lock is hold
		if (fromCache != null) {
			// already in cache

			// update serial

			Long newSerial = serial.incrementAndGet();

			tryLock.increaseBackRef(fromCache, newSerial);

			tryLock.removeSelfFromLocks();
			
			log.trace("try get from cache successful {}",idBytes);

			return fromCache.message;

		} else {

			// no entry in cache , should reset state as before (remove
			// Getting lock)
			// we can not translate to loading directly,since other
			// thread getting action may
			SlotLock removedDebug = slotLocks.remove(idBytes);

			// after upper line, slot lock no longer hold , needs loop try

			// this must be the one this thread put in
			assert removedDebug == tryLock;

			// no in cache ,translate to next stage

			//
			//

			return null;
		}
	}

	private AbstractMessage tryGetAsWait(SlotLock actuallyLocked)
			throws InterruptedException {

		// other thread already hold Loading slot,but may already removed it
		// from locks
		
		AtomicBoolean fromRecycle=new AtomicBoolean(false);

		AccessRecord accessRecord = actuallyLocked.waitForGetting(fromRecycle);

		if (accessRecord == null) {
			// already removed from slot
			// we do not auto call tryGetAsLoad , we should handle this
			// situation
			// outside in a loop, leads less stack overflow possible
			
			log.warn("try get {} as wait failed",actuallyLocked.idBytes);
			return null;
		}
		
		if (!fromRecycle.get()) {
			//other thread just getting or loading
			log.trace("try get {} as wait success",actuallyLocked.idBytes);
			return accessRecord.message;
		}
		
		log.trace("resume {} from recycleing",actuallyLocked.idBytes);
		//the accessrecord resumed from recycling ,must readd the whole chain
		return recursiveAddAndGet(accessRecord.message, false);
	}

	/**
	 * @param idBytes
	 * @return
	 * @throws DataStoreException
	 * @throws InterruptedException
	 * @throws BadReferenceException
	 *             if no such message in datastore
	 */
	private Object tryGetAsLoad(String idBytes) throws DataStoreException,
			InterruptedException, BadReferenceException {

		SlotLock tryLoadLock = new SlotLock(LRUList, slotLocks, idBytes,
				LockState.Loading);

		SlotLock prevLock = slotLocks.putIfAbsent(idBytes, tryLoadLock);

		if (prevLock != null) {
			// if other thread is getting ,we ignore this try, since we can not
			// makes
			// the thread holds getting slot to enter waiting

			// other thread is loading ,we ignore this try

			// if other thread is recycling ,we need to wait it (return the
			// slotlock)
			return prevLock;
		}

		// current thread hold the slot lock
		log.trace(" {} not in cache ,load it from db", idBytes);

		// only one thread can load one idbytes at same time

		AbstractMessage loadFromDb;

		try {
			loadFromDb = dataStore.loadWithDependency(idBytes);
		} catch (SQLiteBusyException e) {
			throw new DataStoreException(e);
		} catch (ParseException e) {
			throw new DataStoreException(e);
		}

		if (loadFromDb == null) {
			throw new BadReferenceException(idBytes);
		}

		AbstractMessage debug = recursiveAddAndGet(loadFromDb, true);

		// spin try
		for (int i = 0; debug == null && i < TRY_MAX_TIMES; i++) {
			debug = recursiveAddAndGet(loadFromDb, true);
			log.warn("try recursiveAddAndGet {} for {} times failed",loadFromDb.getIdBytes(),i+1);
		}

		assert (debug == loadFromDb);

		SlotLock removeDebug = tryLoadLock.removeSelfFromLocks();

		assert (removeDebug == tryLoadLock);

		return loadFromDb;
	}

	private static int TRY_MAX_TIMES = 100;

	private AbstractMessage spinGet(String idBytes)
			throws InterruptedException, DataStoreException,
			BadReferenceException {

		for (int counter = 0; counter < TRY_MAX_TIMES; ++counter) {
			Object messageOrSlotLock = tryGetFromCache(idBytes);

			if (messageOrSlotLock != null
					&& messageOrSlotLock instanceof AbstractMessage) {
				return (AbstractMessage) messageOrSlotLock;
			}

			if (messageOrSlotLock == null) {
				log.trace("{} not in cache, and no other thread hold slot lock ,try as load",idBytes);
				messageOrSlotLock = tryGetAsLoad(idBytes);

				if (messageOrSlotLock == null) {
					
					log.trace("tryGetAsLoad failed, {} not a valid message ",idBytes);
					return null;
				}

				if (messageOrSlotLock instanceof AbstractMessage) {
					return (AbstractMessage) messageOrSlotLock;
				}
				
				log.trace("want to tryGetAsLoad {}, but slotlock already hold by other thread",idBytes);
			} else {
				log.trace("other thread hold slot lock of {},try get it as wait",idBytes);
			}
			
			assert messageOrSlotLock instanceof SlotLock;
			

			AbstractMessage ret = tryGetAsWait((SlotLock) messageOrSlotLock);

			if (ret != null) {
				return ret;
			}
			
			log.warn("spin get {} times failed",counter+1);

		}

		return null;

	}

	/**
	 * get a message from cache, it may in runtime cache, or loaded from db
	 * 
	 * it's back ref will be increased by one
	 * 
	 * @param idBytes
	 * @return
	 * @throws InterruptedException
	 * @throws DataStoreException
	 * @throws BadReferenceException
	 * @throws Exception
	 */
	@Override
	public AbstractMessage get(String idBytes) throws DataStoreException,
			InterruptedException, BadReferenceException {

		return spinGet(idBytes);

	}

	@Override
	public void judgeMessage(String idBytes, boolean positive) {

		AccessRecord maybeInCache = cache.get(idBytes);

		// do not need lock, increaseXXX is synced method
		if (maybeInCache != null) {
			if (positive) {
				maybeInCache.message.increaseAgreeCounter();
			} else {
				maybeInCache.message.increaseDisagreeCounter();
			}
		}

		try {
			dataStore.judgeMessage(idBytes, positive);
		} catch (SQLiteBusyException e) {
			// not throw when busy
			log.error("{}", e);
		}
	}

	/**
	 * add a message to cache. note: currently only support top level save. that
	 * means except the message passed in all of it's leaf should be already
	 * saved to db (or added to cache) (a message which ref a file message is
	 * supported) a complete chain from caller is not considered right now
	 * 
	 * @param message
	 * @return the actual added message, with backRef . must be release after
	 *         use
	 * @throws InterruptedException
	 * @throws DataStoreException
	 * @throws BadReferenceException
	 * @throws DuplicateMessageException 
	 * @throws Exception
	 */
	public AbstractMessage save(AbstractMessage message)
			throws DataStoreException, InterruptedException,
			BadReferenceException, DuplicateMessageException {
		
		//no need to consider passed in message with already exits idbytes
		//we assume caller passed in a valid message, so if it's the same idbytes
		//it must be same as exists.
		
		if (message.anyReferenceIdBytes() != null
				// if message has a AbstractFileMessage as ref, the AbstractFileMessage is belong
				// to message self, no need to complete this chain (it's already a complete chain)
				&& !(message.getReferenceMessage() instanceof AbstractFileMessage)) {
			
			log.trace("getting {} for complete message {}",message.getReferenceIdBytes(),message.getIdBytes());

			if (message instanceof VoteOfMeMessage || message instanceof VoteTopicMessage) {
				//must check this is not duplicate ones
				if (cache.containsKey(message.getIdBytes())) {
					//duplicate 					
					throw new DuplicateMessageException();

				} else
					try {
						if (dataStore.existMessage(message.getIdBytes())) {
							throw new DuplicateMessageException();
						}
					} catch (SQLiteException e) {
						throw new DataStoreException(e);
					}
			}
			
			
			//if message is not VoteOfMeMessage, it will never be same idbytes 
			//since 

			// will throw if error happened
			AbstractMessage ref = get(message.getReferenceIdBytes());

			// make it a complete message
			message.setReferenceMessageLater(ref);
		}

		// currently it's a complete chain
		
		for (int i = 0; i < TRY_MAX_TIMES; i++) {
			AbstractMessage retDebug=recursiveAddAndGet(message, false);
			if (retDebug!=null) {
				assert (retDebug == message) || (retDebug.equals(message));
				unsavedMessageQueue.addUnsaved(retDebug);
				
				return retDebug;
			}
			log.warn("spin recursiveAddAndGet for {} msg {} times failed",message.getIdBytes(),i+1);
		}

		// it should not add same message more than one time. if this is the
		// case , result should be equal

		return null;
	}

	@Override
	public void releaseMessage(AbstractMessage message) {
		
		log.trace("{} backref to be decreased",message.getIdBytes());
		
		// for simple implementation, release message is always called
		// from one thread, this help us do simple backRef decrease 
		// for example, if other thread holding lock ,
		// it must be loading or getting, so decrease backRef didn't 
		// consider recycle . 

		String idBytes = message.getIdBytes();

		AccessRecord accessRecord = cache.get(idBytes);

		// when release message ,it should at least backref=1,
		// should not be in LRUList
		assert accessRecord != null;

		synchronized (LRUList) {
			//debug only
			assert !LRUList.containsKey(accessRecord.serial);
		}

		SlotLock tryRecycle = new SlotLock(LRUList, slotLocks, idBytes,
				LockState.Recycling);

		SlotLock prevLock= slotLocks.putIfAbsent(idBytes, tryRecycle);

		if (prevLock== null) {

			//current thread hold recycle lock

			assert (cache.containsKey(idBytes));

			tryRecycle.accessRecord = cache.get(idBytes);

			assert tryRecycle.accessRecord != null;

			AtomicBoolean LRUupdated = new AtomicBoolean();

			tryRecycle.removeSelfFromLocks(LRUupdated, this.sizeLimit);

			// we only decrease reference message backref when top message
			// actually
			// be recycled (remove from cache, remove from LRUList)

			if (LRUupdated.get() == true) {
				// LRU updated ,we should check if it extends size limit
				recycle();
			}

		} else {
			// other thread hold the lock ,it can not be recycled
			// (other thread will increase it's backref)
			// any backRef decrease must through releaseMessage
			// so won't leave a message not being recycled

			accessRecord.backReference.decrementAndGet();
		}

	}

	/**
	 * kick off first element from LRU and cache map
	 * 
	 */
	private void recycle() {

			
		TreeMap<Long, String> tempCopy=new TreeMap<Long, String>();
		synchronized (LRUList) {
			//temp copy it ,so we don't need to sync on LRUList
			//we may lose some new added entry, but this is OK
			tempCopy.putAll(LRUList);
		}

		// we must iterator the LRUList until we can recycle one .
		// since the one in LRUList may just being getted from other thread
		// leads slot lock hold failed
		Iterator<Entry<Long, String>> iterator = tempCopy.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<Long, String> entry = iterator.next();

			String idBytes = entry.getValue();

			SlotLock tryRecycleLock = new SlotLock(LRUList, slotLocks, idBytes,
					LockState.Recycling);

			SlotLock prevLock = slotLocks.put(idBytes, tryRecycleLock);

			if (prevLock != null) {
				// lock failed, try next
				continue;
			}

			// slot lock ok
			
			assert cache.get(idBytes)!=null;
			// assign accessRecord so that getting waiting for recycle action can success
			tryRecycleLock.accessRecord=cache.get(idBytes);
			
			
			assert tryRecycleLock.accessRecord.backReference.get()==0;
			
			synchronized (LRUList) {
				assert LRUList.containsKey(entry.getKey());
				
				LRUList.remove(entry.getKey());
			}
			
			AccessRecord removeDebug=cache.remove(idBytes);
			
			log.trace("removing {} from cache",idBytes);

			assert removeDebug==tryRecycleLock.accessRecord;
			
			
			//we already decreased backref
			tryRecycleLock.removeSelfFromLocks();
			
			AbstractMessage ref=removeDebug.message.getReferenceMessage();
			
			if (ref!=null) {
				//has reference ,also release it (decrease once, but not recycle)
				assert cache.contains(ref.getIdBytes());

				synchronized (LRUList) {
					//currently we didn't call release message ,so it must have backRef>0
					assert !LRUList.containsKey(cache.get(ref.getIdBytes()).serial);
				}
				
				releaseMessage(ref);
			}
			
			break;
		}
	}

	

	public int getSizeLimit() {
		return sizeLimit;
	}

	/**
	 * marks message as "INVALID" to emulate delete message in runtime
	 * 
	 * @param idBytes
	 */
	private void invalidMessage(String idBytes) {

		AccessRecord record = cache.get(idBytes);

		if (record != null) {

			// mark as invalid
			record.message.invalid();

		}

	}

	private List<AbstractMessage> preload(int preloadSize) throws Exception {

		List<AbstractMessage> preload = dataStore.queryNewest(
				AbstractMessage.class, new PagedInfo(preloadSize, 1));

		log.debug("preloaded messages", preload);

		// unsaved message is FIFO,so preload should follow this rule

		ListIterator<AbstractMessage> iterator = preload.listIterator(0);

		List<AbstractMessage> ret = new ArrayList<AbstractMessage>(
				preload.size());
		while (iterator.hasNext()) {

			// recursive add every message to cache.
			AbstractMessage message = iterator.next();

			AbstractMessage finalUsed = recursiveAddAndGet(message, false);

			ret.add(finalUsed);

			// all of them should be in result, but some of their leafs may
			// duplicate with top

		}

		return ret;
	}

	@Override
	public IDataStore getDataStore() {
		return dataStore;
	}

	private final UnsavedMessageQueue unsavedMessageQueue;

	@Override
	public boolean deleteMessage(BaseUserInfo user, String idBytes) {
		
		if(unsavedMessageQueue.deleteUnsaved(user,idBytes)){
			return true;
		}

		//not in unsaved queue
		if (!dataStore.delete(user, idBytes,false)) {
			return false;
		}

		invalidMessage(idBytes);
		return true;
	}

	@Override
	public void pushLatestMessages(NewMessageListener listener) {
		
		unsavedMessageQueue.pushAllMessages(listener);
	}

	public int getExpiredMillionSeconds() {
		return unsavedMessageQueue.getExpiredMillionSeconds();
	}

	public UnsavedMessageQueue getUnsavedMessageQueue() {
		return unsavedMessageQueue;
	}
	
	public void stateConsist(StateConsistChecker checker){
		checker.check(slotLocks,cache,LRUList,unsavedMessageQueue);
	}
	
	public static interface StateConsistChecker{
		void check (ConcurrentHashMap<String, SlotLock> slotLocks, ConcurrentHashMap<String, AccessRecord> cache, TreeMap<Long, String> lRUList, UnsavedMessageQueue unsavedMessageQueue);
	}

	@Override
	public void stop() {
		try {
			unsavedMessageQueue.stop();
		} catch (InterruptedException e) {
			log.error("{}",e);
		}		
	}

}
