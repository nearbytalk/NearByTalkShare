package org.nearbytalk.test.misc;

import org.nearbytalk.util.DigestUtility;

import junit.framework.TestCase;


public class SHA1PatternTest extends TestCase {

	public void testIsValidSHA1() {

		String validSHA1[] = new String[] {
				"f99f705fa91b99a2782a49761f08c519868e7d98",
				"9263c05a128dfaa661eec4c0d327d0a31eacb00b",
				"1c296aec85fa22bba8d6403cff13ae6e086cdc97",
				"303dccc89633f3b1e2f3e2df88885fd03ab785c8" };

		for (String string : validSHA1) {
			assertTrue(DigestUtility.isValidSHA1(string));
		}

	}

	public void testInvalidSHA1() {

		assertFalse(DigestUtility.isValidSHA1("bba8d6403cff13ae6e086cdc97"));
		assertFalse(DigestUtility
				.isValidSHA1("1c296aec85fa22bba8d6403cff13ae6e086cdc97aaa"));
		assertFalse(DigestUtility
				.isValidSHA1("gggggggggggggggggggggggggggggggggggggggg"));

	}

}
