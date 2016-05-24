package com.kappa_labs.ohunter.client.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.client.utilities.SharedDataManager;
import com.kappa_labs.ohunter.client.utilities.Utils;
import com.kappa_labs.ohunter.client.utilities.Wizard;
import com.kappa_labs.ohunter.client.views.CameraOverlay;
import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.entities.SImage;
import com.kappa_labs.ohunter.lib.requests.CompareRequest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * Provides taking photos of targets, camera preview is shown on the screen.
 */
public class CameraActivity extends AppCompatActivity implements Utils.CountEdgesTask.OnEdgesTaskCompleted, CameraOverlay.OnCameraActionListener {

    public static final String TAG = "CameraActivity";

    public static final int DEFAULT_ALPHA = 100;
    public static final int DEFAULT_NUM_ATTEMPTS = 3;
    public static final int DEFAULT_MIN_ATTEMPTS = 1;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 0x01;
    private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 0x02;

    public static final String PHOTOS_TAKEN_KEY = "photos_taken";
    public static final String PHOTOS_EVALUATED_KEY = "photos_evaluated";

    private ImageView templateImageView;
    private Button shootButton;
    private TextView numberOfPhotosTextView;
    private ImageView lastPhotoImageView;
    private FrameLayout previewFrameLayout;
    private FrameLayout limiterTop, limiterLeft, limiterBottom, limiterRight;

    private static Bitmap referenceImage, edgesImage;
    private static Target mTarget;
    private static String mPhotoReference;
    private static int rightRotations;

    private int numberOfAttempts = 0;
    private boolean photosTaken, photosEvaluated;

    @SuppressWarnings("deprecation")
    private Camera mCamera;
    private CameraOverlay mPreview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        /* Request required permissions on Marshmallow */
        if (!checkPermissionForCamera()) {
            requestPermissionForCamera();
        }
        if (SharedDataManager.storePhotosExternally(this)) {
            if (!checkPermissionForExternalStorage()) {
                requestPermissionForExternalStorage();
            }
        }

        lastPhotoImageView = (ImageView) findViewById(R.id.imageView_lastPhoto);

        limiterBottom = (FrameLayout) findViewById(R.id.limiter_bottom);
        limiterLeft = (FrameLayout) findViewById(R.id.limiter_left);
        limiterTop = (FrameLayout) findViewById(R.id.limiter_top);
        limiterRight = (FrameLayout) findViewById(R.id.limiter_right);

        numberOfPhotosTextView = (TextView) findViewById(R.id.textView_numberOfPhotos);
        numberOfPhotosTextView.setText(
                String.format(getResources().getString(R.string.camera_activity_photos_counter),
                DEFAULT_NUM_ATTEMPTS, getString(R.string.number_sign)));

        shootButton = (Button) findViewById(R.id.button_shoot);
        shootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreview.takeAPicture();
            }
        });

        Button rotateLeftButton = (Button) findViewById(R.id.button_rotateLeft);
        assert rotateLeftButton != null;
        rotateLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edgesImage == null) {
                    return;
                }

                rightRotations = (rightRotations + 3) % 4;

                Matrix matrix = new Matrix();
                matrix.postRotate(90);

                Bitmap rotatedBitmap = Bitmap.createBitmap(edgesImage, 0, 0,
                        edgesImage.getWidth(), edgesImage.getHeight(), matrix, true);
                edgesImage.recycle();
                edgesImage = rotatedBitmap;
                templateImageView.setImageBitmap(edgesImage);

                updateLimits();
            }
        });

        Button rotateRightButton = (Button) findViewById(R.id.button_rotateRight);
        assert rotateRightButton != null;
        rotateRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edgesImage == null) {
                    return;
                }

                rightRotations = (rightRotations + 1) % 4;

                Matrix matrix = new Matrix();
                matrix.postRotate(-90);

                Bitmap rotatedBitmap = Bitmap.createBitmap(edgesImage, 0, 0,
                        edgesImage.getWidth(), edgesImage.getHeight(), matrix, true);
                edgesImage.recycle();
                edgesImage = rotatedBitmap;
                templateImageView.setImageBitmap(edgesImage);

                updateLimits();
            }
        });

        SeekBar opacitySeekBar = (SeekBar) findViewById(R.id.seekBar_opacity);
        assert opacitySeekBar != null;
        opacitySeekBar.setProgress(DEFAULT_ALPHA);
        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                templateImageView.setAlpha(progress / 100.f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar colorSeekBar = (SeekBar) findViewById(R.id.seekBar_color);
        assert colorSeekBar != null;
        colorSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (edgesImage == null) {
                    return;
                }
                changeBitmapColor(edgesImage, templateImageView,
                        Color.HSVToColor(new float[]{(float) progress / seekBar.getMax() * 360, 1, 1}));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        /* Show the reference photo edges on top of the photo preview */
        templateImageView = (ImageView) findViewById(R.id.imageView_template);
        Utils.CountEdgesTask edgesTask = new Utils.CountEdgesTask(this, Wizard.getStandardProgressDialog(this,
                getString(R.string.executing_task),
                getString(R.string.waiting_for_result)), referenceImage);
        edgesTask.execute();

        /* Create an instance of Camera, our Preview view and set it as the content of our activity */
        mCamera = getCameraInstance();
        if (mCamera == null) {
            finish();
            return;
        }
        mPreview = new CameraOverlay(this, mCamera);
        previewFrameLayout = (FrameLayout) findViewById(R.id.frameLayout_cam_preview);
        assert previewFrameLayout != null;
        previewFrameLayout.addView(mPreview);
    }

    private void changeBitmapColor(Bitmap sourceBitmap, ImageView image, int color) {
        Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0,
                sourceBitmap.getWidth(), sourceBitmap.getHeight());
        Paint p = new Paint();
        ColorFilter filter = new LightingColorFilter(color, 0);
        p.setColorFilter(filter);
        image.setImageBitmap(resultBitmap);

        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(resultBitmap, 0, 0, p);
    }

    /**
     * A safe way to get an instance of the Camera object.
     *
     * @return Camera object if available, null if camera is unavailable.
     */
    @SuppressWarnings("deprecation")
    private static Camera getCameraInstance() {
        Camera cam = null;
        int cameraCount = Camera.getNumberOfCameras();
        for (int i = 0; i < cameraCount; i++) {
            if (cam == null) {
                try {
                    cam = Camera.open(i);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }
        return cam;
    }

    private boolean checkPermissionForCamera(){
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionForCamera() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)){
            Toast.makeText(this, "Camera permission needed. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    private boolean checkPermissionForExternalStorage() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermissionForExternalStorage(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            Toast.makeText(this, "External Storage permission needed. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
        }
    }

    private CompareRequest makeCompareRequest() {
        Player player = SharedDataManager.getPlayer(this);
        if (player == null) {
            Log.e(TAG, "makeCompareRequest(): Player is null in CameraActivity!");
            Toast.makeText(this, getString(R.string.error_camera_player_null), Toast.LENGTH_SHORT).show();
            return null;
        }

        /* Reference photo */
        Bitmap b = referenceImage;
        Photo photo1 = new Photo();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        photo1.sImage = new SImage(stream.toByteArray(), b.getWidth(), b.getHeight());
        photo1.reference = mPhotoReference;

        /* Similar photos should be stored by now */
        Photo[] similar = SharedDataManager.getPhotosOfTarget(this, mTarget.getPlaceID());
        if (similar.length == 0) {
            Log.e(TAG, "Making request, but no photo is stored/prepared!");
            Toast.makeText(this, getString(R.string.error_camera_no_photo), Toast.LENGTH_SHORT).show();
        }

        return new CompareRequest(player, photo1, similar);
    }

    private void storeRequestForEvaluation(CompareRequest request) {
        /* Store the photos for later use */
        if (SharedDataManager.setRequestForTarget(this, request, mTarget.getPlaceID())) {
            int huntNumber = SharedDataManager.getHuntNumber(this);
            mTarget.setHuntNumber(huntNumber);
            mTarget.removePhotos();
            mTarget.addPhotos(Arrays.asList(request.getSimilarPhotos()));
            SharedDataManager.addRequestToHistory(this, mTarget.getPlaceID(), request);
            SharedDataManager.clearPhotosOfTarget(this, mTarget.getPlaceID());
            finish();
        } else {
            Log.e(TAG, "cannot write the compare request");
            Toast.makeText(this, getString(R.string.cannot_save_request), Toast.LENGTH_SHORT).show();
        }
    }

    private void showStoreForEvaluationDialog(final CompareRequest request) {
        Wizard.storeForEvaluationDialog(CameraActivity.this, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        storeRequestForEvaluation(request);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /* User can for example take another photo */
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        photosTaken = false;
                        photosEvaluated = false;
                        SharedDataManager.clearPhotosOfTarget(CameraActivity.this, mTarget.getPlaceID());
                        finish();
                    }
                }
        );
    }

    @Override
    public void onPreviewSizeChange(int width, int height) {
        ViewGroup.LayoutParams params = previewFrameLayout.getLayoutParams();
        params.width = width;
        params.height = height;
        previewFrameLayout.setLayoutParams(params);

        params = templateImageView.getLayoutParams();
        params.width = width;
        params.height = height;
        templateImageView.setLayoutParams(params);

        /* Adjust limits */
        updateLimits();
    }

    private void updateLimits() {
        if (previewFrameLayout == null || edgesImage == null) {
            return;
        }

        /* Count the limits */
        int width = previewFrameLayout.getWidth();
        int height = previewFrameLayout.getHeight();
        double wRatio = (double) edgesImage.getWidth() / width;
        double hRatio = (double) edgesImage.getHeight() / height;
        double hLimit, vLimit;
        if (wRatio > hRatio) {
            /* Show the vertical stripes, hide horizontals */
            vLimit = height - edgesImage.getHeight() / wRatio;
            hLimit = 0;
        } else {
            /* Show the horizontal stripes, hide verticals */
            hLimit = width - edgesImage.getWidth() / hRatio;
            vLimit = 0;
        }

        ViewGroup.LayoutParams params;
        /* Draw the horizontal stripes */
        params = limiterLeft.getLayoutParams();
        params.width = (int) (hLimit / 2);
        limiterLeft.setLayoutParams(params);
        params = limiterRight.getLayoutParams();
        params.width = (int) (hLimit / 2);
        limiterRight.setLayoutParams(params);
        /* Draw the vertical stripes */
        params = limiterTop.getLayoutParams();
        params.height = (int) (vLimit / 2);
        limiterTop.setLayoutParams(params);
        params = limiterBottom.getLayoutParams();
        params.height = (int) (vLimit / 2);
        limiterBottom.setLayoutParams(params);
    }

    private Bitmap getCroppedCameraPicture() {
        Bitmap photo = CameraOverlay.mBitmap;
        if (photo == null) {
            return null;
        }
        updateLimits();
        /* Rotate the photo the way reference photo is rotated */
        Matrix matrix = new Matrix();
        matrix.postRotate(90 * rightRotations);
        /* Count the limits for this photo */
        double wRatio = (double) edgesImage.getWidth() / photo.getWidth();
        double hRatio = (double) edgesImage.getHeight() / photo.getHeight();
        double horizontalLimit, verticalLimit;
        if (wRatio > hRatio) {
            /* Show the vertical stripes, hide horizontals */
            verticalLimit = photo.getHeight() - edgesImage.getHeight() / wRatio;
            horizontalLimit = 0;
        } else {
            /* Show the horizontal stripes, hide verticals */
            horizontalLimit = photo.getWidth() - edgesImage.getWidth() / hRatio;
            verticalLimit = 0;
        }
        Bitmap cropped = Bitmap.createBitmap(
                photo,
                (int) (horizontalLimit / 2), (int) (verticalLimit / 2),
                (int) (photo.getWidth() - horizontalLimit), (int) (photo.getHeight() - verticalLimit),
                matrix,
                true
        );
        CameraOverlay.mBitmap.recycle();
        CameraOverlay.mBitmap = null;
        return cropped;
    }

    @Override
    public void onImageReady() {
        /* Save the photo locally so that user can see the target in gallery */
        if (SharedDataManager.storePhotosExternally(this)) {
            FileOutputStream out = null;
            try {
                File sd = Environment.getExternalStorageDirectory();
                File sdDir = new File(sd.getAbsolutePath() + "/ohunter/photos");
                //noinspection ResultOfMethodCallIgnored
                sdDir.mkdirs();
                File bmpFile = new File(sdDir, System.currentTimeMillis() + ".jpg");
                out = new FileOutputStream(bmpFile);
                CameraOverlay.mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                Log.d(TAG, "Picture saved to gallery: " + bmpFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /* Store the new photo */
        Bitmap picture = getCroppedCameraPicture();
        if (picture == null) {
            return;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        picture.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        Photo photo = new Photo();
        photo.sImage = new SImage(stream.toByteArray(), picture.getWidth(), picture.getHeight());
        SharedDataManager.addPhotoOfTarget(this, mTarget.getPlaceID(), photo, ((Long) System.currentTimeMillis()).toString());

        /* Update UI information */
        lastPhotoImageView.setImageBitmap(picture);
        ++numberOfAttempts;
        numberOfPhotosTextView.setText(
                String.format(getResources().getString(R.string.camera_activity_photos_counter),
                        (DEFAULT_NUM_ATTEMPTS - numberOfAttempts), getString(R.string.number_sign)));
        if (numberOfAttempts >= DEFAULT_MIN_ATTEMPTS) {
            photosTaken = true;
        }
        if (numberOfAttempts >= DEFAULT_NUM_ATTEMPTS) {
            shootButton.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        /* Prepare new camera preview */
        if (mCamera == null) {
            try {
                mCamera = getCameraInstance();
                mCamera.setPreviewCallback(null);
                mPreview = new CameraOverlay(this, mCamera);
                FrameLayout preview = (FrameLayout) findViewById(R.id.frameLayout_cam_preview);
                assert preview != null;
                preview.removeAllViews();
                preview.addView(mPreview);
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (photosTaken && !photosEvaluated) {
            showStoreForEvaluationDialog(makeCompareRequest());
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        data.putExtra(PHOTOS_TAKEN_KEY, photosTaken);
        data.putExtra(PHOTOS_EVALUATED_KEY, photosEvaluated);
        setResult(RESULT_OK, data);

        /* Release static references */
        mTarget = null;
        if (referenceImage != null && !referenceImage.isRecycled()) {
            referenceImage.recycle();
            referenceImage = null;
        }
        if (edgesImage != null && !edgesImage.isRecycled()) {
            edgesImage.recycle();
            edgesImage = null;
        }

        super.finish();
    }

    @Override
    protected void onPause() {
        super.onPause();

        /* Allow to release the camera */
        if (mCamera != null) {
            mCamera = null;
        }
    }

    /**
     * Sets the target that will be photographed. Should be called before starting this activity.
     *
     * @param target Target which will be photographed.
     */
    public static void initCamera(Target target, String reference) {
        rightRotations = 0;
        mTarget = target;
        mPhotoReference = reference;
        setTemplateImage(Utils.toBitmap(target.getSelectedPhoto().sImage));
    }

    private static void setTemplateImage(Bitmap templateImage) {
        if (referenceImage != null && !referenceImage.isRecycled()) {
            referenceImage.recycle();
        }
        if (edgesImage != null && !edgesImage.isRecycled()) {
            edgesImage.recycle();
            edgesImage = null;
        }
        referenceImage = templateImage.copy(templateImage.getConfig(), true);
    }

    @Override
    public void onEdgesTaskCompleted(Bitmap edges) {
        edgesImage = edges;
        templateImageView.setImageBitmap(edgesImage);
        templateImageView.setAlpha(DEFAULT_ALPHA / 100.f);
        updateLimits();
    }

}
