package org.nearbytalk.test.identity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.BaseIdentifiable;
import org.nearbytalk.identity.ProtectedProxyMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.runtime.Global.VoteAnonymous;
import org.nearbytalk.test.TestUtil;
import org.nearbytalk.test.misc.RandomUtility;
import org.nearbytalk.util.Utility.CustomVoteTopicResult;

import junit.framework.TestCase;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class VoteTopicMessageTest extends TestCase {

	public static class VoteTopicStruct {
		String jsonString;
		String topic;
		Map<String, Integer> results;
		boolean multiSelection;
	}

	public static VoteTopicStruct generateVoteTopicJsonString() {
		JsonObject obj = new JsonObject();

		final String TOPIC = "topic";

		obj.addProperty(VoteTopicMessage.VOTE_TOPIC_KEY, TOPIC);

		HashSet<String> options = new HashSet<String>();

		final int OPTIONS_NUMBER = 5;

		final boolean MULTI_SELECTION = true;

		VoteTopicStruct ret = new VoteTopicStruct();

		ret.results = new HashMap<String, Integer>();

		for (int i = 0; i < OPTIONS_NUMBER; ++i) {

			String key = "option" + i;

			options.add(key);

			ret.results.put(key, i);
		}

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		obj.add(VoteTopicMessage.OPTIONS_KEY,
				gson.fromJson(gson.toJson(options), JsonElement.class));

		obj.addProperty(VoteTopicMessage.MULTI_SELECTION_KEY, MULTI_SELECTION);

		ret.jsonString = String.format("%04x", obj.toString().length())
				+ obj.toString() + gson.toJson(ret.results.values());
		ret.topic = TOPIC;
		ret.multiSelection = MULTI_SELECTION;

		return ret;

	}

	public void testJsonParse() throws ParseException {

		generateVoteTopicMessageWithCheck();
	}

	public static VoteTopicMessage generateVoteTopicMessageWithCheck()
			throws ParseException {

		VoteTopicStruct struct = generateVoteTopicJsonString();

		VoteTopicMessage message = new VoteTopicMessage(
				RandomUtility.randomIdBytesString(),
				RandomUtility.randomUser(), struct.jsonString, Calendar
						.getInstance().getTime(), 0, 0, 0);

		assertEquals(message.getVoteTopic(), struct.topic);
		assertEquals(message.getOptions(), struct.results.keySet());

		assertEquals(message.isMultiSelection(), struct.multiSelection);

		return message;
	}

	public void testShouldNotSetReferenceMessage() throws ParseException {

		VoteTopicMessage generated = generateVoteTopicMessageWithCheck();

		try {
			generated.setReferenceMessageLater(RandomUtility
					.randomNoRefTextMessage(null));
		} catch (UnsupportedOperationException ex) {
			// ok
			return;
		}
		fail("should throw");

	}

	private VoteTopicMessage defaultNew(String text) throws ParseException {
		return new VoteTopicMessage(RandomUtility.randomIdBytesString(),
				RandomUtility.randomUser(), text, Calendar.getInstance()
						.getTime(), 1, 1, 1);
	}

	public void testBadMetaLengthThrow() {

		String randomString = "-1{";

		try {
			defaultNew(randomString);
		} catch (ParseException e) {
			return;
		}

		fail("shoud throw");

	}

	public void testRight() throws ParseException {
		HashSet<String> options = new HashSet<String>();

		options.add("A");
		options.add("B");
		options.add("C");
		options.add("D");

		ArrayList<Integer> result = new ArrayList<Integer>();
		result.add(1);
		result.add(2);
		result.add(3);
		result.add(4);

		CustomVoteTopicResult meta = TestUtil.createCustomVoteTopicJson(
				"topic", true, options, result,"description");

		defaultNew(String.format("%04x", meta.metaHeader.length())
				+ meta.metaHeader + meta.results);

	}

	public void testOptionResultNotMatch() {
		HashSet<String> options = new HashSet<String>();

		options.add("A");
		options.add("B");
		options.add("C");
		options.add("D");

		ArrayList<Integer> result = new ArrayList<Integer>();
		result.add(1);
		result.add(2);
		result.add(3);

		CustomVoteTopicResult meta = TestUtil.createCustomVoteTopicJson(
				"topic", true, options, result,"description");

		try {
			defaultNew(String.format("%04x", meta.metaHeader.length())
					+ meta.metaHeader + meta.results);
		} catch (ParseException e) {
			return;
		}

		fail("should throw");
	}

	public void testEmptyThrow() {

		try {
			defaultNew("");
		} catch (ParseException e) {
			return;
		}

		fail("should throw");
	}

	public void testMissingFieldThrow() {

		HashSet<String> options = new HashSet<String>();

		options.add("A");
		options.add("B");
		options.add("C");
		options.add("D");

		ArrayList<Integer> result = new ArrayList<Integer>();
		result.add(1);
		result.add(2);
		result.add(3);
		result.add(4);

		Object param5[] = new Object[5];

		param5[0] = "topic";
		param5[1] = true;
		param5[2] = options;
		param5[3] = result;
		param5[4] = "description";

		for (int i = 0; i < 5; i++) {

				Object thisFields[] = new Object[5];

				for (int j = 0; j < 5; ++j) {
					thisFields[j] = param5[j];
				}

				thisFields[i] = null;

				CustomVoteTopicResult meta = TestUtil
						.createCustomVoteTopicJson(thisFields);

				try {
					defaultNew(String.format("%04x", meta.metaHeader.length())
							+ meta.metaHeader + meta.results);
				} catch (ParseException e) {
					continue;
				}
				
				if (i==4) {
					//last is description ,not throw is ok
					
					continue;
				}

				fail("should throw");
		}
	}

	public void testWrongMetaHeaderLengthThrow() {
		try {

			HashSet<String> options = new HashSet<String>();

			options.add("A");
			options.add("B");
			options.add("C");
			options.add("D");

			ArrayList<Integer> result = new ArrayList<Integer>();
			result.add(1);
			result.add(2);
			result.add(3);
			result.add(4);

			CustomVoteTopicResult meta = TestUtil.createCustomVoteTopicJson(
					"topic", true, options, result,"description");

			defaultNew(String.format("%04x", 10) + meta.metaHeader
					+ meta.results);
		} catch (ParseException e) {
			return;
		}

		fail("should throw");
	}
	
	public void testCreateProxy(){
		VoteTopicMessage random=RandomUtility.randomVoteTopicMessage(null);
		
		AbstractMessage proxy=random.createProxy(VoteAnonymous.ALWAYS_INVISBLE, true);
		
		String jsonString=GsonThreadInstance.FULL_GSON.get().toJson(proxy);
		
		JsonElement dejson=GsonThreadInstance.FULL_GSON.get().fromJson(jsonString, JsonElement.class);
		
		JsonObject jsonObject=dejson.getAsJsonObject();
		
		assertEquals(random.getIdBytes(), jsonObject.get(BaseIdentifiable.ID_BYTES_KEY).getAsString());
		//TODO check other fields
	}

}
