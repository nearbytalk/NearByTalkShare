package org.nearbytalk.test.http;

import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.nearbytalk.http.GlobalInfoServlet;
import org.nearbytalk.http.GlobalInfoServlet.GlobalInfo;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.GsonThreadInstance;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;

public class GlobalInfoServletTest extends LoginShareTest{
	
	public void testContentLengthHttpHeader() throws ClientProtocolException, IOException{

		HttpClient client = new DefaultHttpClient();

		HttpGet request = new HttpGet(
				getServletAccessPath(GlobalInfoServlet.class));

		HttpResponse response = client.execute(request);

		long length=response.getEntity().getContentLength();

		assertTrue("GlobalInfoServlet must response with correct Content-Length",length>0);	
	}

	public void testNotLogin() throws ClientProtocolException, IOException {

		HttpClient client = new DefaultHttpClient();

		HttpGet request = new HttpGet(
				getServletAccessPath(GlobalInfoServlet.class));

		HttpResponse response = client.execute(request);

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		GlobalInfoServlet.GlobalInfo info = gson.fromJson(new JsonReader(
				new InputStreamReader(response.getEntity().getContent())),
				GlobalInfoServlet.GlobalInfo.class);

		assertNotNull(info);

		assertNull(info.sessionUser);

	}

	public void testLoginedSessionUser() throws Exception {

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		LoginStruct loginStruct=randomUserLoginWithCheck();

		HttpPost request = new HttpPost(
				getServletAccessPath(GlobalInfoServlet.class));

		HttpResponse response= loginStruct.basic.httpClient.execute(request);
		
		request.releaseConnection();

		JsonElement jsonResult = gson.fromJson(new JsonReader(
				new InputStreamReader(response.getEntity().getContent(),
						Global.UTF8_ENCODING)), JsonElement.class);

		GlobalInfoServlet.GlobalInfo info = gson.fromJson(jsonResult
				.getAsJsonObject().get("detail"), GlobalInfo.class);

		assertNotNull(info);

		assertEquals(loginStruct.loginUserInfo, info.sessionUser);

	}
}
