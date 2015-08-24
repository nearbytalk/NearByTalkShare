package org.nearbytalk.identity;

import java.lang.reflect.Type;

import org.nearbytalk.util.Utility;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.sun.org.apache.bcel.internal.generic.NEW;

public class BaseUserInfo extends BaseIdentifiable {

	private String userName;

	public static final String USER_NAME_JSON_KEY="userName";

	public BaseUserInfo(String idBytes,String userName){
		super(idBytes);
		
		//TODO use assert ?
		Utility.assumeNotNullOrEmpty(userName);

		this.userName=userName;
	}
	

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		Utility.assumeNotNullOrEmpty(userName);
		this.userName = userName;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BaseUserInfo other = (BaseUserInfo) obj;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}
	
	/**
	 *  compare without idbytes ,
	 *  for object read back from http compare with runtime generated ones 
	 * @param other
	 * @return
	 */
	public boolean sameStrippedUser(BaseUserInfo other){
		
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}

	@Override
	public void digestId() {
		//do nothing,leaves idBytes as NULL
	}

	
	public static final JsonSerializer<BaseUserInfo> STRIP_ID_BYTES_JSON_SERIALIZER=new JsonSerializer<BaseUserInfo>() {

		@Override
		public JsonElement serialize(BaseUserInfo arg0, Type arg1,
				JsonSerializationContext arg2) {
			
			JsonObject obj=new JsonObject();
			
			obj.addProperty(USER_NAME_JSON_KEY, arg0.userName);

			return obj;
		}
	};
}
