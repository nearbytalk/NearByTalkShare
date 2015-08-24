package org.nearbytalk.dummydns;

public class DummyDNSRunner {
	public static void main(String[] args) throws InterruptedException {
		DummyDNS serverDns=new DummyDNS();
		serverDns.setListenPort(53);
		serverDns.start();
		serverDns.join();
	}

}
