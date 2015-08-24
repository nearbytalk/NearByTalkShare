package org.nearbytalk.test.http;

import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.ProtectedProxyMessage;
import org.nearbytalk.identity.VoteOfMeMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.runtime.Global.VoteAnonymous;
import org.nearbytalk.test.misc.RandomUtility;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class VoteFilterTest extends TalkServletTestShare {

	public void testFilterVoteTopicWhenNotVoted() throws Exception {

		Global.getInstance().anonymousVoteTopic = true;
		Global.getInstance().anonymousVoteOfOthers = VoteAnonymous.ALWAYS_VISIBLE;

		TalkVoteTopicResult topicResult = talkVoteTopicMessageWithCheck();

		LoginStruct secondLogin = randomUserLogin();

		AccessResult secondResult = accessQueryDetail(topicResult.topic.getIdBytes()
				, secondLogin.basic.httpClient);

		MessageListResult msgListResult = GsonThreadInstance.FULL_GSON.get()
				.fromJson(secondResult.response, MessageListResult.class);

		assertVoteTopicIsProxy(msgListResult);

	}

	public static void assertVoteTopicIsProxy(MessageListResult msgListResult) {
		assertVoteTopicIsProxy(msgListResult,true);
	}
	
	static enum VoteOfMeState{
		ProxyInvisible,
		ProxyVisible,
		NoneProxy;
	}
	
	public static void assertVoteOfMeState(MessageListResult result,VoteOfMeState toCheck){
		
		JsonObject object=commonExtra(result);
		assertTrue(object.get(AbstractMessage.MESSAGE_TYPE_JSON_KEY).getAsString()
				.equals(VoteOfMeMessage.MESSAGE_TYPE.toString()));
		if (toCheck==VoteOfMeState.ProxyInvisible) {
			
			assertTrue(object.get(ProtectedProxyMessage.IS_PROTECTED_PROXY_KEY).getAsBoolean());
			assertFalse(object.has(VoteOfMeMessage.OPTIONS_KEY));
			return;
		}
		
		if (toCheck==VoteOfMeState.ProxyVisible) {
			assertTrue(object.get(ProtectedProxyMessage.IS_PROTECTED_PROXY_KEY).getAsBoolean());
			assertTrue(object.has(VoteOfMeMessage.OPTIONS_KEY));
			return;
		}
		
		assertTrue(toCheck==VoteOfMeState.NoneProxy);
		assertTrue(object.has(VoteOfMeMessage.OPTIONS_KEY));
	}
	
	public static JsonObject commonExtra(MessageListResult one){
		assertEquals(1, one.detail.size());
		
		JsonElement ele = one.detail.get(0);

		return ele.getAsJsonObject();
	}

	public static void assertVoteTopicIsProxy(MessageListResult msgListResult,boolean is) {
		
	
		JsonObject obj=commonExtra(msgListResult);

		assertTrue(obj.get(AbstractMessage.MESSAGE_TYPE_JSON_KEY).getAsString()
				.equals(VoteTopicMessage.MESSAGE_TYPE.toString()));

		if (is) {

			assertTrue(obj.get(ProtectedProxyMessage.IS_PROTECTED_PROXY_KEY)
					.getAsBoolean());
		}else {
			assertFalse(obj.has(ProtectedProxyMessage.IS_PROTECTED_PROXY_KEY));
		}

	}

	public static class OtherVoteOnTopicResult{
		
		
		VoteOfMeMessage sendersVoteOfMeMessage;
		
		VoteOfMeMessage myVoteOfMeMessage;

		LoginStruct secondLoginStruct;
		
		MessageListResult queryOthersVoteOfMe;
	}
	
	public OtherVoteOnTopicResult otherVoteOnTopic() throws Exception{
		final TalkVoteOfMeResult voteOfMe = talkVoteOfMeMessageSelfWithCheck();
		
		OtherVoteOnTopicResult ret=new OtherVoteOnTopicResult();
		
		ret.sendersVoteOfMeMessage=voteOfMe.voteOfMe;

		LoginStruct secondLogin = randomUserLogin();
		
		AccessResult secondResult = accessQueryDetail(voteOfMe.voteOfMe.getIdBytes(), secondLogin.basic.httpClient);

		MessageListResult msgListResult = GsonThreadInstance.FULL_GSON.get()
				.fromJson(secondResult.response, MessageListResult.class);
		
		ret.secondLoginStruct=secondLogin;
		ret.queryOthersVoteOfMe=msgListResult;

		return ret;
	}
	
	public void testFilterVoteOfMeVisibleAfterVote() throws Exception{
		
		Global.getInstance().anonymousVoteTopic = true;
		Global.getInstance().anonymousVoteOfOthers = VoteAnonymous.VISIBLE_AFTER_VOTE;

		final OtherVoteOnTopicResult first=otherVoteOnTopic();
		
		assertVoteOfMeState(first.queryOthersVoteOfMe,VoteOfMeState.ProxyVisible);

		talkMessageWithCheck(new MessageCheckCallback<VoteOfMeMessage>() {
			@Override
			public VoteOfMeMessage createMessage(LoginStruct loginStruct) {
				try {
					return RandomUtility.randomVoteOfMeMessage(
							loginStruct.loginUserInfo,
							(VoteTopicMessage) first.sendersVoteOfMeMessage.getReferenceMessage());
				} catch (BadReferenceException e) {
					fail("impossible");
					return null;
				}
			}
		}, first.secondLoginStruct);

		AccessResult thirdResult = accessQueryDetail(first.sendersVoteOfMeMessage.getIdBytes(),
				first.secondLoginStruct.basic.httpClient);

		MessageListResult secondCheck = GsonThreadInstance.FULL_GSON.get()
				.fromJson(thirdResult.response, MessageListResult.class);

		assertVoteOfMeState(secondCheck,VoteOfMeState.NoneProxy);
		
		VoteOfMeMessage fullJson=GsonThreadInstance.FULL_GSON.get()
				.fromJson(secondCheck.detail.get(0), VoteOfMeMessage.class);
		
		assertTrue(fullJson.sameStrippedUser(first.sendersVoteOfMeMessage));

	}

	public void testFilterVoteOfMeAlwaysInvisible() throws Exception {

		Global.getInstance().anonymousVoteTopic = true;
		Global.getInstance().anonymousVoteOfOthers = VoteAnonymous.ALWAYS_INVISBLE;
		
		final OtherVoteOnTopicResult first=otherVoteOnTopic();

		assertVoteOfMeState(first.queryOthersVoteOfMe,VoteOfMeState.ProxyInvisible);

		talkMessageWithCheck(new MessageCheckCallback<VoteOfMeMessage>() {
			@Override
			public VoteOfMeMessage createMessage(LoginStruct loginStruct) {
				try {
					return RandomUtility.randomVoteOfMeMessage(
							loginStruct.loginUserInfo,
							(VoteTopicMessage) first.sendersVoteOfMeMessage.getReferenceMessage());
				} catch (BadReferenceException e) {
					fail("impossible");
					return null;
				}
			}
		}, first.secondLoginStruct);

		AccessResult thirdResult = accessQueryDetail(first.sendersVoteOfMeMessage.getIdBytes(),
				first.secondLoginStruct.basic.httpClient);

		MessageListResult secondCheck = GsonThreadInstance.FULL_GSON.get()
				.fromJson(thirdResult.response, MessageListResult.class);

		assertVoteOfMeState(secondCheck,VoteOfMeState.ProxyInvisible);

	}

}
