package com.bitVaultAttachment.models;

import java.sql.Timestamp;

import javafx.scene.control.Hyperlink;

/**
 * Created by vvdn on 5/18/2017.
 */
public class NotificationListDTO {
  
	private String bitVaultToken;
    private String deviceId;
    private String senderAddress;
    private Timestamp date;
    private short isLocked;
    private short isDownloaded;
    private short isFrom;
    private short from;
    private String hashOfTxnId;
    private String pathOfEncryptedFile;
    private String pathOfUnEncryptedFile;
    private String sessionKey;
    private String TxnId;
    private Hyperlink downloadLink;
    private long size;
    private String notificationTag;
    private String recieverAddress;
  
    public String getNotificationTag() {
		return notificationTag;
	}

	public void setNotificationTag(String notificationTag) {
		this.notificationTag = notificationTag;
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

	public short getIsLocked() {
		return isLocked;
	}

	public void setIsLocked(short isLocked) {
		this.isLocked = isLocked;
	}

	public short getIsDownloaded() {
		return isDownloaded;
	}

	public void setIsDownloaded(short isDownloaded) {
		this.isDownloaded = isDownloaded;
	}

	public short getIsFrom() {
		return isFrom;
	}

	public void setIsFrom(short isFrom) {
		this.isFrom = isFrom;
	}

	public short getFrom() {
		return from;
	}

	public void setFrom(short from) {
		this.from = from;
	}

	public Hyperlink getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(Hyperlink downloadLink) {
        this.downloadLink = downloadLink;
    }

    public String getHashOfTxnId() {
        return hashOfTxnId;
    }

    public void setHashOfTxnId(String hashOfTxnId) {
        this.hashOfTxnId = hashOfTxnId;
    }

    public String getPathOfEncryptedFile() {
        return pathOfEncryptedFile;
    }

    public void setPathOfEncryptedFile(String pathOfEncryptedFile) {
        this.pathOfEncryptedFile = pathOfEncryptedFile;
    }

    public String getPathOfUnEncryptedFile() {
        return pathOfUnEncryptedFile;
    }

    public void setPathOfUnEncryptedFile(String pathOfUnEncryptedFile) {
        this.pathOfUnEncryptedFile = pathOfUnEncryptedFile;
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

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getRecieverAddress() {
		return recieverAddress;
	}

	public void setRecieverAddress(String recieverAddress) {
		this.recieverAddress = recieverAddress;
	}

	public Timestamp getDate() {
		return date;
	}

	public void setDate(Timestamp date) {
		this.date = date;
	}
}
