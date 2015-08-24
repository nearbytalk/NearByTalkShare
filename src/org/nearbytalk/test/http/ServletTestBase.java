package org.nearbytalk.test.http;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.nearbytalk.http.AbstractServlet;
import org.nearbytalk.http.EmbeddedHttpServer;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.runtime.Global.HttpServerInfo;
import org.nearbytalk.test.TestUtil;
import org.nearbytalk.util.Utility;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * base class for all servlet based test. 
 * this class will start server. sub-class no need to do this
 * 
 */
public abstract class ServletTestBase extends TestCase {

	protected EmbeddedHttpServer embeddedHttpServer ;

	private String hostPrefix;

	@Override
	protected void setUp() throws Exception {
		
		TestUtil.resetUniqueObjectIfNessesery();

		Global.getInstance().hostIp = "127.0.0.1";
		embeddedHttpServer = new EmbeddedHttpServer();
		embeddedHttpServer.start();
		hostPrefix = "http://" + Global.getInstance().hostIp + ":"
				+ HttpServerInfo.listenPort;
		
	}

	public <T extends AbstractServlet> String getServletAccessPath(
			Class<T> clazz) {

		return hostPrefix + Utility.makeupAccessPath(clazz);
	}
	
	public static class AccessResult{
		public HttpClient httpClient;
		public String response;
	}
	
	/**
	 * access servlet url with toPost and client
	 * @param class1
	 * @param toPost
	 * @param client previous used client, or null if want new session 
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public <T extends AbstractServlet> AccessResult httpAccess(
			Class<T> class1,Object toPost ,HttpClient client) throws IllegalStateException, IOException {
		
		AccessResult ret=new AccessResult();

		ret.httpClient=(client==null?new DefaultHttpClient(new PoolingClientConnectionManager()):client);

		
		HttpPost request = new HttpPost(
				getServletAccessPath(class1));
		

		Gson gson = GsonThreadInstance.FULL_GSON.get();
		
		String string=null;
		
		if (toPost instanceof JsonElement) {
			string=toPost.toString();
		}else{
			string=gson.toJson(toPost);
		}

		StringEntity entity = new StringEntity(string,
				Global.UTF8_ENCODING);

		request.setEntity(entity);

		HttpResponse response = ret.httpClient.execute(request);

		java.util.Scanner s = new java.util.Scanner(response.getEntity()
				.getContent(), Global.UTF8_ENCODING).useDelimiter("\\A");

		ret.response=s.hasNext() ? s.next() : "";
		request.releaseConnection();

		return ret;

	}

	@Override
	protected void tearDown() throws Exception {
		embeddedHttpServer.stop();
	}

}
