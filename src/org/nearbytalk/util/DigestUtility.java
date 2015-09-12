package org.nearbytalk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import org.nearbytalk.exception.NullFieldException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DigestUtility {

	public final static String DIGEST_ALGORITHM = "SHA-1";
	public final static String CIPER_ALGORITHM = "AES/CBC/PKCS5Padding";
	
	private static final Logger log=LoggerFactory.getLogger(DigestUtility.class);

	public static byte[] digestCanBeNull(String[] strings) {

		MessageDigest digest = getSHA1Digest();

		synchronized (digest) {

			try {
				for (String string : strings) {

					if (string != null) {
						digest.update(Utility.stringUTF8Bytes(string));
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
			}

		}
		return digest.digest();
	}

	public static String byteArrayToHexString(byte[] byteArray) {

		Utility.assumeNotNull(byteArray);

		final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
				'9', 'a', 'b', 'c', 'd', 'e', 'f' };

		char hexChars[] = new char[byteArray.length * 2];

		int v = 0;

		for (int j = 0; j < byteArray.length; ++j) {
			v = byteArray[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];

		}

		return new String(hexChars);
	}

	public static byte[] digestNotNull(byte[][] bytesArray)
			throws NullFieldException {
	

		MessageDigest digest=getSHA1Digest();

		synchronized (digest) {

			for (byte[] bs : bytesArray) {

				Utility.assumeNotNull(bs);

				digest.update(bs);
			}
		}

		return digest.digest();

	}
	
	public static Cipher getAESCipher(){
		Cipher cipher=null;
		
		try {
			cipher=Cipher.getInstance(CIPER_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			log.error(CIPER_ALGORITHM +" not support ");
		} catch (NoSuchPaddingException e) {
			log.error(CIPER_ALGORITHM +" padding not supported");
		}
		
		return cipher;
	}
	
	public static MessageDigest getSHA1Digest(){
		
		MessageDigest digest = null;

		try {
			digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			log.error("SHA1 digest not support");
		}
		
		return digest;
	}

	public static byte[] digestNotNull(String[] strings)
			throws NullFieldException {
		
		MessageDigest digest = getSHA1Digest();

		synchronized (digest) {

			digest.reset();

			for (String string : strings) {

				Utility.assumeNotNull(string);

				digest.update(Utility.stringUTF8Bytes(string));
			}

		}

		return digest.digest();
	}

	public static boolean isValidSHA1(String sha1) {

		return (sha1 != null) && (sha1.length() == 40)
				&& SHA_1_PATTERN.matcher(sha1).matches();

	}

	public static boolean isValidSHA1(byte[] sha1) {

		if (sha1 == null || sha1.length != 20) {
			return false;
		}

		return true;
	}

	public static final Pattern SHA_1_PATTERN;

	static {
		// use lower case
		SHA_1_PATTERN = Pattern.compile("([0-9,a-f]){40}");
	}

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
					.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public static byte[] oneTimeDigest(String string) {
		
		MessageDigest digest = getSHA1Digest();

		Utility.assumeNotNull(string);

		synchronized (digest) {

			digest.reset();

			return digest.digest(Utility.stringUTF8Bytes(string));
		
		}

	}

	public static byte[] digestFile(File inputFile) throws IOException {

		if (!inputFile.exists() || !inputFile.canRead()) {
			throw new IllegalArgumentException("filename can not be read");
		}

		FileInputStream inputStream = new FileInputStream(inputFile);

		MappedByteBuffer byteBuffer = inputStream.getChannel().map(
				MapMode.READ_ONLY, 0, inputFile.length());

		MessageDigest digest = getSHA1Digest();

		synchronized (digest) {

			digest.reset();

			digest.update(byteBuffer);

			inputStream.close();

			return digest.digest();

		}

	}
}
