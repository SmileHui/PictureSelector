package com.smilehui.pictureselector;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.smilehui.pictureselector.utils.FileUtils;
import com.smilehui.pictureselector.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AlbumBitmapCacheHelper {
    private static final String TAG = "AlbumBitmapCacheHelper";
    private static volatile AlbumBitmapCacheHelper instance = null;
    private LruCache<String, Bitmap> cache;
    private static int cacheSize;
    private ArrayList<String> currentShowString;
    private Context mContext;
    ThreadPoolExecutor tpe;

    private AlbumBitmapCacheHelper() {
        this.tpe = new ThreadPoolExecutor(2, 5, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue());
        this.cache = new LruCache<String, Bitmap>(cacheSize) {
            protected int sizeOf(String key, Bitmap value) {
                int result;
                if (VERSION.SDK_INT >= 19) {
                    result = value.getAllocationByteCount();
                } else if (VERSION.SDK_INT >= 12) {
                    result = value.getByteCount();
                } else {
                    result = value.getRowBytes() * value.getHeight();
                }
                return result;
            }
        };
        this.currentShowString = new ArrayList();
    }

    public void releaseAllSizeCache() {
        this.cache.evictAll();
        this.cache.resize(1);
    }

    public void releaseHalfSizeCache() {
        this.cache.resize((int) (Runtime.getRuntime().maxMemory() / 1024L / 8L));
    }

    public void resizeCache() {
        this.cache.resize((int) (Runtime.getRuntime().maxMemory() / 1024L / 4L));
    }

    private void clearCache() {
        this.cache.evictAll();
        this.cache = null;
        this.tpe = null;
        instance = null;
    }

    public static AlbumBitmapCacheHelper getInstance() {
        if (instance == null) {
            synchronized (AlbumBitmapCacheHelper.class) {
                if (instance == null) {
                    instance = new AlbumBitmapCacheHelper();
                }
            }
        }
        return instance;
    }

    public static void init(Context context) {
        Log.d("AlbumBitmapCacheHelper", "init");
        cacheSize = calculateMemoryCacheSize(context);
        AlbumBitmapCacheHelper helper = getInstance();
        helper.mContext = context.getApplicationContext();
    }

    public void uninit() {
        Log.d("AlbumBitmapCacheHelper", "uninit");
        this.tpe.shutdownNow();
        this.clearCache();
    }

    public Bitmap getBitmap(String path, int width, int height, AlbumBitmapCacheHelper.ILoadImageCallback callback, Object... objects) {
        Bitmap bitmap = this.getBitmapFromCache(path, width, height);
        if (bitmap != null) {
            Log.e("AlbumBitmapCacheHelper", "getBitmap from cache");
        } else {
            this.decodeBitmapFromPath(path, width, height, callback, objects);
        }
        return bitmap;
    }

    private void decodeBitmapFromPath(final String path, final int width, final int height, final AlbumBitmapCacheHelper.ILoadImageCallback callback, final Object... objects) throws OutOfMemoryError {
        final Handler handler = new Handler() {
            public void handleMessage(Message msg) {
                if (callback != null) {
                    callback.onLoadImageCallBack((Bitmap) msg.obj, path, objects);
                }

            }
        };
        this.tpe.execute(new Runnable() {
            public void run() {
                if (AlbumBitmapCacheHelper.this.currentShowString.contains(path) && AlbumBitmapCacheHelper.this.cache != null) {
                    Bitmap bitmap = null;
                    if (width != 0 && height != 0) {
                        String msg = Utils.md5(path + "_" + width + "_" + height);
                        String tempPath = FileUtils.getCachePath(AlbumBitmapCacheHelper.this.mContext, "image") + "/" + msg + ".temp";
                        File picFile = new File(path);
                        File tempFile = new File(tempPath);
                        if (tempFile.exists() && picFile.lastModified() <= tempFile.lastModified()) {
                            bitmap = BitmapFactory.decodeFile(tempPath);
                        }

                        if (bitmap == null) {
                            try {
                                bitmap = AlbumBitmapCacheHelper.this.getBitmap(path, width, height);
                            } catch (OutOfMemoryError var11) {
                                bitmap = null;
                            }

                            if (bitmap != null && AlbumBitmapCacheHelper.this.cache != null) {
                                bitmap = AlbumBitmapCacheHelper.centerSquareScaleBitmap(bitmap, bitmap.getWidth() > bitmap.getHeight() ? bitmap.getHeight() : bitmap.getWidth());
                            }

                            if (bitmap != null) {
                                try {
                                    File e = new File(tempPath);
                                    if (!e.exists()) {
                                        e.createNewFile();
                                    } else {
                                        e.delete();
                                        e.createNewFile();
                                    }
                                    FileOutputStream fos = new FileOutputStream(e);
                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                    bitmap.compress(CompressFormat.PNG, 100, byteArrayOutputStream);
                                    fos.write(byteArrayOutputStream.toByteArray());
                                    fos.flush();
                                    fos.close();
                                } catch (IOException var10) {
                                    var10.printStackTrace();
                                }
                            }
                        } else if (AlbumBitmapCacheHelper.this.cache != null) {
                            bitmap = AlbumBitmapCacheHelper.centerSquareScaleBitmap(bitmap, bitmap.getWidth() > bitmap.getHeight() ? bitmap.getHeight() : bitmap.getWidth());
                        }
                    } else {
                        try {
                            bitmap = AlbumBitmapCacheHelper.this.getBitmap(path, width, height);
                        } catch (OutOfMemoryError var12) {
                            var12.printStackTrace();
                        }
                    }

                    if (bitmap != null && AlbumBitmapCacheHelper.this.cache != null) {
                        AlbumBitmapCacheHelper.this.cache.put(path + "_" + width + "_" + height, bitmap);
                    }

                    Message msg1 = Message.obtain();
                    msg1.obj = bitmap;
                    handler.sendMessage(msg1);
                }
            }
        });
    }

    public static Bitmap centerSquareScaleBitmap(Bitmap bitmap, int edgeLength) {
        if (null != bitmap && edgeLength > 0) {
            int widthOrg = bitmap.getWidth();
            int heightOrg = bitmap.getHeight();
            int xTopLeft = (widthOrg - edgeLength) / 2;
            int yTopLeft = (heightOrg - edgeLength) / 2;
            if (xTopLeft == 0 && yTopLeft == 0) {
                return bitmap;
            } else {
                try {
                    Bitmap result = Bitmap.createBitmap(bitmap, xTopLeft, yTopLeft, edgeLength, edgeLength);
                    if (!bitmap.isRecycled()) {
                        bitmap.recycle();
                    }

                    return result;
                } catch (OutOfMemoryError var8) {
                    return bitmap;
                }
            }
        } else {
            return null;
        }
    }

    private int computeScale(Options options, int width, int height) {
        if (options == null) {
            return 1;
        } else {
            int widthScale = (int) ((float) options.outWidth / (float) width);
            int heightScale = (int) ((float) options.outHeight / (float) height);
            int scale = widthScale > heightScale ? widthScale : heightScale;
            if (scale < 1) {
                scale = 1;
            }
            return scale;
        }
    }

    private Bitmap getBitmapFromCache(String path, int width, int height) {
        return this.cache.get(path + "_" + width + "_" + height);
    }

    public void addPathToShowList(String path) {
        this.currentShowString.add(path);
    }

    public void removePathFromShowList(String path) {
        this.currentShowString.remove(path);
    }

    private Bitmap getBitmap(String path, int widthLimit, int heightLimit) throws OutOfMemoryError {
        Bitmap bitmap = null;

        try {
            Options e = new Options();
            e.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, e);
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt("Orientation", 0);
            int w;
            int h;
            int xS;
            int sampleSize1;
            if (widthLimit == 0 && heightLimit == 0) {
                sampleSize1 = this.computeScale(e, mContext.getResources().getDisplayMetrics().widthPixels
                        , mContext.getResources().getDisplayMetrics().widthPixels);
            } else {
                int matrix;
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90
                        || orientation == ExifInterface.ORIENTATION_ROTATE_270
                        || orientation == ExifInterface.ORIENTATION_TRANSPOSE
                        || orientation == ExifInterface.ORIENTATION_TRANSVERSE) {
                    matrix = widthLimit;
                    widthLimit = heightLimit;
                    heightLimit = matrix;
                }

                matrix = e.outWidth;
                w = e.outHeight;
                h = 1;

                for (xS = 1; matrix / 2 > widthLimit; h <<= 1) {
                    matrix /= 2;
                }

                while (w / 2 > heightLimit) {
                    w /= 2;
                    xS <<= 1;
                }

                if (widthLimit != Integer.MAX_VALUE && heightLimit != Integer.MAX_VALUE) {
                    sampleSize1 = Math.max(h, xS);
                } else {
                    sampleSize1 = Math.max(h, xS);
                }
            }

            try {
                e = new Options();
                e.inJustDecodeBounds = false;
                e.inSampleSize = sampleSize1;
                bitmap = BitmapFactory.decodeFile(path, e);
            } catch (OutOfMemoryError var14) {
                var14.printStackTrace();
                e.inSampleSize <<= 1;
                bitmap = BitmapFactory.decodeFile(path, e);
            }

            Matrix matrix1 = new Matrix();
            if (bitmap != null) {
                w = bitmap.getWidth();
                h = bitmap.getHeight();
                if (orientation == 6 || orientation == 8 || orientation == 5 || orientation == 7) {
                    xS = w;
                    w = h;
                    h = xS;
                }

                switch (orientation) {
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                        matrix1.preScale(-1.0F, 1.0F);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix1.setRotate(180.0F, (float) w / 2.0F, (float) h / 2.0F);
                        break;
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                        matrix1.preScale(1.0F, -1.0F);
                        break;
                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        matrix1.setRotate(90.0F, (float) w / 2.0F, (float) h / 2.0F);
                        matrix1.preScale(1.0F, -1.0F);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix1.setRotate(90.0F, (float) w / 2.0F, (float) h / 2.0F);
                        break;
                    case ExifInterface.ORIENTATION_TRANSVERSE:
                        matrix1.setRotate(270.0F, (float) w / 2.0F, (float) h / 2.0F);
                        matrix1.preScale(1.0F, -1.0F);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix1.setRotate(270.0F, (float) w / 2.0F, (float) h / 2.0F);
                }

                if (widthLimit != 0 && heightLimit != 0) {
                    float xS1 = (float) widthLimit / (float) bitmap.getWidth();
                    float yS = (float) heightLimit / (float) bitmap.getHeight();
                    matrix1.postScale(Math.min(xS1, yS), Math.min(xS1, yS));
                }

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix1, true);
            }
        } catch (IOException var15) {
            var15.printStackTrace();
        }

        return bitmap;
    }

    static int calculateMemoryCacheSize(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        boolean largeHeap = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_LARGE_HEAP) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap && VERSION.SDK_INT >= 11) {
            memoryClass = am.getLargeMemoryClass();
        }
        return (int) (1048576L * (long) memoryClass / 8L);
    }

    public interface ILoadImageCallback {
        void onLoadImageCallBack(Bitmap var1, String var2, Object... var3);
    }
}