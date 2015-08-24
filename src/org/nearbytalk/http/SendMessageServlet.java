package org.nearbytalk.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nearbytalk.service.MessageService;
import org.nearbytalk.service.ServiceInstanceMap;


public class SendMessageServlet extends AbstractServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private MessageService service = ServiceInstanceMap.getInstance().getService(
			MessageService.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		super.doPost(req, resp);
	}

}
