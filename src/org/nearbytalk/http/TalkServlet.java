package org.nearbytalk.http;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.exception.DataStoreException;
import org.nearbytalk.exception.DuplicateMessageException;
import org.nearbytalk.exception.FileShareException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.AbstractMessage.MessageType;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.identity.RefUniqueFile;
import org.nearbytalk.identity.VoteOfMeMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.runtime.UniqueObject;
import org.nearbytalk.service.MessageService;
import org.nearbytalk.service.ServiceInstanceMap;
import org.nearbytalk.util.DigestUtility;
import org.nearbytalk.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteException;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

/**
 * 
 * this is where user send message to
 */
public class TalkServlet extends AbstractServlet {

	private MessageService service = ServiceInstanceMap.getInstance().getService(
			MessageService.class);
	
	static Logger log=LoggerFactory.getLogger(TalkServlet.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		super.doPost(req, resp);
	}
	
	/**
	 * parse request as TextMessage
	 * (if this is a file share message,all reference is prepared)
	 * or throws exception when error happened
	 * (file too large,is multipart request but no fileItem)
	 * return will always none-null
	 * @param req
	 * @return
	 * @throws JsonSyntaxException
	 * @throws IOException
	 * @throws FileUploadException
	 * @throws FileShareException
	 * @throws BadReferenceException 
	 */
	private AbstractMessage parseRequest(HttpServletRequest req)
			throws JsonSyntaxException, IOException,
			FileUploadException, FileShareException, BadReferenceException
			{

		Gson gson=GsonThreadInstance.FULL_GSON.get();
		
		if(!ServletFileUpload.isMultipartContent(req)){
			//for compatible only
			AbstractMessage ret=gson.fromJson(req.getReader(), AbstractMessage.class);
			
			if (ret==null) {
				return null;
			}
			
			SessionUserData userData=getFromSession(req.getSession());
			
			ret.setSenderLater(userData.loginedUser);
			return ret;
		}
		
		PlainTextMessage ret=null;
		
		ServletFileUpload upload=new ServletFileUpload();

		FileItemIterator iterator=upload.getItemIterator(req);

		RefUniqueFile refCountFile=null;
		
		boolean fileParsed=false,
				textParsed=false;

		while (iterator.hasNext() && !(fileParsed&&textParsed)) {

			FileItemStream item = iterator.next();
			String name = item.getFieldName();

			if (item.isFormField() && !textParsed) {

				if (!MessageType.PLAIN_TEXT.toString().equals(name)) {
					log.warn("non-standard text message field name:{}",name);
				}

				//although we already know it's PlainTextMessage,but AbstractMessage json deserialize
				//has extra process, so we still needs to be deserialize through AbsractMessage
				ret=gson.fromJson(
						new JsonReader(new InputStreamReader(item.openStream())), AbstractMessage.class);
				
				// here we assume user info in session
				//(user with no session will be rejected by session filter)
				ClientUserInfo userInfo = (ClientUserInfo) req.getSession()
						.getAttribute(ClientUserInfo.class.getSimpleName());

				ret.setSenderLater(userInfo);
				
				log.debug("text message from multi part request parsed:{}",ret);

				textParsed=true;

			} else if(!fileParsed){
				
				if (!MessageType.REF_UNIQUE.toString().equals(name)) {
					log.warn("none-standard file share field name:{}",name);
				}

				//delete message is a delay action, so if we can save file
				//successful, we can assume this file is valid until 
				//server shutdown 
				//no other thread will delete the file
				//only server GC action can delete unused file
				refCountFile=Utility.writeUploadStream(item.openStream(),
						Global.getInstance().fileUploadLimitByte, item.getName(),
						UniqueObject.getInstance().getDataStore().getFileKey());

				log.debug("file part of multi part request parsed:{}",refCountFile.getFileName());
				
				fileParsed=true;
			}
		}

		if (!(fileParsed && textParsed)) {
			log.error("incomplete file share message: has text:{},has file:{}",
					fileParsed, textParsed);
			
			throw new FileShareException(
					ErrorResponse.FIELD_INCOMPLETE);
		}
		
		ret.setReferenceMessageLater(refCountFile);
		
		return ret;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			talk(req, resp);
		} catch (Exception e) {
			
			log.error("Talk failed", e);
			throw new IOException(e);
		}
	}
	
	public static class TextMessageChecker{
		
		public static ErrorResponse check(AbstractMessage message){
			if (message==null) {
				return ErrorResponse.INVALID_MESSAGE;				
				
			}
			if (message.getMessageType()==null) {
				return ErrorResponse.INVALID_MESSAGE_TYPE;				
			}
			
			if (message.getReferenceIdBytes()!=null && 
					!DigestUtility.isValidSHA1(message.getReferenceIdBytes())) {
				return ErrorResponse.INVALID_ID_BYTES;
				
			}
			
			if (message instanceof VoteTopicMessage) {
				VoteTopicMessage cast=(VoteTopicMessage) message;
				
				if (cast.getResults()==null) {
					//this is a special check
					//VoteTopic use Map<option,number> to backstore options 
					//so must check if this map is null
					return ErrorResponse.INVALID_MESSAGE;
				}
				
				if (cast.getVoteTopic()==null || Utility.isEmptyString(cast.getVoteTopic())) {
					return ErrorResponse.INVALID_MESSAGE;
				}
				
				if (cast.getOptions()==null || cast.getOptions().size()<2) {
					return ErrorResponse.INVALID_MESSAGE;
				}
				
				for (String thisOption:cast.getOptions()) {
					//de-json will not give null string
					if (Utility.isEmptyString(thisOption)) {
						return ErrorResponse.INVALID_MESSAGE;
					}					
				}
				
				//TODO check message length
				return null;
			}
			
			
			if (message.asPlainText()==null || message.asPlainText().isEmpty()) {
				return ErrorResponse.MESSAGE_CANNOT_EMPTY;
			}
			
			
			
			if (message instanceof VoteOfMeMessage && 
					(message.getReferenceIdBytes()==null)) {
				//VoteOfMeMessage must reference to VoteTopicMessage
				return ErrorResponse.FIELD_INCOMPLETE;
			}
			
			return null;
			
		}
		
	}

	private void talk(HttpServletRequest req, HttpServletResponse resp) throws JsonIOException, IOException, JsonSyntaxException, FileUploadException{

		AbstractMessage message = null;
		
		try{
			message=parseRequest(req);
		}catch(FileShareException e){
			sendErrorResponse(resp, e.getError());
			return;
		}catch(BadReferenceException e){
			sendErrorResponse(resp,ErrorResponse.BAD_REFERENCE);
			return;
		}
		
		
		
		ErrorResponse error=TextMessageChecker.check(message);

		if (error != null) {
			log.debug("error is {}",error);
			sendErrorResponse(resp, error);
			return;
		}
		// check other info


		SessionUserData sessionUserData=(SessionUserData) req.getSession().getAttribute(SessionUserData.SESSION_USER_DATA_KEY);
	
		
		message.setCreateDateLater(Calendar.getInstance().getTime());
		// generate digest id
		
		message.setSenderLater(sessionUserData.loginedUser);

		message.digestId();
		
		
		if (!(message instanceof VoteOfMeMessage) && !(message instanceof VoteTopicMessage)) {
			
			try {
				serviceStageUnlocked(message, req, resp);
				return;
			} catch (DataStoreException e) {
				sendErrorResponse(resp, ErrorResponse.SERVER_BUSY);
				return;
			} catch (DuplicateMessageException e) {
				//impossible for none vote related 
				assert false;
				return;
			} 
		}

		//VoteOfMe/VoteTopic Message needs to be pre-checked , or
		// delay save logic may break up incorrect duplicate message
		// in runtime. (etc. duplicate vote to same topic will be seen at runtime, but not allowed in db)
		//
		//HttpSession pre-check cache help to reduce second stage MessageCache
		//datastore I/O action. this also help PollServlet 

		boolean isVoteOfMe = (message instanceof VoteOfMeMessage);

		String voteTopicMessageIdBytes=(isVoteOfMe?message.getReferenceIdBytes():message.getIdBytes());
		
		String voteOfMeMessageIdBytes=(isVoteOfMe?message.getIdBytes():null);

		//this sync assume different thread will not set a new ClientUserInfo
		//to session data during checking message can be voted.
		//if not , we may giving back incorrect result while concurrent Login request received
		synchronized (sessionUserData) {
			try {
				if(!canVoteUnlocked(sessionUserData,voteOfMeMessageIdBytes,voteTopicMessageIdBytes)){
					sendErrorResponse(resp, ErrorResponse.DUPLICATE_MESSAGE);
					return;
				}
			} catch (SQLiteException e1) {
				sendErrorResponse(resp, ErrorResponse.SERVER_BUSY);
				return;
			}
			
			try {
				if(serviceStageUnlocked(message, req, resp)){
					//successful send result ,update pre-cache for later use
					
					if (isVoteOfMe) {
						sessionUserData.voteInfo.put(voteTopicMessageIdBytes,true);
						Boolean prev=sessionUserData.voteInfo.put(message.getIdBytes(), false);
						//user should not have voted on it previously
						assert prev==null;
					}else {
						//this message is vote topic
						Boolean prev=sessionUserData.voteInfo.put(voteTopicMessageIdBytes, false);
						//if VoteTopicMessage not talked, there will be not VoteOfMessage update it's value to true
						assert prev==null;
					}
					return;
				}
			} catch (DataStoreException e) {
				//TODO expose new error ?
				//not success, should not update pre-cache 
				sendErrorResponse(resp, ErrorResponse.SERVER_BUSY);
				return;
			} catch (DuplicateMessageException e) {
				
				// pre-cache didn't contain enough info, checked by message cache
				// added to pre-cache
				if (isVoteOfMe) {
					//user haved vote same topic
					Boolean prev=sessionUserData.voteInfo.put(voteTopicMessageIdBytes,true);

					assert prev==null;
					prev=sessionUserData.voteInfo.put(voteOfMeMessageIdBytes,false);
					assert prev==null;
				}else{
					//user have talked same topic
					//but don't know if user have voted it, just put as not voted
					//next time check will override this
					Boolean prev=sessionUserData.voteInfo.put(voteTopicMessageIdBytes, false);
					assert prev==null;
				}
				sendErrorResponse(resp, ErrorResponse.DUPLICATE_MESSAGE);
				return;
			}

		}
		
	}
	
	private boolean serviceStageUnlocked(AbstractMessage message,HttpServletRequest req,HttpServletResponse resp) throws DataStoreException, JsonIOException, IOException, DuplicateMessageException{
		try {
			service.talk(message);
			log.debug("talk success :{}",message);
			sendOkResultStripUserIdBytes(resp, message.getIdBytes());
			return true;
		} catch (BadReferenceException e) {
			sendErrorResponse(resp,ErrorResponse.BAD_REFERENCE);
			return false;
		} 
	}

	/**
	 * 
	 * early reject Invalid voteOfMe or VoteTopic which is invalid
	 *  
	 * @param alreadyLockeData
	 * @param voteOfMeIdBytes
	 * @param voteTopicIdBytes
	 * @return
	 * @throws SQLiteException
	 */
	private boolean canVoteUnlocked(SessionUserData alreadyLockeData, String voteOfMeIdBytes,String voteTopicIdBytes) throws SQLiteException {
		
		
		// can vote check can safety passed the MessageCache logic
		// direct talk to DataStore
		//
		// since user can only Talk from his/her http session
		// the voted record already in SessionUserData 
		// if not , after successful saved (to MessageCache, can be assumed being saved to datastore successfully) 
		// then it will be added to SessionUserData

		Boolean voted=alreadyLockeData.voteInfo.get(voteTopicIdBytes);
		if (voted!=null) {
			
			if (voteOfMeIdBytes == null) {
				//we are checking for if this VoteTopicMessage exists
				
				//this VoteTopic id already exits, user already talked this.
				return false;
			}
			
			//we are checking VoteOfMeMessage
			
			return !voted;
		}
		

		ClientUserInfo clientUserInfo=alreadyLockeData.loginedUser;

		//it must be filtered out if not called login
		assert clientUserInfo!=null;

		//don't know if the topic is valid try as it's exists, ask datastore
		
		Set<String> set=new HashSet<String>();
		set.add(voteTopicIdBytes);
		// we may use a unlock/re-check approach to avoid this IO action in sync block
		List<Boolean> queryResult = UniqueObject.getInstance().getDataStore().queryVoted(clientUserInfo, set, false);
		
		assert queryResult.size()==1;
		
		Boolean result=queryResult.get(0);
		
		if (result==null) {
			//VoteTopicMessage not in datastore, maybe in messagecache ,return as it can be voted
			//second servicestage will check result in messagecache. if VoteTopicMessage and VoteOfMeMessage
			//all in the messageCache, later serviceStage will throw
			return true;
		}
		
		if (result) {
			//already voted
			
			alreadyLockeData.voteInfo.put(voteTopicIdBytes,true);
			return false;
		}else{
			
			//we can not added it right now, since we must second check in serviceStage
			return true;
		}
		
	}
}
