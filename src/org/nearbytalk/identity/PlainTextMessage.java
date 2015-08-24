package org.nearbytalk.identity;

import java.util.Date;

import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.util.Utility;


/**
 * a simple text message, used for 
 * 1 user talk reference_depth=1
 * 2 user upload file's description reference_depth=2 (RefCountFile reference depth=1)
 * 3 user broadcast others talk  reference_depth=referened.reference_depth+1
 *   3.1 if referenced is a TextMessage with reference_depth=1 or reference RefCountFile, just reference this message.
 *   3.2 if referenced is already a broadcast (reference other message), reference should be the inner most message(except RefCountFile)
 * 
 * 
 * some restrictions: can not reference ChatBuildMessage (only ChatBuildMessage can do this)
 * 						  to avoid none-unique appearance (Chat VS Plain)
 * 					  can not reference VoteTopicMessage (only VoteOfMeMessage can do this)
 * 						  must vote on it 
 * 					  can not reference VoteOfMeMessage 
 * 						  to avoid too complex server validate 
 * 
 * these restrictions may be relaxed later
 *
 */
public class PlainTextMessage extends CompoundMessage<AbstractMessage>{
	
	public static final MessageType MESSAGE_TYPE=MessageType.PLAIN_TEXT;

	private String plainText;

	public PlainTextMessage(BaseUserInfo sender,String text){
		
		this(sender, text, null);
	}
	
	/**
	 * for client app use 
	 * 
	 * @param sender
	 * @param text
	 */
	public PlainTextMessage(BaseUserInfo sender,String text,AbstractMessage referenceMessage){
		super(sender, MESSAGE_TYPE,referenceMessage);
		this.plainText = text;
		
		digestId();
		
		parseTopics();
	}

	/**
	 * message construct from db
	 * 
	 * @param sender
	 * @param text
	 * @param messageIdBytes
	 */
	public PlainTextMessage(String idBytes, BaseUserInfo sender, String text,
			Date createDate,String referenceIdBytes,int referenceDepth,
			int referencedCounter,int agreeCounter,int disagreeCounter){
		super(idBytes,sender,MESSAGE_TYPE,createDate,
				referenceIdBytes,referencedCounter,agreeCounter,disagreeCounter);

		Utility.assumeNotNull(text);

		this.plainText = text;

		setReferenceDepth(referenceDepth);

		// not digest
	}

	@Override
	public String asPlainText() {
		return this.plainText;
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
		PlainTextMessage other = (PlainTextMessage) obj;
		if (plainText == null) {
			if (other.plainText != null)
				return false;
		} else if (!plainText.equals(other.plainText))
			return false;
		return true;
	}
	
	
	
	@Override
	public boolean sameStrippedUser(AbstractMessage obj){
		if (this == obj)
			return true;
		if (!super.sameStrippedUser(obj))
			return false;
		if (getClass()!=obj.getClass()) 
			return false;
		PlainTextMessage other=(PlainTextMessage) obj;
		if (plainText == null) {
			if (other.plainText != null)
				return false;
		} else if (!plainText.equals(other.plainText))
			return false;
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.nearbytalk.identity.AbstractMessage#setReferenceMessageLater(org.nearbytalk.identity.AbstractMessage)
	 */
	public void setReferenceMessageLater(AbstractMessage message) throws BadReferenceException {
		
		assert message != null;

		if ((message instanceof PlainTextMessage) && !((PlainTextMessage)message).canBeReferenced()) {
			throw new BadReferenceException("reference message can not act as reference");
		}

		if (message instanceof VoteOfMeMessage || 
				message instanceof VoteTopicMessage || 
				message instanceof ChatBuildMessage) {
			throw new BadReferenceException("reference message can not be VoteOfMe/VoteTopic/ChatBuild");
		}
		
		setReferenceMessage(message);

		if (!(message instanceof RefUniqueFile) && plainText != null) {
			// this is a broadcast

			plainText = plainText.substring(0,
					Math.min(plainText.length(), Global.BROADCAST_MESSAGE_LIMIT));
		}
	}

	@Override
	public void digestId() {
		digestId(true);
	}

	public boolean canBeReferenced() {
		
		return (anyReferenceIdBytes()==null || getReferenceMessage() instanceof RefUniqueFile );
	}

	@Override
	public void invalid() {
		this.plainText=SpecialIdentifiable.DELETED_MESSAGE.asPlainText();
	}
	
}
