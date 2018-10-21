package com.bitVaultAttachment.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;

import com.bitVaultAttachment.apiMethods.Utils;
import com.bitVaultAttachment.constant.BitVaultConstants;
import com.bitVaultAttachment.constant.GlobalCalls;
import com.bitVaultAttachment.iclasses.GeneralManagerCallbacks;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Popup;

/**
 * This is the class which calls Inbox , Compose, Draft Methods
 * 
 * Created by vvdn on 5/22/2017.
 */
public class GeneralPage implements Initializable {

	@FXML
	private VBox generalPane = new VBox();

	@FXML
	private ScrollPane generalScrollPane;

	@FXML
	private Text notificationIcon;
	
	@FXML
	private static Text connectionIcon = new Text();

	@FXML
	private static Text notificationIconNew = new Text();

	@FXML
	private Text searchIcon;

	@FXML
	private TextField searchText;

	private String bitVaultValue;

	private Integer action;

	private boolean isSearch = false;
	
	private boolean isInitGeneralPage = true;
	
	private static Popup popup = new Popup();

	private static VBox textNotificationVbox = new VBox();

	@FXML
	private HBox searchBarMenu;
	
	@FXML
	private HBox connectionBar;

	public String getBitVaultValue() {
		return bitVaultValue;
	}

	public void setBitVaultValue() {
		this.bitVaultValue = MainPage.bitVaultToken;
	}

	public Integer getAction() {
		return action;
	}

	public void setAction(Integer action) {
		this.action = action;
	}

	/**
	 * Redirects to Inbox Fxml
	 * 
	 * @throws IOException
	 */
	@FXML
	public void inboxAction() throws IOException {
		if (generalPane != null) {

			// check if general page is launched for first time
			if( !isInitGeneralPage ){
				GlobalCalls.list.clear();
				notificationIconNew.setVisible(false);
			}
			else if ( isInitGeneralPage && !GlobalCalls.list.isEmpty() ){
				updateNotificationUI();
				notificationIconNew.setVisible(true);
				isInitGeneralPage = false;
			}
			else{
				notificationIconNew.setVisible(false);
				isInitGeneralPage = false;
			}

			// set notification pop-up handlers
			popup.setAutoHide(true);
			popup.setConsumeAutoHidingEvents(true);
			popup.setHideOnEscape(true);
			popup.setAutoFix(true);
			popup.setWidth(200);
			popup.setHeight(200);
			action = BitVaultConstants.IS_FOR_INBOX;
			generalPane.setVisible(true);
			generalPane.getChildren().clear();
			generalPane.requestFocus();

			if (!isSearch)
				searchText.setText("");
			InboxPage inboxPage = new InboxPage();
			inboxPage.setSearchText(searchText != null ? searchText.getText().toString().trim() : "");
			VBox vbox = inboxPage.inBoxAction();

			generalPane.getChildren().add(vbox);
			VBox.setVgrow(vbox, Priority.ALWAYS);
		} else {
			Utils.getLogger().log(Level.SEVERE,"generalPane is null");
		}

	}

	/**
	 * Redirects to Compose Fxml
	 * 
	 * @throws IOException
	 */
	@FXML
	public void composeAction() throws IOException {
		action = BitVaultConstants.IS_FOR_COMPOSE;
		searchText.setText("");

		generalPane.setVisible(true);
		generalPane.getChildren().clear();

		ComposePage composePage = new ComposePage();
		VBox vbox = composePage.creatTemplate();

		generalPane.getChildren().add(vbox);
		VBox.setVgrow(vbox, Priority.ALWAYS);
	}

	/**
	 * Redirects to Receive Fxml
	 * 
	 * @throws IOException
	 */
	@FXML
	public void draftAction() throws IOException {

		action = BitVaultConstants.IS_FOR_DRAFT;

		// make visible
		generalPane.setVisible(true);
		generalPane.getChildren().clear();
		
		if (!isSearch)
			searchText.setText("");

		// add draft page to vbox
		DraftPage draftPage = new DraftPage();
		draftPage.setSearchText(searchText != null ? searchText.getText().toString().trim() : "");
		VBox vbox = draftPage.draftAction();

		generalPane.getChildren().add(vbox);
		VBox.setVgrow(vbox, Priority.ALWAYS);
	}

	/**
	 * Redirects to Pending Fxml
	 * 
	 * @throws IOException
	 */
	@FXML
	private void pendingAction() throws IOException {
		generalPane.setVisible(true);
		generalPane.getChildren().clear();
		FXMLLoader loader = new FXMLLoader(WebCamImageProvider.class.getResource("/fxml/PendingPage.fxml"));
		AnchorPane ap = loader.load();
		PendingPage gp = loader.getController();
		gp.setBitVaultValue(bitVaultValue);
		generalPane.getChildren().addAll(ap);
	}

	/**
	 * This method is used for search functionality
	 * 
	 */
	public void notificationAction() {
		// clear search box
		searchText.setText("");
		
		if(!GlobalCalls.list.isEmpty()){
			// notification pointer
			double notificationWidth = 260;
			double notBoxDisplacementX = 30;
			double notBoxDisplacementY = 20;
			Polygon triangle = new Polygon();
			triangle.getPoints().addAll(notificationWidth - notBoxDisplacementX + 10, 0.0, 
										notificationWidth - notBoxDisplacementX     , 10.0, 
										notificationWidth - notBoxDisplacementX + 20, 10.0);
			triangle.setFill(Color.WHITE);

			AnchorPane notificationPointerHbox = new AnchorPane(triangle);

			// scroll pane for notifications
			ScrollPane spNotification = new ScrollPane();
			spNotification.setMinHeight(40);
			spNotification.setPrefWidth(notificationWidth);
			spNotification.setMaxHeight(150);
			spNotification.setContent(textNotificationVbox);
			spNotification.setFitToWidth(true);
			spNotification.setHbarPolicy(ScrollBarPolicy.NEVER);
			spNotification.setStyle("-fx-background-insets: 0;" + "-fx-background: white;");

			// add to vbox
			VBox notificationVbox = new VBox(notificationPointerHbox, spNotification);
			popup.getContent().addAll(notificationVbox);

			// set position on screen
			Bounds boundsInScreen = notificationIcon.localToScreen(notificationIcon.getBoundsInLocal());
			popup.setAnchorX(boundsInScreen.getMinX() - notificationWidth + notBoxDisplacementX);
			popup.setAnchorY(boundsInScreen.getMinY() + notBoxDisplacementY);
			popup.show(MainPage.stage);
		}

	
	}

	/**
	 * This method is used for Search
	 * 
	 * @throws IOException
	 * 
	 */
	public void searchAction() throws IOException {
		// Create a custom Notification without icon

		isSearch = true;
		if (action == BitVaultConstants.IS_FOR_INBOX) {
			inboxAction();
		} else if (action == BitVaultConstants.IS_FOR_DRAFT) {
			draftAction();
		}
		isSearch = false;
	}
	
	/**
	 * Initialize general Page
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		Utils.initLogger(); // logger
		try {
			generalPane.setPrefHeight(500);
			generalPane.setPrefWidth(750);
			generalPane.getStyleClass().add("onFocusCSS");
			generalPane.setFillWidth(true);

			generalScrollPane.setContent(generalPane);
			inboxAction();

			notificationIcon.setText("d");
			notificationIcon.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 18));
			notificationIcon.setFill(Color.WHITE);
			
			connectionIcon.setText("s");
			connectionIcon.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 22));
			connectionIcon.setFill(Color.WHITE);
			connectionBar.getChildren().add(connectionIcon);
			HBox.setMargin(connectionIcon, new Insets(11, 0, 0, 0));

			notificationIconNew.setText("o");
			notificationIconNew.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 18));
			notificationIconNew.setFill(Color.RED);
			searchBarMenu.getChildren().add(notificationIconNew);
			HBox.setMargin(notificationIconNew, new Insets(11, 11, 0, -7));
			
			searchIcon.setText("c");
			searchIcon.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 18));
			searchIcon.setFill(Color.WHITE);
			searchIcon.setStyle("-fx-background-color: black;");
		
			searchText.setOnKeyPressed(new EventHandler<KeyEvent>()
			{
				@Override
				public void handle(KeyEvent ke)
				{
					if (ke.getCode().equals(KeyCode.ENTER) || ke.getCode().equals(KeyCode.TAB))
					{
						try {
							searchAction();
						} catch (IOException e) {
							Utils.getLogger().log(Level.SEVERE, "Search action error", e);
						}
					}
				}
			});

			initializeUI();
	
		} catch (IOException e) {
			Utils.getLogger().log(Level.SEVERE, "General Page init error", e);
		}
	}
	
	/**
	 * Called when notification is received
	 * @param change
	 */
	public static void onChanged(ListChangeListener.Change<? extends String> change) {

		// BitVaultConstants.notificationMenu.getItems().clear();
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				// Update UI here.
				updateNotificationUI();
			}
		});
	}
	
	/**
	 * Called when the mqtt client connection state changes.
	 * @param prop
	 * @param oldValue
	 * @param newValue
	 */
	public static void onNotificationConnectChanged(ObservableValue<? extends Boolean> prop,
			Boolean oldValue,
			Boolean newValue) {
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				updateConnectionUI(newValue);
			}
		});
	}
	
	/**
	 * Updates the connection UI
	 */
	public static void updateConnectionUI(Boolean newValue) {
		
		if(newValue)
			connectionIcon.setText("s");
		else
			connectionIcon.setText("t");		
	}

	/**
	 * Updates the notification UI
	 */
	public static void updateNotificationUI() {

		textNotificationVbox.getChildren().clear();

		for (int i = 0; i < GlobalCalls.list.size(); i++) {

			Text t = new Text(GlobalCalls.list.get(i));
			t.setStyle("-fx-cursor:hand;");
			t.setOnMouseClicked(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent event) {
					GlobalCalls.list.clear();
					notificationIconNew.setVisible(false);
					if (BitVaultConstants.mGeneralManagerCallbacks != null) {
						BitVaultConstants.mGeneralManagerCallbacks.inboxActionCallback();
					}
					popup.hide();
				}
			});
			textNotificationVbox.getChildren().add(t);
			VBox.setMargin(t, new Insets(10, 5, 0, 5));

			notificationIconNew.setVisible(true);
		}
	}

	/***
	 * This method is used to initialize the interface
	 */
	private void initializeUI() {
		
		// add change listeners
		GlobalCalls.list.addListener(GeneralPage::onChanged);
		GlobalCalls.isConnectedNotification.addListener(GeneralPage::onNotificationConnectChanged);

		BitVaultConstants.mGeneralManagerCallbacks = new GeneralManagerCallbacks() {

			@Override
			public void inboxActionCallback() {
				
				try {
					inboxAction();
				} catch (IOException e) {
					Utils.getLogger().log(Level.SEVERE, "Inbox Action error", e);
				}
			}

			@Override
			public void draftActionCallback() {
				
				try {
					draftAction();
				} catch (IOException e) {
					Utils.getLogger().log(Level.SEVERE, "Draft Action error", e);
				}
			}

			@Override
			public void composeActionCallback() {
	
			}
		};
	}
}
