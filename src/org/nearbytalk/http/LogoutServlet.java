package org.nearbytalk.http;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.http.HttpSession;

import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.service.ClientUserInfoService;
import org.nearbytalk.service.ServiceInstanceMap;


//TODO auto release userInfo when session destoried
/**
 * logout servlet do not response anything, just use as page leave resource
 * clean.
 * 
 */
public class LogoutServlet extends AbstractServlet {

	private ClientUserInfoService sessionControlService = ServiceInstanceMap
			.getInstance().getService(ClientUserInfoService.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public ErrorResponse processReaderWriter(Reader reader, Writer writer,
			HttpSession session) throws IOException {
		
		//only logined user can access this 

		SessionUserData userData = getFromSession(session);

		ClientUserInfo userInfo = userData.loginedUser;

		session.removeAttribute(SessionUserData.SESSION_USER_DATA_KEY);

		sessionControlService.logout(userInfo);
		
		//TODO gives more detail ?
		sendOkResultStripUserIdBytes(writer, "");

		return null;
	}

}
