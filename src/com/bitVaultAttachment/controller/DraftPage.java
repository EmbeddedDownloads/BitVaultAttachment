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
import com.bitVaultAttachment.models.DraftListDTO;
import com.bitVaultAttachment.ui.NotificationBar;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class DraftPage implements Initializable {

	private VBox vboxDraft = new VBox(6);
	private ArrayList<NotificationBar> draftBar = new ArrayList<NotificationBar>();
	private int totalPages=1;
	@FXML
	private Stage stage;
	private String searchText;
	private int pageValue=0;
	private int initialPageValue=0;
	
	public ArrayList<DraftListDTO> list = new ArrayList<DraftListDTO>();
	public EventHandlerUI draftEvents = new EventHandlerUI();
	public HBox DraftHeader = new HBox();
	public Text textDraftHead = new Text();
	public HBox paginationHeader = new HBox();
	public VBox vboxInbox = new VBox(3);
	public VBox root = new VBox(vboxInbox);
	public EventHandler<MouseEvent> rightClickHandler = e -> handleRightAction(e);
	public EventHandler<MouseEvent> leftClickHandler = e -> handleLeftAction(e);
	public Text rightArrow = new Text();
	public Text leftArrow = new Text();
	
	/**
	 * @param location
	 * @param resources
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
	
	}
	
	/**
	 * Creates Connection with Database Fetches data from Database Creates Table
	 * and bind Action on table row
	 */
	private void fetchDataFromDatabase() {
		// Request on the basis of BitVault Id to database and fetch from table
		DbConnection dbConnection = new DbConnection();

		try {
			if (!GlobalCalls.isNullOrEmptyStringCheck(MainPage.bitVaultToken)) {
				
				list.clear();
				
				int	listSize=dbConnection.draftDataListSize(MainPage.bitVaultToken,
						searchText);
				totalPages=listSize%BitVaultConstants.NO_OF_REC<=0?listSize/BitVaultConstants.NO_OF_REC:((listSize/BitVaultConstants.NO_OF_REC)+1);	
				list = (ArrayList<DraftListDTO>) dbConnection.fetchData4Draft(MainPage.bitVaultToken, searchText,
						BitVaultConstants.NO_OF_REC, (pageValue*BitVaultConstants.NO_OF_REC));
				totalPages = totalPages == 0 ? 1 : totalPages;
				if(list.size()<=0)
					pageValue=initialPageValue;
			}
		} catch (ClassNotFoundException e) {
			Utils.getLogger().log(Level.SEVERE, "Error fetching from Database", e);
		} catch (SQLException e) {
			Utils.getLogger().log(Level.SEVERE, "Error fetching from Database", e);
		}
	}

	/**
	 * Draft action - sets draft page
	 * @return
	 */
	public VBox draftAction() {

		try {
			// set the stage;
			stage = MainPage.stage;
			// database fetch
			fetchDataFromDatabase();
			
			// Draft Text Heading
			Text textDraft = new Text("Drafts");
			textDraft.getStyleClass().add("headerLabel");
			textDraft.setTextAlignment(TextAlignment.LEFT);

			// hbox container for text
			HBox boxDraft = new HBox(textDraft);
			boxDraft.setPrefWidth(700);
			boxDraft.setMinHeight(60);
			boxDraft.setAlignment(Pos.CENTER_LEFT);

			// Number of drafts text
			textDraftHead.setText(" Attachments");
			textDraftHead.getStyleClass().addAll("inboxDownloadNewContainer");
			textDraftHead.setTextAlignment(TextAlignment.LEFT);
			
			// create the right arrow image text
			rightArrow.setText("r");
			rightArrow.setStyle(" -fx-cursor:hand;");
			rightArrow.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 14));
			HBox.setMargin(rightArrow, new Insets(5, 12, 5, 5));
			rightArrow.addEventHandler(MouseEvent.MOUSE_CLICKED, rightClickHandler);
			
			if(list.size()<=0 || (pageValue+1)==totalPages)
				rightArrow.setVisible(false);
			else
				rightArrow.setVisible(true);

			// create the right arrow image text
			leftArrow.setText("q");
			leftArrow.setStyle(" -fx-cursor:hand;");
			leftArrow.setFont(Font.loadFont(getClass().getResourceAsStream("/res/bitdesktopapp.ttf"), 14));
			if(pageValue==0)
				leftArrow.setVisible(false);
			else
				leftArrow.setVisible(true);
			HBox.setMargin(leftArrow, new Insets(5, 12, 5, 5));
			leftArrow.addEventHandler(MouseEvent.MOUSE_CLICKED, leftClickHandler);
			
			Text pageNo = new Text();
			pageNo.setText("Page "+(pageValue+1)+" of "+totalPages);
			pageNo.getStyleClass().addAll("inboxDownloadNewContainer");

			// hbox container for text
			paginationHeader.setPrefWidth(700);
			paginationHeader.setMinHeight(30);
			paginationHeader.setAlignment(Pos.CENTER_RIGHT);
			paginationHeader.getChildren().clear();
			paginationHeader.getChildren().addAll(pageNo,leftArrow,rightArrow);
			HBox.setHgrow(paginationHeader, Priority.ALWAYS);
			

			// hbox container for text
			DraftHeader.setPrefWidth(700);
			DraftHeader.setMinHeight(30);
			DraftHeader.setAlignment(Pos.CENTER_LEFT);
			DraftHeader.getChildren().clear();
			DraftHeader.getChildren().addAll(textDraftHead, paginationHeader);

			// vbox Drafts list container
			vboxDraft.setAlignment(Pos.CENTER);
			vboxDraft.setPrefWidth(650);

			// Add to notification bars to Drafts list
			if(draftBar.size()>0 && list.size()>0){
				draftBar.clear();
				vboxDraft.getChildren().clear();
			}

			for (int i = 0; i < list.size(); i++) {

				draftBar.add(new NotificationBar(this, list.get(i), stage));
				draftBar.get(draftBar.size() - 1).setDraft(draftEvents);
				vboxDraft.getChildren().add(draftBar.get(draftBar.size() - 1).getContainer());
			}

			// add to scroll pane
			ScrollPane sptop = new ScrollPane();
			sptop.getStyleClass().add("spbottom");
			sptop.setPrefWidth(700);
			sptop.setContent(vboxDraft);
			sptop.setFitToWidth(true);
			sptop.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
			sptop.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

			// -------------- set hierarchy --------------
			// add margins
			VBox.setMargin(boxDraft, new Insets(0, 10, 0, 10));
			VBox.setMargin(DraftHeader, new Insets(0, 10, 0, 10));
			VBox.setMargin(sptop, new Insets(0, 10, 0, 10));

			// add all the nodes to vbox container
			vboxInbox.getChildren().clear();
			vboxInbox.getChildren().addAll(boxDraft, DraftHeader, sptop);
			vboxInbox.setAlignment(Pos.CENTER);
			
			// add vbox to root
			root.setPrefSize(800, 600);
			root.getStyleClass().add("parentContainerClass");

			return root;

		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE, "Error loading Draft Page", e);
		}

		return null;
	}
	
	/**
	 * left navigation button handler
	 * @param e
	 * @return
	 */
	private Object handleLeftAction(MouseEvent e) {
		
		initialPageValue=pageValue;
		if(pageValue>0)
			pageValue--;
		draftAction();
		return null;
	}
	
	/**
	 * right navigation button handler
	 * @param e
	 * @return
	 */
	private Object handleRightAction(MouseEvent e) {
		initialPageValue=pageValue;
		pageValue++;
		draftAction();
		return null;
	}

	public VBox getDraftBox() {
		return vboxDraft;
	}

	public ArrayList<NotificationBar> getDraftBar() {
		return draftBar;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}
}
