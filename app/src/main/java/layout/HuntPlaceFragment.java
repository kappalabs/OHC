package layout;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kappa_labs.ohunter.client.PageChangeAdapter;
import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.Utils;
import com.kappa_labs.ohunter.client.entities.PlaceInfo;
import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.entities.Place;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * {@link Fragment} subclass to show information about selected target.
 * Use the {@link HuntPlaceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HuntPlaceFragment extends Fragment implements PageChangeAdapter {

    private static int numberOfPhotos;
    private static String[] daytimeTexts;
    private static Bitmap[] photoBitmaps;
    private static List<PlaceInfo> infoList;
    private static double maxHeightRatio;
    private static boolean dataInvalidated = true;

    private int selectedPhotoIndex;

    private LinearLayout mInfoContainerView;
    private SeekBar mPhotoSeekBar;
    private ImageView mPhotoImageView;
    private TextView mPhotoInfoTextView;
    private TextView mPhotoCounterTextView;


    public HuntPlaceFragment() {
        /* Required empty public constructor */
    }

    /**
     * Create a new instance of this fragment.
     *
     * @return A new instance of this fragment.
     */
    public static HuntPlaceFragment newInstance() {
        return new HuntPlaceFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /* Inflate the layout for this fragment */
        final View view = inflater.inflate(R.layout.fragment_hunt_place, container, false);

        mPhotoImageView = (ImageView) view.findViewById(R.id.imageView_photo);
        mPhotoSeekBar = (SeekBar) view.findViewById(R.id.seekBar_photo);
        mInfoContainerView = (LinearLayout) view.findViewById(R.id.linearLayout_place_info);
        mPhotoCounterTextView = (TextView) view.findViewById(R.id.textView_photo_counter);
        mPhotoInfoTextView = (TextView) view.findViewById(R.id.textView_photo_info);

        mPhotoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                /* Add currently selected picture */
                int pos = getSelectedPicturePosition();
                if (pos < 0) {
                    return;
                }
                selectedPhotoIndex = pos;
                mPhotoImageView.setImageBitmap(getBitmapAt(pos));
                mPhotoInfoTextView.setText(daytimeTexts[pos]);
                mPhotoCounterTextView.setText(String.format(
                        getResources().getString(R.string.place_fragment_photo_counter),
                        pos + 1, numberOfPhotos));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        /* Wait for the view to draw, then use its size to update the information and display photo */
        ViewTreeObserver observer = view.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                dataInvalidated = true;
                update();
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        return view;
    }

    @Override
    public void onPageSelected() {
        if (dataInvalidated && mPhotoImageView != null) {
            mPhotoImageView.setImageDrawable(null);
            update();
        }
    }

    /**
     * Get the position of selected photo for active target.
     *
     * @return The position of selected photo for active target.
     */
    private int getSelectedPicturePosition() {
        if (mPhotoSeekBar == null || numberOfPhotos <= 0) {
            return -1;
        }
        int wid = mPhotoSeekBar.getMax();
        float step = wid / numberOfPhotos;
        int index = (int)Math.floor(mPhotoSeekBar.getProgress() / step);
        return Math.min(Math.max(index, 0), numberOfPhotos - 1);
    }

    /**
     * Gets the reference to the bitmap for active target at given selected position.
     *
     * @param position Position of the requested bitmap.
     * @return The reference to requested bitmap for active place.
     */
    public Bitmap getBitmapAt(int position) {
        if (position >= 0 && position < numberOfPhotos && photoBitmaps != null) {
            return photoBitmaps[position];
        }
        return null;
    }

    /**
     * Gets the reference to selected bitmap for active target.
     *
     * @return The reference to selected bitmap for active target.
     */
    public Bitmap getSelectedBitmap() {
        return getBitmapAt(selectedPhotoIndex);
    }

    /**
     * Update information on this fragment if it's invalidated (i.e. after calling changePlace()).
     */
    public void update() {
        if (!dataInvalidated) {
            return;
        }

        /* Set size of the image view */
        if (mPhotoImageView != null) {
            mPhotoImageView.getLayoutParams().height = (int) (maxHeightRatio * mPhotoImageView.getWidth());
            mPhotoImageView.requestLayout();
        }

        /* Add text information about the target */
        mInfoContainerView.removeAllViews();
        if (infoList != null) {
            for (PlaceInfo info : infoList) {
                /* Do not show empty paragraphs */
                if (info.isEmpty()) {
                    continue;
                }

                /* Add information as view */
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 6, 0, 0);
                row.setLayoutParams(params);

                /* Title of the paragraph */
                TextView title = new TextView(getContext());
                title.setText(info.getTitle());
                title.setTypeface(null, Typeface.BOLD);
                row.addView(title);

                /* Content of the paragraph */
                TextView content = new TextView(getContext());
                content.setText(info.getContent());
                /* Changes style according to type of information */
                if (info.getType() == PlaceInfo.InfoType.URL) {
                    Linkify.addLinks(content, Linkify.WEB_URLS);
                }
                row.addView(content);

                mInfoContainerView.addView(row);
            }
        }

        /* Show selected photo */
        if (numberOfPhotos > 0) {
            /* If no image is visible, add the first one and reset the progress bar position to 0 */
            if (mPhotoImageView != null && mPhotoImageView.getDrawable() == null) {
                mPhotoImageView.setScaleType(ImageView.ScaleType.FIT_START);
                mPhotoImageView.setImageBitmap(photoBitmaps[0]);
                mPhotoSeekBar.setProgress(0);
                mPhotoInfoTextView.setText(daytimeTexts[0]);
                mPhotoCounterTextView.setText(String.format(
                        getResources().getString(R.string.place_fragment_photo_counter),
                        1, numberOfPhotos));
            }
        }

        dataInvalidated = false;
    }

    /**
     * Sets information on this fragment to match given place. Passing null will clear the page.
     * Invalidates the fragment data, to draw changes, call update().
     *
     * @param context Context of the caller.
     * @param place Information from this place object will be used.
     */
    public static void changePlace(Context context, Place place) {
        maxHeightRatio = 0;
        numberOfPhotos = 0;
        photoBitmaps = null;
        daytimeTexts = null;

        if (place != null) {
            numberOfPhotos = place.getNumberOfPhotos();
            photoBitmaps = new Bitmap[numberOfPhotos];
            daytimeTexts = new String[numberOfPhotos];

            /* Initialize data for photos */
            for (int i = 0; i < numberOfPhotos; i++) {
                Photo photo = place.getPhoto(i);
                //TODO: proc je tady nekdy photo.simage null?
                photoBitmaps[i] = Utils.toBitmap(photo.sImage);
                daytimeTexts[i] = Utils.daytimeToString(context, place.getPhoto(i).daytime);

                /* Find maximum height ratio */
                final double rat =  (double) photo.getHeight() / photo.getWidth();
                if (rat > maxHeightRatio) {
                    maxHeightRatio = rat;
                }
            }

            /* Initialize data for place */
            infoList = new ArrayList<>();
            Set<String> keySet = place.getGfields().keySet();
            for (String key : keySet) {
                PlaceInfo info = PlaceInfo.buildPlaceInfo(context, key, place.getGField(key));
                infoList.add(info);
            }
            Collections.sort(infoList);
        }

        dataInvalidated = true;
    }

}
