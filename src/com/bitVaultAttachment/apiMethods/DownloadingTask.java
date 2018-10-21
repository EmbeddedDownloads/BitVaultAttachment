package com.bitVaultAttachment.apiMethods;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.bitVaultAttachment.constant.BitVaultConstants;
import com.bitVaultAttachment.constant.GlobalCalls;
import com.bitVaultAttachment.constant.Messages;
import com.bitVaultAttachment.database.DbConnection;
import com.bitVaultAttachment.models.DataFromPBCModel;

import javafx.application.Platform;
import javafx.concurrent.Task;

public class DownloadingTask extends Task<Void> {
	
	private String bitvaultToken;
	private String txidHash;
	private String tag;
	private String receiverWalletAddress;
	
	private final long DOWNLOAD_PROGRESS_STEP = 4096;//2097152;
	private final String FILE_NAME_EXT = ".zip_enc";
	
	CloseableHttpClient httpClient;
	HttpGet httpget;
	HttpPost httppost;

	/**
	 * Constructor
	 * 
	 * @param txidHash
	 * @param tag
	 * @param receiverWalletAddress
	 */
	public DownloadingTask(String bitvaultToken, String txidHash, String tag, String receiverWalletAddress) {
		// set parameters
		this.bitvaultToken = bitvaultToken;
		this.txidHash = txidHash;
		this.tag = tag;
		this.receiverWalletAddress = receiverWalletAddress;
	}

	@Override
	protected Void call() throws Exception {
		
		// Update message
		this.updateMessage(Messages.DOWNLOADING_ATT);
		
		// Downloading task
		DataFromPBCModel pbcData = null;
		try {
			if (!isCancelled()) {
				pbcData = downloadAttachment();				
			}
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"File download error", e);
			throw e;
		}
		
		// check CRC
		String mCRC = null;
		try {
			if (!isCancelled()) {
				String resultFileHash = getFileHash();
				Utils.getLogger().log(Level.FINEST,"File Hash : " + resultFileHash);
				
				String msgforCRC = 	  
						  pbcData.getResultSet().getTag().trim() 			+ "|$$|" 
						+ pbcData.getResultSet().getHashTxid().trim() 		+ "|$$|" 
						+ pbcData.getResultSet().getReceiver().trim() 		+ "|$$|" 
						+ resultFileHash.trim() 							+ "|$$|" 
						+ pbcData.getResultSet().getSessionKey().trim()	+ "|$$|" 
						+ pbcData.getResultSet().getPbcId().trim()			+ "|$$|" 
						+ pbcData.getResultSet().getAppId().trim() 		+ "|$$|" 
						+ pbcData.getResultSet().getTimestamp() 	+ "|$$|"
						+ pbcData.getResultSet().getSender().trim()  		+ "|$$|"
						+ pbcData.getResultSet().getWebServerKey().trim();

				Utils.getLogger().log(Level.FINEST,msgforCRC);

				// find CRC
				try {
					mCRC = getCRC(msgforCRC);
					Utils.getLogger().log(Level.FINEST,"CRC: " + mCRC);
				} catch (Exception e) {
					Utils.getLogger().log(Level.SEVERE,"crc exception", e);
				}
				// check if CRC is matched
				if( mCRC != null && mCRC.equalsIgnoreCase(pbcData.getResultSet().getCrc()) ){
					Utils.getLogger().log(Level.FINEST,"CRC matched");
				}
				else{
					throw new Exception ("CRC match FAILED");
				}
			}
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"File download error", e);
			throw e;
		}
		
		// Update message
		this.updateMessage(Messages.DOWNLOADING_ACK);
		// Downloading task
		try {
			if (!isCancelled()) {
				acknowledgeNodes(pbcData.getResultSet().getHashTxid(),
						pbcData.getResultSet().getTag(),
						mCRC);
			}
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"Acknowledgment error", e);
			throw e;
		}
		
		// Update DB and set downloaded
		DbConnection connection = new DbConnection();
		try {
			if(!GlobalCalls.isNullOrEmptyStringCheck(bitvaultToken) && 
					!GlobalCalls.isNullOrEmptyStringCheck(txidHash)){
				connection.updateNotificationDtaOnDownloading(bitvaultToken, txidHash);
				
				Platform.runLater(new Runnable() {
		            @Override public void run() {
		            	if (BitVaultConstants.mGeneralManagerCallbacks != null) {
							BitVaultConstants.mGeneralManagerCallbacks.inboxActionCallback();
						} 
		            }
		        });
			}
		} catch (ClassNotFoundException | SQLException e1) {
			Utils.getLogger().log(Level.SEVERE,"database update error", e1);
			throw e1;
		}

		return null;
	}
	
	/**
	 * download attachments from any one node
	 * @throws Exception
	 */
	public DataFromPBCModel downloadAttachment() throws Exception {
		
		DataFromPBCModel pbcData = null;
		for (int nodeToDownload = 1; nodeToDownload <= PBCRequest.NODES_PBC; nodeToDownload++) {

			try {
				pbcData = downloadAttachmentFromNode( txidHash, tag, receiverWalletAddress, nodeToDownload);
				Utils.getLogger().log(Level.INFO,"file downloaded from node " + nodeToDownload);
				break;
			} catch (Exception e) {
				Utils.getLogger().log(Level.SEVERE,"file download exception : node " + nodeToDownload, e);
				if ( nodeToDownload == PBCRequest.NODES_PBC)
					throw new Exception("download exception: queried all nodes");
			}
			// check if thread is cancelled
			if ( isCancelled() )
				throw new Exception("shutdown exception");
		}
		return pbcData;
	}
	
	/**
	 * Acknowledges nodes for deletion
	 * @param pbcData
	 * @throws Exception
	 */
	public void acknowledgeNodes(String hashTxid, String tag, String crc) throws Exception {
		
		int ackNodes = 0;
		for (int nodeToAck = 1; nodeToAck <= PBCRequest.NODES_PBC; nodeToAck++) {
			try {
				String result = ackDownloadedAttachment(hashTxid, tag, crc, nodeToAck);
				if( result != null && result.equalsIgnoreCase("success")){
					Utils.getLogger().log(Level.INFO,"file deleted from node " + nodeToAck);
					ackNodes++;
				}
			} catch (Exception e) {
				Utils.getLogger().log(Level.SEVERE,"ACK exception : node " + nodeToAck, e);
			}
			// check if thread is cancelled
			if ( isCancelled() )
				throw new Exception("shutdown exception");
		}
		if( ackNodes < PBCRequest.NODES_SENT_SUCCESS_THRES)
			throw new Exception("acknowledment failed");
	}

	/**
	 * Download attachment from node and return model
	 * @param txid
	 * @param tag
	 * @param receiverAddress
	 * @param node
	 * @return
	 * @throws Exception
	 */
	public DataFromPBCModel downloadAttachmentFromNode(String hashTxid, String tag, String receiverAddress, int node) 
			throws Exception{
		
		String hashTxidDecoded=Hex.toHexString(Base64.decode(hashTxid));
		// file to save
		File downloadFile = new File(BitVaultConstants.PATH_FOR_DOWNLOADED_FILES 
				+ File.separator + hashTxidDecoded + FILE_NAME_EXT);
		
		// Get attachment data from PBC
		System.out.println(hashTxid);
		DataFromPBCModel pbcDownloadModel = PBCRequest.getAttachmentLink(hashTxid.trim(), tag.trim(), receiverAddress.trim(), node);
		if (pbcDownloadModel != null && pbcDownloadModel.getStatus().equalsIgnoreCase("success")){
			Utils.getLogger().log(Level.FINEST,"attachment link returned");
		}
		else {
			throw new Exception("attachment link error");
		}
		
		// get the URL for sending
		String URL = PBCRequest.URL_HTTPS_EXT;
		URL += PBCRequest.getUrlForNode(node);
		URL += PBCRequest.URL_GET_FILE + pbcDownloadModel.getResultSet().getFileId();

		// Set configuration
		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectTimeout(PBCRequest.CONNECT_TIMEOUT)
				.setSocketTimeout(PBCRequest.SOCKET_TIMEOUT)
				.setConnectionRequestTimeout(PBCRequest.REQUEST_TIMEOUT)
				.build();

		// create http client
		httpClient = HttpClients.custom()
				.setDefaultRequestConfig(defaultRequestConfig)
				.build();
		
		CloseableHttpResponse response = null;
		try {
			httpget = new HttpGet(URL);
			response = httpClient.execute(httpget);

			Utils.getLogger().log(Level.FINEST,"\nNODE " + node + " ----------------------------------------");
		
			// handle response
			int status = response.getStatusLine().getStatusCode();
			Utils.getLogger().log(Level.FINEST,"status : " + status);
			if (!(status >= 200 && status < 300)) {
				throw new Exception("Unexpected response status: " + status);
			}

			HttpEntity entity = response.getEntity();
			long fileSize = entity.getContentLength();
			Utils.getLogger().log(Level.FINEST,"File Size: " + fileSize);

			// download file from PBC
			int inByte;
			InputStream is = entity.getContent();
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(downloadFile));

			long startTime = System.currentTimeMillis();
			long workToBeDone = fileSize;
			long workDone = 0;

			// if there is file to be downloaded
			if (entity != null) {

				try {
					while ((inByte = is.read()) != -1) {
						// write to file
						bos.write(inByte);
						// update progress
						workDone++;
						if ((workDone % DOWNLOAD_PROGRESS_STEP) == 0) {
							updateProgress(workDone, workToBeDone);
						}
						// check if thread is cancelled
						if ( isCancelled() ){
							throw new Exception("shutdown exception");
						}
					}
				} catch (Exception e) {
					Utils.getLogger().log(Level.SEVERE,"file download read/write error", e);	
					is.close();
					bos.close();
					throw e;
				} finally {
					is.close();
					bos.close();
				}
			}
			long stopTime = System.currentTimeMillis();
			Utils.getLogger().log(Level.FINEST,"Time to download : " + ((stopTime - startTime) / 1000.0) + " sec");
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"http connection error", e);
			httpClient.close();
			response.close();
			throw e;
		}
		
		// close the client
		try {
			httpClient.close();
			response.close();
			Utils.getLogger().log(Level.SEVERE,"connection closed");
		} catch (IOException e) {
			Utils.getLogger().log(Level.SEVERE,"http connection close error", e);
			throw e;
		}
		
		return pbcDownloadModel;
	}
	
	/**
	 * Acknowledge attachment is downloaded from for deletion.
	 * 
	 * @param txid
	 * @param tag
	 * @param crc
	 * @param node
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String ackDownloadedAttachment(String hashTxid, String tag, String crc, int node) {

		Utils.getLogger().log(Level.FINEST,"/n ackDownloadedAttachment API");
		JSONObject sendObj = new JSONObject();
		sendObj.put("transactionId", hashTxid);
		sendObj.put("tag", tag);
		sendObj.put("crc", crc);
		Utils.getLogger().log(Level.FINEST,sendObj.toJSONString());

		// get the URL for sending
		String URL = PBCRequest.URL_HTTPS_EXT;
		URL += PBCRequest.getUrlForNode(node);
		URL += PBCRequest.URL_SEND_ACK;

		// Set configuration
		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectTimeout(PBCRequest.CONNECT_TIMEOUT)
				.setSocketTimeout(PBCRequest.SOCKET_TIMEOUT)
				.setConnectionRequestTimeout(PBCRequest.REQUEST_TIMEOUT).build();

		// create http client
		CloseableHttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(defaultRequestConfig)
				.build();
		
		JSONObject obj = null;
		try {
			httppost = new HttpPost(URL);

			// add header and body
			StringEntity params = new StringEntity(sendObj.toJSONString());
			httppost.addHeader("content-type", "application/json");
			httppost.setEntity(params);

			// connect and post
			HttpResponse result = httpClient.execute(httppost);

			// handle response
			int status = result.getStatusLine().getStatusCode();
			if (!(status >= 200 && status < 300)) {
				throw new Exception("Unexpected response status: " + status);
			}

			// get result
			String jsonResult = EntityUtils.toString(result.getEntity(), "UTF-8");
			Utils.getLogger().log(Level.FINEST,"response : " + jsonResult);

			// parse JSON result
			try {
				JSONParser parser = new JSONParser();
				Object resultObject = parser.parse(jsonResult);
				obj = (JSONObject) resultObject;
				Utils.getLogger().log(Level.FINEST,"status : " + (String) obj.get("status"));
				Utils.getLogger().log(Level.FINEST,"message : " + (String) obj.get("message"));
				Utils.getLogger().log(Level.FINEST,"resulSet : " + (String) obj.get("resultSet"));
			} catch (Exception e) {
				Utils.getLogger().log(Level.SEVERE,"json parsing error", e);
			}
		} catch (Exception ex) {
			Utils.getLogger().log(Level.SEVERE,"http connection exception", ex);
		}
		return (String) obj.get("status");
	}
	
	/**
	 * Returns the SHA256 of a file
	 * @return
	 * @throws Exception
	 */
	public String getFileHash() throws Exception {
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			Utils.getLogger().log(Level.SEVERE,"hash function init error", e);
			throw e;
		}
		
        FileInputStream fis = null;
        String filePath = BitVaultConstants.PATH_FOR_DOWNLOADED_FILES 
				+ File.separator + Hex.toHexString(Base64.decode(txidHash)) + FILE_NAME_EXT;
		try {
			fis = new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			Utils.getLogger().log(Level.SEVERE,"file open error for finding hash", e);
		}
        
        byte[] dataBytes = new byte[1024];
        int nread = 0; 
        
        while ((nread = fis.read(dataBytes)) != -1) {
          md.update(dataBytes, 0, nread);
        };
        byte[] mdbytes = md.digest();
        
        return Hex.toHexString(mdbytes);
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
	 * Shuts down the http client
	 */
	public void shutdownClient(){
		try {
			if (httpget != null)
				httpget.abort();
			if (httpClient != null)
				httpClient.close();
			this.cancel();
			Utils.getLogger().log(Level.INFO,"client closed/aborted");
		} catch (IOException e) {
			Utils.getLogger().log(Level.SEVERE,"client close/abort error", e);
		}
	}

	public String getBitvaultToken() {
		return bitvaultToken;
	}

	public void setBitvaultToken(String bitvaultToken) {
		this.bitvaultToken = bitvaultToken;
	}
}
