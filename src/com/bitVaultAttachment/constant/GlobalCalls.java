package com.bitVaultAttachment.constant;

import java.util.ArrayList;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TextField;

public class GlobalCalls {
	
	public static ObservableList<String> list =  FXCollections.observableArrayList();
	
	public static BooleanProperty isConnectedNotification = new SimpleBooleanProperty(false);
	
	/**
	 * Text length limiter
	 * @param tf
	 * @param maxLength
	 */
	public static void addTextLengthLimiter(final TextField tf, final int maxLength) {
	    tf.textProperty().addListener(new ChangeListener<String>() {
	        @Override
	        public void changed(final ObservableValue<? extends String> ov, final String oldValue, final String newValue) {
	            if (tf!=null && tf.getText()!=null &&  tf.getText().length() > maxLength) {
	                String s = tf.getText().substring(0, maxLength);
	                tf.setText(s);
	            }
	        }
	    });
	}
	
	/**
	 * Checks for string null or empty
	 * @param strgToBeChecked
	 * @return
	 */
	public static boolean isNullOrEmptyStringCheck(String strgToBeChecked){
		boolean flag=true;
		if(strgToBeChecked!=null && !strgToBeChecked.trim().isEmpty())
			flag=false;
		return flag;
	}
	
	/**
	 * Checks DTO null or empty
	 * @param objectToBeChecked
	 * @return
	 */
	public static boolean isNullOrEmptyDTOCheck(Object objectToBeChecked){
		boolean flag=true;
		if(objectToBeChecked!=null )
			flag=false;
		return flag;
	}
	
	/**
	 * Checks list null or empty
	 * @param listToBeChecked
	 * @return
	 */
	public static boolean isNullOrEmptyListCheck(ArrayList<?> listToBeChecked){
		boolean flag=true;
		if(listToBeChecked!=null && !listToBeChecked.isEmpty())
			flag=false;
		return flag;
	}
	
	


}
