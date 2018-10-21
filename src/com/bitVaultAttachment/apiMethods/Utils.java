package com.bitVaultAttachment.apiMethods;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.bitVaultAttachment.constant.BitVaultConstants;

public class Utils {

	private static Logger logger;
	private static FileHandler fh;
	private static Formatter simpleFormatter;
	private static int MAX_LOG_FILE_SIZE = 5000000; // 5 MB rotating
	private static int NO_OF_LOG_FILES = 1;
	private static Level LOG_LEVEL = Level.INFO;

	/**
	 * Initializes the application logger
	 */
	public static void initLogger(){

		logger = Logger.getLogger(Utils.class.getName());
		try {
			fh = new FileHandler( BitVaultConstants.PATH_FOR_LOG_FILES + "bitach_log.txt",  
					MAX_LOG_FILE_SIZE, NO_OF_LOG_FILES, true);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
		simpleFormatter = new SimpleFormatter();
		fh.setFormatter(simpleFormatter);
		logger.addHandler(fh);

		// set levels
		logger.setLevel(LOG_LEVEL);
		logger.info("logger init done");
	}

	public static Logger getLogger() {
		return logger;
	}

	/**
	 * Hashes the TXID
	 * 
	 * @param txId
	 * @return
	 */
	public static byte[] hashGenerate(String hexString) throws NoSuchAlgorithmException {

		MessageDigest digest = null;
		byte[] hash = null;

		try {
			digest = MessageDigest.getInstance("SHA-256");
			hash = digest.digest(hexString.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			Utils.getLogger().log(Level.SEVERE,"txid hashing error", e);
			throw e;
		}
		return hash;
	}
}
