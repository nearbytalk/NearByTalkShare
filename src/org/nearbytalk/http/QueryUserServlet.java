package org.nearbytalk.http;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.query.UserQuery;
import org.nearbytalk.query.PagedQuery.PagedInfo;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.service.ClientUserInfoService;
import org.nearbytalk.service.ServiceInstanceMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class QueryUserServlet extends AbstractServlet {

	private ClientUserInfoService service = ServiceInstanceMap.getInstance()
			.getService(ClientUserInfoService.class);

	private static final Logger log = LoggerFactory
			.getLogger(QueryUserServlet.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static class UserQueryChecker{
		public static ErrorResponse check(UserQuery toCheck){
			if (toCheck==null) {
				return ErrorResponse.INVALID_USER_QUERY;
			}
			
			if (toCheck.keywords==null) {
				return ErrorResponse.INVALID_SEARCH_NAME;
			}
			
			if (toCheck.searchType==null) {
				return ErrorResponse.INVALID_SEARCH_TYPE;
			}
			
			return null;
		} 
	}

	@Override
	public ErrorResponse processReaderWriter(Reader reader, Writer writer,
			HttpSession session) throws IOException {

		Gson gson = GsonThreadInstance.FULL_GSON.get();
		UserQuery userQuery = gson.fromJson(reader, UserQuery.class);

		if (userQuery.pagedInfo == null) {
			userQuery.pagedInfo = new PagedInfo();
		}

		ErrorResponse error = UserQueryChecker.check(userQuery);

		if (error != null) {
			return error;
		}

		List<ClientUserInfo> results;
		try {
			results = service.queryUser(userQuery);

			sendOkResultStripUserIdBytes(writer, results);
			return null;
		} catch (IOException e) {
			throw e;
		} catch (Exception e){
			//TODO makes exception clear
			return ErrorResponse.ACTION_FAILED;
		}

	}
}
