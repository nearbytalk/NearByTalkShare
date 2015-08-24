package org.nearbytalk.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.exception.DataStoreException;
import org.nearbytalk.exception.DuplicateMessageException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.BaseUserInfo;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.query.SearchType;
import org.nearbytalk.query.PagedQuery.PagedInfo;
import org.nearbytalk.runtime.UniqueObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MessageService extends AbstractService {
	
	private static Logger log=LoggerFactory.getLogger(MessageService.class);

	public void talk(AbstractMessage compositeMessage) throws DataStoreException, BadReferenceException, DuplicateMessageException {
		
		//TODO precheck vote message restriction

		try {
			AbstractMessage actuallySaved = UniqueObject.getInstance()
					.getMessageCache().save(compositeMessage);
			if (actuallySaved != null) {
				//valid
				UniqueObject.getInstance().getMessageDispatcher()
						.pushMessage(actuallySaved);
			}
			
			log.error("invalid message {}",compositeMessage);
		}  catch (InterruptedException e) {
			//TODO do nothing?
			log.error("error {}",e);
		}

		// ignore if can not add to cache

	}

	public List<AbstractMessage> queryMessage(ClientUserInfo userInfo,
			int pageSize, int pageNumber) {

		// TODO
		return new ArrayList<AbstractMessage>();

	}

	public AbstractMessage queryDetail(String idBytes) {
		// TODO throw out exception ?
		try {
			AbstractMessage ret=UniqueObject.getInstance().getMessageCache().get(idBytes);
			
			UniqueObject.getInstance().getMessageCache().releaseMessage(ret);
			
			return ret;
			
		} catch (DataStoreException e) {
			return null;
		} catch (BadReferenceException e) {
			return null;
		} catch (InterruptedException e) {
			return null;
		}
	}

	public <T extends AbstractMessage> List<T> queryMessage(Class<T> clazz,
			String keywords, PagedInfo pagedInfo) throws Exception {

		return getDataStore().queryMessage(clazz, keywords, pagedInfo);
	}

	public List<AbstractMessage> queryMessage(Date date, SearchType searchType,
			PagedInfo pagedInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	public void judgeMessage(String idBytes, boolean positive) {
		UniqueObject.getInstance().getMessageCache()
				.judgeMessage(idBytes, positive);
	}

	public List<AbstractMessage> queryHotest(PagedInfo pagedInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * delete user's message, invalid from datastore,then message cache.
	 * 
	 * @param user
	 * @param idBytes
	 * @return
	 */
	public boolean deleteMessage(BaseUserInfo user, String idBytes) {

		return UniqueObject.getInstance().getMessageCache()
				.deleteMessage(user, idBytes);
	}

	public List<AbstractMessage> queryTopic(String keywords, boolean fuzzy,
			PagedInfo pagedInfo) throws Exception {
		return getDataStore().queryTopic(keywords, fuzzy,
				pagedInfo);
	}

}
