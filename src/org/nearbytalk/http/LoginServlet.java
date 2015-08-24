package org.nearbytalk.http;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.http.HttpSession;

import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.runtime.OnlineUserInfoMap.LoginResult;
import org.nearbytalk.service.ClientUserInfoService;
import org.nearbytalk.service.ServiceInstanceMap;
import org.nearbytalk.util.DigestUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LoginServlet extends AbstractServlet {

	private static final Logger log = LoggerFactory
			.getLogger(LoginServlet.class);

	private ClientUserInfoService service = ServiceInstanceMap.getInstance()
			.getService(ClientUserInfoService.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static class ClientUserInfoChecker {

		static public ErrorResponse check(ClientUserInfo toCheck) {

			if (toCheck == null) {
				return ErrorResponse.INVALID_CLIENT_USER_INFO;
			}

			if (toCheck.getUserName() == null
					|| toCheck.getUserName().isEmpty()
					// DB column only has 40 char
					|| toCheck.getUserName().length() > 40) {
				return ErrorResponse.INVALID_NULL_USER_NAME;
			}

			if (!DigestUtility.isValidSHA1(toCheck.getIdBytes())) {
				return ErrorResponse.INVALID_ID_BYTES;
			}

			if (toCheck.getDescription() != null
					&& toCheck.getDescription().length() > Global.USER_DESCRIPTION_MAX_LENGTH) {
				return ErrorResponse.USER_DESCRIPTION_TOO_LONG;
			}

			return null;
		}

	}

	@Override
	public ErrorResponse processReaderWriter(Reader reader, Writer writer,
			HttpSession session) throws IOException {

		// so there is no "get random login method"

		// try to load request user client info

		// TODO continues Login attack defense

		ClientUserInfo newInfo = GsonThreadInstance.FULL_GSON.get().fromJson(
				reader, ClientUserInfo.class);

		ErrorResponse error = ClientUserInfoChecker.check(newInfo);

		if (error != null) {
			return error;
		}

		SessionUserData sessionUserData = (SessionUserData) session
				.getAttribute(SessionUserData.SESSION_USER_DATA_KEY);

		synchronized (sessionUserData) {

			ClientUserInfo oldInfo = sessionUserData.loginedUser;

			LoginResult loginResult=null;
			try {
				loginResult = service.login(oldInfo, newInfo);
			} catch (Exception e) {
				//TODO makes exception clear
				return ErrorResponse.ACTION_FAILED;
			}

			if (!loginResult.successed()) {

				return loginResult.failIsIdClash ? ErrorResponse.ID_CLASH
						: ErrorResponse.USER_NAME_SPACE_FULL;
			}

			// login servlet return user idBytes to login user
			// other servlet (message,or view user detail info) should strip
			// user idBytes out.

			if (oldInfo != null
					&& !oldInfo.getIdBytes()
							.equals(loginResult.loginedUserInfo)) {

				// clear previous different user session data
				sessionUserData.voteInfo.clear();
				sessionUserData.judgedMessageIdBytes.clear();

			}

			sessionUserData.loginedUser = loginResult.loginedUserInfo;
		}

		GsonThreadInstance.writeServletResult(false, true,
				sessionUserData.loginedUser, writer);

		return null;
	}
}
