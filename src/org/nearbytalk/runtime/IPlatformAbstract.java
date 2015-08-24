package org.nearbytalk.runtime;

public interface IPlatformAbstract {

	String getAppRootDirectory();

	String getRawDataStorePassword();

	public void commitPasswordChangeNew(String newRawPassword);

}
