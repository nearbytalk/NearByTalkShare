package org.nearbytalk.identity;

import java.util.Date;
import java.util.regex.Pattern;

import org.nearbytalk.util.Utility;


public abstract class AbstractFileMessage extends AbstractMessage {

	private String fileName;

	public AbstractFileMessage(ClientUserInfo sender,MessageType messageType, String fileName,Date createDate) {
		super(sender,messageType,createDate);
		Utility.assumeNotNullOrEmpty(fileName);
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	static private final Pattern IMAGE_PATTERN;

	static {
		IMAGE_PATTERN = Pattern.compile("\\.(jpg|jpeg|bmp|png|gif)$",
				Pattern.CASE_INSENSITIVE);
	}

	static public boolean isImage(String filename) {

		return IMAGE_PATTERN.matcher(filename).find();

	}
	
	
	boolean isImage() {
		return isImage(fileName);
	}

	@Override
	public String asPlainText(){
		return fileName;
	}

	@Override
	public final void setReferenceMessageLater(AbstractMessage message){
		throw new UnsupportedOperationException("file message can not reference others");
	}

	@Override
	public void replaceReferenceMessage(AbstractMessage toReplace) {
		
		//do nothing
	}
	
	
}
