package com.kappa_labs.ohunter.client.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
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
import com.kappa_labs.ohunter.lib.net.Request;
import com.kappa_labs.ohunter.lib.requests.CompareRequest;

import java.io.ByteArrayOutputStream;

/**
 * Full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * Provides taking photos of targets, camera preview is shown on the screen.
 */
public class CameraActivity extends AppCompatActivity implements Utils.OnEdgesTaskCompleted, CameraOverlay.OnCameraActionListener {

    public static final String TAG = "CameraActivity";

    public static final int DEFAULT_ALPHA = 100;
    public static final int DEFAULT_NUM_ATTEMPTS = 3;
    public static final int DEFAULT_MIN_ATTEMPTS = 1;

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
    private static int rightRotations;

    private int numberOfAttempts = 0;
    private double vLimit, hLimit;
    private boolean photosTaken, photosEvaluated;

    @SuppressWarnings("deprecation")
    private Camera mCamera;
    private CameraOverlay mPreview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

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
                matrix.postRotate(-90);

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
                matrix.postRotate(90);

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
        Utils.CountEdgesTask edgesTask = Utils.getInstance().
                new CountEdgesTask(this, Wizard.getStandardProgressDialog(this,
                getString(R.string.executing_task),
                getString(R.string.waiting_for_result)), referenceImage);
        edgesTask.execute();

        /* Create an instance of Camera, our Preview view and set it as the content of our activity */
        mCamera = getCameraInstance();
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
    private static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
            /* Camera is not available (in use or does not exist) */
        }
        return c;
    }

    private Request makeCompareRequest() {
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
        photo1.reference = mTarget.getSelectedPhoto().reference;

        /* Similar photos should be stored by now */
        Photo[] similar = SharedDataManager.getPhotosOfPlace(this, mTarget.getPlaceID());
        if (similar.length == 0) {
            Log.e(TAG, "Making request, but no photo is stored/prepared!");
            Toast.makeText(this, getString(R.string.error_camera_no_photo), Toast.LENGTH_SHORT).show();
        }

        return new CompareRequest(player, photo1, similar);
    }

    private void storeRequestForEvaluation(Request request) {
        /* Store the photos for later use */
        if (SharedDataManager.setCompareRequestForTarget(CameraActivity.this, request, mTarget.getPlaceID())) {
            SharedDataManager.clearPhotosOfTarget(CameraActivity.this, mTarget.getPlaceID());
            finish();
        } else {
            Log.e(TAG, "cannot write the compare request");
            Toast.makeText(CameraActivity.this,
                    getString(R.string.cannot_save_request), Toast.LENGTH_SHORT).show();
        }
    }

    private void showStoreForEvaluationDialog(final Request request) {
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
        /* Rotate the photo the way reference photo is rotated */
        Matrix matrix = new Matrix();
        matrix.postRotate(-90 * rightRotations);
        Bitmap cropped = Bitmap.createBitmap(photo, (int) (hLimit / 2), (int) (vLimit / 2),
                photo.getWidth() - (int) hLimit, photo.getHeight() - (int) vLimit, matrix, true);
        CameraOverlay.mBitmap.recycle();
        CameraOverlay.mBitmap = null;
        return cropped;
    }

    @Override
    public void onImageReady() {
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
//            uploadFab.show();
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
        if (referenceImage != null) {
            referenceImage.recycle();
            referenceImage = null;
        }
        if (edgesImage != null) {
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
    public static void initCamera(Target target) {
        rightRotations = 0;
        mTarget = target;
        setTemplateImage(Utils.toBitmap(target.getSelectedPhoto().sImage));
    }

    private static void setTemplateImage(Bitmap templateImage) {
        if (referenceImage != null) {
            referenceImage.recycle();
        }
        if (edgesImage != null) {
            edgesImage.recycle();
            edgesImage = null;
        }
        referenceImage = templateImage;
    }

    @Override
    public void onEdgesTaskCompleted(Bitmap edges) {
        edgesImage = edges;
        templateImageView.setImageBitmap(edgesImage);
        templateImageView.setAlpha(DEFAULT_ALPHA / 100.f);
        updateLimits();
    }

}
