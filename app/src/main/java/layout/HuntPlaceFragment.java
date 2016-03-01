package layout;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.entities.Place;

import java.util.Set;

import client.ohunter.fojjta.cekuj.net.ohunter.PageChangeAdapter;
import client.ohunter.fojjta.cekuj.net.ohunter.R;
import client.ohunter.fojjta.cekuj.net.ohunter.Utils;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HuntPlaceFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HuntPlaceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HuntPlaceFragment extends Fragment implements PageChangeAdapter {

    private static final String TAG = "HuntPlaceFragment";
    private static final String HEIGHT_RATIO_KEY = "HEIGHT_RATIO_KEY";

    private static Place mPlace;
    private static double maxHeightRatio;
    private static boolean placeInvalidated = true;

    private OnFragmentInteractionListener mListener;

    private LinearLayout mInfoContainerView;
    private SeekBar mPhotoSeekBar;
    private ImageView mPhotoImageView;
    private TextView mPhotoInfoTextView;
    private TextView mPhotoCounterTextView;


    public HuntPlaceFragment() { }

    /**
     * Create a new instance of this fragment.
     *
     * @return A new instance of this fragment.
     */
    public static HuntPlaceFragment newInstance() {
        return new HuntPlaceFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_hunt_place, container, false);
        mPhotoImageView = (ImageView) view.findViewById(R.id.imageView_photo);
        mPhotoSeekBar = (SeekBar) view.findViewById(R.id.seekBar_photo);
        mInfoContainerView = (LinearLayout) view.findViewById(R.id.linearLayout_place_info);
        mPhotoCounterTextView = (TextView) view.findViewById(R.id.textView_photo_counter);
        mPhotoInfoTextView = (TextView) view.findViewById(R.id.textView_photo_info);

        mPhotoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                /* Set size of image view */
                if (mPhotoImageView != null) {
                    mPhotoImageView.getLayoutParams().height = (int) (maxHeightRatio * mPhotoImageView.getWidth());
                    mPhotoImageView.requestLayout();
                }
                /* Add currently selected picture */
                int pos = getSelectedPicturePosition();
                if (pos < 0) {
                    return;
                }
                mPhotoImageView.setImageBitmap(getSelectedPicture(pos));
                if (mPlace.photos != null) {
                    mPhotoInfoTextView.setText(
                            Utils.daytimeToString(getContext(), mPlace.photos.get(pos).daytime));
                    mPhotoCounterTextView.setText(String.format(
                            getResources().getString(R.string.place_fragment_photo_counter),
                            pos + 1, mPlace.photos.size()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mPhotoImageView.setImageDrawable(null);
        updatePlace();

        return view;
    }

    @Override
    public void onPageSelected() {
        if (placeInvalidated && mPhotoImageView != null) {
            mPhotoImageView.setImageDrawable(null);
            updatePlace();
            placeInvalidated = false;
        }
    }

    /**
     * Get the position of selected photo for active place.
     *
     * @return The position of selected photo for active place.
     */
    public int getSelectedPicturePosition() {
        if (mPhotoSeekBar == null || mPlace == null || mPlace.photos == null) {
            return -1;
        }
        int wid = mPhotoSeekBar.getMax();
        float step = wid / mPlace.photos.size();
        int index = (int)Math.floor(mPhotoSeekBar.getProgress() / step);
        return Math.min(Math.max(index, 0), mPlace.photos.size() - 1);
    }

    /**
     * Get the photo for active place at given selected position.
     *
     * @param position Position of the selected picture.
     * @return The selected photo for active place.
     */
    public Bitmap getSelectedPicture(int position) {
        if (mPlace == null || mPlace.photos == null || mPlace.photos.size() <= 0) {
            return null;
        }
        Photo photo = mPlace.photos.get(position);
        return Utils.toBitmap(photo.image);
    }

    /**
     * Get the selected photo for active place.
     *
     * @return The selected photo for active place.
     */
    public Bitmap getSelectedPicture() {
        if (mPlace == null || mPlace.photos == null || mPlace.photos.size() <= 0) {
            return null;
        }
        Photo photo = mPlace.photos.get(getSelectedPicturePosition());
        return Utils.toBitmap(photo.image);
    }

    public void updatePlace() {
        if (mPlace == null) {
            return;
        }
        /* Add text information */
        mInfoContainerView.removeAllViews();
        //TODO: na-cashovat!
        Set<String> keySet = mPlace.gfields.keySet();
        for (String key : keySet) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 6, 0, 0);
            row.setLayoutParams(params);

            TextView title = new TextView(this.getContext());
            boolean isUrl = false;
            switch (key) {
                case "icon":
                    title.setText(getString(R.string.gfields_icon));
                    isUrl = true;
                    break;
                case "formatted_address":
                    title.setText(getString(R.string.gfields_formatted_address));
                    break;
                case "name":
                    title.setText(getString(R.string.gfields_name));
                    break;
                case "place_id":
                    title.setText(getString(R.string.gfields_place_id));
                    break;
                case "url":
                    title.setText(getString(R.string.gfields_url));
                    isUrl = true;
                    break;
                case "website":
                    title.setText(getString(R.string.gfields_website));
                    isUrl = true;
                    break;
                default:
                    title.setText(key);
            }
            title.setTypeface(null, Typeface.BOLD);
            row.addView(title);

            TextView content = new TextView(getContext());
            content.setText(mPlace.gfields.get(key));
            if (isUrl) {
                Linkify.addLinks(content, Linkify.WEB_URLS);
            }
            row.addView(content);

            mInfoContainerView.addView(row);
        }

        /* Show selected photo */
        if (mPlace.photos == null || mPlace.photos.size() <= 0) {
            return;
        }
        /* If no image is visible, add the first one and reset the progress bar position to 0 */
        if (mPhotoImageView != null && mPhotoImageView.getDrawable() == null) {
            mPhotoImageView.setScaleType(ImageView.ScaleType.FIT_START);
            mPhotoImageView.setImageBitmap(Utils.toBitmap(mPlace.photos.get(0).image));
            mPhotoSeekBar.setProgress(0);
            mPhotoInfoTextView.setText(
                    Utils.daytimeToString(getContext(), mPlace.photos.get(0).daytime));
            mPhotoCounterTextView.setText(String.format(
                    getResources().getString(R.string.place_fragment_photo_counter),
                    1, mPlace.photos.size()));
        }
    }

    public static void changePlace(Place newPlace) {
        mPlace = newPlace;
        maxHeightRatio = 0;
        if (mPlace != null && mPlace.photos != null) {
            for (Photo photo : mPlace.photos) {
                final double rat =  (double) photo.getHeight() / photo.getWidth();
                if (rat > maxHeightRatio) {
                    maxHeightRatio = rat;
                }
            }
        }
        placeInvalidated = true;
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
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {

    }
}
