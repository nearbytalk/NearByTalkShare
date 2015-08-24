package org.nearbytalk.http;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.runtime.NewMessageListener;
import org.nearbytalk.runtime.UniqueObject;
import org.nearbytalk.service.ClientUserInfoService;
import org.nearbytalk.service.ServiceInstanceMap;


/**
 * clean onlined user info when session is destoried
 * 
 */
public class SessionUserCleanListener implements HttpSessionListener{

		
	ClientUserInfoService service=ServiceInstanceMap.getInstance().getService(ClientUserInfoService.class);
	@Override
	public void sessionCreated(HttpSessionEvent se) {
		se.getSession().setAttribute(SessionUserData.SESSION_USER_DATA_KEY, new SessionUserData());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		
		SessionUserData sessionUserData=(SessionUserData) se.getSession()
				.getAttribute(SessionUserData.SESSION_USER_DATA_KEY);

		ClientUserInfo loginedUserInfo = sessionUserData.loginedUser;

		if(loginedUserInfo!=null){
			service.logout(loginedUserInfo);
		}
		
		
		NewMessageListener listener = (NewMessageListener) se.getSession()
				.getAttribute(NewMessageListener.POLL_RESULT_LIST_KEY);
		
		if(listener!=null){
			UniqueObject.getInstance().getMessageDispatcher().unregisterListener(listener);
		}
		
	}

}
