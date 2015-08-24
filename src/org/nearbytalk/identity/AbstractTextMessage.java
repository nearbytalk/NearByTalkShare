package org.nearbytalk.identity;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import org.nearbytalk.runtime.DateFormaterThreadInstance;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.util.DigestUtility;
import org.nearbytalk.util.Utility;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class AbstractTextMessage extends AbstractMessage {

	/**
	 * used for construct from db
	 * 
	 * @param sender
	 * @param messageType
	 * @param createDate
	 */
	protected AbstractTextMessage(String idBytes,BaseUserInfo sender,MessageType messageType,Date createDate
			,int referencedCounter,int agreeCounter,int disagreeCounter){
		super(idBytes,sender,messageType,createDate,
				referencedCounter,agreeCounter,disagreeCounter);
	}
	
	protected AbstractTextMessage(BaseUserInfo sender,MessageType messageType){
		super(sender,messageType,Calendar.getInstance().getTime());
	}
	
	

	private Set<String> topics;
	
	protected void setTopics(Set<String> topics) {
		this.topics = topics;
	}
	
	

	
	
	
	protected <T> T jsonElementToType(Type type, JsonObject obj, String key) {

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		JsonElement valueElement = obj.get(key);

		if (valueElement == null) {
			return null;
		}

		return gson.fromJson(valueElement, type);
	}
		
	/**
	 * create idBytes based on common fields
	 * @param withCreateDate if true, createDate is considered as part of digest idBytes
	 *        for {@code PlainTextMessage},createDate is considered as part of digest
	 *        same user/text but different date is treated as different message
	 *        {@code VoteTopicMessage} {@code VoteOfMeMessage} createDate is not considered
	 *        as part of digest idBytes,so same topic vote or same vote of me is only allowed once
	 * 
	 */
	protected void digestId(boolean withCreateDate){
		
		MessageDigest digest = DigestUtility.getSHA1Digest();

		synchronized (digest) {

			digest.reset();

			try {
				digest.update(getMessageType().toString().getBytes(Global.UTF8_ENCODING));
			} catch (UnsupportedEncodingException e1) {
				//impossible
			}

			digest.update(DigestUtility.hexStringToByteArray(getSender()
					.getIdBytes()));
			
			String additional=additionalDigestString();
			
			if (additional!=null) {
				digest.update(Utility.stringUTF8Bytes(additional));
			}

			String anyReferenceId=anyReferenceIdBytes();

			if (anyReferenceId!= null) {
				digest.update(Utility.stringUTF8Bytes(anyReferenceId));
			}

			if (withCreateDate) {
				try {
					digest.update(DateFormaterThreadInstance.get()
							.format(this.getCreateDate())
							.getBytes(Global.UTF8_ENCODING));
				} catch (UnsupportedEncodingException e) {
					//impossible
				}
			}

			setIdBytes(digest.digest());

		}
	}
	
	/**
	 * additional Digest string except basic ones (sender_id,ref_id,create_date(or not))
	 * for basic text message , just use asPlainText is OK
	 * for VoteOfMe/VoteTopic , needs to identified by key content, should not be different across create_date,
	 * 							just return the const part of msg 
	 * @return
	 */
	protected String additionalDigestString(){
		return asPlainText();
	}
	
	@Override
	public void parseTopics() {
		setTopics(Utility.parseTopics(asPlainText()));
	}

	@Override
	public final Set<String> getTopics() {
		return topics;
	}
	
}
