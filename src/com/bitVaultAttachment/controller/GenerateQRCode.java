package com.bitVaultAttachment.controller;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.logging.Level;

import com.bitVaultAttachment.apiMethods.Utils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;

/**
 * Created by vvdn on 5/29/2017.
 */
public class GenerateQRCode {
    /**
     * Create a QR Code from String Value
     *
     * @param data--String whose Qr Code is to be created
     * @return Image for QR code
     * @throws WriterException
     */
    public static ImageView generateQRCode(String data) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        int width = 180;
        int height = 180;
        BufferedImage bufferedImage = null;

        BitMatrix byteMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
        bufferedImage = new BufferedImage(width-40, height-40, BufferedImage.TYPE_INT_RGB);
        bufferedImage.createGraphics();
        Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
        graphics.setColor(Color.decode("#F3F3F3"));
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.BLACK);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (byteMatrix.get(i, j)) {
                    graphics.fillRect(i-20, j-20, 1, 1);
                }
            }
        }

        Utils.getLogger().log(Level.FINEST,"Generated QR successfully");
        ImageView qrView = new ImageView();
        qrView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
        qrView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
        return qrView;
    }
}
