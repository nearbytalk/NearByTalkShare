package org.nearbytalk.test.identity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.ProtectedProxyMessage;
import org.nearbytalk.identity.AbstractMessage.MessageType;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.test.misc.RandomUtility;

import junit.framework.TestCase;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class ProtectedProxyMessageTest extends TestCase {

	public void testToJsonBasic() {

		AbstractMessage absMsg = new ProtectedProxyMessage(null,
				RandomUtility.randomUser(), MessageType.VOTE_TOPIC, null);

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		String asAbs = gson.toJson(absMsg);

		JsonElement element = gson.fromJson(asAbs, JsonElement.class);

		JsonObject obj = element.getAsJsonObject();

		assertTrue(obj.get(ProtectedProxyMessage.IS_PROTECTED_PROXY_KEY)
				.getAsBoolean());

		assertEquals(absMsg.getMessageType(),
				gson.fromJson(obj.get("messageType"), MessageType.class));
		
		ClientUserInfo deserializeUser=gson.fromJson(obj.get(AbstractMessage.SENDER_JSON_KEY), ClientUserInfo.class);
		
		assertEquals(absMsg.getSender(), deserializeUser);

	}

	public void testToJsonWithProperties() {

		Map<String, Object> properties = new HashMap<String, Object>();

		Set<String> propSet = new HashSet<String>();

		propSet.add("abc");
		propSet.add("bdc");
		propSet.add("ac");

		String key = "propSet";

		properties.put(key, propSet);

		AbstractMessage absMsg = new ProtectedProxyMessage(properties,
				RandomUtility.randomUser(), MessageType.VOTE_TOPIC, null);

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		String jsonString = gson.toJson(absMsg);

		JsonElement deserialized = gson.fromJson(jsonString, JsonElement.class);

		JsonObject obj = deserialized.getAsJsonObject();

		JsonElement propReadback = obj.get(key);

		Set<String> readBackSet = gson.fromJson(propReadback,
				new TypeToken<HashSet<String>>() {
				}.getType());

		assertEquals(propSet.size(), readBackSet.size());

		for (String string : readBackSet) {
			assertTrue(propSet.contains(string));
		}
	}
}
