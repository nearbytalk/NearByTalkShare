package org.nearbytalk.test.dummydns;

import org.nearbytalk.dummydns.DummyDNS;

import junit.framework.TestCase;


public class DummyDNSTest extends TestCase{
	
	public void testStopBefore1stPackage() throws InterruptedException{
		
		DummyDNS dns=new DummyDNS();
		
		dns.start();
		
		//enough for dns thread to enter waiting loop
		Thread.sleep(1000);
		
		dns.setStopping(true);
		
		//enough for dns thread to stop
		Thread.sleep(1000);
		
		assertFalse(dns.isAlive());
		
	}

}
