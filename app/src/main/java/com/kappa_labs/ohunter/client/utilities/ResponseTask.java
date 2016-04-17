package com.kappa_labs.ohunter.client.utilities;

import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.Request;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Class providing asynchronous communication with server.
 */
public class ResponseTask extends AsyncTask<Request, Void, Response> {

    private static final String TAG = "ResponseTask";

    private OHException ohException;
    private OnResponseTaskCompleted mListener;
    private DialogFragment mProgressDialog;
    private Object mData;
    private Request mRequest;

    public interface OnResponseTaskCompleted {
        void onResponseTaskCompleted(Request request, Response response, OHException ohex, Object data);
    }


    /**
     * Create a new task to retrieve data from server.
     *
     * @param caller The caller, that will be notified, can be null.
     * @param progressDialog Reference to dialog, which will be closed after this task.
     */
    public ResponseTask(DialogFragment progressDialog, OnResponseTaskCompleted caller) {
        this.mProgressDialog = progressDialog;
        this.mListener = caller;
    }

    /**
     * Create a new task to retrieve data from server.
     *
     * @param caller The caller, that will be notified, can be null.
     * @param progressDialog Reference to dialog, which will be closed after this task.
     * @param data Data object that will be returned on callback, when this task is completed.
     */
    public ResponseTask(DialogFragment progressDialog, Object data, OnResponseTaskCompleted caller) {
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
        if (mProgressDialog != null && mProgressDialog.getFragmentManager() != null) {
            mProgressDialog.dismissAllowingStateLoss();
        }
    }

    public static Response getServerResponse(Request request) throws OHException {
        /* Check if server address is set */
        if (Utils.getAddress() == null || Utils.getPort() == 0) {
            throw new RuntimeException("Utils must have server set before any communication!");
        }

        Log.d(TAG, "getServerResponse(): asking server [" + Utils.getAddress() + ":" + Utils.getPort() + "]\n -> request info: " + request);
        Response response = null;
        Socket server = null;
        try {
            server = new Socket();
            int mTimeout = Utils.DEFAULT_TIMEOUT;
            server.connect(new InetSocketAddress(Utils.getAddress(), Utils.getPort()), mTimeout);
            ObjectOutputStream oos = null;
            ObjectInputStream ois = null;
            try {
                oos = new ObjectOutputStream(server.getOutputStream());
                oos.writeObject(request);
                oos.flush();
                Log.d(TAG, "Data sent, waiting for result...");

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

}
