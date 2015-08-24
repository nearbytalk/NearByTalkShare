package org.nearbytalk.test.datastore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.nearbytalk.datastore.IDataStore;
import org.nearbytalk.identity.AbstractFileMessage;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.BaseUserInfo;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.query.PagedQuery.PagedInfo;
import org.nearbytalk.test.MessageCloner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteBusyException;
import com.almworks.sqlite4java.SQLiteException;

public class TestDataStore implements IDataStore {
	
	private Logger log=LoggerFactory.getLogger(TestDataStore.class);

	public HashMap<String, AbstractMessage> container = new HashMap<String, AbstractMessage>();

	@Override
	public synchronized void preCheck(String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public synchronized boolean saveOrUpdateUser(ClientUserInfo userInfo)
			throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public synchronized AbstractMessage loadWithDependency(String idBytes) {
		return container.get(idBytes);
	}

	@Override
	public synchronized boolean saveMessage(AbstractMessage message)
			throws Exception {
		container.put(message.getIdBytes(), message);

		if (message.anyReferenceIdBytes() != null) {
			container.get(message.anyReferenceIdBytes())
					.increaseReferencedCounter();
		}

		return true;
	}

	@Override
	public synchronized List<ClientUserInfo> queryUser(String keywords,
			PagedInfo pagedInfo, QueryMethod queryMethod) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public synchronized <T extends AbstractMessage> List<T> queryNewest(
			Class<T> clazz, PagedInfo pagedInfo) throws Exception {

		List<T> all = new ArrayList<T>();

		for (AbstractMessage msg : container.values()) {

			if (clazz.isInstance(msg)) {
				all.add((T) msg);
			}
		}

		Collections.sort(all, new Comparator<T>() {

			@Override
			public int compare(T o1, T o2) {
				return o1.getCreateDate().compareTo(o2.getCreateDate());
			}
		});

		int beginIndex = Math.min(pagedInfo.beginOffset(), all.size());
		int endIndex = Math.min(pagedInfo.beginOffset() + pagedInfo.size,
				all.size());

		ArrayList<T> ret = new ArrayList<T>(all.subList(beginIndex, endIndex));
		Collections.reverse(ret);

		return ret;

	}

	@Override
	public synchronized List<AbstractMessage> queryHotest(PagedInfo pagedInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public synchronized <T extends AbstractMessage> List<T> queryMessage(
			Class<T> clazz, String keywords, PagedInfo pagedInfo)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public synchronized void selfCheck() {
		// TODO Auto-generated method stub

	}

	@Override
	public synchronized void archive() {
		// TODO Auto-generated method stub

	}

	@Override
	public synchronized boolean saveMessage(
			Collection<? extends AbstractMessage> messageList) {

		boolean ret = true;
		for (AbstractMessage abstractMessage : messageList) {
			
			AbstractMessage ref=abstractMessage.getReferenceMessage();

			if (ref instanceof AbstractFileMessage) {

				if (container
						.containsKey(abstractMessage.getReferenceIdBytes())) {
					log.trace("AbstractFileMessage of idbytes {}, already saved",abstractMessage.getReferenceIdBytes());
					
					container.get(abstractMessage.getReferenceIdBytes()).increaseReferencedCounter();
					
				} else {

					container.put(abstractMessage.getReferenceIdBytes(),
							MessageCloner.cloneMsg(abstractMessage
									.getReferenceMessage()));
				}

			}else if(ref !=null){
				container.get(ref.getIdBytes()).increaseReferencedCounter();
			}

			if (container.containsKey(abstractMessage.getIdBytes())) {
				ret = false;
			} else {
				container.put(abstractMessage.getIdBytes(),
						MessageCloner.cloneMsg(abstractMessage));
			}
		}

		return true;
	}

	@Override
	public synchronized void threadRecycle() {
		// TODO Auto-generated method stub

	}

	@Override
	public synchronized void recycle(RecycleCatelog catelog) {
		// TODO Auto-generated method stub

	}

	@Override
	public synchronized boolean delete(BaseUserInfo userInfo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public synchronized boolean delete(BaseUserInfo user, String idBytes,
			boolean cascade) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public synchronized void judgeMessage(String idBytes, boolean positive)
			throws SQLiteBusyException {
		// TODO Auto-generated method stub

	}

	@Override
	public synchronized List<AbstractMessage> queryTopic(String keywords,
			boolean fuzzy, PagedInfo pagedInfo) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public synchronized void updateVoteTopicMessages(
			Collection<VoteTopicMessage> toUpdate) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Boolean> queryVoted(BaseUserInfo user,
			Set<String> voteTopicIdBytes, boolean assumeExist) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean existMessage(String idBytes) throws SQLiteException {
		// TODO Auto-generated method stub
		return false;
	}

}
