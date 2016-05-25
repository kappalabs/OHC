package com.kappa_labs.ohunter.client.utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.entities.SImage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * Singleton class providing useful functions for different types of usage.
 */
public class Utils {

    private static final String TAG = "Utils";

    /**
     * Default server IP address.
     */
    public static final String DEFAULT_ADDRESS = "localhost";
    /**
     * Default server port.
     */
    public static final int DEFAULT_PORT = 4242;
    /**
     * Default timeout for a single server task.
     */
    public static final int DEFAULT_TIMEOUT = 5000;

    private static String mAddress = DEFAULT_ADDRESS;
    private static int mPort = DEFAULT_PORT;

    /* Matrices for Sobel filter */
    private static final int[][] SOBEL_ROW = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};
    private static final int[][] SOBEL_DIAG = {{-2, -1, 0}, {-1, 0, 1}, {0, 1, 2}};


    private Utils() {
        /* Exists only to defeat instantiation */
    }

    /**
     * Sets the server address and port.
     *
     * @param address The server address to be used.
     * @param port The server port to be used.
     */
    public static void initServer(String address, int port) {
        mAddress = address;
        mPort = port;
    }

    /**
     * Sets the server address and port.
     *
     * @param address The server IP:port to be used.
     */
    public static boolean initServer(String address) {
        Log.d(TAG, "Initializing server with " + address);
        String[] parts = address.split(":");
        if (parts.length != 2) {
            mAddress = DEFAULT_ADDRESS;
            return false;
        }
        mAddress = parts[0];
        try {
            mPort = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            Log.e(TAG, "initServer(): Given address has wrong port format!");
            mPort = DEFAULT_PORT;
            return false;
        }
        return true;
    }

    /**
     * Gets the current server address in use.
     *
     * @return The current server address in use.
     */
    public static String getAddress() {
        return mAddress;
    }

    /**
     * Gets the current server port in use.
     *
     * @return The current server port in use.
     */
    public static int getPort() {
        return mPort;
    }

    /**
     * Transforms the SImage object from OHunter library to Android Bitmap.
     *
     * @param sImage The SImage object from OHunter library.
     * @return Converted SImage object into Android Bitmap.
     */
    public static Bitmap toBitmap(SImage sImage) {
        byte[] imgBytes = sImage.getBytes();
        if (imgBytes == null) {
            Log.e(TAG, "mam null simage, interesting...");
            return null;
        }
        return BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
    }

    /**
     * Transforms given SImage to Android Bitmap with optimization respect to given desired size.
     *
     * @param sImage The SImage object from OHunter library.
     * @param rectangularSize Desired rectangular sizes.
     * @return Converted SImage object into Android Bitmap with possibly desired size.
     */
    public static Bitmap toBitmap(SImage sImage, int rectangularSize) {
        byte[] imgBytes = sImage.getBytes();
        if (imgBytes == null) {
            Log.e(TAG, "s");
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length, options);

        if (options.outHeight <= options.outWidth) {
            options.inSampleSize = (int) ((double) options.outHeight / rectangularSize);
        } else {
            options.inSampleSize = (int) ((double) options.outWidth / rectangularSize);
        }

        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length, options);
    }

    /**
     * Class providing reading a bitmap in background async task.
     */
    public static class BitmapWorkerTask extends AsyncTask<SImage, Void, Bitmap> {

        private OnBitmapReady mListener;
        private Object mData;

        /**
         * Interface for callback when bitmap is ready.
         */
        public interface OnBitmapReady {

            /**
             * Called when the bitmap is ready.
             *
             * @param bitmap The prepared bitmap.
             * @param data Data associated with the preceding bitmap task.
             */
            void onBitmapReady(Bitmap bitmap, Object data);
        }

        /**
         * Create new task for reading bitmap.
         *
         * @param caller Listener on bitmap preparation.
         * @param data Optional object specifying this task will be returned when bitmap is ready.
         */
        public BitmapWorkerTask(OnBitmapReady caller, Object data) {
            this.mListener = caller;
            this.mData = data;
        }

        @Override
        protected Bitmap doInBackground(SImage... params) {
            byte[] imgBytes = params[0].getBytes();
            if (imgBytes == null) {
                Log.e(TAG, "mam null simage, interesting...");
                return null;
            }
            return BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (mListener != null) {
                mListener.onBitmapReady(bitmap, mData);
            } else {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        }
    }

    /**
     * Async task for counting edges in background.
     */
    public static class CountEdgesTask extends AsyncTask<Void, Void, Bitmap> {

        private DialogFragment mDialog;
        private Bitmap mBitmap;
        private OnEdgesTaskCompleted mListener;

        /**
         * Interface for callback when the edges task is ready.
         */
        public interface OnEdgesTaskCompleted{

            /**
             * Called when the edges task is ready.
             *
             * @param edges The counted bitmap.
             */
            void onEdgesTaskCompleted(Bitmap edges);
        }

        /**
         * Creates a new task for counting edges of given bitmap.
         *
         * @param caller Listener on the task.
         * @param dialog Dialog, which should be shown when this task is working.
         * @param original Original bitmap to be used for edges result.
         */
        public CountEdgesTask(OnEdgesTaskCompleted caller, DialogFragment dialog, Bitmap original) {
            mListener = caller;
            mDialog = dialog;
            mBitmap = original;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            return sobel(mBitmap);
        }

        @Override
        protected void onPostExecute(Bitmap edges) {
            super.onPostExecute(edges);
            if (mDialog != null) {
                mDialog.dismissAllowingStateLoss();
            }
            if (mListener != null) {
                mListener.onEdgesTaskCompleted(edges);
            }
        }
    }

    private static Bitmap sobel(Bitmap referenceImage) {
        int blurFactor = 2;
        /* Blur the image by changing the size */
        Bitmap smallerOrig = Bitmap.createScaledBitmap(referenceImage,
                referenceImage.getWidth() / blurFactor, referenceImage.getHeight() / blurFactor, true);
        Bitmap blurredOrig = Bitmap.createScaledBitmap(smallerOrig,
                smallerOrig.getWidth() * blurFactor, smallerOrig.getHeight() * blurFactor, true);
        smallerOrig.recycle();

        int width = blurredOrig.getWidth();
        int height = blurredOrig.getHeight();
        Bitmap edges = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[][] grays = new int[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                grays[i][j] = -1;
            }
        }
        int[] bins = new int[256];
        int binsCount = 0;

        int max = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rSum1 = 0, rSum2 = 0, rSum3 = 0, rSum4 = 0;
                int dSum1 = 0, dSum2 = 0, dSum3 = 0, dSum4 = 0;
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int xi = x + i, yj = y + j;
                        if (xi >= 0 && xi < width && yj >= 0 && yj < height) {
                            int gray = grays[xi][yj];
                            if (gray < 0) {
                                gray = rgb2gray(blurredOrig.getPixel(xi, yj));
                                grays[xi][yj] = gray;
                            }
                            rSum1 += gray * SOBEL_ROW[i + 1][j + 1];
                            dSum1 += gray * SOBEL_DIAG[i + 1][j + 1];
                            rSum2 += gray * SOBEL_ROW[1 - i][1 - j];
                            dSum2 += gray * SOBEL_DIAG[1 - i][1 - j];
                            rSum3 += gray * SOBEL_ROW[1 - i][j + 1];
                            dSum3 += gray * SOBEL_DIAG[1 - i][j + 1];
                            rSum4 += gray * SOBEL_ROW[i + 1][1 - j];
                            dSum4 += gray * SOBEL_DIAG[i + 1][1 - j];
                        }
                    }
                }
                int myGray = (Math.max(rSum1, Math.max(dSum1, Math.max(rSum2, Math.max(dSum2,
                        Math.max(rSum3, Math.max(dSum3, Math.max(rSum4, dSum4)))))))) & 0xFF;
                if (myGray > max) {
                    max = myGray;
                }
                if (myGray > 0) {
                    bins[myGray]++;
                    binsCount++;
                }
                int newPixel = (myGray << 24) | 0x00FFFFFF;
                edges.setPixel(x, y, newPixel);
            }
        }
        blurredOrig.recycle();

        /* Find good gray cut value and remove pixels with intensity smaller than this cut */
        int currentCount = 0;
        int grayCut = 0;
        int countCut = (int) (binsCount * 8.0 / 10.0);
        for (int i = 0; i < max; i++) {
            currentCount += bins[i];
            if (currentCount >= countCut) {
                grayCut = i;
                break;
            }
        }
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = edges.getPixel(x, y);
                if (((pixel >> 24) & 0xFF) < grayCut) {
                    edges.setPixel(x, y, 0x00FFFFFF);
                }
            }
        }

        return edges;
    }

    private static int rgb2gray(int color) {
        return (int) (0.21 * ((color >> 16) & 0xFF) + 0.72 * ((color >> 8) & 0xFF) + 0.07 * (color & 0xFF));
    }

    /**
     * Convert daytime to string describing given state.
     *
     * @param context Context of the requester.
     * @param daytime Daytime to be converted.
     * @return The string describing given state.
     */
    public static String daytimeToString(Context context, Photo.DAYTIME daytime) {
        switch (daytime) {
            case DAY:
                return context.getString(R.string.photo_bright);
            case NIGHT:
                return context.getString(R.string.photo_dark);
            default:
                return context.getString(R.string.photo_unknown);
        }
    }

    /**
     * Compute SHA hash of given password.
     *
     * @param password String to be hashed.
     * @return The SHA hash output string.
     */
    public static String getDigest(String password) {
        String digest = null;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA");
            digest = byteArray2Hex(md.digest(password.getBytes()));
        } catch (NoSuchAlgorithmException ex) {
            Log.e(TAG, "Cannog get diggest: " + ex);
        }

        return digest;
    }

    private static String byteArray2Hex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    /**
     * Changes color of given bitmap to specified value. The original bitmap is not released.
     *
     * @param sourceBitmap The bitmap to be colored.
     * @param color The RGB color of color filter.
     * @return The colored bitmap.
     */
    public static Bitmap changeBitmapColor(Bitmap sourceBitmap, int color) {
        Bitmap resultBitmap = sourceBitmap.copy(sourceBitmap.getConfig(), true);
        Paint p = new Paint();
        ColorFilter filter = new LightingColorFilter(color, 0);
        p.setColorFilter(filter);

        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(resultBitmap, 0, 0, p);

        return resultBitmap;
    }

}
