package org.nearbytalk.http;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.nearbytalk.runtime.GsonThreadInstance;

import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonWriter;

public abstract class AbstractServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	protected void sendOkResultStripUserIdBytes(HttpServletResponse resp,Object detail) throws IOException{
		
		sendResultFull(true, resp, true,detail);
	}

	protected void sendOkResultStripUserIdBytes(Writer writer,Object detail) throws IOException{

		GsonThreadInstance.writeServletResult(true, true,detail,
				writer);
	}

	protected void sendResultFull(boolean stripUserIdBytes,
			HttpServletResponse resp, boolean success,Object detail) throws IOException {
		resp.setContentType("application/json");

		GsonThreadInstance.writeServletResult(stripUserIdBytes, success,detail,
				resp.getWriter());
	}
	
	protected void sendErrorResponse(Writer writer, ErrorResponse responseMessage)
			throws JsonIOException, IOException {
		JsonWriter writer2=new JsonWriter(writer);

		writer2.beginObject();

		writer2.name("success");

		writer2.value(false);

		writer2.name("detail");

		writer2.value(responseMessage.toString());

		writer2.endObject();
	}

	protected void sendErrorResponse(HttpServletResponse resp, ErrorResponse responseMessage)
			throws JsonIOException, IOException {

		resp.setContentType("application/json");
		
		Writer writer=resp.getWriter();

		sendErrorResponse(writer, responseMessage);
		
	}
	
	
	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}
	
	

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		processServlet(req, resp,req.getSession(false));
	}

	public void processServlet(HttpServletRequest req, HttpServletResponse resp,HttpSession session)
			throws IOException {

		resp.setContentType("application/json");

		ErrorResponse errorResponse = null;

		try {
			errorResponse = processReaderWriter(req.getReader(),
					resp.getWriter(), session);
			if (errorResponse == null) {
				return;
			}
		} catch (JsonParseException e) {
			errorResponse = ErrorResponse.INVALID_JSON;
		}

		sendErrorResponse(resp, errorResponse);
	}

	public ErrorResponse processReaderWriter(Reader reader,
			Writer writer,HttpSession session) throws IOException{
		
		return null;
	}
	
	/**
	 * get SessionUserData from http session. only servlet 
	 * that already has HttpSession and assume SessionUserData is 
	 * created can call this method
	 * @param session
	 * @return
	 */
	public SessionUserData getFromSession(HttpSession session){
		assert session!=null;
		
		SessionUserData ret=(SessionUserData) session.getAttribute(SessionUserData.SESSION_USER_DATA_KEY);
		
		assert ret!=null;
		
		return ret;
	}

}
