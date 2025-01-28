package com.upitranzact.sdk.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QRCodeGenerator {

    public static Bitmap generateQRCodeWithLogo(String data, Bitmap logo) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap qrCode = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 800, 800);

            Bitmap combinedBitmap = Bitmap.createBitmap(qrCode.getWidth(), qrCode.getHeight(), qrCode.getConfig());
            Canvas canvas = new Canvas(combinedBitmap);

            canvas.drawBitmap(qrCode, 0, 0, null);

            int logoSize = qrCode.getWidth() / 5;
            Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, logoSize, logoSize, false);

            float left = (float) (qrCode.getWidth() - scaledLogo.getWidth()) / 2;
            float top = (float) (qrCode.getHeight() - scaledLogo.getHeight()) / 2;

            canvas.drawBitmap(scaledLogo, left, top, null);

            return combinedBitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
