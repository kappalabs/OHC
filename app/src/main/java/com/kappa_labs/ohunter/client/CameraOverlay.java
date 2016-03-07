package com.kappa_labs.ohunter.client;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraOverlay extends SurfaceView implements SurfaceHolder.Callback {

    public static final String TAG = "CameraOverlay";

    private OnCameraActionListener mListener;
    public static Bitmap mBitmap;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;


    public CameraOverlay(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        init(context);
    }

    public CameraOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraOverlay(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        if (context instanceof OnCameraActionListener) {
            mListener = (OnCameraActionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnCameraActionListener");
        }
        mHolder = getHolder();
        mHolder.addCallback(this);
        /* Deprecated setting, but required on Android versions prior to 3.0 */
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
//        Log.d("Camera", "Vybiram nejlepsi pro rozmery w*h = "+w+"*"+h);
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }
//        Log.d("Camera", "nejlepsi je "+optimalSize.width+"*"+optimalSize.height);
        return optimalSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
//        parameters.setPreviewSize(parameters.getPictureSize().width / mPreviewSize.width,
//                parameters.getPictureSize().height / mPreviewSize.height);
        mCamera.setParameters(parameters);

        ViewGroup.LayoutParams params = this.getLayoutParams();
        double min = Math.min(
                (double) getWidth() / mPreviewSize.width,
                (double) getHeight() / mPreviewSize.height);
        params.width = (int)(mPreviewSize.width * min);
        params.height = (int)(mPreviewSize.height * min);
        this.setLayoutParams(params);
        /* Let the parent activity rearrange the views */
        if (mListener != null) {
            mListener.onPreviewSizeChange(params.width, params.height);
        }

        /* Start preview with new settings */
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mHolder.removeCallback(this);
        mCamera.release();
        mCamera = null;
    }

    /**
     *  Take a picture and and convert it from bytes[] to Bitmap.
     */
    public void takeAPicture() {
        TakePictureTask takePictureTask = new TakePictureTask();
        takePictureTask.execute();
    }

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            /* Release the previous picture */
            if (mBitmap != null) {
                mBitmap.recycle();
            }
            /* Retrieve the new picture */
            mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);

            Log.d(TAG, "foto original ma velikost " + mBitmap.getWidth() + " x " + mBitmap.getHeight());
            mBitmap = Bitmap.createScaledBitmap(mBitmap, mPreviewSize.width, mPreviewSize.height, true);
            Log.d(TAG, "foto zmenena ma velikost " + mBitmap.getWidth() + " x " + mBitmap.getHeight());

            /* Allows to see the preview again */
            mCamera.stopPreview();
            mCamera.startPreview();

            /* Let the parent know about the new prepared picture */
            if (mListener != null) {
                mListener.onImageReady();
            }

            String root = Environment.getExternalStorageDirectory().toString();
            File myDir = new File(root + "/ohunt_camera");
            if(!myDir.exists()) {
                myDir.mkdir();
            }
            File pictureFile = new File(myDir, System.currentTimeMillis()+".jpg");
            if (pictureFile.exists()) {
                pictureFile.delete();
            }
            Log.d(TAG, "ukladam do "+pictureFile.getAbsolutePath()+", canonical: "+pictureFile.getPath());

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    private class TakePictureTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected Void doInBackground(Void... params) {
            mCamera.takePicture(null, null, mPictureCallback);

            return null;
        }

    }

    public interface OnCameraActionListener {
        void onPreviewSizeChange(int width, int height);
        void onImageReady();
    }
}
