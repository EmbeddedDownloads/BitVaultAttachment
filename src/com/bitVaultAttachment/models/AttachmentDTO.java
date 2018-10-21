package com.bitVaultAttachment.models;

public class AttachmentDTO {
	
	private String attachName;
	private int attachId;
	private String hashTxId;
	private long size;
	
	private String attachPath;
	
	
	public String getAttachPath() {
		return attachPath;
	}
	public void setAttachPath(String attachPath) {
		this.attachPath = attachPath;
	}
	public String getAttachName() {
		return attachName;
	}
	public void setAttachName(String attachName) {
		this.attachName = attachName;
	}
	public int getAttachId() {
		return attachId;
	}
	public void setAttachId(int attachId) {
		this.attachId = attachId;
	}
	public String getHashTxId() {
		return hashTxId;
	}
	public void setHashTxId(String hashTxId) {
		this.hashTxId = hashTxId;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	
	
	

}
