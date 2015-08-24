package org.nearbytalk.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Global {

	public String hostIp;

	public static final int USER_DESCRIPTION_MAX_LENGTH = 200;
	
	public static final int BROADCAST_MESSAGE_LIMIT=200;
	
	public static final int MESSAGE_MAX_LENGTH=65535;
	
	public static final int POLL_INTERVAL_MILLION_SECONDS = 30000;
	
	public static final int REFERENCE_MAX_DEPTH=10;
	
	public static final int CHAT_BUILD_MAX_DEPTH=REFERENCE_MAX_DEPTH-1;

	public class HttpServerInfo {

		public static final int listenPort = 8823;

		public boolean isRunning = false;

	}

	public class DNSInfo {

		public static final int DEFAULT_LISTEN_PORT = 53;

		public int listenPort = -1;

		public boolean isRunning = false;
	}

	public DNSInfo dnsInfo = new DNSInfo();

	public HttpServerInfo httpServerInfo = new HttpServerInfo();
	
	public long fileUploadLimitByte = 1024*1024*1024;
	
	public static enum VoteAnonymous{
		
		ALWAYS_VISIBLE,ALWAYS_INVISBLE,VISIBLE_AFTER_VOTE
	}
	
	public VoteAnonymous anonymousVoteOfOthers = VoteAnonymous.VISIBLE_AFTER_VOTE;

	/**
	 * if this is false,vote result will be displayed even user not vote on it
	 * if this is true, only user voted on the topic can see the result
	 * 
	 */
	public boolean anonymousVoteTopic= true;

	public String getAppRootDirectory(){
		return platformAbstract.getAppRootDirectory();
	}
	
	private IPlatformAbstract platformAbstract;


	public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public static final String UTF8_ENCODING = "UTF-8";

	public static final String UPLOAD_PATH = "upload";
	
	public static final String TEMP_UPLOAD_PATH = "temp_upload";

	private Global() {
		Properties prop = new Properties();
		InputStream in = getClass().getResourceAsStream("predefine.properties");

	

		String platformClass = null;
		if (in != null) {
			try {
				prop.load(in);
			} catch (Exception e) {
				e.printStackTrace();
			} finally{
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			platformClass = prop.getProperty("platform");
		}
		
		if (platformClass==null) {
			platformClass="org.nearbytalk.runtime.DesktopPlatform";
		}

		Class<? extends IPlatformAbstract> klass;
		
		try {
			klass = (Class<? extends IPlatformAbstract>) Class
					.forName(platformClass);
			platformAbstract = klass.newInstance();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static Global instance = new Global();

	static public Global getInstance() {
		return instance;
	}

	public static final String DB_FILENAME = "nearbytalk.sqlite";

	public static final String DEFAULT_RAW_DATASTORE_PASSWORD = "0000000000000000000000000000000000000000";

	public String getRawDataStorePassword(){
		return platformAbstract.getRawDataStorePassword();
	}

	public IPlatformAbstract getPlatformAbstract() {
		return platformAbstract;
	}
	
	
}
