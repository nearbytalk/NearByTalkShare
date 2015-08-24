package org.nearbytalk.identity;

import java.lang.reflect.Type;
import java.util.Map;

import org.nearbytalk.exception.BadReferenceException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

/**
 * @author unknown this is a proxy message which wrap VoteTopic/VoteOfMe to
 *         users which should see protected content
 * 
 */
public class ProtectedProxyMessage extends
		CompoundMessage<AbstractMessage> {

	private Map<String, Object> properties;

	public Map<String, Object> getProperties() {
		return properties;
	}

	public ProtectedProxyMessage(Map<String, Object> properties,
			BaseUserInfo sender, MessageType messageType,
			AbstractMessage referenceMessage) {
		super(sender, messageType, referenceMessage);
		this.properties = properties;
	}

	private void throwUnsupport() {
		throw new UnsupportedOperationException(
				"proxy message didn't support this action");
	}

	public static final String IS_PROTECTED_PROXY_KEY = "isProtectedProxy";

	@Override
	public void digestId() {
		throwUnsupport();
	}

	@Override
	public String asPlainText() {
		throwUnsupport();
		return null;
	}

	@Override
	public void setReferenceMessageLater(AbstractMessage message)
			throws BadReferenceException {

		throwUnsupport();
	}

	@Override
	public void invalid() {
		throwUnsupport();
	}

	public static final Type SUPER_TYPE= new TypeToken<CompoundMessage<AbstractMessage>>() {
	}.getType();

	public static JsonSerializer<ProtectedProxyMessage> Gsoner = new JsonSerializer<ProtectedProxyMessage>() {

		@Override
		public JsonElement serialize(ProtectedProxyMessage src, Type typeOfSrc,
				JsonSerializationContext context) {

			JsonObject ret = (JsonObject) context.serialize(src, SUPER_TYPE);

			ret.addProperty(IS_PROTECTED_PROXY_KEY, true);

			Map<String, Object> properties = src.getProperties();

			if (properties == null) {
				return ret;
			}

			// makes properties just self properties

			for (String property : properties.keySet()) {

				ret.add(property, context.serialize(properties.get(property)));

			}

			return ret;
		}
	};

}
