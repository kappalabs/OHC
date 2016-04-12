package com.kappa_labs.ohunter.client.utilities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.entities.SImage;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.Request;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * Singleton class providing useful functions for different types of usage.
 */
public class Utils {

    public static final String TAG = "Utils";

    public static final String DEFAULT_ADDRESS = "localhost";
    public static final int DEFAULT_PORT = 4242;
    public static final int DEFAULT_TIMEOUT = 5000;

    private static String mAddress = DEFAULT_ADDRESS;
    private static int mPort = DEFAULT_PORT;

    /* Matrices for Sobel filter */
    private static final int[][] SOBEL_ROW = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};
    private static final int[][] SOBEL_DIAG = {{-2, -1, 0}, {-1, 0, 1}, {0, 1, 2}};

    private static Utils mInstance = null;


    private Utils() {
        /* Exists only to defeat instantiation */
    }

    /**
     * Get instance of this singleton class.
     *
     * @return The instance of this singleton class.
     */
    public static Utils getInstance() {
        if (mInstance == null) {
            mInstance = new Utils();
        }
        return mInstance;
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
        System.out.println("initing with "+address);
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

    public class BitmapWorkerTask extends AsyncTask<SImage, Void, Bitmap> {

        private OnBitmapReady mListener;
        private Object mData;


//        public BitmapWorkerTask(OnBitmapReady caller) {
//            this.mListener = caller;
//        }

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
            }
        }
    }

    public interface OnBitmapReady {
        void onBitmapReady(Bitmap bitmap, Object data);
    }

    public class RetrieveResponseTask extends AsyncTask<Request, Void, Response> {

        private OHException ohException;
        private OnResponseTaskCompleted mListener;
        private ProgressDialogFragment mProgressDialog;
        private Object mData;
        private Request mRequest;


        /**
         * Create a new task to retrieve data from server.
         *
         * @param caller The caller, that will be notified, can be null.
         * @param progressDialog Reference to dialog, which will be closed after this task.
         */
        public RetrieveResponseTask(OnResponseTaskCompleted caller, ProgressDialogFragment progressDialog) {
            this.mListener = caller;
            this.mProgressDialog = progressDialog;
        }

        /**
         * Create a new task to retrieve data from server.
         *
         * @param caller The caller, that will be notified, can be null.
         * @param progressDialog Reference to dialog, which will be closed after this task.
         * @param data Data object that will be returned on callback, when this task is completed.
         */
        public RetrieveResponseTask(OnResponseTaskCompleted caller, ProgressDialogFragment progressDialog, Object data) {
            this.mListener = caller;
            this.mProgressDialog = progressDialog;
            this.mData = data;
        }

        @Override
        protected Response doInBackground(Request... params) {
            try {
                mRequest = params[0];
                return getServerResponse(mRequest);
            } catch (OHException e) {
                ohException = e;
                return null;
            }
        }

        protected void onPostExecute(Response response) {
            if (mListener != null) {
                mListener.onResponseTaskCompleted(mRequest, response, ohException, mData);
            }
            if (mProgressDialog != null) {
                mProgressDialog.dismissAllowingStateLoss();
            }
        }
    }

    public interface OnResponseTaskCompleted {
        void onResponseTaskCompleted(Request request, Response response, OHException ohex, Object data);
    }

    public static class ProgressDialogFragment extends DialogFragment {

        private static String mTitle, mMessage;

        public void setTexts(String title, String message) {
            mTitle = title;
            mMessage = message;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final ProgressDialog dialog = ProgressDialog.show(getActivity(), mTitle, mMessage, true);
            dialog.setCancelable(false);
//            dialog.setIndeterminate(true);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

            return dialog;
        }
    }

    public static ProgressDialogFragment getStandardDialog(Context context, String title, String message) {
        ProgressDialogFragment dialog = new ProgressDialogFragment();
        dialog.setTexts(title, message);
        dialog.setCancelable(false);
        if (context instanceof AppCompatActivity) {
            final AppCompatActivity activity = (AppCompatActivity) context;

            FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
            transaction.add(dialog, "waiting");
            transaction.commitAllowingStateLoss();
        } else {
            Log.e(TAG, "Cannot show standard dialog, context is not AppCompat Activity!");
            return null;
        }
        return dialog;
    }

    public static ProgressDialogFragment getServerCommunicationDialog(Context context) {
        return getStandardDialog(context, context.getString(R.string.server_communication),
                context.getString(R.string.waiting_for_data));
    }

    public static Response getServerResponse(Request request) throws OHException {
        /* Check if server address is set */
        if (mAddress == null || mPort == 0) {
            throw new RuntimeException("Utils must have server set before any communication!");
        }

        Log.d(TAG, "getServerResponse(): asking server [" + mAddress + ":" + mPort + "]\n -> request info: " + request);
        Response response = null;
        Socket server = null;
        try {
            server = new Socket();
            int mTimeout = DEFAULT_TIMEOUT;
            server.connect(new InetSocketAddress(mAddress, mPort), mTimeout);
            ObjectOutputStream oos = null;
            ObjectInputStream ois = null;
            try {
                oos = new ObjectOutputStream(server.getOutputStream());
                oos.writeObject(request);
                oos.flush();
                Log.d(TAG, "Data odeslana, cekam na odpoved...");

                ois = new ObjectInputStream(server.getInputStream());
                Object obj = ois.readObject();
                try {
                    response = (Response) obj;
                } catch (ClassCastException ex) {
                    if (obj instanceof OHException) {
                        throw (OHException) obj;
                    } else {
                        Log.e(TAG, "Unknown type of response object from the server!");
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                Log.e(TAG, "Error when communicating with server: " + e);
            } finally {
                if (oos != null) {
                    oos.close();
                }
                if (ois != null) {
                    ois.close();
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Error when connecting to server: " + ex);
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error when closing with server: " + e);
                }
            }
        }

        return response;
    }

    public class CountEdgesTask extends AsyncTask<Void, Void, Bitmap> {

        private ProgressDialogFragment mDialog;
        private Bitmap mBitmap;
        private OnEdgesTaskCompleted mListener;

        public CountEdgesTask(OnEdgesTaskCompleted caller, ProgressDialogFragment dialog, Bitmap original) {
            mDialog = dialog;
            mBitmap = original;
            mListener = caller;
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

    public interface OnEdgesTaskCompleted{
        void onEdgesTaskCompleted(Bitmap edges);
    }

    private Bitmap sobel(Bitmap referenceImage) {
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

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int souc1 = 0, souc2 = 0, souc3 = 0, souc4 = 0;
                int souc5 = 0, souc6 = 0, souc7 = 0, souc8 = 0;
                int poc = 0;
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int xi = x + i, yj = y + j;
                        if (xi >= 0 && xi < width && yj >= 0 && yj < height) {
                            // NOTE: pozor, pokud neni v obrazku zelena slozka...!
                            int gray = (blurredOrig.getPixel(xi, yj) >> 8) & 0xFF;
                            souc1 += gray * SOBEL_ROW[i + 1][j + 1];
                            souc2 += gray * SOBEL_DIAG[i + 1][j + 1];
                            souc3 += gray * SOBEL_ROW[1 - i][1 - j];
                            souc4 += gray * SOBEL_DIAG[1 - i][1 - j];
                            souc5 += gray * SOBEL_ROW[1 - i][j + 1];
                            souc6 += gray * SOBEL_DIAG[1 - i][j + 1];
                            souc7 += gray * SOBEL_ROW[i + 1][1 - j];
                            souc8 += gray * SOBEL_DIAG[i + 1][1 - j];
                            poc++;
                        }
                    }
                }
                int myGray = 0xff - Math.max(souc1, Math.max(souc2, Math.max(souc3, Math.max(souc4,
                        Math.max(souc5, Math.max(souc6, Math.max(souc7, souc8))))))) / poc;
                // NOTE: alfa slozka je vynasobena 4 pro zvyrazneni
                int newPixel = ((0xff - myGray) << 26) | (myGray << 16) | (myGray << 8) | myGray;
                edges.setPixel(x, y, newPixel);
            }
        }
        blurredOrig.recycle();

        return edges;
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

}
