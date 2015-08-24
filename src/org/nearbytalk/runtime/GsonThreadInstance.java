package org.nearbytalk.runtime;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;

import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.BaseUserInfo;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.ProtectedProxyMessage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

public class GsonThreadInstance {
	
	static public ThreadLocal<Gson> FULL_GSON = new ThreadLocal<Gson>() {

		@Override
		protected Gson initialValue() {
			return commonBuilder().create();
		}

	};

	static private GsonBuilder commonBuilder() {

		return new GsonBuilder().setDateFormat(Global.DATE_FORMAT)
				.registerTypeAdapter(AbstractMessage.class,
						new AbstractMessage.Gsoner())
						.registerTypeAdapter(AtomicInteger.class, new AtomicIntegerGsoner())
						.registerTypeAdapter(ProtectedProxyMessage.class, ProtectedProxyMessage.Gsoner);
	}
	
	public static final String RESULT_SUCCESS_JSON_KEY="success";

	public static final String RESULT_DETAIL_JSON_KEY="detail";
	
	public static final Type OBJECT_TYPE=new TypeToken<Object>(){}.getType();

	static public void writeServletResult(boolean stripUserIdBytes,boolean success,Object detail,
			Writer writer) throws IOException{
		Gson gson= stripUserIdBytes ? STRIP_USER_ID_BYTES_GSON.get()
				:FULL_GSON.get();
		
		JsonWriter jsonWriter=new JsonWriter(writer);
		
		jsonWriter.beginObject();
		
		jsonWriter.name(RESULT_SUCCESS_JSON_KEY).value(success);
		
		jsonWriter.name(RESULT_DETAIL_JSON_KEY);
		
		gson.toJson(detail,OBJECT_TYPE , jsonWriter);
		
		jsonWriter.endObject();
		
	}

	static public ThreadLocal<Gson> STRIP_USER_ID_BYTES_GSON = new ThreadLocal<Gson>() {

		@Override
		protected Gson initialValue() {
			return commonBuilder()
					.registerTypeAdapter(ClientUserInfo.class,
							ClientUserInfo.STRIP_ID_BYTES_JSON_SERIALIZER)
					.registerTypeAdapter(BaseUserInfo.class,
							BaseUserInfo.STRIP_ID_BYTES_JSON_SERIALIZER)
					.create();
		}
	};
	

}
