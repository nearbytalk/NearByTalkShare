package org.nearbytalk.test;

import java.util.Collection;
import java.util.HashSet;

import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.runtime.UniqueObject;
import org.nearbytalk.util.Utility.CustomVoteTopicResult;

import junit.framework.TestCase;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class TestUtil {
	
	public static abstract class PostCheck<T extends AbstractMessage>{
		
		public void postCheck(T foundInCollection,T toCheck){
			TestCase.assertTrue(toCheck.sameStrippedUser(foundInCollection));
		}
	}
	public static <T extends AbstractMessage> void assertContains(Collection<T> collection,
			T shouldContains) {

		assertContains(collection, shouldContains,new PostCheck<T>() {
		});
	}

	public static <T extends AbstractMessage> void assertContains(Collection<T> collection,
			T shouldContains,PostCheck<T> postCheck) {

		if (collection.contains(shouldContains)) {
			
			for (T t : collection) {
				if (t.equals(shouldContains)) {
					
					postCheck.postCheck(t, shouldContains);
					break;
				}
			}
			return;
		}

		for (AbstractMessage abstractMessage : collection) {
			if (abstractMessage.getIdBytes()
					.equals(shouldContains.getIdBytes())) {
				TestCase.fail("msg in collection : " + abstractMessage
						+ " has same id bytes, but not equals");
			}
		}

		TestCase.fail("no contains idbytes:" + shouldContains.getIdBytes());

	}

	@SuppressWarnings("unchecked")
	public static CustomVoteTopicResult createCustomVoteTopicJson(
			Object[] fiveParam) {
		return createCustomVoteTopicJson((String) fiveParam[0],
				(Boolean) fiveParam[1], (HashSet<String>) fiveParam[2],
				(Collection<Integer>) fiveParam[3],(String)fiveParam[4]);
	}

	public static CustomVoteTopicResult createCustomVoteTopicJson(
			String voteTopic, Boolean multiSelection, HashSet<String> options,
			Collection<Integer> result,String description) {

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		JsonObject metaHeader = new JsonObject();

		JsonElement optionsPart = gson.toJsonTree(options);

		if (voteTopic != null) {

			metaHeader.addProperty(VoteTopicMessage.VOTE_TOPIC_KEY, voteTopic);
		}

		if (multiSelection != null) {

			metaHeader.addProperty(VoteTopicMessage.MULTI_SELECTION_KEY,
					multiSelection);
		}

		if (options != null && !options.isEmpty()) {

			metaHeader.add(VoteTopicMessage.OPTIONS_KEY, optionsPart);
		}
		
		if (description!=null) {
			
			metaHeader.addProperty(VoteTopicMessage.DESCRIPTION_KEY, description);
		}

		CustomVoteTopicResult ret = new CustomVoteTopicResult();

		ret.metaHeader = gson.toJson(metaHeader);

		if (result != null && !result.isEmpty()) {
			ret.results = gson.toJson(result);
		}

		return ret;
	}
	
	private static boolean first=true;
	public static void resetUniqueObjectIfNessesery(){
		
		//TODO temp hack, I've no idea how to store per-jetty server 
		// variable ,must use a singleton object , but junit must re-init
		//every testcase run.
		if (first) {
			first=false;
		}else {
			UniqueObject.getInstance().getMessageCache().stop();
			UniqueObject.reset();
		}
		
	}
}
