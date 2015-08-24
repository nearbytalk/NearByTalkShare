package org.nearbytalk.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.nearbytalk.http.GlobalInfoServlet;
import org.nearbytalk.http.GlobalInfoServlet.GlobalInfo;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.GsonThreadInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * try to find server over address.
 * use a thread pool to do batch checking http url
 * XXX.XXX.XXX.XXX:8823/GlobalServlet and json deserialization
 * to determine if this is a valid server.
 * 
 * use {@link ScanCallback} to receive scanning infomation
 * 
 */
public class ServerDiscover {

	private static Logger log = LoggerFactory.getLogger(ServerDiscover.class);
	
	/**
	 * 10KB is enough for GlobalInfo object json
	 * 
	 */
	public static int MAX_GLOBAL_INFO_LENGTH=10000;
	
	private AtomicInteger completed=new AtomicInteger();

	/**
	 * callback interface to receive scan information
	 * 
	 */
	public static interface ScanCallback {

		/**
		 * will be called when begin to scan addr
		 * this will be the first callback when found a server
		 * @param addr
		 * @param percent how much percent completed. [0~1]
		 */
		public void onScan(Inet4Address addr,float percent);

		/**
		 * will be called when server is found ,
		 * followed by {@link #onScan(Inet4Address)}
		 * @param addr
		 */
		public void onFound(Inet4Address addr);

		/**
		 * will be called after {@link #onScan(Inet4Address)}
		 * @param addr
		 */
		public void onNotFound(Inet4Address addr);
		
		/**
		 * will be called after all IP in this subnet checked.
		 * it's called just after all {@link #onFound(Inet4Address)}
		 * {@link #onNotFound(Inet4Address)} 
		 * 
		 */
		public void onFinished();

	}

	private Inet4Address netAddress;
	private ScanCallback scanCallback;

	private boolean started = false;

	private static final int DEFAULT_HTTP_TIME_OUT_MS = 1000;

	/**
	 * construct info ,not scan
	 * @param netAddress
	 * @param callback
	 */
	public ServerDiscover(Inet4Address netAddress, ScanCallback callback) {
		this.netAddress = netAddress;
		this.scanCallback = callback;
	}

	private ExecutorService threadPool;

	private static final String ADDRESS_FORMAT_STRING = "http://%1$s:%2$d%3$s";

	/**
	 * scan server over netAddress, in thread pool
	 * this is a async call,return immediately.
	 * @return Future takes all thread Future. caller can use this to block until all thread complete, or interrupte it.
	 *     
	 */
	public Future<?> start() {

		synchronized (this) {
			if (threadPool == null) {
				threadPool = Executors.newFixedThreadPool(4);
			}

			if (started) {
				return null;
			}
			started = true;
		}

		final byte[] address = netAddress.getAddress();

		final List<Future<?>> allFuture = new ArrayList<Future<?>>();

		for (int i = 0; i < 255; i++) {
			//deal with different thread sees the same counter problem
			final byte temp=(byte) i;
			allFuture.add(threadPool.submit(new Runnable() {

				@Override
				public void run() {
					
			
					final byte[] thisAddr = address;
					thisAddr[3] = (byte) temp;
					
					HttpURLConnection checkConnection=null;
						
					Inet4Address addr = null;
					try {
						addr=(Inet4Address) Inet4Address
								.getByAddress(thisAddr);

						log.trace("scan {}",addr);

						scanCallback.onScan(addr,completed.get()/(float)255);

						String url = String.format(
								ADDRESS_FORMAT_STRING,
								addr.getHostAddress(),
								Global.HttpServerInfo.listenPort,
								Utility.makeupAccessPath(GlobalInfoServlet.class));

						URL checkUrl = new URL(url);

						checkConnection = (HttpURLConnection) checkUrl
								.openConnection();
						
						checkConnection
								.setConnectTimeout(DEFAULT_HTTP_TIME_OUT_MS);
						
						if (checkConnection.getResponseCode()!=HttpURLConnection.HTTP_OK) {
							
							scanCallback.onNotFound(netAddress);
							
							return;
						}
						
						int contentLength=checkConnection.getContentLength();
						
						//GlobalInfoServlet must has Content-Length header
						//to avoid bad server attack
						if (contentLength<=0 || contentLength>MAX_GLOBAL_INFO_LENGTH) {
							
							log.error("server {} discovered,bug GlobalInfoServlet response " +
									"content length {} out of range",addr,contentLength);
							
							scanCallback.onNotFound(netAddress);
							
							return;
						}

						InputStream inputStream = new BufferedInputStream(
								checkConnection.getInputStream());
						
						
						Gson gson = GsonThreadInstance.STRIP_USER_ID_BYTES_GSON
								.get();

						GlobalInfo globalInfo = gson.fromJson(
								new InputStreamReader(inputStream),
								GlobalInfo.class);
						
						if (globalInfo==null) {
							
							log.error("server {} discovered but can't json deserialization",addr);
							scanCallback.onNotFound(addr);
							return;
						}


						log.trace("check GlobalInfo from {} success: {}",addr,globalInfo);

						scanCallback.onFound(addr);
						return;

					} catch (UnknownHostException e) {
						log.error("{}", e);
					} catch (MalformedURLException e) {
						log.error("{}", e);
					} catch (IOException e) {
						log.trace("addr {} has no server", addr);
					}finally{
						completed.incrementAndGet();
						checkConnection.disconnect();
					}


				}
			}));
		}

		final Future ret=new Future() {

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				stop();
				return true;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public boolean isDone() {

				for (Future<?> future : allFuture) {
					if (!future.isDone()) {
						return false;

					}
				}

				return true;
			}

			@Override
			public Object get() throws InterruptedException, ExecutionException {
				
				for (Future<?> future : allFuture) {
					future.get();
				}
				
				return null;
			}

			@Override
			public Object get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException,
					TimeoutException {
				throw new UnsupportedOperationException();
			}
		};
		
		new Thread(){

			@Override
			public void run() {
				try {
					ret.get();
					scanCallback.onFinished();
				} catch (InterruptedException e) {
					log.debug("scanning is interrupted");
				} catch (ExecutionException e) {
					log.error("{}",e);
				}
			}
			
		}.start();

		return ret;
	}

	public void stop() {

		synchronized (this) {
			if (!started) {
				return;
			}

			threadPool.shutdown();

		}

	}

}
