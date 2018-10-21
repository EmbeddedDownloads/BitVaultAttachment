package com.bitVaultAttachment.controller;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileLock;
import java.util.ResourceBundle;
import java.util.logging.Level;

import com.bitVaultAttachment.apiMethods.Utils;
import com.bitVaultAttachment.constant.BitVaultConstants;
import com.bitVaultAttachment.splashScreen.SplashScreen;
import com.sun.javafx.application.LauncherImpl;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Created by Shelly on 5/17/2017.
 */
public class MainPage extends Application implements Initializable {

	@FXML 
	private ImageView imageView;
	@FXML 
	private ImageView IsRegisterImageView;
	@FXML 
	public static Stage stage;

	public  static String bitVaultToken;

	/**
	 * Main method to Start Application
	 * When Initially Application Starts Splash Screen will open
	 * @param args
	 */
	public static void main(String[] args) {
		// initialize logger
		Utils.initLogger();
		Utils.getLogger().log(Level.INFO, "********* Starting Application **************\n");
		// create lock file
		if ( !lockInstance( BitVaultConstants.PATH_FOR_DATABASE + File.separator + "lockfile") )
			System.exit(0);
		// initialize MainPage
		try {
			LauncherImpl.launchApplication(MainPage.class, SplashScreen.class, args);
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE, "Error starting MainPage", e);
		}
	}

	/**
	 * Start Method Of Application after Splash Screen
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			stage=primaryStage;
			VBox page = (VBox) FXMLLoader.load(MainPage.class.getResource("/fxml/MainPage.fxml"));
			BackgroundImage myBI= new BackgroundImage(new Image("images/background.png",1000,1000,true,true),
					BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
					new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true));
			// set stage and scene
			page.setBackground(new Background(myBI));
			Scene scene = new Scene(page);
			stage.setScene(scene);
			scene.getStylesheets().addAll("bitVaultMain.css");
			stage.getIcons().add(new Image("images/desktop_icon.png"));

			stage.setMinHeight(400);
			stage.setMinWidth(600);
			stage.setTitle("BitVault Attachment");
			stage.show();			
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE, "Error starting MainPage", e);
		}
	}

	/*
	 * Initial methods to bind action with start button
	 * */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		IsRegisterImageView.setFitWidth(150);
		IsRegisterImageView.setFitHeight(150);
		IsRegisterImageView.setPreserveRatio(true);
		IsRegisterImageView.setVisible(false);

		WebCamImageProvider imageProvider = new WebCamImageProvider();
		imageProvider.setRedirectFxml("/fxml/GeneralPage.fxml");
		imageProvider.setPostWebCamAction(BitVaultConstants.CAM_FOR_REGISTRATION);
		imageView.imageProperty().bind(imageProvider.imageProperty());
		IsRegisterImageView.visibleProperty().bind(imageProvider.imageVisibility());
		IsRegisterImageView.imageProperty().bind(imageProvider.imageRegisterVisibility());

		Thread t = new Thread(imageProvider);
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Lock application instance
	 * @param lockFile
	 * @return
	 */
	private static boolean lockInstance(final String lockFile) {
		try {
			final File file = new File(lockFile);
			final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
			final FileLock fileLock = randomAccessFile.getChannel().tryLock();
			if (fileLock != null) {
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						try {
							fileLock.release();
							randomAccessFile.close();
							file.delete();
						} catch (Exception e) {
							Utils.getLogger().log(Level.SEVERE, "Unable to remove lock file: " + lockFile, e);
						}
					}
				});
				return true;
			}
		} catch (Exception e) {
			Utils.getLogger().log(Level.SEVERE, "Unable to create and/or lock file: " + lockFile, e);
		}
		Utils.getLogger().info("Instance already running");
		return false;
	}
}
