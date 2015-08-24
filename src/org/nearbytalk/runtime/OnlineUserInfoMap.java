package org.nearbytalk.runtime;

import java.util.HashMap;
import java.util.HashSet;

import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.util.DigestUtility;
import org.nearbytalk.util.Utility;


public class OnlineUserInfoMap {

	private HashMap<String, ClientUserInfo> idMapInfo = new HashMap<String, ClientUserInfo>();

	private HashSet<String> userNames = new HashSet<String>();
	
	public static class LoginResult{
		public final ClientUserInfo loginedUserInfo;
		public final boolean failIsIdClash;
		
		public boolean successed(){
			return loginedUserInfo!=null;
		}
		
		public LoginResult(ClientUserInfo userInfo) {
			this.loginedUserInfo=userInfo;
			this.failIsIdClash=false;
		}
		public LoginResult(boolean isIdClash){
			this.loginedUserInfo=null;
			this.failIsIdClash=isIdClash;
		}
	}
	
	private Object lock=new Object();


	/**
	 * 尝试登录新的用户，并返回新登录的用户信息
	 * 
	 * 对于userName+idBytes相等的用户，视为同一个 <br>
	 * userNameClash但idBytes不相等的用户，尝试生成随机的新用户名 idBytesClash->登录失败
	 * 
	 * @param origin
	 *            尝试登录的用户
	 * @return 实际登录的用户
	 */
	public LoginResult putUserInfo(ClientUserInfo origin) {

		if (origin == null) {

			throw new IllegalArgumentException(
					"user info to put must not be null");

		}

		Utility.assumeNotNull(origin.getUserName());

		synchronized (lock) {

			ClientUserInfo tryLoadClash = idMapInfo.get(origin.getIdBytes());

			if (tryLoadClash != null) {
				// idclash
				if (origin.getUserName().equals(tryLoadClash.getUserName())) {
					// treat as same user
					return new LoginResult(origin);

				}

				//id clash is not allowed (except renamePut route)
				return new LoginResult(true);

			}

			boolean nameClash = userNames.contains(origin.getUserName());

			if (!nameClash) {
				
				ClientUserInfo cloned=origin.clone();

				idMapInfo.put(origin.getIdBytes(),cloned );
				userNames.add(origin.getUserName());

				return new LoginResult(cloned);

			}

			// name clash but no id clash ,change to serial names

			ClientUserInfo retry = origin.clone();

			for (int i = 0; i < 100; ++i) {

				retry.setUserName(origin.getUserName() + Utility.randomSuffix());

				if (!userNames.contains(retry.getUserName())) {

					idMapInfo.put(retry.getIdBytes(), retry);
					userNames.add(retry.getUserName());

					return new LoginResult(retry);

				}

			}

			//this is by name full (name clash)
			return new LoginResult(false);

		}
	}

	/**
	 * logout is success only if idBytes an userName matches
	 * others field difference not care
	 * 
	 * @param userInfo
	 * @return
	 */
	public boolean remove(ClientUserInfo userInfo) {

		if (userInfo == null) {
			throw new IllegalArgumentException(
					"userInfo to remove must not be null");
		}

		synchronized (lock) {

			ClientUserInfo toRemove = idMapInfo.get(userInfo.getIdBytes());

			if (toRemove == null) {
				return false;
			}
			
			if(!ClientUserInfo.weakSame(userInfo, toRemove)){
				return false;
			}
			

			idMapInfo.remove(userInfo.getIdBytes());
			userNames.remove(userInfo.getUserName());
			
			return true;

		}
	}

	public boolean containsExactly(ClientUserInfo userInfo) {

		ClientUserInfo tryLoad = idMapInfo.get(userInfo.getIdBytes());

		if (tryLoad == null) {
			return false;
		}

		return tryLoad.equals(userInfo);

	}

	public boolean stateConsist() {

		synchronized (lock) {
			
			for(String idBytes:idMapInfo.keySet()){
				if(!DigestUtility.isValidSHA1(idBytes)){
					return false;
				}
			}

			for (ClientUserInfo info : idMapInfo.values()) {
				if (!userNames.contains(info.getUserName())) {
					return false;
				}

			}

			return userNames.size() == idMapInfo.size();

		}

	}

	/**
	 * if can not found suitable new user name (name clash)
	 * prev logined info is still valid
	 * @param newInfo
	 * @return
	 */
	public ClientUserInfo updateUserInfo(ClientUserInfo newInfo) {
		
		if(!idMapInfo.containsKey(newInfo.getIdBytes())){
			throw new IllegalStateException("no such idbytes");
		}
		
		synchronized (lock) {
			
			ClientUserInfo retry=newInfo.clone();
			
			ClientUserInfo prevInfo=idMapInfo.get(newInfo.getIdBytes());

			if (!prevInfo.getUserName().equals(newInfo.getUserName()) 
					&& userNames.contains(retry.getUserName())) {
				//generate name based on pattern
				boolean noSuchPos=true;
				for(int i=0;i<100;++i){
					retry.setUserName(newInfo.getUserName()+Utility.randomSuffix());
					if (!userNames.contains(retry.getUserName())) {
						noSuchPos=false;
						break;
					}
				}
				
				if(noSuchPos){return null;}
			
			}
			
			//replace
			

			userNames.remove(prevInfo.getUserName());
			userNames.add(retry.getUserName());

			idMapInfo.put(newInfo.getIdBytes(), retry);
			return retry;
		}
	}
}
