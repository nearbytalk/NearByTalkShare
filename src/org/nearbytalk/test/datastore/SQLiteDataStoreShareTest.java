package org.nearbytalk.test.datastore;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nearbytalk.datastore.SQLiteDataStore;
import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.exception.DataStoreException;
import org.nearbytalk.exception.FileShareException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.ChatBuildMessage;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.identity.VoteOfMeMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.query.PagedQuery.PagedInfo;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.test.misc.RandomUtility;
import org.nearbytalk.test.misc.ThreadTest;
import org.nearbytalk.test.misc.ThreadTest.SingleTest;

import junit.framework.TestCase;

import com.almworks.sqlite4java.SQLiteBusyException;
import com.almworks.sqlite4java.SQLiteException;

/**
 * @author unknown
 * 
 * common function to save element, all save function has no expensive check(only check return from db)
 * for isolate timing when do performance test
 *
 */
public abstract class SQLiteDataStoreShareTest extends TestCase{
	public SQLiteDataStore dataStore = new SQLiteDataStore();

	@Override
	protected void setUp()  throws DataStoreException {
		dataStore.preCheck(Global.getInstance().getRawDataStorePassword());
	}

	@Override
	protected void tearDown() throws Exception {
		dataStore.threadRecycle();
	}
	
	/**
	 * save random user ,do not check by query
	 * @return
	 * @throws SQLiteBusyException
	 */
	public ClientUserInfo saveRandomUser() throws SQLiteBusyException {
		ClientUserInfo randomUser = RandomUtility.randomUser();

		assertTrue(dataStore.saveOrUpdateUser(randomUser));
		return randomUser;

	}

	public static class SaveChatBuildMessageResult{
		
		public List<ChatBuildMessage> allFlat=new ArrayList<ChatBuildMessage>();
		public List<ChatBuildMessage> topMost=new ArrayList<ChatBuildMessage>();
	}
	

	/**
	 * save list of chatBuildMessage,do not check by query
	 * @param topMostNumber how many topmost chatbuild message is saved
	 * @return all flat list
	 * @throws SQLiteBusyException
	 */
	public SaveChatBuildMessageResult saveChatBuildMessage(int topMostNumber) throws SQLiteBusyException{
		SaveChatBuildMessageResult ret=new SaveChatBuildMessageResult();
		ClientUserInfo user=saveRandomUser();

		for(int i=0;i<topMostNumber;++i){

			List<ChatBuildMessage> toAdd=RandomUtility.randomChatBuildMessage(user, Global.REFERENCE_MAX_DEPTH);
			
			ret.topMost.add(toAdd.get(toAdd.size()-1));

			ret.allFlat.addAll(toAdd);

		}

		assertTrue(dataStore.saveMessage(ret.allFlat));
		return ret;
	}

	/**
	 * check message chain is complete ,and referenceIdBytes= referenceMessage.getIdBytes
	 * @param message
	 */
	public void chainIsComplete(AbstractMessage message) {

		String refId = message.anyReferenceIdBytes();

		while (refId != null) {

			AbstractMessage leaf = message.getReferenceMessage();

			assertNotNull("message not complete:" + message.toString(), leaf);

			assertEquals(refId, leaf.getIdBytes());

			refId = leaf.anyReferenceIdBytes();
			message = leaf;
		}
	}
	
	public static  class SameStrippedUserComparator <T extends AbstractMessage> implements Comparator<T>{

		@Override
		public int compare(T o1, T o2) {
			return o1.sameStrippedUser(o2)?0:1;
		}
		
	}
	
	
	/**
	 * generate random file share message ,save and test.
	 * 
	 * return top-level PlainTextMessage, has a UniqueRef referenceMessage
	 * 
	 * @throws SQLiteBusyException
	 * @throws FileShareException
	 * @throws IOException
	 * @throws ParseException 
	 */
	public PlainTextMessage saveRefereneceFileMessage() throws SQLiteBusyException, FileShareException, IOException, ParseException {
		ClientUserInfo user = saveRandomUser();

		PlainTextMessage msg = RandomUtility.randomFileShareMessage(user);

		assertNotNull(msg.getReferenceMessage());

		assertNull(msg.getReferenceMessage().getReferenceMessage());

		assertTrue(dataStore.saveMessage(msg));
		
		return msg;

	}
	
	
	
	
	

	public <T extends AbstractMessage> boolean hasOnlyOneByQuery(T shouldHave) throws SQLiteBusyException, ParseException{

		return hasOnlyOneByQuery(shouldHave,shouldHave.asPlainText(),new SameStrippedUserComparator<AbstractMessage>(),false);
	}
	
	
	/**
	 * @param toCheck
	 * @param comparator
	 * @param triggerAssert
	 * @return
	 * @throws SQLiteBusyException
	 * @throws ParseException
	 */
	public <T extends AbstractMessage> boolean hasOnlyOneByQuery(T shouldHave,String queryText,Comparator<T> comparator,boolean triggerAssert) throws SQLiteBusyException, ParseException{
		List<T> queried=(List<T>) dataStore.queryMessage(shouldHave.getClass(), 
				queryText, new PagedInfo());
		
		
		boolean ret=false;

		T found=null;

		for (T message: queried) {
			if (comparator.compare(message, shouldHave)==0) {
				
				if (found==null) {
					found=message;
					
						
					//another check by idBytes
					AbstractMessage readBack=dataStore.loadWithDependency(shouldHave.getIdBytes());

					if (triggerAssert) {
						chainIsComplete(found);
						assertEquals(readBack, shouldHave);
					}
					
					ret=true;
				}else{
					System.out.println("duplicate result found: prev one:"+found+"new one:"+message);
					ret=false;
				}
			}
			
		}	
		
		if (found==null) {
			System.out.println(shouldHave.toString()+" not found");
		}
		
		

		
		return ret;
	}
	
	/**
	 * create random PlainTextMessage and save it
	 * @param user user to act as ,if null ,use a new random user
	 * @return saved plainTextMessage
	 * @throws SQLiteBusyException 
	 * @throws ParseException 
	 */
	public PlainTextMessage saveNoRefMessage(ClientUserInfo user) throws SQLiteBusyException, ParseException{
		
		if (user==null) {
			user=saveRandomUser();
		}
		
		PlainTextMessage msg = RandomUtility.randomNoRefTextMessage(user);
		
		assertTrue(dataStore.saveMessage(msg));

		return msg;
		
	}
	
	public PlainTextMessage saveRefMessage() throws SQLiteBusyException, ParseException, BadReferenceException{
		ClientUserInfo randomUser=saveRandomUser();
		
		PlainTextMessage randomMessage = saveNoRefMessage(randomUser);
		
		PlainTextMessage referenceOther = new PlainTextMessage(
				randomUser, RandomUtility.nextString(), randomMessage);

		referenceOther.setReferenceMessageLater(randomMessage);

		assertTrue(dataStore.saveMessage(referenceOther));

		
		return referenceOther;
	}

	
	public VoteTopicMessage saveVoteTopicMessage() throws SQLiteBusyException, ParseException{
		
		HashSet<String> options=new HashSet<String>();
		options.add("A");
		options.add("B");
		options.add("C");
		
		ClientUserInfo user=saveRandomUser();

		VoteTopicMessage toSave=new VoteTopicMessage(user, "topic", "description", true, options);
		
		dataStore.saveMessage(toSave);
		
		String idBytes=toSave.getIdBytes();
		
		AbstractMessage loadBack=dataStore.loadWithDependency(idBytes);
		
		assertEquals(loadBack, toSave);
		
		return toSave;
	}
	
	
	public VoteOfMeMessage saveVoteOfMeMessage() throws SQLiteBusyException, ParseException, BadReferenceException{
			
		VoteTopicMessage topic=saveVoteTopicMessage();
		
		ClientUserInfo random=saveRandomUser();
		
		Set<String> myoptions=new HashSet<String>();
		myoptions.add(topic.getOptions().iterator().next());
		VoteOfMeMessage meMessage=new VoteOfMeMessage(random, "comment", myoptions, topic);
		
		dataStore.saveMessage(meMessage);
		
		AbstractMessage loadBack=dataStore.loadWithDependency(meMessage.getIdBytes());
		
		assertEquals(meMessage, loadBack);
		
		return meMessage;
	}
	
	
	public void checkUserNotVotedQuery(int threadNumber,int loopNumber,int sleepMs,final boolean assumeExist) throws ParseException, SQLiteException, InterruptedException{
		
		final ClientUserInfo user=saveRandomUser();
		
		final VoteTopicMessage savedMessage=saveVoteTopicMessage();
		
		final Set<String> set=new HashSet<String>();
		
		set.add(savedMessage.getIdBytes());
		
		Collection<Exception> exceptions=ThreadTest.run(threadNumber, loopNumber, sleepMs, new SingleTest(){

			@Override
			public void singleTest() throws Exception {
				List<Boolean> result=dataStore.queryVoted(user, set, assumeExist);

				assertTrue(result.size()==1);

				assertNotNull(result.get(0));
				
				assertFalse(result.get(0));
			}

			@Override
			public void threadLeaveCallback() {
				dataStore.threadRecycle();
			}
			
		});

		assertTrue(exceptions.isEmpty());
		
	}
		
	public void checkUserVotedQuery(int threadNumber,int loopNumber,int sleepMs,final boolean assumeExist) throws ParseException, SQLiteException, InterruptedException, BadReferenceException{
		
		final VoteOfMeMessage voted=saveVoteOfMeMessage();

		final Set<String> set=new HashSet<String>();

		set.add(voted.getReferenceIdBytes());
		
		Collection<Exception> exceptions=ThreadTest.run(threadNumber, loopNumber, sleepMs, new SingleTest(){

			@Override
			public void singleTest() throws Exception {
				List<Boolean> result=dataStore.queryVoted(voted.getSender(), set, assumeExist);

				assertTrue(result.size()==1);

				assertNotNull(result.get(0));
				
				assertTrue(result.get(0));
			}

			@Override
			public void threadLeaveCallback() {
				dataStore.threadRecycle();
			}
			
		});

		assertTrue(exceptions.isEmpty());
		
	}
}
