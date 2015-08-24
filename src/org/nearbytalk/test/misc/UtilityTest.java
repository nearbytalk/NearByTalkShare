package org.nearbytalk.test.misc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nearbytalk.exception.FileShareException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.identity.RefUniqueFile;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.test.misc.ThreadTest.SingleTest;
import org.nearbytalk.util.Utility;

import com.google.gson.Gson;

public class UtilityTest extends TestCase {

	public void testDateFuzzySame() {

		Date origin = Calendar.getInstance().getTime();

		Gson gson = GsonThreadInstance.STRIP_USER_ID_BYTES_GSON.get();

		String toJson = gson.toJson(origin);

		Date deserialize = gson.fromJson(toJson, Date.class);

		assertTrue(Utility.dateFuzzySame(origin, deserialize));
	}
	
	public void testNoDupMessage(){
		
		Map<String,PlainTextMessage> generatedIdBytes=new HashMap<String,PlainTextMessage>();
		
		ClientUserInfo user=RandomUtility.randomUser();
		
		String commonPart=RandomUtility.nextString();
		
		for(int i=0;i<1000;++i){
			
			PlainTextMessage thisOne=RandomUtility.randomNoRefTextMessage(user,commonPart,true);
			
			assertFalse("already has "+generatedIdBytes.get(thisOne.getIdBytes())+" \nbut new:"+thisOne,generatedIdBytes.containsKey(thisOne.getIdBytes()));
			
			generatedIdBytes.put(thisOne.getIdBytes(),thisOne);
			
		}
	}

	public void testThreadRandom() throws InterruptedException {

		final Set<String> deDuped = new ConcurrentHashSet<String>();

		assertTrue(ThreadTest.run(200, 10, 10, new SingleTest() {

			@Override
			public void singleTest() throws Exception {

				String random = RandomUtility.nextString();

				if (!deDuped.add(random)) {
					throw new IllegalStateException("has dupped!");
				}

			}
		}).isEmpty());

	}

	public void testWriteUploadStreamJustSize() throws FileShareException,
			IOException {
		String randomString = RandomUtility.nextString();

		byte[] bytes = randomString.getBytes();
		InputStream randomStream = new ByteArrayInputStream(bytes);

		// same size ,should not throw
		RefUniqueFile file = Utility.writeUploadStream(randomStream,
				bytes.length, randomString + ".txt");

		randomString = RandomUtility.nextString();

		randomStream = new ByteArrayInputStream(randomString.getBytes());

	}

	public void testWriteUploadOverflowDelete() throws IOException {
		String randomString = RandomUtility.nextString();

		byte[] bytes = randomString.getBytes();
		InputStream randomStream = new ByteArrayInputStream(bytes);

		File temp = new File(Utility.makeupTempUploadPath());

		String[] beforeFailed = temp.list();

		try {
			RefUniqueFile file = Utility.writeUploadStream(randomStream, 0,
					randomString + ".txt");

			fail("should throw");
		} catch (FileShareException e) {
			// OK
		}

		String[] afterFailed = temp.list();

		assertTrue("temp file should be deleted",
				Arrays.deepEquals(beforeFailed, afterFailed));
	}

	public void testFlatReconstruct() {

		Map<String, AbstractMessage> toReconstruct = new HashMap<String, AbstractMessage>();

		final int flatSize = 400;
		for (int i = 0; i < flatSize; ++i) {

			PlainTextMessage random = RandomUtility
					.randomNoRefTextMessage(null);

			toReconstruct.put(random.getIdBytes(), random);
		}

		//List<PlainTextMessage> reconstruct = Utility.reconstruct(
		//		PlainTextMessage.class, toReconstruct,null);

		//assertEquals("reconstruct size should equals to flat size", flatSize,
		//		reconstruct.size());

	}
	
	public void testReferenceMessageReconstruct(){
		Map<String, AbstractMessage> toReconstruct = new HashMap<String, AbstractMessage>();

		final int flatSize = 400;
		for (int i = 0; i < flatSize; ++i) {

			PlainTextMessage random = RandomUtility
					.randomRefTextMessage(null);

			toReconstruct.put(random.getIdBytes(), random);
			toReconstruct.put(random.anyReferenceIdBytes(), random.getReferenceMessage());
		}

		//List<PlainTextMessage> reconstruct = Utility.reconstruct(
		//		PlainTextMessage.class, toReconstruct,null);

		//assertEquals("reconstruct size should 2x as flat size", flatSize*2,
		//		reconstruct.size());
	}
	
	public void testBadReferenceMessageNotInList(){
		//TODO
		
	}
public void testHasCrossIndex() {

		
		Set<String> ret=Utility.parseTopics("#abc#");
		
		assertNotNull(ret);
		assertEquals(1, ret.size());
		
		assertTrue(ret.contains("abc"));
		
		
	}
	
	public void testMultiCrossIndex(){
		
		Set<String> ret=Utility.parseTopics("#a#b#c#");
		
		assertNotNull(ret);
		
		assertEquals(2, ret.size());
		
		assertTrue(ret.contains("a"));
		assertTrue(ret.contains("c"));
		
		
		ret=Utility.parseTopics("#a#,#b#,#c#,#d#");
		
		assertEquals(4, ret.size());
		
		assertTrue(ret.contains("c"));
		assertTrue(ret.contains("d"));
	}

	public void testOnlyFirst() {
		Set<String> ret=Utility.parseTopics("#abc#bbc#");

		assertNotNull(ret);
		
		assertEquals(1, ret.size());
	
		assertTrue(ret.contains("abc"));
	}
	
	private static void assertEmpty(Collection<?> c){
		assertTrue(c.isEmpty());
	}

	public void testNoCrossIndex() {
		
		assertEmpty(Utility.parseTopics("#a"));
		assertEmpty(Utility.parseTopics("b#"));
		assertEmpty(Utility.parseTopics("abc"));
	}

	public void testInvalidJsonInCrossIndex() {

		assertEmpty(Utility.parseTopics("#a,#"));
		assertEmpty(Utility.parseTopics("#ab:#"));
		assertEmpty(Utility.parseTopics("#ab:cc#"));
		assertEmpty(Utility.parseTopics("#ab[cc#"));
		assertEmpty(Utility.parseTopics("#ab]cc#"));
		assertEmpty(Utility.parseTopics("#ab{cc#"));
		assertEmpty(Utility.parseTopics("#ab}cc#"));

	}
	
	public void testCrossIndexNoDup(){
		
		Set<String> ret=Utility.parseTopics("#abc##abc#");
		assertEquals(1, ret.size());
		assertTrue(ret.contains("abc"));
	}
	
	public void testIsEmptyString(){
		assertTrue(Utility.isEmptyString(""));
		assertTrue(Utility.isEmptyString(" "));
		assertTrue(Utility.isEmptyString("	"));
		assertFalse(Utility.isEmptyString("a"));
		assertFalse(Utility.isEmptyString(","));
		assertFalse(Utility.isEmptyString(RandomUtility.nextString()));
	}
}
