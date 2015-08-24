package org.nearbytalk.test.http;

import java.util.ArrayList;
import java.util.List;

import org.nearbytalk.http.TalkServlet;
import org.nearbytalk.http.TalkServlet.TextMessageChecker;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.ChatBuildMessage;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.identity.AbstractMessage.MessageType;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.service.MessageService;
import org.nearbytalk.service.ServiceInstanceMap;
import org.nearbytalk.test.misc.RandomUtility;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class TalkServletTest extends TalkServletTestShare {
	public void testTalkTextMessage() throws Exception {

		talkMessageWithCheck(new MessageCheckCallback<PlainTextMessage>() {

			@Override
			public PlainTextMessage createMessage(LoginStruct loginStruct) {
				return RandomUtility
						.randomNoRefTextMessage(loginStruct.loginUserInfo);
			}
		});

	}

	public void testChecker() {

		JsonObject object = new JsonObject();

		object.addProperty("plainText", "what i'm talking");

		object.addProperty("messageType", MessageType.PLAIN_TEXT.toString());

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		String textMessage = gson.toJson(object);

		TextMessageChecker checker = new TextMessageChecker();

		PlainTextMessage deserialize = gson.fromJson(textMessage,
				PlainTextMessage.class);

		assertNull(TextMessageChecker.check(deserialize));

	}

	public void testTalkChatBuildMessage() throws Exception {
		talkMessageWithCheck(new MessageCheckCallback<ChatBuildMessage>() {

			@Override
			public ChatBuildMessage createMessage(LoginStruct loginStruct) {
				List<ChatBuildMessage> list = RandomUtility
						.randomChatBuildMessage(loginStruct.loginUserInfo, 1);
				assert list.size() == 1;
				return list.get(0);
			}
		});

	}

	public void testTalkDepthChatBuildMessage() throws Exception {

		final LoginStruct outer[] = new LoginStruct[1];

		final List<ChatBuildMessage> firstOne = new ArrayList<ChatBuildMessage>();

		talkMessageWithCheck(new MessageCheckCallback<ChatBuildMessage>() {

			@Override
			public ChatBuildMessage createMessage(LoginStruct loginStruct) {
				outer[0] = loginStruct;
				List<ChatBuildMessage> list = RandomUtility
						.randomChatBuildMessage(loginStruct.loginUserInfo, 1);
				assert list.size() == 1;
				firstOne.add(list.get(0));
				return list.get(0);
			}
		});

		ChatBuildMessage secondMsg = new ChatBuildMessage(
				outer[0].loginUserInfo, RandomUtility.nextString(),
				firstOne.get(0));

		AccessResult result = httpAccess(TalkServlet.class, secondMsg,
				outer[0].basic.httpClient);

		MessageService service = ServiceInstanceMap.getInstance().getService(
				MessageService.class);

		AbstractMessage message = service.queryDetail(secondMsg.getIdBytes());

		assertNotNull(message);

		assertEquals(message,secondMsg);

	}

	public void testTalkVoteTopicMessage() throws Exception {

		talkVoteTopicMessageWithCheck();

	}

	public void testTalkVoteOfMeMessageSelf() throws Exception {
		talkVoteOfMeMessageSelfWithCheck();
	}

	public void testTalkVoteOfMeMessageOthers() throws Exception {
		talkVoteOfMeMessageOthersWithCheck();
	}
}
