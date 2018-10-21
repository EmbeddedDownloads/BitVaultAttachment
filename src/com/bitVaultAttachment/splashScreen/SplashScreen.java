package com.bitVaultAttachment.splashScreen;

import java.util.logging.Level;

import com.bitVaultAttachment.apiMethods.Utils;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;


public class SplashScreen extends Preloader {
	private Stage splashScreen;
	Label splashImage;
	FlowPane root=null;

	@Override
	public void start(Stage stage) throws Exception {
		splashScreen = stage;
		splashScreen.initStyle(StageStyle.UNDECORATED);

		ImageView imageView = new ImageView(new Image("/images/splashscreen.png"));
		imageView.setFitWidth(1000);
		imageView.setFitHeight(550);
		imageView.setPickOnBounds(true);
		imageView.setX(50);
		imageView.setY(50);

		splashImage = new Label("BitVault Attachment", imageView);

		root = new FlowPane();

		splashScreen.setWidth(1000);
		splashScreen.setHeight(550);
		splashScreen.setResizable(false);
		root.getChildren().addAll(imageView);

		Scene scene = new Scene(root, 1000, 550);
		splashScreen.setScene(scene);
		splashScreen.setOnCloseRequest((ae) -> {
			Platform.exit();
		});
		splashScreen.show();
	}

	@Override
	public void handleStateChangeNotification(StateChangeNotification evt) {
		if (evt.getType() == StateChangeNotification.Type.BEFORE_START) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Utils.getLogger().log(Level.SEVERE, "splash screen wait exception", e);
			}

			Timeline tick0 = new Timeline();
			tick0.setCycleCount(Timeline.INDEFINITE);
			tick0.getKeyFrames().add(new KeyFrame(new Duration(10), new EventHandler<ActionEvent>() {
				public void handle(ActionEvent t) {
					root.setOpacity(root.getOpacity() - 0.01);
					if (root.getOpacity() < 0.01) {// 30 divided by 0.01 equals 3000
						// so you take the duration and
						// divide it be the opacity to
						// get your transition time in
						// milliseconds
						splashScreen.hide();
						tick0.stop();
					}
				}
			}));
			tick0.play();
		}
	}

}

