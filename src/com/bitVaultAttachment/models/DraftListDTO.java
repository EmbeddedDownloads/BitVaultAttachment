package com.bitVaultAttachment.models;

import java.util.Date;

public class DraftListDTO {
	
	    private String bitVaultToken;
	    private String deviceId;
	    private String senderAddress;
	    private Date date;
	    private short isEncrypteed=0;
	    private short isSending=0;
	    private short isCompressed=0;
	    private String pathOfCopressedFilee;
	    private String sessionKey;
	    private String TxnId;
	    private int size;
	    private long timeStampValue;
	    private String fileHash;
	      
		public String getFileHash() {
			return fileHash;
		}
		public void setFileHash(String fileHash) {
			this.fileHash = fileHash;
		}
		public long getTimeStampValue() {
			return timeStampValue;
		}
		public void setTimeStampValue(long timeStampValue) {
			this.timeStampValue = timeStampValue;
		}
		public String getBitVaultToken() {
			return bitVaultToken;
		}
		public void setBitVaultToken(String bitVaultToken) {
			this.bitVaultToken = bitVaultToken;
		}
		public String getDeviceId() {
			return deviceId;
		}
		public void setDeviceId(String deviceId) {
			this.deviceId = deviceId;
		}
		public String getSenderAddress() {
			return senderAddress;
		}
		public void setSenderAddress(String senderAddress) {
			this.senderAddress = senderAddress;
		}
		public Date getDate() {
			return date;
		}
		public void setDate(Date date) {
			this.date = date;
		}
		public short getIsEncrypteed() {
			return isEncrypteed;
		}
		public void setIsEncrypteed(short isEncrypteed) {
			this.isEncrypteed = isEncrypteed;
		}
		public short getIsSending() {
			return isSending;
		}
		public void setIsSending(short isSending) {
			this.isSending = isSending;
		}
		public short getIsCompressed() {
			return isCompressed;
		}
		public void setIsCompressed(short isCompressed) {
			this.isCompressed = isCompressed;
		}
		public String getPathOfCopressedFilee() {
			return pathOfCopressedFilee;
		}
		public void setPathOfCopressedFilee(String pathOfCopressedFilee) {
			this.pathOfCopressedFilee = pathOfCopressedFilee;
		}
		public String getSessionKey() {
			return sessionKey;
		}
		public void setSessionKey(String sessionKey) {
			this.sessionKey = sessionKey;
		}
		public String getTxnId() {
			return TxnId;
		}
		public void setTxnId(String txnId) {
			TxnId = txnId;
		}
		public int getSize() {
			return size;
		}
		public void setSize(int size) {
			this.size = size;
		}
}
