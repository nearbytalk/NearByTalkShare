package org.nearbytalk.test.identity;

import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.test.misc.RandomUtility;
import org.nearbytalk.util.DigestUtility;

import junit.framework.TestCase;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ClientUserInfoTest extends TestCase {

	public void testClientUserInfoEquals() {

		ClientUserInfo info1 = RandomUtility.randomUser();

		ClientUserInfo info2 = new ClientUserInfo(info1.getUserName(),
				info1.getIdBytes());

		info2.setDescription(info1.getDescription());

		assertTrue(info1.equals(info2));

	}

	public void testWeakSame() {

		{
			ClientUserInfo info1 = RandomUtility.randomUser();
			ClientUserInfo info2 = RandomUtility.randomUser();

			assertFalse(ClientUserInfo.weakSame(info1, info2));
		}

		{
			ClientUserInfo info1 = RandomUtility.randomUser();

			ClientUserInfo info2 = new ClientUserInfo(info1.getUserName(),
					info1.getIdBytes());

			assertFalse(info1.getDescription().equals(info2.getDescription()));

			assertTrue(ClientUserInfo.weakSame(info1, info2));
		}
	}

	public void testClone() {
		ClientUserInfo first = RandomUtility.randomUser();

		ClientUserInfo second = first.clone();

		assertTrue(first != second);

		assertEquals(first, second);

		JsonObject obj = new JsonObject();
		obj.addProperty("userName", RandomUtility.nextString());
		obj.addProperty("idBytes", DigestUtility
				.byteArrayToHexString(DigestUtility.oneTimeDigest(RandomUtility
						.nextString())));

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		ClientUserInfo deserializ = gson.fromJson(obj, ClientUserInfo.class);
		
		ClientUserInfo cloned=deserializ.clone();
		
		assertEquals(deserializ, cloned);
		
	}

}
