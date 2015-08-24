package org.nearbytalk.http;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.http.HttpSession;

import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.service.MessageService;
import org.nearbytalk.service.ServiceInstanceMap;
import org.nearbytalk.util.DigestUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class DeleteMessageServlet extends AbstractServlet {

	private static final Logger log=LoggerFactory.getLogger(DeleteMessageServlet.class);
	
	private MessageService service = ServiceInstanceMap.getInstance()
			.getService(MessageService.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	

	public static class DeleteParam {

		String idBytes;
	}

	public static class DeleteParamChecker {
		public static ErrorResponse check(DeleteParam param) {

			if (param == null) {
				return ErrorResponse.INVALID_JSON;
			}

			if (param.idBytes == null) {
				return ErrorResponse.FIELD_INCOMPLETE;
			}

			if (!DigestUtility.isValidSHA1(param.idBytes)) {
				return ErrorResponse.INVALID_ID_BYTES;
			}
			return null;
		}
	}

	@Override
	public ErrorResponse processReaderWriter(Reader reader,Writer writer,HttpSession session) throws JsonSyntaxException,
			JsonIOException, IOException {

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		DeleteParam deleteParam = gson.fromJson(reader,
				DeleteParam.class);

		ErrorResponse error = DeleteParamChecker.check(deleteParam);

		if (error != null) {
			
			log.trace("delete request invalid: {}",error);
			return error;
		}

		SessionUserData userData=getFromSession(session);

		//un-login should be filter out by SessionFilter
		assert userData!=null;
		ClientUserInfo user= userData.loginedUser; 
		assert user!=null;

		boolean result=service.deleteMessage(user, deleteParam.idBytes);
		
		//TODO detail message?

		GsonThreadInstance.writeServletResult(false, result,"",
				writer);
		
		return null;
	}

}
