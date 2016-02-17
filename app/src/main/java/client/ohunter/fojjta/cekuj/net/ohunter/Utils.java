package client.ohunter.fojjta.cekuj.net.ohunter;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

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

    public static final String ADRESS = "192.168.1.196";    // AP doma
//    public static final String ADRESS = "192.168.42.56";  // USB tether
//    public static final String ADRESS = "192.168.43.144"; // Android AP
    public static final int PORT = 4242;


    public static Bitmap toBitmap(SImage sImage) {
        byte[] imgBytes = sImage.getImage();
        return BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
    }

    class RetrieveResponseTask extends AsyncTask<Request, Void, Response> {

        private OHException ohException;
        private OnTaskCompleted mListener;
        private ProgressDialog mProgressDialog;


        public RetrieveResponseTask(OnTaskCompleted caller, ProgressDialog progressDialog) {
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
                mListener.onTaskCompleted(response, ohException);
            }
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
        }
    }

    public interface OnTaskCompleted{
        void onTaskCompleted(Response response, OHException ohex);
    }

    public static ProgressDialog getServerCommunicationDialog(Context context) {
        ProgressDialog dialog = ProgressDialog.show(context,
                context.getString(R.string.server_communication),
                context.getString(R.string.waiting_for_data),
                true);
        dialog.setCancelable(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.show();
        return dialog;
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

}
