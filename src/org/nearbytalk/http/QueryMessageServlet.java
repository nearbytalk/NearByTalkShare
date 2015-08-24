package org.nearbytalk.http;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.query.MessageQuery;
import org.nearbytalk.query.SearchType;
import org.nearbytalk.query.PagedQuery.PagedInfo;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.runtime.UniqueObject;
import org.nearbytalk.service.MessageService;
import org.nearbytalk.service.ServiceInstanceMap;
import org.nearbytalk.util.DigestUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QueryMessageServlet extends AbstractServlet {

	private MessageService service = ServiceInstanceMap.getInstance()
			.getService(MessageService.class);
	
	public static class MessageQueryChecker {

		public static ErrorResponse checkError(MessageQuery message) {

			if (message == null) {
				return ErrorResponse.INVALID_MESSAGE_QUERY;
			}

			if (message.searchType == null) {
				return ErrorResponse.INVALID_SEARCH_TYPE;
			}

			if((message.searchType!=SearchType.BY_RATE)	&& message.keywords==null){
				return ErrorResponse.INVALID_MESSAGE_QUERY;
			}
			
			if (message.searchType==SearchType.EXACTLY && 
					!DigestUtility.isValidSHA1(message.keywords)) {
				return ErrorResponse.INVALID_ID_BYTES;
			}
			
			if (message.searchType==SearchType.BY_USER && message.keywords==null) {
				
				return ErrorResponse.INVALID_USER_NAME;
			}



			return null;

		}
	};
	
	static Logger log=LoggerFactory.getLogger(QueryMessageServlet.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	
	
	@Override
	public ErrorResponse processReaderWriter(Reader reader, Writer writer,HttpSession session)
			throws IOException {
		
		MessageQuery query = GsonThreadInstance.FULL_GSON.get().fromJson(
				reader, MessageQuery.class);

		ErrorResponse error = MessageQueryChecker.checkError(query);

		if (error != null) {
			
			log.error("query invalied :{}",error);
			return error;
		}
		
	
		if (query.pagedInfo == null) {
			log.debug("paged info of query not set,use default");
			query.pagedInfo = new PagedInfo();
		}
		
		log.trace("extract query : {}",query);

	
			
		try {
			List<AbstractMessage> resultList=doQuery(query);

			sendOkResultStripUserIdBytes(writer,
					ProtectedMessageFilter.filterWith(resultList,
							getFromSession(session), UniqueObject.getInstance()
									.getDataStore()));

			return null;

		} catch (IOException e) {
			throw new IOException(e);
		} catch (Exception e) {
			//TODO TBD
			log.error("error not processed {}",e);
			return ErrorResponse.ACTION_FAILED;
		}

		
	}

	private List<AbstractMessage> doQuery(MessageQuery query) throws Exception{

		List<AbstractMessage> resultList = null;

		switch (query.searchType) {
		case FUZZY: {
			
			log.trace("fuzzy query :{}",query.keywords);
			resultList = service.queryMessage(AbstractMessage.class,query.keywords,
					query.pagedInfo);
			break;
		}
		case EXACTLY:{
			log.trace("query by idBytes: {}",query.keywords);
			AbstractMessage result=service.queryDetail(query.keywords);
			if (result!=null) {
				resultList = new LinkedList<AbstractMessage>();
				resultList.add(result);
			}
			break;
		}
		case BY_RATE: {
			log.trace("query by rate");
			resultList = service.queryHotest(query.pagedInfo);
			break;
		}
		case TOPIC_EXACTLY:{
			log.trace("query by topic exactly: {}",query.keywords);
			
			resultList=service.queryTopic(query.keywords,false,query.pagedInfo);
			break;
		}
		case TOPIC_FUZZY:{
			log.trace("query by topic fuzzy: {}",query.keywords);
			resultList=service.queryTopic(query.keywords, true, query.pagedInfo);
			break;
		}
		default:{
			log.error("search type unexpected after check .should not here!");
			
		}

		}

		return resultList;
		


	}
}
