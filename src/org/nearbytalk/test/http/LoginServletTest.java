package org.nearbytalk.test.http;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.nearbytalk.http.ErrorResponse;
import org.nearbytalk.http.GlobalInfoServlet;
import org.nearbytalk.http.LoginServlet;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.runtime.UniqueObject;
import org.nearbytalk.test.misc.RandomUtility;
import org.nearbytalk.util.DigestUtility;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

public class LoginServletTest extends LoginShareTest {

	public void testLoginInvalidClientError() throws Exception {

		HttpClient client = new DefaultHttpClient();

		HttpGet request = new HttpGet(getServletAccessPath(LoginServlet.class));

		HttpResponse response = client.execute(request);

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		JsonReader reader = new JsonReader(new InputStreamReader(response
				.getEntity().getContent(), Global.UTF8_ENCODING));

		JsonElement resultJson = GsonThreadInstance.FULL_GSON.get().fromJson(
				reader, JsonElement.class);

		JsonObject object = resultJson.getAsJsonObject();

		assertTrue(object.has("success"));

		assertFalse(object.get("success").getAsBoolean());

		assertTrue(object.has("detail"));

		ErrorResponse errorMessage = gson.fromJson(object.get("detail"),
				ErrorResponse.class);

		assertEquals(ErrorResponse.INVALID_CLIENT_USER_INFO, errorMessage);

	}

	public void testLoginSuccessInGlobalMap()
			throws Exception {

		LoginStruct loginStruct = randomUserLoginWithCheck();


		JsonElement resultJson = GsonThreadInstance.FULL_GSON.get().fromJson(
				loginStruct.basic.response, JsonElement.class);

		JsonObject object = resultJson.getAsJsonObject();

		assertTrue(object.get("success").getAsBoolean());

		ClientUserInfo readBack = GsonThreadInstance.FULL_GSON.get().fromJson(
				object.get("detail"), ClientUserInfo.class);

		assertEquals(loginStruct.loginUserInfo.getUserName(),
				readBack.getUserName());

		assertEquals(loginStruct.loginUserInfo.getIdBytes(),
				readBack.getIdBytes());

		assertTrue(UniqueObject.getInstance().onlineUserInfoMap
				.containsExactly(loginStruct.loginUserInfo));

	}
	
	public void testServerBusy(){
		//TODO
	}

	public void testLoginSuccess() throws Exception {

		randomUserLoginWithCheck();

	}

	private void checkShouldFaild(HttpClient client, HttpPost request,
			JsonObject sentUser) throws ClientProtocolException, IOException,
			URISyntaxException {

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		{

			StringEntity entity = new StringEntity(gson.toJson(sentUser),
					Global.UTF8_ENCODING);

			request.reset();

			request.setURI(new URI(getServletAccessPath(LoginServlet.class)));

			request.setEntity(entity);

			HttpResponse response = client.execute(request);

			JsonReader reader = new JsonReader(new InputStreamReader(response
					.getEntity().getContent(), Global.UTF8_ENCODING));

			JsonElement resultJson = GsonThreadInstance.FULL_GSON.get()
					.fromJson(reader, JsonElement.class);

			assertTrue(resultJson.isJsonObject());

			JsonObject obj = resultJson.getAsJsonObject();

			// servlet result should always return false;
			assertFalse(obj.get("success").getAsBoolean());

		}

		{

			request.reset();

			request.setURI(new URI(
					getServletAccessPath(GlobalInfoServlet.class)));

			HttpResponse response = client.execute(request);

			// globalInfo servlet should not have sessionUser
			JsonReader reader2 = new JsonReader(new InputStreamReader(response
					.getEntity().getContent(), Global.UTF8_ENCODING));

			JsonElement resultJson2 = GsonThreadInstance.FULL_GSON.get()
					.fromJson(reader2, JsonElement.class);

			JsonObject obj2 = resultJson2.getAsJsonObject();

			assertTrue(obj2.get("success").getAsBoolean());

			assertFalse(obj2.has("sessionUser"));

			// onlineUserMap should not contains this user
			// TODO
		}

	}

	public void testInvalidUserShouldNotInSession()
			throws ClientProtocolException, IOException, URISyntaxException {

		HttpClient client = new DefaultHttpClient();

		HttpPost request = new HttpPost();

		{
			JsonObject empty = new JsonObject();

			checkShouldFaild(client, request, empty);
		}
		{
			JsonObject onlyUserName = new JsonObject();

			onlyUserName.addProperty("userName", RandomUtility.nextString());

			checkShouldFaild(client, request, onlyUserName);

		}

		{

			JsonObject onlyIdBytes = new JsonObject();

			onlyIdBytes.addProperty("IdBytes", DigestUtility
					.byteArrayToHexString(DigestUtility
							.digestNotNull(new String[] { RandomUtility
									.nextString() })));

			checkShouldFaild(client, request, onlyIdBytes);
		}

	}

}
