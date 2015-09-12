package org.nearbytalk.datastore;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.nearbytalk.exception.DataStoreException;
import org.nearbytalk.exception.IncorrectPasswordException;
import org.nearbytalk.identity.AbstractMessage;
import org.nearbytalk.identity.AbstractMessage.MessageType;
import org.nearbytalk.identity.BaseUserInfo;
import org.nearbytalk.identity.ChatBuildMessage;
import org.nearbytalk.identity.ClientUserInfo;
import org.nearbytalk.identity.PlainTextMessage;
import org.nearbytalk.identity.RefUniqueFile;
import org.nearbytalk.identity.SpecialIdentifiable;
import org.nearbytalk.identity.VoteOfMeMessage;
import org.nearbytalk.identity.VoteTopicMessage;
import org.nearbytalk.query.PagedQuery.PagedInfo;
import org.nearbytalk.runtime.DateFormaterThreadInstance;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.util.DigestUtility;
import org.nearbytalk.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteBusyException;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteConstants;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

public class SQLiteDataStore implements IDataStore {

	private static Logger log = LoggerFactory.getLogger(SQLiteDataStore.class);
	
	public static int SQLITE_COMMON_OPEN_FLAGS=SQLiteConstants.SQLITE_OPEN_NOMUTEX | SQLiteConstants.SQLITE_OPEN_SHAREDCACHE;
	
	public static SQLParts[] judgeParts={
		new SQLParts("UPDATE ABSTRACT_MESSAGE SET AGREE_COUNTER=AGREE_COUNTER+1").fix(),
		new SQLParts("UPDATE ABSTRACT_MESSAGE SET DISAGREE_COUNTER=DISAGREE_COUNTER+1").fix()
	};
	
	public static SQLParts messageExistParts = new SQLParts(
			"SELECT 1 FROM ABSTRACT_MESSAGE WHERE ID_BYTES = ? "
			).fix();
	
	public static SQLParts randomFileKeyParts= new SQLParts("" +
			"INSERT INTO META_TABLE VALUES (\"FILE_KEY\",randomblob(16))").fix();
	
	public static SQLParts queryFileKeyParts = new SQLParts("" +
			"SELECT VALUE FROM META_TABLE WHERE KEY='FILE_KEY'").fix();
	
	public static SQLParts updateVoteTopicParts = new SQLParts(
			"UPDATE ABSTRACT_MESSAGE " +
			" SET PLAIN_TEXT = SUBSTR (PLAIN_TEXT,1,?) || ? " +
			" WHERE ID_BYTES = ? AND MESSAGE_TYPE_ID = 2 "
			).fix();
	 
	public static SQLParts saveMessageNoRefParts = new SQLParts(
			"INSERT OR IGNORE INTO ABSTRACT_MESSAGE "
					+ "(ID_BYTES,PLAIN_TEXT,SENDER_ID_BYTES,MESSAGE_TYPE_ID,CREATE_DATE) VALUES(?,?,?,?,?)")
			.fix();

	public static SQLParts saveMessageWithRefParts = new SQLParts(
			"INSERT OR IGNORE INTO ABSTRACT_MESSAGE"
					+ "(ID_BYTES,PLAIN_TEXT,SENDER_ID_BYTES,MESSAGE_TYPE_ID,CREATE_DATE,REFERENCE_ID_BYTES,REFERENCE_DEPTH) "
					+ "SELECT ? AS ID_BYTES,"
					+ "? AS PLAIN_TEXT,"
					+ "? AS SENDER_ID_BYTES,"
					+ "? AS MESSAGE_TYPE_ID, "
					+ "? AS CREATE_DATE, "
					+ "? AS REFERENCE_ID_BYTES, "					
					+ "(SELECT ABSTRACT_MESSAGE.REFERENCE_DEPTH+1 FROM ABSTRACT_MESSAGE WHERE ID_BYTES=?) AS REFERENCE_DEPTH ")
			.fix();

	public static SQLParts saveOrUpdateUserInfoParts = new SQLParts(
			"INSERT OR REPLACE INTO CLIENT_USER_INFO (ID_BYTES,USER_NAME,DESCRIPTION,RANDOM_USER)"
					+ "SELECT ? AS ID_BYTES, " 
					+ " ? AS USER_NAME, " 
					+ " ? AS DESCRIPTION, " 
					+ "( SELECT CASE " 
					+ "(SELECT EXISTS " 
					+ "(SELECT 1 FROM CLIENT_USER_INFO WHERE ID_BYTES = ? )" 
					+ ") WHEN 1 THEN 1 ELSE ?  END ) AS RANDOM_USER").fix();

	public static SQLParts ftsSearchUserParts = new SQLParts(
			"SELECT CLIENT_USER_INFO.* FROM CLIENT_USER_INFO JOIN"
					+ "(SELECT docid FROM " + "FTS_CLIENT_USER_INFO "
					+ "WHERE FTS_CLIENT_USER_INFO  MATCH ? " + "LIMIT ?" + "OFFSET ?)"
					+ "AS fts WHERE fts.docid = CLIENT_USER_INFO.rowid").fix();
	
	public static SQLParts partSearchUserNameParts=new SQLParts(
			" SELECT client_user_info.* " +
			" FROM client_user_info " +
			" WHERE user_name like ? " +
			" LIMIT ? OFFSET ?").fix();
	
	public static SQLParts extractlySearchUserNameParts=new SQLParts(
			"SELECT CLIENT_USER_INFO.* " +
			"FROM CLIENT_USER_INFO " +
			"WHERE USER_NAME = ? " +
			"LIMIT ? OFFSET ?").fix();
	
	private static final String MESSAGE_READ_COLUMN=
			"MESSAGE.ID_BYTES, \n" +
			"MESSAGE.PLAIN_TEXT, \n" +
			"MESSAGE.SENDER_ID_BYTES, \n" +
			"MESSAGE.MESSAGE_TYPE_ID, \n" +
			"MESSAGE.CREATE_DATE, \n" +
			"MESSAGE.REFERENCE_ID_BYTES, \n" +
			"MESSAGE.REFERENCE_DEPTH, \n" +
			"MESSAGE.REFERENCED_COUNTER, \n" +
			"MESSAGE.AGREE_COUNTER, \n" +
			"MESSAGE.DISAGREE_COUNTER";
	
	private static final String USER_MESSAGE_JOIN_FLAT_SQL=
			" SELECT user.id_bytes,user.user_name, result.* \n" +
			" FROM client_user_info AS user \n" +
			" JOIN " +
			" (SELECT " +
			MESSAGE_READ_COLUMN +
			" , flat.top_most " + 
			" FROM abstract_message AS message, flat \n" +
			" WHERE message.id_bytes=flat.id ) AS result \n" +
			" WHERE user.id_bytes = result.sender_id_bytes \n";

	private static final String RECURSIVE_FTS_SQL=
			" WITH RECURSIVE " +
			" flat(id,ref_id,top_most) AS " +
			" (" +
			"  SELECT * FROM (SELECT orig.id_bytes,orig.reference_id_bytes,1 " +
			" FROM abstract_message AS orig " +
			" JOIN " +
			" (SELECT rowid FROM fts_abstract_message " +
			" WHERE fts_abstract_message %s " +
			" ) AS fts WHERE fts.rowid=orig.rowid " +
			" %s ) " +
			" UNION ALL" +
			" SELECT id_bytes,reference_id_bytes,0 FROM abstract_message,flat " +
			" WHERE abstract_message.id_bytes = flat.ref_id )" +
			USER_MESSAGE_JOIN_FLAT_SQL;


	public static SQLParts ftsQueryTypedMessageParts= new SQLParts(
			String.format(RECURSIVE_FTS_SQL, " MATCH ? "," AND orig.message_type_id = ?  LIMIT ? OFFSET ? ")).fix();

	public static SQLParts ftsQueryMessageParts = new SQLParts(
			String.format(RECURSIVE_FTS_SQL," MATCH ? LIMIT ? OFFSET ? "," ")).fix();
	
	private static final String RECURSIVE_TOPIC_SQL=
			" WITH RECURSIVE " +
			" flat(id,ref_id,top_most) AS " +
			" (SELECT orig.id_bytes,orig.reference_id_bytes,1 " +
			" FROM abstract_message AS orig " +
			" JOIN " +
			" (SELECT id_bytes FROM fts_topic " +
			" WHERE topic %s ? LIMIT ? OFFSET ? " +
			" ) AS fts WHERE fts.id_bytes=orig.id_bytes" +
			" UNION " +
			" SELECT id_bytes,reference_id_bytes,0 FROM abstract_message,flat " +
			" WHERE abstract_message.id_bytes = flat.ref_id )" +
			USER_MESSAGE_JOIN_FLAT_SQL;
	
	public static SQLParts queryTopicFuzzyParts = new SQLParts(
			String.format(RECURSIVE_TOPIC_SQL, "MATCH")).fix();
	
	public static SQLParts queryTopicExactlyParts = new SQLParts(
			String.format(RECURSIVE_TOPIC_SQL, "=")).fix();

	public static SQLParts queryNewestUserParts = new SQLParts(
			"SELECT CLIENT_USER_INFO.* " + "FROM CLIENT_USER_INFO "
					+ "ORDER BY CREATE_DATE DESC " + "LIMIT ? OFFSET ? ").fix();

	public static SQLParts queryNewestRefCountFileParts = new SQLParts(
			"SELECT USER.USER_NAME,result.* " +
					" FROM CLIENT_USER_INFO AS USER JOIN "
					+ "(SELECT " 
					+ MESSAGE_READ_COLUMN
					+ " FROM ABSTRACT_MESSAGE AS MESSAGE "
					+ " WHERE MESSAGE_TYPE_ID='1' " + " ORDER BY CREATE_DATE DESC "
					+ " LIMIT ? OFFSET ? ) AS result "
					+ " WHERE USER.ID_BYTES=result.SENDER_ID_BYTES").fix();


	private final static String RECURSIVE_MAIN_TABLE_QUERY=" WITH RECURSIVE \n"
			+ " flat(id,ref_id,top_most) AS \n"
			+ " (SELECT * FROM " +
			"	( SELECT id_bytes,reference_id_bytes,1 \n"
			+ " FROM abstract_message \n"
			// REF_UNIQUE is treated as nested message, should not as top level
			+ " WHERE message_type_id != 1 \n" 
			+ " %s )  "
			+ " UNION "
			+ " SELECT id_bytes,reference_id_bytes,0 FROM abstract_message,flat \n"
			+ " WHERE abstract_message.id_bytes = flat.ref_id  ) \n" 
			+  USER_MESSAGE_JOIN_FLAT_SQL;

	public static SQLParts queryDetailParts = new SQLParts(
			String.format(RECURSIVE_MAIN_TABLE_QUERY, " AND ID_BYTES = ? ")).fix();

	public static SQLParts queryNewestParts = new SQLParts(
			// REF_UNIQUE is treated as nested message, should not as top level
			String.format(RECURSIVE_MAIN_TABLE_QUERY, 
			" ORDER BY create_date DESC LIMIT ? OFFSET ? ")).fix();
	
	public static SQLParts invalidMessageReferenceParts=new SQLParts(
			"UPDATE ABSTRACT_MESSAGE SET REFERENCE_ID_BYTES='" +
						SpecialIdentifiable.DELETED_MESSAGE.getIdBytes()+
			"' WHERE SENDER_ID_BYTES=? AND REFERENCE_ID_BYTES=?  ").fix();
	
	public static SQLParts deleteMessageCascadeParts=new SQLParts("" +
			"DELETE FROM ABSTRACT_MESSAGE WHERE SENDER_ID_BYTES=? AND ID_BYTES=? ").fix();

	public static SQLParts beginTrasactionParts = new SQLParts("BEGIN").fix();

	public static SQLParts commitTrasactionParts = new SQLParts("COMMIT").fix();

	public static SQLParts rollbackTransactionParts = new SQLParts("ROLLBACK").fix();
	
	public static SQLParts saveTopicsParts= new SQLParts("INSERT OR IGNORE " +
			"INTO FTS_TOPIC" +
			"(ID_BYTES,TOPIC) VALUES (?, ?)").fix();
	
	private ReadWriteLock rwLock=new ReentrantReadWriteLock();

	public static class PackedVar {
		public SQLiteConnection connection;

		public PackedVar() throws SQLiteException {

			connection = new SQLiteConnection(new File(Utility.makeupDBPath()));

			try {
				connection.openV2(SQLITE_COMMON_OPEN_FLAGS | SQLiteConstants.SQLITE_OPEN_READWRITE);

				log.debug("creating SQLite connection {}",connection);
				
				connection.exec("PRAGMA key = '"
						+ Global.getInstance().getRawDataStorePassword()+ "'");
				
				connection.setBusyTimeout(1000);
				//avoid block ,we don't care read inconsist too much
				connection.exec("PRAGMA read_uncommitted=TRUE");
				// enforce foreign key
				connection.exec("PRAGMA foreign_keys = ON");
				connection.exec("PRAGMA journal_mode = WAL");	
				//must use 3.7.17 with mmap support
				connection.exec("PRAGMA mmap_size = 33554432");				
			} catch (SQLiteException ex) {
				log.error("connection create failed {}", ex);
				
				if(connection!=null){
					connection.dispose();
				}
				throw ex;
			}

		}
		
		public void dispose(){
			
			if(!connection.isDisposed()){
				log.debug("dispose SQLite connection,{}",connection);
				connection.dispose();
			}
		}
	}

	private class PackedVarThreadLocal {
		
		
		
		private PackedVar get() throws SQLiteException {
			return get(true);
		}

		/**
		 * this function will only be called in different thread, and every
		 * thread R/W (thread local) /(thread safe) /(stack variable) so its
		 * safe
		 * 
		 * @return thread local PackedVar if created ,or create one when
		 *         create=true. null when create is false
		 * @throws SQLiteException
		 */
		private PackedVar get(boolean lockRead) throws SQLiteException {

			PackedVar ret = nullPackedVars.get();
			
			if (ret==null) {

				if(lockRead){
					threadPreCheck();
				}

				ret=new PackedVar();
				nullPackedVars.set(ret);

				return ret;
			}else{
				//ret != null, read lock already took
				return ret;
			}
		}

		private ThreadLocal<PackedVar> nullPackedVars = new ThreadLocal<PackedVar>();
		
		
		public void remove(boolean unlockRead){
			PackedVar toDispose=nullPackedVars.get();
			if(toDispose!=null){
				
				toDispose.dispose();
				nullPackedVars.remove();
				
				if (unlockRead) {
					rwLock.readLock().unlock();
				}
			}
		}
	}
	
	private PackedVarThreadLocal packedVarThreadLocal=new PackedVarThreadLocal();
	

	public SQLiteDataStore() {

		clazzMapMessageType.put(PlainTextMessage.class, PlainTextMessage.MESSAGE_TYPE);
		clazzMapMessageType.put(RefUniqueFile.class, RefUniqueFile.MESSAGE_TYPE);
		clazzMapMessageType.put(ChatBuildMessage.class, ChatBuildMessage.MESSAGE_TYPE);
		clazzMapMessageType.put(VoteTopicMessage.class, VoteTopicMessage.MESSAGE_TYPE);
		clazzMapMessageType.put(VoteOfMeMessage.class, VoteOfMeMessage.MESSAGE_TYPE);
		//PWD_CRYPTO
		//SAME_DOING

	}
	
	/**
	 * 
	 * increase runningThreadNumber if no other thread blocking. 
	 * clear thread connection if other thread blocking (and restore runningThreadNumber)
	 * 
	 * @throws DataStoreException
	 * @throws SQLiteBusyException
	 */
	private void threadPreCheck() throws SQLiteBusyException{

		if(rwLock.readLock().tryLock()){
			//no blocking ,ok 
			return;
		}

		throw new SQLiteBusyException(SQLiteConstants.SQLITE_BUSY, "other thread blocking");
	}

	@Override
	public void preCheck(String tryRawPassword ) throws DataStoreException {
		
		
		//if current thread has other connection, disable it
		//since SQLiteConnection is wrapped in this class
		//outer can not hold thread local connection
		//so its safe to recycle
		
		threadRecycle();
		
		try {
			threadPreCheck();
		} catch (SQLiteBusyException e) {
			throw new DataStoreException(e);
		}
			
		//other thread is blocking
		//increase after connection created,if exception thrown, not increase

		
		SQLiteConnection connection = null;

		try {
			
			//preCheck didn't use thread local connection nor set it
			//since SQLiteDataStore assume every thread local connection is valid
			//and created by global password . but we need to follow running thread
			//restriction, that is : if blocking thread entered, do not preCheck.
			//if not so,password change can be finished after preCheck, makes preCheck
			//caller confused.
			connection=new SQLiteConnection(new File(Utility.makeupDBPath()));
			connection.openV2(SQLITE_COMMON_OPEN_FLAGS | SQLiteConstants.SQLITE_OPEN_READONLY);
			

				connection.exec("PRAGMA key = '"
						+ tryRawPassword + "'");

				connection.exec("PRAGMA read_uncommitted=TRUE");

				connection.exec("PRAGMA journal_mode = WAL");	
				//must use 3.7.17 with mmap support
				connection.exec("PRAGMA mmap_size = 33554432");				

				connection.exec("SELECT COUNT(*) FROM CLIENT_USER_INFO");

			} catch (SQLiteException ex) {
				log.error("connection create failed {}", ex);
				
			
				throw new IncorrectPasswordException();
				
			} finally{
				//must manual
				if(connection!=null){
					connection.dispose();
				}
				rwLock.readLock().unlock();
			}

	}
	

	private boolean saveMessageTopics(AbstractMessage message) throws SQLiteException{
		
		Set<String> crossIndexes=message.getTopics();
		
		//cross index should be parsed out of saving thread
		assert crossIndexes!=null;
		
		if(crossIndexes.isEmpty()){
			return true;
		}
		
		
		SQLiteConnection connection = packedVarThreadLocal.get().connection;
		
			
		for (String crossIndex: crossIndexes) {
		
			SQLiteStatement stmt=connection.prepare(saveTopicsParts);
			
			stmt.bind(1, message.getIdBytes());
			stmt.bind(2, crossIndex);
			
			stmt.stepThrough();
			
			safeDispose(stmt);
			
		}
		
		//TODO check result
		
		
		return true;
	}

	/**
	 * save message with auto commit
	 * 
	 * @param message
	 * @return
	 * @throws SQLiteBusyException
	 */
	private boolean saveMessageSingleInternal(AbstractMessage message)
			throws SQLiteBusyException {

		String anyReferenceIdBytes = message.anyReferenceIdBytes();

		SQLiteStatement stmt = null;

		try {

			SQLiteConnection connection = packedVarThreadLocal.get().connection;

			stmt = connection
					.prepare(anyReferenceIdBytes == null ? saveMessageNoRefParts
							: saveMessageWithRefParts);

			int index=0;
			
			stmt.bind(++index, message.getIdBytes());
			stmt.bind(++index, message.asPlainText());
			stmt.bind(++index, message.getSender().getIdBytes());
			stmt.bind(++index, message.getMessageType().getIndex());
			stmt.bind(
					++index,
					DateFormaterThreadInstance.get().format(
							message.getCreateDate()));

			if (anyReferenceIdBytes != null) {

				stmt.bind(++index, anyReferenceIdBytes);
				stmt.bind(++index, anyReferenceIdBytes);

			}

			stmt.stepThrough();
			
			saveMessageTopics(message);

			log.trace("saveMessage succesed :{}",message);
			return connection.getChanges() == 1;
		} catch (SQLiteBusyException e) {
			throw e;
		} catch (SQLiteException ex) {

			log.error("save message failed: {}" ,ex);

			return false;
		} finally {
			safeDispose(stmt);
		}

	}

	private void execSql(SQLParts parts) throws SQLiteBusyException {

		SQLiteStatement stmt = null;
		try {

			stmt = packedVarThreadLocal.get().connection.prepare(parts);

			stmt.stepThrough();
		} catch (SQLiteBusyException e1) {
			throw e1;
		} catch (SQLiteException e1) {
			log.error("begin transaction failed", e1);
			return;
		} finally {
			safeDispose(stmt);
		}
	}

	private void beginTransaction() throws SQLiteBusyException {

		execSql(beginTrasactionParts);
	}

	private void endTransaction() throws SQLiteBusyException {

		execSql(commitTrasactionParts);
	}

	private void rollbackTransaction() throws SQLiteBusyException {
		execSql(rollbackTransactionParts);
	}

	private boolean saveMessageInternal(AbstractMessage message)
			throws SQLiteBusyException {

		AbstractMessage referencedMessage = message.getReferenceMessage();

		if (referencedMessage instanceof RefUniqueFile) {

			RefUniqueFile refCountFile= (RefUniqueFile) referencedMessage;

			//if saveMessageSingleInternal return false
			//when save refCountFile,this is a duplicate file record
			//(impossible with AtomicRename)
			//not a save error.
			saveMessageSingleInternal(refCountFile);

		}else{
			//if reference message is other type,this reference message should be saved 
			//first
		}

		return saveMessageSingleInternal(message);
	}

	@Override
	public boolean saveMessage(AbstractMessage message) throws SQLiteBusyException {

		boolean saveOk=false;
		try {

			beginTransaction();

			saveOk = saveMessageInternal(message);

			endTransaction();

			return saveOk;
		} catch (SQLiteException ex) {
			rollbackTransaction();
			
			if (ex instanceof SQLiteBusyException) {
				throw (SQLiteBusyException) ex;
			}

			return false;
		} finally{
			if (!saveOk) {
				//clean file share
				if (message.getReferenceMessage() instanceof RefUniqueFile) {

					RefUniqueFile toDelete=(RefUniqueFile)message.getReferenceMessage();
					log.debug("save failed,clean temp upload file {}",toDelete);
					toDelete.deleteFile();
				}	
			}
		}
	}
	
	static public class ClientUserInfoColumnIndex{
		
		static final int ID_BYTES=0;
		static final int USER_NAME=1;
		static final int CREATE_DATE=2;
		static final int TALK_NUMBER=3;
		static final int DESCRIPTION=4;
		static final int IS_RANDOM=5;
		
		static final int ID_BYTES_AND_NAME_ONLY_NUMBER=2;
	}
	
	/**
	 * from db to ClientUserInfo
	 * 
	 * @param stmt
	 * @param beginIndex
	 * @return
	 * @throws SQLiteException
	 * @throws ParseException
	 */
	static public BaseUserInfo parseClientUserInfo(SQLiteStatement stmt,boolean fullParse)
			throws SQLiteException, ParseException {
		
		if (!fullParse) {
			return new BaseUserInfo(stmt.columnString(0),stmt.columnString(1));
			
		}

		String idBytes = stmt.columnString(0);
		String userName = stmt.columnString(1);
		Date createDate = DateFormaterThreadInstance.get().parse(
				stmt.columnString(2));
		int talkNumber = stmt.columnInt(3);
		String description = stmt.columnString(4);
		boolean isRandomUser = (stmt.columnInt(5) != 0);

		ClientUserInfo ret = new ClientUserInfo(userName, idBytes);
		ret.setCreateDate(createDate);
		ret.setTalkNumber(talkNumber);
		ret.setDescription(description);
		ret.setRandomUser(isRandomUser);
		return ret;
	}
	
	public static class AbstractMessageColumnIndex{
		public static final int ID_BYTES=0;
		public static final int TEXT=1;
		public static final int SENDER_ID_BYTES=2;
		public static final int MESSAGE_TYPE_ID=3;
		public static final int CREATE_DATE=4;
		public static final int REFERENCE_ID_BYTES=5;
		public static final int REFERENCE_DEPTH=6;
		public static final int REFERENCED_COUNTER=7;
		public static final int AGREE_COUNTER=8;
		public static final int DISAGREE_COUNTER=9;
	}


	
	public static class MessageRecord {
		
		public AbstractMessage message;
		public boolean top;
	}
	/**
	 * create message by DB record,based on MESSAGE_TYPE_ID
	 * @param clazz
	 * @param stmt
	 * @return
	 * @throws SQLiteException
	 * @throws ParseException
	 */
	static public  MessageRecord parseAbstractMessage(
			SQLiteStatement stmt,boolean parseTopMost) throws SQLiteException,
			ParseException {

		BaseUserInfo sender = parseClientUserInfo(stmt,false);

		String idBytes = stmt.columnString(ClientUserInfoColumnIndex.ID_BYTES_AND_NAME_ONLY_NUMBER+
				AbstractMessageColumnIndex.ID_BYTES);

		String text = stmt.columnString(ClientUserInfoColumnIndex.ID_BYTES_AND_NAME_ONLY_NUMBER+ 
				AbstractMessageColumnIndex.TEXT);

		// String
		// senderIdBytesSkip=stmt.columnString(CLIENT_USER_INFO_PARSE_COLUMNS+2);

		MessageType messageType = MessageType.fromIndex(stmt
				.columnInt(ClientUserInfoColumnIndex.ID_BYTES_AND_NAME_ONLY_NUMBER+ 
						AbstractMessageColumnIndex.MESSAGE_TYPE_ID));

		Date createDate = DateFormaterThreadInstance.get().parse(
				stmt.columnString(ClientUserInfoColumnIndex.ID_BYTES_AND_NAME_ONLY_NUMBER+ 
						AbstractMessageColumnIndex.CREATE_DATE));

		String referenceIdBytes = stmt
				.columnString(ClientUserInfoColumnIndex.ID_BYTES_AND_NAME_ONLY_NUMBER+ 
						AbstractMessageColumnIndex.REFERENCE_ID_BYTES);

		int referenceDepth = stmt.columnInt(ClientUserInfoColumnIndex.ID_BYTES_AND_NAME_ONLY_NUMBER+ 
				AbstractMessageColumnIndex.REFERENCE_DEPTH);
		
		int referencedCounter=stmt.columnInt(ClientUserInfoColumnIndex.ID_BYTES_AND_NAME_ONLY_NUMBER
				+AbstractMessageColumnIndex.REFERENCED_COUNTER);
		
		int agreeCounter=stmt.columnInt(ClientUserInfoColumnIndex.ID_BYTES_AND_NAME_ONLY_NUMBER
				+AbstractMessageColumnIndex.AGREE_COUNTER);
		
		int disagreeCounter=stmt.columnInt(ClientUserInfoColumnIndex.ID_BYTES_AND_NAME_ONLY_NUMBER
				+AbstractMessageColumnIndex.DISAGREE_COUNTER);
				
		MessageRecord ret=new MessageRecord();

		if (parseTopMost) {
			ret.top=(stmt.columnInt(ClientUserInfoColumnIndex.ID_BYTES_AND_NAME_ONLY_NUMBER+
					AbstractMessageColumnIndex.DISAGREE_COUNTER+1)==1);
		}

		switch (messageType) {
		case PLAIN_TEXT: {
			ret.message = new PlainTextMessage(idBytes, sender, text, 
					createDate, referenceIdBytes,referenceDepth,referencedCounter,agreeCounter,disagreeCounter);
			break;
		}

		case REF_UNIQUE: {
			ret.message = new RefUniqueFile(idBytes, text);
			break;
		}
		case VOTE_TOPIC:{
			ret.message=new VoteTopicMessage(idBytes, sender, text, createDate,
					referencedCounter,agreeCounter,disagreeCounter);
			break;
		}
		case VOTE_OF_ME:{
			ret.message=new VoteOfMeMessage(idBytes, sender, text, createDate,referenceIdBytes,
					referencedCounter,agreeCounter,disagreeCounter);
			break;
		}
		case CHAT_BUILD:{
			ret.message = new ChatBuildMessage(idBytes, sender, text, createDate,referenceIdBytes,referenceDepth,
					referencedCounter,agreeCounter,disagreeCounter);
			break;
		} 
		case PWD_CRYPTO:{
			//TODO
		}
		case SAME_DOING:{
			//TODO
		}
		}

		return ret;

	}

	private HashMap<Class<? extends AbstractMessage>, MessageType> clazzMapMessageType = new HashMap<Class<? extends AbstractMessage>, MessageType>();

	private static  interface StepCallback{
		
		void onStep(SQLiteStatement stmt) throws SQLiteException,ParseException;
	}
	
	private static abstract class  MessageOnlyStepCallback <T extends AbstractMessage> implements StepCallback{
		
		abstract void onStep(T one);

		@Override
		public void onStep(SQLiteStatement stmt) throws SQLiteException, ParseException{
			onStep((T)parseAbstractMessage(stmt,false).message);
		}
	}
	
	public static abstract class RecursiveMessageStepCallback implements StepCallback{
		abstract void onStep(MessageRecord record);

		@Override
		public void onStep(SQLiteStatement stmt) throws SQLiteException, ParseException{
			onStep(parseAbstractMessage(stmt, true));
		}
	}
	
	
	
	private static interface BindCallback{
		void onBind(SQLiteStatement stmt) throws SQLiteException;
	}
	
	private void queryInternal(SQLParts sqlParts, BindCallback bindCallback,StepCallback callback)
			throws ParseException, SQLiteBusyException {
		SQLiteStatement stmt = null;
		try {

			stmt = packedVarThreadLocal.get().connection
					.prepare(sqlParts);
			
			bindCallback.onBind(stmt);

			
			while (stmt.step()) {
				callback.onStep(stmt);
			}

		} catch (SQLiteBusyException e) {
			throw e;
		} catch (SQLiteException e) {
			log.error("Query newest message failed {}", e);
		} finally {
			safeDispose(stmt);
		}
	}
	
		
	@Override
	public <T extends AbstractMessage> List<T> queryNewest(Class<T> clazz,
			final PagedInfo pagedInfo) throws SQLiteBusyException, ParseException {


		if (clazz == RefUniqueFile.class) {
			final List<T> ret=new LinkedList<T>();
			//only flat result
			queryInternal(queryNewestRefCountFileParts, new BindCallback() {

				@Override
				public void onBind(SQLiteStatement stmt) throws SQLiteException {
					stmt.bind(1, pagedInfo.size);
					stmt.bind(2, pagedInfo.beginOffset());
				}
			}, new MessageOnlyStepCallback<T>() {
				@Override
				public void onStep(T one) {
					ret.add(one);
				}
			});
			
			return ret;
		}
		
		final HashMap<String,MessageRecord> idMapMessage = new HashMap<String,MessageRecord>();
		
		queryInternal(queryNewestParts, new BindCallback() {
			@Override
			public void onBind(SQLiteStatement stmt) throws SQLiteException {
				stmt.bind(1, pagedInfo.size);
				stmt.bind(2, pagedInfo.beginOffset());				
			}
		}, new RecursiveMessageStepCallback() {

			@Override
			public void onStep(MessageRecord one) {
				if(!idMapMessage.containsKey(one.message.getIdBytes())){
					idMapMessage.put(one.message.getIdBytes(), one);
				}
				
			}
		});
		
		return Utility.reconstruct(clazz, idMapMessage);
	

	}

	@Override
	public List<AbstractMessage> queryHotest(PagedInfo pagedInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean delete(BaseUserInfo userInfo) {
		// TODO Auto-generated method stub
		return false;
	}
	

	@Override
	public boolean delete(BaseUserInfo user,String idBytes, boolean cascade) {
		
		SQLiteStatement stmt=null;
		
		SQLiteStatement stmt1=null;
		
	
		try {		
			
			SQLiteConnection conn=packedVarThreadLocal.get().connection;
			if (cascade) {
				stmt=conn.prepare(deleteMessageCascadeParts);
				stmt.bind(1, user.getIdBytes());
				stmt.bind(2, idBytes);
				stmt.stepThrough(); 
				return conn.getChanges()>0;
			}
	
			
			
			stmt=conn.prepare(invalidMessageReferenceParts);
			stmt.bind(1, user.getIdBytes());
			stmt.bind(2, idBytes);
			stmt.stepThrough();

			stmt1=conn.prepare(deleteMessageCascadeParts);
			stmt1.bind(1,user.getIdBytes());
			stmt1.bind(2,idBytes);
			stmt1.stepThrough();
			
			
			return conn.getChanges()>0;
			
		} catch (SQLiteException e) {
			log.error("error happend: {}",e);
		} finally{
			safeDispose(stmt);
			safeDispose(stmt1);
		}
		
		return true;
	}

	@Override
	public void selfCheck() {
		// TODO Auto-generated method stub

	}

	@Override
	public void archive() {
		// TODO Auto-generated method stub

	}

	private void safeDispose(SQLiteStatement stmt) {
		if (stmt != null) {
			stmt.dispose();
		}
	}


	@Override
	public boolean saveOrUpdateUser(ClientUserInfo userInfo)
			throws SQLiteBusyException {

		SQLiteStatement stmt = null;
		try {

			SQLiteConnection connection = packedVarThreadLocal.get().connection;
			beginTransaction();
			stmt = connection.prepare(saveOrUpdateUserInfoParts);
			stmt.reset();
			stmt.bind(1, userInfo.getIdBytes());
			stmt.bind(2, userInfo.getUserName());
			stmt.bind(3, userInfo.getDescription());
			stmt.bind(4, userInfo.getIdBytes());
			stmt.bind(5, userInfo.isRandomUser() ? 1 : 0);

			stmt.stepThrough();
			endTransaction();

			int insertRows = connection.getChanges();

			if (insertRows == 1) {

				log.debug("save or update user {} successed",userInfo);
				return true;
			}
			
			return false;

		} catch (SQLiteBusyException e) {
			rollbackTransaction();
			throw e;
		} catch (SQLiteException e) {

			rollbackTransaction();
			log.error("saveClientUserInfo failed {}", e);

			return false;
		} finally {
			safeDispose(stmt);
		}

	}

	@Override
	public List<ClientUserInfo> queryUser(String keywords, PagedInfo pagedInfo, QueryMethod queryMethod)
			throws SQLiteBusyException {

		SQLiteStatement stmt = null;

		try {
			
			SQLParts usedParts=null;
			
			switch (queryMethod) {
			case PART:
				usedParts=partSearchUserNameParts;
				break;
			case TOKEN:
				usedParts=ftsSearchUserParts;
				break;
			case EXACTLY:
				usedParts=extractlySearchUserNameParts;
				break;
			}

			stmt = packedVarThreadLocal.get().connection.prepare(
					usedParts);

			stmt.bind(1, queryMethod==QueryMethod.PART?"%"+keywords+"%":keywords);

			stmt.bind(2, pagedInfo.size);

			stmt.bind(3, pagedInfo.beginOffset());

			List<ClientUserInfo> ret = new ArrayList<ClientUserInfo>();

			while (stmt.step()) {
				//TODO return BaseUserInfo list?
				ret.add((ClientUserInfo) parseClientUserInfo(stmt,true));
			}
			return ret;
		} catch (SQLiteBusyException e) {
			throw e;
		} catch (SQLiteException e) {

			log.error("query user failed {}", e);

			return null;
		} catch (ParseException e) {
			log.error( "query user failed {}", e);
			return null;
		}

	}


	@Override
	public boolean saveMessage(Collection<? extends AbstractMessage> messageList)
			throws SQLiteBusyException {

		beginTransaction();
		
		boolean result=true;

		for (AbstractMessage message : messageList) {

			try {

				result=saveMessageInternal(message);

			} catch (SQLiteBusyException e) {
				// never throw when batch save
				log.error("database busy", e);
				result=false;
			}
		}

		endTransaction();

		return result;
	
	}

	@Override
	public <T extends AbstractMessage> List<T> queryMessage(final Class<T> clazz,
			final String keyword, final PagedInfo pagedInfo) throws SQLiteBusyException,
			ParseException {
		


		final HashMap<String,MessageRecord> idMapMessage = new HashMap<String,MessageRecord>();
		// this function is for fast query use, so do not 
		// load all dependency .use detail query interface to get it
		
		
		SQLParts usedSQL=(clazz==AbstractMessage.class?ftsQueryMessageParts:ftsQueryTypedMessageParts );
		
		queryInternal(usedSQL, new BindCallback() {

			@Override
			public void onBind(SQLiteStatement stmt) throws SQLiteException {
				stmt.bind(1, keyword);
				
				
				if (clazz==AbstractMessage.class) {
					stmt.bind(2, pagedInfo.size);
					stmt.bind(3, pagedInfo.beginOffset());
					return;
				} else {	
					stmt.bind(2, clazzMapMessageType.get(clazz).getIndex());					
					stmt.bind(3, pagedInfo.size);
					stmt.bind(4, pagedInfo.beginOffset());
				}

			}
		}, new RecursiveMessageStepCallback() {

			void onStep(MessageRecord one) {
				if (!idMapMessage.containsKey(one.message.getIdBytes())) {
					idMapMessage.put(one.message.getIdBytes(), one);
				}
			}
		});
		
		

		List<T> ret=Utility.reconstruct(clazz, idMapMessage);
		
		return ret;

	}

	public void threadRecycle(boolean unlockRead){
		packedVarThreadLocal.remove(unlockRead);
	}

	public void threadRecycle(){
		threadRecycle(true);
	}
	
	public static void main(String[] args) {
		
		System.out.println("queryNewestParts");
		System.out.println(queryNewestParts);
		
		System.out.println("ftsQueryMessageParts");
		System.out.println(ftsQueryMessageParts);
	}
	
	@Override
	public AbstractMessage loadWithDependency(final String idBytes) throws SQLiteBusyException, ParseException{
		
		assert DigestUtility.isValidSHA1(idBytes);
		
		final HashMap<String,MessageRecord> idMapMessage = new HashMap<String,MessageRecord>();

		queryInternal(queryDetailParts, new BindCallback() {
			
			@Override
			public void onBind(SQLiteStatement stmt) throws SQLiteException {
				stmt.bind(1, idBytes);
			}
		}, new RecursiveMessageStepCallback() {

			@Override
			void onStep(MessageRecord one) {
				//no need to check if result is duplicate,since we can not save circle reference message
				//so result is always chain (no duplicate)
				idMapMessage.put(one.message.getIdBytes(), one);
			}
		});
		
		List<AbstractMessage> ret=Utility.reconstruct(AbstractMessage.class, idMapMessage);
		
		//reconstruct result is order by createDate desc, 
		//so first element (if any)must be the one we need
		return ret.isEmpty()?null:ret.get(0);
	}

	@Override
	public void recycle(RecycleCatelog catelog) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void judgeMessage(String idBytes, boolean positive) throws SQLiteBusyException {
			
		SQLiteStatement stmt = null;

		try {
			stmt = packedVarThreadLocal.get().connection.prepare(
					judgeParts[positive?0:1]);

			stmt.bind(1, idBytes);

			stmt.stepThrough();
		} catch(SQLiteBusyException e){
			throw e;
		} catch (SQLiteException e) {
			log.error("{}",e);
		}finally{
			safeDispose(stmt);
		}


	}

	@Override
	public List<AbstractMessage> queryTopic(final String keywords, final boolean fuzzy,final PagedInfo pagedInfo) throws SQLiteBusyException, ParseException {
		
		log.trace("query topic {}",keywords);
		
		final Map<String,MessageRecord> ret=new HashMap<String,MessageRecord>();
		queryInternal(fuzzy?queryTopicFuzzyParts:queryTopicExactlyParts, new BindCallback() {
			
			@Override
			public void onBind(SQLiteStatement stmt) throws SQLiteException {
				
				stmt.bind(1, keywords);
				stmt.bind(2, pagedInfo.size);
				stmt.bind(3, pagedInfo.beginOffset());
				
			}
		}, new RecursiveMessageStepCallback() {

			@Override
			void onStep(MessageRecord one) {
				
				//must check if we already put it. since this query may have duplicate
				if (!ret.containsKey(one.message.getIdBytes())) {
					ret.put(one.message.getIdBytes(),one);
				}
			}
			
			
		});
		return Utility.reconstruct(AbstractMessage.class, ret);
	}

	private long blockingThreadId = -1;

	void executeSQL(String sql) throws SQLiteException {

		SQLiteConnection connection = packedVarThreadLocal.get(false).connection;

		connection.exec(sql);

	}
	
	

	void unblockOtherThread() {
		
		threadRecycle(false);

		// to avoid different thread enter

		if (Thread.currentThread().getId() == blockingThreadId) {
			blockingThreadId=-1;
			rwLock.writeLock().unlock();
			return;
		}

		if (blockingThreadId==-2) {
			
			log.error("blocking failed, so do not call unblock");
			return;
		}

		log.error("must same thread unlock locked datastore");
		throw new IllegalThreadStateException("unblockOtherThread called from thread different the one blockOtherThread");
	}

	/**
	 * blocking other thread connections up to timeout 
	 * for sqlite mutex like use
	 * if successed ,must call unblockOtherThread in pair (must in same thread)
	 * if failed ,no need to call unblockOtherThread
	 * 
	 * @param msTimeout
	 * @return
	 */
	boolean blockOtherThread(int msTimeout) {
		
		//must recycle current thread too
		threadRecycle();


		try {
			if(rwLock.writeLock().tryLock(msTimeout,TimeUnit.MILLISECONDS)){
				blockingThreadId = Thread.currentThread().getId();
				return true;
			}
		} catch (InterruptedException e) {
			//blocking failed
		}

		blockingThreadId = -2;
		return false;
	}

	@Override
	public void updateVoteTopicMessages(Collection<VoteTopicMessage> toUpdate)
			throws SQLiteBusyException {

		SQLiteConnection connection;
		try {
			connection = packedVarThreadLocal.get().connection;
		} catch (SQLiteBusyException e) {
			throw e;
		}catch (SQLiteException e1) {
			//TODO
			return;
		}

		SQLiteStatement stmt = null;

		try {
			stmt = connection.prepare(updateVoteTopicParts);
		} catch (SQLiteBusyException e) {
			throw e;
		}catch (SQLiteException e1) {
			//TODO return
		}

		for (VoteTopicMessage voteTopicMessage : toUpdate) {

			int replaceBeginIndex = VoteTopicMessage.META_HEADER_LENGTH_LENGTH + voteTopicMessage.getMetaHeaderLength();

			try {
				stmt.bind(1, replaceBeginIndex);
				stmt.bind(2, voteTopicMessage.getVoteResultString());
			} catch (SQLiteException e) {
			}
			

		}

		safeDispose(stmt);

	}
	
	public static String createQueryVotedSQL(int number,boolean assumeExist){
		
		StringBuilder builder=new StringBuilder();
		
		builder.append("WITH QUERY_TOPIC_SET AS " +
				"( SELECT ? AS VOTE_TOPIC_ID_BYTES ");

		for (int i = 1; i < number; i++) {
			
			builder.append(" UNION SELECT ? ");
		}
		
		builder.append(" )");
		
		builder.append(
				" SELECT " +
		(assumeExist?"REFERENCE_ID_BYTES":" REFERENCE_ID_BYTES,ID_BYTES ") +
				"  FROM ( QUERY_TOPIC_SET " +
				" LEFT JOIN " +
				" (SELECT " +
				" REFERENCE_ID_BYTES " +
				" FROM ABSTRACT_MESSAGE " +
				" WHERE SENDER_ID_BYTES = ? " +
				// only one VoteOfMe of one user is allowed 
				// for one VoteTopic ,so this condition with
				// index is fastest
				" AND MESSAGE_TYPE_ID = 3 " +
				") " +
				" ON VOTE_TOPIC_ID_BYTES=REFERENCE_ID_BYTES)");
		
		if (!assumeExist) {
			builder.append(" LEFT JOIN (SELECT ID_BYTES FROM ABSTRACT_MESSAGE " +
					//message type is VoteTopicMessage
					"WHERE MESSAGE_TYPE_ID = 2 " +
					//this condition check help 
					" AND ID_BYTES IN QUERY_TOPIC_SET " +
					") ON VOTE_TOPIC_ID_BYTES=ID_BYTES");
		}
		
		return builder.toString();
	}
	
	public static class QueryVotedCacheKey{
		int placeHolderNumber;
		boolean assumeExist;
		public QueryVotedCacheKey(int placeHolderNumber,boolean assumeExist) {
			super();
			this.assumeExist = assumeExist;
			this.placeHolderNumber = placeHolderNumber;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (assumeExist ? 1231 : 1237);
			result = prime * result + placeHolderNumber;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			QueryVotedCacheKey other = (QueryVotedCacheKey) obj;
			if (assumeExist != other.assumeExist)
				return false;
			if (placeHolderNumber != other.placeHolderNumber)
				return false;
			return true;
		}
		
	}
	
	ConcurrentHashMap<QueryVotedCacheKey, SQLParts> cachedSQLParts=new ConcurrentHashMap<SQLiteDataStore.QueryVotedCacheKey, SQLParts>();

	private SQLParts createCachedParts(int size,boolean assumeExist){
		
		QueryVotedCacheKey key=new QueryVotedCacheKey(size,assumeExist );
		
		SQLParts ret=cachedSQLParts.get(key);
		
		if (ret!=null) {
			return ret;
		}
		
		SQLParts set=new SQLParts(createQueryVotedSQL(size, assumeExist)).fix();

		cachedSQLParts.putIfAbsent(key, set);
		
		return set;
	}

	@Override
	public List<Boolean> queryVoted(BaseUserInfo user,
			Set<String> voteTopicIdBytes, boolean assumeExistPassedIn) throws SQLiteException {
		
		//more test needed, maybe same speed 
		//when assumeExist true or false
		final boolean assumeExist=assumeExistPassedIn;

		SQLiteStatement stmt=null;

		try{

			stmt = packedVarThreadLocal.get().connection.prepare(createCachedParts(voteTopicIdBytes.size(), assumeExist));

			List<Boolean> ret=new ArrayList<Boolean>();

			int index=1;
			for (String idbytes: voteTopicIdBytes) {
				stmt.bind(index++, idbytes);
			}

			stmt.bind(index++, user.getIdBytes());

			while(stmt.step()){
				
				//i think JIT is smart enough to skip the assumeExist condition check

				if (assumeExist) {
					ret.add(!stmt.columnNull(0));
				}else{
					String idBytesColumn=stmt.columnString(1);
					if (idBytesColumn==null) {
						//no such VoteTopicMessage
						ret.add(null);
					}else {
						//if column 0 (ref_id from msg where ref_id=to_query) 
						// is null ,means no such VoteOfMeMessage ref to_query
						// is not null, means already VoteOfMeMessage ref to_query
						ret.add(!stmt.columnNull(0));
					}
				}
			}

			return ret;
		}finally {
			safeDispose(stmt);
		}
	}

	@Override
	public boolean existMessage(String idBytes) throws SQLiteException {
		SQLiteStatement stmt=null;
		try {
			SQLiteConnection conn=packedVarThreadLocal.get().connection;
			
			stmt=conn.prepare(messageExistParts);
			
			stmt.bind(1, idBytes);
			
			return stmt.step();
			
		} finally {
			safeDispose(stmt);
		}
	}
	
	private byte[] unchangedMasterKey;

	@Override
	public byte[] getFileKey() {
		
		if(unchangedMasterKey!=null){
			return unchangedMasterKey;
		}

		SQLiteStatement queryStmt=null;
		SQLiteStatement initStmt=null;
		try {
			SQLiteConnection conn=packedVarThreadLocal.get().connection;
			
			queryStmt=conn.prepare(queryFileKeyParts);
			
			queryStmt.step();
			
			if (queryStmt.hasRow()) {
				
				unchangedMasterKey=queryStmt.columnBlob(0);
				return unchangedMasterKey;
			}
			
			initStmt=conn.prepare(randomFileKeyParts);
			
			initStmt.stepThrough();
			
			return getFileKey();
			
		} catch (SQLiteException e) {
			log.error("impossible {}",e);
			return null;
		} finally {
			safeDispose(queryStmt);
			safeDispose(initStmt);
		}
	}
}
