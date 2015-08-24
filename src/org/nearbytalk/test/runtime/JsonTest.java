package org.nearbytalk.test.runtime;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.util.DigestUtility;
import org.nearbytalk.util.Result;
import org.nearbytalk.util.Utility;

import junit.framework.TestCase;

import com.google.gson.Gson;

public class JsonTest extends TestCase {

	ClientUserInfo testUserInfo = new ClientUserInfo("dsf",
			DigestUtility.oneTimeDigest("bad"));

	public void testNoInvalidSHA1ClientUserInfo()
			throws UnsupportedEncodingException {

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		String wrongJson = "{\"userName\":\"nearbytalk\",\"idBytes\":"
				+ "\"1234567890123456789012345678901234567 890\"}";

		ClientUserInfo deserialize = gson.fromJson(wrongJson,
				ClientUserInfo.class);

		//TODO should add ClientUserInfoChecker test
	//	assertNull(deserialize);

	}

	public void testNoInvalidUserNameInfo() {
		Gson gson = GsonThreadInstance.FULL_GSON.get();

		String wrongJson = "{\"userName\":\"\",\"idBytes\":"
				+ "\"1234567890123456789012345678901234567890\"}";

		ClientUserInfo deserialize = gson.fromJson(wrongJson,
				ClientUserInfo.class);

		//TODO should add ClientUserInfoChecker test
		//assertNull(deserialize);
	}

	public void testRightClientUserInfoJsonDeserialize() {

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		String expectedJsonString = "{\"userName\":\"nearbytalk\",\"idBytes\":"
				+ "\"1234567890123456789012345678901234567890\"}";

		ClientUserInfo deserialize = gson.fromJson(expectedJsonString,
				ClientUserInfo.class);

		assertNotNull(deserialize);

		assertTrue(DigestUtility.isValidSHA1(deserialize.getIdBytes()));

	}

	public void testExpectedClientUserInfoJson() throws IOException {

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		String resultString = gson.toJson(testUserInfo);

		ClientUserInfo object = gson.fromJson(resultString,
				ClientUserInfo.class);

		Utility.assumeNotNullOrEmpty(object.getUserName());

		assertTrue(DigestUtility.isValidSHA1(object.getIdBytes()));

		assertEquals(testUserInfo.getIdBytes(), object.getIdBytes());

		assertEquals(testUserInfo.getUserName(), object.getUserName());

	}
	
	public void testWriteResultWriter() throws IOException{
		Writer writer=new StringWriter();
		
		String detailString="detail String";
		
		GsonThreadInstance.writeServletResult(true, true, detailString , writer);
		
		String jsonString=writer.toString();
		
		Result readBack=GsonThreadInstance.FULL_GSON.get().fromJson(jsonString, Result.class);
		
		assertTrue(readBack.isSuccess());
		assertEquals(readBack.getDetail(),detailString);
		
	}

}
