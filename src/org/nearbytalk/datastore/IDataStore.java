package org.nearbytalk.datastore;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.nearbytalk.exception.NearByTalkException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.BaseUserInfo;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.query.PagedQuery.PagedInfo;

import com.almworks.sqlite4java.SQLiteBusyException;
import com.almworks.sqlite4java.SQLiteException;

public interface IDataStore {
	
	/**
	 * @author unknown
	 * 
	 * which method to use
	 *
	 */
	public enum QueryMethod {
		
		/**
		 * keywords is treated as token
		 */
		TOKEN,
		/**
		 * keywords is treated as part of text
		 */
		PART,
		/**
		 * just the same 
		 */
		EXACTLY
	}

	public void preCheck(String rawPassword)throws NearByTalkException;

	/**
	 * if idBytes same ,update existing info
	 * or save new info 
	 * if name clash ,simply return false
	 * @param userInfo
	 * @return
	 */
	public boolean saveOrUpdateUser(ClientUserInfo userInfo)throws Exception;

	public boolean saveMessage(AbstractMessage message) throws Exception;

	/**
	 * query user by keywords. if exactly is set to true, only exactly userName match is used.
	 * otherwise user description is also searched.
	 * @param keywords 
	 * @param pagedInfo
	 * @param extractly  if this is search by userName exactly
	 * @return
	 * @throws Exception
	 */
	public List<ClientUserInfo> queryUser(String keywords, PagedInfo pagedInfo, QueryMethod queryMethod)throws Exception;

	/**
	 * query newest TextMessage, should order by create date desc
	 * @param pagedInfo
	 * @return
	 */
	public <T extends AbstractMessage> List<T> queryNewest(Class<T> clazz,PagedInfo pagedInfo) throws Exception;

	public List<AbstractMessage> queryHotest(PagedInfo pagedInfo);
	
	/**
	 * query message by keywords 
	 *  can also pass Class as type limitation ,all dependency loaded
	 *  (means any reference message is recurisve loaded)
	 *  but topics are not parsed(leave this to client cpu)
	 * 
	 * @param clazz which type message is queried. allowed values: any leaf class of AbstractMessage, or AbstractMessage.class
	 * @param keywords search keywords
	 * @param pagedInfo
	 * @return list of typed message. if AbstractMessage.class is passed, any type message may be returned
	 * @throws Exception
	 */
	public <T extends AbstractMessage> List<T> queryMessage(Class<T> clazz,String keywords,PagedInfo pagedInfo) throws Exception;

	/**
	 * delete user and all his messages, but leaves any message 
	 * which reference his to reference point to a "INVALID MESSAGE"
	 * if null passed, delete any user with no talk
	 * (not depends on talk_number, but truly count(*) from abstract_message)
	 * @param userInfo user to delete, or any user with no talk if null passed
	 * @return
	 */
	public boolean delete(BaseUserInfo userInfo);
	
	/**
	 * delete specified message
	 * datastore has no information about user info,so callee must gives user info 
	 * to avoid none-author delete by mistake
	 * @param user which user this message belongs to
	 * @param idBytes to delete, should not null
	 * @param cascade if 
	 * @return
	 */
	public boolean delete(BaseUserInfo user,String idBytes,boolean cascade);

	public void selfCheck();

	public void archive();
	
	public static enum RecycleCatelog{
		EMPTY_RANDOM_USER,
		INVALID_MESSAGES;
		
	}
	
	public void recycle(RecycleCatelog catelog);
	
	/**
	 * batch save message list . do not assume all messages are saved.
	 * save as many as I can ,do not rollback transiaction.
	 * should not throw if some message get failed
	 * @param messageList
	 * @return 
	 * @throws Exception
	 */
	public boolean saveMessage(final Collection<? extends AbstractMessage> messageList) throws Exception;
	
	public void threadRecycle();

	/**
	 * load message of idbytes , all dependency is loaded. null if no such message
	 * @param idBytes
	 * @return
	 */
	public AbstractMessage loadWithDependency(String idBytes) throws SQLiteBusyException,ParseException;
	
	public void judgeMessage(String idBytes,boolean positive) throws SQLiteBusyException;

	/**
	 * query message by there topic (extactly string between ##)
	 * @param keywords
	 * @param pagedInfo
	 * @return
	 * @throws Exception 
	 */
	public List<AbstractMessage> queryTopic(String keywords, boolean fuzzy,PagedInfo pagedInfo) throws Exception;
	
	/**
	 * VoteTopicMessage vote result must be update by outside, there is no direct record structure 1:1 map
	 * @param toUpdate
	 * @throws SQLiteBusyException 
	 */
	public void updateVoteTopicMessages(Collection<VoteTopicMessage> toUpdate) throws SQLiteBusyException;

	
	/**
	 * query relationship between a UserInfo and collection of voteTopicMessage ids 
	 * @param user
	 * @param voteTopicIdBytes
	 * @param assumeExist assume idbytes must exist in datastore
	 * @return list of state boolean, true means user already voted ,false means not yet, null means no such idbytes
	 * @throws SQLiteException 
	 */
	public List<Boolean> queryVoted(BaseUserInfo user,Set<String> voteTopicIdBytes,boolean assumeExist) throws SQLiteException;
	
	/**
	 * for test if duplicate message in store
	 * @param idBytes
	 * @return
	 * @throws SQLiteException 
	 */
	public boolean existMessage(String idBytes) throws SQLiteException;
	
	/**
	 * @return 16 byte for file upload encrypt/decrypt, if already exists, use existed key, or use a random key
	 */
	public byte[] getFileKey();
	

}
