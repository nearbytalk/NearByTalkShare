package org.nearbytalk.test.identity;

import java.io.IOException;

import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.exception.FileShareException;
import org.nearbytalk.http.ErrorResponse;
import org.nearbytalk.http.TalkServlet;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.identity.AbstractMessage.MessageType;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.test.misc.RandomUtility;

import junit.framework.TestCase;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class PlainTextMessageTest extends TestCase {

	public void testDigest() {

		PlainTextMessage randomMessage = new PlainTextMessage(
				RandomUtility.randomUser(), RandomUtility.nextString());

		assertNotNull(randomMessage.getIdBytes());
	}
	
	public void testSetReferenceMessageLater() throws BadReferenceException{
		
		PlainTextMessage leaf=RandomUtility.randomNoRefTextMessage(null);
		PlainTextMessage random=RandomUtility.randomNoRefTextMessage(null);
		
		assertEquals(1,random.getReferenceDepth());
		
		random.setReferenceMessageLater(leaf);

		assertEquals(2,random.getReferenceDepth());
	}

	public void testClientSideReferenceIdDigest() {

		PlainTextMessage randomMessage = new PlainTextMessage(
				RandomUtility.randomUser(), RandomUtility.nextString());
		// TODO use json deserialize construct
		/*
		 * TextMessage referenceOthers = new TextMessage(
		 * RandomUtility.randomUser(), "sadjfa", randomMessage.getIdBytes());
		 * 
		 * assertNotNull(referenceOthers.getIdBytes());
		 */

	}

	public void testChecker() {

		JsonObject obj = new JsonObject();

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		obj.addProperty("plainText", "");

		PlainTextMessage deserialize = gson.fromJson(obj,
				PlainTextMessage.class);

		assertTrue(TalkServlet.TextMessageChecker.check(deserialize) == ErrorResponse.INVALID_MESSAGE_TYPE);

		obj.addProperty("messageType", "PLAIN_TEXT");

		deserialize = gson.fromJson(obj, PlainTextMessage.class);

		assertTrue(TalkServlet.TextMessageChecker.check(deserialize) == ErrorResponse.MESSAGE_CANNOT_EMPTY);

	}

	public void testDifferentDateToDifferentId() {

		// TODO
	}

	public void testEquals() {

		PlainTextMessage origin = RandomUtility.randomNoRefTextMessage(null);

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		String toJson = gson.toJson(origin);

		PlainTextMessage toCompare = gson.fromJson(toJson,
				PlainTextMessage.class);

		assertTrue(origin.sameStrippedUser(toCompare));

	}

	public void testSameIgnoreUserIdBytes() {

		PlainTextMessage origin = RandomUtility.randomNoRefTextMessage(null);

		Gson gson = GsonThreadInstance.STRIP_USER_ID_BYTES_GSON.get();

		String toJson = gson.toJson(origin);

		PlainTextMessage toCompare = gson.fromJson(toJson,
				PlainTextMessage.class);

		assertTrue(origin.sameStrippedUser(toCompare));
	}

	public void testJsonToInstance() {
		JsonObject object = new JsonObject();

		object.addProperty("plainText", "what i'm talking");

		object.addProperty("messageType", MessageType.PLAIN_TEXT.toString());

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		String string = gson.toJson(object);

		PlainTextMessage fromJson = gson.fromJson(string,
				PlainTextMessage.class);

		assertNotNull(fromJson);

	}
	
	public <T extends AbstractMessage> void checkFromJsonWithTopics  (Class<T> clazz){
		
		PlainTextMessage randomMessage=RandomUtility.randomNoRefTextMessage(null);
		
		Gson gson=GsonThreadInstance.FULL_GSON.get();
		
		String string=gson.toJson(randomMessage);
		
		T fromJsonMessage=gson.fromJson(string, clazz);
		
		assertNotNull(fromJsonMessage);
		
		assertTrue(randomMessage.sameStrippedUser(fromJsonMessage));
		
		assertNotNull(randomMessage.getTopics());
		
		assertTrue(randomMessage.getTopics().size()>0);
		
		assertEquals(randomMessage.getTopics(), fromJsonMessage.getTopics());
		
	}
	
	public void testFromJsonWithTopicesBaseClass(){
		
		checkFromJsonWithTopics(PlainTextMessage.class);
		checkFromJsonWithTopics(AbstractMessage.class);
	}

	private PlainTextMessage longMessage() {
		String veryLong = RandomUtility.nextString();
		
		int onceLong=veryLong.length();

		for (int i = 0; i < Global.BROADCAST_MESSAGE_LIMIT / onceLong
				+ 1; ++i) {
			veryLong = veryLong + RandomUtility.nextString();
		}

		assertTrue(veryLong.length() > Global.BROADCAST_MESSAGE_LIMIT);

		PlainTextMessage random = new PlainTextMessage(
				RandomUtility.randomUser(), veryLong);

		return random;

	}

	public void testFileShareNotLimitContent() throws FileShareException, IOException, BadReferenceException {

		PlainTextMessage random = longMessage();

		random.setReferenceMessageLater(RandomUtility.randomUploadFile());

		assertTrue(random.asPlainText().length() > Global.BROADCAST_MESSAGE_LIMIT);
	}

	public void testBroadcastLimitContent() throws BadReferenceException {

		PlainTextMessage random = longMessage();

		random.setReferenceMessageLater(RandomUtility
				.randomNoRefTextMessage(null));

		assertTrue(random.asPlainText().length() <= Global.BROADCAST_MESSAGE_LIMIT);

	}

}
