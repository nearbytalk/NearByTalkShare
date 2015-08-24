package org.nearbytalk.test.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.runtime.NewMessageListener;


public class TestNewMessageListener implements NewMessageListener {

	public List<AbstractMessage> list = new ArrayList<AbstractMessage>();

	@Override
	public void newMessagePushed(AbstractMessage newMessage) {
		list.add(newMessage);
	}

	@Override
	public void newMessagePushed(
			Collection<? extends AbstractMessage> newMessages) {
		list.addAll(newMessages);
	}

}
