package layout;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.kappa_labs.ohunter.lib.entities.Place;

import java.util.ArrayList;

import client.ohunter.fojjta.cekuj.net.ohunter.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HuntOfferFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HuntOfferFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HuntOfferFragment extends Fragment {

    private static final String PARAM_GREENS_KEY = "param_green_key";
    private static final String PARAM_RED_KEY = "param_red_key";

    private ArrayList<Place> mParamGreen;
    private ArrayList<Place> mParamRed;

    private ArrayAdapter<Place> mGreenAdapter;
    private ArrayAdapter<Place> mRedAdapter;

    private OnFragmentInteractionListener mListener;

    private ListView hmenu_green_listview;
    private ListView hmenu_red_listview;

    private static final String TAG = "HuntOfferFragment";


    public HuntOfferFragment() {
        // Required empty public constructor
    }

    /**
     * Create a new instance of this fragment.
     *
     * @param green_places Green (selected) places in the ohunter-menu.
     * @param red_places Red (offer) places in the ohunter-menu.
     * @return A new instance of fragment HuntOfferFragment.
     */
    public static HuntOfferFragment newInstance(ArrayList<Place> green_places, ArrayList<Place> red_places) {
        HuntOfferFragment fragment = new HuntOfferFragment();
        Bundle args = new Bundle();
        args.putSerializable(PARAM_GREENS_KEY, green_places);
        args.putSerializable(PARAM_RED_KEY, red_places);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParamGreen = (ArrayList<Place>) getArguments().getSerializable(PARAM_GREENS_KEY);
            mParamRed = (ArrayList<Place>) getArguments().getSerializable(PARAM_RED_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_hunt_offer, container, false);

        hmenu_green_listview = (ListView) view.findViewById(R.id.listView_hmenu_green);
        hmenu_red_listview = (ListView) view.findViewById(R.id.listView_hmenu_red);

        /* GREEN ==================== */
        hmenu_green_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "green position: " + position);
                for (int i = 0; i < hmenu_green_listview.getChildCount(); i++) {
                    hmenu_green_listview.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }
                for (int i = 0; i < hmenu_red_listview.getChildCount(); i++) {
                    hmenu_red_listview.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }
                view.setBackgroundColor(Color.GREEN);

                if (mListener != null) {
                    mListener.onItemSelected(mParamGreen.get(position));
                }
            }
        });
        hmenu_green_listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "green long pos: " + position);
                for (int i = 0; i < hmenu_green_listview.getChildCount(); i++) {
                    hmenu_green_listview.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }
                for (int i = 0; i < hmenu_red_listview.getChildCount(); i++) {
                    hmenu_red_listview.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }

                if (mListener != null) {
                    mListener.onItemUnselected();
                    Place removed = mParamGreen.get(position);
                    mGreenAdapter.remove(removed);
                    mListener.onGreenRejected(removed);
                    mRedAdapter.add(removed);
                    return true;
                }
                return false;
            }
        });

        /* RED ==================== */
        hmenu_red_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "red position: " + position);
                for (int i = 0; i < hmenu_green_listview.getChildCount(); i++) {
                    hmenu_green_listview.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }
                for (int i = 0; i < hmenu_red_listview.getChildCount(); i++) {
                    hmenu_red_listview.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }
                view.setBackgroundColor(Color.RED);

                if (mListener != null) {
                    mListener.onItemSelected(mParamRed.get(position));
                }
            }
        });
        hmenu_red_listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "red long pos: " + position);
                for (int i = 0; i < hmenu_green_listview.getChildCount(); i++) {
                    hmenu_green_listview.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }
                for (int i = 0; i < hmenu_red_listview.getChildCount(); i++) {
                    hmenu_red_listview.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }

                if (mListener != null) {
                    mListener.onItemUnselected();
                    Place removed = mParamRed.get(position);
                    mRedAdapter.remove(removed);
                    mListener.onGreenRejected(removed);
                    mGreenAdapter.add(removed);
                    return true;
                }
                return false;
            }
        });

        mGreenAdapter = new ArrayAdapter<>(hmenu_green_listview.getContext(),
                android.R.layout.simple_list_item_1, mParamGreen);
        mRedAdapter = new ArrayAdapter<>(hmenu_red_listview.getContext(),
                android.R.layout.simple_list_item_1, mParamRed);

        hmenu_green_listview.setAdapter(mGreenAdapter);
        hmenu_red_listview.setAdapter(mRedAdapter);

        return view;
    }

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
        void onItemSelected(Place place);
        void onItemUnselected();
        void onGreenRejected(Place place);
        void onRedRejected(Place place);
        void onRedAccepted(Place place);
    }
}
