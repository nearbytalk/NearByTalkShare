package org.nearbytalk.test.identity;

import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.runtime.UniqueObject;
import org.nearbytalk.runtime.OnlineUserInfoMap.LoginResult;
import org.nearbytalk.service.ClientUserInfoService;
import org.nearbytalk.service.ServiceInstanceMap;
import org.nearbytalk.test.misc.RandomUtility;

import junit.framework.TestCase;


public class ClientUserInfoServiceTest extends TestCase {

	ClientUserInfoService service = ServiceInstanceMap.getInstance()
			.getService(ClientUserInfoService.class);

	private void assumeOnlineUserInfoMapOk() {
		assertTrue(UniqueObject.getInstance().onlineUserInfoMap
				.stateConsist());
	}

	public ClientUserInfo randomLoginWithCheck() throws Exception {

		assumeOnlineUserInfoMapOk();

		ClientUserInfo random = RandomUtility.randomUser();

		LoginResult loginResult = service.login(null, random);

		assumeOnlineUserInfoMapOk();

		assertTrue(loginResult.successed());

		assertEquals(loginResult.loginedUserInfo, random);

		assertTrue(UniqueObject.getInstance().onlineUserInfoMap
				.containsExactly(random));

		return random;

	}

	public void testLoginSuccess() throws Exception {
		randomLoginWithCheck();
	}

	public void testLogoutCleanOnlineUserMap() throws Exception {

		ClientUserInfo logined = randomLoginWithCheck();

		service.logout(logined);

		assumeOnlineUserInfoMapOk();

		assertFalse(UniqueObject.getInstance().onlineUserInfoMap
				.containsExactly(logined));

		assumeOnlineUserInfoMapOk();

	}

	public void testRenameLogin() throws Exception {

		ClientUserInfo logined = randomLoginWithCheck();
		assumeOnlineUserInfoMapOk();

		ClientUserInfo backup = new ClientUserInfo(logined.getUserName(),
				logined.getIdBytes());

		backup.setDescription(logined.getDescription());

		logined.setUserName(RandomUtility.nextString());

		LoginResult renamed = service.login(backup, logined);

		assumeOnlineUserInfoMapOk();

		assertNotNull(renamed.loginedUserInfo);

		assertEquals(renamed.loginedUserInfo.getUserName(),
				logined.getUserName());

		assertEquals(renamed.loginedUserInfo.getIdBytes(), backup.getIdBytes());

		assertTrue(UniqueObject.getInstance().onlineUserInfoMap
				.containsExactly(logined));
		assertFalse(UniqueObject.getInstance().onlineUserInfoMap
				.containsExactly(backup));

	}

	public void testEditDescription() throws Exception {

		ClientUserInfo logined = randomLoginWithCheck();

		ClientUserInfo changeDescription = new ClientUserInfo(
				logined.getUserName(), logined.getIdBytes());

		changeDescription.setDescription(RandomUtility.nextString());

		LoginResult retry = service.login(logined, changeDescription);

		assertTrue(retry.successed());

		assertNotNull(retry.loginedUserInfo);

		assertTrue(ClientUserInfo.weakSame(changeDescription,
				retry.loginedUserInfo));

		assertEquals(changeDescription.getDescription(),
				retry.loginedUserInfo.getDescription());
	}
}
