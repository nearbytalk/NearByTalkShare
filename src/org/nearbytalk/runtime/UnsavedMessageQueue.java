package org.nearbytalk.runtime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.BaseUserInfo;
import org.nearbytalk.identity.VoteOfMeMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteBusyException;

/**
 * a cache layer between app and datastore. contains unsaved message(to merge
 * multi-thread write into batch) and a short list of newly saved message (to
 * provide "newest message" function) <br>
 * 
 * all writes comes to {@link #pushMessage(AbstractMessage)} will be buffered in
 * unsavedQueue, unsavedQueue will be flashed as following condition :<br>
 * 
 * 1)timeout expired <br>
 * 2)size limit expired <br>
 * 
 * when flushing , message will be stored to datastore and rolled into
 * savedQueue.
 * 
 */
public class UnsavedMessageQueue extends TimerTask {

	private final ConcurrentLinkedQueue<AbstractMessage> unsavedQueue = new ConcurrentLinkedQueue<AbstractMessage>();

	private final ConcurrentLinkedQueue<AbstractMessage> savedQueue = new ConcurrentLinkedQueue<AbstractMessage>();

	/**
	 * VoteTopicMessage needs update logic ,do not in unsavedQueue
	 * 
	 */
	private final ConcurrentHashMap<String, VoteTopicMessage> updateQueue = new ConcurrentHashMap<String, VoteTopicMessage>();
	
	private final AtomicBoolean stopped=new AtomicBoolean(false);

	/**
	 * internal lock for consist access
	 */
	private final Object unionLock = new Object();

	/**
	 * how many messages will be kept before flushing to datastore
	 */
	private final int expiredMessageLength;

	/**
	 * how long will message kept before flushing to datastore
	 */
	private final int expiredMillionSeconds;

	private int losingMessages = 0;
	
	private Object waitObject = new Object();

	/**
	 * 
	 * there is such contuation times save speed can not catch up pushing
	 */
	private int continuationTimeout = 0;

	private Timer timer = new Timer(true);

	private static Logger log = LoggerFactory
			.getLogger(UnsavedMessageQueue.class);

	public UnsavedMessageQueue(int expiredMillionSeconds,
			int expiredMessageLength, IMessageCache messageCache)
			{
		this.messageCache=messageCache;
		this.expiredMillionSeconds = expiredMillionSeconds;
		timer.schedule(this, 0, expiredMillionSeconds);
		this.expiredMessageLength = expiredMessageLength;

		if (expiredMessageLength < 0) {
			throw new IllegalArgumentException(
					"expired message length should >=0 ");

		}

	}

	public void preload(List<AbstractMessage> preload) {
		// load newest message into queue to feed init request
		ListIterator<AbstractMessage> iterator = preload.listIterator(Math.max(
				preload.size(), 0));

		while (iterator.hasPrevious()) {
			AbstractMessage message = iterator.previous();
			savedQueue.add(message);
		}
	}


	

	/**
	 * add new message to queue. if queue is full , head element will be popped
	 * all NewMessageListener will be notified
	 * 
	 * @param newMessage
	 * @return
	 */
	public void addUnsaved(AbstractMessage newMessage) {
		assert newMessage!=null;
		
		if (!stopped.compareAndSet(false, false)) {
			log.error("add unsaved with stopped queue, do nothing");
			return;
		}


		if (newMessage instanceof VoteOfMeMessage) {
			// update VoteTopicMessage
			updateQueue.put(newMessage.getReferenceIdBytes(),
					(VoteTopicMessage) newMessage.getReferenceMessage());
		}
			
		unsavedQueue.add(newMessage);

	}
	

	private int batchSave(int maxSave) {
		List<AbstractMessage> toSave = new ArrayList<AbstractMessage>();

		// we should synchronized on saved queue
		// this assume new registered listener will not receive
		// segmented history.
		// for example :

		int i = 0;
		// thread1 :listener registered -> receive savedQueue history
		// timer thread: unsaedQueue message polled
		// thread1 : listener ->receive unsavedQueue history,without polled
		// message
		synchronized (unionLock) {

			for (; i < maxSave; ++i) {
				AbstractMessage rotate = unsavedQueue.poll();

				if (rotate == null) {
					break;
				}

				log.trace("rolling message {} ", rotate);

				// there is chance datastore save failed bug message
				// is already put into savedQueue, we ignore this
				// treat these "save failed" message as "saved"
				savedQueue.poll();
				savedQueue.add(rotate);
				toSave.add(rotate);
			}

			log.debug("{} messages rolled from unsaved queue to saved queue", i);
		}

		try {

			messageCache.getDataStore().saveMessage(toSave);
		} catch (Exception e) {
			log.error("batch save failed", e);
		}
		
		for (AbstractMessage abstractMessage : toSave) {
			messageCache.releaseMessage(abstractMessage);
		}
		
		

		return i;
	}

	@Override
	public void run() {
		
		log.debug("running save message progress..");

		saveUnsavedMessages();

		updateVoteTopicMessages();
		
		synchronized (waitObject) {
			flushed=true;
			waitObject.notifyAll();
		}
	}
	
	private void saveUnsavedMessages(){
		// do not care thread change. if isEmpty return true
		// but unsavedQueue is modified immediately , flush will
		// be delayed to next time
		if (!unsavedQueue.isEmpty()) {

			boolean oncetimeout = false;
			long usedTime = 0;

			int batchCount = 0;
			final int batchNumber = 1000;

			for (; usedTime < expiredMillionSeconds * 2; ++batchCount) {

				int savedNumber = batchSave(batchNumber);

				usedTime = System.currentTimeMillis()
						- super.scheduledExecutionTime();

				oncetimeout = usedTime > expiredMillionSeconds;

				if (savedNumber < batchNumber) {
					break;
				}
			}

			if (!oncetimeout) {
				log.trace("batch saving in time");
				continuationTimeout = 0;
				return;
			}

			log.debug("batch saving out of time");

			if (continuationTimeout++ > 10) {
				// giving up caching queue

				log.error(
						"try {} times with batch save {} messages still not finished ,giving up cache queue",
						batchCount, batchNumber);
				int thisLosing = 0;
				while (unsavedQueue.poll() != null) {
					++thisLosing;
				}
				log.error("losing {} messages ", thisLosing);

				losingMessages += thisLosing;
			}
		}	
	}
	
	private void updateVoteTopicMessages(){
		Set<String> keys = updateQueue.keySet();
		// only

		LinkedList<VoteTopicMessage> updateVoteTopics = new LinkedList<VoteTopicMessage>();

		for (String key : keys) {
			updateVoteTopics.add(updateQueue.remove(key));
		}
		try {
			messageCache.getDataStore().updateVoteTopicMessages(updateVoteTopics);
		} catch (SQLiteBusyException e) {
			//TODO delay to next update ?
			log.error("db busy {}",e);
		}

		for (AbstractMessage abstractMessage : updateVoteTopics) {
			messageCache.releaseMessage(abstractMessage);
		}
	}
	
	private final IMessageCache messageCache;

	public int getLosingMessages() {
		return losingMessages;
	}

	public int getExpiredMessageLength() {
		return expiredMessageLength;
	}

	public int getExpiredMillionSeconds() {
		return expiredMillionSeconds;
	}

	/**
	 * checking savedQueue and unsavedQueue not explores, for test only.
	 * unsavedQueue may not strict less than
	 * {@link UnsavedMessageQueue#expiredMessageLength} wait over
	 * {@link UnsavedMessageQueue#expiredMillionSeconds} should assume this.
	 * 
	 * @return
	 */
	public boolean stateConsist() {
		return savedQueue.size() < expiredMessageLength
				&& unsavedQueue.size() < expiredMessageLength;
	}

	public boolean deleteUnsaved(BaseUserInfo user, String idBytes) {
		synchronized (unionLock) {
			//prevent save thread
			
			Iterator<AbstractMessage> it=unsavedQueue.iterator();
			
			while (it.hasNext()) {
				AbstractMessage msg=it.next();
				
				if (msg.getSender().equals(user) && msg.getIdBytes().equals(idBytes)) {
					
					it.remove();
					return true;
				}
			}
			
			return false;
		}
	}
	
	
	/**
	 * push saved+unsaved messages to listener(internal queue will not be
	 * modified)
	 * 
	 * @param listener
	 */
	public void pushAllMessages(NewMessageListener listener) {

		synchronized (unionLock) {

			listener.newMessagePushed(savedQueue);
			listener.newMessagePushed(unsavedQueue);
		}
	}

	public IMessageCache getMessageCache() {
		return messageCache;
	}
	
	private boolean flushed=false;
	
	public void flushAndWait() throws InterruptedException{
		
		log.info("flushing Unsaved message queue");
		
		synchronized (waitObject) {
			
			if (!stopped.compareAndSet(false, false)) {
				log.info("already stopped , will not flushAndWait, just return");
				return;
			}
			
			flushed=false;

			timer.schedule(new TimerTask() {
				//must use new TimerTask. Timer didn't allow duplicate TimerTask 
				@Override
				public void run() {

					log.info("Unsaved message run triggered");
					UnsavedMessageQueue.this.run();
				}
			}, 0);
			
			if(!flushed){
					
				log.info("not flushed ,waiting ");
				waitObject.wait();
				log.info("notified flushed ok");
			}
		}
	}
	
	/**
	 * stop background timer thread saving action (also recycle that thread's datastore connection), will block until saving complete
	 * @throws InterruptedException 
	 * 
	 */
	public void stop() throws InterruptedException{

		synchronized (waitObject) {
			
			boolean firstCallStop=stopped.compareAndSet(false, true);
			
			if (!firstCallStop) {
				//already called
				return;
			}


			flushed=false;

			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					UnsavedMessageQueue.this.run();

					timer.cancel();
					
					messageCache.getDataStore().threadRecycle();
					
				}
			}, 0);
			if(!flushed){

				log.info("not flushed ,waiting ");
				waitObject.wait();
				log.info("notified flushed ok");
			}
		}
	}
}
