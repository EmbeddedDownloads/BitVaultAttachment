package com.bitVaultAttachment.constant;

import java.io.File;
import java.util.Locale;
import java.util.logging.Level;

import com.bitVaultAttachment.apiMethods.Utils;
import com.bitVaultAttachment.database.DbConnection;
import com.bitVaultAttachment.iclasses.GeneralManagerCallbacks;
import com.bitVaultAttachment.iclasses.InboxPageCallback;

public class BitVaultConstants {

	public static Integer CAM_FOR_REGISTRATION = 1;

	public static Integer CAM_FOR_SCAN_TXN_ID = 2;

	public static Integer CAM_FOR_UNLOCK = 3;

	public static Integer IS_FOR_INBOX = 1;

	public static Integer IS_FOR_DRAFT = 2;

	public static Integer IS_FOR_COMPOSE = 3;

	public static  int SORT_BY_ADDRESS = 1;

	public static Integer SORT_BY_DATE = 2;

	public static Integer SORT_BY_SIZE = 3;

	public static short TRUE = 1;

	public static short FALSE = 0;

	public static String PATH_FOR_ENCRYPTED_FILES = actualPath(checkOs(),"EncryptedFiles")+File.separator;

	public static String PATH_FOR_DOWNLOADED_FILES = actualPath(checkOs(),"DownloadedFiles")+File.separator;

	public static String PATH_FOR_MQTT_FILES = actualPath(checkOs(),"MQTT")+File.separator;

	public static String PATH_FOR_DATABASE = actualPath(checkOs(),"Database");
	
	public static String PATH_FOR_LOG_FILES = actualPath(checkOs(),"Logs")+File.separator;

	public static int OFFSET=0;

	public static int NO_OF_REC=10;

	public static GeneralManagerCallbacks mGeneralManagerCallbacks = null;

	public static InboxPageCallback mInboxPageCallbacks = null;

	public enum OSType {
		Windows, MacOS, Linux, Other
	};

	protected static OSType detectedOS;

	/**
	 * Check OS Path
	 * @return
	 */
	public static String checkOs() {

		String path=System.getProperty("user.home");
		switch (getOperatingSystemType()) {
		case Windows:
			path=path+File.separator+"AppData"+File.separator+"Local"+File.separator;
			break;
		case MacOS:
			path=path+File.separator+"Library"+File.separator+"Application Support"+File.separator;
			break;
		case Linux:
			path=path+File.separator+"opt"+File.separator;
			break;

		default:
			path=path+"";
			break;
		}
		path=path+File.separator+"Bitach";
		File file = new File(path);
		if (!file.exists()) {
			if (file.mkdirs()) {
				System.out.println("Multiple directories are created!");
			} else {
				System.out.println("Failed to create multiple directories!");
			}
		}
		return path;
	}

	/**
	 * Create the DB scheme
	 * @param actualPath
	 * @param registration_token
	 */
	public static void createDatabaseScheme(String actualPath,String registration_token) {

		DbConnection connection=new DbConnection();
		connection.createScheme(registration_token);		
	}
	
	/**
	 * Get path for folder
	 * @param path
	 * @param destinationFolder
	 * @return
	 */
	private static String actualPath(String path,String destinationFolder) {

		path=path+File.separator+destinationFolder;
		File file = new File(path);
		if (!file.exists()) {
			if (file.mkdirs()) {
				Utils.getLogger().log(Level.SEVERE,"Multiple directories are created!");
			} else {
				Utils.getLogger().log(Level.SEVERE,"Failed to create multiple directories!");
			}
		}
		return path;
	}
	
	/**
	 * Get OS Type
	 * @return
	 */
	public static OSType getOperatingSystemType() {
		if (detectedOS == null) {
			String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
			if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
				detectedOS = OSType.MacOS;
			} else if (OS.indexOf("win") >= 0) {
				detectedOS = OSType.Windows;
			} else if (OS.indexOf("nux") >= 0) {
				detectedOS = OSType.Linux;
			} else {
				detectedOS = OSType.Other;
			}
		}
		return detectedOS;
	}
}
