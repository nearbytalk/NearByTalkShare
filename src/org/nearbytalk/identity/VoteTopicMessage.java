package org.nearbytalk.identity;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.nearbytalk.runtime.Global.VoteAnonymous;
import org.nearbytalk.util.Utility;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

/**
 * VoteTopicMessage digest didn't consider create_date 
 * 		means different time same vote content is treated as same msg
 * 		content is the const header part of asPlainText
 * 
 * 
 * currently, SQLite lacks ability to update the json part of abstract_message
 * so use a hybird approach to achive the atomic update semantic.
 * const string part is saved as text header , will not change during record update
 * vote result is saved as text next part, will be update 
 * still, this is not atomic (like update set value=value+1) in sqlite, just plain update
 * so runtime manage a unique message cache (which one message only has one instance in cache)
 * and a updated VoteTopic queue (including lastest runtime updated result)
 * every VoteOfMeMessage update the unique message cache correctly, so we can 
 * safety overwrite previous updated VoteTopicMessage result in sqlite.
 * 
 * a custom sqlite function to update record will
 * make everything much simpler (not implemented currently)
 * 
 *
 */
public class VoteTopicMessage extends AbstractTextMessage implements IVoteVisibleControl{

	public static MessageType MESSAGE_TYPE = MessageType.VOTE_TOPIC;

	public final static String VOTE_TOPIC_KEY = "voteTopic";

	public final static String OPTIONS_KEY = "options";
	
	public final static String DESCRIPTION_KEY = "description";

	public final static String MULTI_SELECTION_KEY = "multiSelection";

	private String voteTopic;
	
	private String description;
	
	public final static int META_HEADER_LENGTH_LENGTH=4;
	
	private int metaHeaderLength;

	/**
	 * map needs not to be concurrent map, key is only read-only, will not insert.
	 * value is protected by sync on this
	 * 
	 * must be map will stable order,not hashmap. we need this when get keySet as part of idbytes digest
	 * 
	 */
	private LinkedHashMap<String,Long> results=new LinkedHashMap<String, Long>();

	private Boolean multiSelection;

	public VoteTopicMessage(String idBytes, BaseUserInfo sender, String text,
			Date createDate,int referencedCounter,int agreeCounter,int disagreeCounter) throws ParseException{
		super(idBytes,sender,MESSAGE_TYPE,createDate,referencedCounter,agreeCounter,disagreeCounter);
		parseCustomJson(text);

		setReferenceDepth(1);

		// not digest
	}
	
	/**
	 * for de-json use
	 * @param voteTopic
	 * @param description
	 * @param multiSelection
	 * @param options
	 */
	private VoteTopicMessage(String voteTopic,String description,boolean multiSelection,List<String> options){
		super(null, MESSAGE_TYPE);
		this.voteTopic=voteTopic;
		this.description=description;
		this.multiSelection=multiSelection;
		
		for (String string : options) {
			results.put(string, new Long(0));
		}
		
	}
	
	/**
	 * for message clone use (test)
	 * @param sender
	 * @param voteTopic
	 * @param description
	 * @param multiSelection
	 * @param results
	 */
	public VoteTopicMessage(BaseUserInfo sender,String voteTopic,String description,boolean multiSelection,LinkedHashMap<String,Long> results){
		super(sender, MESSAGE_TYPE);
		this.voteTopic=voteTopic;
		this.description=description;
		this.multiSelection=multiSelection;
			
		this.results.putAll(results);
		
		digestId();
		parseTopics();
	}


	/**
	 * for runtime constructor (for test now)
	 * @param sender
	 * @param voteTopic
	 * @param description
	 * @param multiSelection
	 * @param options
	 */
	public VoteTopicMessage(BaseUserInfo sender,String voteTopic,String description,boolean multiSelection,Collection<String> options){
		super(sender, MESSAGE_TYPE);
		this.voteTopic=voteTopic;
		this.description=description;
		this.multiSelection=multiSelection;
			
		for (String string : options) {
			this.results.put(string, 0l);
		}
		
		digestId();
		parseTopics();
	}
	
	@Override
	public String getReferenceIdBytes() {
		return null;
	}

	@Override
	public AbstractMessage getReferenceMessage() {
		return null;
	}

	@Override
	public String asPlainText() {
		
		// VoteTopicMessage have no direct database struct with it 
		// so no guarantee for atomic update. it's serialize as following:
		// metalength as number ,metaHeader as json (extactly long as metalength number) , jsonArray as voteofme selection result
		
		Writer writer=plainTextHeaderWriter();
		
		JsonWriter jsonWriter=new JsonWriter(writer);
		
		Gson gson=GsonThreadInstance.FULL_GSON.get();

		synchronized (this) {

			gson.toJson(gson.toJsonTree(results.values()),jsonWriter);
		}

		return writer.toString();
	}

	@Override
	public void setReferenceMessageLater(AbstractMessage message) {

		throw new UnsupportedOperationException(
				"VoteTopic do not support reference message");
	}

	public String getVoteTopic() {
		return voteTopic;
	}

	public Set<String> getOptions() {
		assert results!=null;
		return results.keySet();
	}
	
	public LinkedHashMap<String, Long> getResults(){
		return results;
	}

	public boolean isMultiSelection() {
		return multiSelection;
	}

	private static final Type LIST_STRING_TYPE= new TypeToken<List<String>>(){}.getType();

	private static final Type COLLECTION_LONG_TYPE= new TypeToken<Collection<Long>>(){}.getType();

		
	public static int JSON_BEGIN_INDEX=4;

	private void parseCustomJson(String jsonString) throws ParseException {

		Gson gson = GsonThreadInstance.FULL_GSON.get();

		
		
		if (jsonString.length()<4) {
			throw new ParseException(" json string too short",0);
		}
		
		metaHeaderLength=Integer.parseInt(jsonString.substring(0,4), 16);
		
		if (metaHeaderLength<=0) {
			throw new ParseException("meta header length <= 0", 0);
		}
		
		int resultBeginIndex=JSON_BEGIN_INDEX+metaHeaderLength;
		
		String metaHeaderString=jsonString.substring(JSON_BEGIN_INDEX,resultBeginIndex);
		
		
		JsonElement metaHeaderElement=null;
		try {
			metaHeaderElement=gson.fromJson(metaHeaderString, JsonElement.class);
		} catch (JsonSyntaxException e) {
			throw new ParseException("bad json", 0);
		}
		
		
		JsonObject metaHeaderObject=metaHeaderElement.getAsJsonObject();
		
		JsonElement voteTopicElement=metaHeaderObject.get(VOTE_TOPIC_KEY);
		
		if (voteTopicElement==null) {
			
			throw new ParseException("no vote_topic field", 0);
		}
		
		voteTopic = voteTopicElement.getAsString();

		List<String> options= jsonElementToType(LIST_STRING_TYPE, metaHeaderObject, OPTIONS_KEY);
		
		if (options==null) {
			throw new ParseException("bad json, no options property", 0);
		}

		JsonElement descriptionElement = metaHeaderObject.get(DESCRIPTION_KEY);

		if (descriptionElement != null) {
			description = descriptionElement.getAsString();
		}

		
		JsonElement multiSelectionElement=metaHeaderObject.get(MULTI_SELECTION_KEY);
		
		if (multiSelectionElement==null) {
			throw new ParseException("bad json", 0);
		}

		multiSelection=multiSelectionElement.getAsBoolean();
		
		String resultJsonString=jsonString.substring(resultBeginIndex);
		
		Collection<Long> resultArray= null;
		
		try{
			resultArray=gson.fromJson(resultJsonString, COLLECTION_LONG_TYPE);
		}catch (JsonSyntaxException e) {
			throw new ParseException("bad json", 0);
		}

		//just throw

		if (
				voteTopic==null || voteTopic.isEmpty() || 
				options== null || options.isEmpty() ||
				multiSelection == null || 
				resultArray==null || resultArray.isEmpty()) {
			throw new ParseException("json string lacks nessery key or fields empty",0);
		}
		
		if (options.size()!=resultArray.size()) {
			throw new ParseException("options size not fit result size", 0);
		}
		
		Iterator<String> optionIterater=options.iterator();
		Iterator<Long> valueIterator=resultArray.iterator();
		
		while (optionIterater.hasNext()) {
			String key=optionIterater.next();
			Long value=valueIterator.next();
			
			results.put(key, value);
		}

	}
	
	private Writer plainTextHeaderWriter(){
		Gson gson=GsonThreadInstance.FULL_GSON.get();
		
		JsonObject metaHeader=new JsonObject() ;
		
		JsonElement optionsPart=gson.toJsonTree(getOptions());

		metaHeader.addProperty(VOTE_TOPIC_KEY, voteTopic);

		metaHeader.addProperty(DESCRIPTION_KEY, description);

		metaHeader.addProperty(MULTI_SELECTION_KEY, multiSelection);
		
		metaHeader.add(OPTIONS_KEY, optionsPart);
		
		String metaString=gson.toJson(metaHeader);
		
		String metaLength=String.format("%04x",metaString.length());
		
		StringWriter writer=new StringWriter();
		
		writer.append(metaLength);
		
		JsonWriter jsonWriter=new JsonWriter(writer);
		
		gson.toJson(metaHeader,jsonWriter);
		
		return writer;
	}

	@Override
	public void digestId() {
		digestId(false);
	}
	
	

	@Override
	protected String additionalDigestString() {
		Writer headerPart=plainTextHeaderWriter();
		
		return headerPart.toString();
	}

	@Override
	public void invalid() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void parseTopics() {
		Set<String> topics=Utility.parseTopics(voteTopic);
		
		topics.addAll(Utility.parseTopics(description));

		//ignore options

		setTopics(topics);
	}

	@Override
	public void replaceReferenceMessage(AbstractMessage toReplace) {
		//do nothing
	}

	public void update(Set<String> options) throws BadReferenceException{
		if (options==null || options.isEmpty()) {
			throw new BadReferenceException("options empty");
		}
		
		if (!multiSelection && options.size()!=1) {
			throw new BadReferenceException("only one option allowed");
		}
		
		synchronized (this) {
			for (String string : options) {
				results.put(string, results.get(string)+1);
			}
		}
		
	}
	
	public int getMetaHeaderLength(){
		return metaHeaderLength;
	}
	
	public String getVoteResultString(){
		return GsonThreadInstance.FULL_GSON.get().toJson(results.values());
	}

	public String getDescription() {
		return description;
	}
	
	private ProtectedProxyMessage proxyMessage;

	@Override
	public AbstractMessage createProxy(VoteAnonymous unused,boolean voteTopicInvisble){
		
		if (!voteTopicInvisble) {
			return this;
		}
		
		if (proxyMessage!=null) {
			//hot code path
			return proxyMessage;
		}
		
		
		//this sync use this object as lock, may hit multi-thread performance with update
		//but this is cold path.
		synchronized (this) {
			
			if (proxyMessage!=null) {
				return proxyMessage;
			}

			Map<String, Object> properties=new HashMap<String, Object>();

			assert voteTopic!=null;
			assert multiSelection!=null;
			assert getOptions()!=null;

			properties.put(VOTE_TOPIC_KEY, voteTopic);
			properties.put(DESCRIPTION_KEY, description);
			properties.put(MULTI_SELECTION_KEY, multiSelection);
			properties.put(OPTIONS_KEY, getOptions());

			proxyMessage=new ProtectedProxyMessage(properties, getSender(), MESSAGE_TYPE, null);
			proxyMessage.setIdBytes(getIdBytes());

			return proxyMessage;
		}
	}

	@Override
	public String getVoteTopicIdBytes() {
		return getIdBytes();
	}

}
