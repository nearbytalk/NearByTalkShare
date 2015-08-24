package org.nearbytalk.runtime;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class AtomicIntegerGsoner implements JsonSerializer<AtomicInteger>,
		JsonDeserializer<AtomicInteger> {

	private final static Type INTEGER_TYPE = new TypeToken<Integer>() {
	}.getType();

	@Override
	public JsonElement serialize(AtomicInteger src, Type typeOfSrc,
			JsonSerializationContext context) {
		return context.serialize(src, INTEGER_TYPE);
	}

	@Override
	public AtomicInteger deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		return new AtomicInteger(json.getAsInt());
	}

}
