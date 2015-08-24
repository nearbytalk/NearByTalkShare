package org.nearbytalk.test.datastore;

import java.io.File;
import java.io.FileNotFoundException;

import org.nearbytalk.datastore.PasswordChanger;
import org.nearbytalk.datastore.SQLiteDataStore;
import org.nearbytalk.exception.DataStoreException;
import org.nearbytalk.runtime.DesktopPlatform;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.IPlatformAbstract;
import org.nearbytalk.test.misc.RandomUtility;
import org.nearbytalk.util.DigestUtility;

import junit.framework.TestCase;


public class PasswordChangerWithDesktopPlatformTest extends TestCase {

	IPlatformAbstract platformAbstract = Global.getInstance()
			.getPlatformAbstract();

	DesktopPlatform desktopPlatformAbstract = (DesktopPlatform) Global
			.getInstance().getPlatformAbstract();

	private SQLiteDataStore preCheck() throws DataStoreException {

		SQLiteDataStore ret = new SQLiteDataStore();
		ret.preCheck(Global.getInstance().getRawDataStorePassword());
		return ret;
	}

	public void testAutoCreatePasswordFile() throws DataStoreException,
			FileNotFoundException {

		File passwordFile = new File(
				desktopPlatformAbstract.getPasswordFilePath());
		
		String currentPassword=DesktopPlatform.readFile(passwordFile);
		
		if (!Global.DEFAULT_RAW_DATASTORE_PASSWORD.equals(currentPassword)) {
			
			throw new RuntimeException("not default password,should not run this test");
		}

		passwordFile.delete();

		preCheck();

		assertTrue(passwordFile.exists());

		String shouldBeContent = desktopPlatformAbstract
				.getRawDataStorePassword();

		String readFromPasswordFile = DesktopPlatform.readFile(passwordFile);

		assertEquals(shouldBeContent, readFromPasswordFile);

	}

	private static class TestChangeCallback implements
			PasswordChanger.PasswordChangeCallback {

		@Override
		public boolean checkFreeSpace(long l) {
			return true;
		}

		public boolean changeSuccess = false;

		@Override
		public void onPasswordChangeFinished(boolean success) {

			changeSuccess = success;
		}
	};

	public void testChangePassword() throws DataStoreException {

		SQLiteDataStore toTest = preCheck();

		String newPlainPassword = RandomUtility.nextString();

		String expectedRawPassword = DigestUtility
				.byteArrayToHexString(DigestUtility
						.oneTimeDigest(newPlainPassword));
		
		System.out.println("new password should be :"+expectedRawPassword);

		desktopPlatformAbstract.setToBeChangedPassword(expectedRawPassword);

		TestChangeCallback testChangeCallback = new TestChangeCallback();

		// should not sleep at all
		PasswordChanger changer = new PasswordChanger(toTest,
				testChangeCallback, newPlainPassword, 0);

		changer.doChangePassword();

		assertEquals(expectedRawPassword, Global.getInstance()
				.getRawDataStorePassword());

		toTest.preCheck(Global.getInstance().getRawDataStorePassword());

	}

}
