package org.nearbytalk.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.query.PollQuery;
import org.nearbytalk.query.PollQuery.PollType;
import org.nearbytalk.runtime.BaseListener;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.runtime.NewMessageListener;
import org.nearbytalk.runtime.UniqueObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteException;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;

public class PollServlet extends AbstractServlet {

	private static final Logger log = LoggerFactory
			.getLogger(PollServlet.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private void sendOKMessageList(HttpServletResponse resp,
			List<AbstractMessage> toSend, SessionUserData sessionUserData)
			throws JsonIOException, IOException {

		try {
			sendOkResultStripUserIdBytes(resp, ProtectedMessageFilter.filterWith(toSend, sessionUserData, UniqueObject.getInstance().getDataStore()));
		} catch (SQLiteException e) {
			sendErrorResponse(resp, ErrorResponse.SERVER_BUSY);
		}
	}



	
	static class PollQueryChecker{
		public static ErrorResponse check(PollQuery query){
			
			if(query==null){
				return ErrorResponse.INVALID_MESSAGE_QUERY;
			}
			
			if (query.pollType==null) {
				return ErrorResponse.INVALID_SEARCH_TYPE;
			}
			
			return null;
		}
	}
	
	private PollQuery readAndCheck(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		Gson gson = GsonThreadInstance.FULL_GSON.get();

		PollQuery query =null;
		try{
			query = gson.fromJson(req.getReader(), PollQuery.class);	
		}catch(JsonParseException ex){
			sendErrorResponse(resp, ErrorResponse.INVALID_JSON);
			return null;
		}
		
		ErrorResponse error=PollQueryChecker.check(query);
		
		if (error!=null) {
			
			sendErrorResponse(resp, error);
			return null;
		}
		
		return query;
	}

	/**
	 * if first call poll, history messages will be query and return to client
	 * immediately ,listener will be registered and save to session
	 * 
	 * @param session
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	private void firstTimeQueryAll(HttpSession session, HttpServletRequest req,
			HttpServletResponse resp, SessionUserData sessionUserData) throws IOException {

		PollQuery query=readAndCheck(req, resp);

		if (query == null) {
			return;
		}

		// first time query always return immediately,treat as lazy listener
		// if this is a refresh poll
		BaseListener newOne = BaseListener
				.createByType(query.pollType);

		session.setAttribute(NewMessageListener.POLL_RESULT_LIST_KEY, newOne);

		UniqueObject.getInstance().getMessageDispatcher().registerListener(newOne);

		UniqueObject.getInstance().getMessageCache().pushLatestMessages(newOne);

		log.trace("first time query all");

		sendOKMessageList(resp, newOne.pollAll(),sessionUserData);
	}

	/**
	 * switch listener will trigger a message flush(by old one)
	 * 
	 * @param session
	 * @param oldOne
	 * @param newType
	 * @param resp
	 * @throws IOException 
	 * @throws JsonIOException 
	 */
	private void switchListener(Continuation continuation, HttpSession session,
			BaseListener oldOne, PollType newType, HttpServletResponse resp,SessionUserData sessionUserData) 
					throws JsonIOException, IOException {

		log.debug("user switch polling type from {} to {}",
				oldOne.getPollType(), newType);

		BaseListener newListener = BaseListener.createByType(newType);

		newListener.resetContinuation(continuation);

		session.setAttribute(NewMessageListener.POLL_RESULT_LIST_KEY,
				newListener);

		UniqueObject.getInstance().getMessageDispatcher().registerListener(newListener);

		// this step is not atomic
		// if another thread push messages very fast between
		// these statement,messages will duplicate
		// but this is not important, we can dedup in client side

		// flush old,or user may lost polling messages
		oldOne.resetContinuation(null);

		UniqueObject.getInstance().getMessageDispatcher()
				.unregisterListener(oldOne);

		//when switch listener , continuation is not suspend
		//so we must trigger a positive flush action to flush
		//batch pushed message to client
		
		sendOKMessageList(resp, Collections.<AbstractMessage> emptyList(),sessionUserData);
		
	}

	@Override
	public void processServlet(HttpServletRequest req, HttpServletResponse resp,HttpSession session)
			throws IOException {
		
		SessionUserData sessionUserData=getFromSession(session);

		// only logined user can poll message

		BaseListener listener = (BaseListener) session
				.getAttribute(NewMessageListener.POLL_RESULT_LIST_KEY);

		if (listener == null) {
			// this is first connect
			// should query and return immediatly
			// no need to create continuation and poll
			firstTimeQueryAll(session, req, resp, sessionUserData);
			return;
		}

		Continuation continuation = ContinuationSupport.getContinuation(req);

		// new polling request,not first request
		if (continuation.isInitial()) {

			PollQuery query = readAndCheck(req, resp);

			if (query == null) {
				return;
			}

			log.trace("new continuation ,query {} extracted ", query);
			
			if (query.pollType==PollType.REFRESH) {
				//just force poll
				
				log.trace("REFRESH poll received ,flush previous waited polling (if any)");
			
				//force previous waiting queue flushed to client
				//(if this is a overlap request with previous polling not flush)
				listener.resetContinuation(null);
				
				//at this point ,any new pushed message will stored in listener
				//(no continuation is suspended with listener)
				
				List<AbstractMessage> pollList=listener.pollAll();
				
				log.trace("flush not sent messages to client immediately :{}",pollList);
				
				sendOKMessageList(resp, pollList,sessionUserData);
				
				return;
			} 
			
			if(query.pollType==PollType.HISTORY){
				log.trace("HISTORY poll received ,query online messages");
				
				final List<AbstractMessage> history=new ArrayList<AbstractMessage>();
				
				UniqueObject.getInstance().getMessageCache().pushLatestMessages(new NewMessageListener() {
					@Override
					public void newMessagePushed(Collection<? extends AbstractMessage> newMessages) {
						history.addAll(newMessages);
					}
					@Override
					public void newMessagePushed(AbstractMessage newMessage) {						
					}
				});
				
				sendOKMessageList(resp, history,sessionUserData);
				//do not care overlap request
				return;
				
			}

			// new continuation, should check needs switch type

			if (listener.getPollType() != query.pollType) {
				// must remove prev listener and create new one
				// switch
				switchListener(continuation, session, listener, query.pollType,
						resp,sessionUserData);
				return;
			}
			continuation.setTimeout(Global.POLL_INTERVAL_MILLION_SECONDS);

			if (listener.resetContinuation(continuation)){
				continuation.suspend(resp);

				// makes listener wait on it

				log.trace("poll suspended");
				return;
			}
			
			listener.resetContinuation(null);
			
			//overlap poll request,new one is rejected
			
			log.trace("overlap poll request, new one is rejected");
			
			sendOKMessageList(resp, Collections.<AbstractMessage> emptyList(),sessionUserData);
			return;

		}

		// resume from suspend: result in continuation
		// time expired from suspend: positive poll from listener

		// clean prev binded continuation
		listener.resetContinuation(null);

		if (continuation.isExpired()) {
			// in lazy polling mode ,we should fetch result positively

			if (listener.getPollType() == PollType.LAZY) {
				log.trace("lazy continuation expired, positively poll saved messages");

				// eager mode this may also happend ,but poll result may empty
				// (largely)
				List<AbstractMessage> initList = listener.pollAll();

				sendOKMessageList(resp, initList,sessionUserData);
				return;
			} else {
				log.trace("eager continuation expired, send empty list");
				sendOKMessageList(resp, Collections.<AbstractMessage> emptyList(),sessionUserData);
				return;
			}

		}

		// this is by resumed
		List<AbstractMessage> pollList = (List<AbstractMessage>) req
				.getAttribute(NewMessageListener.POLL_RESULT_LIST_KEY);
		sendOKMessageList(resp, pollList,sessionUserData);
		// clean up listeners waiting
	}

}
