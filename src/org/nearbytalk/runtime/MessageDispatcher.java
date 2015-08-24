package org.nearbytalk.runtime;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nearbytalk.identity.AbstractMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MessageDispatcher {

	private static Logger log = LoggerFactory
			.getLogger(MessageDispatcher.class);

	/**
	 * if pushHistory is true, that history message will be pushed
	 * 
	 * @param listener
	 * @param pushHistory
	 */
	public void registerListener(NewMessageListener listener) {
		listeners.add(listener);
		// flash online info to it

		log.debug("listener registered ");
	}

	public void unregisterListener(NewMessageListener listener) {
		log.debug("listener unregistered");
		listeners.remove(listener);
	}

	private void notifyListeners(AbstractMessage newMessage) {

		for (NewMessageListener listener : listeners) {
			listener.newMessagePushed(newMessage);
		}

	}

	/**
	 * listeners to receive new message notify
	 */
	private ConcurrentHashSet<NewMessageListener> listeners = new ConcurrentHashSet<NewMessageListener>();

	public void pushMessage(AbstractMessage newMessage) {

		log.debug("new message pushed :{}, notify listeners", newMessage);

		notifyListeners(newMessage);

	}

}
