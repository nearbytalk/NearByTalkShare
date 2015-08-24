package org.nearbytalk.test.http;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.nearbytalk.http.PollServlet;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.query.PollQuery;
import org.nearbytalk.query.PollQuery.PollType;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.service.MessageService;
import org.nearbytalk.service.ServiceInstanceMap;
import org.nearbytalk.test.misc.RandomUtility;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class PollServletTest extends LoginShareTest {

	public static class ResultChecker {
		public void check(List<PlainTextMessage> result) {

			assertNotNull(result);

			Date prevDate = null;

			for (PlainTextMessage plainTextMessage : result) {

				assertNotNull(plainTextMessage.getCreateDate());

				if (prevDate == null) {
					prevDate = plainTextMessage.getCreateDate();
					continue;
				}
				
				assertFalse(prevDate.after(plainTextMessage.getCreateDate()));

			}

		}
	}

	private long sendPollWithCheck(ClientUserInfo loginUser,
			HttpClient httpClient, PollType pollType, ResultChecker checker)
			throws Exception {

		HttpPost request = new HttpPost(getServletAccessPath(PollServlet.class));

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		PollQuery query = new PollQuery();

		query.pollType = pollType;

		Date begin = Calendar.getInstance().getTime();

		String response = httpAccess(PollServlet.class, query, httpClient).response;

		Date end = Calendar.getInstance().getTime();

		long millionDiff = Math.abs(begin.getTime() - end.getTime());
		
		assertNotNull(response);

		assertFalse(response.isEmpty());

		JsonElement resultJson = GsonThreadInstance.FULL_GSON.get().fromJson(
				response, JsonElement.class);

		JsonObject object = resultJson.getAsJsonObject();

		assertTrue(object.get("success").getAsBoolean());

		List<PlainTextMessage> messages = gson.fromJson(object.get("detail"),
				new TypeToken<List<PlainTextMessage>>() {
				}.getType());

		checker.check(messages);

		request.releaseConnection();

		return millionDiff;
	}

	public void testLazy2EagerSwitch() throws Exception {

		LoginStruct loginStruct = randomUserLoginWithCheck();
		
		final Set<PlainTextMessage> onlineMessages=new HashSet<PlainTextMessage>();

		long pollMillionSeconds = sendPollWithCheck(loginStruct.loginUserInfo,
				loginStruct.basic.httpClient, PollType.LAZY,
				new ResultChecker(){

					@Override
					public void check(List<PlainTextMessage> result) {
						// TODO Auto-generated method stub
						super.check(result);
						onlineMessages.addAll(result);
					}
			
		});

		long sendPoll = sendPollWithCheck(loginStruct.loginUserInfo,
				loginStruct.basic.httpClient, PollType.EAGER,
				new ResultChecker() {

					@Override
					public void check(List<PlainTextMessage> result) {
						// TODO Auto-generated method stub
						super.check(result);
						
						assertTrue(onlineMessages.containsAll(result));
					}
			
				});
	}

	public void testLongPollContinuationTimeout() throws Exception {

		LoginStruct loginStruct = randomUserLoginWithCheck();

		long pollMillionSeconds = sendPollWithCheck(loginStruct.loginUserInfo,
				loginStruct.basic.httpClient, PollType.LAZY,
				new ResultChecker());

		long secondPoll = sendPollWithCheck(loginStruct.loginUserInfo,
				loginStruct.basic.httpClient, PollType.LAZY,
				new ResultChecker() {

					@Override
					public void check(List<PlainTextMessage> result) {
						// TODO Auto-generated method stub
						assertTrue(result.isEmpty());
					}
				});

		assertTrue(Math.abs(secondPoll - Global.POLL_INTERVAL_MILLION_SECONDS) < 1000);

	}

	public void testLongPollReadQueueMessageBack() throws Exception {
		LoginStruct loginStruct = randomUserLoginWithCheck();

		long pollMillionSeconds = sendPollWithCheck(loginStruct.loginUserInfo,
				loginStruct.basic.httpClient, PollType.LAZY,
				new ResultChecker());

		final PlainTextMessage shouldCachedInQueue = RandomUtility
				.randomNoRefTextMessage(loginStruct.loginUserInfo);
		//
		ServiceInstanceMap.getInstance().getService(MessageService.class)
				.talk(shouldCachedInQueue);

		long secondPoll = sendPollWithCheck(loginStruct.loginUserInfo,
				loginStruct.basic.httpClient, PollType.LAZY,
				new ResultChecker() {

					@Override
					public void check(List<PlainTextMessage> result) {
						// TODO Auto-generated method stub
						assertFalse(result.isEmpty());

						PlainTextMessage readBack = result.get(0);

						assertTrue(readBack
								.sameStrippedUser(shouldCachedInQueue));
					}
				});

		assertTrue(secondPoll > Global.POLL_INTERVAL_MILLION_SECONDS);
	}

	public void testHistoryPoll() throws Exception {
		LoginStruct loginStruct = randomUserLoginWithCheck();

		MessageService service = ServiceInstanceMap.getInstance().getService(
				MessageService.class);

		final Set<PlainTextMessage> shouldHas = new HashSet<PlainTextMessage>();

		for (int i = 0; i < 100; i++) {

			PlainTextMessage one = RandomUtility
					.randomNoRefTextMessage(loginStruct.loginUserInfo);
			shouldHas.add(one);

			service.talk(one);
		}

		long pollMillionSeconds = sendPollWithCheck(loginStruct.loginUserInfo,
				loginStruct.basic.httpClient, PollType.HISTORY,
				new ResultChecker() {

					@Override
					public void check(List<PlainTextMessage> result) {
						// TODO Auto-generated method stub
						super.check(result);

						int subSet = 0;

						for (PlainTextMessage textMessage : shouldHas) {

							boolean atLeastOne = false;

							for (PlainTextMessage textMessage2 : result) {
								if (textMessage
										.sameStrippedUser(textMessage2)) {
									atLeastOne = true;
									++subSet;
									break;
								}
							}

							if (result.size() < shouldHas.size()) {
								assertTrue(
										"should has message in history poll",
										atLeastOne);
							}
						}

						assertEquals(Math.min(result.size(), shouldHas.size()),
								subSet);

					}
				});

		assertTrue("history poll should return immediately",
				pollMillionSeconds < 5000);
	}

	public void testMessageBetweenPoll() throws Exception {
		LoginStruct loginStruct = randomUserLoginWithCheck();

		long pollMillionSeconds = sendPollWithCheck(loginStruct.loginUserInfo,
				loginStruct.basic.httpClient, PollType.LAZY,
				new ResultChecker());

		final PlainTextMessage shouldCachedInQueue = RandomUtility
				.randomNoRefTextMessage(loginStruct.loginUserInfo);
		//

		new Thread() {
			@Override
			public void run() {
				try {
					sleep(Global.POLL_INTERVAL_MILLION_SECONDS / 2);
					ServiceInstanceMap.getInstance()
							.getService(MessageService.class)
							.talk(shouldCachedInQueue);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}.start();

		long secondPoll = sendPollWithCheck(loginStruct.loginUserInfo,
				loginStruct.basic.httpClient, PollType.LAZY,
				new ResultChecker() {

					@Override
					public void check(List<PlainTextMessage> result) {
						// TODO Auto-generated method stub
						assertFalse(result.isEmpty());

						PlainTextMessage readBack = result.get(0);

						assertTrue(readBack
								.sameStrippedUser(shouldCachedInQueue));
					}
				});
	}

	public void testFirstImmediately() throws Exception {

		LoginStruct loginStruct = randomUserLoginWithCheck();

		long pollMillionSeconds = sendPollWithCheck(loginStruct.loginUserInfo,
				loginStruct.basic.httpClient, PollType.LAZY,
				new ResultChecker());

		assertTrue(pollMillionSeconds < Global.POLL_INTERVAL_MILLION_SECONDS/5);
	}

	public void testSecondPollShouldSuspendAfterOverlap() throws Exception {

		LoginStruct loginStruct = overlapReqest();

		long secondPullAfterOverlap = sendPollWithCheck(
				loginStruct.loginUserInfo, loginStruct.basic.httpClient,
				PollType.LAZY, new ResultChecker() {

					@Override
					public void check(List<PlainTextMessage> result) {
						super.check(result);
						assertTrue("second overlap pull", result.isEmpty());
					}

				});

		assertTrue(
				"second pull should last long",
				Math.abs(secondPullAfterOverlap
						- Global.POLL_INTERVAL_MILLION_SECONDS) < 1000);
	}

	public void testOverlapRequest() throws Exception {
		overlapReqest();
	}

	private LoginStruct overlapReqest() throws Exception {
		final LoginStruct loginStruct = randomUserLoginWithCheck();


		// first poll
		long pollMillionSeconds = sendPollWithCheck(loginStruct.loginUserInfo,
				loginStruct.basic.httpClient, PollType.LAZY,
				new ResultChecker());

		final PlainTextMessage shouldCachedInQueue = RandomUtility
				.randomNoRefTextMessage(loginStruct.loginUserInfo);

		Thread talkMessageThread = new Thread() {
			@Override
			public void run() {
				try {
					sleep(Global.POLL_INTERVAL_MILLION_SECONDS / 4);
					ServiceInstanceMap.getInstance()
							.getService(MessageService.class)
							.talk(shouldCachedInQueue);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		};

		final AtomicBoolean noError = new AtomicBoolean(true);

		// first poll request
		Thread firstPollThread = new Thread() {

			public void run() {

				try {
					sendPollWithCheck(loginStruct.loginUserInfo,
							loginStruct.basic.httpClient, PollType.LAZY,
							new ResultChecker() {

								@Override
								public void check(List<PlainTextMessage> result) {
									super.check(result);

									for (PlainTextMessage textMessage : result) {
										if (textMessage
												.sameStrippedUser(shouldCachedInQueue)) {
											return;
										}
									}

									fail("last talk should in result list!");
									noError.set(false);
								}

							});
				} catch (Exception e) {
					noError.set(false);
				}
			}

		};

		talkMessageThread.start();

		// start first poll
		firstPollThread.start();

		Thread.sleep(Global.POLL_INTERVAL_MILLION_SECONDS / 2);

		// to do a overlap poll
		long shouldSmall = sendPollWithCheck(loginStruct.loginUserInfo,
				loginStruct.basic.httpClient, PollType.LAZY,
				new ResultChecker() {

					@Override
					public void check(List<PlainTextMessage> result) {
						// TODO Auto-generated method stub
						super.check(result);
						assertTrue("overlap poll should has empty result",
								result.isEmpty());
					}
				});

		assertTrue("overlap poll should return immediately", shouldSmall < 1000);

		firstPollThread.join();
		talkMessageThread.join();

		assertTrue(noError.get());

		return loginStruct;
	}
}
