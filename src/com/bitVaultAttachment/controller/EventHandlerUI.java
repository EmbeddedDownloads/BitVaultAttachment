package com.bitVaultAttachment.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;

import org.bouncycastle.util.encoders.Hex;

import com.bitVaultAttachment.apiMethods.PBCRequest;
import com.bitVaultAttachment.apiMethods.Utils;
import com.bitVaultAttachment.constant.BitVaultConstants;
import com.bitVaultAttachment.constant.GlobalCalls;
import com.bitVaultAttachment.constant.Messages;
import com.bitVaultAttachment.database.DbConnection;
import com.bitVaultAttachment.models.DraftAttachmentDTO;
import com.bitVaultAttachment.ui.NotificationBar;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class EventHandlerUI {

	/* ----------------- Set Event Handlers ---------------- */

	/**
	 * Set bar mouse click handler
	 * 
	 * @param b
	 */
	public void setBarClickEvent(NotificationBar b) {

		EventHandler<MouseEvent> barClickHandler = e -> handleBarClick(e, b);
		b.getContainer().addEventHandler(MouseEvent.MOUSE_CLICKED, barClickHandler);
	}

	/**
	 * Set delete mouse click handler for Inbox
	 * 
	 * @param b
	 * @param primaryStage
	 */
	public void setDeleteClickEvent(InboxPage Inbox, NotificationBar b, Stage primaryStage) {

		EventHandler<MouseEvent> deleteClickHandler = e -> handleDeleteClick(e, Inbox, b, primaryStage);
		b.getDeleteImage().addEventHandler(MouseEvent.MOUSE_CLICKED, deleteClickHandler);
	}

	/**
	 * Set delete mouse click handler for Drafts
	 * 
	 * @param b
	 * @param primaryStage
	 */
	public void setDeleteClickEvent(DraftPage Draft, NotificationBar b, Stage primaryStage) {

		EventHandler<MouseEvent> deleteClickHandler = e -> handleDeleteClick(e, Draft, b, primaryStage);
		b.getDeleteImage().addEventHandler(MouseEvent.MOUSE_CLICKED, deleteClickHandler);
	}

	/**
	 * Set download mouse click handler
	 * 
	 * @param b
	 */
	public void setDownloadClickEvent(NotificationBar b) {

		EventHandler<MouseEvent> downloadClickHandler = e -> handleDownloadClick(e, b);
		b.getDownloadImage().addEventHandler(MouseEvent.MOUSE_CLICKED, downloadClickHandler);
	}

	/**
	 * Set lock mouse click handler
	 * 
	 * @param b
	 */
	public void setLockClickEvent(NotificationBar b) {

		EventHandler<MouseEvent> lockClickHandler = e -> handleLockClick(e, b);
		b.getLockedImage().addEventHandler(MouseEvent.MOUSE_CLICKED, lockClickHandler);

	}

	/**
	 * Open in Folder mouse click handler
	 * 
	 * @param b
	 */
	public void setOpenFolderClickEvent(NotificationBar b) {

		EventHandler<MouseEvent> openFolderClickHandler = e -> handleOpenFolderClick(e, b);
		b.getOpenFolderImage().addEventHandler(MouseEvent.MOUSE_CLICKED, openFolderClickHandler);
	}

	/**
	 * Set retry mouse click handler
	 * 
	 * @param b
	 */
	public void setRetryClickEvent(NotificationBar b) {
		EventHandler<MouseEvent> retryClickHandler = e -> handleRetryClick(e, b);
		b.getRetryImage().addEventHandler(MouseEvent.MOUSE_CLICKED, retryClickHandler);

	}

	/**
	 * Set others option click handler
	 * 
	 * @param textOther
	 * @param textOtherArrow
	 */
	public void setOthersClickEvent(Text textOther, Text textOtherArrow, InboxPage Inbox) {

		EventHandler<MouseEvent> othersClickHandler = e -> handleOthersClick(e, Inbox);
		textOther.addEventHandler(MouseEvent.MOUSE_CLICKED, othersClickHandler);
		textOtherArrow.addEventHandler(MouseEvent.MOUSE_CLICKED, othersClickHandler);
	}

	/**
	 * set sort click handler
	 * 
	 * @param textSort
	 * @param textSortImage
	 * @param parentStage
	 * @param sortingMenu
	 */
	public void setSortClickEvent(Text textSort, Text textSortImage, Stage parentStage, ContextMenu sortingMenu) {

		EventHandler<MouseEvent> sortClickHandler = e -> handleSortClick(e, parentStage, textSortImage, sortingMenu);
		textSortImage.addEventHandler(MouseEvent.MOUSE_PRESSED, sortClickHandler);
		textSort.addEventHandler(MouseEvent.MOUSE_PRESSED, sortClickHandler);

		// set handlers for context menu
		sortingMenu.setOnShown(e -> setOnShown(e, sortingMenu));
		sortingMenu.setOnHidden(e -> setOnHidden(e, sortingMenu));
	}

	/**
	 * on hidden handler for context menu
	 * 
	 * @param e
	 * @param sortingMenu
	 */
	public void setOnHidden(Event e, ContextMenu sortingMenu) {
		sortingMenu.setConsumeAutoHidingEvents(true);
	}

	/**
	 * on shown handler for context menu
	 * 
	 * @param e
	 * @param sortingMenu
	 */
	public void setOnShown(Event e, ContextMenu sortingMenu) {
		sortingMenu.setConsumeAutoHidingEvents(true);
	}

	/**
	 * 
	 * /** set public address click handler
	 * 
	 * @param item
	 * @param Inbox
	 */
	public void setPublicAddressSortClickEvent(MenuItem item, InboxPage Inbox) {

		EventHandler<ActionEvent> publicAddressClickHandler = e -> handlePublicAddressClick(e, item, Inbox);
		item.addEventHandler(ActionEvent.ACTION, publicAddressClickHandler);
	}

	/**
	 * set size click handler
	 * 
	 * @param item
	 * @param Inbox
	 */
	public void setSizeSortClickEvent(MenuItem item, InboxPage Inbox) {

		EventHandler<ActionEvent> sizeClickHandler = e -> handleSizeClick(e, item, Inbox);
		item.addEventHandler(ActionEvent.ACTION, sizeClickHandler);
	}

	/**
	 * set date click handler
	 * 
	 * @param item
	 * @param Inbox
	 */
	public void setDateClickEvent(MenuItem item, InboxPage Inbox) {

		EventHandler<ActionEvent> attachmentsClickHandler = e -> handleDateClick(e, item, Inbox);
		item.addEventHandler(ActionEvent.ACTION, attachmentsClickHandler);

	}

	/* ----------------------- Define Handlers ---------------------------- */

	/**
	 * Bar clicked handler
	 * 
	 * @param e
	 * @param p
	 */
	public void handleBarClick(MouseEvent e, NotificationBar b) {

		if (!b.getLockedState()) {
			if (b.getShowAttachmentState() == false) {
				// create the attachments list
				b.getAttachmentList(b.getBitVaultToken(), b.getHashOfTxnId());
				b.getContainer().getChildren().add(b.getAttachmentContainer());
				b.setShowAttachmentState(true);
			} else {
				// remove attachments list
				b.getContainer().getChildren().removeAll(b.getAttachmentContainer());
				b.getAttachmentContainer().getChildren().removeAll(b.getAttachmentContainer().getChildren());
				b.setShowAttachmentState(false);
			}
		}
		e.consume();
	}

	/**
	 * Lock clicked handler
	 * 
	 * @param e
	 * @param b
	 */
	public void handleLockClick(MouseEvent e, NotificationBar b) {

		Stage newWindow = new Stage();
		ImageView viewForCam = new ImageView();
		viewForCam.setFitHeight(250);
		viewForCam.setFitWidth(250);
		Label imageLabel = new Label();
		imageLabel.setText("Scan QR Code From BitVault");
		imageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
		imageLabel.setPadding(new Insets(0,0,0,0));
		Label errorLabel = new Label();
		errorLabel.setText("Incorrect QR Code");
		errorLabel.setStyle("-fx-text-fill: red;");
		errorLabel.setVisible(false);
		VBox imageBox = new VBox();
		imageBox.getChildren().addAll(viewForCam,errorLabel,imageLabel);
		imageBox.setAlignment(Pos.CENTER);
		imageBox.setSpacing(5);
		Scene scene = new Scene(imageBox);
		newWindow.setScene(scene);
		newWindow.setHeight(400);
		newWindow.setWidth(350);
		newWindow.setMinHeight(400);
		newWindow.setMinWidth(350);
		newWindow.setMaxHeight(400);
		newWindow.setMaxWidth(350);
		newWindow.setTitle("Scan");
		newWindow.initStyle(StageStyle.UTILITY);
		newWindow.initModality(Modality.WINDOW_MODAL);
		newWindow.initOwner(MainPage.stage);
		newWindow.show();
		WebCamImageProvider imageProvider = new WebCamImageProvider();
		imageProvider.setPostWebCamAction(BitVaultConstants.CAM_FOR_UNLOCK);
		imageProvider.setStageKey(newWindow.toString());
		imageProvider.setHashOfTxnId(b.getHashOfTxnId());
		viewForCam.setVisible(true);
		viewForCam.imageProperty().bind(imageProvider.imageProperty());
		errorLabel.visibleProperty().bindBidirectional(imageProvider.errorMsgVisibility());
		newWindow.setOnCloseRequest(event -> {
			imageProvider.stopCamera(true);
			// Save file
		});
		Thread t = new Thread(imageProvider);
		t.setDaemon(true);
		t.start();
		
		e.consume();

	}

	/**
	 * Open Folder clicked handler
	 * 
	 * @param e
	 * @param b
	 */
	public void handleOpenFolderClick(MouseEvent e, NotificationBar b) {

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Desktop.getDesktop().open(new File(b.getPathOfUnEncryptedFiles().trim()));
				} catch (IOException e1) {
					Utils.getLogger().log(Level.SEVERE,"open folder exception", e1);
				}
			}      
		});
		thread.start();

		Utils.getLogger().log(Level.FINEST,"open in folder clicked");
		e.consume();
	}

	/**
	 * Delete clicked handler for inbox
	 * 
	 * @param e
	 * @param b
	 * @param primaryStage
	 */
	public void handleDeleteClick(MouseEvent e, InboxPage Inbox, NotificationBar b, Stage primaryStage) {

		Stage deleteStage = new Stage();

		Text textPrompt = new Text(Messages.DELETE_ATT);
		HBox hboxPrompt = new HBox(textPrompt);
		HBox.setHgrow(textPrompt, Priority.ALWAYS);
		HBox.setMargin(textPrompt, new Insets(40, 20, 20, 20));
		hboxPrompt.setAlignment(Pos.CENTER);

		Button buttonCancel = new Button("Cancel");
		buttonCancel.setMinWidth(100);
		buttonCancel.getStyleClass().add("actionButtons");
		buttonCancel.setOnAction(p -> cancelDelete(deleteStage));

		Button buttonDelete = new Button("Delete");
		buttonDelete.setMinWidth(100);
		buttonDelete.getStyleClass().add("actionButtons");
		buttonDelete.setOnAction(p -> proceedDelete(Inbox, b, deleteStage));

		HBox hboxButtonContainer = new HBox(buttonDelete, buttonCancel);
		HBox.setHgrow(buttonDelete, Priority.ALWAYS);
		HBox.setMargin(buttonDelete, new Insets(10, 20, 20, 20));
		HBox.setMargin(buttonCancel, new Insets(10, 20, 20, 20));
		HBox.setHgrow(buttonCancel, Priority.ALWAYS);
		hboxButtonContainer.setAlignment(Pos.CENTER);

		// set pop-up for delete
		VBox deletePopUp = new VBox(hboxPrompt, hboxButtonContainer);
		VBox.setVgrow(hboxButtonContainer, Priority.ALWAYS);
		VBox.setVgrow(textPrompt, Priority.ALWAYS);
		deletePopUp.setAlignment(Pos.CENTER);
		deletePopUp.setFillWidth(true);
		deletePopUp.setMinHeight(180);
		deletePopUp.setMinWidth(450);
		deletePopUp.setStyle("-fx-background-color: white;");

		// create new scene and utility stage
		Scene deleteScene = new Scene(deletePopUp);
		deleteScene.getStylesheets().addAll("bitVaultMain.css");
		deleteStage.setScene(deleteScene);
		deleteStage.initOwner(primaryStage);
		deleteStage.initStyle(StageStyle.TRANSPARENT);
		deleteStage.initModality(Modality.APPLICATION_MODAL);
		deleteStage.show();

		e.consume();
	}

	/**
	 * Delete Notification bar if button selected for inbox
	 * 
	 * @param b
	 * @param deleteStage
	 */
	public void proceedDelete(InboxPage Inbox, NotificationBar b, Stage deleteStage) {

		DbConnection connection = new DbConnection();
		try {
			if(!b.getDownloadedState()){
			}
			if(!GlobalCalls.isNullOrEmptyStringCheck(b.getBitVaultToken()) && 
					!GlobalCalls.isNullOrEmptyStringCheck(b.getHashOfTxnId())){
				connection.updateNotificationDtaOnDelete(b.getBitVaultToken(), b.getHashOfTxnId());
				if (BitVaultConstants.mGeneralManagerCallbacks != null) {
					BitVaultConstants.mGeneralManagerCallbacks.inboxActionCallback();
				}
			}
		} catch (ClassNotFoundException | SQLException e) {
			Utils.getLogger().log(Level.SEVERE,"db fetch error for delete", e);
		}

		// delete notification bar from parent vbox
		if (b.getContainer().getParent() == Inbox.vboxNewAttachments) {
			Inbox.vboxNewAttachments.getChildren().remove(b.getContainer());
			Inbox.newNotifications.remove(b);
		} else if (b.getContainer().getParent() == Inbox.vboxDownAttachments) {
			Inbox.vboxDownAttachments.getChildren().remove(b.getContainer());
			Inbox.downloadedList.remove(b);
		}
		// close pop-up
		deleteStage.close();
	}

	/**
	 * Cancel deleting notification bar if button selected
	 * 
	 * @param deleteStage
	 */
	public void cancelDelete(Stage deleteStage) {

		// close popup
		deleteStage.close();
	}

	/**
	 * Delete clicked handler for draft
	 * 
	 * @param e
	 * @param b
	 * @param primaryStage
	 */
	public void handleDeleteClick(MouseEvent e, DraftPage Draft, NotificationBar b, Stage primaryStage) {

		Stage deleteStage = new Stage();

		Text textPrompt = new Text("Delete Attachment(s)?");
		HBox hboxPrompt = new HBox(textPrompt);
		HBox.setHgrow(textPrompt, Priority.ALWAYS);
		HBox.setMargin(textPrompt, new Insets(40, 20, 20, 20));
		hboxPrompt.setAlignment(Pos.CENTER);

		Button buttonCancel = new Button("Cancel");
		buttonCancel.setMinWidth(100);
		buttonCancel.getStyleClass().add("actionButtons");
		buttonCancel.setOnAction(p -> cancelDelete(deleteStage));

		Button buttonDelete = new Button("Delete");
		buttonDelete.setMinWidth(100);
		buttonDelete.getStyleClass().add("actionButtons");
		buttonDelete.setOnAction(p -> proceedDelete(Draft, b, deleteStage));

		HBox hboxButtonContainer = new HBox(buttonDelete, buttonCancel);
		HBox.setHgrow(buttonDelete, Priority.ALWAYS);
		HBox.setMargin(buttonDelete, new Insets(10, 20, 20, 20));
		HBox.setMargin(buttonCancel, new Insets(10, 20, 20, 20));
		HBox.setHgrow(buttonCancel, Priority.ALWAYS);
		hboxButtonContainer.setAlignment(Pos.CENTER);

		// set pop-up for delete
		VBox deletePopUp = new VBox(hboxPrompt, hboxButtonContainer);
		VBox.setVgrow(hboxButtonContainer, Priority.ALWAYS);
		VBox.setVgrow(textPrompt, Priority.ALWAYS);
		deletePopUp.setAlignment(Pos.CENTER);
		deletePopUp.setMinHeight(180);
		deletePopUp.setMinWidth(450);
		deletePopUp.setStyle("-fx-background-color: white;");
		deletePopUp.setPadding(new Insets(20,20,20,20));
		deletePopUp.setEffect(new DropShadow());

		// create new scene and utility stage
		Scene deleteScene = new Scene(deletePopUp);
//		deleteScene.getRoot().setEffects(new DropShadow());
		deleteScene.getStylesheets().addAll("bitVaultMain.css");
		deleteStage.setScene(deleteScene);
		deleteStage.getScene().setFill(Color.TRANSPARENT);
		deleteStage.initOwner(primaryStage);
		deleteStage.initStyle(StageStyle.UNDECORATED);
		deleteStage.initModality(Modality.APPLICATION_MODAL);
		deleteStage.show();

		e.consume();
	}

	/**
	 * Delete Notification bar if button selected for draft
	 * 
	 * @param b
	 * @param deleteStage
	 */
	public void proceedDelete(DraftPage Draft, NotificationBar b, Stage deleteStage) {

		DbConnection connection = new DbConnection();
		try {
			if(!GlobalCalls.isNullOrEmptyStringCheck(b.getBitVaultToken()) && 
					!GlobalCalls.isNullOrEmptyStringCheck(b.getTxnId())){
				connection.updatDraftDtaOnDelete(b.getBitVaultToken(), b.getTxnId());
				if (BitVaultConstants.mGeneralManagerCallbacks != null) {
					BitVaultConstants.mGeneralManagerCallbacks.inboxActionCallback();
				}
			}
			
		} catch (ClassNotFoundException | SQLException e) {
			Utils.getLogger().log(Level.SEVERE,"DB fetch error for delete", e);
		}
		// Delete Files from disk
		Utils.getLogger().log(Level.FINEST, "deleting form disk");
		File fileToDelComp = null;
		File fileToDelEnc = null;
		try {
			fileToDelComp = new File( BitVaultConstants.PATH_FOR_ENCRYPTED_FILES + 
					Hex.toHexString(Utils.hashGenerate(b.getTxnId())) + ".zip");
		} catch (NoSuchAlgorithmException e) {
			Utils.getLogger().log(Level.SEVERE, "Unable to open compressed file for deleting", e);
		}
		try {
			fileToDelEnc = new File( BitVaultConstants.PATH_FOR_ENCRYPTED_FILES + 
					Hex.toHexString(Utils.hashGenerate(b.getTxnId())) + ".zip_enc");
		} catch (NoSuchAlgorithmException e) {
			Utils.getLogger().log(Level.SEVERE, "Unable to open encrypted file for deleting", e);
		}
		if(fileToDelComp.exists()){
			fileToDelComp.delete();
		}
		if(fileToDelEnc.exists()){
			fileToDelEnc.delete();
		}
		// delete from notification bar
		if (b.getContainer().getParent() == Draft.getDraftBox()) {
			Draft.getDraftBox().getChildren().remove(b.getContainer());
			Draft.getDraftBar().remove(b);
		}
		// close pop-up
		deleteStage.close();
	}

	/**
	 * Download clicked handler
	 * 
	 * @param e
	 * @param b
	 */
	public void handleDownloadClick(MouseEvent e, NotificationBar b) {

		// set tag for downloading
		String notificationTag = null;
		if( b.getNotificationTag().trim().equalsIgnoreCase(PBCRequest.A2A_NOTIFICATION_TAG) ){
			notificationTag = PBCRequest.A2A_TAG;
		}
		else if( b.getNotificationTag().trim().equalsIgnoreCase(PBCRequest.B2A_NOTIFICATION_TAG) ){
			notificationTag = PBCRequest.B2A_TAG;
		}
		
		// Downloading task
		try {
			DownloadPopUpPage downloadTask = new DownloadPopUpPage(
					b.getBitVaultToken(), 
					b.getHashOfTxnId(), 
					notificationTag, 
					b.getRecieverAddress());
			downloadTask.downloadAttachment();
		} catch (Exception e2) {
			Utils.getLogger().log(Level.SEVERE,"download error", e2);
		}
		e.consume();
	}

	/**
	 * Retry clicked handler
	 * 
	 * @param e
	 * @param b
	 */
	public void handleRetryClick(MouseEvent e, NotificationBar b) {
		
		SendingPopupPage sendPopUp;
		DbConnection connection = new DbConnection();
		ArrayList<DraftAttachmentDTO> attachmentListFinal;
		try {
			if(!GlobalCalls.isNullOrEmptyStringCheck(b.getBitVaultToken()) && 
					!GlobalCalls.isNullOrEmptyStringCheck(b.getTxnId())){
				attachmentListFinal = (ArrayList<DraftAttachmentDTO>) connection
						.fetchDraftAttachmentList(b.getBitVaultToken(), b.getTxnId());
				sendPopUp = new SendingPopupPage(b.getTxnId(), b.getSessionKey(), attachmentListFinal);
				sendPopUp.sendAttachment();
			}
		} catch (ClassNotFoundException | SQLException e1) {
			Utils.getLogger().log(Level.SEVERE,"retry sending failed", e1);
		} catch (Exception e1) {
			Utils.getLogger().log(Level.SEVERE,"retry sending failed", e1);
		}
		e.consume();
	}

	/**
	 * Others clicked handler
	 * 
	 * @param e
	 */
	public void handleOthersClick(MouseEvent e, InboxPage Inbox) {

		// keep copy before sorting
		ArrayList<NotificationBar> beforeSortNewNotifications = new ArrayList<NotificationBar>(Inbox.newNotifications);
		ArrayList<NotificationBar> beforeSortDownloadedList = new ArrayList<NotificationBar>(Inbox.downloadedList);

		// sort lists
		Collections.sort(Inbox.newNotifications, new OthersComparator());
		Collections.sort(Inbox.downloadedList, new OthersComparator());

		// reverse if already sorted
		if (beforeSortNewNotifications.equals(Inbox.newNotifications))
			Collections.reverse(Inbox.newNotifications);

		// reverse if already sorted
		if (beforeSortDownloadedList.equals(Inbox.downloadedList))
			Collections.reverse(Inbox.downloadedList);

		// remove all children from vbox container
		Inbox.vboxNewAttachments.getChildren().remove(0, Inbox.vboxNewAttachments.getChildren().size());
		Inbox.vboxDownAttachments.getChildren().remove(0, Inbox.vboxDownAttachments.getChildren().size());

		// add the sorted bar to vbox container
		for (int i = 0; i < Inbox.newNotifications.size(); i++) {
			Inbox.vboxNewAttachments.getChildren().add(Inbox.newNotifications.get(i).getContainer());
		}

		// add the sorted bar to vbox container
		for (int i = 0; i < Inbox.downloadedList.size(); i++) {
			Inbox.vboxDownAttachments.getChildren().add(Inbox.downloadedList.get(i).getContainer());
		}
		e.consume();
	}

	/**
	 * Sort clicked handler
	 * 
	 * @param e
	 * @param primaryStage
	 * @param sortingMenu
	 */
	public void handleSortClick(MouseEvent e, Stage primaryStage, Text textSortImage, ContextMenu sortingMenu) {

		  // open sorting context menu
		  Bounds boundsInScreen = textSortImage.localToScreen(textSortImage.getBoundsInLocal());
		  sortingMenu.show(primaryStage, 
		    boundsInScreen.getMaxX() - 157, 
		    boundsInScreen.getMaxY() + 5 );
		  sortingMenu.setAutoHide(true);
		  // consume event at capture stage
		  e.consume();
		 }

	/**
	 * Public Address Clicked Handler
	 * 
	 * @param e
	 * @param item
	 * @param Inbox
	 */
	public void handlePublicAddressClick(ActionEvent e, MenuItem item, InboxPage Inbox) {

		// keep copy before sorting
		ArrayList<NotificationBar> beforeSortNewNotifications = new ArrayList<NotificationBar>(Inbox.newNotifications);
		ArrayList<NotificationBar> beforeSortDownloadedList = new ArrayList<NotificationBar>(Inbox.downloadedList);

		// sort lists
		Collections.sort(Inbox.newNotifications, new PublicAddressComparator());
		Collections.sort(Inbox.downloadedList, new PublicAddressComparator());

		// reverse if already sorted
		if (beforeSortNewNotifications.equals(Inbox.newNotifications))
			Collections.reverse(Inbox.newNotifications);

		// reverse if already sorted
		if (beforeSortDownloadedList.equals(Inbox.downloadedList))
			Collections.reverse(Inbox.downloadedList);

		// remove all children from vbox container
		Inbox.vboxNewAttachments.getChildren().remove(0, Inbox.vboxNewAttachments.getChildren().size());
		Inbox.vboxDownAttachments.getChildren().remove(0, Inbox.vboxDownAttachments.getChildren().size());

		// add the sorted bar to vbox container
		for (int i = 0; i < Inbox.newNotifications.size(); i++) {
			Inbox.vboxNewAttachments.getChildren().add(Inbox.newNotifications.get(i).getContainer());
		}

		// add the sorted bar to vbox container
		for (int i = 0; i < Inbox.downloadedList.size(); i++) {
			Inbox.vboxDownAttachments.getChildren().add(Inbox.downloadedList.get(i).getContainer());
		}

		e.consume();
	}

	/**
	 * Size Clicked Handler
	 * 
	 * @param e
	 * @param item
	 * @param Inbox
	 */
	public void handleSizeClick(ActionEvent e, MenuItem item, InboxPage Inbox) {

		// keep copy before sorting
		ArrayList<NotificationBar> beforeSortNewNotifications = new ArrayList<NotificationBar>(Inbox.newNotifications);
		ArrayList<NotificationBar> beforeSortDownloadedList = new ArrayList<NotificationBar>(Inbox.downloadedList);

		// sort lists
		Collections.sort(Inbox.newNotifications, new SizeComparator());
		Collections.sort(Inbox.downloadedList, new SizeComparator());

		// reverse if already sorted
		if (beforeSortNewNotifications.equals(Inbox.newNotifications))
			Collections.reverse(Inbox.newNotifications);

		// reverse if already sorted
		if (beforeSortDownloadedList.equals(Inbox.downloadedList))
			Collections.reverse(Inbox.downloadedList);

		// remove all children from vbox container
		Inbox.vboxNewAttachments.getChildren().remove(0, Inbox.vboxNewAttachments.getChildren().size());
		Inbox.vboxDownAttachments.getChildren().remove(0, Inbox.vboxDownAttachments.getChildren().size());

		// add the sorted bar to vbox container
		for (int i = 0; i < Inbox.newNotifications.size(); i++) {
			Inbox.vboxNewAttachments.getChildren().add(Inbox.newNotifications.get(i).getContainer());
		}

		// add the sorted bar to vbox container
		for (int i = 0; i < Inbox.downloadedList.size(); i++) {
			Inbox.vboxDownAttachments.getChildren().add(Inbox.downloadedList.get(i).getContainer());
		}
		e.consume();
	}

	/**
	 * Date Clicked Handler
	 * 
	 * @param e
	 * @param item
	 * @param Inbox
	 */
	public void handleDateClick(ActionEvent e, MenuItem item, InboxPage Inbox) {

		// keep copy before sorting
		ArrayList<NotificationBar> beforeSortNewNotifications = new ArrayList<NotificationBar>(Inbox.newNotifications);
		ArrayList<NotificationBar> beforeSortDownloadedList = new ArrayList<NotificationBar>(Inbox.downloadedList);

		// sort lists
		Collections.sort(Inbox.newNotifications, new DateComparator());
		Collections.sort(Inbox.downloadedList, new DateComparator());

		// reverse if already sorted
		if (beforeSortNewNotifications.equals(Inbox.newNotifications))
			Collections.reverse(Inbox.newNotifications);

		// reverse if already sorted
		if (beforeSortDownloadedList.equals(Inbox.downloadedList))
			Collections.reverse(Inbox.downloadedList);

		// remove all children from vbox container
		Inbox.vboxNewAttachments.getChildren().remove(0, Inbox.vboxNewAttachments.getChildren().size());
		Inbox.vboxDownAttachments.getChildren().remove(0, Inbox.vboxDownAttachments.getChildren().size());

		// add the sorted bar to vbox container
		for (int i = 0; i < Inbox.newNotifications.size(); i++) {
			Inbox.vboxNewAttachments.getChildren().add(Inbox.newNotifications.get(i).getContainer());
		}

		// add the sorted bar to vbox container
		for (int i = 0; i < Inbox.downloadedList.size(); i++) {
			Inbox.vboxDownAttachments.getChildren().add(Inbox.downloadedList.get(i).getContainer());
		}

		e.consume();
	}

	/* ---------------------- comparators ---------------------------- */

	/**
	 * class compares the public address of notification bar
	 * 
	 * @author root
	 *
	 */
	public class PublicAddressComparator implements Comparator<NotificationBar> {

		public int compare(NotificationBar bar1, NotificationBar bar2) {

			if ((bar1.getFromAddress().compareTo(bar2.getFromAddress()) > 0))
				return -1;
			else if ((bar1.getFromAddress().compareTo(bar2.getFromAddress()) < 0))
				return 1;
			else
				return 0;
		}
	}

	/**
	 * class compares the size attachments of notification bar
	 * 
	 * @author root
	 *
	 */
	public class SizeComparator implements Comparator<NotificationBar> {

		public int compare(NotificationBar bar1, NotificationBar bar2) {

			if (bar1.getSizeAttachments() < bar2.getSizeAttachments())
				return -1;
			else if (bar1.getSizeAttachments() > bar2.getSizeAttachments())
				return 1;
			else
				return 0;
		}
	}

	/**
	 * class compares the Date in notification bar
	 * 
	 * @author root
	 *
	 */
	public class DateComparator implements Comparator<NotificationBar> {

		public int compare(NotificationBar bar1, NotificationBar bar2) {

			return ~bar1.getDate().compareTo(bar2.getDate());
		}
	}

	/**
	 * class compares the notification from others in notification bar
	 * 
	 * @author root
	 *
	 */
	public class OthersComparator implements Comparator<NotificationBar> {

		public int compare(NotificationBar bar1, NotificationBar bar2) {

			if (((bar1.getFromAddress().compareTo("BitVault")) == 0)
					&& ((bar2.getFromAddress().compareTo("BitVault")) != 0))
				return 1;
			else if (((bar1.getFromAddress().compareTo("BitVault")) != 0)
					&& ((bar2.getFromAddress().compareTo("BitVault")) == 0))
				return -1;
			else
				return 0;
		}
	}
}
