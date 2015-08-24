package org.nearbytalk.test.http;

import org.nearbytalk.http.EmbeddedHttpServer;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.test.TestUtil;

import junit.framework.TestCase;


public class EmbeddedHttpServerTest extends TestCase {

	public void testServerStartIn10S() throws Exception {
		
		TestUtil.resetUniqueObjectIfNessesery();
		
		EmbeddedHttpServer server = new EmbeddedHttpServer();

		server.start();

		for (int i = 0; i < 5; ++i) {

			if (!server.isStarted()) {
				Thread.sleep(2000);
			}

		}

		assertTrue(server.isStarted());

		server.stop();

	}
	
	public void testUpdateGlobalIsRunning() throws Exception{

		TestUtil.resetUniqueObjectIfNessesery();
		
		EmbeddedHttpServer server=new EmbeddedHttpServer();
		
		assertFalse(Global.getInstance().httpServerInfo.isRunning);
		
		server.start();
		for (int i = 0; i < 5; ++i) {

			if (!Global.getInstance().httpServerInfo.isRunning) {
				Thread.sleep(2000);
			}

		}
		
		assertTrue(Global.getInstance().httpServerInfo.isRunning);

		server.stop();
		
		assertFalse(Global.getInstance().httpServerInfo.isRunning);
		
	}

}
