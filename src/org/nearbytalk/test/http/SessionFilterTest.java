package org.nearbytalk.test.http;

import java.io.IOException;

import org.nearbytalk.http.AbstractServlet;
import org.nearbytalk.http.ErrorResponse;
import org.nearbytalk.http.PollServlet;
import org.nearbytalk.http.TalkServlet;
import org.nearbytalk.runtime.GsonThreadInstance;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SessionFilterTest extends LoginShareTest {

	private <T extends AbstractServlet> void shouldReturnNotLogin(Class<T> clazz)
			throws IOException {

		AccessResult result = httpAccess(clazz, "dummy", null);

		JsonElement ele = GsonThreadInstance.STRIP_USER_ID_BYTES_GSON.get()
				.fromJson(result.response, JsonElement.class);

		JsonObject obj = ele.getAsJsonObject();

		assertFalse(obj.get("success").getAsBoolean());

		assertEquals(obj.get("detail").getAsString(),
				ErrorResponse.NOT_LOGIN.toString());

		return;

	}

	public void testNotLoginResult() throws IOException {

		Class<? extends AbstractServlet> protectedUrl[] = new Class[] {
				TalkServlet.class, PollServlet.class };

		for (Class<? extends AbstractServlet> class1 : protectedUrl) {

			shouldReturnNotLogin(class1);
		}

	}

}
