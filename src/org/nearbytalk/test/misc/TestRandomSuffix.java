package org.nearbytalk.test.misc;

import org.nearbytalk.util.Utility;

import junit.framework.TestCase;


public class TestRandomSuffix extends TestCase {

	public void testRandomSuffix() {

		String randomSuffix = Utility.randomSuffix();
		
		assertTrue(randomSuffix.length()>1);
		assertTrue(randomSuffix.charAt(0)=='_');
		
		assertTrue(randomSuffix.matches("^_[a-zA-Z0-9]{5}$"));

	}
}
