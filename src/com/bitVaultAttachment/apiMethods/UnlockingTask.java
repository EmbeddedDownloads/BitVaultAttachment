package com.bitVaultAttachment.apiMethods;

import java.awt.Component;
import java.awt.HeadlessException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.Key;
import java.security.MessageDigest;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JDialog;
import javax.swing.JFileChooser;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import com.bitVaultAttachment.constant.BitVaultConstants;
import com.bitVaultAttachment.constant.GlobalCalls;
import com.bitVaultAttachment.database.DbConnection;
import com.bitVaultAttachment.models.AttachmentDTO;

import javafx.application.Platform;
import javafx.concurrent.Task;

public class UnlockingTask extends Task<Void> {

	/**
	 * Private Parameters
	 */
	final private String SYMALGORITHM = "AES/CBC/PKCS5Padding";

	// IV - 16 bytes static
	final byte[] ivBytes = { (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32, (byte) 0x56, (byte) 0x35, (byte) 0xE3,
			(byte) 0x03, (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32, (byte) 0x56, (byte) 0x35, (byte) 0xE3,
			(byte) 0x03 };

	private AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
	private final int ENC_BUFFER_LEN = 2097152; // 1048576 = 1 MB ; 2097152 = 2 MB
	private final int DEC_BUFFER_LEN = ENC_BUFFER_LEN + 32;
	private final String append = "_enc";
	private final int ZIP_BUFFER = 2048;
	private File selectedOutputPath;

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * Public Parameters
	 */
	public String bitVaultToken,txid, skey,hashOfTxnId;
	public File file;

	/**
	 * Constructor for sending task
	 * 
	 * @param txid
	 * @param skey
	 * @param attachmentList
	 */
	public UnlockingTask(String bitVaultToken,String txid, String skey,String hashOfTxnId, File file) {
		this.txid = txid;
		this.skey = skey;
		this.file = file;
		this.hashOfTxnId=hashOfTxnId;
		this.bitVaultToken=bitVaultToken;
	}

	/**
	 * Implement the unlocking task thread
	 */
	@Override
	protected Void call() throws Exception {

		// Update message
		Utils.getLogger().log(Level.INFO, "Unlocking Attachments...");
		this.updateMessage("Unlocking attachment(s) ");
		this.updateProgress(0, 1);

		// Decrypt the compressed file
		String fileHash = null;
		try {
			if (!isCancelled()) {
				fileHash = decryptFile(txid, skey, file);
				Utils.getLogger().log(Level.FINEST,"SHA256: " + fileHash);
			}
		} catch (Exception e) {
			throw e;
		}

		// Open save folder location
		try {
			AlwaysOnTopFileChooser chooser = new AlwaysOnTopFileChooser();
			chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.showSaveDialog(null);
			
			String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
			if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
				selectedOutputPath = chooser.getCurrentDirectory();
			}else{
				selectedOutputPath = chooser.getSelectedFile();
			}
			Utils.getLogger().log(Level.FINEST, "selected path :" + selectedOutputPath.getPath());

		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE, "error selecting folder", e);
			throw e;
		}

		// Open compressed file
		File compressedFile = null;
		try {
			if (!isCancelled()) {
				compressedFile = new File(BitVaultConstants.PATH_FOR_DOWNLOADED_FILES + File.separator + Hex.toHexString(Base64.decode(hashOfTxnId)) + ".zip");
			}
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"Zipped file read error", e);
			throw e;
		}

		// Update message
		this.updateMessage("Decompressing attachment(s)");
		this.updateProgress(0, 1);
		ArrayList<AttachmentDTO> listOfAttchmntDto=null;

		// UnZip the files
		try {
			if (!isCancelled()) {
				listOfAttchmntDto=UnZipFiles(compressedFile);
			}
		} catch (Exception e) {
			throw e;
		}
		
		// Update DB and set downloaded
		DbConnection connection = new DbConnection();
		try {
			if(!GlobalCalls.isNullOrEmptyStringCheck(bitVaultToken) && 
					!GlobalCalls.isNullOrEmptyStringCheck(hashOfTxnId)){
				connection.updateNotificationDtaOnUnlocking(bitVaultToken, hashOfTxnId,selectedOutputPath.getPath(),listOfAttchmntDto);
				
				Platform.runLater(new Runnable() {
		            @Override public void run() {
		            	if (BitVaultConstants.mGeneralManagerCallbacks != null) {
							BitVaultConstants.mGeneralManagerCallbacks.inboxActionCallback();
						} 
		            }
		        });
			}
		} catch (ClassNotFoundException | SQLException e1) {
			Utils.getLogger().log(Level.SEVERE,"database update error", e1);
			throw e1;
		}

		// delete compressed file
		try {
			if( compressedFile.exists() ){
				compressedFile.delete();
				Utils.getLogger().log(Level.INFO,"deleted compressed file");
			}
			else{
				throw new Exception("error when deleting compressed file");
			}
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"Zipped file delete error", e);
		}

		// delete encrypted file
		try {

			if(file.exists()){
				file.delete();
				Utils.getLogger().log(Level.INFO,"deleted encrypted file");
			}
			else{
				throw new Exception("encrypted file not found while deleting");
			}
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"encrypted file delete error", e);
		}
		
		return null;
	}

	/**
	 * Decrypt file and return its SHA256
	 * 
	 * @param txid
	 * @param skey
	 * @param f
	 * @return
	 * @throws Exception
	 */
	public String decryptFile(String txid, String skey, File f) throws Exception {

		// check if file in correct format
		if (!f.getName().endsWith("_enc")) {
			Utils.getLogger().log(Level.SEVERE,"File not in correct format: " + f.getName());
			throw new Exception();
		}

		/* ------------------- Initialize the Keys --------------------- */
		// Get TXID Symmetric Key
		SecretKey txidKey = null;
		try {
			txidKey = getAESKey(txid);
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"TXID key encoding error", e);
			throw e;
		}

		// Get Session key
		Key sessionKey = null;
		try {
			sessionKey = getAESKey(skey);
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"Session Key encoding error", e);
			throw e;
		}

		Utils.getLogger().log(Level.FINEST,"File Name: " + f.getAbsolutePath() + " " + f.getName() + "\n");

		/* ------------------- Decrypt the file -------------------------- */

		long startTime = System.currentTimeMillis();

		// Init txid key cipher object
		Cipher dcipher1 = null;
		try {
			dcipher1 = Cipher.getInstance(SYMALGORITHM, "BC");
			dcipher1.init(Cipher.DECRYPT_MODE, txidKey, ivSpec);
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"txid cipher init error", e);
			throw e;
		}

		// init session key cipher object
		Cipher dcipher2 = null;
		try {
			dcipher2 = Cipher.getInstance(SYMALGORITHM, "BC");
			dcipher2.init(Cipher.DECRYPT_MODE, sessionKey, ivSpec);
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"session cipher init error", e);
			throw e;
		}

		// input stream for file being read and output stream for writing
		// encrypted file.
		byte[] buffer = new byte[DEC_BUFFER_LEN];
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		InputStream is = new FileInputStream(f);
		BufferedInputStream inStream = new BufferedInputStream(is);
		DigestInputStream digest = new DigestInputStream(inStream, md);

		int lastIndex = f.getName().length() - append.length();
		String outFileName = f.getName().substring(0, lastIndex);
		Utils.getLogger().log(Level.FINEST,"outFileName: " + outFileName);
		OutputStream os = new FileOutputStream(
				BitVaultConstants.PATH_FOR_DOWNLOADED_FILES + File.separator + outFileName);

		Utils.getLogger().log(Level.INFO,"Decrypting the File..." + "\n");

		long workToBeDone = (f.length() / DEC_BUFFER_LEN) + 1;
		long workDone = 0;

		// initial read to buffer
		int bytesRead = 0;
		try {
			bytesRead = digest.read(buffer);
		} catch (IOException e) {
			digest.close();
			os.flush();
			os.close();
			Utils.getLogger().log(Level.SEVERE,"Buffer IO error", e);
			throw e;
		}

		// Reads buffer from file and decrypts
		while (bytesRead != -1) {

			byte[][] bWrite = null;
			int len = 0;

			// decrypt the buffer
			try {
				bWrite = decryptBuffer(buffer, bytesRead, dcipher1, dcipher2);
				len = (new BigInteger(bWrite[1]).intValue());

			} catch (Exception e) {
				digest.close();
				os.flush();
				os.close();
				Utils.getLogger().log(Level.SEVERE,"Buffer decrypt error", e);
				throw e;
			}

			try {
				// write to output file
				os.write(bWrite[0], 0, len);

				// update UI
				this.updateProgress(++workDone, workToBeDone);

				// read again from buffer
				bytesRead = digest.read(buffer);

			} catch (Exception e) {
				digest.close();
				os.flush();
				os.close();
				Utils.getLogger().log(Level.SEVERE,"Buffer IO error", e);
				throw e;
			}

			// check if task is cancelled
			if (isCancelled()) {
				break;
			}
		}
		digest.close();
		os.flush();
		os.close();
		Utils.getLogger().log(Level.INFO,"File Decrypted: ");

		long stopTime = System.currentTimeMillis();
		Utils.getLogger().log(Level.FINEST,"Time to Decrypt: " + ((stopTime - startTime) / 1000.0) + "");

		// return SHA256 of input encrypted file
		return Hex.toHexString(md.digest());
	}

	/**
	 * UnZips the attachments
	 * 
	 * @param f
	 * @throws Exception
	 */
	public ArrayList<AttachmentDTO> UnZipFiles(File f) throws Exception {

		// ----------------------- DECOMPRESS ---------------------
		long startTime = System.currentTimeMillis();
		ArrayList<AttachmentDTO> listOfAttchmntDto=new ArrayList<>();

		try {
			long workDone = 0;
			long workToBeDone = f.length();

			// open file to unzip
			BufferedOutputStream dest = null;
			FileInputStream fis = new FileInputStream(f);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
			ZipEntry entry;
			
			AttachmentDTO attachmentDTO;
			int attchId=1;

			// Extract each entry
			while ((entry = zis.getNextEntry()) != null) {

				Utils.getLogger().log(Level.FINEST,"Extracting: " + entry);

				int count;
				byte data[] = new byte[ZIP_BUFFER];

				// write the files to the disk
				File fout = new File(selectedOutputPath.getPath() + File.separator + entry.getName());
				FileOutputStream fos = new FileOutputStream(fout);
				dest = new BufferedOutputStream(fos, ZIP_BUFFER);

				while ((count = zis.read(data, 0, ZIP_BUFFER)) != -1) {
					// write data
					dest.write(data, 0, count);
					// update progress
					workDone += count;
					this.updateProgress(workDone, workToBeDone);
					// check if task is cancelled
					if (isCancelled()) {
						break;
					}
				}
				dest.flush();
				dest.close();
				
				// set the attachment DTO
				attachmentDTO=new AttachmentDTO();
				attachmentDTO.setAttachName(entry.getName());
				attachmentDTO.setAttachId(attchId++);
				attachmentDTO.setAttachPath(selectedOutputPath.getPath());
				attachmentDTO.setHashTxId(hashOfTxnId);
				attachmentDTO.setSize(entry.getSize());
				listOfAttchmntDto.add(attachmentDTO);

				if (isCancelled()) {
					break;
				}
			}
			zis.close();
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"Unzip error", e);
			throw e;
		}

		long stopTime = System.currentTimeMillis();
		Utils.getLogger().log(Level.FINEST,"Time to UnZip: " + ((stopTime - startTime) / 1000.0) + "");
		return listOfAttchmntDto;

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
	 * Decrypts the Buffer with TXID and Session Key
	 * 
	 * @param input
	 * @param length
	 * @param txidCipher
	 * @param skeyCipher
	 * @return returns decrypted data and size in mcipher[0] and mcipher[1]
	 * @throws Exception
	 */
	public byte[][] decryptBuffer(byte[] input, int length, Cipher txidCipher, Cipher skeyCipher) throws Exception {

		byte[][] mplain = new byte[2][];

		try {
			mplain = symDecryption(input, length, skeyCipher);
			mplain = symDecryption(mplain[0], new BigInteger(mplain[1]).intValue(), txidCipher);
			mplain[0] = Arrays.copyOfRange(mplain[0], 0, new BigInteger(mplain[1]).intValue());

		} catch (Exception e) {
			throw e;
		}

		return mplain;
	}

	/**
	 * finds CRC32 of text
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public String getCRC(String text) throws Exception {

		try {
			byte[] checksumbyte = Strings.toByteArray(text);
			Checksum checksum = new CRC32();
			checksum.update(checksumbyte, 0, checksumbyte.length);
			return Long.toHexString(checksum.getValue());
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE,"chechksum error", e);
			throw e;
		}
	}

	/**
	 * Puts the file chooser over all windows
	 * @author root
	 *
	 */
	@SuppressWarnings("serial")
	class AlwaysOnTopFileChooser extends JFileChooser {
		protected JDialog createDialog(Component parent) throws HeadlessException {
			JDialog dialog = super.createDialog(parent);
			dialog.setAlwaysOnTop(true);
			return dialog;
		}
	}
}
