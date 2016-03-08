package com.kappa_labs.ohunter.client;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.kappa_labs.ohunter.lib.entities.Photo;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.FillPlacesRequest;
import com.kappa_labs.ohunter.lib.requests.Request;
import android.widget.GridLayout.LayoutParams;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by kappa on 8.3.16.
 */
public class TileAdapter extends BaseAdapter {

    private Context mContext;
    private Player mPlayer;
    private ArrayList<String> mPlaceIDs;
    private LruCache<String, Bitmap> mMemoryCache;


    public TileAdapter(Context context, ArrayList<String> placeIDs, Player player) {
        this.mContext = context;
        this.mPlayer = player;
        this.mPlaceIDs = placeIDs;

        // Get memory class of this device, exceeding this amount will throw an
        // OutOfMemory exception.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {

            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in bytes rather than number
                // of mPlaceIDs.
                return bitmap.getByteCount();
            }

        };
    }

    @Override
    public int getCount() {
        return mPlaceIDs.size();
    }

    @Override
    public Object getItem(int position) {
        return mPlaceIDs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PlaceTile placeTile;

        if (convertView == null) {
            placeTile = new PlaceTile(mContext);
        } else {
            placeTile = (PlaceTile) convertView;
        }

        loadPlace(position, placeTile);

        return placeTile;
    }

    public void loadPlace(int position, final PlaceTile placeTile) {
        Request request = new FillPlacesRequest(
                mPlayer, new String[]{mPlaceIDs.get(position)}, Photo.DAYTIME.DAY, 800, 480);
        final String placeID = mPlaceIDs.get(position);
        Utils.RetrieveResponseTask responseTask =
                Utils.getInstance().new RetrieveResponseTask(new Utils.OnResponseTaskCompleted() {
                    @Override
                    public void onResponseTaskCompleted(Response response, OHException ohex, int code) {
                        if (ohex == null && response != null && response.places != null && response.places.length > 0) {
                            placeTile.setPlace(response.places[0]);
                        } else {
                            mPlaceIDs.remove(placeID);
                            notifyDataSetChanged();
                        }
                    }
                },
//                        Utils.getServerCommunicationDialog(mContext),
                        null,
                        position);
        responseTask.execute(request);
    }

//    @Override
//    public View getView(int arg0, View convertView, ViewGroup arg2) {
//        ImageView img = null;
//
//        if (convertView == null) {
//            img = new ImageView(mContext);
//            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
//            GridView.LayoutParams params = new AbsListView.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//            img.setLayoutParams(params);
//        } else {
//            img = (ImageView) convertView;
//        }
//
//        int resId = mContext.getResources().getIdentifier(mPlaceIDs.get(arg0),
//                "drawable", mContext.getPackageName());
//
//        loadBitmap(resId, img);
//
//        return img;
//    }

//    public void loadBitmap(int resId, ImageView imageView) {
//        if (cancelPotentialWork(resId, imageView)) {
//            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
//            imageView.setBackgroundResource(R.drawable.empty_photo);
//            task.execute(resId);
//        }
//    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(
                    bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    public static boolean cancelPotentialWork(int data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final int bitmapData = bitmapWorkerTask.data;
            if (bitmapData != data) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was
        // cancelled
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
        public int data = 0;
        private final WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage
            // collected
            imageViewReference = new WeakReference<>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params) {
            data = params[0];
            final Bitmap bitmap = decodeSampledBitmapFromResource(
                    mContext.getResources(), data, 100, 100);
            addBitmapToMemoryCache(String.valueOf(params[0]), bitmap);
            return bitmap;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and
            // width
            final int heightRatio = Math.round((float) height
                    / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }
}
