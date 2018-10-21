package com.bitVaultAttachment.controller;


import java.io.File;
import java.math.BigInteger;
import java.util.logging.Level;

import com.bitVaultAttachment.apiMethods.UnlockingTask;
import com.bitVaultAttachment.apiMethods.Utils;
import com.bitVaultAttachment.constant.BitVaultConstants;

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
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
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

public class UnlockPopupPage {
	private String txid;
	private String skey;
	private String hashOfTxnId;
	private String bitVaultToken;
	private File file;

	private Text imageText;
	private ProgressBar pb;

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

	public Service<Void> sendService;
	
	/**
	 * Constructor for Pop-Up Page
	 * @param txid
	 * @param skey
	 * @param attachmentList
	 */
	public UnlockPopupPage(String bitVaultToken, String txid, String skey,String hashOfTxnId, File file)
			throws Exception {
		// check if key format is correct
		try {
			new BigInteger(txid, 16);
			new BigInteger(skey, 16);
			if( ( txid.length() != 64 ) || ( skey.length() != 64 ) )
				throw new Exception();
		}
		catch (NumberFormatException e) {
			Utils.getLogger().log(Level.SEVERE, "key format error", e);
			throw e;
		}

		// set parameters
		this.setTxid(txid);
		this.setSkey(skey);
		this.setFile(file);
		this.setHashOfTxnId(hashOfTxnId);
		this.setBitVaultToken(bitVaultToken);

		// create sending service
		sendService = new Service<Void>() {
			@Override
			protected Task<Void> createTask() {
				return new UnlockingTask(bitVaultToken,txid, skey,hashOfTxnId, file);
			}
		};
	}

	/**
	 * attachment unlocking method
	 */
	public void unlockAttachment(){
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
	 * This Method will create Pop-ups and bind action actions for Buttons in case of Compose action.
	 * 
	 */
	public void createPopup() {

		// start a new stage
		newWindow = new Stage();
		nextWindow = new Stage();

		// text to show progress
		imageText=new Text();
		nextImageText=new Text();

		// Ok button
		OkBtn=new Button("OK");
		OkBtn.setPrefHeight(30);
		OkBtn.setPrefWidth(100);
		OkBtn.getStyleClass().add("actionButtons");

		// cancel button
		cancelBtn=new Button("Cancel");
		cancelBtn.setPrefHeight(30);
		cancelBtn.setPrefWidth(100);
		cancelBtn.getStyleClass().add("actionButtons");

		// retry button
		retryBtn=new Button("Retry");
		retryBtn.setPrefHeight(30);
		retryBtn.setPrefWidth(100);
		retryBtn.getStyleClass().add("actionButtons");

		// hbox holding buttons
		btnHbox =new HBox();
		btnHbox.setAlignment(Pos.CENTER);
		btnHbox.setPadding(new Insets(50,0,0,0));
		btnHbox.setVisible(false);
		btnHbox.setSpacing(20);

		// progress bar
		pb=new ProgressBar(0);
		pb.getStyleClass().add("progressBarCss");
		pb.setProgress(0);
		pb.setPrefWidth(420);
		pb.setPrefHeight(30);
		pb.setPadding(new Insets(50,0,0,0));

		// set message of text
		imageText.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14	));
		nextImageText.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14	));

		// information box
		infoBox=new VBox();
		infoBox.getChildren().addAll(imageText,pb);

		VBox.setVgrow(pb,Priority.ALWAYS);
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
		nextInfoBox=new VBox();
		nextInfoBox.getChildren().addAll(nextImageText,btnHbox);

		VBox.setVgrow(pb,Priority.ALWAYS);
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
	void addEventHandlers (){

		/**
		 * This method is called when message is sent successfully
		 */
		sendService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				btnHbox.getChildren().clear();
				btnHbox.setVisible(true);
				if( btnHbox.getChildren().isEmpty() ) {
					btnHbox.getChildren().addAll(OkBtn); 
				}
				nextImageText.setText("File(s) Unlocked Successfully.");
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
				//show error on failed
				nextImageText.setFill(Color.RED);
				btnHbox.setVisible(true);
				nextImageText.setText("Error: File(s) Unlocking Failed");
				if (btnHbox.getChildren().isEmpty()){
					btnHbox.getChildren().addAll(retryBtn,cancelBtn);
				}
				newWindow.close();
				nextWindow.show();
			}
		});

		/**
		 * This method is called when Message is sent successfully it will redirect you to Inbox
		 */
		OkBtn.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				nextWindow.close();
				
				if (BitVaultConstants.mGeneralManagerCallbacks != null) {
					BitVaultConstants.mGeneralManagerCallbacks.inboxActionCallback();
				}
				// show attachment callback
            	BitVaultConstants.mInboxPageCallbacks.showAttachmentCallback(hashOfTxnId);
			}
		});

		/**
		 * redirect to Inbox when cancelled
		 */
		cancelBtn.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				nextWindow.close();
			}
		});

		/**
		 * This method is used to for Retry action if there 
		 * is some problem In PBC server and message is not sent
		 */
		retryBtn.setOnAction(new EventHandler<ActionEvent>() {

			//todo Retry action will resend request to PBC 
			@Override
			public void handle(ActionEvent event) {
				
				nextWindow.close();
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
				newWindow.setTitle("Scan");
				newWindow.initStyle(StageStyle.UTILITY);
				newWindow.initModality(Modality.APPLICATION_MODAL);
				newWindow.show();
				WebCamImageProvider imageProvider = new WebCamImageProvider();
				imageProvider.setPostWebCamAction(BitVaultConstants.CAM_FOR_UNLOCK);
				imageProvider.setStageKey(newWindow.toString());
				imageProvider.setHashOfTxnId(hashOfTxnId);
				viewForCam.setVisible(true);
				viewForCam.imageProperty().bind(imageProvider.imageProperty());
				errorLabel.visibleProperty().bindBidirectional(imageProvider.errorMsgVisibility());

				// start camera thread
				Thread t = new Thread(imageProvider);
				t.setDaemon(true);
				t.start();
			}
		});

		/**
		 * This method is called when pop up is closed.
		 */
		newWindow.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event){
				sendService.cancel();
			}
		});
	}

	/**
	 * binding of UI to worker
	 * @param worker
	 */
	public void bindToWorker(final Worker<Void> worker){

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

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getHashOfTxnId() {
		return hashOfTxnId;
	}

	public void setHashOfTxnId(String hashOfTxnId) {
		this.hashOfTxnId = hashOfTxnId;
	}

	public String getBitVaultToken() {
		return bitVaultToken;
	}

	public void setBitVaultToken(String bitVaultToken) {
		this.bitVaultToken = bitVaultToken;
	}
}
