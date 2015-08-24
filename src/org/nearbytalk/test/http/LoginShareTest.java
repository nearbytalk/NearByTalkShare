package org.nearbytalk.test.http;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.nearbytalk.http.LoginServlet;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.test.misc.RandomUtility;
import org.nearbytalk.util.DigestUtility;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class LoginShareTest extends ServletTestBase {

	public static class LoginStruct {
		public AccessResult basic;
		public ClientUserInfo loginUserInfo;
	}

	public LoginStruct randomUserLoginWithCheck() throws Exception{
		
		LoginStruct ret=randomUserLogin();
		
		checkLoginSuccess(ret);
		
		return ret;
	}
	

	public LoginStruct randomUserLogin() throws ClientProtocolException,
			IOException {

		LoginStruct ret = new LoginStruct();


		ret.loginUserInfo = new ClientUserInfo(RandomUtility.nextString(),
				DigestUtility.oneTimeDigest(RandomUtility.nextString()));

		ret.basic=httpAccess(LoginServlet.class, ret.loginUserInfo, null);

		return ret;

	}

	public void checkLoginSuccess(LoginStruct loginStruct) throws Exception {

		assertTrue(loginStruct.basic.response!=null && !loginStruct.basic.response.isEmpty());

		JsonElement resultJson = GsonThreadInstance.FULL_GSON.get().fromJson(
				loginStruct.basic.response, JsonElement.class);

		assertTrue(resultJson.isJsonObject());

		JsonObject obj = resultJson.getAsJsonObject();

		assertTrue(obj.has("success"));

		assertTrue(obj.get("success").getAsBoolean());

		assertTrue(obj.has("detail"));

		JsonObject detailPart = obj.get("detail").getAsJsonObject();

		ClientUserInfo readBack = GsonThreadInstance.FULL_GSON.get().fromJson(
				detailPart, ClientUserInfo.class);

		assertEquals(loginStruct.loginUserInfo.getIdBytes(),
				readBack.getIdBytes());

	}

}
