package client.ohunter.fojjta.cekuj.net.ohunter;


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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraOverlay extends SurfaceView implements SurfaceHolder.Callback {

    public static final String TAG = "CameraOverlay";

    public Bitmap mBitmap;
    SurfaceHolder mHolder;
    static Camera mCamera;



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

    /**
     *  Take a picture and and convert it from bytes[] to Bitmap.
     */
    public void takeAPicture() {
//        mCamera.takePicture(null, null, mPictureCallback);
        TakePictureTask takePictureTask = new TakePictureTask();
        takePictureTask.execute();

//        mCamera.stopPreview();
//        mCamera.startPreview();
    }

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);

//            Log.d(TAG, "v onpicturetaken pred filem");
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
            // This returns the preview back to the live camera feed
            mCamera.stopPreview();
            mCamera.startPreview();
        }

        @Override
        protected Void doInBackground(Void... params) {
            mCamera.takePicture(null, null, mPictureCallback);

//            try {
//                Thread.sleep(1500); // few seconds preview
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }

            return null;
        }

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
