package layout;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.entities.Place;

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

    private static final String ARG_PLACE = "place";

    private static Place mPlace;
    private static boolean placeInvalidated = true;

    private OnFragmentInteractionListener mListener;

//    private ImageView mImageView;
//    private LinearLayout mPhotosView;
    private SeekBar mPhotoSeekBar;
    private ImageView mPhotoImageView;


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

        mPhotoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mPlace == null || mPlace.photos == null || mPlace.photos.size() <= 0) {
                    return;
                }
                int wid = mPhotoSeekBar.getMax();
                float step = wid / mPlace.photos.size();
                int index = (int)Math.floor(progress / step);
                index = Math.min(Math.max(index, 0), mPlace.photos.size()-1);
                Photo photo = mPlace.photos.get(index);

//                mPhotoImageView.setScaleType(ImageView.ScaleType.FIT_XY);
                mPhotoImageView.setImageBitmap(Utils.toBitmap(photo.image));
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

    private void updatePlace() {
        if (mPlace == null || mPlace.photos == null || mPlace.photos.size() <= 0) {
            return;
        }
        /* If no image is visible, add the first one and reset the progress bar position to 0 */
        if (mPhotoImageView != null && mPhotoImageView.getDrawable() == null) {
            mPhotoImageView.setScaleType(ImageView.ScaleType.FIT_START);
            mPhotoImageView.setImageBitmap(Utils.toBitmap(mPlace.photos.get(0).image));
            mPhotoSeekBar.setProgress(0);
        }
    }

    public static void changePlace(Place newPlace) {
        mPlace = newPlace;
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
