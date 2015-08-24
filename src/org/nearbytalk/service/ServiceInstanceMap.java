package org.nearbytalk.service;

import java.util.HashMap;

public class ServiceInstanceMap {

	private HashMap<Class, AbstractService> allService = new HashMap<Class, AbstractService>();

	private ServiceInstanceMap() {
		allService
				.put(ClientUserInfoService.class, new ClientUserInfoService());

		allService.put(MessageService.class, new MessageService());

	}

	private static ServiceInstanceMap instanceMap = new ServiceInstanceMap();

	public <T extends AbstractService> T getService(Class<T> clazz) {

		if (clazz == null) {
			throw new IllegalArgumentException("class should not be null");

		}

		if (allService.containsKey(clazz)) {
			return (T) allService.get(clazz);
		}

		throw new IllegalStateException("not such service in map "
				+ clazz.getCanonicalName());

	}

	public static ServiceInstanceMap getInstance() {
		return instanceMap;
	}

}
