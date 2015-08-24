package org.nearbytalk.http;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.http.HttpSession;

import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.service.MessageService;
import org.nearbytalk.service.ServiceInstanceMap;
import org.nearbytalk.util.DigestUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class JudgeMessageServlet extends AbstractServlet {

	private static final Logger log = LoggerFactory
			.getLogger(JudgeMessageServlet.class);

	private MessageService service = ServiceInstanceMap.getInstance()
			.getService(MessageService.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static class JudgeRequest {
		public String idBytes;
		public Boolean positive;
	}

	public static ErrorResponse check(JudgeRequest judgeRequest) {
		if (judgeRequest == null) {
			return ErrorResponse.INVALID_JSON;
		}

		if (!DigestUtility.isValidSHA1(judgeRequest.idBytes)) {

			return ErrorResponse.INVALID_ID_BYTES;
		}

		if (judgeRequest.positive == null) {
			return ErrorResponse.FIELD_INCOMPLETE;
		}

		return null;
	}

	@Override
	public ErrorResponse processReaderWriter(Reader reader, Writer writer,
			HttpSession session) throws IOException {

		Gson gson = GsonThreadInstance.STRIP_USER_ID_BYTES_GSON.get();

		SessionUserData sessionUserData = getFromSession(session);

		JudgeRequest judgeRequest = gson.fromJson(reader, JudgeRequest.class);

		ErrorResponse error = check(judgeRequest);

		if (error != null) {
			return error;
		}
		
		boolean notJudgedYet=sessionUserData.judgedMessageIdBytes.add(judgeRequest.idBytes);

		if (!notJudgedYet) {

			// this is not very important action,
			// so do not lock on SessionUserData

			// TODO makes client clear
			return ErrorResponse.DUPLICATE_MESSAGE;
		}

		service.judgeMessage(judgeRequest.idBytes, judgeRequest.positive);

		sendOkResultStripUserIdBytes(writer, judgeRequest.idBytes);

		return null;
	}

}
