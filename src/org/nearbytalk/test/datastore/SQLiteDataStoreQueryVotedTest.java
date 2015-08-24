package org.nearbytalk.test.datastore;

import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.test.misc.RandomUtility;

import com.almworks.sqlite4java.SQLiteException;

public class SQLiteDataStoreQueryVotedTest extends SQLiteDataStoreShareTest {
	public void testUserNotVotedQueryAssumeExist() throws ParseException,
			SQLiteException, InterruptedException {

		checkUserNotVotedQuery(1, 1, 0, true);
	}

	public void testUserNotVotedQueryNotAssumeExist() throws ParseException,
			SQLiteException, InterruptedException {

		checkUserNotVotedQuery(1, 1, 0, false);

	}

	public void testUserVotedQueryAssumeExist() throws SQLiteException,
			BadReferenceException, ParseException, InterruptedException {

		checkUserVotedQuery(1, 1, 0, true);
	}

	public void testUserVotedQueryNotAssumeExist() throws SQLiteException,
			BadReferenceException, ParseException, InterruptedException {

		checkUserVotedQuery(1, 1, 0, false);
	}

	public void testNoneExistQueryVoted() throws SQLiteException {

		ClientUserInfo user = saveRandomUser();

		Set<String> set = new HashSet<String>();

		set.add(RandomUtility.randomIdBytesString());

		List<Boolean> result = dataStore.queryVoted(user, set, false);

		assertTrue(result.size() == 1);

		assertNull(result.get(0));
	}
}
