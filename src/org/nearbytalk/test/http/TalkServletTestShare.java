package org.nearbytalk.test.http;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.http.QueryMessageServlet;
import org.nearbytalk.http.TalkServlet;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.VoteOfMeMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.query.MessageQuery;
import org.nearbytalk.query.SearchType;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.service.MessageService;
import org.nearbytalk.service.ServiceInstanceMap;
import org.nearbytalk.test.misc.RandomUtility;
import org.nearbytalk.util.DigestUtility;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class TalkServletTestShare extends LoginShareTest {

	public static abstract class MessageCheckCallback<T extends AbstractMessage> {
		public abstract T createMessage(LoginStruct loginStruct);

		public void postCheckFromService(T queryBack) throws Exception {
		}
		
		public void postCheckDefault(T sent,T queryBack) throws Exception{
			assertEquals(sent,queryBack);

			assertSame(sent.getClass() , queryBack.getClass());

			postCheckFromService(queryBack);
		}
	}

	public <T extends AbstractMessage> void talkMessageWithCheck(
			MessageCheckCallback<T> callback) throws Exception {
		talkMessageWithCheck(callback,null);
	}
	
	public AccessResult accessQueryDetail(String Idbytes,HttpClient httpClient) throws IllegalStateException, IOException{
		MessageQuery query = new MessageQuery();

		query.searchType = SearchType.EXACTLY;
		query.keywords = Idbytes;
		return httpAccess(QueryMessageServlet.class,
				query, httpClient);

	}

	@SuppressWarnings("unchecked")
	public <T extends AbstractMessage> void talkMessageWithCheck(
			MessageCheckCallback<T> callback, LoginStruct loginStructUsed) throws Exception {
		
		final LoginStruct loginStruct= (loginStructUsed==null?randomUserLoginWithCheck():loginStructUsed);

		T myTalk = callback.createMessage(loginStruct);

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		AccessResult basic = httpAccess(TalkServlet.class, myTalk,
				loginStruct.basic.httpClient);

		JsonElement twitterResult = gson.fromJson(basic.response,
				JsonElement.class);

		JsonObject obj = twitterResult.getAsJsonObject();

		assertTrue(obj.get(GsonThreadInstance.RESULT_SUCCESS_JSON_KEY).getAsBoolean());

		String expectedIdBytes = obj.get(GsonThreadInstance.RESULT_DETAIL_JSON_KEY).getAsString();

		assertTrue(DigestUtility.isValidSHA1(expectedIdBytes));
		
		assertEquals(myTalk.getIdBytes(), expectedIdBytes);

		MessageService service = ServiceInstanceMap.getInstance().getService(
				MessageService.class);

		AbstractMessage message = service.queryDetail(expectedIdBytes);

		assertNotNull(message);
		
		//makes there create date just same . or query back one may be some secons slow...
		myTalk.setCreateDateLater(message.getCreateDate());

		callback.postCheckDefault(myTalk, (T) message);
		
	}

	
	
	public static class TalkVoteTopicResult{
		
		VoteTopicMessage topic;
		
		LoginStruct loginStruct;
	}
	
	public static class TalkVoteOfMeResult{
		VoteOfMeMessage voteOfMe;
		
		LoginStruct loginStruct;
	}
	
	public TalkVoteTopicResult talkVoteTopicMessageWithCheck() throws Exception {

		final TalkVoteTopicResult ret=new TalkVoteTopicResult();

		talkMessageWithCheck(new MessageCheckCallback<VoteTopicMessage>() {

			@Override
			public VoteTopicMessage createMessage(LoginStruct loginStruct) {
				ret.loginStruct = loginStruct;
				
				VoteTopicMessage created=RandomUtility.randomVoteTopicMessage(loginStruct.loginUserInfo);
				
				ret.topic=created;
				return created;
			}
			
		});

		return ret;
	}
	
	
	public TalkVoteOfMeResult talkVoteOfMeMessageOthersWithCheck() throws Exception{
		final TalkVoteTopicResult topic=talkVoteTopicMessageWithCheck();
		
		TalkVoteOfMeResult ret=new TalkVoteOfMeResult();
		
		ret.loginStruct=topic.loginStruct;
		
		talkMessageWithCheck(new MessageCheckCallback<VoteOfMeMessage>() {

			@Override
			public VoteOfMeMessage createMessage(LoginStruct loginStruct) {
				try {
					return RandomUtility.randomVoteOfMeMessage(loginStruct.loginUserInfo, topic.topic);
				} catch (BadReferenceException e) {
					fail("impossible");
					return null;
				}
			}
		});
		
		return ret;
	}

	public TalkVoteOfMeResult talkVoteOfMeMessageSelfWithCheck() throws Exception{
		final TalkVoteTopicResult topic=talkVoteTopicMessageWithCheck();
		
		final TalkVoteOfMeResult ret=new TalkVoteOfMeResult();
		
		ret.loginStruct=topic.loginStruct;
		
		talkMessageWithCheck(new MessageCheckCallback<VoteOfMeMessage>() {

			@Override
			public VoteOfMeMessage createMessage(LoginStruct loginStruct) {
				try {

					VoteOfMeMessage created=RandomUtility.randomVoteOfMeMessage(loginStruct.loginUserInfo, topic.topic);
					ret.voteOfMe=created;
					return created;
				} catch (BadReferenceException e) {
					fail("impossible");
					return null;
				}
			}
		},topic.loginStruct);
		
		return ret;
	}
	
	
	
}
