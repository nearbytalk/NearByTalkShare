package org.nearbytalk.util;

import java.net.UnknownHostException;

public class IpAddrConvert {

	public static byte[] string2Addr(String ipString) throws UnknownHostException {

		String[] addrArray = ipString.split("\\.");

		if (addrArray.length != 4) {
			throw new UnknownHostException();
		}

		byte[] addr = new byte[4];

		int shiftBits[] = { 6, 4, 2, 0 };

		for (int i = 0; i < addrArray.length; i++) {

			int thisOne = Integer.parseInt(addrArray[i]);

			if (thisOne < 0 || thisOne > 255) {
				throw new UnknownHostException("byte " + i + ": " + thisOne
						+ " out of range [0-255]");
			}

			addr[i] = (byte) (thisOne & (0xFF << shiftBits[i]) >> shiftBits[i]);
		}

		return addr;

	}

	public static String addr2String(byte[] ipAddr) throws UnknownHostException {

		if (ipAddr.length != 4) {
			throw new UnknownHostException("addr is not 4 length");
		}
		
		StringBuilder stringBuilder=new StringBuilder();
		
		for (byte b : ipAddr) {
			
			stringBuilder.append((int)(b&0xFF)).append('.');
		}
		
		return stringBuilder.substring(0, stringBuilder.length()-1);

	}

}
