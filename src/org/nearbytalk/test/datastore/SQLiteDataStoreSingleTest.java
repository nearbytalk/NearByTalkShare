package org.nearbytalk.test.datastore;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nearbytalk.datastore.IDataStore.QueryMethod;
import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.exception.FileShareException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.ChatBuildMessage;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.identity.RefUniqueFile;
import org.nearbytalk.identity.VoteOfMeMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.query.PagedQuery.PagedInfo;
import org.nearbytalk.test.TestUtil;
import org.nearbytalk.test.misc.RandomUtility;
import org.nearbytalk.test.misc.ThreadTest;
import org.nearbytalk.util.DigestUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteBusyException;

public class SQLiteDataStoreSingleTest extends SQLiteDataStoreShareTest{

	private static Logger log=LoggerFactory.getLogger(SQLiteDataStoreSingleTest.class);

	public void testQueryAllClientUserInfo() throws SQLiteBusyException {

		List<ClientUserInfo> result = dataStore.queryUser("*", new PagedInfo(
				10, 0),QueryMethod.EXACTLY);

		assertNotNull(result);

	}
	

	/**
	 * query by topic should load all dependency 
	 * @throws SQLiteBusyException 
	 */
	public void testQueryByTopicWithDependency() throws SQLiteBusyException, ParseException{
		
		SaveChatBuildMessageResult result=saveChatBuildMessage(1);
		
		ChatBuildMessage topMost=result.topMost.get(0);
		
		assertTrue(!topMost.getTopics().isEmpty());
		
		for (String oneTopic: topMost.getTopics()) {
			
			List<AbstractMessage> queryBack=dataStore.queryTopic(oneTopic, true, new PagedInfo());
			
			boolean atLeast=false;
			for (AbstractMessage abstractMessage : queryBack) {
				
				chainIsComplete(abstractMessage);
				
				if (abstractMessage.getIdBytes().equals(topMost.getIdBytes())) {
					assertEquals(abstractMessage, topMost);
					atLeast=true;
				}
			}
			
			assertTrue(atLeast);
			
			
		}
	}

	public void testQuerySavedClientUserInfoExactly() throws SQLiteBusyException {

		checkQuery(true);

	}
	
	private void checkQuery(boolean exactly) throws SQLiteBusyException{
		ClientUserInfo randomUser = saveRandomUser();

		List<ClientUserInfo> result = dataStore.queryUser(
				randomUser.getUserName(), new PagedInfo(1000, 0),QueryMethod.EXACTLY);
		
		assertNotNull(result);
		assertTrue(result.contains(randomUser));
		
		log.debug("expected user {} not in result list {}",randomUser,result);

	}
	
	public void testQuerySavedClientUserInfoFuzzy() throws SQLiteBusyException {

		checkQuery(false);

	}

	public void testThreadQueryAllClientUserInfo() throws InterruptedException {

		assertTrue(ThreadTest.run(100, 100, 10, new ThreadTest.SingleTest() {

			@Override
			public void singleTest() {

				try {
					testQueryAllClientUserInfo();
				} catch (SQLiteBusyException e) {
					// justOk
				}

			}

			@Override
			public void threadLeaveCallback() {
				dataStore.threadRecycle();
			}
			
		}).isEmpty());
	}

	public void testSaveRandomUser() {

		final int max = 10;
		int counter = 0;
		while (counter++ < max) {

			try {
				saveRandomUser();
				return;
			} catch (SQLiteBusyException e) {
				e.printStackTrace();
			}
		}

		fail(max + "tries not success");
	}


	public void testRenameUser() throws SQLiteBusyException {

		ClientUserInfo randomUser = saveRandomUser();

		randomUser.setUserName(RandomUtility.nextString());

		assertTrue(dataStore.saveOrUpdateUser(randomUser));

	}

	public void testSaveNoneRefTextMessage() throws SQLiteBusyException, ParseException {

		PlainTextMessage saved=saveNoRefMessage(null);

		assertTrue(hasOnlyOneByQuery(saved));
	}
	
	public void testQuerySavedAnyMessageByKeywords() throws SQLiteBusyException, ParseException{
		ClientUserInfo randomUser = saveRandomUser();

		PlainTextMessage randomMessage = RandomUtility
				.randomNoRefTextMessage(randomUser);

		assertTrue(dataStore.saveMessage(randomMessage));
		
		
		Class typeCheck[] = { AbstractMessage.class, PlainTextMessage.class };

		for (Class clazz : typeCheck) {

			List<AbstractMessage> queried = dataStore.queryMessage(
					clazz, randomMessage.asPlainText(),
					new PagedInfo());

			boolean atLeast = false;

			for (AbstractMessage plainTextMessage : queried) {
				if (plainTextMessage.sameStrippedUser(randomMessage)) {
					atLeast = true;
				}

			}
			assertTrue(clazz.toString()+" check failed",atLeast);

		}
	}

	public void testQuerySavedTextMessageByKeywords() throws SQLiteBusyException,
			ParseException {

		saveNoRefMessage(null);

	}

	public void testQueryBatchSavedMessage() throws SQLiteBusyException,
			ParseException {

		ClientUserInfo info = saveRandomUser();

		List<AbstractMessage> batch = new ArrayList<AbstractMessage>();

		for (int i = 0; i < 1000; ++i) {
			batch.add(RandomUtility.randomNoRefTextMessage(info));
		}

		dataStore.saveMessage(batch);

		for (AbstractMessage message : batch) {

			List<PlainTextMessage> result = dataStore.queryMessage(
					PlainTextMessage.class, message.getIdBytes(), new PagedInfo());

			assertFalse(result.isEmpty());

			assertEquals(result.get(0), message);
		}
	}

	public void testNotSaveNoneExistsUserMessage() throws SQLiteBusyException {

		PlainTextMessage randomMessage = RandomUtility.randomNoRefTextMessage(null);

		assertFalse(dataStore.saveMessage(randomMessage));
	}

	public void test2Test() throws SQLiteBusyException, ParseException, BadReferenceException {
		testNotSaveNoneExistsUserMessage();
		testSaveTextReferenceMessage();
	}

	public void testSaveTextReferenceMessage() throws SQLiteBusyException, ParseException, BadReferenceException {

		saveRefMessage();
	}
	
	/**
	 * check if message in list is ordered as create date desc
	 * @param toCheck list to check ,can not be null or empty
	 */
	public <T extends AbstractMessage> void checkMessageOrderByCreateDateDesc(List<T> toCheck){
		
		assertNotNull(toCheck);
		
		assertFalse(toCheck.isEmpty());
		
		T prev=toCheck.get(0);
		
		for(T next:toCheck){
			
			long prevSeconds=prev.getCreateDate().getTime()/1000;
			
			long nextSeconds=next.getCreateDate().getTime()/1000;
			
			assertTrue(prevSeconds>=nextSeconds);
			
			prev=next;
		}
		
	}
	

	public void testQueryNewestShouldLoadDependency() throws Exception{
		
		
		SaveChatBuildMessageResult result=saveChatBuildMessage(1);
		
		List<AbstractMessage> newest=dataStore.queryNewest(AbstractMessage.class, new PagedInfo());
		
		Map<String,AbstractMessage> map=new HashMap<String,AbstractMessage>();
		
		for (AbstractMessage abstractMessage : newest) {

			map.put(abstractMessage.getIdBytes(), abstractMessage);
			
		}
		
		AbstractMessage topMost=result.topMost.get(0);
		
		assertTrue(newest.contains(topMost));
		
		chainIsComplete(topMost);
		
	}
	
	public void testQueryNewestOrderByDate() throws SQLiteBusyException, FileShareException, IOException, ParseException{
		
		PagedInfo singlePagedInfo=new PagedInfo();
		
		//test file share first
		for(int i=0;i<10;++i){
			saveRefereneceFileMessage();
		}
		
		List<RefUniqueFile> list=dataStore.queryNewest(RefUniqueFile.class, singlePagedInfo);
		
		checkMessageOrderByCreateDateDesc(list);
		
		
		for (int i = 0; i < 10 ; i++) {
			saveNoRefMessage(null);
		}
		
		List<PlainTextMessage> list2 = dataStore.queryNewest(PlainTextMessage.class, singlePagedInfo);
		
		checkMessageOrderByCreateDateDesc(list2);
		
		List<PlainTextMessage> mixedList= dataStore.queryNewest(PlainTextMessage.class, 
				new PagedInfo(singlePagedInfo.size*2, 1));
		
		checkMessageOrderByCreateDateDesc(mixedList);
	}
	
	public void testSaveRefUniqueMessage() throws SQLiteBusyException, FileShareException, IOException, ParseException{
		
		PlainTextMessage topMessage=saveRefereneceFileMessage();
		
		assertTrue(hasOnlyOneByQuery(topMessage.getReferenceMessage()));
	}

	public void testQueryNewestPlainTextMessage() throws ParseException,
			SQLiteBusyException, InterruptedException {
		
		//if we want to check this saved message is newest,must distinct 
		//it at create_date field (second is minimal unit)
		Thread.sleep(2000);
		
		PlainTextMessage saved=saveNoRefMessage(null);

		List<PlainTextMessage> queryResultList = dataStore.queryNewest(
				PlainTextMessage.class, new PagedInfo());
		
		assertNotNull(queryResultList);
		
		assertFalse(queryResultList.isEmpty());
		
		assertTrue("saved message should be newest in query result",queryResultList.get(0).equals(saved));

		for (AbstractMessage thisMessage : queryResultList) {
			assertNotNull(thisMessage.getSender());
			assertNotNull(thisMessage.getSender().getUserName());
			assertNull(thisMessage.getSender().getIdBytes());			
		}
		

	}

	public void testNameClash() throws SQLiteBusyException {
		ClientUserInfo randomUser1 = saveRandomUser();

		dataStore.saveOrUpdateUser(randomUser1);

		ClientUserInfo shouldNameClash = new ClientUserInfo(
				randomUser1.getUserName(),
				DigestUtility.oneTimeDigest(RandomUtility.nextString()));

		assertFalse(dataStore.saveOrUpdateUser(shouldNameClash));
	}

	public void testQuerySavedMessageByIdBytes() throws SQLiteBusyException,
			ParseException {
		ClientUserInfo randomUser = saveRandomUser();

		PlainTextMessage randomMessage = RandomUtility
				.randomNoRefTextMessage(randomUser);

		dataStore.saveMessage(randomMessage);

		List<PlainTextMessage> queried = dataStore.queryMessage(PlainTextMessage.class,
				randomMessage.getIdBytes(), new PagedInfo());

		assertNotNull(queried);

		assertEquals(1, queried.size());

		assertEquals(queried.get(0), randomMessage);
	}

	public void testBatchSaveMessages() throws SQLiteBusyException {

		ClientUserInfo randomUser = saveRandomUser();

		List<PlainTextMessage> all = new LinkedList<PlainTextMessage>();

		for (int i = 0; i < 1000; ++i) {
			all.add(RandomUtility.randomNoRefTextMessage(randomUser));
		}

		dataStore.saveMessage(all);

	}
	
	
	
	
	public void testLoadWithDenpendency() throws SQLiteBusyException, BadReferenceException, ParseException{
		
		AbstractMessage saved=saveRefMessage();
		
		AbstractMessage readBack=dataStore.loadWithDependency(saved.getIdBytes());
		
		assertNotNull(readBack);
		
		assertTrue(saved.sameStrippedUser(readBack));
		
		assertNotNull(readBack.getReferenceMessage());
		
		assertTrue(saved.getReferenceMessage().sameStrippedUser(readBack.getReferenceMessage()));
		
	}
	
	public void testDeleteCascadeNoRef() throws SQLiteBusyException, ParseException, BadReferenceException{
		
		PlainTextMessage msg=saveNoRefMessage(null);
		
		assertTrue(dataStore.delete(msg.getSender(),msg.getIdBytes(),true));
		
		assertFalse(hasOnlyOneByQuery(msg));
			
	}
	
	public void testDeleteCascadeWithRef() throws SQLiteBusyException, BadReferenceException, ParseException{
	
		PlainTextMessage msg=saveRefMessage();
		
		assertTrue(dataStore.delete(msg.getReferenceMessage().getSender(),msg.anyReferenceIdBytes(),true));
		
		assertFalse(hasOnlyOneByQuery(msg.getReferenceMessage()));
		
		assertFalse(hasOnlyOneByQuery(msg));
	}
	
	public void testDeleteNoCascade() throws SQLiteBusyException, BadReferenceException, ParseException {

		PlainTextMessage msg = saveRefMessage();

		//delete reference message
		dataStore.delete(msg.getReferenceMessage().getSender(),msg.anyReferenceIdBytes(), false);

		assertFalse(hasOnlyOneByQuery(msg.getReferenceMessage()));

		assertTrue(hasOnlyOneByQuery(msg,msg.asPlainText(),new Comparator<PlainTextMessage>() {

			@Override
			public int compare(PlainTextMessage o1, PlainTextMessage o2) {
				
				//when reference is deleted ,reference_id_bytes of read back will be different ,
				//but other field is the same.
				return (o1.getIdBytes().equals(o2.getIdBytes()) && o1.asPlainText().equals(o2.asPlainText()))?0:1;
			}
		},false));
	}

	public void testSaveReferenceFileMessage() throws SQLiteBusyException, FileShareException, IOException, ParseException{
		
		PlainTextMessage msg=saveRefereneceFileMessage();
		assertTrue(hasOnlyOneByQuery(msg));
	}

	public void testSaveRefMessage() throws SQLiteBusyException, ParseException, BadReferenceException{
		
		PlainTextMessage referenceOther=saveRefMessage();
		
		assertTrue(hasOnlyOneByQuery(referenceOther));

		AbstractMessage readBack=dataStore.loadWithDependency(referenceOther.getReferenceIdBytes());

		assertEquals("message is referenced,referenced_counter should be 1",1,readBack.getReferencedCounter());

	}
	
	private void checkFuzzyQuery(AbstractMessage shouldHave,String fullTopic) throws Exception{
		
				
		
		Set<String> fuzzyKeyword=new HashSet<String>();
		
		if (fullTopic.length()>4) {
			fuzzyKeyword.add(fullTopic.substring(0,2));
			fuzzyKeyword.add(fullTopic.substring(fullTopic.length()-2,fullTopic.length()));
			
			
			fuzzyKeyword.add(fullTopic.substring(1,fullTopic.length()-1));
		}
		
		
		for (String string : fuzzyKeyword) {
			
			List<AbstractMessage> queryBack=dataStore.queryTopic(string, true, new PagedInfo());
			
			assertEquals(1, Collections.frequency(queryBack, shouldHave));
			
			int index=queryBack.indexOf(shouldHave);
			
			AbstractMessage toCheck=queryBack.get(index);
			
			toCheck.parseTopics();
			
			assertEquals(shouldHave.getTopics(), toCheck.getTopics());
			
		}
	}
	
	public void testLeafMessageTopMost() throws SQLiteBusyException, ParseException{
		PlainTextMessage leaf=saveNoRefMessage(null);
		
		PlainTextMessage second=new PlainTextMessage(leaf.getSender(), leaf.asPlainText(),leaf);
		
		dataStore.saveMessage(second);
		
		List<AbstractMessage> readBack=dataStore.queryMessage(AbstractMessage.class, leaf.asPlainText(), new PagedInfo());
		
		assertEquals(2, readBack.size());
		
		assertTrue(readBack.contains(leaf));

		assertTrue(readBack.contains(second));
	}
	
	
	public void testSaveMessageWithTopicCheck() throws Exception{
		
		ClientUserInfo user=saveRandomUser();
		
		PlainTextMessage withTopics=RandomUtility.randomNoRefTextMessage(user);
		
		Set<String> topics=withTopics.getTopics();
		
		assertNotNull(topics);
		
		assertFalse(topics.isEmpty());

		dataStore.saveMessage(withTopics);
		
		for (String topic: topics) {
			List<AbstractMessage> queryBack=dataStore.queryTopic(topic,false ,new PagedInfo());
			
			assertNotNull(queryBack);

			assertFalse(queryBack.isEmpty());

			assertEquals(1,Collections.frequency(queryBack,withTopics));

			int index=queryBack.indexOf(withTopics);

			AbstractMessage queryBackOne=queryBack.get(index);

			queryBackOne.parseTopics();

			assertEquals(queryBackOne.getTopics(), topics);
			
			checkFuzzyQuery(withTopics, topic);

		}
		
	}
	
	/**
	 * save chain of chat build message, and return it.
	 * leaf at list head,top most at list end
	 * @return
	 * @throws SQLiteBusyException
	 * @throws ParseException 
	 */
	public void testSaveChatBuildMessage() throws SQLiteBusyException, ParseException{
		
		SaveChatBuildMessageResult result=saveChatBuildMessage(1);
		
		dataStore.saveMessage(result.allFlat);
		
		for (int i = 0; i < result.allFlat.size(); i++) {
			
			ChatBuildMessage thisOne=result.allFlat.get(i);
			
			ChatBuildMessage loadFromDb=(ChatBuildMessage) dataStore.loadWithDependency(thisOne.getIdBytes());
			
			assertEquals(thisOne, loadFromDb);
			
			ChatBuildMessage currentTop=loadFromDb;
			
			do {
				
				String referenceIdBytes=currentTop.getReferenceIdBytes();
				if (referenceIdBytes!=null) {
					
					ChatBuildMessage currentLeaf=currentTop.getReferenceMessage();
					assertNotNull(currentLeaf);
					
					currentTop=currentLeaf;
				}else{
					break;
				}
				
			} while (currentTop!=null);
			
		}
		
	}
	
	public void testSaveVoteTopicMessage() throws SQLiteBusyException, ParseException{
		saveVoteTopicMessage();
	}
	
	
	public void testSaveVoteOfMeMessage() throws SQLiteBusyException, BadReferenceException, ParseException{
		saveVoteOfMeMessage();
	}
	
	public void testManyVoteOfMeMessage() throws SQLiteBusyException, ParseException, BadReferenceException{
		VoteTopicMessage topic=saveVoteTopicMessage();
		
		ArrayList<VoteOfMeMessage> toSave=new ArrayList<VoteOfMeMessage>();
		
		Set<String> myOptions=new HashSet<String>();
		myOptions.add(topic.getOptions().iterator().next());
		
		for(int i=0;i<100;++i){
			
			ClientUserInfo temp=saveRandomUser();
			
			VoteOfMeMessage oneMeMessage=new VoteOfMeMessage(temp, "comment", myOptions, topic);
			
			toSave.add(oneMeMessage);
		}
		
		dataStore.saveMessage(toSave);
		
		for (VoteOfMeMessage expected: toSave) {
			
			VoteOfMeMessage loadBack=(VoteOfMeMessage) dataStore.loadWithDependency(expected.getIdBytes());
			
			assertEquals(expected, loadBack);
			
		}
	}
		

	public void testLoadMessageSender() throws SQLiteBusyException,
			ParseException {
		PlainTextMessage msg = saveNoRefMessage(null);

		List<PlainTextMessage> ret = dataStore.queryNewest(
				PlainTextMessage.class, new PagedInfo());

		TestUtil.assertContains(ret, msg,
				new TestUtil.PostCheck<PlainTextMessage>() {

					@Override
					public void postCheck(PlainTextMessage foundInCollection,
							PlainTextMessage toCheck) {
						
						assertEquals(toCheck.getSender().getIdBytes(), foundInCollection.getSender().getIdBytes());
						assertEquals(toCheck.getSender().getUserName(), foundInCollection.getSender().getUserName());
					}
			
				});
	}

}
