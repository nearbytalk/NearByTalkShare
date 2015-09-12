package org.nearbytalk.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.nearbytalk.datastore.SQLiteDataStore.MessageRecord;
import org.nearbytalk.exception.BadReferenceException;
import org.nearbytalk.exception.FileShareException;
import org.nearbytalk.exception.NullFieldException;
import org.nearbytalk.http.AbstractServlet;
import org.nearbytalk.http.EmbeddedHttpServer;
import org.nearbytalk.http.ErrorResponse;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.RefUniqueFile;
import org.nearbytalk.runtime.AtomicRename;
import org.nearbytalk.runtime.Global;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Utility {

	private static final Logger log = LoggerFactory.getLogger(Utility.class);

	private static final ThreadLocal<Random> THREAD_LOCAL_RANDOM = new ThreadLocal<Random>() {

		@Override
		protected Random initialValue() {
			return new Random();
		}

	};

	/**
	 * generate random suffix based on pattern '_[a-zA-Z0-9]{5}'
	 * 
	 * @return
	 */
	public static String randomSuffix() {

		char suffix[] = new char[6];

		for (int i = 1; i < suffix.length; ++i) {
			while (true) {
				int thisTry = THREAD_LOCAL_RANDOM.get().nextInt(256);

				if ((thisTry >= 'a' && thisTry <= 'z')
						|| (thisTry >= 'A' && thisTry <= 'Z')
						|| (thisTry >= '0' && thisTry <= '9')) {
					suffix[i] = (char) thisTry;
					break;
				}
			}
		}

		suffix[0] = '_';

		return new String(suffix);
	}

	public static void assumeNotNull(Object object) {

		if (object == null) {
			throw new NullFieldException();
		}

	}

	public static void assumeNotNullOrEmpty(String string) {

		assumeNotNull(string);

		if (string.isEmpty()) {
			throw new NullFieldException();

		}
	}

	public static <E extends Enum<E>> E valueOfEnum(E[] enums, String value) {

		for (E e : enums) {
			if (e.toString().equals(value)) {
				return e;
			}
		}

		return null;
	}

	public static <T extends AbstractServlet> String makeupAccessPath(
			Class<T> servletClass) {
		return EmbeddedHttpServer.CONTEXT_PREFIX + servletClass.getSimpleName();

	}

	public static byte[] stringUTF8Bytes(String string) {
		assumeNotNull(string);

		try {
			return string.getBytes(Global.UTF8_ENCODING);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// impossible
		return null;
	}

	public static String makeupDBPath() {
		return Global.getInstance().getAppRootDirectory()+ File.separatorChar
				+ Global.DB_FILENAME;
	}

	public static String makeupUploadPath() {
		return Global.getInstance().getAppRootDirectory() + File.separatorChar
				+ Global.UPLOAD_PATH;
	}

	public static String makeupTempUploadPath() {
		return Global.getInstance().getAppRootDirectory() + File.separatorChar
				+ Global.TEMP_UPLOAD_PATH;
	}

	public static boolean dateFuzzySame(Date lhs, Date rhs) {
		if (lhs == null && rhs == null) {
			return true;
		}

		if (lhs != null && rhs != null) {
			return Math.abs(lhs.getTime() - rhs.getTime()) / 1000 == 0;
		}

		// one is null
		return false;

	}

	public static InputStream getDecryptedStream(InputStream plainInputStream,byte[] decryptKey){
		
		Cipher cipher= DigestUtility.getAESCipher();
		
		SecretKeySpec keySpec=new SecretKeySpec(decryptKey, "AES");
		
		try {
			cipher.init(Cipher.DECRYPT_MODE, keySpec,
					new IvParameterSpec(decryptKey));
		} catch (InvalidKeyException e) {
			log.error("impossible {}",e);
		} catch (InvalidAlgorithmParameterException e) {
			log.error("impossible {}",e);
		}
		
		CipherInputStream ret=new CipherInputStream(plainInputStream, cipher);
		
		return ret;
	}
	/**
	 * write input stream as a RefCountFile (for http client upload use) if
	 * stream overflow sizeLimit,FileShareException is thrown
	 * 
	 * @param is
	 * @param sizeLimit
	 * @param fileName
	 *            uploaded file name,extension will be reserved for easy view.
	 *            ignored if null
	 * @return
	 * @throws IOException
	 * @throws FileShareException
	 */
	public static RefUniqueFile writeUploadStream(InputStream is,
			long sizeLimit, String fileName,byte[] encryptKey) throws IOException,
			FileShareException {

		File tempUploadRoot = new File(Utility.makeupTempUploadPath());

		MessageDigest digest = DigestUtility.getSHA1Digest();

		DigestInputStream digestInputStream = new DigestInputStream(is, digest);

		// create temp upload first,avoid folder not exists
		tempUploadRoot.mkdirs();

		String suffix = null;
		if (fileName != null) {

			int index = fileName.indexOf(".");
			if (index != -1) {
				suffix = fileName.substring(index);

			}
		}

		// first ,write stream to temp_upload dir

		File tempUpload = File.createTempFile("upload", suffix, tempUploadRoot);

		
		FileOutputStream fileOstream= new FileOutputStream(tempUpload);

		Cipher aesCipher=DigestUtility.getAESCipher();
		
		SecretKeySpec keySpec=new SecretKeySpec(encryptKey, "AES");
		
		try {
			aesCipher.init(Cipher.ENCRYPT_MODE,keySpec,
					new IvParameterSpec(encryptKey));
		} catch (InvalidKeyException e) {
			log.error("error: {}",e);
			//impossible
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			log.error("error: {}",e);
		}
		
		CipherOutputStream aesOstream=new CipherOutputStream(fileOstream, aesCipher);

		// copy from commons-io IOUtils.copy
		long count = 0;

		int n = 0;

		byte[] buffer = new byte[1024 * 4];

		boolean overflow = false;

		while (-1 != (n = digestInputStream.read(buffer))) {
			aesOstream.write(buffer, 0, n);
			count += n;
			if (sizeLimit < count) {
				overflow = true;
				break;
			}
		}

		digestInputStream.close();
		aesOstream.close();


		if (overflow) {
			log.error("file size overflow limit {},delete tempFile {}",
					sizeLimit, tempUpload);
			tempUpload.delete();

			throw new FileShareException(ErrorResponse.FILE_TOO_LARGE);
		}

		log.debug("file shared temp saved to {}", tempUpload);

		// then try to move temp file to distributed dir

		final String tempFileIdBytes = DigestUtility
				.byteArrayToHexString(digest.digest());

		// use distributed dir to avoid none-atomic renameTo problem
		// TODO currently we hard-encode distributed dir up to 256,
		// so we should limit concurrent write thread up to 256 too
		// (or two threads may rename to same dir)
		// powerful host storge may support more than

		String afterRename = AtomicRename.rename(tempUpload, suffix,
				tempFileIdBytes);

		return new RefUniqueFile(tempFileIdBytes, afterRename);
	}
	
	/**
	 * @param toConstruct
	 * @param goodRecord
	 * @param badRecord
	 * @param recursiveLimit
	 * @return
	 */
	private static  boolean recursiveConstruct(AbstractMessage toConstruct,
			final Map<String,MessageRecord> flat,
			Set<String> goodRecord,
			Set<String> badRecord,
			int recursiveLimit){
		
		

		if (recursiveLimit<=0) {
			/**message reference depth override {@code Global.REFERENCE_MAX_DEPTH}
			 * can not tell if this message and its recursive reference is complete.
			 * so do nothing to good/bad record
			 * just return false
			 */
			return false;
		}
		
		if (goodRecord.contains(toConstruct.getIdBytes())) {
			return true;
		}

		if (badRecord.contains(toConstruct.getIdBytes())) {
			//already checked
			return false;
		}
		
		String referenceIdBytes=toConstruct.getReferenceIdBytes();
		if (referenceIdBytes==null) {
			
			//no dependency ,OK
			goodRecord.add(toConstruct.getIdBytes());
			
			return true;
		}
		
		if (badRecord.contains(referenceIdBytes)) {
			//reference to incomplete message chain ,just treat as incomplete
			badRecord.add(toConstruct.getIdBytes());
			return false;
		}
		
		AbstractMessage reference=flat.get(referenceIdBytes).message;
				
		if (reference==null) {
			
			//dependency not exists
			badRecord.add(referenceIdBytes);
			
			return false;
		}
		
		//dependency exists, but not sure if it's dependency is complete
		if (recursiveConstruct(reference, flat, goodRecord, badRecord, recursiveLimit-1)) {
			
			try {
				toConstruct.setReferenceMessageLater(reference);
				goodRecord.add(toConstruct.getIdBytes());
			} catch (BadReferenceException e) {
				badRecord.add(toConstruct.getIdBytes());
				log.error("{} can not act as reference message of {}:{}",new Object[]{reference,toConstruct,e});
				
				return false;
			}
			
			return true;
		}
		
		//dependency not complete
		badRecord.add(toConstruct.getIdBytes());
		
		return false;
	}
	
	/**
	 * interface to hook into reconstruct. used for deciding if message should be in result list
	 *
	 * @param <T>
	 */
	public interface PreAddToResultAction<T extends AbstractMessage>{
		/**
		 * return true if preAddToResult should be in result list
		 * @param preAddToResult
		 * @return
		 */
		public boolean onPreAddToResult(T preAddToResult);
	}
	
	/**
	 * reconstruct flat message list to referenced list
	 * final list order is by createDate *Desc*, so toper level message always appears before bottomer ones
	 * @param flat
	 * @return
	 */
	public static <T extends AbstractMessage> List<T> reconstruct(final Class<T> clazz,
			final Map<String,MessageRecord> flat){
		
		List<T> ret=new ArrayList<T>();
		
		Set<String> badChain=new HashSet<String>();
		
		Set<String> goodChain=new HashSet<String>();
		
		for (MessageRecord record: flat.values()) {

			AbstractMessage thisOne=record.message;
			if ((thisOne.getClass() != clazz) && 
					(clazz!=AbstractMessage.class)) {
				//if pass AbstractMessage as clazz,any message is allowed 
				//to be top level message
				continue;
			}
			
			
			//TODO recursive depth and CHAT_BUILD needs semantic equals check
			if(record.top && recursiveConstruct(thisOne, flat, goodChain, 
					badChain, Global.REFERENCE_MAX_DEPTH)){
				
				//we can not assume result is actual T, just cast
				//if not ,that is caller's bug
				ret.add((T) record.message);
			}

		}
		//must manual sort (order by date is destroyed by flat map)
		
		Collections.sort(ret, new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				//we need to order desc
				return o2.getCreateDate().compareTo(o1.getCreateDate());
			}
		});
		
		return ret;
	}
	
	//TODO need test, but may be enough
	public final static String JSON_PATTERN=".*[\\{\\}\\[\\]:\\\"\\\\,/].*";
	
	/**
	 * 
	 * limit max topic length, to avoid super long cross index, maybe made dynamic 
	 */
	public static final int MAX_TOPIC_LENGTH=10;

	/**
	 * limit max topic number per message, to avoid bad message explode cross index table ,maybe made dynamic
	 */
	public static final int MAX_TOPIC_NUMBER=5;
	
	/**
	 * create list of cross index topic from string, every string will not longer than MAX_CROSS_INDEX_LENGTH
	 * list size will not more than MAX_CROSS_INDEX_NUMBER
	 * @param toParse
	 * @return list of cross index, never be null
	 */
	public static Set<String> parseTopics(String toParse){
		
		if(toParse==null){
			//some sub-class does not implement asPlainText
			return Collections.EMPTY_SET;
		}
		
		
		Set<String> ret=new HashSet<String>();
		
		int beginIndex=0;
		do {

			beginIndex=toParse.indexOf('#',beginIndex);
			if (beginIndex==-1) {
				//no begin mark
				return ret.isEmpty()?Collections.EMPTY_SET:ret;
			}


			//do not allow empty cross index string
			int endIndex=beginIndex+2;
			String maybeTopic=null;
			//use manual approach instead of index of 
			//for that only 10 length is supported at most

			
			while((endIndex<toParse.length()) && (endIndex<=(beginIndex+MAX_TOPIC_LENGTH+1))){

				if(toParse.charAt(endIndex)=='#'){
					maybeTopic=toParse.substring(beginIndex+1, endIndex);
					break;
				}

				++endIndex;
			}

			if (maybeTopic==null){
				//no end mark
				return ret.isEmpty()?Collections.EMPTY_SET:ret;

			}
			
				
			if(maybeTopic.matches(JSON_PATTERN)) {
				//skip one mark only, to preserve more mark
				beginIndex=endIndex;
			 }else{
				ret.add(maybeTopic);
				//skip end mark
				beginIndex=endIndex+1;
			 }
		}while(ret.size()<=MAX_TOPIC_NUMBER);
		
		return ret.isEmpty()?Collections.EMPTY_SET:ret;
	}
	
	/**
	 * convert idBytes to toString out, cut only first 6 char
	 * @param idBytes can be null
	 * @return
	 */
	public static String idBytesToString(String idBytes){
		return idBytes==null?null:idBytes.substring(0, 6);
	}
	
	public static class CustomVoteTopicResult{
		
		public String metaHeader;
		public String results;
		
	}
	
	static public final Pattern EMPTY_STRING_PATTERN;

	static {
		EMPTY_STRING_PATTERN= Pattern.compile("^[\\s]*$",
				Pattern.CASE_INSENSITIVE);
	}

	public static boolean isEmptyString(String toTest){
		return EMPTY_STRING_PATTERN.matcher(toTest).find();
	}
	
}
