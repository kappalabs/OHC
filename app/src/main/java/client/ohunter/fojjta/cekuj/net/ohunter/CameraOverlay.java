package client.ohunter.fojjta.cekuj.net.ohunter;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class CameraOverlay extends SurfaceView implements SurfaceHolder.Callback {

    public static Bitmap mBitmap;
    SurfaceHolder mHolder;
    static Camera mCamera;

    public static final String TAG = "CameraOverlay";

    public CameraOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);


        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {

            //Setting the camera's aspect ratio
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            Camera.Size optimalSize = getOptimalPreviewSize(sizes,
                    getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
//            Camera.Size optimalSize = sizes.get(0);
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            mCamera.setParameters(parameters);

            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private Camera.Size optimalSize;
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//
//        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
//        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
//        setMeasuredDimension(width, height);
//
//        Camera.Parameters parameters = mCamera.getParameters();
//        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
//        if (sizes != null) {
//            optimalSize = getOptimalPreviewSize(sizes, width, height);
//        }
//    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        Log.d("Camera", "Vybiram nejlepsi pro rozmery w*h = "+w+"*"+h);
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        Log.d("Camera", "nejlepsi je "+optimalSize.width+"*"+optimalSize.height);
        return optimalSize;
    }

    public Camera.Parameters getParameters() {
        if (mCamera != null) {
            return mCamera.getParameters();
        }
        return null;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
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
        parameters.getSupportedPreviewSizes();
        mCamera.setParameters(parameters);
        mCamera.startPreview();

        // start preview with new settings
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

    /***
     *
     *  Take a picture and and convert it from bytes[] to Bitmap.
     *
     */
    public static void takeAPicture() {
        Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            }
        };
        mCamera.takePicture(null, null, mPictureCallback);

        mCamera.stopPreview();
        mCamera.startPreview();
    }

//    private Bitmap bitmap_;
//
//    public Bitmap takeAPicture() {
//        Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
//            @Override
//            public void onPictureTaken(byte[] data, Camera camera) {
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                bitmap_ = BitmapFactory.decodeByteArray(data, 0, data.length, options);
//            }
//        };
//        mCamera.takePicture(null, null, mPictureCallback);
//
//        mCamera.stopPreview();
//        mCamera.startPreview();
//        return bitmap_;
//    }
}
