package org.nearbytalk.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.GsonThreadInstance;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;

/**
 * 
 * return global infomation to user
 */
public class GlobalInfoServlet extends AbstractServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		sendGlobalInfo(req, resp);
	}

	public static class GlobalInfo {

		public ClientUserInfo sessionUser;
		
		//TODO
		//a temp hack,must has none-null field to makes gson not serialize a empty 
		//gson string
		public int userDescriptionMaxLength=Global.USER_DESCRIPTION_MAX_LENGTH;
		
		public long fileUploadLimitByte=Global.getInstance().fileUploadLimitByte;

		@Override
		public String toString() {
			return "userDescriptionMaxLength:"+userDescriptionMaxLength+",fileUploadLimitByte:"+fileUploadLimitByte
					+String.valueOf(sessionUser);
		}
		
		

	}

	private void sendGlobalInfo(HttpServletRequest req, HttpServletResponse resp)
			throws JsonIOException, IOException {

		GlobalInfo info = new GlobalInfo();

		HttpSession session = req.getSession(false);

		if (session != null) {
			
			SessionUserData sessionUserData=(SessionUserData) session.getAttribute(SessionUserData.SESSION_USER_DATA_KEY);
			
			if (sessionUserData!=null) {
				info.sessionUser = sessionUserData.loginedUser;
			}
		}
		
		
		Gson gson=GsonThreadInstance.FULL_GSON.get();
		
		JsonObject resultObject=new JsonObject();
		
		resultObject.addProperty(GsonThreadInstance.RESULT_SUCCESS_JSON_KEY, true);

		resultObject.add(GsonThreadInstance.RESULT_DETAIL_JSON_KEY, gson.toJsonTree(info));
		
		resp.setContentType("application/json");

		String jsonString=resultObject.toString();
		
		byte[] bytes=jsonString.getBytes(Global.UTF8_ENCODING);
		
		resp.setContentLength(bytes.length);
		resp.getOutputStream().write(bytes);
	}
}
