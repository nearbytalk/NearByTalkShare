package org.nearbytalk.http;

import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.nearbytalk.identity.ClientUserInfo;


public class SessionUserData {
	
	public static final String SESSION_USER_DATA_KEY = SessionUserData.class.getSimpleName();

	public ClientUserInfo loginedUser;

	
	
	/**
	 * help TalkServlet pre-check vote related messages, 
	 * also help PollServlet to determine which VoteTopicMessage result can be seen to user
	 * 
	 * <br>
	 * true means this is a VoteTopicMessage, user already vote on it
	 * <br>
	 * false means 1 this is a VoteTopicMessage and user not vote on it
	 *          or 2 this is a VoteOfMeMessage
	 *          
	 * <br>
	 * we related that VoteTopic/VoteOfMe idbytes will never clashed .
	 * 
	 * 
	 */
	public ConcurrentHashMap<String,Boolean> voteInfo=new ConcurrentHashMap<String,Boolean>();

	/**
	 * help same session can only do once judgement on same message 
	 */
	public ConcurrentHashSet<String> judgedMessageIdBytes=new ConcurrentHashSet<String>();

}
