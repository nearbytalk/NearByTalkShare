package org.nearbytalk.test.misc;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

import org.nearbytalk.runtime.AtomicRename;
import org.nearbytalk.test.misc.ThreadTest.SingleTest;
import org.nearbytalk.util.DigestUtility;
import org.nearbytalk.util.Utility;

import junit.framework.TestCase;


public class AtomicRenameTest extends TestCase {

	private List<File> tempFiles = new ArrayList<File>();

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		File tempUploadDir = new File(Utility.makeupTempUploadPath());

		int threadNumber=50;
		
		for (int i = 0; i < threadNumber; i++) {

			String ext = null;

			if (i > threadNumber/2) {
				ext = "." + i;
			}

			File temp = File
					.createTempFile("atomic_rename", ext, tempUploadDir);

			tempFiles.add(temp);

		}

	}

	public void testAtomicRenaming() throws InterruptedException {
		byte[] tempIdBytes = DigestUtility.oneTimeDigest(RandomUtility
				.nextString());

		final String tempIdBytesString = DigestUtility
				.byteArrayToHexString(tempIdBytes);

		final ConcurrentSkipListSet<String> renamingResult = new ConcurrentSkipListSet<String>();

		Collection<Exception> errors= ThreadTest.run(tempFiles.size(), 1, 0, new SingleTest() {

			@Override
			public void singleTest(int threadIndex, int threadNumber)
					throws Exception {

				File thisFile = tempFiles.get(threadIndex);

				String basename = thisFile.getName();

				int suffixPos = basename.indexOf(".");

				String ext = null;
				if (suffixPos != -1) {
					ext = basename.substring(suffixPos);
				}

				renamingResult.add(AtomicRename.rename(
						tempFiles.get(threadIndex), ext, tempIdBytesString));
			}
		});
		
		assertTrue(errors.isEmpty());

		assertEquals(1, renamingResult.size());

		String finalDir = Utility.makeupUploadPath() + File.separatorChar
				+ AtomicRename.distributedKey(tempIdBytesString);

		String[] resultFile = new File(finalDir).list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(tempIdBytesString);
			}
		});

		assertEquals("same idBytes file should happen once in distrubted dir",
				1, resultFile.length);

		assertTrue(
				"final idBytes file should be the one return in AtomicRename",
				renamingResult.first().contains(resultFile[0]));
	}

	@Override
	protected void tearDown() throws Exception {
		// TODO Auto-generated method stub
		super.tearDown();
	}

}
