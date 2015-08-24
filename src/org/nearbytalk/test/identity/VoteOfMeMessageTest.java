package org.nearbytalk.test.identity;

import java.text.ParseException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.VoteOfMeMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.test.misc.RandomUtility;

import junit.framework.TestCase;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class VoteOfMeMessageTest extends TestCase {

	public static class VoteOfMeStruct {
		VoteOfMeMessage voteOfMe;
		VoteTopicMessage topic;
	}

	public VoteOfMeStruct generateVoteOfMeMessageWithCheck() throws ParseException {

		VoteTopicMessage topic = VoteTopicMessageTest
				.generateVoteTopicMessageWithCheck();

		Set<String> options = topic.getOptions();

		Set<String> selection = new HashSet<String>();

		if (topic.isMultiSelection()) {
			// random select

			Random random = new Random();
			for (String string : options) {
				if (random.nextBoolean()) {
					selection.add(string);
				}
			}
		}

		if (selection.isEmpty()) {
			// assume at least one
			selection.add(options.iterator().next());
		}

		JsonObject obj = new JsonObject();

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		obj.add(VoteOfMeMessage.OPTIONS_KEY, gson.toJsonTree(selection));

		VoteOfMeMessage vote = new VoteOfMeMessage(
				RandomUtility.randomIdBytesString(),
				RandomUtility.randomUser(), obj.toString(), Calendar
						.getInstance().getTime(), topic.getIdBytes(),0,0,0);

		VoteOfMeStruct ret = new VoteOfMeStruct();
		ret.topic = topic;
		ret.voteOfMe = vote;

		return ret;

	}

	public void testJsonParse() throws ParseException {

		generateVoteOfMeMessageWithCheck();

	}

	public void testSetTopic() throws ParseException {

		VoteOfMeStruct struct = generateVoteOfMeMessageWithCheck();
		try {
			struct.voteOfMe.setReferenceMessageLater(struct.topic);
		} catch (BadReferenceException e) {
			e.printStackTrace();
			fail("should not throw");
		}

	}
	
	public void testSetReferenceMessageThrow() throws ParseException{
		VoteOfMeStruct struct=generateVoteOfMeMessageWithCheck();
		
		AbstractMessage random=RandomUtility.randomNoRefTextMessage(null);
		
		try {
			struct.voteOfMe.setReferenceMessageLater(random);
		} catch (BadReferenceException e) {
			return;
		}
		
		fail("should throw");
		
	}
}
