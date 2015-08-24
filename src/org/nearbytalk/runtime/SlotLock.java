package org.nearbytalk.runtime;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.nearbytalk.runtime.MemoryMessageCache.AccessRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SlotLock {

	private final ConcurrentHashMap<String, SlotLock> locks;
	
	private final TreeMap<Long, String> LRUList;
	
	private final static Logger log=LoggerFactory.getLogger(SlotLock.class);
	
	private final ReentrantReadWriteLock rwLock=new ReentrantReadWriteLock();
	
	private Condition waitCondition;

	final String idBytes;

	public SlotLock(TreeMap<Long,String> LRUList, ConcurrentHashMap<String, SlotLock> locks, String idBytes,
			LockState initLockState) {
		this.LRUList=LRUList;
		this.idBytes = idBytes;
		this.locks = locks;
		this.lockState = initLockState;
		
		if (this.lockState==LockState.Loading || this.lockState==LockState.Recycling) {
			log.trace("{} idbytes {}",lockState,idBytes);
			waitCondition=rwLock.writeLock().newCondition();
		}
	}

	public static enum LockState {
		Getting, Loading, Recycling,
	}

	LockState lockState;

	AccessRecord accessRecord;

	/**
	 * mark if lock is already removed from slotlocks.
	 * we can only check this var to determine if valid, 
	 * 
	 * 
	 * it's impossible to use wait condition . since 
	 * 
	 *  put-to-slotlocks-and-wait is not atomic, thread for Getting may get 
	 *  a slotlock which is already removed from slotlocks, which makes wait 
	 *  useless
	 * 
	 * 
	 * 
	 */
	boolean alreadyInvalid = false;

	/**
	 * try get message when current thread not hold the slot lock, will increase backref
	 * try get accessrecord while other thread is doing something else 
	 * on the same idbytes. for simple impl, this action didn't update
	 * the serial, since only the thread actually hold the slot lock 
	 * update it's serial
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public AccessRecord waitForGetting(AtomicBoolean fromRecycle) throws InterruptedException {


		rwLock.readLock().lock();

		if (alreadyInvalid) {
			// this slot lock is not useful anymore
			
			log.trace("found already invalid slotlock for {}, failed",idBytes);
			rwLock.readLock().unlock();
			return null;
		}

		if(lockState==LockState.Getting){
			if (accessRecord!=null) {
				increaseBackRefUnlocked(accessRecord);
			}	
			rwLock.readLock().unlock();
			log.trace("successful get accessRecord as read lock for {}",idBytes);
			return accessRecord;
		}




		assert (lockState == LockState.Loading  || 
				//while wait for recycleing finished ,just return the recycled one
				(lockState== LockState.Recycling));

		rwLock.readLock().unlock();

		// out of sync block ,must recheck
		rwLock.writeLock().lock();

		//TODO is this check useless? since recycle may always wake after remove slot lock
		//so it must be true ?
		if (alreadyInvalid) {

			log.trace("found invalid slotlock for {} when second write lock try, return NULL",idBytes);
			rwLock.writeLock().unlock();
			return null;
		}

		log.trace("waiting for {} loading or recycling",idBytes);

		assert waitCondition!=null;

		waitCondition.await();
		log.trace("{} loading or recycling complete",idBytes);

		//TODO loading may also failed, but we assume it will not now
		assert accessRecord!=null;

		assert idBytes.equals(accessRecord.message.getIdBytes());
		
		if (lockState==LockState.Loading) {
			increaseBackRefUnlocked(accessRecord);
			fromRecycle.set(false);
		}else {
			assert lockState==LockState.Recycling;
			//do not increase backRef here, since 
			//notify outside this needs extra work
			fromRecycle.set(true);
		}

		rwLock.writeLock().unlock();

		return accessRecord;
	}
	
	/**
	 * remove slot lock from locks, don't decrease backref
	 * @return
	 */
	public SlotLock removeSelfFromLocks(){
		return removeSelfFromLocks(null,0);
	}
	
	
	/**
	 * remove slot lock from locks, decrease backref
	 * @param LRUexceeded
	 * @param sizeLimit
	 * @return
	 */
	public SlotLock removeSelfFromLocks(AtomicBoolean LRUexceeded,int sizeLimit) {

		//this thread must hold the slotlock
		assert locks.containsValue(this);
		
		
		
		rwLock.writeLock().lock();
		alreadyInvalid = true;

		if (lockState==LockState.Loading) {
			//TODO load may also failed ,but we assume it will not now
			assert idBytes.equals(accessRecord.message.getIdBytes());
		}
		
		if (LRUexceeded!=null) {

			LRUexceeded.set(false);
			// only one thread can
			if (accessRecord.backReference.decrementAndGet() == 0) {
				// it can be recycled
				synchronized (LRUList) {

					assert !LRUList.containsKey(accessRecord.serial);
					assert !LRUList.containsValue(idBytes);

					LRUList.put(accessRecord.serial, idBytes);
					
					if (LRUList.size()>sizeLimit) {
						log.trace("LRU size {} overflow {}",LRUList.size(),sizeLimit);
						LRUexceeded.set(true);
					}
					// TODO trigger recycle
				}
			}
		}

		if (waitCondition!=null) {
			assert (lockState==LockState.Loading)|| (lockState==LockState.Recycling);
			waitCondition.signalAll();			
		}else{
			assert lockState==LockState.Getting;
		}

		rwLock.writeLock().unlock();
		return locks.remove(idBytes);
	}
	
	/**
	 * must have readLock locked
	 * @param oldRecord
	 */
	private void increaseBackRefUnlocked(AccessRecord oldRecord){
		long nowBackRef = oldRecord.backReference.incrementAndGet();

		if (nowBackRef == 1) {
			// it must be in LRUList
			synchronized (LRUList) {
				String removeDebug = LRUList.remove(oldRecord.serial);
				assert removeDebug.equals(idBytes);
			}
		}
	}

	public void increaseBackRef(AccessRecord fromCache, long newSerial) {

		rwLock.readLock().lock();

		assert this.accessRecord == null;
		this.accessRecord = fromCache;
		
		increaseBackRefUnlocked(accessRecord);

		fromCache.serial = newSerial;
		rwLock.readLock().unlock();
	}


}
