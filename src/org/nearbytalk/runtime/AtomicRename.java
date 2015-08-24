package org.nearbytalk.runtime;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.ConcurrentHashMap;

import org.nearbytalk.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * avoid same id-bytes file rename problem
 * 
 * different thread may try to do renaming with same idBytes (which also results
 * same final file) at same time. this class use a lockless two-level
 * concurrentHashMap to avoid this.
 * 
 * first level: key is 2-byte HEX string (idBytes[0,1], distributed dir name) ,
 * different key file goes to different second level check second level: key is
 * file idBytes,value is the renaming action at the time. use
 * <code>ConcurrentHashMap.putIfAbsent</code> result to check if current thread
 * can holding the idBytes key-value placeholder. if can-not hold: other thread
 * is doing renaming with same idBytes, use its value (we only use first upload
 * file extension ) if can hold it: no conflict renaming action is taken place
 * then check folder to assume no history file has same idBytes. and use it as
 * result if exists. or do final renaming then remove second level placeholder
 * 
 */
public class AtomicRename {

	// master key= distributed dir name (with naming convention 00 01 02 ~ ff )
	static private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> renamingLocks = new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();
	// value map key:idBytes in renaming
	// value map value:final renaming path
	// (00/01234567890123456789012345678090123456789.dat)
	// relative to upload root

	public final static int DISTRIBUTED_NUMBER = 0xFF;

	private static Logger log = LoggerFactory.getLogger(AtomicRename.class);

	static {
		for (int i = 0; i < DISTRIBUTED_NUMBER; i++) {

			// extending renamingLocks first
			String hex = String.format("%02x", i);

			renamingLocks.put(hex, new ConcurrentHashMap<String, String>());
		}

	}

	public static String distributedKey(String idBytes) {
		return idBytes.substring(0, 2);
	}

	/**
	 * renaming upload file in upload temp dir to final upload dir(in
	 * distributed subdir)
	 * 
	 * @param fileInUploadTemp
	 * @param extension
	 *            finalFile extension. if null,no extension is append
	 * @param idByteString
	 * @return
	 */
	public static String rename(File fileInUploadTemp, String extension,
			final String idByteString) {

		String key = distributedKey(idByteString);

		String finalBasename = idByteString
				+ (extension != null ? extension : "");

		log.trace("try to rename {} to {} ", fileInUploadTemp, finalBasename);

		String distributedRelativePath = key + File.separatorChar
				+ finalBasename;

		String prevRenaming = renamingLocks.get(key).putIfAbsent(idByteString,
				distributedRelativePath);

		if (prevRenaming != null) {

			log.trace("other thread is doing same renaming .use {} as result",
					prevRenaming);
			return prevRenaming;
		}

		// already holding the renaming lock
		// do renaming action

		String saveRoot = Utility.makeupUploadPath();

		File distributedDir = new File(saveRoot + File.separatorChar + key);

		distributedDir.mkdirs();

		String[] sameIdFiles = distributedDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(idByteString);
			}
		});

		// check if same id bytes file exists
		if (sameIdFiles.length > 0) {
			fileInUploadTemp.delete();
			log.trace(
					"distributed dir already has file {} with same idBytes. use as result",
					sameIdFiles[0]);
			return key + File.separatorChar + sameIdFiles[0];
		}

		// no idbytes file exists,and no other renaming action has same idbytes

		fileInUploadTemp.renameTo(new File(saveRoot + File.separatorChar
				+ distributedRelativePath));

		log.trace("do final renaming {}", distributedRelativePath);

		// release lock,at this point,renaming is finished
		renamingLocks.get(key).remove(idByteString);

		return distributedRelativePath;
	}
}
