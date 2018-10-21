package com.bitVaultAttachment.models;

public class DataToPBCModel {
    private String tag="";
    private String hashTXId="";
    private String receiverWalletAdress="";
    private String filepath="";
    private String crc="";
    private String encryptedSessionKey = "";
    private String appId = "";
    private String pbcId = "";
    private long timeStamp ;
    private String txId = "";
    private String dataHash = "";
    private String senderAddress = "";
    private String webServerKeyValue = "";

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getEncryptedSessionKey() {
        return encryptedSessionKey;
    }

    public void setEncryptedSessionKey(String encryptedSessionKey) {
        this.encryptedSessionKey = encryptedSessionKey;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPbcId() {
        return pbcId;
    }

    public void setPbcId(String pbcId) {
        this.pbcId = pbcId;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getHashTXId() {
        return hashTXId;
    }

    public void setHashTXId(String hashTXId) {
        this.hashTXId = hashTXId;
    }

    public String getReceiverWalletAdress() {
        return receiverWalletAdress;
    }

    public void setReceiverWalletAdress(String receiverWalletAdress) {
        this.receiverWalletAdress = receiverWalletAdress;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getCrc() {
        return crc;
    }

    public void setCrc(String crc) {
        this.crc = crc;
    }

	public String getWebServerKeyValue() {
		return webServerKeyValue;
	}

	public void setWebServerKeyValue(String webServerKeyValue) {
		this.webServerKeyValue = webServerKeyValue;
	}

	public String getDataHash() {
		return dataHash;
	}

	public void setDataHash(String dataHash) {
		this.dataHash = dataHash;
	}


}
