/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.orgcom.picky;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

/**
 *
 * @author Rasendori
 */
public class SoundDuration {
    String TAG="djd";
    Context context;

    public SoundDuration(Context context) {
        this.context=context;
    }

    public long getSoundDuration(int rawId, String file) {
        MediaPlayer player;
        if (file==null || file.equals(""))
            player= MediaPlayer.create(context, rawId);
        else
            player= MediaPlayer.create(context, Uri.parse(file));
        int duration = player!=null?player.getDuration():0;
        //Log.i(TAG,"          getSoundDuration="+duration);
        if (player!=null) player.release();
        return duration;
    }
}
