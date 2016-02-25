package client.ohunter.fojjta.cekuj.net.ohunter;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.kappa_labs.ohunter.lib.entities.Place;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.entities.SImage;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.Request;
import com.kappa_labs.ohunter.lib.requests.SearchRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Class providing useful functions for different types of usage.
 */
public class Utils {

    public static final String TAG = "Utils";

//    public static final String ADRESS = "192.168.1.196";    // AP doma
//    public static final String ADRESS = "192.168.42.56";  // USB tether
//    public static final String ADRESS = "192.168.43.144"; // Android AP
    public static final String ADRESS = "195.113.16.233"; // Eduroam
    public static final int PORT = 4242;

    /* Matrices for Sobel filter */
    private static final int[][] SOBEL_ROW = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};
    private static final int[][] SOBEL_DIAG = {{-2, -1, 0}, {-1, 0, 1}, {0, 1, 2}};


    public static Bitmap toBitmap(SImage sImage) {
        byte[] imgBytes = sImage.getImage();
        return BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
    }

    class RetrieveResponseTask extends AsyncTask<Request, Void, Response> {

        private OHException ohException;
        private OnResponseTaskCompleted mListener;
        private ProgressDialog mProgressDialog;


        public RetrieveResponseTask(OnResponseTaskCompleted caller, ProgressDialog progressDialog) {
            this.mListener = caller;
            this.mProgressDialog = progressDialog;
        }

        @Override
        protected Response doInBackground(Request... params) {
            try {
                return getServerResponse(params[0]);
            } catch (OHException e) {
                ohException = e;
                return null;
            }
        }

        protected void onPostExecute(Response response) {
            if (mListener != null) {
                mListener.onResponseTaskCompleted(response, ohException);
            }
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
        }
    }

    public interface OnResponseTaskCompleted{
        void onResponseTaskCompleted(Response response, OHException ohex);
    }

    public static ProgressDialog getStandardDialog(Context context, String title, String message) {
        ProgressDialog dialog = ProgressDialog.show(context, title, message, true);
        dialog.setCancelable(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.show();
        return dialog;
    }

    public static ProgressDialog getServerCommunicationDialog(Context context) {
        return getStandardDialog(context, context.getString(R.string.server_communication),
                context.getString(R.string.waiting_for_data));
    }

    public static Response getServerResponse(Request request) throws OHException {
        Log.d(TAG, "getServerResponse(): start");
        Response response = null;
        Socket server = null;
        try {
            server = new Socket(ADRESS, PORT);
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
                e.printStackTrace();
            } finally {
                if (oos != null) {
                    oos.close();
                }
                if (ois != null) {
                    ois.close();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return response;
    }


    class CountEdgesTask extends AsyncTask<Void, Void, Bitmap> {

        private ProgressDialog mDialog;
        private Bitmap mBitmap;
        private OnEdgesTaskCompleted mListener;

        public CountEdgesTask(OnEdgesTaskCompleted caller, ProgressDialog dialog, Bitmap original) {
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
                mDialog.dismiss();
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
        Bitmap edges = null;
        Bitmap orig = referenceImage;
        /* Make the image bigger, so that it's blured */
        Bitmap bigOrig = Bitmap.createScaledBitmap(referenceImage,
                orig.getWidth() * 2, orig.getHeight() * 2, true);
        int width = bigOrig.getWidth();
        int height = bigOrig.getHeight();
        Bitmap bigEdges = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

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
                            int gray = (bigOrig.getPixel(xi, yj) >> 8) & 0xFF;
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
                bigEdges.setPixel(x, y, newPixel);
            }
        }
        /* Original size is different from this one -> resize back */
        edges = Bitmap.createScaledBitmap(bigEdges, orig.getWidth(), orig.getHeight(), true);
        bigOrig.recycle();
        bigEdges.recycle();

        return edges;
    }

}
