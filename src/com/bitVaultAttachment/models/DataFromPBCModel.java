package com.bitVaultAttachment.models;

import org.json.simple.JSONObject;

public class DataFromPBCModel {

	private String message;
	private String status;
	private ResultSet resultSet = new ResultSet();
	
	/**
	 * Constructor
	 */
	public DataFromPBCModel(JSONObject header, JSONObject messageBlock ){
		
		setStatus((String) header.get("status"));
		setMessage((String) header.get("message"));
		
		getResultSet().setTimestamp((Long) messageBlock.get("timestamp"));
		getResultSet().setPbcId((String) messageBlock.get("pbcId"));
		getResultSet().setFileId((String) messageBlock.get("fileId"));
		getResultSet().setAppId((String) messageBlock.get("appId"));
		getResultSet().setHashTxid((String) messageBlock.get("transactionId"));
		getResultSet().setCrc((String) messageBlock.get("crc"));
		getResultSet().setReceiver((String) messageBlock.get("receiver"));
		getResultSet().setTag((String) messageBlock.get("tag"));
		getResultSet().setSessionKey((String) messageBlock.get("sessionKey"));
		getResultSet().setSender((String) messageBlock.get("sender"));
		getResultSet().setWebServerKey((String) messageBlock.get("webServerKey"));
	}

	public class ResultSet {
		
		private Long timestamp;
		private String pbcId;
		private String fileId;
		private String appId;
		private String hashTxid;
		private String crc;
		private String receiver;
		private String tag;
		private String sessionKey;
		private String sender;
		private String webServerKey;
		
		public String getHashTxid() {
			return hashTxid;
		}

		public void setTimestamp(String string) {
			// TODO Auto-generated method stub
			
		}

		public void setHashTxid(String hashTxid) {
			this.hashTxid = hashTxid;
		}

		public String getSender() {
			return sender;
		}

		public void setSender(String sender) {
			this.sender = sender;
		}

		public String getPbcId() {
			return pbcId;
		}

		public void setPbcId(String pbcId) {
			this.pbcId = pbcId;
		}

		public String getFileId() {
			return fileId;
		}

		public void setFileId(String fileId) {
			this.fileId = fileId;
		}

		public String getAppId() {
			return appId;
		}

		public void setAppId(String appId) {
			this.appId = appId;
		}

		public String getCrc() {
			return crc;
		}

		public void setCrc(String crc) {
			this.crc = crc;
		}

		public String getReceiver() {
			return receiver;
		}

		public void setReceiver(String receiver) {
			this.receiver = receiver;
		}

		public String getTag() {
			return tag;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}

		public String getSessionKey() {
			return sessionKey;
		}

		public void setSessionKey(String sessionKey) {
			this.sessionKey = sessionKey;
		}

		public String getWebServerKey() {
			return webServerKey;
		}

		public void setWebServerKey(String webServerKey) {
			this.webServerKey = webServerKey;
		}

		public Long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(Long timestamp) {
			this.timestamp = timestamp;
		}
	}
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public void setResultSet(ResultSet resultSet) {
		this.resultSet = resultSet;
	}
}
