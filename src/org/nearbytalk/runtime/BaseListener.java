package org.nearbytalk.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import org.eclipse.jetty.continuation.Continuation;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.query.PollQuery.PollType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class BaseListener implements NewMessageListener {

	private final ArrayBlockingQueue<AbstractMessage> queue = new ArrayBlockingQueue<AbstractMessage>(
			1000);

	private Continuation continuation;
	
	private static final Logger log=LoggerFactory.getLogger(BaseListener.class);
	
	private final PollType pollType;
	
	protected BaseListener(PollType pollType){
		this.pollType=pollType;
	}
	
	

	public PollType getPollType() {
		return pollType;
	}



	/**
	 * use this function we can detect overlap request and graceful
	 * flush messages to previous request-response session 
	 * 
	 * @param newContinuation
	 */
	public boolean resetContinuation(Continuation newContinuation) {
		synchronized (this) {
			if (inPollingState()) {
				if (newContinuation==null) {
					log.trace("already wait on a continuation,force flush");
					doFlush();				
					log.trace("attached continuation cleared");
					return true;
				}else{
					log.trace("new continuation rejected");
					return false;
				}
			}else{
				this.continuation = newContinuation;
				log.trace("no attached continuation or just expired,new one attached");
				return true;
			}
		}
	}

	/**
	 * poll all pending messages out,this action is atomic,
	 * queue will be cleared
	 * 
	 * @return
	 */
	public List<AbstractMessage> pollAll() {

		ArrayList<AbstractMessage> ret = new ArrayList<AbstractMessage>();

		queue.drainTo(ret);

		log.debug("poll all messages {}",ret);
		
		return ret;

	}
	
	private boolean inPollingState(){
		return continuation != null
				&& continuation.isSuspended();
	}
	
	private void doFlush(){
		
		ArrayList<AbstractMessage> toFlush = new ArrayList<AbstractMessage>();
		
		queue.drainTo(toFlush);
		
		log.trace("will flush to polling client {}",toFlush);

		continuation.setAttribute(
				POLL_RESULT_LIST_KEY, toFlush);

		continuation.resume();
	}

	abstract protected boolean shouldFlush(ArrayBlockingQueue<? extends AbstractMessage> queue);

	@Override
	public void newMessagePushed(AbstractMessage newMessage) {
		
		log.trace("new message received {}",newMessage);
		
		if (queue.remainingCapacity() == 0) {

			// if another thread just call pollAll,
			// this may leads 1 message lose.
			// but that is not important
			AbstractMessage givesUp=queue.poll();
			
			log.debug("no more capacity ,gives up {} ",givesUp);
		}

		queue.add(newMessage);
		
		//must synchronized by this because continuation may be changed 
		//by other thread
		synchronized (this) {

			if (shouldFlush(queue) && inPollingState()) {
				
				log.trace("should flush queue to client");
				doFlush();
				return;
			}
		}


	}

	@Override
	public void newMessagePushed(Collection<? extends AbstractMessage> newMessages) {
		for (AbstractMessage textMessage : newMessages) {
			newMessagePushed(textMessage);
		}
	}
	
	/**
	 * create a listener by type .only EAGER is treated as create eager
	 * listener.other type will create lazy listener
	 * @param type
	 * @return
	 */
	public static BaseListener createByType(PollType type){

		return type == PollType.EAGER ? new EagerListener()
				: new LazyListener() ;

	}

}
