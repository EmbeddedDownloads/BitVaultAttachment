package com.bitVaultAttachment.ui;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.bitVaultAttachment.constant.GlobalCalls;
import com.bitVaultAttachment.controller.DraftPage;
import com.bitVaultAttachment.controller.EventHandlerUI;
import com.bitVaultAttachment.controller.InboxPage;
import com.bitVaultAttachment.database.DbConnection;
import com.bitVaultAttachment.models.AttachmentDTO;
import com.bitVaultAttachment.models.DraftListDTO;
import com.bitVaultAttachment.models.NotificationListDTO;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class NotificationBar {

	private boolean isDownloaded = false;
	private boolean isDownloading = false;
	private boolean isLocked = true;
	private boolean isShowAttachments = false;
	private Date date;
	private Timestamp dateInTimeStamp;

	private String fromAddress;
	private String bitVaultToken;
	private String pathOfUnEncryptedFiles;

	private String hashOfTxnId;
	private String txnId;
	private String sessionKey;
	private HBox bar, spacerbar1, spacerbar2;
	private VBox container;
	private Text textFrom, textHashTxnId, textTime;
	private Text textDeleteImage, textDownloadImage, textLockedImage, textFolderImage, textRetryImage;

	private long sizeAttachments;
	private int numAttachments = 5;
	private VBox attachmentContainer = new VBox(3);

	private String notificationTag;
	private String recieverAddress;

	/**
	 * Notification bar for Inbox with DTO
	 * 
	 * @param Inbox
	 * @param model
	 * @param handler
	 * @param primaryStage
	 */
	public NotificationBar(InboxPage Inbox, NotificationListDTO model, Stage primaryStage) {
		this.fromAddress = model.getSenderAddress();
		this.setSizeAttachments(model.getSize());
		this.hashOfTxnId = model.getHashOfTxnId();
		this.bitVaultToken = model.getBitVaultToken();
		this.notificationTag=model.getNotificationTag();
		this.pathOfUnEncryptedFiles=model.getPathOfUnEncryptedFile();
		this.recieverAddress=model.getRecieverAddress();

		this.dateInTimeStamp = model.getDate();

		// copy model to UI
		createBarMain();
		setNotificationBarHandlers(Inbox, primaryStage);
	}

	/**
	 * Notification Bar for Draft Page
	 * 
	 * @param Draft
	 * @param test
	 * @param handler
	 * @param primaryStage
	 */
	public NotificationBar(DraftPage Draft, DraftListDTO model, Stage primaryStage) {

		this.fromAddress = model.getTxnId();
		this.setSizeAttachments(model.getSize());
		this.date = model.getDate();
		this.bitVaultToken = model.getBitVaultToken();
		this.txnId=model.getTxnId();
		this.sessionKey=model.getSessionKey();

		createBarMain();
		setNotificationBarHandlers(Draft, primaryStage);
	}

	/**
	 * Notification Bar for Draft Page with DTO
	 * 
	 * @param Draft
	 * @param model
	 * @param handler
	 * @param primaryStage
	 */
	public NotificationBar(DraftPage Draft, NotificationListDTO model, Stage primaryStage) {

		// copy model to UI
		createBarMain();
		setNotificationBarHandlers(Draft, primaryStage);
	}

	/**
	 * Creates Main Bar Object
	 */
	public void createBarMain() {

		// spacer bars
		spacerbar1 = new HBox(5);
		spacerbar1.setAlignment(Pos.CENTER_LEFT);

		spacerbar2 = new HBox(5);
		spacerbar2.setAlignment(Pos.CENTER_RIGHT);
		spacerbar2.setMinWidth(320);

		// sender address
		textFrom = new Text();
		textFrom.setText(fromAddress.trim());
		textFrom.getStyleClass().add("nameColumn");
		textFrom.setTextAlignment(TextAlignment.LEFT);		
	
		// Hash of Txid
		textHashTxnId = new Text();
		textHashTxnId.setText(hashOfTxnId!=null?("XXX"+hashOfTxnId.substring(hashOfTxnId.length()-5,hashOfTxnId.length()).trim()):"");
		textHashTxnId.getStyleClass().add("nameColumn");
		textHashTxnId.setTextAlignment(TextAlignment.LEFT);
		
		HBox hboxFrom = new HBox(textFrom);
		hboxFrom.setAlignment(Pos.CENTER_LEFT);
		HBox.setMargin(textFrom, new Insets(0, 10, 0, 10));
		HBox.setMargin(textHashTxnId, new Insets(0, 10, 0, 20));
		HBox.setHgrow(hboxFrom, Priority.ALWAYS);
		hboxFrom.setMinWidth(150);

		HBox hboxHashTxnId = new HBox(textHashTxnId);
		hboxHashTxnId.setAlignment(Pos.CENTER_RIGHT);
		HBox.setMargin(textHashTxnId, new Insets(0, 10, 0, 5));
		HBox.setHgrow(hboxHashTxnId, Priority.ALWAYS);
		
		//wrapping
		textFrom.wrappingWidthProperty().bind(spacerbar1.widthProperty().subtract(10).subtract(hboxHashTxnId.widthProperty()));

		// Date/Time
		textTime = new Text();
		Date dt = dateInTimeStamp!=null?new Date(dateInTimeStamp.getTime()):new Date(); 
		SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM d, yyyy hh:mm");
		
		textTime.setText(dt != null ? dateFormatter.format(dt) : "");	
		textTime.getStyleClass().addAll("nameColumn", "timeColumn");
		textTime.setTextAlignment(TextAlignment.LEFT);
		
		HBox hboxTime = new HBox(textTime);
		hboxTime.setAlignment(Pos.CENTER_RIGHT);
		HBox.setMargin(textTime, new Insets(0, 50, 0, 50));
		HBox.setHgrow(hboxTime, Priority.ALWAYS);

		// create the delete image text
		textDeleteImage = new Text();
		textDeleteImage.setText("e");
		textDeleteImage.setStyle(" -fx-cursor:hand;");
		textDeleteImage.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 18));
		HBox.setMargin(textDeleteImage, new Insets(5, 12, 5, 5));

		// create the download image text
		textDownloadImage = new Text();
		textDownloadImage.setText("k");
		textDownloadImage.setStyle(" -fx-cursor:hand;");
		textDownloadImage.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 18));
		HBox.setMargin(textDownloadImage, new Insets(5, 12, 5, 5));
		
		// add nodes to spacerbars
		spacerbar1.getChildren().addAll(hboxFrom, hboxHashTxnId);
		spacerbar2.getChildren().addAll(hboxTime, textDeleteImage, textDownloadImage);

		// primary hbox for notification bar
		bar = new HBox(5);
		bar.setMinHeight(40);
		bar.setAlignment(Pos.CENTER);
		bar.getChildren().addAll(spacerbar1, spacerbar2);
		HBox.setHgrow(spacerbar1, Priority.ALWAYS);
		
		// container for whole notification bar
		container = new VBox(0, bar);
		container.setPrefWidth(700);
		container.setPrefHeight(50);
		container.setAlignment(Pos.CENTER);
		container.getStyleClass().add("containerClass");
	}

	/**
	 * set notification handler for Inbox Page
	 * 
	 * @param Inbox
	 * @param handler
	 * @param primaryStage
	 */
	public void setNotificationBarHandlers(InboxPage Inbox, Stage primaryStage) {
		// set event handlers
		Inbox.inboxEvents.setBarClickEvent(this);
		Inbox.inboxEvents.setDeleteClickEvent(Inbox, this, primaryStage);
		Inbox.inboxEvents.setDownloadClickEvent(this);
	}

	/**
	 * set notification handler for Draft Page
	 * 
	 * @param Draft
	 * @param handler
	 * @param primaryStage
	 */
	public void setNotificationBarHandlers(DraftPage Draft, Stage primaryStage) {
		// set event handlers
		Draft.draftEvents.setBarClickEvent(this);
		Draft.draftEvents.setDeleteClickEvent(Draft, this, primaryStage);
		Draft.draftEvents.setDownloadClickEvent(this);
	}

	/**
	 * creates the new attachment list to be added to notification bar
	 * @param bitVaultToken
	 * @param hashTxId
	 */
	public void getAttachmentList(String bitVaultToken, String hashTxId) {

		// Add to attachments list
		DbConnection connection = new DbConnection();
		ArrayList<AttachmentDTO> attachmentDTOList = null;
		try {
			if(!GlobalCalls.isNullOrEmptyStringCheck(bitVaultToken)
					&& !GlobalCalls.isNullOrEmptyStringCheck(hashTxId))
				attachmentDTOList = (ArrayList<AttachmentDTO>) connection.fetchAttachmentData4Inbox(bitVaultToken,
						hashTxId);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}

		ArrayList<AttachmentBar> attachments = new ArrayList<>();
		int counter = 0;
		for (AttachmentDTO attachmentDTO : attachmentDTOList) {

			attachments.add(new AttachmentBar(attachmentDTO.getAttachName(), attachmentDTO.getAttachId(),attachmentDTO.getSize()));
			attachmentContainer.getChildren().add((attachments.get(counter++)).getBar());
		}
		// add vbox
		attachmentContainer.setAlignment(Pos.CENTER);
	}

	/**
	 * set bar as not downloaded
	 * @param handler
	 */
	public void setNotDownloaded(EventHandlerUI handler) {

		isDownloaded = false;
		spacerbar2.getChildren().remove(1);

		// create the delete image text
		textDeleteImage = new Text();
		textDeleteImage.setText("e");
		textDeleteImage.setStyle(" -fx-cursor:hand;");
		textDeleteImage.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 18));
		HBox.setMargin(textDeleteImage, new Insets(5, 12, 5, 5));

		textFrom.setFill(Color.web("#2EB2EB"));

		spacerbar2.getChildren().add(1, textDeleteImage);
		isDownloaded = false;
		isDownloading = false;
	}

	/**
	 * set bar as downloaded
	 * @param Inbox
	 * @param handler
	 * @param primaryStage
	 */
	public void setDownloaded(InboxPage Inbox, EventHandlerUI handler, Stage primaryStage) {

		spacerbar2.getChildren().remove(1);
		spacerbar2.getChildren().remove(1);

		// create the delete image text
		textDeleteImage = new Text();
		textDeleteImage.setText("e");
		textDeleteImage.setStyle(" -fx-cursor:hand;");
		textDeleteImage.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 18));
		HBox.setMargin(textDeleteImage, new Insets(5, 12, 5, 5));

		// create the delete image text
		textLockedImage = new Text();
		textLockedImage.setText("i");
		textLockedImage.setStyle(" -fx-cursor:hand;");
		textLockedImage.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 18));
		HBox.setMargin(textLockedImage, new Insets(5, 12, 5, 5));

		textFrom.getStyleClass().add("greyNameColumn");

		handler.setDeleteClickEvent(Inbox, this, primaryStage);
		handler.setLockClickEvent(this);

		spacerbar2.getChildren().addAll(textDeleteImage, textLockedImage);

		isDownloaded = true;
		isDownloading = false;
	}

	/**
	 * unlocked bar
	 * @param handler
	 */
	public void setUnlocked(EventHandlerUI handler) {

		// change locked status to false
		isLocked = false;

		// set unlocked image
		textFolderImage = new Text();
		textFolderImage.setText("p");
		textFolderImage.setStyle(" -fx-cursor:hand;");
		textFolderImage.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 18));
		HBox.setMargin(textFolderImage, new Insets(5, 12, 5, 5));

		textFrom.setStyle(" -fx-cursor:hand;");

		spacerbar2.getChildren().remove(2);
		spacerbar2.getChildren().addAll(textFolderImage);

		// set open folder click event
		handler.setOpenFolderClickEvent(this);
	}

	/**
	 * set bar as draft
	 * @param handler
	 */
	public void setDraft(EventHandlerUI handler) {

		isLocked = false; // for drop down pane
		textRetryImage = new Text();
		textRetryImage.setText("n");
		textRetryImage.setStyle(" -fx-cursor:hand;");
		textRetryImage.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 18));
		HBox.setMargin(textRetryImage, new Insets(5, 12, 5, 5));

		textFrom.setStyle(" -fx-cursor:hand;");

		spacerbar2.getChildren().remove(2);
		spacerbar2.getChildren().addAll(textRetryImage);

		handler.setRetryClickEvent(this);
	}

	/* 
	 * Getters and Setters 
	 * 
	 * */
	public VBox getContainer() {
		return container;
	}

	public VBox getAttachmentContainer() {
		return attachmentContainer;
	}

	public Date getDate() {
		return date;
	}

	public boolean getDownloadedState() {
		return isDownloaded;
	}

	public boolean getDownloadingState() {
		return isDownloading;
	}

	public boolean getLockedState() {
		return isLocked;
	}

	public boolean getShowAttachmentState() {
		return isShowAttachments;
	}

	public void setShowAttachmentState(boolean value) {
		isShowAttachments = value;
	}

	public Text getDownloadImage() {
		return textDownloadImage;
	}

	public Text getLockedImage() {
		return textLockedImage;
	}

	public Text getDeleteImage() {
		// return deleteImage;
		return textDeleteImage;
	}

	public Text getOpenFolderImage() {
		return textFolderImage;
	}

	public Text getRetryImage() {
		return textRetryImage;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public int getNumAttachments() {
		return numAttachments;
	}

	public void setNumAttachments(int numAttachments) {
		this.numAttachments = numAttachments;
	}

	public String getHashOfTxnId() {
		return hashOfTxnId;
	}

	public void setHashOfTxnId(String hashOfTxnId) {
		this.hashOfTxnId = hashOfTxnId;
	}

	public String getNotificationTag() {
		return notificationTag;
	}

	public void setNotificationTag(String notificationTag) {
		this.notificationTag = notificationTag;
	}

	public long getSizeAttachments() {
		return sizeAttachments;
	}

	public void setSizeAttachments(long sizeAttachments) {
		this.sizeAttachments = sizeAttachments;
	}

	public String getPathOfUnEncryptedFiles() {
		return pathOfUnEncryptedFiles;
	}

	public void setPathOfUnEncryptedFiles(String pathOfUnEncryptedFiles) {
		this.pathOfUnEncryptedFiles = pathOfUnEncryptedFiles;
	}

	public String getRecieverAddress() {
		return recieverAddress;
	}

	public void setRecieverAddress(String recieverAddress) {
		this.recieverAddress = recieverAddress;
	}
	
	public String getBitVaultToken() {
		return bitVaultToken;
	}

	public void setBitVaultToken(String bitVaultToken) {
		this.bitVaultToken = bitVaultToken;
	}

	public String getTxnId() {
		return txnId;
	}

	public void setTxnId(String txnId) {
		this.txnId = txnId;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}
}