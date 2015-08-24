package org.nearbytalk.runtime;

import org.nearbytalk.datastore.IDataStore;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.runtime.OnlineUserInfoMap.LoginResult;
import org.nearbytalk.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * provide a unique view of online user map and user record in db
 * 
 * online uesr must be distinct from db user record, for that we need to
 * implement this logic:
 * 
 * only userName is public ,idBytes only visible to self, no one can pretend to
 * be others when the one whom to be is online ,even if he has the same userName
 * and idBytes.
 * 
 * any clash between different session is not allowed, one must knows userName
 * and idBytes exactly to do logout
 * 
 * id clash in same http session can change its name (but not different
 * session), cascade to datastore recode
 * 
 * id clash in db only can update db
 * 
 * if there is clash ,must told what happend ,and try to get a useable random
 * one must be thread safe
 * 
 */
public class UserCache {

	private static Logger log = LoggerFactory.getLogger(UserCache.class);

	private final OnlineUserInfoMap onlineCache ;
	private final IDataStore dataStore;

	UserCache(OnlineUserInfoMap onlineCache,IDataStore dataStore) {
		this.onlineCache=onlineCache;
		this.dataStore = dataStore;
	}


	private static final int DATA_STORE_SAVE_TRY_TIMES = 5;

	/**
	 * 
	 * do final datastore update, if reach this stage, user is assume no clash
	 * online , will save to datastore as new record or update exist. this
	 * function will sync onlineCache,release any allocated temp random
	 * userName(with no success) precondition : trySave is in onlineCache
	 * already,only need dataStore sync
	 * 
	 * @return
	 * @throws Exception 
	 */
	private LoginResult dataStoreUpdate(final String bareUserName,final ClientUserInfo trySave) throws Exception {

		int dbRetry = 0;
		
		ClientUserInfo myTry=trySave.clone();

		while (dbRetry++ < DATA_STORE_SAVE_TRY_TIMES) {
			
			
			if(dataStore.saveOrUpdateUser(myTry)){
				return new LoginResult(myTry);
			}
					
			// db save failed,must try another
			// first remove from online user map
			onlineCache.remove(myTry);
			myTry.setUserName(bareUserName+ Utility.randomSuffix());
			
			LoginResult onlineUserTry=onlineCache.putUserInfo(myTry);
			if (onlineUserTry.loginedUserInfo == null) {
				// can not allocate a useable username online
				return onlineUserTry;
			}
			//else another try
		
			myTry=onlineUserTry.loginedUserInfo;
		}
		
		//this is name clash
		return new LoginResult(false);

	}

	/**
	 * @param newLogin
	 * @return
	 * @throws Exception 
	 */
	public LoginResult login(ClientUserInfo newLogin) throws Exception {

		// first try a useable userinfo by online user info
		LoginResult retry = onlineCache.putUserInfo(newLogin);

		if (retry.loginedUserInfo == null) {
			//error happed ,id clash or name clash
			return retry;
		}

		// if same id,this will be a db recode update action
		// at this stage ,use is assumed to be unique in onlineUser(but not
		// database)
		// so its safe to update datastore .
		// different thread try to do same thing will clash in
		// online user map check stage,would not reach here
		// datastore will update same id user record.

		return dataStoreUpdate(newLogin.getUserName(),retry.loginedUserInfo);

	}

	/**
	 * http session end ,or user leave page,or change accout call this function
	 * to release onlineUser resources,and do datastore resource recycle(if user
	 * is random ,and with no talk should be removed from datastore)
	 * 
	 * @param toRemove
	 * @return
	 */
	public boolean logout(ClientUserInfo toRemove) {
		boolean removeOnline = onlineCache.remove(toRemove);
		if (removeOnline) {
			dataStore.delete(null);
		}
		return removeOnline;
	}

	/**
	 * try to rename online info and db recode DataStoreCache and onlineCache do
	 * not have enough information about if current action is renaming (do not
	 * take care http session), must be combined by enclose logic
	 * 
	 * @param toRename
	 * @return
	 * @throws Exception 
	 */
	public LoginResult updateUserInfo(ClientUserInfo toRename) throws Exception {

		ClientUserInfo retry = onlineCache.updateUserInfo(toRename);

		if (retry == null) {

			// can not allocate online userName
			return new LoginResult(false);

		}
		// at this stage ,there is no idClash (prev login assume this condition)
		// what to do is update db username
		return dataStoreUpdate(toRename.getUserName(),retry);
	}

	
	public OnlineUserInfoMap getOnlineUserInfoMap(){
		return onlineCache;
	}
}
