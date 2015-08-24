package org.nearbytalk.identity;

import org.nearbytalk.runtime.Global.VoteAnonymous;

public interface IVoteVisibleControl {
	public AbstractMessage createProxy(VoteAnonymous voteOfMeAnonymous, boolean voteTopicInvisible) ;

	public String getVoteTopicIdBytes();
}
