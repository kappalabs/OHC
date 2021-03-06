package com.kappa_labs.ohunter.client.views;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

/**
 * Class for camera preview.
 */
public class CameraOverlay extends SurfaceView implements SurfaceHolder.Callback {

    public static final String TAG = "CameraOverlay";

    private OnCameraActionListener mListener;
    public static Bitmap mBitmap;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;


    /**
     * Creates a new custom camera preview surface.
     *
     * @param context Context of the parent.
     * @param camera Opened camera object.
     */
    public CameraOverlay(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        init(context);
    }

    /**
     * Creates a new custom camera preview surface.
     *
     * @param context Context of the parent.
     * @param attrs Attributes for the surface view.
     */
    public CameraOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Creates a new custom camera preview surface.
     *
     * @param context Context of the parent.
     * @param attrs Attributes for the surface view.
     * @param defStyle Style for the surface view.
     */
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
        if (mCamera == null) {
            Log.e(TAG, "surfaceCreated: Camera is unavailable!");
            return;
        }
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
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
        if (mHolder.getSurface() == null) {
            /* Preview surface does not exist */
            return;
        }

        /* Stop preview before making changes */
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            /* ignore: tried to stop a non-existent preview */
        }

        /* Set preview size and make any resize, rotate or reformatting changes here */
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
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
     * Take a picture and and convert it from bytes[] to Bitmap.
     */
    public void takeAPicture() {
        TakePictureTask takePictureTask = new TakePictureTask();
        takePictureTask.execute();
    }

    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            /* Release the previous picture */
            if (mBitmap != null) {
                mBitmap.recycle();
                mBitmap = null;
            }
            /* Retrieve the new picture */
            mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            /* Scale the picture to match the sizes of the preview/template one */
            mBitmap = Bitmap.createScaledBitmap(mBitmap, mPreviewSize.width, mPreviewSize.height, true);

            /* Allows to see the preview again */
            mCamera.stopPreview();
            mCamera.startPreview();

            /* Let the parent know about the new prepared picture */
            if (mListener != null) {
                mListener.onImageReady();
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

    /**
     * Interface for listener on this camera preview.
     */
    public interface OnCameraActionListener {
        /**
         * Called when the size of this view changes.
         *
         * @param width New width of this view.
         * @param height New height of this view.
         */
        void onPreviewSizeChange(int width, int height);

        /**
         * Called when the image was taken and is ready to use.
         */
        void onImageReady();
    }

}
