package com.bitVaultAttachment.controller;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.logging.Level;

import com.bitVaultAttachment.apiMethods.SendingTask;
import com.bitVaultAttachment.apiMethods.Utils;
import com.bitVaultAttachment.constant.BitVaultConstants;
import com.bitVaultAttachment.constant.Messages;
import com.bitVaultAttachment.models.DraftAttachmentDTO;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
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
import javafx.stage.WindowEvent;

/**
 * This Class is used to maintain Pop ups for different cases like Compose,
 * Inbox etc
 * 
 * 
 * @author Shelly
 *
 */
public class SendingPopupPage {

	private String txid;
	private String skey;
	private ArrayList<DraftAttachmentDTO> attachmentList;

	private Text imageText;
	private ProgressBar pb = new ProgressBar(0);

	private Button cancelBtn;
	private Button OkBtn;
	private Button retryBtn;
	private Text nextImageText;
	private HBox btnHbox;
	private VBox infoBox;
	private Scene scene;
	private VBox nextInfoBox;
	private Scene nextScene;
	private Stage newWindow;
	private Stage nextWindow;

	private SendingTask sendTask;
	public Service<Void> sendService;


	/**
	 * Constructor for Pop-Up Page
	 * 
	 * @param txid
	 * @param skey
	 * @param attachmentList
	 */
	public SendingPopupPage(String txid, String skey, ArrayList<DraftAttachmentDTO> attachmentList) throws Exception {
		// check if txid characters are correct
		try {
			new BigInteger(txid, 16);
		} catch (NumberFormatException e) {
			Utils.getLogger().log(Level.SEVERE,"txid format error", e);
			throw e;
		}

		// set parameters
		this.setTxid(txid);
		this.setSkey(skey);
		this.setAttachmentList(attachmentList);

		// create sending service
		sendService = new Service<Void>() {
			@Override
			protected Task<Void> createTask() {
				sendTask = new SendingTask(txid, skey, attachmentList);
				return sendTask;
			}
		};
	}

	/**
	 * attachment sending task
	 */
	public void sendAttachment() {
		this.createPopup();
		bindToWorker(sendService);
		addEventHandlers();
		try {
			sendService.start();
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"task error", e);
		}
	}

	/**
	 * This Method will create Popups and bind action actions for Buttons in
	 * case ofCompose action.
	 * 
	 */
	public void createPopup() {

		// start a new stage
		newWindow = new Stage();
		nextWindow = new Stage();

		// text to show progress
		imageText = new Text();
		nextImageText = new Text();

		// Ok button
		OkBtn = new Button("OK");
		OkBtn.setPrefHeight(30);
		OkBtn.setPrefWidth(100);
		OkBtn.getStyleClass().add("actionButtons");

		// cancel button
		cancelBtn = new Button("Cancel");
		cancelBtn.setPrefHeight(30);
		cancelBtn.setPrefWidth(100);
		cancelBtn.getStyleClass().add("actionButtons");

		// retry button
		retryBtn = new Button("Retry");
		retryBtn.setPrefHeight(30);
		retryBtn.setPrefWidth(100);
		retryBtn.getStyleClass().add("actionButtons");

		// hbox holding buttons
		btnHbox = new HBox();
		btnHbox.setAlignment(Pos.CENTER);
		btnHbox.setPadding(new Insets(50, 0, 0, 0));
		btnHbox.setVisible(false);
		btnHbox.setSpacing(30);

		// progress bar
		pb.getStyleClass().add("progressBarCss");
		pb.setProgress(0);
		pb.setPrefWidth(420);
		pb.setPrefHeight(30);
		pb.setPadding(new Insets(50, 0, 0, 0));

		// set message of text
		imageText.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));
		nextImageText.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));

		// information box
		infoBox = new VBox();
		infoBox.getChildren().addAll(imageText, pb);

		VBox.setVgrow(pb, Priority.ALWAYS);
		infoBox.setAlignment(Pos.CENTER);
		infoBox.setSpacing(20);
		infoBox.setStyle("-fx-background-color:white;");
		infoBox.setSpacing(10);

		scene = new Scene(infoBox);
		scene.getStylesheets().addAll("bitVaultMain.css");

		// set the scene and stage
		newWindow.setScene(scene);
		newWindow.setHeight(180);
		newWindow.setWidth(450);

		newWindow.initStyle(StageStyle.UTILITY);
		newWindow.initModality(Modality.APPLICATION_MODAL);
		newWindow.centerOnScreen();
		newWindow.initOwner(MainPage.stage);
		newWindow.setFullScreen(false);
		newWindow.setResizable(false);
		newWindow.show();

		// next information box
		nextInfoBox = new VBox();
		nextInfoBox.getChildren().addAll(nextImageText, btnHbox);
		VBox.setMargin(nextImageText, new Insets(40,0,0,0));

		VBox.setVgrow(pb, Priority.ALWAYS);
		nextInfoBox.setAlignment(Pos.CENTER);
		nextInfoBox.setSpacing(20);
		nextInfoBox.setStyle("-fx-background-color:white;");
		nextInfoBox.setSpacing(10);

		nextScene = new Scene(nextInfoBox);
		nextScene.getStylesheets().addAll("bitVaultMain.css");

		// set new scene and stage
		nextWindow.setScene(nextScene);
		nextWindow.setHeight(180);
		nextWindow.setWidth(450);

		nextWindow.initStyle(StageStyle.TRANSPARENT);
		nextWindow.initModality(Modality.APPLICATION_MODAL);
		nextWindow.centerOnScreen();
		nextWindow.initOwner(MainPage.stage);
		nextWindow.setFullScreen(false);
		nextWindow.setResizable(false);
	}

	/**
	 * Add the event handlers
	 */
	void addEventHandlers() {

		/**
		 * This method is called when successful message is send
		 */
		sendService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				nextImageText.setFill(Color.BLACK);
				btnHbox.getChildren().clear();
				btnHbox.setVisible(true);
				if (btnHbox.getChildren().isEmpty()) {
					btnHbox.getChildren().addAll(OkBtn);
				}
				nextImageText.setText(Messages.SENDING_SUCCESS);
				newWindow.close();
				nextWindow.show();
			}
		});

		/**
		 * This method is used when Message sending is failed
		 */
		sendService.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				// open pop-up on failed
				nextImageText.setFill(Color.RED);
				btnHbox.getChildren().clear();
				btnHbox.setVisible(true);
				nextImageText.setText(Messages.SENDING_ERROR + sendService.getMessage());
				if (btnHbox.getChildren().isEmpty()) {
					if(!sendService.getMessage().trim().equalsIgnoreCase("File(s) already sent with Transaction ID")){
						btnHbox.getChildren().addAll(retryBtn);
					}
					btnHbox.getChildren().addAll(cancelBtn);
				}
				newWindow.close();
				nextWindow.show();
			}
		});

		/**
		 * This method is used when Message sending is cancelled
		 */
		sendService.setOnCancelled(new EventHandler<WorkerStateEvent>() {

			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				// open pop-up on cancelled
				nextImageText.setFill(Color.RED);
				btnHbox.setVisible(true);
				nextImageText.setText(Messages.SENDING_FAILED);
				if (btnHbox.getChildren().isEmpty()) {
					btnHbox.getChildren().addAll(retryBtn, cancelBtn);
				}
				newWindow.close();
				nextWindow.show();
			}
		});

		/**
		 * This method is called when attachment is sent successfully it will
		 * redirect you to Inbox
		 */
		OkBtn.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				nextWindow.close();
				if (BitVaultConstants.mGeneralManagerCallbacks != null) {
					BitVaultConstants.mGeneralManagerCallbacks.inboxActionCallback();
				}

			}

		});

		/*
		 * This method will save data in database If there is any error and will
		 * redirect you to Draft
		 */
		cancelBtn.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				nextWindow.close();
				if (BitVaultConstants.mGeneralManagerCallbacks != null) {
					BitVaultConstants.mGeneralManagerCallbacks.draftActionCallback();
				}
			}
		});

		/*
		 * This method is used to for Retry action if there is some problem In
		 * PBC server and message is not sent
		 */
		retryBtn.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				nextWindow.close();
				newWindow.show();
				sendService.reset();
				sendService.restart();
			}

		});

		/**
		 * This method is called when pop up is closed.
		 */
		newWindow.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				sendTask.shutdownClient();
				sendService.cancel();
			}
		});
	}

	/**
	 * binding of UI to worker
	 * 
	 * @param worker
	 */
	public void bindToWorker(final Worker<Void> worker) {

		// bind worker to UI
		pb.progressProperty().bind(worker.progressProperty());
		imageText.textProperty().bind(worker.messageProperty());

		// Display the exception message when an exception occurs in the worker
		worker.exceptionProperty().addListener((prop, oldValue, newValue) -> {
			if (newValue != null) {
				Utils.getLogger().log(Level.SEVERE,"worker exception " + newValue);
			} else {
				Utils.getLogger().log(Level.SEVERE,"worker exception ");
			}
		});
	}

	public String getTxid() {
		return txid;
	}

	public void setTxid(String txid) {
		this.txid = txid;
	}

	public String getSkey() {
		return skey;
	}

	public void setSkey(String skey) {
		this.skey = skey;
	}

	public ArrayList<DraftAttachmentDTO> getAttachmentList() {
		return attachmentList;
	}

	public void setAttachmentList(ArrayList<DraftAttachmentDTO> attachmentList) {
		this.attachmentList = attachmentList;
	}
}
