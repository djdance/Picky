/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.orgcom.picky;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

/**
 *
 * @author Rasendori
 */
public class BitmapResizer {
    String TAG="djd";

    public BitmapResizer() {
    }

    public String getResizedPath(String imagePath) {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        //inJustDecodeBounds = true <-- will not load the bitmap into memory
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / 500, photoH / 500);
        if (scaleFactor <= 0)
            scaleFactor = 1;
        Log.d(TAG, "getResizedBitmap photoW="+photoW+", photoH="+photoH+", scaleFactor=" + scaleFactor);
        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, bmOptions);
        ExifInterface ei;
        try {
            ei = new ExifInterface(imagePath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.d(TAG, "exif orientation = " + orientation);
            //postlog.post("exif - orientation="+orientation);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    bitmap = rotateImage(bitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    bitmap = rotateImage(bitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    bitmap = rotateImage(bitmap, 270);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "exif orientation error "+e);
        }
        // save image
        File folder = new File(Environment.getExternalStorageDirectory() + "/pickyimages");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        if (success)
            try{
                String sss= Environment.getExternalStorageDirectory()+"/pickyimages/"+System.currentTimeMillis()+".jpg";
                FileOutputStream out = new FileOutputStream(sss);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
                imagePath=sss;
                Log.d(TAG,"getResizedBitmap  saved to "+imagePath);
            } catch (Exception e){
                Log.e(TAG, "getResizedBitmap err "+e);
            }
        return imagePath;
    }

    private Bitmap rotateImage(Bitmap source, float angle) {
        Bitmap bitmap = null;
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        try {
            bitmap = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                    matrix, true);
        } catch (OutOfMemoryError err) {
            Log.e(TAG,"OutOfMemoryError "+err);
        }
        return bitmap;
    }
}
