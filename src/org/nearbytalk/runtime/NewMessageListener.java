package org.nearbytalk.runtime;

import java.util.Collection;
import java.util.EventListener;

import org.nearbytalk.identity.AbstractMessage;


/**
 * 
 * object who cares new message should implement this interface
 * and register to UnsavedMessageQueue
 * 
 * 
 */
public interface NewMessageListener extends EventListener{

	public static final String POLL_RESULT_LIST_KEY="POLL_RESULT_LIST_KEY";
	
	void newMessagePushed(AbstractMessage newMessage);
	
	void newMessagePushed(Collection<? extends AbstractMessage> newMessages);
}
