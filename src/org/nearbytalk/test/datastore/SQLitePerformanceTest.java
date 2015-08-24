package org.nearbytalk.test.datastore;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.ChatBuildMessage;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.query.PagedQuery.PagedInfo;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.test.misc.RandomUtility;
import org.nearbytalk.test.misc.ThreadTest;
import org.nearbytalk.test.misc.ThreadTest.SingleTest;
import org.nearbytalk.util.Utility;

import com.almworks.sqlite4java.SQLiteBusyException;
import com.almworks.sqlite4java.SQLiteException;

public class SQLitePerformanceTest extends SQLiteDataStoreShareTest {

	public SQLitePerformanceTest() throws SQLiteException {
		super();
		// TODO Auto-generated constructor stub
	}

	private final int ONE_THSOUND = 1000;

	public void testSingleThreadMultiWrite() throws SQLiteBusyException {
		
		for(PlainTextMessage message:generateRandomMessagesFlatWithRefAndSavedUser(ONE_THSOUND)){
			
			dataStore.saveMessage(message);
			
		}

	}

	public void testSingleThreadWrite() throws SQLiteBusyException {
		writeMessages();
	}

	/**
	 * generate random messages of same user(auto generated and saved)
	 * @param number
	 * @return
	 * @throws SQLiteBusyException
	 */
	public ArrayList<PlainTextMessage> generateRandomMessagesFlatWithRefAndSavedUser(int number) throws SQLiteBusyException {

		ClientUserInfo user = saveRandomUser();

		ArrayList<PlainTextMessage> list = new ArrayList<PlainTextMessage>(number*2);

		for (int i = 0; i < number; i++) {
			PlainTextMessage withRef=RandomUtility.randomRefTextMessage(user);
			list.add((PlainTextMessage) withRef.getReferenceMessage());
			list.add(withRef);
		}

		return list;
	}

	public void writeMessages() throws SQLiteBusyException {
		dataStore.saveMessage(generateRandomMessagesFlatWithRefAndSavedUser(ONE_THSOUND));
	}

	class WriteThread extends Thread {
		public void run() {
			try {
				writeMessages();
			} catch (SQLiteBusyException e) {
				ok = false;
			}
		}

		public boolean ok = true;
	}

	public void testSingleThreadWriteWithQuery() throws InterruptedException {

		WriteThread writeThread = new WriteThread();

		writeThread.start();

		testThreadQuery();

		writeThread.join();

		assertTrue(writeThread.ok);
	}

	public void testThreadQuery() throws InterruptedException {

		Collection<Exception> erros = ThreadTest.run(10, 100, 10,
				new SingleTest() {

					@Override
					public void singleTest() throws Exception {

						dataStore.queryMessage(PlainTextMessage.class,
								RandomUtility.nextString(), new PagedInfo());

					}
				});

		assertTrue(erros.isEmpty());

	}

	public void noTestThreadWrite() throws SQLiteBusyException,
			InterruptedException {

		final ArrayList<PlainTextMessage> randomMessages = generateRandomMessagesFlatWithRefAndSavedUser(ONE_THSOUND);

		final int splite = 500;

		Collection<Exception> errors = ThreadTest.run(randomMessages.size()
				/ splite, 1, 0, new SingleTest() {

			@Override
			public void singleTest(int threadIndex, int threadNumber)
					throws Exception {

				if(!dataStore.saveMessage(randomMessages.subList(threadIndex
						* splite, (threadIndex + 1) * splite))){
					
					throw new IllegalStateException("batch save not success");
				}

			}

		});

		assertTrue(errors.isEmpty());

	}
	
	public void testFtsQueryPerformance() throws SQLiteBusyException, ParseException{
		

		ArrayList<PlainTextMessage> randomList=generateRandomMessagesFlatWithRefAndSavedUser(ONE_THSOUND) ;
		
		dataStore.saveMessage(randomList);
		
		for (AbstractMessage plainTextMessage : randomList) {
			
			String plainText=plainTextMessage.asPlainText();
			
			List<AbstractMessage> shouldBe=dataStore.queryMessage(AbstractMessage.class, plainText, new PagedInfo());
			
			assertEquals(1, shouldBe.size());
			
			assertTrue(shouldBe.get(0).sameStrippedUser(plainTextMessage));
			
			String[] toSearch={
					plainText.substring(1),
					plainText.substring(2),
					plainText.substring(0,plainText.length()-1),
					plainText.substring(0,plainText.length()-2),
					plainText.substring(plainText.length()/2),
					plainText.substring(0,plainText.length()/2)
			};
			
			for (String string : toSearch) {
				
				List<AbstractMessage> queryBack=dataStore.queryMessage(AbstractMessage.class, string, new PagedInfo());
				
				assertTrue(!queryBack.isEmpty());
				
			}
			
		}
	}
	
	
	/**
	 * test how fast dependency is loaded
	 * @throws SQLiteBusyException
	 * @throws ParseException 
	 */
	public void testLoadChatBuildMessages() throws SQLiteBusyException, ParseException{

		SaveChatBuildMessageResult result=saveChatBuildMessage(ONE_THSOUND);
		
		
		//we only test top most load speed, avoid short chains
		
		for (ChatBuildMessage chatBuildMessage : result.topMost) {
			AbstractMessage loadFromDb=dataStore.loadWithDependency(chatBuildMessage.getIdBytes());
			
			assertEquals(loadFromDb, chatBuildMessage);
		}
	}
	
	public static interface PagedQueryAction{
		List<AbstractMessage> action(PagedInfo info) throws SQLiteBusyException, ParseException;
	}

	private void pageCheckQueryMessage(int pageSize,
			Map<String, ? extends AbstractMessage> shouldHas,PagedQueryAction pagedQueryAction) throws SQLiteBusyException, ParseException {

		List<AbstractMessage> readBack;

		PagedInfo info=new PagedInfo();
		
		Set<String> loadBack=new HashSet<String>();
		
		while(true){

			
			readBack=pagedQueryAction.action(info);
			
			if(readBack==null || readBack.isEmpty()){
				break;
			}
			
			
			
			for (AbstractMessage abstractMessage : readBack) {
				String idBytes=abstractMessage.getIdBytes();
				assertTrue(shouldHas.containsKey(idBytes));
				assertEquals(shouldHas.get(idBytes),abstractMessage);
				
				chainIsComplete(abstractMessage);
				
				loadBack.add(idBytes);
			}
			
			if(readBack.size()<info.size){
				break;
			}
			
			info.index+=1;
		} 
		
		assertTrue(shouldHas.keySet().containsAll(loadBack));
		assertEquals(shouldHas.size(),loadBack.size());
		
	}
	
	/**
	 * test query and load dependency speed( deep ChatBuildMessage) 
	 * @throws SQLiteBusyException 
	 * @throws ParseException 
	 */
	public void testQueryChatBuildMessage() throws SQLiteBusyException, ParseException{

		ClientUserInfo user=saveRandomUser();

		List<ChatBuildMessage> all=new ArrayList<ChatBuildMessage>();
		
		Map<String,ChatBuildMessage> topMost=new HashMap<String,ChatBuildMessage>();
		
		final String sameContent=RandomUtility.nextString();

		for(int i=0;i<ONE_THSOUND;++i){

			List<ChatBuildMessage> toAdd=RandomUtility.randomChatBuildMessage(user, Global.REFERENCE_MAX_DEPTH,sameContent);
			
			ChatBuildMessage top=toAdd.get(toAdd.size()-1);
			
			topMost.put(top.getIdBytes(),top);
			
			for (ChatBuildMessage chatBuildMessage : toAdd) {
				all.add(chatBuildMessage);
			}

		}
		
		dataStore.saveMessage(all);
		
		//we only test top most load speed, avoid short chains
		
		pageCheckQueryMessage(20,topMost, 
				new PagedQueryAction() {
			@Override
			public List<AbstractMessage> action(PagedInfo info) throws SQLiteBusyException, ParseException {
				return dataStore.queryMessage(AbstractMessage.class, sameContent,info);
			}
		});

		pageCheckQueryMessage(20,topMost, 
				new PagedQueryAction() {
			@Override
			public List<AbstractMessage> action(PagedInfo info) throws SQLiteBusyException,ParseException {
				return (List<AbstractMessage>)(List)dataStore.queryMessage(ChatBuildMessage.class, sameContent,info);
			}
		});

		
	}

	public void testQueryByTopicWithDependency() throws SQLiteBusyException{
		
		ClientUserInfo randomUserInfo=saveRandomUser();
		
	}
	
	public void testQueryByTopic() throws SQLiteBusyException, ParseException {
		
		List<PlainTextMessage> toSave=new ArrayList<PlainTextMessage>();
		
		Map<String, PlainTextMessage> toCheckMap=new HashMap<String, PlainTextMessage>();
		
		ClientUserInfo user=saveRandomUser();
		
		final String commonTopic=RandomUtility.nextString(Utility.MAX_TOPIC_LENGTH);
		
		for (int i = 0; i < ONE_THSOUND; i++) {
			
			PlainTextMessage thisOne=RandomUtility.randomNoRefTextMessage(user, "#"+commonTopic+"#",false);
			toSave.add(thisOne);
			toCheckMap.put(thisOne.getIdBytes(), thisOne);
		}
		
		dataStore.saveMessage(toSave);
		
		pageCheckQueryMessage(20,toCheckMap, new PagedQueryAction() {
			
			@Override
			public List<AbstractMessage> action(PagedInfo info) throws SQLiteBusyException, ParseException{
				return dataStore.queryTopic(commonTopic.substring(1,commonTopic.length()), true, info);
			}
		});
	}

	
}
