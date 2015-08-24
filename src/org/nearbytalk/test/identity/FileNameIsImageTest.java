package org.nearbytalk.test.identity;

import org.nearbytalk.identity.AbstractFileMessage;

import junit.framework.TestCase;


public class FileNameIsImageTest extends TestCase {

	public void testIsImageLowerCase() {

		String lowerCase[] = new String[] { "a.bmp", "b.jpeg", "c.gif", "d.jpg" };

		for (String string : lowerCase) {
			assertTrue(AbstractFileMessage.isImage(string));
		}

	}

	public void testIsImageMixCase() {

		String upperCase[] = new String[] { "x.BMp", "u.jPg", "c.GIF", "x.PnG" };

		for (String string : upperCase) {
			assertTrue(AbstractFileMessage.isImage(string));
		}

	}

	public void testIsNotImageFile() {
		String notImages[] = new String[] { "a.mp3", "badf", "bmp", "Jpeg",
				"gifC", "d.avi" };

		for (String string : notImages) {
			assertFalse(AbstractFileMessage.isImage(string));
		}
	}
}
