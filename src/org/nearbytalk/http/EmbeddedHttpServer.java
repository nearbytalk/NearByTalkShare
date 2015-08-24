package org.nearbytalk.http;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.UniqueObject;
import org.nearbytalk.runtime.Global.HttpServerInfo;
import org.nearbytalk.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteBusyException;

public class EmbeddedHttpServer implements Listener {

	public static final String CONTEXT_PREFIX = "/";

	private void registerServlet(ServletContextHandler contextHandler) {

		Class<AbstractServlet>[] toRegisterClass = new Class[] {
				LoginServlet.class, LogoutServlet.class,
				QueryMessageServlet.class, QueryUserServlet.class,
				TalkServlet.class, GlobalInfoServlet.class ,PollServlet.class,
				DeleteMessageServlet.class,JudgeMessageServlet.class};

		for (Class<? extends AbstractServlet> clazz : toRegisterClass) {

			contextHandler.addServlet(clazz, Utility.makeupAccessPath(clazz));
		}
		
		contextHandler.addServlet(CustomResourceServlet.class, EmbeddedHttpServer.CONTEXT_PREFIX+"*");
	}


	private Server jettyServer = new Server(HttpServerInfo.listenPort);

	private	ServletContextHandler servletContext = new ServletContextHandler(
				ServletContextHandler.SESSIONS);
	
	public EmbeddedHttpServer() {


		servletContext.setContextPath(CONTEXT_PREFIX);
		
		servletContext.getSessionHandler().addEventListener(new SessionUserCleanListener());
		
		//error page config
		
		ErrorPageErrorHandler errorHandler=new ErrorPageErrorHandler();

		errorHandler.addErrorPage(SQLiteBusyException.class,
				Utility.makeupAccessPath(ErrorServlet.class));
		
		servletContext.setErrorHandler(errorHandler);

		registerServlet(servletContext);

		 
		servletContext.addFilter(EncodingFilter.class, "*", FilterMapping.ALL);
		
		servletContext.addFilter(SessionFilter.class, "*",FilterMapping.ALL);
		
		//clean data store thread session
		servletContext.addFilter(DataStoreFilter.class, "*", FilterMapping.ALL);
		
		HandlerList handlers = new HandlerList();
		
		
		handlers.setHandlers(new Handler[] { servletContext});

		jettyServer.setHandler(handlers);

		jettyServer.addLifeCycleListener(this);

	}

 
	public Server getJettyServer() {
		return jettyServer;
	}

	public boolean isStarted() {
		return jettyServer.isStarted();
	}

	public void stop() throws Exception {
		jettyServer.stop();
		
		UniqueObject.getInstance().getMessageCache().stop();

		UniqueObject.getInstance().getDataStore().threadRecycle();

		
		log.info("server stop ");
	}

	public static void main(String[] args) throws Exception {

		EmbeddedHttpServer embeddedHttpServer = new EmbeddedHttpServer();

		embeddedHttpServer.start();
		
		

	}
	
	private static Logger log = LoggerFactory.getLogger(EmbeddedHttpServer.class);

	public void start() throws Exception {
		jettyServer.start();
		log.info("server started at port:"+HttpServerInfo.listenPort);
	}
	
	/**
	 * for test purpose , to check clean user listener
	 * @param seconds
	 */
	public void setSessionTimeoutSeconds(int seconds){

		servletContext.getSessionHandler().
		getSessionManager().setMaxInactiveInterval(seconds);

	}


	@Override
	public void lifeCycleStarting(LifeCycle event) {
		Global.getInstance().httpServerInfo.isRunning=false;
	}


	@Override
	public void lifeCycleStarted(LifeCycle event) {
		Global.getInstance().httpServerInfo.isRunning=true;
	}


	@Override
	public void lifeCycleFailure(LifeCycle event, Throwable cause) {
		Global.getInstance().httpServerInfo.isRunning=false;
	}


	@Override
	public void lifeCycleStopping(LifeCycle event) {
		Global.getInstance().httpServerInfo.isRunning=false;
	}


	@Override
	public void lifeCycleStopped(LifeCycle event) {
		Global.getInstance().httpServerInfo.isRunning=false;
	}

}
