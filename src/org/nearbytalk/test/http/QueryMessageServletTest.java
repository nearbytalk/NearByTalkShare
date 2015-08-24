package org.nearbytalk.test.http;

import java.io.IOException;

import org.nearbytalk.http.QueryMessageServlet;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.BaseUserInfo;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.query.MessageQuery;
import org.nearbytalk.query.SearchType;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.runtime.MemoryMessageCache;
import org.nearbytalk.runtime.UniqueObject;
import org.nearbytalk.service.MessageService;
import org.nearbytalk.service.ServiceInstanceMap;
import org.nearbytalk.test.misc.RandomUtility;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class QueryMessageServletTest extends TalkServletTestShare {
	
	public static void assertUserIdBytesStripped(BaseUserInfo userInfo){
		assertNull(userInfo.getIdBytes());
	}

	public void testQueryFuzzy() throws Exception {

		LoginStruct loginStruct = randomUserLoginWithCheck();

		// TODO use talk servlet to talk

		MessageService service = ServiceInstanceMap.getInstance().getService(
				MessageService.class);

		PlainTextMessage msg = RandomUtility
				.randomNoRefTextMessage(loginStruct.loginUserInfo);

		service.talk(msg);

		// assume message is flashed to datastore

		MemoryMessageCache cache = (MemoryMessageCache) UniqueObject
				.getInstance().getMessageCache();

		cache.getUnsavedMessageQueue().flushAndWait();

		MessageQuery query = new MessageQuery();

		query.searchType = SearchType.FUZZY;

		query.keywords = msg.asPlainText();

		AccessResult result = httpAccess(QueryMessageServlet.class, query, loginStruct.basic.httpClient);

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		JsonElement resultJson = gson.fromJson(result.response,
				JsonElement.class);

		assertTrue(resultJson.isJsonObject());

		JsonObject obj = resultJson.getAsJsonObject();

		assertTrue(obj.has("success"));

		assertTrue(obj.get("success").getAsBoolean());

		assertTrue(obj.has("detail"));

		JsonArray array = obj.get("detail").getAsJsonArray();

		AbstractMessage[] resultList = gson.fromJson(array,
				AbstractMessage[].class);

		assertNotNull(resultList);

		boolean shouldSame = false;

		for (AbstractMessage message : resultList) {
			assertNotNull(message.getSender());
			assertNotNull(message.getSender().getUserName());
			assertUserIdBytesStripped(message.getSender());

			if (message.sameStrippedUser(msg)) {
				shouldSame = true;
			}

		}

		assertTrue(shouldSame);

	}

	public void testQueryTopicExactly() throws Exception {

		talkMessageWithCheck(new MessageCheckCallback<PlainTextMessage>() {

			@Override
			public PlainTextMessage createMessage(LoginStruct loginStruct) {
				return RandomUtility
						.randomNoRefTextMessage(loginStruct.loginUserInfo);
			}

			@Override
			public void postCheckFromService(PlainTextMessage talk)
					throws InterruptedException, IllegalStateException,
					IOException {

				assertTrue(!talk.getTopics().isEmpty());
				MemoryMessageCache cache = (MemoryMessageCache) UniqueObject
						.getInstance().getMessageCache();

				cache.getUnsavedMessageQueue().flushAndWait();

				// TODO currently we don't query the unsaved message queue,
				// so can not query message just talked
				
				LoginStruct struct=randomUserLogin();

				for (String topic : talk.getTopics()) {

					MessageQuery query = new MessageQuery();
					query.searchType = SearchType.TOPIC_EXACTLY;
					query.keywords = topic;

					AccessResult result = httpAccess(QueryMessageServlet.class,
							query, struct.basic.httpClient);

					Gson gson = GsonThreadInstance.FULL_GSON.get();

					JsonElement jsonElement = gson.fromJson(result.response,
							JsonElement.class);

					JsonObject object = jsonElement.getAsJsonObject();

					assertTrue(object.get("success").getAsBoolean());

					JsonElement detailElement = object.get("detail");

					JsonArray array = detailElement.getAsJsonArray();

					assertTrue(array.size() > 0);

					PlainTextMessage fromJsonMessage = gson.fromJson(
							array.get(0), PlainTextMessage.class);
					
					assertUserIdBytesStripped(fromJsonMessage.getSender());

					assertTrue(fromJsonMessage.sameStrippedUser(talk));

				}
			}

		});

	}

	public void testQueryDetail() throws Exception {

		talkMessageWithCheck(new MessageCheckCallback<PlainTextMessage>() {

			@Override
			public PlainTextMessage createMessage(LoginStruct loginStruct) {
				return RandomUtility
						.randomNoRefTextMessage(loginStruct.loginUserInfo);
			}

			@Override
			public void postCheckFromService(PlainTextMessage talk)
					throws IllegalStateException, IOException {
				// TODO check dependency

				MessageQuery query = new MessageQuery();

				query.searchType = SearchType.EXACTLY;
				query.keywords = talk.getIdBytes();
				
				LoginStruct struct=randomUserLogin();

				AccessResult result = httpAccess(QueryMessageServlet.class,
						query, struct.basic.httpClient);

				Gson gson = GsonThreadInstance.FULL_GSON.get();

				JsonElement jsonElement = gson.fromJson(result.response,
						JsonElement.class);

				JsonObject object = jsonElement.getAsJsonObject();

				assertTrue(object.get("success").getAsBoolean());

				JsonElement detailElement = object.get("detail");

				JsonArray array = detailElement.getAsJsonArray();

				PlainTextMessage fromJsonMessage = gson.fromJson(array.get(0),
						PlainTextMessage.class);

				assertUserIdBytesStripped(fromJsonMessage.getSender());

				assertTrue(fromJsonMessage.sameStrippedUser(talk));
			}

		});

	}

}
