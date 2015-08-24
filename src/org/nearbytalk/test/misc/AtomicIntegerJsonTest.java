package org.nearbytalk.test.misc;

import java.util.concurrent.atomic.AtomicInteger;

import org.nearbytalk.runtime.GsonThreadInstance;

import junit.framework.TestCase;

import com.google.gson.Gson;

public class AtomicIntegerJsonTest extends TestCase{
	
	public void testAtomicIntegerJson(){
		Gson gson = GsonThreadInstance.FULL_GSON.get();
		
		int toJsonValue=123;
		
		String jsonString=gson.toJson(new AtomicInteger(toJsonValue));
		
		int value=gson.fromJson(jsonString, Integer.class);
		
		assertEquals(toJsonValue, value);
	}
	
	public void testIntegerJsonString2AtomicInteger(){
		Gson gson=GsonThreadInstance.FULL_GSON.get();
		
		int toJsonValue=12;
		
		String jsonString=gson.toJson(toJsonValue);
		
		AtomicInteger atomicInteger=gson.fromJson(jsonString, AtomicInteger.class);
		
		assertEquals(toJsonValue, atomicInteger.get());
		
	}

}
