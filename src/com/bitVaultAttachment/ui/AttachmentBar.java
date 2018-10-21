package com.bitVaultAttachment.ui;

import java.text.DecimalFormat;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;

public class AttachmentBar {
	
	private Text textAttachmentName, textAttachmentSize;
	private String nameAttachment = "";
	private HBox bar;
	private long sizeAttachment = 0;
	
	public AttachmentBar(String attachName,int attachId,long size) {
		this.nameAttachment =attachName.trim();
		this.sizeAttachment=size;
		this.createBar();
	}
	
	public void createBar(){
		
		// primary hbox for attachment bar
    	bar = new HBox(5);
    	bar.setMinHeight(30);
    	bar.setAlignment(Pos.CENTER_LEFT);
    	
    	// Attachment Name
    	textAttachmentName = new Text();
    	textAttachmentName.setText(nameAttachment.trim());
    	textAttachmentName.setId("textAttachmentName");
    	
    	HBox hboxAttachmentName = new HBox(textAttachmentName);
    	hboxAttachmentName.setAlignment(Pos.CENTER_LEFT);
    	HBox.setMargin(textAttachmentName, new Insets(0, 0, 0, 10) );
    	HBox.setHgrow(hboxAttachmentName, Priority.ALWAYS);
    	
    	// Attachment Size
		textAttachmentSize = new Text(getSizeToDisplay(sizeAttachment));
    	textAttachmentSize.setId("textAttachmentSize");
    	
    	HBox hboxAttachmentSize = new HBox(textAttachmentSize);
    	hboxAttachmentSize.setAlignment(Pos.CENTER_RIGHT);
    	HBox.setMargin(textAttachmentSize, new Insets(0, 5, 0, 0) );
    	HBox.setHgrow(hboxAttachmentSize, Priority.ALWAYS);
    	
    	// add to main bar box
    	bar.getChildren().addAll(hboxAttachmentName, hboxAttachmentSize);
    	HBox.setHgrow(bar, Priority.ALWAYS);
	}
	
	/**
	 * returns size to display as text
	 * @param sizeAttachment
	 * @return
	 */
	public String getSizeToDisplay (double sizeAttachment){
		
		Double sizeInKb=(double) (sizeAttachment / 1024);
		return sizeInKb>1024?(new DecimalFormat("##.##").format(sizeInKb/1024)+" MB"):
			(new DecimalFormat("##.##").format(sizeInKb).equalsIgnoreCase("0")?"0.01 KB":
				new DecimalFormat("##.##").format(sizeInKb) + " KB");
	}
	
	/**
	 * return container bar
	 */
	public HBox getBar(){
		return bar;
	}
	
}
