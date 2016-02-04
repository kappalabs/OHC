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
public class HuntPlaceFragment extends Fragment {

    private static final String ARG_PLACE = "place";

    private Place mPlace;

    private OnFragmentInteractionListener mListener;

//    private ImageView mImageView;
//    private LinearLayout mPhotosView;
    private SeekBar mPhotoSeekBar;
    private ImageView mPhotoImageView;


    public HuntPlaceFragment() { }

    /**
     * Create a new instance of this fragment.
     *
     * @param place Place which details should be shown in this fragment.
     * @return A new instance of this fragment.
     */
    public static HuntPlaceFragment newInstance(Place place) {
        HuntPlaceFragment fragment = new HuntPlaceFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PLACE, place);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPlace = (Place) getArguments().getSerializable(ARG_PLACE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //NOTE: volana po onCreate()
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_hunt_place, container, false);
//        mImageView = (ImageView) view.findViewById(R.id.imageView_photos);
//        mPhotosView = (LinearLayout) view.findViewById(R.id.linearLayoutGallery);
        mPhotoImageView = (ImageView) view.findViewById(R.id.imageView_photo);
        mPhotoSeekBar = (SeekBar) view.findViewById(R.id.seekBar_photo);

        mPhotoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mPlace == null || mPlace.photos == null) {
                    return;
                }
                int wid = mPhotoSeekBar.getMax();
                float step = 1.0f;
                if (mPlace.photos.size() > 1) {
                    step = wid / (mPlace.photos.size() - 1);
                }
                int indx = (int)Math.ceil(progress / step);
                indx = Math.min(Math.max(indx, 0), mPlace.photos.size()-1);
                Photo photo = mPlace.photos.get(indx);

                mPhotoImageView.setScaleType(ImageView.ScaleType.FIT_START);
                mPhotoImageView.setImageBitmap(Utils.toBitmap(photo.image));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

//        if (mPlace != null && mPlace.photos != null) {
//            for (Photo photo : mPlace.photos) {
////                mImageView.setImageBitmap(Utils.toBitmap(photo.image));
//                ImageView iv = new ImageView(view.getContext());
//                iv.setImageBitmap(Utils.toBitmap(photo.image));
//                iv.setScaleType(ImageView.ScaleType.FIT_XY);
//
//                LinearLayout ll = new LinearLayout(view.getContext());
//                ll.setPadding(1, 1, 1, 1);
//
//                GradientDrawable drawable = new GradientDrawable();
//                drawable.setShape(GradientDrawable.RECTANGLE);
//                drawable.setStroke(3, Color.BLACK);
//                drawable.setCornerRadius(8);
//                drawable.setColor(Color.BLUE);
//
//                ll.setBackgroundDrawable(drawable);
//                ll.addView(iv);
//
////                mPhotosView.addView(ll);
//            }
//        }

        return view;
    }

//    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
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
