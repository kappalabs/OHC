package com.kappa_labs.ohunter.client;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.lib.entities.Place;

import java.util.Objects;

/**
 * TODO: document your custom view class.
 */
public class TargetTileView extends View {

    private static final String TAG = "PlaceTileView";

    private String nameTitleString = getResources().getString(R.string.name_label);
    private String nameString;
    private String addressTitleString = getResources().getString(R.string.address_label);
    private String addressString;
    private String photosTitleString = getResources().getString(R.string.num_photos_label);
    private String photosString;
    private Drawable backgroundDrawable;
    private float textDimension;
    private float titleTextDimension;
    private int titleTextColor = ContextCompat.getColor(getContext(), R.color.my_primary_text);
    private int textColor = ContextCompat.getColor(getContext(), R.color.my_secondary_text);

    private TextPaint mTextPaint, mTitleTextPaint;
    private float nameTextWidth, addressTextWidth, photosTextWidth;
    private float nameTextHeight, addressTextHeight, photosTextHeight;
    private float nameTitleWidth, addressTitleWidth, photosTitleWidth;
    private float nameTitleHeight, addressTitleHeight, photosTitleHeight;

    private Paint mPaint = new Paint();
    private Path mPath = new Path();

    // TODO: totok MP pujde do LRU cache, nacist ho jiz umime z lokalu
    private Place mPlace;
    private Target mTarget;


    public TargetTileView(Context context) {
        super(context);

        init();
    }

    private void init() {
        nameString = "name";
        addressString = "address";
        photosString = "#";
        backgroundDrawable = null;
        textDimension = 22;
        titleTextDimension = 25;

        /* Set up a default TextPaint object */
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTitleTextPaint = new TextPaint();
        mTitleTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTitleTextPaint.setTextAlign(Paint.Align.LEFT);

        /* Prepare metrics for titles */
        mTitleTextPaint.setTextSize(titleTextDimension);
        mTitleTextPaint.setColor(titleTextColor);
        Paint.FontMetrics fontMetrics = mTitleTextPaint.getFontMetrics();
        nameTitleWidth = mTitleTextPaint.measureText(nameTitleString);
        nameTitleHeight = fontMetrics.bottom;
        addressTitleWidth = mTitleTextPaint.measureText(addressTitleString);
        addressTitleHeight = fontMetrics.bottom;
        photosTitleWidth = mTitleTextPaint.measureText(photosTitleString);
        photosTitleHeight = fontMetrics.bottom;

        /* Update TextPaint and text measurements from attributes */
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(textDimension);
        mTextPaint.setColor(textColor);
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();

        nameTextWidth = mTextPaint.measureText(nameString);
        nameTextHeight = fontMetrics.bottom;
        addressTextWidth = mTextPaint.measureText(addressString);
        addressTextHeight = fontMetrics.bottom;
        photosTextWidth = mTextPaint.measureText(photosString);
        photosTextHeight = fontMetrics.bottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /* Draw image on background */
        if (backgroundDrawable != null) {
            backgroundDrawable.setBounds(0, 0, getWidth(), getHeight());
            backgroundDrawable.draw(canvas);
            mTarget.setIsPhotoDrawn(true);
        }

        /* Draw text on the opposite side of tile */
        if (mTarget.isRotationDrawn()) {
            /* Add mask on the background, so that the text is readable */
            canvas.drawColor(ContextCompat.getColor(getContext(), R.color.white_shadow));

            /* Draw the text */
            float leftGap = getWidth() / 10f;
            float heightSum = getHeight() / 6f + nameTitleHeight / 2;
            canvas.drawText(nameTitleString, leftGap, heightSum, mTitleTextPaint);
            heightSum += nameTitleHeight * 3 + nameTextHeight / 2;
            canvas.drawText(nameString, leftGap, heightSum, mTextPaint);
            heightSum += nameTextHeight * 4 + addressTitleHeight / 2;
            canvas.drawText(addressTitleString, leftGap, heightSum, mTitleTextPaint);
            heightSum += addressTitleHeight * 3 + addressTextHeight / 2;
            canvas.drawText(addressString, leftGap, heightSum, mTextPaint);
            heightSum += addressTextHeight * 4 + photosTitleHeight / 2;
            canvas.drawText(photosTitleString, leftGap, heightSum, mTitleTextPaint);
            heightSum += photosTitleHeight * 3 + photosTextHeight / 2;
            canvas.drawText(photosString, leftGap, heightSum, mTextPaint);
        }
        /* Draw state mark in the corner */
        drawCorner(canvas);

        /* Draw frame around the whole tile */
        drawFrame(canvas);

        /* Highlight if selected */
        if (mTarget.isHighlighted()) {
            drawHighlight(canvas);
        }
    }

    private void drawCorner(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int twoThirds = (int)((double) width / 3 * 2);

        int color = stateToColor();
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        int darker = Color.HSVToColor(hsv);

        /* Draw the triangle in corner */
        mPath.rewind();
        mPath.moveTo(twoThirds, height);
        mPath.lineTo(width, height);
        mPath.lineTo(width, twoThirds);
        mPath.lineTo(twoThirds, height);
        mPath.close();

        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(mPath, mPaint);

        /* Draw dark line on the edge of that triangle */
        mPath.rewind();
        mPath.moveTo(twoThirds, height);
        mPath.lineTo(width, twoThirds);
        mPath.close();

        mPaint.setStrokeWidth(3);
        mPaint.setPathEffect(null);
        mPaint.setColor(darker);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(mPath, mPaint);
    }

    private void drawFrame(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        mPath.rewind();
        mPath.moveTo(0, 0);
        mPath.lineTo(0, height);
        mPath.lineTo(width, height);
        mPath.lineTo(width, 0);
        mPath.close();

        mPaint.setStrokeWidth(5);
        mPaint.setPathEffect(null);
        mPaint.setColor(ContextCompat.getColor(getContext(), R.color.black_shadow));
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(mPath, mPaint);
    }

    private void drawHighlight(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        mPath.rewind();
        mPath.moveTo(0, 0);
        mPath.lineTo(0, height);
        mPath.lineTo(width, height);
        mPath.lineTo(width, 0);
        mPath.close();

        mPaint.setStrokeWidth(dpToPx(10));
        mPaint.setPathEffect(null);
        mPaint.setColor(ContextCompat.getColor(getContext(), R.color.state_chosen));
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(mPath, mPaint);
    }

    private int stateToColor() {
        switch (mTarget.getState()) {
            case ACCEPTED:
                return ContextCompat.getColor(getContext(), R.color.state_green);
            case DEFERRED:
                return ContextCompat.getColor(getContext(), R.color.state_deferred);
            case REJECTED:
                return ContextCompat.getColor(getContext(), R.color.state_red);
            case ACTIVATED:
                return ContextCompat.getColor(getContext(), R.color.state_activated);
            case COMPLETED:
                return ContextCompat.getColor(getContext(), R.color.state_completed);
            case LOCKED:
                return ContextCompat.getColor(getContext(), R.color.state_locked);
            case PHOTOGENIC:
                return ContextCompat.getColor(getContext(), R.color.state_photogenic);
            default:
                return Color.WHITE;
        }
    }

    /**
     * Convert dp to pixels.
     *
     * @param dp The dp to be converted.
     * @return The appropriate number of pixels for given dp value.
     */
    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @Override
    protected void onMeasure(int wid, int _) {
        /* We want the tile to be square */
        super.onMeasure(wid, wid);
    }

    /**
     * Start pending animations, invalidate content.
     */
    public void update() {
        if (mTarget.isRotated() != mTarget.isRotationDrawn()) {
            final ObjectAnimator anim2 = (ObjectAnimator) AnimatorInflater.loadAnimator(getContext(), R.animator.half_flip2);
            anim2.setTarget(this);
            anim2.setDuration(100);
            anim2.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    invalidate();
                }
            });

            ObjectAnimator anim = (ObjectAnimator) AnimatorInflater.loadAnimator(getContext(), R.animator.half_flip1);
            anim.setTarget(this);
            anim.setDuration(100);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mTarget.setIsRotationDrawn(mTarget.isRotated());
                    invalidate();
                    anim2.start();
                }
            });
            anim.start();
        }
        if (!mTarget.isPhotoDrawn()) {
            Place place = PlacesManager.getPlace(getContext(), getPlaceID());
            if (place != null) {
                backgroundDrawable = cropBitmap(Utils.toBitmap(place.getPhoto(mTarget.getPhotoIndex()).sImage));
            }
        }
        invalidate();
    }

    public void setTarget(Target target) {
        this.mTarget = target;
    }

    public Place getPlace() {
        return mPlace;
    }

    public void setPlace(Place place) {
        /* Change the place only when it's necessary */
        if (place == null || this.mPlace != null && Objects.equals(this.mPlace.getID(), mTarget.getPlaceID())) {
            return;
        }
        setPlaceID(place.getID());
        this.mPlace = place;
        if (place.getNumberOfPhotos() > mTarget.getPhotoIndex()) {
//            //TODO: vyresit to pres asynctask aby nedochazelo k zasekavani UI pri prochazeni nabidky
//            Utils.BitmapWorkerTask bitmapTask = Utils.getInstance().new BitmapWorkerTask(new Utils.OnBitmapReady() {
//                @Override
//                public void onBitmapReady(Bitmap bitmap) {
//                    backgroundDrawable = cropBitmap(bitmap);
//                    invalidate();
//                }
//            });
//            bitmapTask.execute(place.getPhoto(0).sImage);
//            this.backgroundDrawable = getResources().getDrawable(R.color.my_primary_light, null);
//
////            PlacesManager.getPreview(getContext(), this);

            this.backgroundDrawable =
                    cropBitmap(Utils.toBitmap(place.getPhoto(mTarget.getPhotoIndex()).sImage));
        }
        this.nameString = place.getGField("name");
        this.addressString = place.getGField("formatted_address");
        this.photosString = place.getNumberOfPhotos() + "";

        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    /**
     * Crop the image so that the center is aligned to the tile center and no background is visible.
     * Preserve size ratio.
     *
     * @param bitmap The bitmap to crop.
     * @return Drawable witch preserved size ratio that is cropped and fills rectangle.
     */
    private Drawable cropBitmap(Bitmap bitmap) {
        Bitmap cropped;
        if (bitmap.getWidth() >= bitmap.getHeight()){
            cropped = Bitmap.createBitmap(bitmap, bitmap.getWidth() / 2 - bitmap.getHeight() / 2, 0,
                    bitmap.getHeight(), bitmap.getHeight()
            );
        } else{
            cropped = Bitmap.createBitmap(bitmap, 0, bitmap.getHeight() / 2 - bitmap.getWidth() / 2,
                    bitmap.getWidth(), bitmap.getWidth()
            );
        }
//        bitmap.recycle();
        return new BitmapDrawable(getResources(), cropped);
    }

//    public void setPreview(Drawable preview) {
//        this.backgroundDrawable = preview;
//        invalidate();
//    }

    /**
     * Gets the unique place ID string value.
     *
     * @return The unique place ID string value.
     */
    public String getPlaceID() {
        return mTarget.getPlaceID();
    }

    /**
     * Sets the view's place ID string value. In the view, this string
     * is the unique identifier of a place from Google Places API.
     *
     * @param placeID The place ID string value to use.
     */
    public void setPlaceID(String placeID) {
        mTarget.setPlaceID(placeID);
    }

    public Target.TargetState getState() {
        return mTarget.getState();
    }

    public void setHighlighted(boolean highlighted) {
        if (mTarget.isHighlighted() != highlighted) {
            mTarget.setHighlighted(highlighted);
            invalidate();
        }
    }

}
