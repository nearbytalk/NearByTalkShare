package org.nearbytalk.test.runtime;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.OnlineUserInfoMap;
import org.nearbytalk.runtime.OnlineUserInfoMap.LoginResult;
import org.nearbytalk.test.misc.RandomUtility;

import junit.framework.TestCase;


public class OnlineUserInfoMapTest extends TestCase{

	public void testPutReturnSame() {

		ClientUserInfo toPut = RandomUtility.randomUser();

		OnlineUserInfoMap map = new OnlineUserInfoMap();

		LoginResult putBack = map.putUserInfo(toPut);

		assertEquals(putBack.loginedUserInfo, toPut);

	}
	
	public void testIdNotClash() throws UnsupportedEncodingException{
		ClientUserInfo first=RandomUtility.randomUser();
		
		ClientUserInfo same=new ClientUserInfo(new String(first.getUserName().getBytes(Global.UTF8_ENCODING)), first.getIdBytes());
		
		OnlineUserInfoMap map=new OnlineUserInfoMap();
		
		LoginResult firstResult=map.putUserInfo(first);
		
		assertFalse(firstResult.failIsIdClash);
		
		assertEquals(first,firstResult.loginedUserInfo);
		
		LoginResult secondResult=map.putUserInfo(same);
		
		assertNotNull(secondResult.loginedUserInfo);
		
		assertFalse(secondResult.failIsIdClash);
	}

	public void testNameClashReturnNew() {

		ClientUserInfo comesFirst = RandomUtility.randomUser();

		ClientUserInfo nameClash = RandomUtility.randomUser();

		nameClash.setUserName(comesFirst.getUserName());

		ClientUserInfo backUp = new ClientUserInfo(nameClash.getUserName(),
				nameClash.getIdBytes());

		OnlineUserInfoMap map = new OnlineUserInfoMap();

		map.putUserInfo(comesFirst);

		LoginResult readBack = map.putUserInfo(nameClash);
		// should generate serial userName

		assertNotNull(readBack);

		assertNotSame(nameClash, readBack);

		assertEquals(nameClash.getIdBytes(), backUp.getIdBytes());

		assertFalse(readBack.loginedUserInfo.getUserName().equals(nameClash.getUserName()));

	}

	public void testLoginLogout() {

		OnlineUserInfoMap map = new OnlineUserInfoMap();

		assertTrue(map.stateConsist());

		LoginResult readBack = map.putUserInfo(RandomUtility.randomUser());

		assertTrue(map.stateConsist());

		assertTrue(map.containsExactly(readBack.loginedUserInfo));

		assertTrue(map.stateConsist());

		map.remove(readBack.loginedUserInfo);

		assertTrue(map.stateConsist());
	}

	public void testLoginLogoutThread() throws InterruptedException {

		final ConcurrentLinkedQueue<ClientUserInfo> deleteList = new ConcurrentLinkedQueue<ClientUserInfo>();

		final OnlineUserInfoMap map = new OnlineUserInfoMap();

		final List<Object> finished = new ArrayList<Object>();

		Thread loginThread = new Thread(new Runnable() {

			@Override
			public void run() {

				for (int i = 0; i < 1000; ++i) {
					ClientUserInfo logined = RandomUtility.randomUser();

					assertTrue(map.stateConsist());
					map.putUserInfo(logined);
					deleteList.add(logined);

					assertTrue(map.stateConsist());
				}

				finished.add(OnlineUserInfoMapTest.this);

			}
		});

		Thread logoutThread = new Thread(new Runnable() {

			@Override
			public void run() {

				while (finished.isEmpty()) {
					ClientUserInfo toLogout = deleteList.poll();
					if (toLogout != null) {

						assertTrue(map.containsExactly(toLogout));

						assertTrue(map.stateConsist());
						map.remove(toLogout);

						assertTrue(map.stateConsist());
					}
				}

			}
		});

		Thread checkThread = new Thread(new Runnable() {

			@Override
			public void run() {

				while (finished.isEmpty()) {
					
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					assertTrue(map.stateConsist());
				}

			}
		});

		checkThread.start();
		logoutThread.start();
		loginThread.start();

		checkThread.join();
		logoutThread.join();
		loginThread.join();

	}

	public void testContainsExtactlyFalse() {

		OnlineUserInfoMap map = new OnlineUserInfoMap();

		map.putUserInfo(RandomUtility.randomUser());

		assertFalse(map.containsExactly(RandomUtility.randomUser()));

	}

	public void testRemoveThrow() {

		OnlineUserInfoMap map = new OnlineUserInfoMap();

		map.putUserInfo(RandomUtility.randomUser());

		try {

			map.remove(RandomUtility.randomUser());

			assertTrue("should not throw", true);
			
			return;

		} catch (Exception e) {
			
			
			fail("should not throw");
			return;
		}

		

	}

	public void testExtactlySameLogin() {

		OnlineUserInfoMap map = new OnlineUserInfoMap();

		ClientUserInfo first = RandomUtility.randomUser();

		ClientUserInfo backup = first.clone();
		
		backup.setDescription(first.getDescription());

		LoginResult readBack = map.putUserInfo(first);
		
		LoginResult second=map.putUserInfo(first);

		assertEquals(backup, readBack.loginedUserInfo);
		assertEquals(backup, first);
		assertEquals(second.loginedUserInfo, first);

	}
}
