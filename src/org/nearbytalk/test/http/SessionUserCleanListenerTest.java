package org.nearbytalk.test.http;

import org.nearbytalk.runtime.UniqueObject;

public class SessionUserCleanListenerTest extends LoginShareTest {
	
	@Override
	protected void setUp() throws Exception{
		super.setUp();
		this.embeddedHttpServer.setSessionTimeoutSeconds(1);
	}

	public void testShouldCleanUser() throws Exception {


		LoginStruct result = randomUserLoginWithCheck();
		
		result.basic.httpClient.getConnectionManager().shutdown();

		assertTrue(UniqueObject.getInstance().onlineUserInfoMap
				.containsExactly(result.loginUserInfo));

		Thread.sleep(4000);

		assertFalse(UniqueObject.getInstance().onlineUserInfoMap
				.containsExactly(result.loginUserInfo));


	}

}
