package org.nearbytalk.test.misc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.exception.FileShareException;
import org.nearbytalk.identity.BaseUserInfo;
import org.nearbytalk.identity.ChatBuildMessage;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.identity.RefUniqueFile;
import org.nearbytalk.identity.VoteOfMeMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.util.DigestUtility;
import org.nearbytalk.util.Utility;


public class RandomUtility {
	
	private static Random random=new Random();
	
	public static String randomIdBytesString(){
		return DigestUtility.byteArrayToHexString(DigestUtility.oneTimeDigest(nextString()));
	}
	
	public static String nextString(){
		return nextString(20);
	}

	/**
	 * generate random string(must be valid unicode string)
	 * 
	 * @return
	 */
	public static String nextString(int maxNumber) {

		char longChars[] = new char[maxNumber];

		for (int i = 0; i < longChars.length; ++i) {

			while (true) {
				
				char temp;
				synchronized(random){
				temp = (char)random .nextInt(
						NormalChineseChars.ALL.length() + 256);
				}

				if ((temp >= 'a' && temp <= 'z')
						|| (temp >= 'A' && temp <= 'Z') 
						|| temp >= '0'&& temp <= '9') {
					longChars[i] = temp;
					break;

				} else {
					int index = temp % NormalChineseChars.ALL.length();
					longChars[i] = NormalChineseChars.ALL.charAt(index);
					break;
				}
			}
		}

		return new String(longChars);

	}
	
	public static RefUniqueFile randomUploadFile() throws FileShareException, IOException{
		
		String randomString=nextString();
		
		InputStream randomStream=new ByteArrayInputStream(randomString.getBytes());
		
		return Utility.writeUploadStream(randomStream, Integer.MAX_VALUE, randomString+".txt");
		
	}

	/**
	 * generate ref text message with depth (1= noref)
	 * @param info
	 * @param depth
	 * @return
	 */
	public static PlainTextMessage randomRefTextMessage(ClientUserInfo info){
		assert info!=null;
		PlainTextMessage nowTop=null;
		
		PlainTextMessage leafMessage=randomNoRefTextMessage(info);
		nowTop=new PlainTextMessage(leafMessage.getSender(), nextString()+randomTopicsString());
		try {
			nowTop.setReferenceMessageLater(leafMessage);
		} catch (BadReferenceException e) {
			//impossible
		}
		nowTop.digestId();

		return nowTop;
	}
	
	/**
	 * create a random file share message
	 * @param info if null,create random user
	 * @return
	 * @throws FileShareException
	 * @throws IOException
	 */
	public static PlainTextMessage randomFileShareMessage(ClientUserInfo info) throws FileShareException, IOException{
		RefUniqueFile randomFile=randomUploadFile();
		
		PlainTextMessage ret=randomNoRefTextMessage(info);
		try {
			ret.setReferenceMessageLater(randomFile);
		} catch (BadReferenceException e) {
			//impossible
		}
		return ret;
	}
	
	public static String randomTopicsString(){
		
		StringBuilder builder=new StringBuilder();
		
		int loop=random.nextInt(Utility.MAX_TOPIC_NUMBER*2);
		
		for (int i = 0; i < Math.max(loop,1); i++) {
			builder.append("#").append(nextString(Utility.MAX_TOPIC_LENGTH)).append("#");
		}
		
		return builder.toString();
	}

	/**
	 * generate random chat build message, with topic parsed
	 * @param sender
	 * @param depth
	 * @return
	 */
	public static List<ChatBuildMessage> randomChatBuildMessage(ClientUserInfo sender,int depth){
		return randomChatBuildMessage(sender, depth,null);
	}
	/**
	 * generate a chain of chatbuildMessage. leaf node first, top node last
	 * if topString is not null, topMost message will have content of topString, for performance test
	 * @param sender
	 * @param depth
	 * @param topString topMost ChatBuildMessage content, random if null.
	 * @return
	 */
	public static List<ChatBuildMessage> randomChatBuildMessage(ClientUserInfo sender,int depth,String topString){
		
		if (depth==0) {
			return null;
		}
		
		List<ChatBuildMessage> ret=new ArrayList<ChatBuildMessage>();
		
		ChatBuildMessage leaf=null;
		
		ClientUserInfo useSender=(sender==null?randomUser():sender);

		for (int i = 0; i < depth; i++) {
			
			ChatBuildMessage thisOne;
			if (topString!=null && i==depth-1) {
				thisOne=new ChatBuildMessage(useSender, topString+randomTopicsString(), leaf);
			}else{
				thisOne=new ChatBuildMessage(useSender, nextString()+randomTopicsString(), leaf);
			}
			
			thisOne.parseTopics();

			ret.add(thisOne);
			leaf=thisOne;
		}
		
		return ret;
	}

	/**
	 * generate a PlainTextMessage, with no reference, topics already parsed
	 * @param clientUserInfo
	 * @return
	 */
	public static PlainTextMessage randomNoRefTextMessage(
			ClientUserInfo clientUserInfo) {
		return randomNoRefTextMessage(clientUserInfo, null,true);
	}
	/**
	 * create random PlainTextMessage with no ref and random topics 
	 * topics already parsed
	 * @param clientUserInfo
	 * @param contentSeed used as part of content
	 * @return
	 */
	public static PlainTextMessage randomNoRefTextMessage(
			ClientUserInfo clientUserInfo,String contentSeed,boolean atLeastTopic) {

		PlainTextMessage ret = null;

		//when contentSeed is not null, we must assume randomTopicString is not zero
		//or this will generate same message between calls
		ret=new PlainTextMessage(clientUserInfo==null?randomUser():clientUserInfo, 
				(contentSeed!=null?contentSeed:RandomUtility.nextString())+
				(atLeastTopic?randomTopicsString():nextString()));
		
		ret.parseTopics();
		
		return ret;

	}

	public static ClientUserInfo randomUser() {

		ClientUserInfo ret = new ClientUserInfo(RandomUtility.nextString(),
				DigestUtility.oneTimeDigest(RandomUtility.nextString()));
		ret.setDescription(nextString());

		ret.setCreateDate(Calendar.getInstance().getTime());

		return ret;

	}

	public static VoteTopicMessage randomVoteTopicMessage(BaseUserInfo sender) {
		if(sender==null){
			sender=randomUser();
		}
		
		Set<String> options=new HashSet<String>();
		
		int number=random.nextInt(5)+2;
		
		while(true){
			
			options.add(nextString(12));
			if (options.size()==number) {
				break;
			}
		}
		
		return new VoteTopicMessage(sender, "topic"+nextString(), nextString(), random.nextBoolean(), new ArrayList(options));
	}
	
	public static VoteOfMeMessage randomVoteOfMeMessage(BaseUserInfo sender,VoteTopicMessage topic) throws BadReferenceException{
		if (sender==null) {
			sender=randomUser();
		}
		
		Set<String> options=new HashSet<String>();
		
		ArrayList<String> alloptions=new ArrayList<String>(topic.getOptions());
		
			
		int selectNumber=1;
		if (topic.isMultiSelection()) {
			selectNumber=Math.max(1,random.nextInt(alloptions.size()));
		}

		for(int i=0;i<selectNumber;++i){
			options.add(alloptions.get(random.nextInt(alloptions.size())));
		}

		return new VoteOfMeMessage(sender, "comment"+nextString(10), options, topic);
		
	}
	
	public static byte[] randomBytes16(){
		
		byte[] ret=new byte[16];
		random.nextBytes(ret);
		return ret;
	}
}
