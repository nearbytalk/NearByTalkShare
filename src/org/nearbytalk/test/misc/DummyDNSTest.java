package org.nearbytalk.test.misc;

import java.io.IOException;
import java.net.UnknownHostException;

import junit.framework.TestCase;

import org.nearbytalk.dummydns.DummyDNS;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.util.IpAddrConvert;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;


public class DummyDNSTest extends TestCase {

	public void testUnknownException() {

		DummyDNS dummyDNS = new DummyDNS();

		try {
			dummyDNS.setHostIp("abccc");
			fail("should throw " + UnknownHostException.class.getSimpleName());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block

			dummyDNS.setStopping(true);
		}
	}

	public void testRunning() throws InterruptedException {
		DummyDNS dummyDNS = new DummyDNS();

		dummyDNS.start();

		Thread.sleep(1000);

		assertEquals(true, Global.getInstance().dnsInfo.isRunning);

		dummyDNS.setStopping(true);
	}

	
	public DummyDNS startAndWaitForStart(String resolveName, int port)
			throws UnknownHostException, InterruptedException {

		DummyDNS dummyDNS = new DummyDNS();

		dummyDNS.setHostIp(resolveName);

		dummyDNS.setListenPort(port);

		dummyDNS.start();

		for (int i = 0; i < 3; ++i) {
			Thread.sleep(1000);
			if (Global.getInstance().dnsInfo.isRunning) {
				break;
			}
		}

		assertTrue(Global.getInstance().dnsInfo.isRunning);

		return dummyDNS;
	}

	public void testLookup() throws IOException, InterruptedException {

		String expectedIp = "104.12.52.3";

		DummyDNS dummyDNS = startAndWaitForStart(expectedIp, 53);

		int type = Type.ANY, dclass = DClass.IN;

		for (int i = 0; i < 10; ++i) {

			Resolver res = new SimpleResolver("127.0.0.1");

			res.setPort(Global.getInstance().dnsInfo.listenPort);

			Name name = Name.fromString("www.baidu.com", Name.root);

			Record rec = Record.newRecord(name, type, dclass);
			Message query = Message.newQuery(rec);

			Message response = res.send(query);

			Record[] answers = response.getSectionArray(Section.ANSWER);

			assertNotNull(answers);

			for (Record record : answers) {

				if (record.getClass() != ARecord.class) {
					continue;
				}

				ARecord aRecord = (ARecord) record;

				String reversedIp = IpAddrConvert.addr2String(aRecord
						.getAddress().getAddress());

				assertEquals(expectedIp, reversedIp);

			}

		}

		dummyDNS.setStopping(true);

	}
}
