package com.bitVaultAttachment.database;

import java.io.File;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.util.encoders.Hex;

import com.bitVaultAttachment.apiMethods.Utils;
import com.bitVaultAttachment.constant.BitVaultConstants;
import com.bitVaultAttachment.constant.GlobalCalls;
import com.bitVaultAttachment.models.AttachmentDTO;
import com.bitVaultAttachment.models.DraftAttachmentDTO;
import com.bitVaultAttachment.models.DraftListDTO;
import com.bitVaultAttachment.models.NotificationListDTO;


/**
 * This is my Database connection Class
 * 
 * Created by vvdn on 6/1/2017.
 */
public class DbConnection {

	private static Connection mConnection;
	private static String bitVaultToken = "";

	/**
	 * This method is used for Driver Management
	 * 
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private void getConnection(String registration_token) throws ClassNotFoundException, SQLException {
		// get DB path and class name
		Class.forName("org.hsqldb.jdbcDriver");
		String path = BitVaultConstants.PATH_FOR_DATABASE;

		// convert UUID String to bytes
		UUID uuidOfToken = null;
		try {
			uuidOfToken = UUID.fromString(registration_token);
		} catch (Exception e1) {
			Utils.getLogger().log(Level.SEVERE,"failed to parse bitvault-token", e1);
		}
		long hi = uuidOfToken.getMostSignificantBits();
		long lo = uuidOfToken.getLeastSignificantBits();
		byte[] uuidInBytes = ByteBuffer.allocate(16).putLong(hi).putLong(lo).array();
		// get double hash for DB name
		String doubleHashOfBitvaultToken = null;
		try {
			doubleHashOfBitvaultToken = Hex.toHexString(doubleHash(uuidInBytes));
			Utils.getLogger().log(Level.FINEST,"hash of token : " + doubleHashOfBitvaultToken);
		} catch (NoSuchAlgorithmException e) {
			Utils.getLogger().log(Level.SEVERE,"hash of token failed", e);
		}
		// Key Derivation for DB
		String hkdfSalt = "desktop_db_salt" + registration_token;
		byte[] dbKey = new byte[16];
		try {
			Digest digest = new SHA256Digest();
			HKDFBytesGenerator kdfBytesGenerator = new HKDFBytesGenerator(digest);     
			kdfBytesGenerator.init(new HKDFParameters( uuidInBytes, hkdfSalt.getBytes(), null));
			kdfBytesGenerator.generateBytes(dbKey, 0, 16);
			Utils.getLogger().log(Level.FINEST, "DB Key : " + Hex.toHexString(dbKey));
		} catch (DataLengthException e1) {
			Utils.getLogger().log(Level.SEVERE, "db key generation error", e1);
		} catch (IllegalArgumentException e1) {
			Utils.getLogger().log(Level.SEVERE, "db key generation error", e1);
		}

		// try and open DB
		try {
			mConnection = DriverManager.getConnection("jdbc:hsqldb:file:" + path + File.separator
					+ doubleHashOfBitvaultToken+".db;sql.syntax_ora=true;crypt_key=" + 
					Hex.toHexString(dbKey) + ";crypt_type=AES", "SA", "");
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"Error opening DB", e);
		}
	}

	/**
	 * This method is used to fetch Data from Database for Notifications
	 * 
	 * @param bitVaultToken
	 * 
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public ResultSet fetchInboxData(String bitVaultToken, String searchText, short isDownloaded, int sortBy,
			int noOfRec, int offset) throws SQLException, ClassNotFoundException {
		if (mConnection == null)
			getConnection(bitVaultToken);
		Statement stat = mConnection.createStatement();
		ResultSet rs;

		String sortByStr = "date";
		sortByStr = findSortBy(sortBy);

		String queryString = "select * from NotifctnTbl where isDownloaded=" + isDownloaded + " and bitVaultToken='"
				+ bitVaultToken.trim() + "'";
		queryString += ((!GlobalCalls.isNullOrEmptyStringCheck(searchText))
				? " and senderAddress like '%" + searchText + "%'" : "") + " order by " + sortByStr + " desc limit "
				+ offset + ", " + noOfRec + ";";

		rs = stat.executeQuery(queryString);

		return rs;

	}

	/**
	 * Sort data
	 * @param sortBy
	 * @return
	 */
	private String findSortBy(int sortBy) {

		String sortByStr = "date";
		switch (sortBy) {
		case 1:
			sortByStr = "senderAddress";
			break;
		case 2:
			sortByStr = "date";
			break;
		case 3:
			sortByStr = "size";
			break;

		default:
			sortByStr = "date";
			break;
		}
		return sortByStr;
	}

	/**
	 * This method is used to fetch Data size from Database for Notifications
	 * 
	 * @param bitVaultToken
	 * 
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public int inboxDataListSize(String bitVaultToken, String searchText, short isDownloaded)
			throws SQLException, ClassNotFoundException {
		if (mConnection == null)
			getConnection(bitVaultToken);
		Statement stat = mConnection.createStatement();
		ResultSet res;
		int count = 0;

		String queryString = "select count(*) from NotifctnTbl where isDownloaded=" + isDownloaded
				+ " and bitVaultToken='" + bitVaultToken + "'";
		queryString += ((!GlobalCalls.isNullOrEmptyStringCheck(searchText))
				? " and senderAddress like '%" + searchText + "%'" : "") + "  ;";
		res = stat.executeQuery(queryString);
		while (res.next()) {
			count = res.getInt(1);
		}

		return count;
	}

	/**
	 * This method is used to fetch Data from Database for Drafts
	 * 
	 * @param bitVaultToken
	 * 
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public int draftDataListSize(String bitVaultToken, String searchText) throws SQLException, ClassNotFoundException {

		if (mConnection == null)
			getConnection(bitVaultToken);
		Statement stat = mConnection.createStatement();
		ResultSet res;
		int count = 0;
		String queryString = "select count(*) from DraftsTbl where bitVaultToken='" + bitVaultToken + "'";
		queryString += ((!GlobalCalls.isNullOrEmptyStringCheck(searchText)) ? " and TxId like '%" + searchText + "%'"
				: "") + " ;";
		res = stat.executeQuery(queryString);
		while (res.next()) {
			count = res.getInt(1);
		}

		return count;
	}

	/**
	 * This method is used to fetch Data from Database for Inbox Attachments
	 * 
	 * @param bitVaultToken
	 * 
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public ResultSet fetchAttachmentDataInbox(String bitVaultToken, String hashTxId)
			throws SQLException, ClassNotFoundException {
		if (mConnection == null)
			getConnection(bitVaultToken);
		Statement stat = mConnection.createStatement();
		ResultSet rs = null;

		rs = stat.executeQuery("select * from AttachmentTbl where bitVaultToken='" + bitVaultToken + "' and hashTxId='"
				+ hashTxId + "';");

		return rs;

	}

	/**
	 * 
	 * This will convert Result set into Data Transfer object.
	 * 
	 * @param bitVaultId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public List<NotificationListDTO> fetchData4Inbox(String bitVaultToken, String searchText, short isDownloaded,
			int sortBy, int noOfRec, int offset) throws ClassNotFoundException, SQLException {
		ResultSet rs = fetchInboxData(bitVaultToken, searchText, isDownloaded, sortBy, noOfRec, offset);
		NotificationListDTO notificationListDTO;
		List<NotificationListDTO> list = new ArrayList<NotificationListDTO>();

		while (rs.next()) {
			notificationListDTO = new NotificationListDTO();
			notificationListDTO.setBitVaultToken(rs.getString("bitVaultToken").trim());
			notificationListDTO.setSenderAddress(rs.getString("senderAddress").trim());
			notificationListDTO.setSize(rs.getLong("size"));
			notificationListDTO.setDate(rs.getTimestamp("date"));
			notificationListDTO.setIsDownloaded(rs.getShort("isDownloaded"));
			notificationListDTO.setIsLocked(rs.getShort("isLocked"));
			notificationListDTO.setHashOfTxnId(rs.getString("hashTxId").trim());
			notificationListDTO.setNotificationTag(rs.getString("notificationTag").trim());
			notificationListDTO.setPathOfUnEncryptedFile(rs.getString("PathOfUnEncryptedFiles").trim());
			notificationListDTO.setRecieverAddress(rs.getString("recieverAddress").trim());
			list.add(notificationListDTO);
		}
		rs.close();
		return list;
	}

	/**
	 * 
	 * This will convert Result set into Data Transfer object.
	 * 
	 * @param bitVaultId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public List<AttachmentDTO> fetchAttachmentData4Inbox(String bitVaultToken, String hashTxId)
			throws ClassNotFoundException, SQLException {
		ResultSet rs = fetchAttachmentDataInbox(bitVaultToken, hashTxId);
		AttachmentDTO attachmentDTO;
		List<AttachmentDTO> list = new ArrayList<AttachmentDTO>();

		while (rs.next()) {
			attachmentDTO = new AttachmentDTO();
			attachmentDTO.setAttachName(rs.getString("attachmentName").trim());
			attachmentDTO.setAttachId(rs.getInt("attachId"));
			attachmentDTO.setHashTxId(rs.getString("hashTxId").trim());
			attachmentDTO.setSize(rs.getLong("size"));
			list.add(attachmentDTO);
		}
		rs.close();
		return list;
	}

	/**
	 * 
	 * This will convert Result set into Data Transfer object.
	 * 
	 * @param bitVaultId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public List<DraftListDTO> fetchData4Draft(String bitVaultToken, String searchText, int noOfRec, int offset)
			throws ClassNotFoundException, SQLException {
		ResultSet rs = fetchDraftData(bitVaultToken, searchText, noOfRec, offset);

		DraftListDTO draftListDTO;
		List<DraftListDTO> list = new ArrayList<DraftListDTO>();

		while (rs.next()) {
			draftListDTO = new DraftListDTO();
			draftListDTO.setBitVaultToken(rs.getString("bitVaultToken").trim());
			draftListDTO.setSenderAddress(rs.getString("senderAddress").trim());
			draftListDTO.setDate(rs.getDate("date"));
			draftListDTO.setIsCompressed(rs.getShort("isCompressed"));
			draftListDTO.setIsSending(rs.getShort("isSending"));
			draftListDTO.setIsEncrypteed(rs.getShort("isEncrypted"));
			draftListDTO.setTxnId(rs.getString("TxId").trim());
			draftListDTO.setSessionKey(rs.getString("SessionKey").trim());
			draftListDTO.setPathOfCopressedFilee(rs.getString("compressedFileName").trim());
			list.add(draftListDTO);
		}
		rs.close();
		return list;
	}

	/**
	 * This method is used to fetch Data from Database for Notifications
	 * 
	 * @param bitVaultToken
	 * 
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public ResultSet fetchDraftData(String bitVaultToken, String searchText, int noOfRec, int offset)
			throws SQLException, ClassNotFoundException {
		if (mConnection == null)
			getConnection(bitVaultToken);
		Statement stat = mConnection.createStatement();
		ResultSet rs;
		String queryString = "select * from DraftsTbl where bitVaultToken='" + bitVaultToken + "'";
		queryString += ((!GlobalCalls.isNullOrEmptyStringCheck(searchText)) ? " and TxId like '%" + searchText + "%'"
				: "") + " order by date desc limit " + offset + ", " + noOfRec + ";";
		rs = stat.executeQuery(queryString);

		return rs;

	}

	/**
	 * 
	 * This will convert Result set into Data Transfer object.
	 * 
	 * @param bitVaultId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public List<DraftAttachmentDTO> fetchAttachmentData4Draft(String bitVaultToken, String txId)
			throws ClassNotFoundException, SQLException {
		ResultSet rs = fetchAttachmentDataDraft(bitVaultToken, txId);
		DraftAttachmentDTO attachmentDTO;
		List<DraftAttachmentDTO> list = new ArrayList<DraftAttachmentDTO>();

		while (rs.next()) {
			attachmentDTO = new DraftAttachmentDTO();
			attachmentDTO.setAttachName(rs.getString("attachmentName").trim());
			attachmentDTO.setAttachPath(rs.getString("attachmentPath").trim());
			attachmentDTO.setSize(rs.getInt("size"));
			attachmentDTO.setAttachId(rs.getInt("attachId"));
			attachmentDTO.setTxId(rs.getString("TxId").trim());
			list.add(attachmentDTO);
		}
		rs.close();
		return list;
	}

	/**
	 * This method is used to fetch Data from Database for Inbox Attachments
	 * 
	 * @param bitVaultToken
	 * 
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public ResultSet fetchAttachmentDataDraft(String bitVaultToken, String txId)
			throws SQLException, ClassNotFoundException {
		if (mConnection == null)
			getConnection(bitVaultToken);
		Statement stat = mConnection.createStatement();
		ResultSet rs = null;

		rs = stat.executeQuery("select * from DraftsAttachmentTbl where bitVaultToken='" + bitVaultToken
				+ "' and TxId='" + txId + "';");

		return rs;
	}

	/**
	 * 
	 * This method is to insert data into Table when new notification .
	 * 
	 * @param bitVaultId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */

	public boolean addNotificationDta(String bitVaultToken, String sendersAddress, String hashTxId,
			String notificationTag, String recieverAddress) throws ClassNotFoundException, SQLException {

		if (mConnection == null)
			getConnection(bitVaultToken);

		try {
			PreparedStatement prep = mConnection
					.prepareStatement("insert into NotifctnTbl values (?,?,?,?,?,?,?,?,?,?,?,?,?);");

			prep.setString(1, bitVaultToken);
			prep.setString(2, sendersAddress.trim());
			prep.setInt(3, 0);
			prep.setTimestamp(4, getCurrentDateInTimeStamp());
			prep.setShort(5, (short) 0);
			prep.setShort(6, (short) 0);
			prep.setString(7, hashTxId);
			prep.setString(8, ""); 
			prep.setString(9, ""); 
			prep.setString(10, "");
			prep.setShort(11, (short) 0);
			prep.setString(12, notificationTag);
			prep.setString(13, recieverAddress);
			prep.addBatch();
			mConnection.setAutoCommit(false);
			prep.executeBatch();
			mConnection.setAutoCommit(true);
		} catch (Exception e) {
			return false;
		}

		return true;
	}

	/**
	 * convert util date to sql date
	 * 
	 * @return New Date
	 */
	private java.sql.Date getCurrentDate() {
		java.util.Date utilDate = new java.util.Date();
		java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
		return sqlDate;
	}

	/**
	 * convert util date to sql date
	 * 
	 * @return New Date
	 */
	private java.sql.Timestamp getCurrentDateInTimeStamp() {
		java.util.Date utilDate = new java.util.Date();
		return new java.sql.Timestamp(utilDate.getTime());
	}

	/**
	 * Create Database Scheme
	 * @param registration_token
	 */
	public void createScheme(String registration_token) {
		// get device token
		bitVaultToken = registration_token;

		try {
			if (mConnection == null)
				getConnection(registration_token);

			Statement stat2 = mConnection.createStatement();
			stat2.executeUpdate(
					"create TABLE IF NOT EXISTS NotifctnTbl (bitVaultToken char(200), senderAddress char(200),size long,date TIMESTAMP,isDownloaded int,isLocked int ,hashTxId char(200), PathOfUnEncryptedFiles char(200), TxId char(200), sessionKey char(200),FromBitvault int,notificationTag char(200),recieverAddress char(200), primary key(bitVaultToken,hashTxId));");
			stat2.executeUpdate(
					"create TABLE IF NOT EXISTS AttachmentTbl (bitVaultToken char(200), hashTxId char(200),attachId int,attachmentName char(200) ,size long, primary key(bitVaultToken,hashTxId,attachId));");
			stat2.executeUpdate(
					"create TABLE IF NOT EXISTS DraftsTbl (bitVaultToken char(200), senderAddress char(200),date Date,isEncrypted int,isSending int,isCompressed int ,TxId char(200),SessionKey char(200),compressedFileName char(200),size int,timeStamp long,fileHash char(200), primary key(bitVaultToken,TxId));");
			stat2.executeUpdate(
					"create TABLE IF NOT EXISTS DraftsAttachmentTbl (bitVaultToken char(200), TxId char(200),attachId int,attachmentName char(200) ,attachmentPath char(200),size long , primary key(bitVaultToken,TxId,attachId));");

			mConnection.commit();

		} catch (ClassNotFoundException e) {
			Utils.getLogger().log(Level.SEVERE,"error creating scheme", e);
		} catch (SQLException e) {
			Utils.getLogger().log(Level.SEVERE,"error creating scheme", e);
		}

	}

	/**
	 * 
	 * This method is to change data when Notification is downloaded .
	 * 
	 * @param bitVaultId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */

	public boolean updateNotificationDtaOnDownloading(String bitVaultToken, String hashTxId)
			throws ClassNotFoundException, SQLException {

		if (mConnection == null)
			getConnection(bitVaultToken);

		try {
			PreparedStatement prep = mConnection.prepareStatement(
					"update NotifctnTbl set isDownloaded = ? where hashTxId = ? and  bitVaultToken = ?;");

			// set the preparedstatement parameters
			prep.setShort(1, (short) 1);
			prep.setString(2, hashTxId);
			prep.setString(3, bitVaultToken);
			mConnection.setAutoCommit(false);
			prep.executeUpdate();
			mConnection.setAutoCommit(true);
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"Error updating notification data on downloading", e);
			return false;
		}

		return true;
	}

	/**
	 * 
	 * This method is to change data when Notification is unlocked .
	 * 
	 * @param bitVaultId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */

	public boolean updateNotificationDtaOnUnlocking(String bitVaultToken, String hashTxId,
			String pathOfUnEncryptedFiles, ArrayList<AttachmentDTO> attachmentDTOs)
					throws ClassNotFoundException, SQLException {

		if (mConnection == null)
			getConnection(bitVaultToken);

		try {
			PreparedStatement prep = mConnection.prepareStatement(
					"update NotifctnTbl set isLocked = ?,PathOfUnEncryptedFiles = ? , size = ? where hashTxId = ? and  bitVaultToken = ?;");

			// set the prepared statement parameters
			prep.setShort(1, (short) 1);
			prep.setString(2, pathOfUnEncryptedFiles);
			prep.setString(4, hashTxId);
			prep.setString(5, bitVaultToken);

			PreparedStatement prep2;
			long finalSize = 0;
			for (AttachmentDTO attachmentDTO : attachmentDTOs) {
				prep2 = mConnection.prepareStatement("insert into AttachmentTbl values (?, ?,?,?,?);");

				prep2.setString(1, bitVaultToken);
				prep2.setString(2, hashTxId);
				prep2.setInt(3, attachmentDTO.getAttachId());
				prep2.setString(4, attachmentDTO.getAttachName());
				prep2.setLong(5, attachmentDTO.getSize());
				finalSize = finalSize + attachmentDTO.getSize();
				prep2.executeUpdate();
			}
			prep.setLong(3, finalSize);
			mConnection.setAutoCommit(false);
			prep.executeUpdate();
			mConnection.setAutoCommit(true);

		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"Error updating notification data on unlocking", e);
			return false;
		}

		return true;
	}

	/**
	 * 
	 * This method is to delete data when delete link is clicked.
	 * 
	 * @param bitVaultId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */

	public boolean updateNotificationDtaOnDelete(String bitVaultToken, String hashTxId)
			throws ClassNotFoundException, SQLException {

		if (mConnection == null)
			getConnection(bitVaultToken);

		try {
			PreparedStatement prep = mConnection
					.prepareStatement("delete from  NotifctnTbl where hashTxId = ? and  bitVaultToken = ?;");

			prep.setString(1, hashTxId);
			prep.setString(2, bitVaultToken);
			mConnection.setAutoCommit(false);
			prep.executeUpdate();
			mConnection.setAutoCommit(true);

			PreparedStatement prep2 = mConnection
					.prepareStatement("delete from  AttachmentTbl where hashTxId = ? and  bitVaultToken = ?;");
			prep2.setString(1, hashTxId);
			prep2.setString(2, bitVaultToken);
			prep2.executeUpdate();

		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"Error updating notification data", e);
			return false;
		}

		return true;
	}

	/**
	 * 
	 * This method is to insert data into Table when new notification .
	 * 
	 * @param bitVaultId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */

	public boolean addDraftDta(DraftListDTO draftListDTO, List<DraftAttachmentDTO> attachmentDTOs)
			throws ClassNotFoundException, SQLException {

		if (mConnection == null)
			getConnection(bitVaultToken);

		try {
			PreparedStatement prep = mConnection
					.prepareStatement("insert into DraftsTbl values (?, ?,?,?,?,?,?,?,?,?,?,?);");

			prep.setString(1, draftListDTO.getBitVaultToken());
			prep.setString(2, "");
			prep.setDate(3, getCurrentDate());
			prep.setShort(4, (short) 0);
			prep.setShort(5, (short) 0);
			prep.setShort(6, (short) 0);
			prep.setString(7, draftListDTO.getTxnId());
			prep.setString(8, draftListDTO.getSessionKey());
			prep.setString(9, "");
			prep.setInt(10, 0);
			prep.setLong(11, draftListDTO.getTimeStampValue());
			prep.setString(12, draftListDTO.getFileHash());
			prep.addBatch();
			mConnection.setAutoCommit(false);
			prep.executeBatch();
			mConnection.setAutoCommit(true);

			PreparedStatement prep2;
			for (DraftAttachmentDTO attachmentDTO : attachmentDTOs) {
				prep2 = mConnection.prepareStatement("insert into DraftsAttachmentTbl values (?, ?,?,?,?,?);");

				prep2.setString(1, draftListDTO.getBitVaultToken());
				prep2.setString(2, draftListDTO.getTxnId());
				prep2.setInt(3, attachmentDTO.getAttachId());
				prep2.setString(4, attachmentDTO.getAttachName());
				prep2.setString(5, attachmentDTO.getAttachPath());
				prep2.setLong(6, attachmentDTO.getSize());
				prep2.addBatch();
				prep2.executeBatch();
			}
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"Error adding DB draft data", e);
			return false;
		}

		return true;
	}

	/**
	 * Fetch and check if records exist
	 * @param bitVaultToken
	 * @param TxId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public DraftListDTO fetchIfRecordExist(String bitVaultToken, String TxId)
			throws ClassNotFoundException, SQLException {

		if (mConnection == null)
			getConnection(bitVaultToken);

		Statement stmt = mConnection.createStatement();

		ResultSet rs = stmt.executeQuery(
				"SELECT * FROM DraftsTbl WHERE bitVaultToken ='" + bitVaultToken + "' and TxId='" + TxId + "';");
		DraftListDTO draftListDTO = null;

		while (rs.next()) {
			draftListDTO = new DraftListDTO();
			draftListDTO.setBitVaultToken(rs.getString("bitVaultToken").trim());
			draftListDTO.setSenderAddress(rs.getString("senderAddress").trim());
			draftListDTO.setDate(rs.getDate("date"));
			draftListDTO.setIsCompressed(rs.getShort("isCompressed"));
			draftListDTO.setIsSending(rs.getShort("isSending"));
			draftListDTO.setIsEncrypteed(rs.getShort("isEncrypted"));
			draftListDTO.setTxnId(rs.getString("TxId").trim());
			draftListDTO.setSessionKey(rs.getString("SessionKey").trim());
			draftListDTO.setPathOfCopressedFilee(rs.getString("compressedFileName").trim());
			draftListDTO.setSize(rs.getInt("size"));
			draftListDTO.setTimeStampValue(rs.getLong("timeStamp"));
			draftListDTO.setFileHash(rs.getString("fileHash")!=null?rs.getString("fileHash").trim():rs.getString("fileHash"));
		}
		rs.close();

		return draftListDTO;
	}

	/**
	 * Fetch Draft Attachment List
	 * @param bitVaultToken
	 * @param TxId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public List<DraftAttachmentDTO> fetchDraftAttachmentList(String bitVaultToken, String TxId)
			throws ClassNotFoundException, SQLException {

		if (mConnection == null)
			getConnection(bitVaultToken);

		Statement stmt = mConnection.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT * FROM DraftsAttachmentTbl WHERE bitVaultToken ='" + bitVaultToken
				+ "' and TxId='" + TxId + "';");
		List<DraftAttachmentDTO> attachmentDTOs = new ArrayList<>();
		DraftAttachmentDTO attachmentDTO = null;

		while (rs.next()) {
			attachmentDTO = new DraftAttachmentDTO();
			attachmentDTO.setAttachName(rs.getString("attachmentName").trim());
			attachmentDTO.setAttachId(rs.getInt("attachId"));
			attachmentDTO.setAttachPath(rs.getString("attachmentPath").trim());
			attachmentDTO.setTxId(rs.getString("TxId").trim());
			attachmentDTO.setSize(rs.getLong("size"));
			attachmentDTOs.add(attachmentDTO);
		}
		rs.close();

		return attachmentDTOs;
	}

	/**
	 * 
	 * This method is to delete data when delete link is clicked.
	 * 
	 * @param bitVaultId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */

	public boolean updatDraftDtaOnDelete(String bitVaultToken, String TxId)
			throws ClassNotFoundException, SQLException {

		if (mConnection == null)
			getConnection(bitVaultToken);

		try {
			PreparedStatement prep = mConnection
					.prepareStatement("delete from  DraftsTbl where TxId = ? and  bitVaultToken = ?;");

			prep.setString(1, TxId);
			prep.setString(2, bitVaultToken);
			mConnection.setAutoCommit(false);
			prep.executeUpdate();
			mConnection.setAutoCommit(true);

			PreparedStatement prep2 = mConnection
					.prepareStatement("delete from  DraftsAttachmentTbl where TxId = ? and  bitVaultToken = ?;");
			prep2.setString(1, TxId);
			prep2.setString(2, bitVaultToken);

		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"Error updating DB Draft Data on delete", e);
			return false;
		}

		return true;
	}

	/**
	 * 
	 * This method Changes the status of draft data.
	 * 
	 * @param bitVaultId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */

	public boolean updatDraftDtaOnRetry(DraftListDTO draftListDTO) throws ClassNotFoundException, SQLException {

		if (mConnection == null)
			getConnection(bitVaultToken);

		try {
			PreparedStatement prep = mConnection.prepareStatement(
					"update DraftsTbl set isEncrypted = ?, isSending = ?, isCompressed = ?, compressedFileName = ?, fileHash = ? where TxId = ? and  bitVaultToken = ?;");
			// set the prepared statement parameters
			prep.setShort(1, draftListDTO.getIsEncrypteed());
			prep.setShort(2, draftListDTO.getIsSending());
			prep.setShort(3, draftListDTO.getIsCompressed());
			prep.setString(4, draftListDTO.getPathOfCopressedFilee());
			prep.setString(5, draftListDTO.getTxnId());
			prep.setString(6, draftListDTO.getBitVaultToken());
			prep.setString(7, draftListDTO.getFileHash());
			mConnection.setAutoCommit(false);
			prep.executeUpdate();
			mConnection.setAutoCommit(true);
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"error updating DB draft entry on retry", e);
			return false;
		}

		return true;
	}

	/**
	 * Double Hashes the TXID
	 * 
	 * @param txId
	 * @return
	 */
	public byte[] doubleHash(byte[] hexValue) throws NoSuchAlgorithmException {

		MessageDigest digest = null;
		byte[] hash = null;

		try {
			digest = MessageDigest.getInstance("SHA-256");
			hash = digest.digest(digest.digest(hexValue));
		} catch (NoSuchAlgorithmException e) {
			Utils.getLogger().log(Level.SEVERE,"hashing error", e);
			throw e;
		}
		return hash;
	}
}
