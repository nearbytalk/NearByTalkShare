package org.nearbytalk.identity;

import java.util.Date;

import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.runtime.GsonThreadInstance;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PwdCryptoMessage extends AbstractTextMessage{
	
	public static final MessageType MESSAGE_TYPE=MessageType.PWD_CRYPTO;

	public PwdCryptoMessage(String idBytes, BaseUserInfo sender,
			 Date createDate,String text,int referencedCounter,int agreeCounter,int disagreeCounter){
		super(idBytes,sender,MESSAGE_TYPE,createDate,referencedCounter,agreeCounter,disagreeCounter);
		// TODO Auto-generated constructor stub
		parseJson(text);
		
		setReferenceDepth(1);
	}
	
	private String receiverName;
	
	public final String RECEIVER_NAME_KEY="receiverName";
	
	private String receiverIdBytes;

	private void parseJson(String jsonString){
				Gson gson = GsonThreadInstance.FULL_GSON.get();

		JsonElement element = gson.fromJson(jsonString, JsonElement.class);

		//just throw
		JsonObject object = element.getAsJsonObject();

		receiverName= jsonElementToType(String.class, object, RECEIVER_NAME_KEY);

	}

	@Override
	public void digestId() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getReferenceIdBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AbstractMessage getReferenceMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String asPlainText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setReferenceMessageLater(AbstractMessage message)
			throws BadReferenceException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invalid() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void parseTopics() {
		//pwdcrypto message do not have cross index
	}

	@Override
	public void replaceReferenceMessage(AbstractMessage toReplace) {
		
		//do nothing
	}
	
}
