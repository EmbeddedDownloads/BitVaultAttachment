package com.bitVaultAttachment.controller;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;

import com.bitVaultAttachment.apiMethods.Utils;
import com.bitVaultAttachment.constant.BitVaultConstants;
import com.bitVaultAttachment.constant.GlobalCalls;
import com.bitVaultAttachment.database.DbConnection;
import com.bitVaultAttachment.iclasses.InboxPageCallback;
import com.bitVaultAttachment.models.NotificationListDTO;
import com.bitVaultAttachment.ui.NotificationBar;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

/**
 * Created by vvdn on 5/29/2017.
 */
public class InboxPage implements Initializable {

	@FXML
	private TableView<NotificationListDTO> tableId;

	@FXML
	private Label senderAddress;

	@FXML
	private Button lockUnlockBtn;

	@FXML
	private ProgressIndicator progressIndicator;

	@FXML
	private Label label;

	@FXML
	private Hyperlink dwnloadLink;

	@FXML
	private Button stop;

	@FXML
	private VBox paneId;

	@FXML
	private AnchorPane mainPane;

	@FXML
	private Stage stage;

	@FXML
	private ComboBox<String> comboBox;

	private String searchText;
	private int pageValue = 0;
	private int totalPages = 1;
	private int initialPageValue = 0;
	private static String sortValue ="Date";
	private int pageValueDownloaded = 0;
	private int totalPagesDownloaded = 0;
	private int initialPageValueDownloaded = 0;
	private Text textSort;
	private HBox inboxRightCont;
	private int sortBy = BitVaultConstants.SORT_BY_DATE;
	
	public ArrayList<NotificationListDTO> list = new ArrayList<NotificationListDTO>();
	public ArrayList<NotificationListDTO> listOfDownloaded = new ArrayList<NotificationListDTO>();
	public ArrayList<NotificationBar> newNotifications = new ArrayList<NotificationBar>();
	public ArrayList<NotificationBar> downloadedList = new ArrayList<NotificationBar>();
	public EventHandlerUI inboxEvents = new EventHandlerUI();
	public VBox vboxNewAttachments = new VBox(6);
	public VBox vboxDownAttachments = new VBox(6);
	
	public EventHandler<MouseEvent> rightClickHandlerDownloaded = e -> handleRightActionDownloaded(e);
	public Text rightArrowDownloaded = new Text();
	public EventHandler<MouseEvent> leftClickHandlerDownloaded = e -> handleLeftActionDownloaded(e);
	public Text leftArrowDownloaded = new Text();
	public HBox paginationHeader = new HBox();
	public HBox paginationHeaderDownloaded = new HBox();
	public VBox vboxInbox = new VBox(10);
	public VBox root = new VBox(vboxInbox);
	public EventHandler<MouseEvent> rightClickHandler = e -> handleRightAction(e);
	public Text rightArrow = new Text();
	public EventHandler<MouseEvent> leftClickHandler = e -> handleLeftAction(e);
	public Text leftArrow = new Text();

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}

	/**
	 * Inbox Init method
	 * 
	 * @param location
	 * @param resources
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {

	}

	/**
	 * Fetch inbox data from DB
	 */
	private void fetchDataFromDatabase() {
		// Request on the basis of BitVault Id to database and fetch from table
		DbConnection dbConnection = new DbConnection();

		try {
			if (!GlobalCalls.isNullOrEmptyStringCheck(MainPage.bitVaultToken)) {

				list.clear();
				listOfDownloaded.clear();
				int listSize = dbConnection.inboxDataListSize(MainPage.bitVaultToken, searchText,
						BitVaultConstants.FALSE);
				int listSizeDownloaded = dbConnection.inboxDataListSize(MainPage.bitVaultToken, searchText,
						BitVaultConstants.TRUE);
				totalPages = listSize % BitVaultConstants.NO_OF_REC <= 0 ? listSize / BitVaultConstants.NO_OF_REC
						: ((listSize / BitVaultConstants.NO_OF_REC) + 1);
				totalPagesDownloaded = listSizeDownloaded % BitVaultConstants.NO_OF_REC <= 0
						? listSizeDownloaded / BitVaultConstants.NO_OF_REC
								: ((listSizeDownloaded / BitVaultConstants.NO_OF_REC) + 1);
				list = (ArrayList<NotificationListDTO>) dbConnection.fetchData4Inbox(MainPage.bitVaultToken, searchText,
						BitVaultConstants.FALSE, sortBy, BitVaultConstants.NO_OF_REC,
						(pageValue * BitVaultConstants.NO_OF_REC));
				listOfDownloaded = (ArrayList<NotificationListDTO>) dbConnection.fetchData4Inbox(MainPage.bitVaultToken,
						searchText, BitVaultConstants.TRUE, sortBy, BitVaultConstants.NO_OF_REC,
						(pageValueDownloaded * BitVaultConstants.NO_OF_REC));

				totalPages = totalPages == 0 ? 1 : totalPages;

				totalPagesDownloaded = totalPagesDownloaded == 0 ? 1 : totalPagesDownloaded;
				if (list.size() <= 0)
					pageValue = initialPageValue;

				if (listOfDownloaded.size() <= 0)
					pageValueDownloaded = initialPageValueDownloaded;

			}

		} catch (ClassNotFoundException e) {
			Utils.getLogger().log(Level.SEVERE, "Init Inbox Page error", e);
		} catch (SQLException e) {
			Utils.getLogger().log(Level.SEVERE, "Init Inbox Page error", e);
		}
	}
	
	/**
	 * Load Inbox Page
	 * @return
	 */
	public VBox inBoxAction() {
		
		try {
			// set the stage;
			stage = MainPage.stage;

			// number of attachments
			int newAttachments = 0;
			// fetch from DB
			fetchDataFromDatabase();
			
			// create new combo-box for sorting
			comboBox = new ComboBox<>(FXCollections.observableArrayList("Public Address", "Size", "Date"));
			comboBox.setPromptText("Select");

			comboBox.setOnAction((event) -> {
				handleSortClick();
				event.consume();
			});
			comboBox.setValue(sortValue);

			// create the right arrow image text
			rightArrow.setText("r");
			rightArrow.setStyle(" -fx-cursor:hand;");
			rightArrow.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 14));

			HBox.setMargin(rightArrow, new Insets(5, 12, 5, 5));
			rightArrow.addEventHandler(MouseEvent.MOUSE_CLICKED, rightClickHandler);

			if (list.size() <= 0 || (pageValue + 1) == totalPages)
				rightArrow.setVisible(false);
			else
				rightArrow.setVisible(true);

			// create the right arrow image text
			leftArrow.setText("q");
			leftArrow.setStyle(" -fx-cursor:hand;");
			leftArrow.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 14));
			if (pageValue == 0)
				leftArrow.setVisible(false);
			else
				leftArrow.setVisible(true);
			// leftArrow.setFill(Color.GREY);
			HBox.setMargin(leftArrow, new Insets(5, 12, 5, 5));
			leftArrow.addEventHandler(MouseEvent.MOUSE_CLICKED, leftClickHandler);

			Text pageNo = new Text();
			pageNo.setText("Page " + (pageValue + 1) + " of " + totalPages);
			pageNo.getStyleClass().addAll("inboxDownloadNewContainer");

			// hbox container for text
			paginationHeader.setPrefWidth(700);
			paginationHeader.setMinHeight(30);
			paginationHeader.setAlignment(Pos.CENTER_RIGHT);
			paginationHeader.getChildren().clear();
			paginationHeader.getChildren().addAll(pageNo, leftArrow, rightArrow);
			HBox.setHgrow(paginationHeader, Priority.ALWAYS);

			// create the right arrow image text
			rightArrowDownloaded.setText("r");
			rightArrowDownloaded.setStyle(" -fx-cursor:hand;");
			rightArrowDownloaded.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 14));

			HBox.setMargin(rightArrowDownloaded, new Insets(5, 12, 5, 5));
			rightArrowDownloaded.addEventHandler(MouseEvent.MOUSE_CLICKED, rightClickHandlerDownloaded);

			if (listOfDownloaded.size() <= 0 || (pageValueDownloaded + 1) == totalPagesDownloaded)
				rightArrowDownloaded.setVisible(false);
			else
				rightArrowDownloaded.setVisible(true);

			// create the right arrow image text
			leftArrowDownloaded.setText("q");
			leftArrowDownloaded.setStyle(" -fx-cursor:hand;");
			leftArrowDownloaded.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 14));
			if (pageValueDownloaded == 0)
				leftArrowDownloaded.setVisible(false);
			else
				leftArrowDownloaded.setVisible(true);

			HBox.setMargin(leftArrowDownloaded, new Insets(5, 12, 5, 5));
			leftArrowDownloaded.addEventHandler(MouseEvent.MOUSE_CLICKED, leftClickHandlerDownloaded);

			Text pageNoDownloaded = new Text();
			pageNoDownloaded.setText("Page " + (pageValueDownloaded + 1) + " of " + totalPagesDownloaded);
			pageNoDownloaded.getStyleClass().addAll("inboxDownloadNewContainer");

			// hbox container for text
			paginationHeaderDownloaded.setPrefWidth(700);
			paginationHeaderDownloaded.setMinHeight(30);
			paginationHeaderDownloaded.setAlignment(Pos.CENTER_RIGHT);
			paginationHeaderDownloaded.getChildren().clear();
			paginationHeaderDownloaded.getChildren().addAll(pageNoDownloaded, leftArrowDownloaded,
					rightArrowDownloaded);
			HBox.setHgrow(paginationHeaderDownloaded, Priority.ALWAYS);

			// -------- Inbox Text ------------
			// Add inbox text
			Text textInbox = new Text("Inbox");
			textInbox.setTextAlignment(TextAlignment.LEFT);
			textInbox.getStyleClass().add("headerLabel");

			// -------- Sorting ----------------
			Text textFromOthers = new Text("Attachment(s) from others");
			textFromOthers.setStyle(" -fx-cursor:hand;");
			textFromOthers.getStyleClass().addAll("textFromOthers", "inboxDownloadNewContainer");

			// add arrow down text image
			Text arrowDownImage = new Text();
			arrowDownImage.setText("m");
			arrowDownImage.setStyle(" -fx-cursor:hand;");
			arrowDownImage.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 14));

			// add text for sort option
			textSort = new Text("Sort By :");
			textSort.getStyleClass().addAll("textFromOthers", "inboxDownloadNewContainer");
			HBox.setMargin(textSort, new Insets(0, 0, 0, 50));

			// context menu for displaying sorting options
			MenuItem cmItem1 = new MenuItem();
			MenuItem cmItem2 = new MenuItem();
			MenuItem cmItem3 = new MenuItem();
			Label lbl1 = new Label("Public Address");
			Label lbl2 = new Label("Size");
			Label lbl3 = new Label("Date");
			lbl1.setPrefWidth(130);
			lbl2.setPrefWidth(130);
			lbl3.setPrefWidth(130);
			cmItem1.setGraphic(lbl1);
			cmItem2.setGraphic(lbl2);
			cmItem3.setGraphic(lbl3);

			EventHandler<ActionEvent> addressSortClickHandler = e -> handleAddressSortClick(e);
			cmItem1.addEventHandler(ActionEvent.ACTION, addressSortClickHandler);

			EventHandler<ActionEvent> dateSortClickHandler = e -> handledateSortClick(e);
			cmItem3.addEventHandler(ActionEvent.ACTION, dateSortClickHandler);

			EventHandler<ActionEvent> sizeSortClickHandler = e -> handlesizeSortClick(e);
			cmItem2.addEventHandler(ActionEvent.ACTION, sizeSortClickHandler);

			// add items to menu
			ContextMenu sortingMenu = new ContextMenu();
			sortingMenu.getItems().addAll(cmItem1, cmItem2, cmItem3);

			// add sort image
			Text textSortImage = new Text();
			textSortImage.setText("l");
			textSortImage.setStyle(" -fx-cursor:hand;");
			textSortImage.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 14));

			// The right side container for all the nodes
			inboxRightCont = new HBox(textSort, comboBox);
			HBox.setHgrow(inboxRightCont, Priority.ALWAYS);
			inboxRightCont.setAlignment(Pos.CENTER_RIGHT);
			HBox.setMargin(textSortImage, new Insets(0, 0, 3, 5));
			HBox.setMargin(arrowDownImage, new Insets(0, 0, 3, 3));
			HBox.setMargin(comboBox, new Insets(0, 0, 0, 10));

			// hbox container for the inbox text and right side container
			HBox boxInbox = new HBox(textInbox, inboxRightCont);
			boxInbox.setMinHeight(40);
			boxInbox.setAlignment(Pos.BOTTOM_LEFT);

			// New Attachment list container
			vboxNewAttachments.setAlignment(Pos.CENTER);

			// Add notification bars to New attachment list
			if (newNotifications.size() > 0 && list.size() > 0) {
				newNotifications.clear();
				vboxNewAttachments.getChildren().clear();
			}

			for (int i = 0; i < list.size(); i++) {
				newAttachments++;
				newNotifications.add(new NotificationBar(this, list.get(i), stage));
				vboxNewAttachments.getChildren().add(newNotifications.get(newNotifications.size() - 1).getContainer());
			}

			// Add notification bars to the Downloaded list
			if (downloadedList.size() > 0 && listOfDownloaded.size() > 0) {
				downloadedList.clear();
				vboxDownAttachments.getChildren().clear();
			}

			for (int i = 0; i < listOfDownloaded.size(); i++) {
				downloadedList.add(new NotificationBar(this, listOfDownloaded.get(i), stage));
				downloadedList.get(downloadedList.size() - 1).setDownloaded(this, inboxEvents, stage);
				if (listOfDownloaded.get(i).getIsLocked() == BitVaultConstants.TRUE) {
					downloadedList.get(downloadedList.size() - 1).setUnlocked(inboxEvents);
				}
				vboxDownAttachments.getChildren().add(downloadedList.get(downloadedList.size() - 1).getContainer());
			}

			// ------ New Attachments -----------
			// header
			Text textNewAttHead = new Text();
			textNewAttHead.setText(newAttachments + " New BitVault Attachment");
			textNewAttHead.getStyleClass().add("inboxDownloadNewContainer");
			textNewAttHead.setTextAlignment(TextAlignment.LEFT);

			HBox hboxNewAttachmentHead = new HBox(textNewAttHead, paginationHeader);
			hboxNewAttachmentHead.setMinHeight(20);
			hboxNewAttachmentHead.setAlignment(Pos.CENTER_LEFT);
			hboxNewAttachmentHead.getStyleClass().add("newAttHeader");

			// add to scroll pane
			ScrollPane sptop = new ScrollPane();
			sptop.getStyleClass().add("spbottom");
			sptop.setMinHeight(110);
			sptop.setMaxHeight(110);
			sptop.setContent(vboxNewAttachments);
			sptop.setFitToWidth(true);
			sptop.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
			sptop.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

			// ------------- Downloaded attachments --------
			// add download attachment header
			Text textDownloadAttachmentHead = new Text();
			textDownloadAttachmentHead.setText("Downloaded BitVault Attachment(s)");
			textDownloadAttachmentHead.getStyleClass().add("inboxDownloadNewContainer");
			textDownloadAttachmentHead.setTextAlignment(TextAlignment.LEFT);

			// container for the download attachment text
			HBox hboxDownloadAttachmentHead = new HBox(textDownloadAttachmentHead, paginationHeaderDownloaded);
			hboxDownloadAttachmentHead.getStyleClass().add("newAttHeader");
			hboxDownloadAttachmentHead.setMinHeight(20);
			hboxDownloadAttachmentHead.setAlignment(Pos.CENTER_LEFT);

			// Downloaded List container
			vboxDownAttachments.setAlignment(Pos.CENTER);

			// define the download attachments scroll-pane
			ScrollPane spbottom = new ScrollPane();
			spbottom.getStyleClass().add("spbottom");
			spbottom.setContent(vboxDownAttachments);
			spbottom.setFitToWidth(true);
			spbottom.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
			spbottom.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

			// add event handlers for each option
			inboxEvents.setOthersClickEvent(textFromOthers, arrowDownImage, this);
			inboxEvents.setSortClickEvent(textSort, textSortImage, stage, sortingMenu);

			// -------------- set hierarchy --------------
			// set margins
			VBox.setMargin(boxInbox, new Insets(8, 10, 0, 10));
			VBox.setMargin(hboxNewAttachmentHead, new Insets(0, 10, 0, 10));
			VBox.setMargin(sptop, new Insets(0, 10, 0, 10));
			VBox.setMargin(hboxDownloadAttachmentHead, new Insets(10, 10, 0, 10));
			VBox.setMargin(spbottom, new Insets(0, 10, 0, 10));

			// add all the elements to the vbox container

			vboxInbox.getChildren().clear();
			vboxInbox.getChildren().addAll(boxInbox, hboxNewAttachmentHead, sptop, hboxDownloadAttachmentHead,
					spbottom);
			vboxInbox.setAlignment(Pos.CENTER);
			vboxInbox.setPrefWidth(700);
			VBox.setVgrow(spbottom, Priority.ALWAYS);

			// set the container with respect to the root vbox
			root.setPrefSize(800, 600);
			root.getStyleClass().add("parentContainerClass");

			// show attachment callback implement
			BitVaultConstants.mInboxPageCallbacks = new InboxPageCallback() {

				@Override
				public void showAttachmentCallback(String hashOfTxid) {
					for(int i = 0; i<downloadedList.size(); i++){
						if( downloadedList.get(i).getHashOfTxnId().equalsIgnoreCase(hashOfTxid)){
							downloadedList.get(i).getAttachmentList(downloadedList.get(i).getBitVaultToken(), downloadedList.get(i).getHashOfTxnId());
							downloadedList.get(i).getContainer().getChildren().add(downloadedList.get(i).getAttachmentContainer());
							downloadedList.get(i).setShowAttachmentState(true);
						}
					}

				}
			};
			return root;

		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE, "Loading Inbox Page error" + e.toString() , e);
		}
		return null;
	}

	protected void handleSortClick() {

		sortValue = (String) comboBox.getValue();

		switch (sortValue) {
		case "Public Address":
			sortBy = BitVaultConstants.SORT_BY_ADDRESS;
			break;
		case "Size":
			sortBy = BitVaultConstants.SORT_BY_SIZE;
			break;
		case "Date":
			sortBy = BitVaultConstants.SORT_BY_DATE;
			break;
		default:
			sortBy = BitVaultConstants.SORT_BY_DATE;
			break;
		}

		pageValue = 0;
		inBoxAction();

	}

	private void handlesizeSortClick(ActionEvent e) {
		sortBy = BitVaultConstants.SORT_BY_SIZE;
		pageValue = 0;
		inBoxAction();
	}

	private void handledateSortClick(ActionEvent e) {
		sortBy = BitVaultConstants.SORT_BY_DATE;
		pageValue = 0;
		inBoxAction();
	}

	private void handleAddressSortClick(ActionEvent e) {
		sortBy = BitVaultConstants.SORT_BY_ADDRESS;
		pageValue = 0;
		inBoxAction();
	}

	private Object handleLeftAction(MouseEvent e) {

		initialPageValue = pageValue;
		if (pageValue > 0)
			pageValue--;
		inBoxAction();
		return null;
	}

	private Object handleRightAction(MouseEvent e) {
	
		initialPageValue = pageValue;
		pageValue++;
		inBoxAction();
		return null;
	}

	private Object handleLeftActionDownloaded(MouseEvent e) {
		
		initialPageValueDownloaded = pageValueDownloaded;
		if (pageValueDownloaded > 0)
			pageValueDownloaded--;
		inBoxAction();
		return null;
	}

	private Object handleRightActionDownloaded(MouseEvent e) {
		
		initialPageValueDownloaded = pageValueDownloaded;
		pageValueDownloaded++;
		inBoxAction();
		return null;
	}

}
