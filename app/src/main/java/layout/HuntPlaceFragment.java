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

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.adapters.PageChangeAdapter;
import com.kappa_labs.ohunter.client.entities.PlaceInfo;
import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.client.utilities.Utils;
import com.kappa_labs.ohunter.lib.entities.Photo;

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

    private static OnFragmentInteractionListener mListener;

    private static String placeID;
    private static int numberOfPhotos;
    private static String[] daytimeTexts;
    private static Bitmap[] photoBitmaps;
    private static List<PlaceInfo> infoList;
    private static double maxHeightRatio;
    private static boolean dataInvalidated = true;
    private static int selectedPhotoIndex;

    private static ImageView mPhotoImageView;
    private LinearLayout mInfoContainerView;
    private SeekBar mPhotoSeekBar;
    private TextView mPhotoInfoTextView;
    private TextView mPhotoCounterTextView;


    public HuntPlaceFragment() {
        /* Required empty public constructor */
    }

    /**
     * Initiates private fields for a new game.
     */
    public static void initNewHunt() {
        placeID = null;
        numberOfPhotos = 0;
        daytimeTexts = null;
        mPhotoImageView = null;
        recycleBitmaps();
        infoList = null;
        maxHeightRatio = 0;
        dataInvalidated = true;
        selectedPhotoIndex = -1;
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
                Bitmap bitmap = getBitmapAt(pos);
                if (bitmap != null && !bitmap.isRecycled()) {
                    mPhotoImageView.setImageBitmap(bitmap);
                }
                mPhotoInfoTextView.setText(daytimeTexts[pos]);
                mPhotoCounterTextView.setText(String.format(
                        getResources().getString(R.string.place_fragment_photo_counter),
                        pos + 1, numberOfPhotos));
                if (mListener != null) {
                    mListener.onSelectionChanged(pos);
                }
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
                mPhotoSeekBar.invalidate();
                mPhotoCounterTextView.invalidate();
                mPhotoInfoTextView.invalidate();
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

    @Override
    public void onStop() {
        super.onStop();

        initNewHunt();
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
        float step = (float) mPhotoSeekBar.getMax() / numberOfPhotos;
        int index = (int) Math.floor(mPhotoSeekBar.getProgress() / step);
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
     * Update information on this fragment if it's invalidated (i.e. after calling changeTarget()).
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
        if (numberOfPhotos > 0 && selectedPhotoIndex < numberOfPhotos) {
            /* If no image is visible, add the first one and reset the progress bar position to 0 */
            if (mPhotoImageView != null && mPhotoImageView.getDrawable() == null) {
                mPhotoImageView.setScaleType(ImageView.ScaleType.FIT_START);
                float step = (float) mPhotoSeekBar.getMax() / numberOfPhotos;
                int progress = (int) Math.floor(selectedPhotoIndex * step + step / 2);
                Bitmap selected = photoBitmaps[selectedPhotoIndex];
                if (selected != null && !selected.isRecycled()) {
                    mPhotoImageView.setImageBitmap(selected);
                }
                mPhotoSeekBar.setProgress(progress);
                mPhotoInfoTextView.setText(daytimeTexts[selectedPhotoIndex]);
                mPhotoCounterTextView.setText(String.format(
                        getResources().getString(R.string.place_fragment_photo_counter),
                        selectedPhotoIndex + 1,
                        numberOfPhotos)
                );
            }
        }

        dataInvalidated = false;
    }

    private static void recycleBitmaps() {
        if (photoBitmaps == null) {
            return;
        }
        for (int i = 0; i < photoBitmaps.length; i++) {
            if (photoBitmaps[i] != null && !photoBitmaps[i].isRecycled()) {
                photoBitmaps[i].recycle();
                photoBitmaps[i] = null;
            }
        }
        photoBitmaps = null;
    }

    /**
     * Sets information on this fragment to match given target. Passing null will clear the page.
     * Invalidates the fragment data, to draw changes (call update()).
     *
     * @param context Context of the caller.
     * @param target Information from this target object will be used.
     */
    public static void changePlace(Context context, Target target) {
        if (mPhotoImageView == null) {
            return;
        }
        mPhotoImageView.setImageDrawable(null);
        recycleBitmaps();

        /* Update information only when the target changes */
        if (target == null && placeID != null) {
            placeID = null;
            maxHeightRatio = 0;
            numberOfPhotos = 0;
            selectedPhotoIndex = 0;
            photoBitmaps = null;
            daytimeTexts = null;
        } else if (target != null/* && !Objects.equals(place.getID(), placeID)*/) {
            placeID = target.getPlaceID();
            maxHeightRatio = 0;
            numberOfPhotos = target.getNumberOfPhotos();
            selectedPhotoIndex = target.getPhotoIndex();
            photoBitmaps = new Bitmap[numberOfPhotos];
            daytimeTexts = new String[numberOfPhotos];

            /* Initialize data for photos */
            for (int i = 0; i < numberOfPhotos; i++) {
                Photo photo = target.getPhoto(i);
                if (photo == null) {
                    continue;
                }
                Utils.BitmapWorkerTask bitmapTask = new Utils.BitmapWorkerTask(new Utils.BitmapWorkerTask.OnBitmapReady() {
                    @Override
                    public void onBitmapReady(Bitmap bitmap, Object data) {
                        if (data instanceof BitmapReferencer) {
                            BitmapReferencer referencer = (BitmapReferencer) data;
                            if (referencer.bitmaps[referencer.index] != null && !referencer.bitmaps[referencer.index].isRecycled()) {
                                referencer.bitmaps[referencer.index].recycle();
                            }
                            referencer.bitmaps[referencer.index] = bitmap;
                            if (selectedPhotoIndex == referencer.index && mPhotoImageView != null) {
                                mPhotoImageView.setImageBitmap(bitmap);
                            }
                        }
                    }
                }, new BitmapReferencer(photoBitmaps, i));
                bitmapTask.execute(photo.sImage);
                daytimeTexts[i] = Utils.daytimeToString(context, photo.daytime);

                /* Find maximum height ratio */
                final double rat =  (double) photo.getHeight() / photo.getWidth();
                if (rat > maxHeightRatio) {
                    maxHeightRatio = rat;
                }
            }

            /* Initialize data about the target */
            infoList = new ArrayList<>();
            Set<String> keySet = target.getGfields().keySet();
            for (String key : keySet) {
                PlaceInfo info = PlaceInfo.buildPlaceInfo(context, key, target.getGField(key));
                infoList.add(info);
            }
            Collections.sort(infoList);
        }
        dataInvalidated = true;
    }

    private static class BitmapReferencer {
        public Bitmap[] bitmaps;
        public int index;

        public BitmapReferencer(Bitmap[] bitmaps, int index) {
            this.bitmaps = bitmaps;
            this.index = index;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }

    /**
     * Interface that parent activity must implement.
     */
    public interface OnFragmentInteractionListener {
        /**
         * Called when target's reference photo is changed.
         *
         * @param photoIndex Index of new selected reference photo.
         */
        void onSelectionChanged(int photoIndex);
    }

}
