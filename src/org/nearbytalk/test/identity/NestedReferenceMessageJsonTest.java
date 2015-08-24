package org.nearbytalk.test.identity;

import java.io.IOException;

import org.nearbytalk.exception.FileShareException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.RefUniqueFile;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.test.misc.RandomUtility;

import junit.framework.TestCase;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class NestedReferenceMessageJsonTest extends TestCase {

	public void testNestedJson() throws FileShareException, IOException {

		AbstractMessage nested = RandomUtility.randomFileShareMessage(null);

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		JsonObject object = gson.toJsonTree(nested).getAsJsonObject();

		assertEquals(nested.asPlainText(),object.get("plainText").getAsString());


		JsonObject referenceMessage = object.get("referenceMessage")
				.getAsJsonObject();

		assertNotNull(referenceMessage);

		String fileNameProperty = referenceMessage.get("fileName")
				.getAsString();

		assertEquals(
				((RefUniqueFile) nested.getReferenceMessage()).getFileName(),
				fileNameProperty);

	}

}
