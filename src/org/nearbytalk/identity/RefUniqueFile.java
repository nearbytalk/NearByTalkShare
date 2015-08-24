package org.nearbytalk.identity;

import java.io.File;
import java.util.Calendar;
import java.util.Collections;
import java.util.Set;

/**
 * 
 * identifiable file, digest as byte[] 
 * 
 * different FileShareMessage can share same RefCountFile
 * 
 */
public class RefUniqueFile extends AbstractFileMessage{
	
	public final static MessageType MESSAGE_TYPE=MessageType.REF_UNIQUE;

	/**
	 * 
	 * if fileName is relative, this is a file in dataStore .
	 * if fileName is absolute,	reference external files
	 * 
	 * @param idBytes
	 */
	public RefUniqueFile(String idBytes, String fileName) {
		super(SpecialIdentifiable.BINARY_OWNER,MESSAGE_TYPE ,
				fileName, Calendar.getInstance().getTime());
		setIdBytes(idBytes);
		//TODO check fileName 
	}

	@Override
	public void digestId() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getReferenceIdBytes() {
		return null;
	}

	@Override
	public AbstractMessage getReferenceMessage() {
		return null;
	}

	public void deleteFile() {
		// TODO Auto-generated method stub
		File deletePath=new File(getFileName());
		if (!deletePath.isAbsolute()) {
			//datastore temp upload
			
			deletePath.delete();
		}
		
	}

	@Override
	public void invalid() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void parseTopics() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<String> getTopics() {
		return Collections.EMPTY_SET;
	}

}
