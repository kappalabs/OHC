package com.kappa_labs.ohunter.client;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kappa_labs.ohunter.lib.entities.Place;

import java.io.InputStream;
import java.util.List;

/**
 * ArrayAdapter for Offer Fragment showing basic information about the Places.
 */
public class PlaceArrayAdapter extends ArrayAdapter<Place> {


    public PlaceArrayAdapter(Context context, int resource, List<Place> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.place_item_row, parent, false);
        }

        Place place = getItem(position);

        if (place != null) {
            ImageView iconImageView = (ImageView) v.findViewById(R.id.imageView_icon);
            TextView nameTextView = (TextView) v.findViewById(R.id.textView_name);
            TextView addressTextView = (TextView) v.findViewById(R.id.textView_address);
            TextView numPhotosTextView = (TextView) v.findViewById(R.id.textView_num_photos);

            if (iconImageView != null) {
                //TODO: nacashovat ikony? nebo zobrazovat fotky?
                /* Show the first available picture */
                if (place.photos != null && place.photos.size() > 0) {
                    iconImageView.setImageBitmap(Utils.toBitmap(place.photos.get(0).sImage));
                }
//                /* Show the icon */
//                String icon = place.gfields.get("icon");
//                if (icon == null) {
//                    //TODO: napr: nejaky obrazek s otaznikem
//                } else {
//                    new DownloadImageTask(iconImageView).execute(icon);
//                }
            }
            if (nameTextView != null) {
                String name = place.gfields.get("name");
                if (name == null) {
                    nameTextView.setText(v.getResources().getString(R.string.unknown));
                } else {
                    nameTextView.setText(name);
                }
            }
            if (addressTextView != null) {
                String address = place.gfields.get("formatted_address");
                if (address == null) {
                    addressTextView.setText(v.getResources().getString(R.string.unknown));
                } else {
                    addressTextView.setText(address);
                }
            }
            if (numPhotosTextView != null) {
                if (place.photos == null) {
                    numPhotosTextView.setText(v.getResources().getString(R.string.unknown));
                } else {
                    numPhotosTextView.setText(String.valueOf(place.photos.size()));
                }
            }
        }

        return v;
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String pictureUrl = urls[0];
            Bitmap bitmap = null;
            try {
                InputStream in = new java.net.URL(pictureUrl).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                bmImage.setImageBitmap(result);
            }
        }
    }

}
