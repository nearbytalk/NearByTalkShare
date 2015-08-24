package org.nearbytalk.runtime;

import java.util.concurrent.ArrayBlockingQueue;

import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.query.PollQuery.PollType;


public class LazyListener extends BaseListener {

	public LazyListener() {
		super(PollType.LAZY);
	}

	@Override
	protected boolean shouldFlush(ArrayBlockingQueue<? extends AbstractMessage> queue) {

		return queue.remainingCapacity() < queue.size();
	}
}
