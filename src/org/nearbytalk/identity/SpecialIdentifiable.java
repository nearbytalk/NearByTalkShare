package org.nearbytalk.identity;

import java.util.Calendar;

public class SpecialIdentifiable {

	public final static ClientUserInfo BINARY_OWNER = new ClientUserInfo(
			"BINARY OWNER", "0000000000000000000000000000000000000001");
	
	public final static PlainTextMessage DELETED_MESSAGE=new PlainTextMessage("0000000000000000000000000000000000000002", BINARY_OWNER, "抱歉，已经删除。sorry deleted", Calendar.getInstance().getTime(), null, 1,0,0,0);

}
