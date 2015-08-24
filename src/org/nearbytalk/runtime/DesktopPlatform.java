package org.nearbytalk.runtime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Properties;
import java.util.Scanner;

import org.nearbytalk.util.DigestUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * load from predefine.properties
 * 
 */
public class DesktopPlatform implements IPlatformAbstract {

	static private Logger log = LoggerFactory.getLogger(DesktopPlatform.class);

	@Override
	public String getAppRootDirectory() {
		return rootDirectory;
	}

	private String rootDirectory = "I:\\me\\java\\NearByTalkShare\\WebRoot";

	public DesktopPlatform() {
		Properties prop = new Properties();
		InputStream in = getClass().getResourceAsStream("predefine.properties");

		if (in == null) {
			return;
		}

		try {
			prop.load(in);
			String readRootDirectory = prop.getProperty("rootDirectory");

			if (readRootDirectory == null) {
				return;
			}
			rootDirectory = readRootDirectory;

		} catch (Exception e) {
			rootDirectory = ".";
		} finally {
			try {
				in.close();
			} catch (IOException e) {
			}
		}
	}

	public final static String PASSWORD_FILE_NAME = "password";

	private static final String TEMP_PASSWORD_FILE_NAME = "temp_new_password";

	public String getPasswordFilePath() {

		return getAppRootDirectory() + File.separatorChar + PASSWORD_FILE_NAME;
	}

	public String getTempPasswordFilePath() {
		return getAppRootDirectory() + File.separatorChar
				+ TEMP_PASSWORD_FILE_NAME;
	}

	public static String readFile(File file) throws FileNotFoundException {

		Scanner scanner = new Scanner(file);

		String ret = scanner.next();

		scanner.close();

		return ret;

	}

	@Override
	public String getRawDataStorePassword() {

		File passwordFile = new File(getPasswordFilePath());

		File tempPasswordFile = new File(getTempPasswordFilePath());
		String mayBeNewPassword = null;

		try {

			// try temp password file first
			mayBeNewPassword = readFile(tempPasswordFile);

			if (!DigestUtility.isValidSHA1(mayBeNewPassword)) {

				// if tempPassword not write complete
				tempPasswordFile.delete();
			} else {

				passwordFile.delete();

				tempPasswordFile.renameTo(passwordFile);

				return mayBeNewPassword;
			}
		} catch (FileNotFoundException e1) {
			// nothing
		}

		if (mayBeNewPassword == null) {
			// check to_be_password file if any

			File toBePassword = new File(getToBeChangedPasswordPath());

			try {

				mayBeNewPassword = readFile(toBePassword);

				// not valid ,may be previous

				// has to_be_password,but may be useless, must check
				// EXPORT_TO_TEMP is complete

				// may previous action not write whole password, this is invalid
				if (DigestUtility.isValidSHA1(mayBeNewPassword)&& !passwordFile.exists()) {
					// previous run crash after commitState, but before
					// commitChangePassword

					passwordFile.delete();

					toBePassword.renameTo(passwordFile);

					return mayBeNewPassword;
				}
				// previous EXPORT_TO_TEMP not complete, this is a invalid file

				// clean up invalid to_be_password file
				toBePassword.delete();

			} catch (FileNotFoundException e) {
				log.debug("to_be_password file not found ");
			}
		}

		// no padding temp_password_file or to_be_password_file

		try {
			return readFile(passwordFile);
		} catch (FileNotFoundException e) {

			try {
				passwordFile.createNewFile();
				Writer writer = new BufferedWriter(new FileWriter(passwordFile));
				writer.write(Global.DEFAULT_RAW_DATASTORE_PASSWORD);
				writer.close();
			} catch (IOException e1) {
				log.debug("file not found:", e1);
			}

			return Global.DEFAULT_RAW_DATASTORE_PASSWORD;
		}

	}

	@Override
	public void commitPasswordChangeNew(String newRawPassword) {
		File tempPasswordFile = new File(getTempPasswordFilePath());

		tempPasswordFile.delete();

		Writer writer;

		try {
			writer = new BufferedWriter(new FileWriter(tempPasswordFile));
			writer.write(newRawPassword);
			writer.close();

			// tempPasswordFile write ok, can safely delete to_be_password file

			File toBeFile = new File(getToBeChangedPasswordPath());

			toBeFile.delete();

			File origFile = new File(getPasswordFilePath());
			origFile.delete();
			tempPasswordFile.renameTo(origFile);
		} catch (IOException e) {

			log.error("renaming back temp password file failed : ", e);
		}

	}

	public final static String TO_BE_NEW_PASS_FILE_NAME = "to_be_newpass";

	private String getToBeChangedPasswordPath() {
		return getAppRootDirectory() + File.separatorChar
				+ TO_BE_NEW_PASS_FILE_NAME;
	}

	/**
	 * for desktop platform test, this is necessary
	 * 
	 * if change process already commit EXPORT_TO_TEMP = true, but killed
	 * between commitPasswordChangeNew
	 * 
	 * next time PlatformAbstract must gives new password (but not be called
	 * commitPasswordChangeNew before)
	 * 
	 * so we need a way to automatic get new password. this function is for it
	 * call setToBeChangedPassword before actual repassword process, we can get
	 * new password even before commitPasswordChangeNew called . this is not
	 * problem for real mobile , since init password is input by user
	 * 
	 * 
	 * 
	 * 
	 * @param toBeChanged
	 */
	public void setToBeChangedPassword(String toBeChangedRawPassword) {

		File toBeChangedFile = new File(getToBeChangedPasswordPath());

		toBeChangedFile.delete();

		try {
			toBeChangedFile.createNewFile();
		} catch (IOException e1) {
			log.error("create new file {} failed", toBeChangedFile);
		}

		try {
			Writer writer = new BufferedWriter(new FileWriter(toBeChangedFile));

			writer.write(toBeChangedRawPassword);
			writer.close();
		} catch (IOException e) {
			log.error("write to to be changed password file failed : {}",
					toBeChangedFile);
		}
	}

}
