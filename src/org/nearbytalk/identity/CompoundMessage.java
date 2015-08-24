package org.nearbytalk.identity;

import java.util.Date;

import org.nearbytalk.util.DigestUtility;
import org.nearbytalk.util.Utility;


public abstract class CompoundMessage <T extends AbstractMessage> extends AbstractTextMessage{

	private String referenceIdBytes;
	
	private T referenceMessage;
	
	public CompoundMessage(BaseUserInfo sender, MessageType messageType, T referenceMessage) {
		super(sender, messageType);
		this.referenceMessage=referenceMessage;
		if (referenceMessage!=null) {
			setReferenceDepth(referenceMessage.getReferenceDepth()+1);
			referenceIdBytes=referenceMessage.getIdBytes();
		}
	}
	
	

	/**
	 * set referenceMessage. can be null
	 * @param referenceMessage
	 */
	protected void setReferenceMessage(T referenceMessage) {
		this.referenceMessage = referenceMessage;

		if (referenceMessage!=null) {
			assert referenceMessage.getIdBytes() !=null;

			if(referenceIdBytes!=null){
				assert referenceIdBytes.equals(referenceMessage.getIdBytes());
			}

			setReferenceDepth(referenceMessage.getReferenceDepth()+1);
			referenceIdBytes=referenceMessage.getIdBytes();
			referenceMessage.increaseReferencedCounter();
		}

	}



	public CompoundMessage(String idBytes, BaseUserInfo sender,
			MessageType messageType, Date createDate, String referenceIdBytes,
			int referencedCounter,int agreeCounter, int disagreeCounter) {
		super(idBytes, sender, messageType, createDate, referencedCounter,
				agreeCounter, disagreeCounter);
		
		if(referenceIdBytes!=null){
			assert DigestUtility.isValidSHA1(referenceIdBytes);
			this.referenceIdBytes=referenceIdBytes;
		}
	}

	@Override
	public final String getReferenceIdBytes() {
		return referenceIdBytes;
	}

	@Override
	public final T getReferenceMessage() {
		return referenceMessage;
	}



	@Override
	public boolean sameStrippedUser(AbstractMessage obj) {
		if (this == obj)
			return true;
		if (!super.sameStrippedUser(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CompoundMessage other = (CompoundMessage) obj;
		if (referenceIdBytes == null) {
			if (other.referenceIdBytes != null)
				return false;
		} else if (!referenceIdBytes.equals(other.referenceIdBytes))
			return false;
		if (referenceMessage == null) {
			if (other.referenceMessage != null)
				return false;
		} else if (!referenceMessage.sameStrippedUser(other.referenceMessage))
			return false;
		return true;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime
				* result
				+ ((referenceIdBytes == null) ? 0 : referenceIdBytes.hashCode());
		result = prime
				* result
				+ ((referenceMessage == null) ? 0 : referenceMessage.hashCode());
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
		CompoundMessage other = (CompoundMessage) obj;
		if (referenceIdBytes == null) {
			if (other.referenceIdBytes != null)
				return false;
		} else if (!referenceIdBytes.equals(other.referenceIdBytes))
			return false;
		if (referenceMessage == null) {
			if (other.referenceMessage != null)
				return false;
		} else if (!referenceMessage.equals(other.referenceMessage))
			return false;
		return true;
	}



	@Override
	public String toString() {
		return super.toString()+"[ref_id=" + Utility.idBytesToString(referenceIdBytes)
				+ "plain_text="+asPlainText()+", ref_msg=" + (referenceMessage==null?null:Utility.idBytesToString(referenceMessage.getIdBytes())) + "]";
	}



	@Override
	public void replaceReferenceMessage(AbstractMessage toReplace) {
		
		if (referenceMessage!=null) {
			assert referenceMessage.sameStrippedUser(toReplace);
		}
		
		referenceMessage=(T) toReplace;
	}

}
