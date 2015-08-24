package org.nearbytalk.service;

import org.nearbytalk.datastore.IDataStore;
import org.nearbytalk.runtime.UniqueObject;

public class AbstractService {


	protected IDataStore getDataStore() {
		return UniqueObject.getInstance().getDataStore();
	}

}
