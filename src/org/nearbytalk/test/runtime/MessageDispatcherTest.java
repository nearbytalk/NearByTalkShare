package org.nearbytalk.test.runtime;

import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.runtime.MessageDispatcher;
import org.nearbytalk.test.misc.RandomUtility;

import junit.framework.TestCase;


public class MessageDispatcherTest extends TestCase {
	

	

	public void testOnlineMessageNotify() throws Exception {
		TestNewMessageListener listener = new TestNewMessageListener();

		MessageDispatcher dispatcher = new MessageDispatcher();

		dispatcher.registerListener(listener);

		AbstractMessage shouldbe = RandomUtility.randomNoRefTextMessage(null);

		dispatcher.pushMessage(shouldbe);

		assertEquals(listener.list.size() , 1);

		assertEquals(shouldbe, listener.list.get(0));

	}
}
