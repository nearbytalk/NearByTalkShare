package org.nearbytalk.test.identity;

import java.text.ParseException;
import java.util.Calendar;

import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.test.identity.VoteTopicMessageTest.VoteTopicStruct;
import org.nearbytalk.test.misc.RandomUtility;

import junit.framework.TestCase;

import com.google.gson.Gson;

public class AbstractTextMessageTest extends TestCase {

	private Gson gson = GsonThreadInstance.FULL_GSON.get();

	public void testTypedJsonDeserializePlainText() {

		PlainTextMessage plainTextMessage = RandomUtility
				.randomNoRefTextMessage(null);

		AbstractMessage deserialize = gson.fromJson(
				gson.toJson(plainTextMessage), AbstractMessage.class);

		assertTrue(deserialize instanceof PlainTextMessage);

		assertTrue(plainTextMessage.sameStrippedUser(deserialize));

	}

	public void testTypedJsonDeserializeVoteTopic() throws ParseException {

		VoteTopicStruct struct = VoteTopicMessageTest
				.generateVoteTopicJsonString();

		VoteTopicMessage voteTopic = new VoteTopicMessage(
				RandomUtility.randomIdBytesString(),
				RandomUtility.randomUser(), struct.jsonString, Calendar
						.getInstance().getTime(),0,0,0);
		
		AbstractMessage deserialize = gson.fromJson(
				gson.toJson(voteTopic), AbstractMessage.class);

		assertTrue(deserialize instanceof VoteTopicMessage);

		assertTrue(voteTopic.sameStrippedUser(deserialize));

	}

}
