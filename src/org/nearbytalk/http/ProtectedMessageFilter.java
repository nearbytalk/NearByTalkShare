package org.nearbytalk.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.nearbytalk.datastore.IDataStore;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.IVoteVisibleControl;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.Global.VoteAnonymous;

import com.almworks.sqlite4java.SQLiteException;

public class ProtectedMessageFilter {

	public static class CheckInfo {
		int index;

		IVoteVisibleControl toCheck;

		public CheckInfo(int index, IVoteVisibleControl toCheck) {
			this.index = index;
			this.toCheck = toCheck;
		}

	}

	public static class BiMap {
		private List<CheckInfo> checkInfoList = new ArrayList<CheckInfo>();

		private Map<String, List<Integer>> topicIdBytesMapIndex = new HashMap<String, List<Integer>>();

		public void addEntry(int index, IVoteVisibleControl toCheck) {

			checkInfoList.add(new CheckInfo(index, toCheck));

			String topicIdBytes = toCheck.getVoteTopicIdBytes();

			List<Integer> indeces = topicIdBytesMapIndex.get(topicIdBytes);
			if (indeces == null) {
				indeces = new ArrayList<Integer>();
				topicIdBytesMapIndex.put(topicIdBytes, indeces);
			}

			assert !indeces.contains(indeces);

			indeces.add(checkInfoList.size()-1);
		}
	}

	public static List<AbstractMessage> filterWith(
			List<AbstractMessage> originalList,
			SessionUserData sessionUserData, IDataStore dataStore)
			throws SQLiteException {

		Global.VoteAnonymous anonymousVoteOfOthers = Global.getInstance().anonymousVoteOfOthers;

		boolean anonymousVoteTopic = Global.getInstance().anonymousVoteTopic;

		if ((anonymousVoteOfOthers == VoteAnonymous.ALWAYS_VISIBLE)
				&& !anonymousVoteTopic) {
			// all visible
			return originalList;
		}

		List<AbstractMessage> ret = new ArrayList<AbstractMessage>(
				originalList.size());

		final String thisUserIdBytes = sessionUserData.loginedUser.getIdBytes();

		BiMap biMap = new BiMap();

		// since iterator list is a long-time action,
		// we didn't lock on sessionUserData for sync
		// instead , use temp ref first, then check it before final action
		// if user changed, we should discard this result
		// (to avoid wrong result to new logined user)
		ClientUserInfo userInfoWhenCheck = sessionUserData.loginedUser;

		// TODO ,this action can be split to multi-thread
		for (int i = 0, max = originalList.size(); i < max; ++i) {

			AbstractMessage thisOne = originalList.get(i);

			if (thisOne.getSender().getIdBytes().equals(thisUserIdBytes)) {
				// user can always see his/her message completely (no matter if
				// VoteOfMe or VoteTopic)
				ret.add(thisOne);

			} else if (!(thisOne instanceof IVoteVisibleControl)) {

				// no need to protect
				ret.add(thisOne);
			} else {

				IVoteVisibleControl cast = (IVoteVisibleControl) thisOne;

				String topicIdBytes = cast.getVoteTopicIdBytes();

				Boolean preCheck = sessionUserData.voteInfo.get(topicIdBytes);

				if (preCheck != null) {
					// pre-cache check hit
					if (preCheck) {
						// use already vote on same topic
						ret.add(cast
								.createProxy(
										anonymousVoteOfOthers,
										false));
						continue;
					} else {
						// user have not vote on same topic
						// (we assume VoteOfMe VoteTopic idbytes will never
						// clash, so if key hit, it must be a VoteTopic)
						ret.add(cast
								.createProxy(
										anonymousVoteOfOthers ,
										anonymousVoteTopic));
						continue;
					}
				}

				// pre cache miss, should do second stage check

				// vote related message pushed here are all in runtime cache
				// so new talked vote message will not be here, we can
				// safely assume following:

				// if current user have voted a topic in same session ,it will
				// be in
				// pre-check cache so we got the correct result.

				// if current user voted a same topic before, it will be in
				// datastore
				// so query gives correct result

				// makes a placeholder first
				ret.add(null);
				biMap.addEntry(i, cast);
			}
		}

		if (biMap.checkInfoList.isEmpty()) {
			//all information fetched , no need to query datastore
			return ret;
		}

		Set<String> toCheckTopicIdBytes = biMap.topicIdBytesMapIndex.keySet();

		synchronized (sessionUserData) {
			if (userInfoWhenCheck != sessionUserData.loginedUser) {
				// new user kicked in ,discard all results

				return Collections.emptyList();
			}

			// no change ,so we can treat it as unchanged

			// we are sure all topic already flushed to datastore
			// (if not ,it can not pass pre-check cache test and reach here),
			//
			List<Boolean> queryResult = dataStore.queryVoted(
					sessionUserData.loginedUser, toCheckTopicIdBytes, true);

			assert queryResult.size() == biMap.topicIdBytesMapIndex.size();

			Iterator<Boolean> resultIterator = queryResult.iterator();

			Iterator<Entry<String, List<Integer>>> topicIdBytesIterator = biMap.topicIdBytesMapIndex
					.entrySet().iterator();

			for (int i = 0, max = queryResult.size(); i < max; ++i) {

				boolean voted = resultIterator.next();

				Entry<String, List<Integer>> entry = topicIdBytesIterator.next();

				for (Integer index : entry.getValue()) {
					//
					CheckInfo checkInfo = biMap.checkInfoList.get(index);

					ret.set(checkInfo.index, checkInfo.toCheck.createProxy(anonymousVoteOfOthers, !voted));
				}

				sessionUserData.voteInfo.put(entry.getKey(), voted);
				//TODO different thread may filter trigger same topic idbytes check
				//needs more check

			}

			return ret;

		}

	}
}
