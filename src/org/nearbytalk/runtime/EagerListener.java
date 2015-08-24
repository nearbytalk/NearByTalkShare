package org.nearbytalk.runtime;

import java.util.concurrent.ArrayBlockingQueue;

import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.query.PollQuery.PollType;


public class EagerListener extends BaseListener{

	public EagerListener(){
		super(PollType.EAGER);
	}

	@Override
	protected boolean shouldFlush(ArrayBlockingQueue<? extends AbstractMessage> queue) {
		return true;
	}


}
