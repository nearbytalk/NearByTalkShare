package org.nearbytalk.http;

import java.io.IOException;
import java.util.HashSet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SessionFilter implements Filter {

	private HashSet<String> unprotectedUrls = new HashSet<String>();
	
	private HashSet<String> needSessionDataPrecreateUrls=new HashSet<String>();
	
	private static final Logger log=LoggerFactory.getLogger(SessionFilter.class);
	
	private static final String LOGIN_SERVLET_URL=Utility.makeupAccessPath(LoginServlet.class);

	Class<AbstractServlet>[] unprotectedServletClass = new Class[] {
			//not login access can read this to get server information
			GlobalInfoServlet.class,
			//not login user can query user TODO ?
			QueryUserServlet.class ,  
			};
	
	Class<AbstractServlet>[] needSessionDataPrecreateServletClass= new Class[]{
			LoginServlet.class	,
			QueryMessageServlet.class,
			PollServlet.class,
			//not login user cann't judge message
			//to prevent user repeatly judge one message.
			JudgeMessageServlet.class
	};

	public SessionFilter() {

		for (Class clazz : unprotectedServletClass) {
			unprotectedUrls.add(Utility.makeupAccessPath(clazz));
		}
		
		//according to request.getServletPath, CustomResourceServlet (which used /* pathspec)
		//will generated an empty getServletPath
		unprotectedUrls.add("");
		
		for (Class clazz: needSessionDataPrecreateServletClass) {
			needSessionDataPrecreateUrls.add(Utility.makeupAccessPath(clazz));
		}

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}
	
	private static final String NOT_LOGIN_RESULT = "{\"success\":false,\"detail\":\"NOT_LOGIN\"}";
	
	private void doFilterWithSessionUser(HttpSession session,ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException{
		boolean sessionCheckOk=true;
		if (null == session) {
			sessionCheckOk=false;
		}else {
			ClientUserInfo sessionUser =null;
			
			SessionUserData sessionUserData=(SessionUserData) session.getAttribute(SessionUserData.SESSION_USER_DATA_KEY);
			
			synchronized (sessionUserData) {
				sessionUser = sessionUserData.loginedUser;
			}


			if(sessionUser==null){

				sessionCheckOk=false;

				log.trace("user has session but not logined,direct");
			}
		}

		if (!sessionCheckOk) {

			log.trace("request from {}:{} is not logined, gives NOT_LOGIN result",
					req.getRemoteAddr(), req.getRemotePort());

			res.getWriter().append(NOT_LOGIN_RESULT).flush();

			return;

		}else{
			log.trace("request from {}:{} already in session,passed",
					req.getRemoteAddr(), req.getRemotePort());
		}

		chain.doFilter(req, res);
	}
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		String url = request.getServletPath();

		if (unprotectedUrls.contains(url)) {
			chain.doFilter(req, res);
			return;
		}
		
		//all protected urls, needs login info

		boolean needSessionDataPrecreate=needSessionDataPrecreateUrls.contains(url);

		//TODO different thread may get duplicate session for same client
		//right now didn't care this situation
		HttpSession session =request.getSession(false);
		if (session==null) {
			session= request.getSession(needSessionDataPrecreate);
		}
		
		if (LOGIN_SERVLET_URL.equals(url)) {
			//login is special
			chain.doFilter(req, res);
			return;
		}
		
		doFilterWithSessionUser(session, request, response, chain);
	}
	

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

}
