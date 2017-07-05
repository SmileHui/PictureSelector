package com.smilehui.pictureselector.utils;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;

public class FileUtils {

    private static String TAG = "FileUtils";

    public FileUtils() {
    }

    public static String getCachePath(Context context, @NonNull String dir) {
        boolean sdCardExist = Environment.getExternalStorageState().equals("mounted");
        File cacheDir = context.getExternalCacheDir();
        if (!sdCardExist || cacheDir == null || !cacheDir.exists() || !cacheDir.mkdirs()) {
            cacheDir = context.getCacheDir();
        }

        File tarDir = new File(cacheDir.getPath() + File.separator + dir);
        if (!tarDir.exists()) {
            boolean result = tarDir.mkdir();
            Log.w(TAG, "getCachePath = " + tarDir.getPath() + ", result = " + result);
            if (!result) {
                tarDir = new File("/sdcard/cache/" + dir);
                if (!tarDir.exists()) {
                    result = tarDir.mkdirs();
                }
                Log.e(TAG, "change path = " + tarDir.getPath() + ", result = " + result);
            }
        }

        return tarDir.getPath();
    }

}
