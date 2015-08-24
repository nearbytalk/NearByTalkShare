package org.nearbytalk.test.misc;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.nearbytalk.test.http.ServletTestBase;
import org.nearbytalk.util.ServerDiscover;
import org.nearbytalk.util.ServerDiscover.ScanCallback;


public class ServerDiscoverTest extends ServletTestBase {


	Inet4Address local=null;
	
	public void testLocalScan() throws UnknownHostException, InterruptedException, ExecutionException, SocketException {

		
		
		Enumeration e = NetworkInterface.getNetworkInterfaces();
		while(e.hasMoreElements())
		{
		    NetworkInterface n = (NetworkInterface) e.nextElement();
		    Enumeration ee = n.getInetAddresses();
		    while (ee.hasMoreElements())
		    {
		        InetAddress i = (InetAddress) ee.nextElement();

		        if (i.isSiteLocalAddress() && i instanceof Inet4Address) {
					
		        	local=(Inet4Address) i;
		        	break;
				}
		    }
		    if (local!=null) {
		    	break;
			}
		}
		
		assertNotNull(local);
		

		final List<Inet4Address> found=new ArrayList<Inet4Address>();
		ServerDiscover discover = new ServerDiscover(
				local,

				new ScanCallback() {

					@Override
					public void onFound(Inet4Address addr) {

						found.add(addr);

					}

					@Override
					public void onScan(Inet4Address addr,float percent) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void onNotFound(Inet4Address addr) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void onFinished() {
						// TODO Auto-generated method stub
						
					}
				});
		Future<?> future = discover.start();

		future.get();

		
		assertTrue(found.contains(local));

	}
}
