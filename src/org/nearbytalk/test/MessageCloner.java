package org.nearbytalk.test;

import java.util.HashMap;
import java.util.Map;

import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.ChatBuildMessage;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.identity.PwdCryptoMessage;
import org.nearbytalk.identity.RefUniqueFile;
import org.nearbytalk.identity.VoteOfMeMessage;
import org.nearbytalk.identity.VoteTopicMessage;


public class MessageCloner {

	static public AbstractMessage cloneMsg(AbstractMessage msg) {

		for (Class<? extends AbstractMessage> clazz : IMPL_MAP.keySet()) {
			if (clazz == msg.getClass()) {
				return IMPL_MAP.get(clazz).cloneMsg(msg);
			}

		}

		return null;
	}

	private static interface Impl<T extends AbstractMessage> {

		public T cloneMsg(AbstractMessage msg);

	}

	private static final Impl<PlainTextMessage> PLAIN_TEXT_IMPL = new Impl<PlainTextMessage>() {
		@Override
		public PlainTextMessage cloneMsg(AbstractMessage toClone) {

			PlainTextMessage cast = (PlainTextMessage) toClone;

			return new PlainTextMessage(cast.getIdBytes(), cast.getSender(),
					cast.asPlainText(), toClone.getCreateDate(),
					toClone.getReferenceIdBytes(), toClone.getReferenceDepth(),
					toClone.getReferencedCounter(), toClone.getAgreeCounter(),
					toClone.getDisagreeCounter());

		}

	};

	private static final Impl<ChatBuildMessage> CHAT_BUILD_IMPL = new Impl<ChatBuildMessage>() {

		@Override
		public ChatBuildMessage cloneMsg(AbstractMessage toClone) {

			ChatBuildMessage cast = (ChatBuildMessage) toClone;

			ChatBuildMessage ret=new ChatBuildMessage(cast.getIdBytes(), cast.getSender(),
					cast.asPlainText(), cast.getCreateDate(),
					cast.getReferenceIdBytes(), cast.getReferenceDepth(),
					cast.getReferencedCounter(), cast.getAgreeCounter(),
					cast.getDisagreeCounter());
			//TODO deep clone 
			ret.replaceReferenceMessage(toClone.getReferenceMessage());
			
			return ret;
		}

	};

	private static final Impl<VoteTopicMessage> VOTE_TOPIC_IMPL = new Impl<VoteTopicMessage>() {

		@Override
		public VoteTopicMessage cloneMsg(AbstractMessage msg) {
			VoteTopicMessage cast = (VoteTopicMessage) msg;
			return new VoteTopicMessage(cast.getSender(), cast.getVoteTopic(),
					cast.getDescription(), cast.isMultiSelection(),
					cast.getResults());
		}
	};

	private static final Impl<VoteOfMeMessage> VOTE_OF_ME_IMPL = new Impl<VoteOfMeMessage>() {

		@Override
		public VoteOfMeMessage cloneMsg(AbstractMessage msg) {
			VoteOfMeMessage cast = (VoteOfMeMessage) msg;
			return new VoteOfMeMessage(cast.getIdBytes(), cast.getSender(),
					cast.asPlainText(), cast.getCreateDate(),
					cast.getReferenceIdBytes(), cast.getReferencedCounter(),
					cast.getAgreeCounter(), cast.getDisagreeCounter());
		}
	};

	public static final Impl<RefUniqueFile> REF_UNIQUE_IMPL = new Impl<RefUniqueFile>() {

		@Override
		public RefUniqueFile cloneMsg(AbstractMessage msg) {
			RefUniqueFile cast = (RefUniqueFile) msg;
			return new RefUniqueFile(cast.getIdBytes(), cast.getFileName());
		}
	};

	public static final Impl<PwdCryptoMessage> PWD_CRYPTO_IMPL = new Impl<PwdCryptoMessage>() {

		@Override
		public PwdCryptoMessage cloneMsg(AbstractMessage msg) {
			throw new UnsupportedOperationException("not impled");
		}
	};

	private final static Map<Class<? extends AbstractMessage>, Impl<? extends AbstractMessage>> IMPL_MAP = new HashMap<Class<? extends AbstractMessage>, Impl<? extends AbstractMessage>>();

	static {

		IMPL_MAP.put(PlainTextMessage.class, PLAIN_TEXT_IMPL);
		IMPL_MAP.put(ChatBuildMessage.class, CHAT_BUILD_IMPL);
		IMPL_MAP.put(VoteTopicMessage.class, VOTE_TOPIC_IMPL);
		IMPL_MAP.put(VoteOfMeMessage.class, VOTE_OF_ME_IMPL);
		IMPL_MAP.put(RefUniqueFile.class, REF_UNIQUE_IMPL);
		IMPL_MAP.put(PwdCryptoMessage.class, PWD_CRYPTO_IMPL);
	}
}