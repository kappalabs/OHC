package client.ohunter.fojjta.cekuj.net.ohunter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.entities.SImage;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.CompareRequest;
import com.kappa_labs.ohunter.lib.requests.Request;

import java.io.ByteArrayOutputStream;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CameraActivity extends AppCompatActivity implements Utils.OnEdgesTaskCompleted, CameraOverlay.OnCameraActionListener, Utils.OnResponseTaskCompleted {

    public static final String TAG = "CameraActivity";

    public static final int DEFAULT_ALPHA = 100;
    public static final int DEFAULT_NUM_ATTEMPTS = 3;
    public static final int DEFAULT_MIN_ATTEMPTS = 1;

    private ImageView templateImageView;
    private SeekBar opacitySeekBar;
    private SeekBar colorSeekBar;
    private Button shootButton;
    private Button rotateLeftButton, rotateRightButton;
    private TextView numberOfPhotosTextview;
    private TextView scoreTextview;
    private ImageView lastPhotoImageview;
    private FloatingActionButton uploadFab;

    private static Bitmap referenceImage, edgesImage;

    private int numberOfAttempts = 0;

    private Camera mCamera;
    private CameraOverlay mPreview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        lastPhotoImageview = (ImageView) findViewById(R.id.imageView_lastPhoto);

        numberOfPhotosTextview = (TextView) findViewById(R.id.textView_numberOfPhotos);
        numberOfPhotosTextview.setText(DEFAULT_NUM_ATTEMPTS + getString(R.string.number_sign));

        scoreTextview = (TextView) findViewById(R.id.textView_score);
        scoreTextview.setVisibility(View.GONE);

        shootButton = (Button) findViewById(R.id.button_shoot);
        shootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreview.takeAPicture();
            }
        });

        rotateLeftButton = (Button) findViewById(R.id.button_rotateLeft);
        rotateLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edgesImage == null) {
                    return;
                }

                Matrix matrix = new Matrix();
                matrix.postRotate(-90);

                Bitmap rotatedBitmap = Bitmap.createBitmap(edgesImage, 0, 0,
                        edgesImage.getWidth(), edgesImage.getHeight(), matrix, true);
                edgesImage.recycle();
                edgesImage = rotatedBitmap;
                templateImageView.setImageBitmap(edgesImage);
            }
        });

        rotateRightButton = (Button) findViewById(R.id.button_rotateRight);
        rotateRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edgesImage == null) {
                    return;
                }

                Matrix matrix = new Matrix();
                matrix.postRotate(90);

                Bitmap rotatedBitmap = Bitmap.createBitmap(edgesImage, 0, 0,
                        edgesImage.getWidth(), edgesImage.getHeight(), matrix, true);
                edgesImage.recycle();
                edgesImage = rotatedBitmap;
                templateImageView.setImageBitmap(edgesImage);
            }
        });

        opacitySeekBar = (SeekBar) findViewById(R.id.seekBar_opacity);
        opacitySeekBar.setProgress(DEFAULT_ALPHA);
        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                templateImageView.setAlpha(progress / 100.f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        colorSeekBar = (SeekBar) findViewById(R.id.seekBar_color);
        colorSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                changeBitmapColor(edgesImage, templateImageView,
                        Color.HSVToColor(new float[]{(float)progress / seekBar.getMax() * 360, 1, 1}));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        uploadFab = (FloatingActionButton) findViewById(R.id.fab_upload);
        uploadFab.setVisibility(View.GONE);
        uploadFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: pripojeni k serveru, odeslani dat - pripadne offline ulozeni
                if (CameraOverlay.mBitmap != null) {
                    serverCommunication();
                }
            }
        });

        /* Show the reference photo edges on top of the photo preview */
        templateImageView = (ImageView) findViewById(R.id.imageView_template);
        Utils.CountEdgesTask edgesTask = Utils.getInstance().
                new CountEdgesTask(this, Utils.getStandardDialog(this,
                getString(R.string.executing_task),
                getString(R.string.waiting_for_result)), referenceImage);
        edgesTask.execute();

        /* Create an instance of Camera, our Preview view and set it as the content of our activity */
        mCamera = getCameraInstance();
        mPreview = new CameraOverlay(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.frameLayout_cam_preview);
        preview.addView(mPreview);
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
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c;
    }

    private void serverCommunication() {
        Player player = SharedDataManager.getPlayer(this);
        if (player == null) {
            Log.e(TAG, "serverCommunication(): Player is null in CameraActivity!");
            Toast.makeText(this, getString(R.string.error_camera_player_null), Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap b;
        /* First photo */
        b = referenceImage;
        Photo photo1 = new Photo();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        photo1.image = new SImage(stream.toByteArray(), b.getWidth(), b.getHeight());

        /* Second photo */
        b = CameraOverlay.mBitmap;
        Photo photo2 = new Photo();
        stream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        photo2.image = new SImage(stream.toByteArray(), b.getWidth(), b.getHeight());

        Request request = new CompareRequest(player, photo1, photo2);

        /* Asynchronously execute and wait for callback when result ready*/
        Utils.RetrieveResponseTask responseTask = Utils.getInstance().
                new RetrieveResponseTask(this, Utils.getServerCommunicationDialog(this));
        responseTask.execute(request);
    }

    @Override
    public void onResponseTaskCompleted(Response response, OHException ohex) {
        /* Problem on server side */
        if (ohex != null) {
            Toast.makeText(CameraActivity.this, getString(R.string.recieved_ohex) + " " + ohex,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, getString(R.string.recieved_ohex) + ohex);
            return;
        }
        /* Problem on client side */
        if (response == null) {
            Log.e(TAG, "Problem on client side -> cannot lock the Place yet...");
            Toast.makeText(CameraActivity.this, getString(R.string.server_unreachable_error),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        /* Success */
        // TODO: jak nalozit s vysledkem?
        Toast.makeText(CameraActivity.this,
                getString(R.string.similarity_is) + " " + response.similarity, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "response similarity: " + response.similarity);
        scoreTextview.setText(String.format("%.1f%%", response.similarity * 100));
        scoreTextview.setVisibility(View.VISIBLE);
    }

//    @Override
//    protected void onPostCreate(Bundle savedInstanceState) {
//        super.onPostCreate(savedInstanceState);
//
//        if (mPreview != null) {
//            Camera.Parameters pars = mPreview.getParameters();
//
//            Log.d("Camera", "preview size: "+camRelativeLayout.getHeight()+"x"+camRelativeLayout.getWidth());
//
//            if (pars == null) {
//                return;
//            }
//            List<Camera.Size> ls = pars.getSupportedPictureSizes();
//            for (Camera.Size s : ls) {
//                Log.d("Camera", "size: " + s.height + "x" + s.width);
//            }
//        }
//    }

    @Override
    public void onImageReady() {
        lastPhotoImageview.setImageBitmap(CameraOverlay.mBitmap);
        ++numberOfAttempts;
        numberOfPhotosTextview.setText((DEFAULT_NUM_ATTEMPTS - numberOfAttempts) + getString(R.string.number_sign));
        if (numberOfAttempts >= DEFAULT_MIN_ATTEMPTS) {
            // TODO: zobrazovat fab uz tady?
            uploadFab.show();
        }
        if (numberOfAttempts >= DEFAULT_NUM_ATTEMPTS) {
            shootButton.setEnabled(false);
            uploadFab.show();
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
                preview.removeAllViews();
                preview.addView(mPreview);
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        /* Allow to release the camera */
        if (mCamera != null) {
            mCamera = null;
        }
    }

    public static void setTemplateImage(Bitmap templateImage) {
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
    }

}
