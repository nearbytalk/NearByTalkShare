package org.nearbytalk.service;

import java.util.List;

import org.nearbytalk.datastore.IDataStore.QueryMethod;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.query.SearchType;
import org.nearbytalk.query.UserQuery;
import org.nearbytalk.runtime.UniqueObject;
import org.nearbytalk.runtime.OnlineUserInfoMap.LoginResult;


public class ClientUserInfoService extends AbstractService {

	/**
	 * try to login. if there is user already logined, logout it first
	 * 
	 * @param session
	 * @param toLogin
	 * @return
	 */
	/**
	 * @param session
	 * @param toLogin
	 * @return
	 * @throws Exception 
	 */
	public LoginResult login(ClientUserInfo oldLogin, ClientUserInfo toLogin) throws Exception {

		assert toLogin!=null;

		if (oldLogin == null) {
			// this client first login
			return UniqueObject.getInstance().userCache.login(toLogin);

		}

		// already has one
		if (oldLogin.equals(toLogin)) {
			// no change
			return new LoginResult(toLogin);
		}

		// same user rename action
		if (oldLogin.getIdBytes().equals(toLogin.getIdBytes())) {
			return UniqueObject.getInstance().userCache.updateUserInfo(toLogin);
		}

		// totally different user

		logout(oldLogin);

		return UniqueObject.getInstance().userCache.login(toLogin);

	}

	/**
	 * try to logout user. if toLogout is not exactly the same as online user
	 * map just return false,and do not remove session info.
	 * 
	 * @param session
	 * @param toLogout
	 * @return
	 */
	public boolean logout(ClientUserInfo toLogout) {

		// have session ,should check its exactly same user
		if (toLogout == null) {
			return false;
		}

		return UniqueObject.getInstance().userCache.logout(toLogout);

	}

	/**
	 * @param keywords
	 *            query user which contains keywords
	 * @return
	 * @throws Exception 
	 */
	public List<ClientUserInfo> queryUser(UserQuery query) throws Exception {
		
		/*

		 * Table 12-2. Blocks Containing Han Ideographs

Block                                   | Range       | Comment
----------------------------------------+-------------+-----------------------------------------------------
CJK Unified Ideographs                  | 4E00–9FFF   | Common
CJK Unified Ideographs Extension A      | 3400–4DBF   | Rare
CJK Unified Ideographs Extension B      | 20000–2A6DF | Rare, historic
CJK Unified Ideographs Extension C      | 2A700–2B73F | Rare, historic
CJK Unified Ideographs Extension D      | 2B740–2B81F | Uncommon, some in current use
CJK Compatibility Ideographs            | F900–FAFF   | Duplicates, unifiable variants, corporate characters
CJK Compatibility Ideographs Supplement | 2F800–2FA1F | Unifiable variants
		 * 
		 */
		
		//when query user, mozporter may not tokenize string very well, so
		//we did a little trick here:
		//if CJK---> FTS
		//if none-CJK ---> LIKE
		//mixed ---> LIKE
		
		QueryMethod method=QueryMethod.PART;
		if (query.searchType==SearchType.EXACTLY) {
			
			method=QueryMethod.EXACTLY;
		}else {
			method=QueryMethod.TOKEN;

			for (int i = 0; i < query.keywords.length(); i++) {
				int codePoint=query.keywords.codePointAt(i);
				if (codePoint<0x4e00 || codePoint>0x9fff) {
					method=QueryMethod.PART;
					break;
				}
			}
		}
		
		return getDataStore().queryUser(query.keywords,
				query.pagedInfo, method);

	}

}
