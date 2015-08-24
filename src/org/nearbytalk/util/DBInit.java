package org.nearbytalk.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.nearbytalk.runtime.Global;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;

public class DBInit {

	private static String loadScript(String fileName)
			throws FileNotFoundException {

		String text = new Scanner(new File(fileName), "UTF-8").useDelimiter(
				"\\A").next();

		return text;

	}

	public static void main(String[] args) throws SQLiteException, FileNotFoundException {
		
		
		new File(Global.DB_FILENAME).delete();

		SQLiteConnection connection = new SQLiteConnection(new File(
				Global.DB_FILENAME));
		
		connection.open(true);
		
		String passwordSQL=String.format("PRAGMA KEY= '%s'", Global.DEFAULT_RAW_DATASTORE_PASSWORD);
		
		connection.exec(passwordSQL);
		
		System.out.println("exec create_table.sql");
		
		String createTableSQL=loadScript("create_table.sql");
		
		connection.exec(createTableSQL);

		System.out.println("exec reset_db.sql");
		
		String resetDBSQL=loadScript("reset_db.sql");
		
		connection.exec(resetDBSQL);

	}
}
