package org.nearbytalk.datastore;

import org.nearbytalk.runtime.Global;
import org.nearbytalk.util.DigestUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteException;

public class PasswordChanger {

	private Logger log = LoggerFactory.getLogger(PasswordChanger.class);

	private String newPasswordAsHash;

	private PasswordChangeCallback changeCallback;

	public static interface PasswordChangeCallback {

		boolean checkFreeSpace(long l);

		/**
		 * report state change
		 * 
		 * @param state
		 */
		void onPasswordChangeFinished(boolean success);

	}

	private int msTimeout;

	private SQLiteDataStore sqLiteDataStore;

	public PasswordChanger(SQLiteDataStore datastore,
			PasswordChangeCallback changeCallback, String newPasswordPlainText,
			int msTimeout) {

		this.sqLiteDataStore = datastore;
		this.changeCallback = changeCallback;

		assert changeCallback != null;

		this.msTimeout = msTimeout;

		if (newPasswordPlainText == null) {
			newPasswordAsHash = Global.DEFAULT_RAW_DATASTORE_PASSWORD;
		} else {
			newPasswordAsHash = DigestUtility
					.byteArrayToHexString(DigestUtility
							.oneTimeDigest(newPasswordPlainText));
		}

	}

	public void doChangePassword() {

		boolean blockingOk = sqLiteDataStore.blockOtherThread(msTimeout);

		if (!blockingOk) {

			changeCallback.onPasswordChangeFinished(false);
			return;

		}

		String exportSQL = String.format("PRAGMA rekey = '%s';",
				newPasswordAsHash);

		try {
			sqLiteDataStore.executeSQL(exportSQL);
			// manual close connection
			Global.getInstance().getPlatformAbstract().commitPasswordChangeNew(newPasswordAsHash);
			changeCallback.onPasswordChangeFinished(true);
			return;
		} catch (SQLiteException e) {
			changeCallback.onPasswordChangeFinished(false);
			return;
		} finally {

			sqLiteDataStore.unblockOtherThread();
		}

	}

}
