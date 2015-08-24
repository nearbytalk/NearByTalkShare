package org.nearbytalk.identity;

import java.util.Date;

import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.runtime.Global;


public class ChatBuildMessage extends CompoundMessage<ChatBuildMessage>{

	public static final MessageType MESSAGE_TYPE = MessageType.CHAT_BUILD;
	
	private String plainText;
	
	public ChatBuildMessage(String idBytes, BaseUserInfo sender, String text,
			Date createDate,String referenceIdBytes,int referenceDepth,int referencedCounter,int agreeCounter,int disagreeCounter){
		super(idBytes,sender,MESSAGE_TYPE,createDate,referenceIdBytes,referencedCounter,agreeCounter,disagreeCounter);
		
		assert text!=null;
		plainText=text;

		setReferenceDepth(referenceDepth);
	}
	
	public ChatBuildMessage(BaseUserInfo sender,String text,ChatBuildMessage referenceMessage){
		super(sender, MESSAGE_TYPE,referenceMessage);
		
		assert text!=null;
		plainText=text;
		parseTopics();
		digestId();
	}

	@Override
	public final void digestId() {
		digestId(true);
	}

	@Override
	public final String asPlainText() {
		return plainText;
	}

	@Override
	public final void setReferenceMessageLater(AbstractMessage message) throws BadReferenceException {
		if (!(message instanceof ChatBuildMessage)) {
			throw new BadReferenceException("ChatBuild can only reference ChatBuild");
		}
		
		ChatBuildMessage cast=(ChatBuildMessage) message;
		
		if (!(cast.getReferenceDepth()<=Global.CHAT_BUILD_MAX_DEPTH)) {
			throw new BadReferenceException("chat build too deep");
		}
		
		setReferenceMessage(cast);
		
	}

	@Override
	public void invalid() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((plainText == null) ? 0 : plainText.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChatBuildMessage other = (ChatBuildMessage) obj;
		if (plainText == null) {
			if (other.plainText != null)
				return false;
		} else if (!plainText.equals(other.plainText))
			return false;
		return true;
	}
	

	@Override
	public boolean sameStrippedUser(AbstractMessage obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if(getClass()!=obj.getClass())
			return false;
		ChatBuildMessage other=(ChatBuildMessage) obj;
		if (plainText == null) {
			if (other.plainText != null)
				return false;
		} else if (!plainText.equals(other.plainText))
			return false;
		return true;
	}

	
}
