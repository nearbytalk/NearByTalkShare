package org.nearbytalk.test.datastore;

import java.text.ParseException;

import org.nearbytalk.exception.BadReferenceException;

import com.almworks.sqlite4java.SQLiteException;

public class SQLiteDataStoreQueryVotedPerformanceTest extends
		SQLiteDataStoreShareTest {
	public void testUserNotVotedQueryAssumeExist() throws ParseException,
			SQLiteException, InterruptedException {

		checkUserNotVotedQuery(100, 1, 0, true);
	}

	public void testUserNotVotedQueryNotAssumeExist() throws ParseException,
			SQLiteException, InterruptedException {

		checkUserNotVotedQuery(100, 1, 0, false);

	}

	public void testUserVotedQueryAssumeExist() throws SQLiteException,
			BadReferenceException, ParseException, InterruptedException {

		checkUserVotedQuery(100, 1, 0, true);
	}

	public void testUserVotedQueryNotAssumeExist() throws SQLiteException,
			BadReferenceException, ParseException, InterruptedException {

		checkUserVotedQuery(100, 1, 0, false);
	}
}
