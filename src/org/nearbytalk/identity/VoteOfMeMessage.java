package org.nearbytalk.identity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.runtime.Global.VoteAnonymous;
import org.nearbytalk.util.Utility;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

public class VoteOfMeMessage extends CompoundMessage<VoteTopicMessage> implements IVoteVisibleControl{

	private Set<String> options;
	
	/**
	 * 
	 * other words to say
	 */
	private String comment;
	
	public static final MessageType MESSAGE_TYPE=MessageType.VOTE_OF_ME;
	
	public static final String OPTIONS_KEY="options";
	
	public static final String COMMENT_KEY="comment";

	public VoteOfMeMessage(String idBytes, BaseUserInfo sender, String text,
			Date createDate,String referenceIdBytes,int referencedCounter,int agreeCounter,int disagreeCounter){
		super(idBytes,sender,MESSAGE_TYPE,createDate,referenceIdBytes,referencedCounter,agreeCounter,disagreeCounter);
		
		parseJson(text);

		setReferenceDepth(2);
	}
	
	public VoteOfMeMessage(BaseUserInfo sender,String comment,Set<String> myOptions,VoteTopicMessage voteTopicMessage) throws BadReferenceException{
		super(sender, MESSAGE_TYPE, voteTopicMessage);
		this.comment=comment;
		this.options=myOptions;
		voteTopicMessage.update(myOptions);
		
		digestId();
		parseTopics();
	}

	@Override
	public String asPlainText() {
		
		
		JsonObject object=new JsonObject();
		
		object.addProperty(COMMENT_KEY, comment);
		
		Gson gson=GsonThreadInstance.FULL_GSON.get();
		
		object.add(OPTIONS_KEY, gson.toJsonTree(options));
		
		return gson.toJson(object);
		
	}

	@Override
	public void setReferenceMessageLater(AbstractMessage message) throws BadReferenceException {
		
		if (! (message instanceof VoteTopicMessage)) {
			throw new BadReferenceException("VoteOfMe Message can only reference VoteTopicMessage");
		}
		
		VoteTopicMessage voteTopicMessage=(VoteTopicMessage) message;
		
		if (!voteTopicMessage.getOptions().containsAll(options)) {
			throw new BadReferenceException("VoteOfMe options not allowed in VoteTopic");
		}
		
		if (!voteTopicMessage.isMultiSelection() && options.size()>1) {
			throw new BadReferenceException("topic not allow multi selection");
		}
		
		voteTopicMessage.update(options);
		
		setReferenceMessage(voteTopicMessage);
		
	}

	private void parseJson(String jsonString) {

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		JsonElement element = gson.fromJson(jsonString, JsonElement.class);
		
		JsonObject object=element.getAsJsonObject();
		
		options=jsonElementToType(new TypeToken<Set<String>>(){}.getType(), object,OPTIONS_KEY );
		
		if (options==null) {
			throw new JsonParseException("options key is null");
		}
		
		JsonElement commentElement=object.get(COMMENT_KEY);

		if (commentElement!=null) {
			comment=commentElement.getAsString();
		}
		
	}

	public Set<String> getOptions() {
		return options;
	}

	@Override
	public void digestId() {
		
		// only consider senderIdBytes,anyReferenceIdBytes(idBytes of VoteTopicMessage)
		// this allow only once VoteOfMeMessage for a VoteTopicMessage
		
		digestId(false);
	}
	
	

	@Override
	protected String additionalDigestString() {
		return null;
	}

	@Override
	public void invalid() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void parseTopics() {
		setTopics(Utility.parseTopics(comment));
	}

	public String getComment() {
		return comment;
	}
	
	
	//depends on runtime control ,this is very likely only 1 of 3 is used
	
	ProtectedProxyMessage selfProtectWithTopicUnprotect;
	
	ProtectedProxyMessage selfProtectWithTopicProtect;
	
	ProtectedProxyMessage selfUnprotectWithTopicProtect;
	
	private AbstractMessage fastCheck(boolean voteOfMeInvisible,boolean voteTopicInvisible){
		
		if (voteOfMeInvisible) {
			if (voteTopicInvisible) {
				return selfProtectWithTopicProtect;
			}else{
				return selfProtectWithTopicUnprotect;				
			}
		}else {
			return selfUnprotectWithTopicProtect;
		}
	}

	@Override
	public AbstractMessage createProxy(VoteAnonymous voteOfMeAnonymous,boolean voteTopicInvisible){
		
		if (voteOfMeAnonymous == VoteAnonymous.ALWAYS_VISIBLE && !voteTopicInvisible ) {
			//no change with each other
			return this;
		}
		
		boolean voteOfMeInvisible=(voteOfMeAnonymous==VoteAnonymous.ALWAYS_INVISBLE);

		AbstractMessage ret=fastCheck(voteOfMeInvisible , voteTopicInvisible);

		if (ret!=null) {
			return ret;
		}
		
		
		// cold path here
		//for simple use ,3 different method use same sync lock
		synchronized (this) {
			
			ret=fastCheck(voteOfMeInvisible, voteTopicInvisible);

			if (ret!=null) {
				return ret;
			}
			
			Map<String, Object> properties=null;
			
			if (!voteOfMeInvisible) {
				properties=new HashMap<String, Object>();
				//this is a trick, if VISIBLE_AFTER_VOTE, we also reserve
				//options and comment, leave it to client hidden.
				//since truely VISIBLE_AFTER_VOTE needs client re-query server
				//to get correct result, it's too complex for us.
				//(but we still makes VoteTopic result truely hidden from server)
				properties.put(OPTIONS_KEY, options);
				properties.put(COMMENT_KEY, comment);
			}
			
			ret=new ProtectedProxyMessage(properties, getSender(), MESSAGE_TYPE, 
					getReferenceMessage().createProxy(voteOfMeAnonymous, voteTopicInvisible));
			ret.setIdBytes(getIdBytes());

			if (voteOfMeInvisible) {
				if (voteTopicInvisible) {
					assert selfProtectWithTopicProtect==null;
					selfProtectWithTopicProtect=(ProtectedProxyMessage) ret;
				}else{
					assert selfProtectWithTopicUnprotect==null;
					selfProtectWithTopicUnprotect=(ProtectedProxyMessage) ret;				
				}
			}else {
				assert selfUnprotectWithTopicProtect==null;
				selfUnprotectWithTopicProtect=(ProtectedProxyMessage) ret;
			}		
			
			return ret;
			
		}
	}

	@Override
	public String getVoteTopicIdBytes() {
		return getReferenceIdBytes();
	}
	
}
