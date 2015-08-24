package org.nearbytalk.test.misc;

import java.net.UnknownHostException;

import org.nearbytalk.util.IpAddrConvert;

import junit.framework.TestCase;


public class IpAddrConvertTest extends TestCase {

	public void testReverseSame() throws UnknownHostException {
		String ip = "124.223.112.55";

		byte[] addr = IpAddrConvert.string2Addr(ip);

		String reverseIp = IpAddrConvert.addr2String(addr);

		assertEquals(ip, reverseIp);
	}

	public void testInvalidString() {

		String[] bads = new String[] { "asdfasd", "192.13.12.12a3", "123.12",
				"532", "123.21.23.442", "-1232132", "-12.12.44.55" };

		for (String string : bads) {

			try {

				byte[] addr = IpAddrConvert.string2Addr(string);

				fail("should throw");

			} catch (Exception e) {
				continue;
			}

			fail("should throw");
		}

	}
}
