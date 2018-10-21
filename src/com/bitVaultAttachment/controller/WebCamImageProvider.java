package com.bitVaultAttachment.controller;
/**
 * Created by vvdn on 5/17/2017.
 */

import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import com.bitVaultAttachment.apiMethods.MqttClientAsync;
import com.bitVaultAttachment.apiMethods.Utils;
import com.bitVaultAttachment.constant.BitVaultConstants;
import com.bitVaultAttachment.constant.GlobalCalls;
import com.github.sarxos.webcam.Webcam;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.sun.javafx.stage.StageHelper;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class WebCamImageProvider extends Task<Void> {

	private boolean stopCamera = false;
	private BufferedImage grabbedImage;
	private Webcam webCam = null;
	private ObjectProperty<Image> imageProperty = new SimpleObjectProperty<Image>();
	private ObjectProperty<String> qrCodeProperty = new SimpleObjectProperty<String>();
	private ObjectProperty<Boolean> imageVisibility = new SimpleObjectProperty<Boolean>();
	private ObjectProperty<Boolean> errorMsgVisibility = new SimpleObjectProperty<Boolean>();
	private ObjectProperty<Image> imageRegisterVisibility = new SimpleObjectProperty<Image>();
	private ObjectProperty<Boolean> qrCodeEditProperty = new SimpleObjectProperty<Boolean>();
	private StringProperty fps = new SimpleStringProperty();
	private String qrCode = "";
	private String redirectFxml;
	private Integer postWebCamAction;
	private String stageKey;
	private String hashOfTxnId;

	public String getHashOfTxnId() {
		return hashOfTxnId;
	}

	public void setHashOfTxnId(String hashOfTxnId) {
		this.hashOfTxnId = hashOfTxnId;
	}

	private boolean isRegisterSuccessful = false;

	public boolean isRegisterSuccessful() {
		return isRegisterSuccessful;
	}

	public void setRegisterSuccessful(boolean isRegisterSuccessful) {
		this.isRegisterSuccessful = isRegisterSuccessful;
	}

	public String getStageKey() {
		return stageKey;
	}

	public void setStageKey(String stageKey) {
		this.stageKey = stageKey;
	}

	public Integer getPostWebCamAction() {
		return postWebCamAction;
	}

	public void setPostWebCamAction(Integer postWebCamAction) {
		this.postWebCamAction = postWebCamAction;
	}

	public String getRedirectFxml() {
		return redirectFxml;
	}

	public void setRedirectFxml(String redirectFxml) {
		this.redirectFxml = redirectFxml;
	}

	/**
	 * Returns QR String from Image
	 * @param image
	 * @return
	 * @throws NotFoundException
	 */
	public static String readQRCode(BufferedImage image) throws NotFoundException {
		BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
		Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap);
		return qrCodeResult.getText();
	}

	/*
	 * This method is used to open Webcam Read QR code from Image Convert it
	 * into String close webcam Redirect it to specific FXML
	 *
	 */
	@Override
	protected Void call() throws Exception {

		webCam = Webcam.getDefault();
		try {
			webCam.open();
		} catch (Exception e1) {
			Utils.getLogger().log(Level.SEVERE, "Failed to open camera.", e1);
			Platform.runLater(() -> {
				// Alert
				Alert alert = new Alert(AlertType.ERROR, "Failed to open camera.", ButtonType.OK);
				alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
				alert.showAndWait();
			});
			throw e1;
		}

		int counter = 0;
		LocalTime ts = LocalTime.now();
		while (!stopCamera) {
			try {
				// First display the grabbed image
				if ((grabbedImage = webCam.getImage()) != null) {
					final Image mainiamge = SwingFXUtils.toFXImage(grabbedImage, null);
					Platform.runLater(() -> imageProperty.set(mainiamge));
					grabbedImage.flush();
				}

				// Analyse the image if a qr code could be found
				try {
					qrCode = readQRCode(grabbedImage);
					qrCodeProperty.set(qrCode);
					qrCodeEditProperty.set(true);
					String[] keys =null;
					if (!qrCode.trim().equalsIgnoreCase("")
							&& postWebCamAction == BitVaultConstants.CAM_FOR_REGISTRATION) {

						imageVisibility.set(true);
						imageRegisterVisibility.set(new Image("/images/registering.png"));
						// request to notification server to register for isRegisterSuccessful
						Utils.getLogger().log(Level.INFO,"Registering...");
						MqttClientAsync mqttclient = new MqttClientAsync(qrCode);
						isRegisterSuccessful = mqttclient.mqttCheckRegistration();

						if (postWebCamAction == BitVaultConstants.CAM_FOR_REGISTRATION && !isRegisterSuccessful) {
							Utils.getLogger().log(Level.INFO,"Registration Failed");
							imageVisibility.set(true);
							imageRegisterVisibility.set(new Image("/images/scanError.png"));
							grabbedImage.flush();
							qrCode = null;
							Thread.sleep(2000); // wait

						} else if (isRegisterSuccessful) {
							Utils.getLogger().log(Level.INFO,"Registration Success");
							MainPage.bitVaultToken = qrCode;
							break;
						}
					} else if (!qrCode.trim().equalsIgnoreCase("")
							&& postWebCamAction == BitVaultConstants.CAM_FOR_SCAN_TXN_ID
							&& (qrCode.trim().length() != 64 || !qrCode.trim().matches("[0-9a-fA-F]+")) ) {
						errorMsgVisibility.set(true);
						qrCode="";
						qrCodeProperty.set(qrCode);

					} else if(!qrCode.trim().equalsIgnoreCase("") &&  
							postWebCamAction == BitVaultConstants.CAM_FOR_UNLOCK){
						// split keys and parse
						keys = qrCode.split("\\:");
						if(keys.length>1 && (keys[0]!=null && keys[1]!=null) && (keys[0].trim().length() == 64 && keys[0].trim().matches("[0-9a-fA-F]+") &&
								(keys[1].trim().length() == 64 && keys[1].trim().matches("[0-9a-fA-F]+")))){
							errorMsgVisibility.set(false);
							break;

						} else{
							errorMsgVisibility.set(true);
							qrCode="";
							qrCodeProperty.set(qrCode);
						}
					} else {
						errorMsgVisibility.set(false);
						break;
					}
				} catch (Throwable e) {

				}
				// Check the frame rate
				if (ts.getSecond() == LocalTime.now().getSecond()) {
					counter++;
				} else {
					ts = LocalTime.now();
					final int value = counter;
					counter = 0;
					Platform.runLater(() -> fps.set(value + ""));
				}

			} catch (Exception e) {
				Utils.getLogger().log(Level.SEVERE, "webcam read error", e);
			}
		}
		webCam.close();

		/**
		 * tasks to run later
		 */
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					// action when scanning for registration
					if (postWebCamAction == BitVaultConstants.CAM_FOR_REGISTRATION) {

						// check registration status
						if (!isRegisterSuccessful) {
							imageRegisterVisibility.set(new Image("/images/scanError.png"));
							stopCamera = true;
						} else {
							// set as connected
							GlobalCalls.isConnectedNotification.set(true);
							// get DB path and create scheme
							try {
								String path=BitVaultConstants.PATH_FOR_DATABASE;
								BitVaultConstants.createDatabaseScheme(path,qrCode);
								// load fxml page
								FXMLLoader loader = new FXMLLoader(WebCamImageProvider.class.getResource(redirectFxml));
								VBox root = loader.load();
								Scene scene = new Scene(root);
								scene.getStylesheets().add("bitVaultMain.css");
								MainPage.stage.setScene(scene);
							} catch (Exception e) {
								Utils.getLogger().log(Level.SEVERE, "Error Starting Application", e);
								// Alert
								Alert alert = new Alert(AlertType.ERROR, "Loading application failed", ButtonType.OK);
								alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
								alert.showAndWait();
								System.exit(0); // exit application
							}
						}
					}
					// return action in case of scan TxId
					else if (postWebCamAction == BitVaultConstants.CAM_FOR_SCAN_TXN_ID
							|| postWebCamAction == BitVaultConstants.CAM_FOR_UNLOCK) {

						qrCodeProperty.unbind();
						final List<Stage> allStages = StageHelper.getStages();
						Iterator<Stage> iterator = allStages.iterator();
						Stage stage2BeClosed = null;
						while (iterator.hasNext()) {
							Stage stage = (Stage) iterator.next();
							if (stageKey.toString().equalsIgnoreCase(stage.toString())) {
								stage2BeClosed = stage;
							}
						}
						if (stage2BeClosed != null)
							stage2BeClosed.close();
					}
					// action in case of unlocking files
					if (postWebCamAction == BitVaultConstants.CAM_FOR_UNLOCK && !qrCode.trim().equalsIgnoreCase("")) {

						String txidDecoded = null;
						String skeyDecoded = null;
						String[] keys = qrCode.split("\\:");
						txidDecoded = keys[0];
						skeyDecoded = keys[1];
						UnlockPopupPage unlockPopUp;
						File encFile = null;

						// open file to unlock
						try {
							encFile = new File(BitVaultConstants.PATH_FOR_DOWNLOADED_FILES + Hex.toHexString(Base64.decode(hashOfTxnId)) + ".zip_enc");
						} catch (Exception e) {
							Utils.getLogger().log(Level.SEVERE, "file read error", e);
						}
						// unlock files
						try {
							unlockPopUp = new UnlockPopupPage(MainPage.bitVaultToken,
									txidDecoded.trim(), skeyDecoded.trim(), hashOfTxnId.trim(), encFile);
							unlockPopUp.unlockAttachment();
						} catch (Exception e) {
							Utils.getLogger().log(Level.SEVERE, "unlock error", e);
						}
					}
				} catch ( Exception e) {
					Utils.getLogger().log(Level.SEVERE, "camera post process error", e);
				}
			}
		});
		return null;
	}

	public StringProperty fpsProperty() {
		return fps;
	}

	public boolean isStopCamera() {
		return stopCamera;
	}

	public void stopCamera(boolean stopCamera) {
		this.stopCamera = stopCamera;
	}

	public ObjectProperty<Image> imageProperty() {
		return imageProperty;
	}

	public String getQrCode() {
		return qrCode;
	}

	public ObjectProperty<String> qrCodeProperty() {
		return qrCodeProperty;
	}

	public ObjectProperty<Boolean> imageVisibility() {
		return imageVisibility;
	}

	public ObjectProperty<Boolean> errorMsgVisibility() {
		return errorMsgVisibility;
	}


	public ObjectProperty<Image> imageRegisterVisibility() {
		return imageRegisterVisibility;
	}

	public ObjectProperty<Boolean> qrCodeEditProperty() {
		return qrCodeEditProperty;
	}
}
