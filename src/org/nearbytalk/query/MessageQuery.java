package org.nearbytalk.query;

import java.util.Date;

public class MessageQuery extends PagedQuery {

	public SearchType searchType;

	public Date date;

	public String keywords;

	@Override
	public String toString() {
		return searchType.toString()+",with keywords="+keywords;
	}
	
	

}
