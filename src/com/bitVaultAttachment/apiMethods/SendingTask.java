package com.bitVaultAttachment.apiMethods;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.DigestOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.bitVaultAttachment.apiMethods.CountingHttpEntity.ProgressListener;
import com.bitVaultAttachment.constant.BitVaultConstants;
import com.bitVaultAttachment.constant.GlobalCalls;
import com.bitVaultAttachment.constant.Messages;
import com.bitVaultAttachment.controller.MainPage;
import com.bitVaultAttachment.database.DbConnection;
import com.bitVaultAttachment.models.DataToPBCModel;
import com.bitVaultAttachment.models.DraftAttachmentDTO;
import com.bitVaultAttachment.models.DraftListDTO;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.concurrent.Task;

public class SendingTask extends Task<Void> {

	final private String SYMALGORITHM = "AES/CBC/PKCS5Padding";
	// IV - 16 bytes static
	final private byte[] ivBytes = { (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32, (byte) 0x56, (byte) 0x35, (byte) 0xE3,
			(byte) 0x03, (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32, (byte) 0x56, (byte) 0x35, (byte) 0xE3,
			(byte) 0x03 };
	private AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
	private final int ENC_BUFFER_LEN = 2097152; // 1048576 = 1 MB ; 2097152 = 2 MB
	private final String append = "_enc";
	private final int ZIP_BUFFER = 2048;
	public String txid, skey;
	private String ZippedFileName = txid + ".zip";
	
	private final int MAX_RETRY_SENDING_NODE = 2;
	protected static final long UPLOADING_TIMEOUT = 20000;
	private UploadProgress uploadProg = new UploadProgress();
	private double totalUploadedBytes = 0;
	private ProgressListener listener;
	private double totalProgress = 0;
	
	private CloseableHttpClient httpClient;
	private HttpPost httppost;
	private Object waiterResponse = new Object();
	private boolean clientExited = false;
	private boolean notifiedMsg = false;	

	public ArrayList<DraftAttachmentDTO> attachmentList;
	public static String KEY_STATUS = "status";
	public static String KEY_MESSAGE = "message";
	
	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * Constructor for sending task
	 * 
	 * @param txid
	 * @param skey
	 * @param attachmentList
	 */
	public SendingTask(String txid, String skey, ArrayList<DraftAttachmentDTO> attachmentList) {
		this.txid = txid;
		this.skey = skey;
		this.attachmentList = attachmentList;
		
		// listener to update UI
		uploadProg.progressProperty().addListener(
				(obs, oldProgress, newProgress) -> this.updateProgress((double) newProgress, totalProgress));

		// listener for updating bytes uploaded
		listener = new ProgressListener() {
			@Override
			public void transferred(long num) {
				totalUploadedBytes += num;
				// update progress bar
				uploadProg.setProgress(totalUploadedBytes);
				// stop waiting for response functions
				synchronized (waiterResponse){
					waiterResponse.notifyAll();
					notifiedMsg = true;
				}
			}
		};
	}

	/**
	 * Implement the sending task thread
	 */
	@Override
	protected Void call() throws Exception {

		DbConnection connection = new DbConnection();
		ZippedFileName = Hex.toHexString(Utils.hashGenerate(txid)) + ".zip";
		Utils.getLogger().log(Level.FINEST, "txid : " + txid);
		Utils.getLogger().log(Level.FINEST, "filename : " + ZippedFileName);
		long timeStamp = System.currentTimeMillis();

		// insert into database If Does not exist
		DraftListDTO draftListDTO = null;
		if (!GlobalCalls.isNullOrEmptyStringCheck(MainPage.bitVaultToken)
				&& !GlobalCalls.isNullOrEmptyStringCheck(txid)) {
			draftListDTO = connection.fetchIfRecordExist(MainPage.bitVaultToken.trim(), txid.trim());
			
		}
		if (draftListDTO == null) {
			draftListDTO = new DraftListDTO();
			draftListDTO.setBitVaultToken(MainPage.bitVaultToken);
			draftListDTO.setTxnId(txid);
			draftListDTO.setSessionKey(skey);
			draftListDTO.setTimeStampValue(timeStamp);
			if (draftListDTO != null && !GlobalCalls.isNullOrEmptyListCheck(attachmentList))
				connection.addDraftDta(draftListDTO, attachmentList);
		} else {
			attachmentList=new ArrayList<>();
			attachmentList = (ArrayList<DraftAttachmentDTO>) connection.fetchDraftAttachmentList(MainPage.bitVaultToken, txid);
			timeStamp=draftListDTO.getTimeStampValue();
		}

		// Update message
		this.updateMessage(Messages.COMPRESSING_ATT);

		// zip the files
		try {
			if (!isCancelled() && draftListDTO.getIsCompressed()==BitVaultConstants.FALSE) {
				Utils.getLogger().log(Level.INFO, "Compressing files...");
				ZipFiles(attachmentList);
				// database update
				draftListDTO.setIsCompressed(BitVaultConstants.TRUE);
				draftListDTO.setPathOfCopressedFilee(
						BitVaultConstants.PATH_FOR_ENCRYPTED_FILES + File.separator + ZippedFileName);
				if (draftListDTO != null)
					connection.updatDraftDtaOnRetry(draftListDTO);
			}
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"Zipping error", e);
			throw e;
		}

		// Update message
		this.updateMessage(Messages.ENCRYPTING_ATT);

		// Encrypt the compressed file
		String fileHash = draftListDTO.getFileHash();
		try {
			if (!isCancelled() && draftListDTO.getIsEncrypteed()==BitVaultConstants.FALSE) {
				// Open compressed file
				File compressedFile = null;
				try {
					if (!isCancelled()) {
						compressedFile = new File(BitVaultConstants.PATH_FOR_ENCRYPTED_FILES + File.separator + ZippedFileName);
					}
				} catch (Exception e) {
					Utils.getLogger().log(Level.SEVERE,"Zipped file read error", e);
					throw e;
				}
				
				// encrypt compressed file
				Utils.getLogger().log(Level.INFO, "Encrypting files...");
				fileHash = encryptFile(txid, skey, compressedFile);
				
				// delete compressed file
				try {
					if (!isCancelled()) {
						compressedFile.delete();
					}
				} catch (Exception e) {
					Utils.getLogger().log(Level.SEVERE,"Zipped file delete error", e);
					throw e;
				}
				
				// database update
				draftListDTO.setIsEncrypteed(BitVaultConstants.TRUE);
				draftListDTO.setFileHash(fileHash);
				if (draftListDTO != null)
					connection.updatDraftDtaOnRetry(draftListDTO);
				Utils.getLogger().log(Level.FINEST, "SHA256: " + fileHash);
			}
		} catch (Exception e) {
			throw e;
		}
		
		// generate string for CRC
		String filePath = BitVaultConstants.PATH_FOR_ENCRYPTED_FILES + File.separator + ZippedFileName + append;
		String txidHash =  Base64.toBase64String(Utils.hashGenerate(txid));
		String webServerValue = PBCRequest.WEB_SERVER_VALUE;
		String tag = PBCRequest.A2A_TAG;
		
		// setting temporary addresses for testing
		String receiverAddress = "NA";
		String senderAddress = "NA";		
		String encryptedSessionKey = "NA";
		String pbcId = "NA";
		String appId = "NA";

		String msgforCRC = tag + "|$$|" +txidHash + "|$$|" + receiverAddress + "|$$|" + fileHash + "|$$|"
				+ encryptedSessionKey + "|$$|" + pbcId + "|$$|" + appId + "|$$|" + timeStamp + "|$$|" + senderAddress
				+ "|$$|" + webServerValue;

		Utils.getLogger().log(Level.FINEST, msgforCRC);

		// find CRC
		String mCRC = null;
		try {
			mCRC = getCRC(msgforCRC);
			Utils.getLogger().log(Level.FINEST,"CRC: " + mCRC);
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"crc exception", e);
		}

		DataToPBCModel dataToPBCModel = new DataToPBCModel();

		// set DTO for PBC
		dataToPBCModel.setTag(tag);
		dataToPBCModel.setHashTXId(txidHash);
		dataToPBCModel.setReceiverWalletAdress(receiverAddress);
		dataToPBCModel.setCrc(mCRC);
		dataToPBCModel.setFilepath(filePath);
		dataToPBCModel.setEncryptedSessionKey(encryptedSessionKey);
		dataToPBCModel.setPbcId(pbcId);
		dataToPBCModel.setAppId(appId);
		dataToPBCModel.setTimeStamp(timeStamp);
		dataToPBCModel.setTxId(txid);
		dataToPBCModel.setDataHash(fileHash);
		dataToPBCModel.setSenderAddress(senderAddress);
		dataToPBCModel.setWebServerKeyValue(webServerValue);

		// send to PBC
		try {
			Utils.getLogger().log(Level.INFO, "Sending Files...");
			sendAttachments(dataToPBCModel);
			Utils.getLogger().log(Level.INFO, "attachments sent");
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"file sending failed", e);
			throw e;
		}
		
		// delete database entry
		if (!GlobalCalls.isNullOrEmptyStringCheck(MainPage.bitVaultToken)
				&& !GlobalCalls.isNullOrEmptyStringCheck(txid)) {
			connection.updatDraftDtaOnDelete(MainPage.bitVaultToken, txid);
		}
		
		// open encrypted file
		File encFile = null;
		try {
			encFile = new File(BitVaultConstants.PATH_FOR_ENCRYPTED_FILES + File.separator + ZippedFileName + append);
	
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"encrypted file read error when deleting", e);
		}
		
		// delete encrypted file
		try {
			if(encFile.exists()){
				encFile.delete();
				Utils.getLogger().log(Level.INFO, "deleted encrypted file");
			}
			else{
				this.updateMessage(Messages.FILE_SND_ERROR);
				throw new Exception("encrypted file not found while deleting");
			}
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"encrypted file delete error", e);
		}

		return null;
	}

	/**
	 * Encrypts input file and returns Hash of encrypted file
	 * 
	 * @param txid
	 * @param skey
	 * @param f
	 * @return
	 * @throws Exception
	 */
	public String encryptFile(String txid, String skey, File f) throws Exception {

		/* ------------------- Initialize the Keys --------------------- */
		// Get TXID Symmetric Key
		SecretKey txidKey = null;
		try {
			txidKey = getAESKey(txid);
		} catch (Exception e) {
			Utils.getLogger().log(Level.FINEST, "TXID key encoding error");
			throw e;
		}

		// Get Session key
		Key SessionKey = null;
		try {
			SessionKey = getAESKey(skey);
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"Session Key encoding error", e);
			throw e;
		}

		Utils.getLogger().log(Level.FINEST, "File Name: " + f.getAbsolutePath() + "\n");

		/* ------------------- Encrypt the file -------------------------- */

		long startTime = System.currentTimeMillis();
		byte[] buffer = new byte[ENC_BUFFER_LEN];
		MessageDigest md = MessageDigest.getInstance("SHA-256");

		// Init txid key
		Cipher ecipher1 = null;
		try {
			ecipher1 = Cipher.getInstance(SYMALGORITHM, "BC");
			ecipher1.init(Cipher.ENCRYPT_MODE, txidKey, ivSpec);

		} catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
			Utils.getLogger().log(Level.SEVERE,"txid key init error", e);
			throw e;
		}

		// Init session key
		Cipher ecipher2 = null;
		try {
			ecipher2 = Cipher.getInstance(SYMALGORITHM, "BC");
			ecipher2.init(Cipher.ENCRYPT_MODE, SessionKey, ivSpec);

		} catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
			Utils.getLogger().log(Level.SEVERE,"session key init error", e);
			throw e;
		}

		// Create a input stream for reading file and output stream for writing
		// encrypted file.
		InputStream is = new FileInputStream(f);
		BufferedInputStream inStream = new BufferedInputStream(is);
		OutputStream os = new FileOutputStream(
				BitVaultConstants.PATH_FOR_ENCRYPTED_FILES + File.separator + f.getName() + append);
		DigestOutputStream digest = new DigestOutputStream(os, md);

		long workToBeDone = (f.length() / ENC_BUFFER_LEN) + 1;
		long workDone = 0;

		// read initial buffer
		int bytesRead = 0;
		try {
			bytesRead = inStream.read(buffer);
		} catch (IOException e) {
			inStream.close();
			os.flush();
			os.close();
			digest.close();
			Utils.getLogger().log(Level.SEVERE,"Buffer IO error", e);
			throw e;
		}

		// read file to buffer and encrypt
		while (bytesRead != -1) {

			byte[][] bWrite = null;
			int len = 0;

			// encrypt buffer
			try {
				bWrite = encryptBuffer(buffer, bytesRead, ecipher1, ecipher2);
				len = (new BigInteger(bWrite[1]).intValue());

			} catch (Exception e) {
				Utils.getLogger().log(Level.SEVERE,"Buffer encrypt error");
				inStream.close();
				os.flush();
				os.close();
				digest.close();
				throw e;
			}

			// write encrypted buffer to output file
			try {
				// write to encrypted file
				digest.write(bWrite[0], 0, len);
				// update UI
				this.updateProgress(++workDone, workToBeDone);
				// read again to buffer
				bytesRead = inStream.read(buffer);

			} catch (IOException e) {
				Utils.getLogger().log(Level.SEVERE,"Buffer IO error", e);
				inStream.close();
				os.flush();
				os.close();
				digest.close();
			}
			// check if task is cancelled
			if (isCancelled()) {
				break;
			}
		}
		inStream.close();
		os.flush();
		os.close();
		digest.close();
		Utils.getLogger().log(Level.FINEST, "File Encrypted: " + f.getName());

		// ---------------------------------------------------

		long stopTime = System.currentTimeMillis();
		Utils.getLogger().log(Level.FINEST, "Time to Encrypt: " + ((stopTime - startTime) / 1000.0) + " sec");

		// return SHA256 of output encrypted file
		return Hex.toHexString(md.digest());

	}

	/**
	 * Zips the attachments into one file
	 * 
	 * @param attachmentList
	 * @throws Exception
	 */
	public void ZipFiles(ArrayList<DraftAttachmentDTO> attachmentList) throws Exception {

		// ----------------- COMPRESSION -------------------------
		long startTime = System.currentTimeMillis();

		try {
			BufferedInputStream origin = null;
			FileOutputStream dest = new FileOutputStream(
					BitVaultConstants.PATH_FOR_ENCRYPTED_FILES + File.separator + ZippedFileName);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

			byte data[] = new byte[ZIP_BUFFER];
			long workDone = 0;
			long workToBeDone = 0;

			// find total work to be done
			for (int i = 0; i < attachmentList.size(); i++) {
				workToBeDone += attachmentList.get(i).getSize();
			}
			workToBeDone = (workToBeDone / ZIP_BUFFER) + 1;

			// compress each entry
			for (int i = 0; i < attachmentList.size(); i++) {

				// adding the file
				Utils.getLogger().log(Level.FINEST, "Adding: " + attachmentList.get(i));
				FileInputStream fi = new FileInputStream(attachmentList.get(i).getAttachPath());
				origin = new BufferedInputStream(fi, ZIP_BUFFER);

				// create new zip entry
				ZipEntry entry = new ZipEntry(attachmentList.get(i).getAttachName());
				out.putNextEntry(entry);

				int count;
				while ((count = origin.read(data, 0, ZIP_BUFFER)) != -1) {
					// write to file
					out.write(data, 0, count);
					// update progress
					this.updateProgress(++workDone, workToBeDone);
					// check if task is cancelled
					if (isCancelled()) {
						break;
					}
				}
				origin.close();

				// check if task is cancelled
				if (isCancelled()) {
					break;
				}
			}
			out.close();
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"Zip Error", e);
			throw e;
		}

		long stopTime = System.currentTimeMillis();
		Utils.getLogger().log(Level.FINEST, "Time to Zip: " + ((stopTime - startTime) / 1000.0) + "");

	}

	/**
	 * Gets AES Key
	 * 
	 * @param key
	 *            as string (hex)
	 * @return key as SecretKey object
	 * @throws Exception
	 */
	public SecretKey getAESKey(String key) throws Exception {

		SecretKey symKey = null;

		byte[] input = Hex.decode(key);
		symKey = new SecretKeySpec(input, "AES");

		return symKey;
	}

	/**
	 * Generates a Session Key
	 * 
	 * @return session key
	 * @throws Exception
	 */
	public Key genSessionKey() throws Exception {

		Key SessionKey = null;

		KeyGenerator SessionGenerator = KeyGenerator.getInstance("AES", "BC");
		SessionGenerator.init(256);
		SessionKey = SessionGenerator.generateKey();

		return SessionKey;
	}

	/**
	 * Symmetric encryption of data using AES256
	 * 
	 * @param input
	 * @param length
	 * @param ecipher
	 * @return returns encrypted data and size in result[0] and result[1]
	 * @throws Exception
	 */
	public byte[][] symEncryption(byte[] input, int length, Cipher ecipher) throws Exception {

		byte[][] result = new byte[2][];

		try {
			byte[] cipherText = new byte[ecipher.getOutputSize(length)];
			int ctLength = ecipher.update(input, 0, length, cipherText, 0);
			ctLength += ecipher.doFinal(cipherText, ctLength);

			result[0] = cipherText;
			result[1] = BigInteger.valueOf(ctLength).toByteArray();

		} catch (Exception e) {
			throw e;
		}

		return result;
	}

	/**
	 * Encrypts the Buffer with TXID and Session Key
	 * 
	 * @param input
	 * @param length
	 * @param ecipher1
	 * @param ecipher2
	 * @return returns encrypted data and size in mcipher[0] and mcipher[1]
	 * @throws Exception
	 */
	public byte[][] encryptBuffer(byte[] input, int length, Cipher txidCipher, Cipher skeyCipher) throws Exception {

		byte[][] mcipher = new byte[2][];

		try {
			mcipher = symEncryption(input, length, txidCipher);
			mcipher = symEncryption(mcipher[0], new BigInteger(mcipher[1]).intValue(), skeyCipher);
			mcipher[0] = Arrays.copyOfRange(mcipher[0], 0, new BigInteger(mcipher[1]).intValue());

		} catch (Exception e) {
			throw e;
		}

		return mcipher;
	}

	/**
	 * finds CRC32 of text
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public String getCRC(String text) throws Exception {

		try {
			byte[] checksumbyte = Strings.toByteArray(text);
			Checksum checksum = new CRC32();
			checksum.update(checksumbyte, 0, checksumbyte.length);
			return Long.toHexString(checksum.getValue());
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"chechksum error", e);
			throw e;
		}
	}

	/**
	 * send attachments to all the nodes
	 * 
	 * @param pbcModel
	 * @throws Exception
	 */
	public void sendAttachments(DataToPBCModel pbcModel) throws Exception {

		totalUploadedBytes = 0;
		long fileSize = new File(pbcModel.getFilepath()).length();
		ArrayList<Integer> nodesToSend = new ArrayList<Integer>();
		
		this.updateMessage(Messages.CONNECTING);
		
		// check nodes if file already uploaded
		int nodeToCheck = 1;
		int numNodesSuccess = 0;
		nodesToSend.clear();
		for (nodeToCheck = 1; nodeToCheck <= PBCRequest.NODES_PBC; nodeToCheck++) {
			try {
				Utils.getLogger().log(Level.FINEST, "\nChecking if file already uploaded : " + nodeToCheck);
				String result = PBCRequest.getAttachmentBlockStatus(pbcModel.getTag(), pbcModel.getHashTXId(),
						nodeToCheck);
				if ((result == null) || (result.equalsIgnoreCase(PBCRequest.BLOCK_CREATED))) {
					nodesToSend.add(nodeToCheck);
					Utils.getLogger().log(Level.FINEST, "not uploaded");
				} else if (result.equalsIgnoreCase(PBCRequest.BLOCK_SAVED) || 
						(result.equalsIgnoreCase(PBCRequest.BLOCK_INPROCESS))) {
					numNodesSuccess++;
					Utils.getLogger().log(Level.FINEST, "uploaded");
				}
				// else DELETED
			} catch (Exception e) {
				Utils.getLogger().log(Level.SEVERE,"Checking for attachment failed on NODE : " + nodeToCheck, e);
			}
			// check if thread is cancelled
			if ( isCancelled() ){
				throw new Exception("shutdown exception");
			}
		}
		
		// return if file already sent for txid
		if( numNodesSuccess >= PBCRequest.NODES_SENT_SUCCESS_THRES){
			// delete database entry
			if (!GlobalCalls.isNullOrEmptyStringCheck(MainPage.bitVaultToken)
					&& !GlobalCalls.isNullOrEmptyStringCheck(txid)) {
				DbConnection connection=new DbConnection();
				connection.updatDraftDtaOnDelete(MainPage.bitVaultToken, txid);
			}
			
			this.updateMessage(Messages.FILE_ALREADY_SENT);
			throw new Exception("Sending failed: file already sent");
		}

		// update the total progress 
		totalProgress = (double) (fileSize * nodesToSend.size());
		Utils.getLogger().log(Level.FINEST, "filesize " + fileSize);
		Utils.getLogger().log(Level.FINEST, "uploaded bytes " + totalUploadedBytes);
		Utils.getLogger().log(Level.FINEST, "total progress " + totalProgress);
		
		this.updateMessage(Messages.SENDING_ATT);

		// send to all PBC nodes
		int i;
		boolean successFlag = false;
		for (i = 0; i < nodesToSend.size(); i++) {
			int retryCount = 0;
			while (true) {
				try {
					// upload attachments
					String result = sendAttachmentsToNode(pbcModel, nodesToSend.get(i));
					if (result.equalsIgnoreCase("success")) {
						numNodesSuccess++;
						break;
					} else if (result.equalsIgnoreCase("error")) {
						this.updateMessage(Messages.CRC_MATCH_FAIL);
						throw new Exception("Error uploading: crc did not match");
					} else {
						this.updateMessage(Messages.UPLOADING_FAILED);
						throw new Exception("Error uploading: " + result);
					}
				} catch (Exception e) {
					// success if already sent to required number of nodes
					if (numNodesSuccess >= PBCRequest.NODES_SENT_SUCCESS_THRES) {
						successFlag = true;
						break;
					}
					// retry sending to the node if failed
					if (++retryCount > MAX_RETRY_SENDING_NODE)
						break;
					Utils.getLogger().log(Level.INFO,"\nRetrying sending to node " + nodesToSend.get(i) + " : retry " + retryCount);
					totalUploadedBytes = (fileSize * i);
				}
				// check if thread is cancelled
				if ( isCancelled() ){
					this.updateMessage(Messages.UPLOADING_FAILED);
					throw new Exception("upload cancelled");
				}
			}
			if (successFlag)
				break;
		}
		// check if sent to minimum number of nodes required for success
		if (numNodesSuccess < PBCRequest.NODES_SENT_SUCCESS_THRES){
			this.updateMessage(Messages.UPLOADING_FAILED);
			throw new Exception("sending failed: below threshold");
		}
			
	}

	/**
	 * Send attachments to PBC
	 * 
	 * @param pbcModel
	 * @param node
	 * @return
	 * @throws Exception
	 */
	public String sendAttachmentsToNode(DataToPBCModel pbcModel, int node) throws Exception {

		// set connection configuration
		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectTimeout(PBCRequest.CONNECT_TIMEOUT)
				.setSocketTimeout(PBCRequest.SOCKET_TIMEOUT)
				.setConnectionRequestTimeout(PBCRequest.REQUEST_TIMEOUT)
				.build();

		// create client
		httpClient = HttpClients.custom()
				.setDefaultRequestConfig(defaultRequestConfig).build();
		
		JSONObject obj = null;
		try {
			// get the URL for sending
			String URL = PBCRequest.URL_HTTPS_EXT;
			URL += PBCRequest.getUrlForNode(node);
			URL += PBCRequest.URL_SEND;

			httppost = new HttpPost(URL);
			String boundary = "-------------" + System.currentTimeMillis();
			httppost.setHeader("Content-type", "multipart/form-data; boundary=" + boundary);

			File fileToUpload = new File(pbcModel.getFilepath());

			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setBoundary(boundary);
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE).setCharset(Charset.forName("UTF-8"));

			builder.addPart(PBCRequest.KEY_TRANSACTION_ID,
					new StringBody(pbcModel.getHashTXId(), ContentType.TEXT_PLAIN));
			builder.addPart(PBCRequest.KEY_CRC, new StringBody(pbcModel.getCrc(), ContentType.MULTIPART_FORM_DATA));
			builder.addPart(PBCRequest.KEY_DATA_TAG,
					new StringBody(pbcModel.getTag(), ContentType.MULTIPART_FORM_DATA));
			builder.addPart(PBCRequest.KEY_RECEIVER,
					new StringBody(pbcModel.getReceiverWalletAdress(), ContentType.MULTIPART_FORM_DATA));
			builder.addPart(PBCRequest.KEY_FILE,
					new FileBody(fileToUpload, ContentType.DEFAULT_BINARY, fileToUpload.getName()));
			builder.addPart(PBCRequest.KEY_APPID, new StringBody(pbcModel.getAppId(), ContentType.MULTIPART_FORM_DATA));
			builder.addPart(PBCRequest.KEY_TIMESTAMP,
					new StringBody(String.valueOf(pbcModel.getTimeStamp()), ContentType.MULTIPART_FORM_DATA));
			builder.addPart(PBCRequest.KEY_SESSION_KEY,
					new StringBody(pbcModel.getEncryptedSessionKey(), ContentType.MULTIPART_FORM_DATA));
			builder.addPart(PBCRequest.KEY_PBC_ID,
					new StringBody(pbcModel.getPbcId(), ContentType.MULTIPART_FORM_DATA));
			builder.addPart(PBCRequest.KEY_DATA_HASH,
					new StringBody(pbcModel.getDataHash(), ContentType.MULTIPART_FORM_DATA));
			builder.addPart(PBCRequest.KEY_SENDER,
					new StringBody(pbcModel.getSenderAddress(), ContentType.MULTIPART_FORM_DATA));
			builder.addPart(PBCRequest.KEY_WEB_SERVER_KEY,
					new StringBody(pbcModel.getWebServerKeyValue(), ContentType.MULTIPART_FORM_DATA));

			HttpEntity reqEntity = builder.build();			
			CountingHttpEntity ce = new CountingHttpEntity(reqEntity, listener);
			httppost.setEntity(ce);
			
			Utils.getLogger().log(Level.FINEST, "\nNODE: " + node + " ----------------------------------------");
			Utils.getLogger().log(Level.FINEST, "Executing request " + httppost.getRequestLine());
			
			long startTime = System.currentTimeMillis();

			// thread to check if uploading has stalled
			clientExited = false;
			notifiedMsg = false;
			Thread t1 = new Thread(new Runnable() {
				public void run()
				{
					synchronized (waiterResponse) {

						while(!clientExited){
							try {
								waiterResponse.wait(UPLOADING_TIMEOUT);
								if( !notifiedMsg )
									throw new InterruptedException("interrupted");
								else
									notifiedMsg = false;
							} catch (InterruptedException e) {
								try {
									httppost.abort();
									httpClient.close();
									Utils.getLogger().log(Level.SEVERE, "client closed from waiter");
								} catch (IOException e1) {
									Utils.getLogger().log(Level.SEVERE, "client close error");
								} finally {
									clientExited = true;
								}
							}
						}
					}
				}});  
			t1.start();

			// execute 
			CloseableHttpResponse response = httpClient.execute(httppost);

			// close upload stall thread
			clientExited = true;
			notifiedMsg = true;
			synchronized (waiterResponse ){
				waiterResponse.notifyAll();
			}

			long stopTime = System.currentTimeMillis();
			Utils.getLogger().log(Level.FINEST, "Time to upload to node " + node + " : " + ((stopTime - startTime) / 1000.0) + " sec");

			try {
				// handle response
				int status = response.getStatusLine().getStatusCode();
				Utils.getLogger().log(Level.FINEST, "response: " + status);
				if (!(status >= 200 && status < 300)) {
					throw new Exception("Unexpected response status: " + status);
				}

				String jsonResult = EntityUtils.toString(response.getEntity(), "UTF-8");
				Utils.getLogger().log(Level.FINEST, "response : " + jsonResult);

				// parse JSON result
				try {
					JSONParser parser = new JSONParser();
					Object resultObject = parser.parse(jsonResult);
					obj = (JSONObject) resultObject;
					Utils.getLogger().log(Level.FINEST, KEY_STATUS + " : " + (String) obj.get(KEY_STATUS));
					Utils.getLogger().log(Level.FINEST, KEY_MESSAGE + " : " + (String) obj.get(KEY_MESSAGE));

				} catch (Exception e) {
					Utils.getLogger().log(Level.SEVERE,"json parsing error", e);
				}

			} finally {
				response.close();
			}
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"http connection error", e);
		} finally {
			try {
				// close upload stall thread
				clientExited = true;
				notifiedMsg = true;
				synchronized (waiterResponse ){
					waiterResponse.notifyAll();
				}
				httpClient.close();
			} catch (IOException e) {
				Utils.getLogger().log(Level.SEVERE,"http connection close error", e);
			}
		}
		
		return (String) obj.get("status");
	}

	/**
	 * class to monitor upload progress
	 * 
	 * @author root
	 *
	 */
	public class UploadProgress {

		private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(this, "progress");

		/**
		 * returns progress value
		 * 
		 * @return
		 */
		public double getProgress() {
			return progress.get();
		}

		/**
		 * returns progress property
		 * 
		 * @return
		 */
		public ReadOnlyDoubleProperty progressProperty() {
			return progress.getReadOnlyProperty();
		}

		/**
		 * sets progress
		 * 
		 * @param p
		 */
		public void setProgress(double p) {
			progress.set(p);
		}
	}
	
	/**
	 * Shuts down the http client
	 */
	public void shutdownClient(){
		try {
			if (httppost != null)
				httppost.abort();
			if (httpClient != null)
				httpClient.close();
			this.cancel();
			Utils.getLogger().log(Level.FINEST, "client closed/aborted");
		} catch (IOException e) {
			Utils.getLogger().log(Level.SEVERE,"client close/abort error", e);
		}
	}
}
