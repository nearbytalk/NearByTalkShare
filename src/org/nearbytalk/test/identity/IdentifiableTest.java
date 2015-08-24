package org.nearbytalk.test.identity;

import org.nearbytalk.identity.BaseIdentifiable;
import org.nearbytalk.identity.Identifiable;

import junit.framework.TestCase;


public class IdentifiableTest extends TestCase {

	public void testInvalidSHA1Exception() {

		try {
			Identifiable temp = new BaseIdentifiable() {

				{
					String nullString=null;
					super.setIdBytes(nullString);
				}

				@Override
				public void digestId() {
					// TODO Auto-generated method stub

				}
			};

			fail("should throw");

		} catch (java.lang.AssertionError e) {

			assertTrue("here is all right", true);

			return;
		}

		fail("exception is not invalidSHA-1 exception");

	}
}
