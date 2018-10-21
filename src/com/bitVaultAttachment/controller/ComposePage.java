package com.bitVaultAttachment.controller;

import java.io.File;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import com.bitVaultAttachment.apiMethods.Utils;
import com.bitVaultAttachment.constant.BitVaultConstants;
import com.bitVaultAttachment.constant.GlobalCalls;
import com.bitVaultAttachment.constant.Messages;
import com.bitVaultAttachment.models.DraftAttachmentDTO;
import com.google.zxing.WriterException;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Created by vvdn on 5/29/2017. This class is used for sending New
 * BitVaultAttachments to Other Desktop
 */
public class ComposePage implements Initializable {

	// Add attachments button
	@FXML
	private Button addAttachments;

	// Next Button to compose
	@FXML
	private Button nextCompose;

	@FXML
	private Button sendBtn = new Button("Send");

	// BitValut value
	private String bitVaultValue;

	// session key generated
	private byte[] sessionKey = null;

	// vertical pane that contains all the attachments
	@FXML
	private VBox verticalComposePane;

	// For showing Encrypted session key
	final private String SYMALGORITHM = "AES/CBC/PKCS5Padding";

	// IV - 16 bytes static
	final byte[] ivBytes = { (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32, (byte) 0x56, (byte) 0x35, (byte) 0xE3,
			(byte) 0x03, (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32, (byte) 0x56, (byte) 0x35, (byte) 0xE3,
			(byte) 0x03 };

	private AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);

	@FXML
	private TextField txId;

	@FXML
	private AnchorPane mainPane;

	@FXML
	private VBox qrCodePane;

	@FXML
	private Label scanLabel;

	@FXML
	private ScrollPane attachScrollPane = new ScrollPane();

	@FXML
	private VBox attachContainerPane = new VBox(5);

	private HBox attachmentsbar, leftBar, rightBar;

	private Text attachName, attachSize, scanImage;

	private Text textDeleteImage;

	@FXML
	private ImageView imageViewNew;

	@FXML
	private HBox txnIdFooter = new HBox();

	private WebCamImageProvider imageProvider;

	public ArrayList<File> attachmentListFinal = new ArrayList<>();

	public String getBitVaultValue() {
		return bitVaultValue;
	}

	public void setBitVaultValue(String bitVaultValue) {
		this.bitVaultValue = bitVaultValue;
	}

	/**
	 * Method to add attachments in case of compose action
	 * 
	 * parameters null
	 *
	 * return void
	 */
	public void addAttachments() {
		FileChooser fc = new FileChooser();
		String path = System.getProperty("user.home");
		fc.setInitialDirectory(new File(path));
		List<File> selectedFiles = fc.showOpenMultipleDialog(null);
		if (selectedFiles != null) {
			for (int i = 0; i < selectedFiles.size(); i++) {
				if (!attachmentListFinal.contains(selectedFiles.get(i)) && selectedFiles.get(i).length() != 0) {
					add2TableView(selectedFiles.get(i), true);
					attachmentListFinal.add(selectedFiles.get(i));
				}
			}
		} else {
			Utils.getLogger().log(Level.SEVERE, "No File Found");
		}
		if (attachmentListFinal != null && attachmentListFinal.size() > 0) {
			nextCompose.setVisible(true);
		}
	}
	
	/**
	 * Init Method when Fxml is loaded
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		Utils.getLogger().info("addattachments");
		verticalComposePane.prefWidthProperty().bind(mainPane.widthProperty());
		verticalComposePane.prefHeightProperty().bind(mainPane.heightProperty());
		addAttachments.setVisible(true);
		
		createListTableView(true);
		nextCompose.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				try {
					nextButtonAction(verticalComposePane);

				} catch (WriterException e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	/**
	 * 
	 * This method is called when click on Next button and Generate QR Code.
	 *
	 * @param verticalComposePane2
	 * 
	 * @throws WriterException
	 */
	protected void nextButtonAction(VBox verticalComposePane2) throws WriterException {
		
		// Add and Next button removed
		txnIdFooter.getChildren().removeAll(nextCompose);
		addAttachments.setVisible(false);
		nextCompose.setVisible(false);
		// To Render table without edit functionality
		createListTableView(false);
		// Qr Code generated
		generateQRCodeForCompose(verticalComposePane);
	}

	/**
	 * This method create table View for attachments.
	 * @param isEditable
	 */
	private void createListTableView(boolean isEditable) {
	
		attachContainerPane.setAlignment(Pos.TOP_LEFT);
		attachContainerPane.setPrefWidth(650);
		attachContainerPane.getChildren().clear();

		for (File file2BeAdded : attachmentListFinal) {
			add2TableView(file2BeAdded, isEditable);
		}

		attachScrollPane.setPrefHeight(150);
		attachScrollPane.setPrefWidth(700);
		attachScrollPane.setContent(attachContainerPane);
		attachScrollPane.setFitToWidth(true);
		attachScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		attachScrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		attachScrollPane.setStyle("-fx-background-color:transparent;");
		VBox.setMargin(attachScrollPane, new Insets(0, 10, 0, 10));
		verticalComposePane.getStyleClass().add("parentContainerClass");
	}

	/**
	 * This method add row for each file in table view.
	 * @param file2BeAdded
	 * @param iseditable
	 */
	private void add2TableView(File file2BeAdded, boolean iseditable) {
		
		leftBar = new HBox();
		leftBar.setPrefWidth(100);
		leftBar.setAlignment(Pos.CENTER_LEFT);
		leftBar.setPadding(new Insets(0, 0, 0, 20));
		
		rightBar = new HBox();
		rightBar.setPrefWidth(100);
		rightBar.setPadding(new Insets(0, 10, 0, 0));
		rightBar.setSpacing(20);
		rightBar.setAlignment(Pos.CENTER_RIGHT);

		attachName = new Text();
		attachName.setText(file2BeAdded.getName());
		attachName.getStyleClass().add("nameColumn");
		attachName.setFont(Font.font("Arial", FontWeight.BOLD, 15));
		attachName.setTextAlignment(TextAlignment.LEFT);

		attachSize = new Text();
		attachSize.setText(getSizeInfo(file2BeAdded));
		attachSize.setFill(Color.GREY);
		attachSize.setFont(Font.font("Arial", FontWeight.BOLD, 15));
		attachSize.setTextAlignment(TextAlignment.LEFT);
		attachSize.prefWidth(100);

		EventHandler<MouseEvent> deleteClickHandler = e -> handleDeleteClick(e, file2BeAdded);
		
		// create the delete image text
		textDeleteImage = new Text();
		textDeleteImage.setText("e");
		textDeleteImage.setStyle("-fx-cursor:hand");
		textDeleteImage.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 18));
		textDeleteImage.addEventHandler(MouseEvent.MOUSE_CLICKED, deleteClickHandler);
		HBox.setMargin(textDeleteImage, new Insets(5, 12, 5, 5));

		leftBar.getChildren().addAll(attachName);
		rightBar.getChildren().addAll(attachSize);
		HBox.setHgrow(rightBar, Priority.ALWAYS);
		HBox.setHgrow(leftBar, Priority.ALWAYS);

		if (iseditable) {
			rightBar.getChildren().addAll(textDeleteImage);
		}
		// hbox for left and right bar
		attachmentsbar = new HBox(5);
		attachmentsbar.setPrefHeight(40);
		attachmentsbar.getChildren().addAll(leftBar, rightBar);
		attachmentsbar.setAlignment(Pos.CENTER);
		//HBox.setHgrow(attachmentsbar, Priority.ALWAYS);

		VBox container = new VBox(0, attachmentsbar);
		container.setMinHeight(40);
		container.setAlignment(Pos.CENTER);
		container.getStyleClass().add("containerClass");
		
		attachContainerPane.getChildren().add(container);
		attachScrollPane.setStyle("-fx-background-color:transparent;");
		attachScrollPane.setFitToWidth(true);
		VBox.setMargin(attachScrollPane, new Insets(0, 10, 0, 10));
		verticalComposePane.getStyleClass().add("parentContainerClass");
	}

	/**
	 * Handle delete image click event
	 * @param e
	 * @param file2BeAdded
	 * @param stage
	 */
	private void handleDeleteClick(MouseEvent e, File file2BeAdded) {
		
		attachmentListFinal.remove(file2BeAdded);
		createListTableView(true);
		if (attachmentListFinal == null || attachmentListFinal.size() < 1) {
			nextCompose.setVisible(false);
		} else {
			nextCompose.setVisible(true);
		}
		e.consume();
	}

	/**
	 * This method opens new window with webcam to scan transaction Id
	 * 
	 * @param e
	 * @param txId
	 * @param file2BeAdded
	 * @param stage
	 */
	private void handleScanAction(MouseEvent e, TextField txId) {

		//Read qr code from bitvault.
		Stage newWindow = new Stage();
		ImageView viewForCam = new ImageView();
		viewForCam.setFitHeight(250);
		viewForCam.setFitWidth(250);
		Label imageLabel = new Label();
		imageLabel.setText(Messages.SCAN_TXID);
		imageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
		imageLabel.setPadding(new Insets(0, 0, 0, 0));
		Label errorLabel = new Label();
		errorLabel.setText(Messages.INCORRECT_QR);
		errorLabel.setStyle("-fx-text-fill: red;");
		errorLabel.setVisible(false);
		VBox imageBox = new VBox();
		imageBox.getChildren().addAll(viewForCam, errorLabel, imageLabel);
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
		newWindow.initOwner(MainPage.stage);
		newWindow.setTitle("Scan");
		newWindow.initStyle(StageStyle.UTILITY);
		newWindow.initModality(Modality.WINDOW_MODAL);
		newWindow.setOnCloseRequest(event -> {
			imageProvider.stopCamera(true);
			txId.setEditable(true);
			txId.textProperty().unbind();
			txId.editableProperty().unbind();
		});
		newWindow.show();
		imageProvider = new WebCamImageProvider();
		imageProvider.setPostWebCamAction(BitVaultConstants.CAM_FOR_SCAN_TXN_ID);
		imageProvider.setStageKey(newWindow.toString());
		viewForCam.setVisible(true);
		viewForCam.imageProperty().bind(imageProvider.imageProperty());

		try {
			txId.textProperty().bindBidirectional(imageProvider.qrCodeProperty());
			txId.editableProperty().bindBidirectional(imageProvider.qrCodeEditProperty());
			errorLabel.visibleProperty().bindBidirectional(imageProvider.errorMsgVisibility());
		} catch (Exception e1) {
			Utils.getLogger().log(Level.SEVERE, "txid null", e1);
		}
		// start camera thread
		Thread t = new Thread(imageProvider);
		t.setDaemon(true);
		t.start();
		e.consume();

	}

	/**
	 * This method is used to create QR code from session key and List Size
	 * 
	 * @param pane--Pane
	 * in which QR code image will open
	 * 
	 * @throws WriterException
	 */
	private void generateQRCodeForCompose(VBox pane) throws WriterException {
		// Qr Code Generated
		createQrCode(pane);
		qrCodePane.setVisible(true);
		scanLabel.setVisible(true);

		txnIdFooter.setPrefHeight(30);
		sendBtn.setDisable(true);

		// Method to open new window and and bind webcam to get Tx Id
		bindWebCamWithButton(txnIdFooter);
		bindSendButton(txnIdFooter);

		VBox.setVgrow(txnIdFooter, Priority.ALWAYS);
		VBox.setMargin(txnIdFooter, new Insets(0, 10, 0, 10));
	}

	/**
	 * This method binds send button and sends request to Notification server.
	 * 
	 * @param txnIdFooter
	 * 
	 * returns null
	 */
	private void bindSendButton(HBox txnIdFooter) {
		
		sendBtn.getStyleClass().add("actionButtons");
		sendBtn.setPrefHeight(30);
		sendBtn.setPrefWidth(130);
		sendBtn.setPadding(new Insets(0, 10, 0, 20));

		sendBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				int attachId = 1;
				ArrayList<DraftAttachmentDTO> list = new ArrayList<>();

				// add attachment list
				try {
					DraftAttachmentDTO attachmentDTO;
					for (File file : attachmentListFinal) {
						attachmentDTO = new DraftAttachmentDTO();
						attachmentDTO.setAttachName(file.getName());
						attachmentDTO.setTxId(txId.getText());
						attachmentDTO.setAttachPath(file.getAbsolutePath());
						attachmentDTO.setSize((long) file.length());
						attachmentDTO.setAttachId(attachId++);
						list.add(attachmentDTO);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Sending pop-up page
				SendingPopupPage sendPopUp;

				try {
					sendPopUp = new SendingPopupPage(txId.getText(), Hex.toHexString(sessionKey), list);
					sendPopUp.sendAttachment();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		HBox senBtnHPane = new HBox();
		senBtnHPane.setPrefHeight(30);
		senBtnHPane.setAlignment(Pos.BOTTOM_RIGHT);
		senBtnHPane.getChildren().addAll(sendBtn);

		txnIdFooter.getChildren().addAll(senBtnHPane);
		HBox.setHgrow(senBtnHPane, Priority.ALWAYS);
	}

	/**
	 * This method is used to bind webcam to read Tx Id
	 * 
	 * @param txnIdFooter
	 * 
	 * returns null
	 */
	private void bindWebCamWithButton(HBox txnIdFooter) {
		
		Stage newWindow = new Stage();
		ImageView viewForCam = new ImageView();
		viewForCam.setFitHeight(250);
		viewForCam.setFitWidth(250);
		Label imageLabel = new Label();
		imageLabel.setText(Messages.SCAN_TXID);
		imageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
		imageLabel.setPadding(new Insets(20, 0, 0, 0));
		VBox imageBox = new VBox();
		imageBox.getChildren().addAll(viewForCam, imageLabel);
		imageBox.setAlignment(Pos.CENTER);
		imageBox.setSpacing(5);
		Scene scene = new Scene(imageBox);
		newWindow.setScene(scene);
		newWindow.setHeight(400);
		newWindow.setWidth(350);
		newWindow.setTitle("Scan");
		newWindow.initStyle(StageStyle.UTILITY);
		newWindow.initModality(Modality.APPLICATION_MODAL);
		newWindow.setOnCloseRequest(event -> {
			imageProvider.stopCamera(true);
		});

		txId = new TextField();
		txId.setPromptText(Messages.TXID_PROMPT);
		txId.setPrefWidth(500);
		txId.setPrefHeight(30);
		txId.setStyle("-fx-background-color:transparent");
		GlobalCalls.addTextLengthLimiter(txId, 64);
		txId.textProperty().addListener((observable, oldValue, newValue) -> {
			try {
				if (txId.getText().length() == 64) {
					sendBtn.setDisable(false);
				} else {
					sendBtn.setDisable(true);
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});

		EventHandler<MouseEvent> scanClickHandler = e -> handleScanAction(e, txId);
		// create the delete image text
		scanImage = new Text();
		scanImage.setText("h");
		scanImage.setStyle("-fx-cursor:hand");
		scanImage.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 25));
		scanImage.addEventHandler(MouseEvent.MOUSE_CLICKED, scanClickHandler);

		HBox.setHgrow(txId, Priority.ALWAYS);

		HBox txIdHPaneMain = new HBox();
		txIdHPaneMain.setAlignment(Pos.BOTTOM_LEFT);
		txIdHPaneMain.setSpacing(5);
		txIdHPaneMain.setPadding(new Insets(0, 5, 0, 0));
		txIdHPaneMain.setStyle("-fx-background-color:white;");
		txIdHPaneMain.getChildren().addAll(txId, scanImage);
		txIdHPaneMain.setMinHeight(30);
		txIdHPaneMain.setMaxHeight(30);
		HBox.setMargin(scanImage, new Insets(0, 0, 3, 0));

		HBox txIdHPane = new HBox();
		txIdHPane.setAlignment(Pos.BOTTOM_LEFT);
		txIdHPane.setSpacing(5);
		txIdHPane.getChildren().addAll(txIdHPaneMain);
		txIdHPane.setMinHeight(30);

		txnIdFooter.getChildren().addAll(txIdHPane);
		HBox.setHgrow(txIdHPaneMain, Priority.ALWAYS);
		HBox.setHgrow(txIdHPane, Priority.ALWAYS);
	}

	/**
	 * This method is used to create QR code from session key and List Size
	 * 
	 * @param pane
	 * 
	 * @throws WriterException
	 * 
	 * @return void
	 */
	private void createQrCode(VBox pane) throws WriterException {

		// Session key
		String QRdata = "";
		try {
			// generate new session key
			sessionKey = generateSessionKey();
			Utils.getLogger().log(Level.FINEST, "Session Key:" + Hex.toHexString(sessionKey) + "\n");
			
			// read device token in bytes
			UUID uuidOfToken = null;
			try {
				uuidOfToken = UUID.fromString(MainPage.bitVaultToken.trim());
			} catch (Exception e1) {
				Utils.getLogger().log(Level.SEVERE, "failed to parse bitvault-token", e1);
				e1.printStackTrace();
			}
			long hi = uuidOfToken.getMostSignificantBits();
			long lo = uuidOfToken.getLeastSignificantBits();
			byte[] uuidInBytes = ByteBuffer.allocate(16).putLong(hi).putLong(lo).array();
			
			// use device token as encryption key
			SecretKey secKey = null;
			try {
				secKey = getAESKey(Hex.toHexString(uuidInBytes).trim());
			} catch (Exception e) {
				Utils.getLogger().log(Level.SEVERE, "TXID key encoding error", e);
				throw e;
			}

			Cipher ecipher1 = null;
			try {
				ecipher1 = Cipher.getInstance(SYMALGORITHM, "BC");
				ecipher1.init(Cipher.ENCRYPT_MODE, secKey, ivSpec);
			} catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
				Utils.getLogger().log(Level.SEVERE, "txid key init error", e);
				throw e;
			}
			// encrypt session key with device token
			byte[][] bWrite = null;
			bWrite = symEncryption(sessionKey, sessionKey.length, ecipher1);
			QRdata = Base64.toBase64String(bWrite[0]);		
		} catch (Exception e) {
			e.printStackTrace();
		}
		// find total file size of attachments
		double totalSize = 0;
		for (File file : attachmentListFinal) {
			totalSize = totalSize + file.length();
		}
		// display qr code
		QRdata = QRdata + ":" + totalSize;
		imageViewNew = new ImageView();
		imageViewNew = GenerateQRCode.generateQRCode(QRdata);
		qrCodePane.getChildren().add(imageViewNew);
	}

	/**
	 * Gets AES Key
	 * 
	 * @param key
	 *            as string (hex)
	 * @return key as SecretKey object
	 * @throws Exception
	 */
	public SecretKey getAESKey(String key) throws Exception {

		SecretKey symKey = null;
		byte[] input = Hex.decode(key);
		symKey = new SecretKeySpec(input, "AES");

		return symKey;
	}

	/**
	 * Symmetric encryption of data using AES256
	 * 
	 * @param input
	 * @param length
	 * @param ecipher
	 * @return returns encrypted data and size in result[0] and result[1]
	 * @throws Exception
	 */
	public byte[][] symEncryption(byte[] input, int length, Cipher ecipher) throws Exception {

		byte[][] result = new byte[2][];

		try {
			byte[] cipherText = new byte[ecipher.getOutputSize(length)];
			int ctLength = ecipher.update(input, 0, length, cipherText, 0);
			ctLength += ecipher.doFinal(cipherText, ctLength);

			result[0] = cipherText;
			result[1] = BigInteger.valueOf(ctLength).toByteArray();

		} catch (Exception e) {
			throw e;
		}

		return result;
	}

	/**
	 * Symmetric decryption of data using AES256
	 * 
	 * @param cipherText
	 * @param length
	 * @param dcipher
	 * @return returns decrypted data and size in result[0] and result[1]
	 * @throws Exception
	 */
	public byte[][] symDecryption(byte[] cipherText, int length, Cipher dcipher) throws Exception {

		byte[][] result = new byte[2][];

		try {
			byte[] plainText = new byte[dcipher.getOutputSize(length)];
			int ptLength = dcipher.update(cipherText, 0, length, plainText, 0);
			ptLength += dcipher.doFinal(plainText, ptLength);

			result[0] = plainText;
			result[1] = BigInteger.valueOf(ptLength).toByteArray();

		} catch (Exception e) {
			throw e;
		}

		return result;
	}

	/**
	 * This method is used to generate session key
	 * 
	 * @return Session key encoded in bytes
	 * @throws Exception
	 */
	private byte[] generateSessionKey() throws Exception {
		
		Key SessionKey = genSessionKey();
		byte[] encodedSessionKey = SessionKey.getEncoded();
		return encodedSessionKey;
	}

	/**
	 * Generate Session Key - AES
	 * @return
	 * @throws Exception
	 */
	public static Key genSessionKey() throws Exception {
		
		Security.addProvider(new BouncyCastleProvider());
		KeyGenerator SessionGenerator = KeyGenerator.getInstance("AES", "BC");
		SessionGenerator.init(256);
		Key SessionKey = SessionGenerator.generateKey();
		return SessionKey;
	}
	
	/**
	 * create template for compose page
	 * @return
	 */
	public VBox creatTemplate() {
		verticalComposePane = new VBox();
		verticalComposePane.setPrefHeight(450);
		verticalComposePane.setPrefHeight(750);
		verticalComposePane.setFillWidth(true);
		verticalComposePane.setSpacing(5);

		Text label = new Text();
		label.setText("Compose");
		label.getStyleClass().add("headerLabel");
		label.setFont(Font.font("Arial", FontWeight.LIGHT, 12));
		label.setTextAlignment(TextAlignment.LEFT);

		HBox firstHPane = new HBox();
		firstHPane.setPrefHeight(30);
		firstHPane.setPrefWidth(130);

		HBox leftHBox = new HBox();
		Text attachLabel = new Text();
		attachLabel.setText("Attachments");
		attachLabel.getStyleClass().add("inboxDownloadNewContainer");

		leftHBox.getChildren().add(attachLabel);
		HBox.setMargin(attachLabel, new Insets(10, 0, 0, 0));
		leftHBox.setPadding(new Insets(0, 0, 0, 3));
		HBox.setHgrow(leftHBox, Priority.ALWAYS);

		HBox rightHBox = new HBox();
		addAttachments = new Button();
		addAttachments.setPrefHeight(30);
		addAttachments.setPrefWidth(130);
		addAttachments.getStyleClass().add("addAttachments");

		addAttachments.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				addAttachments();
			}
		});

		rightHBox.getChildren().add(addAttachments);
		rightHBox.setAlignment(Pos.CENTER_RIGHT);
		HBox.setHgrow(rightHBox, Priority.ALWAYS);

		firstHPane.getChildren().addAll(leftHBox, rightHBox);

		HBox attachHPane = new HBox();
		attachHPane.setPrefHeight(350);
		attachHPane.setPrefWidth(200);
		attachHPane.setFillHeight(true);

		attachScrollPane = new ScrollPane();
		attachScrollPane.setPrefHeight(550);
		attachScrollPane.setPrefWidth(228);
		attachScrollPane.setFitToWidth(true);
		attachScrollPane.setFitToHeight(true);

		qrCodePane = new VBox();
		qrCodePane.setPrefHeight(350);
		qrCodePane.setPrefWidth(228);

		qrCodePane.setPadding(new Insets(10, 0, 0, 0));
		qrCodePane.setSpacing(5);
		qrCodePane.setAlignment(Pos.CENTER);

		scanLabel = new Label();
		scanLabel.setText(Messages.SCAN_QR);
		scanLabel.setVisible(false);
		scanLabel.setFont(Font.font("Arial", FontWeight.LIGHT, 12));
		scanLabel.setAlignment(Pos.TOP_CENTER);
	
		qrCodePane.getChildren().addAll(scanLabel);
		attachHPane.getChildren().addAll(attachScrollPane, qrCodePane);
		HBox.setHgrow(attachScrollPane, Priority.ALWAYS);

		HBox footerHPane = new HBox();
		footerHPane.setPrefHeight(40);
		footerHPane.setPrefWidth(200);
		footerHPane.setFillHeight(true);
		footerHPane.setPadding(new Insets(0, 15, 0, 0));

		nextCompose = new Button();
		nextCompose.setPrefHeight(30);
		nextCompose.setPrefWidth(130);
		nextCompose.setText("Next");
		nextCompose.setVisible(false);
		nextCompose.getStyleClass().add("actionButtons");
		createListTableView(true);
		nextCompose.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				try {
					nextButtonAction(verticalComposePane);

				} catch (WriterException e1) {
					e1.printStackTrace();
				}
			}
		});

		txnIdFooter.getChildren().add(nextCompose);
		txnIdFooter.setAlignment(Pos.BOTTOM_RIGHT);
		txnIdFooter.setPadding(new Insets(10, 20, 10, 0));
		VBox.setVgrow(txnIdFooter, Priority.ALWAYS);

		verticalComposePane.getChildren().addAll(label, firstHPane, attachHPane, txnIdFooter);

		VBox.setMargin(label, new Insets(15, 10, 0, 10));
		VBox.setMargin(firstHPane, new Insets(0, 10, 10, 10));
		VBox.setMargin(attachHPane, new Insets(0, 10, 0, 10));
		VBox.setMargin(footerHPane, new Insets(0, 10, 0, 10));

		return verticalComposePane;
	}
	
	/**
	 * returns attachment size for display
	 * @param f
	 * @return
	 */
	String getSizeInfo (File f){
		
		Double sizeInKb = (double) (f.length() / (double) 1024);
		return sizeInKb > 1024 ? (new DecimalFormat("##.##").format(sizeInKb / 1024) + " MB")
				: (new DecimalFormat("##.##").format(sizeInKb).equalsIgnoreCase("0") ? "0.01 KB"
						: new DecimalFormat("##.##").format(sizeInKb) + " KB");
	}
}
