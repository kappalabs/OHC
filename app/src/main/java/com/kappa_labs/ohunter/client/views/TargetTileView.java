package com.kappa_labs.ohunter.client.views;

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
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.entities.Target;
import com.kappa_labs.ohunter.client.utilities.Utils;
import com.kappa_labs.ohunter.lib.entities.Photo;

import java.util.Objects;

/**
 * Tile view to show basic information about one target. Shows its state by color, basic information
 * on the other side of the itself and number of points given/taken for manipulation with its target.
 */
public class TargetTileView extends View {

    private static final int MAX_PREVIEW_SIZE = 256;

    private String nameTitleString = getResources().getString(R.string.name_label);
    private String nameString;
    private String addressTitleString = getResources().getString(R.string.address_label);
    private String addressString;
    private String photosTitleString = getResources().getString(R.string.num_photos_label);
    private String photosString;
    private String gainString, lossString;
    private Drawable backgroundDrawable;
    private int titleTextColor = ContextCompat.getColor(getContext(), R.color.my_primary_text);
    private int textColor = ContextCompat.getColor(getContext(), R.color.my_secondary_text);

    private TextPaint mTextPaint, mTitleTextPaint;
    private TextPaint mGainTextPaint, mGainBackgroundPaint, mLossTextPaint, mLossBackgroundPaint;
    private float gainTextHeight, lossTextHeight;
    private StaticLayout nameTitleLayout, nameTextLayout;
    private StaticLayout addressTitleLayout, addressTextLayout;
    private StaticLayout photosTitleLayout, photosTextLayout;
    private float verticalGap = dpToPx(5);
    private boolean showName = true;
    private boolean showAddress = true;

    private int paintedPhotoIndex = -1;
    private String paintedTargetID = null;

    private Paint mPaint = new Paint();
    private Path mPath = new Path();

    private Target mTarget;


    /**
     * Creates a new target tile.
     *
     * @param context Context of the parent.
     */
    public TargetTileView(Context context) {
        super(context);

        init();
    }

    private void init() {
        nameString = "name";
        addressString = "address";
        photosString = "#";
        gainString = lossString = "";
        backgroundDrawable = null;
        final float textDimension = 24;
        final float titleTextDimension = 27;
        final float scoreTextDimension = 150;
        final int outlineWidth = 8;

        /* Set up a default TextPaint object */
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(textDimension);
        mTextPaint.setColor(textColor);
        mTitleTextPaint = new TextPaint();
        mTitleTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTitleTextPaint.setTextAlign(Paint.Align.CENTER);
        mTitleTextPaint.setTextSize(titleTextDimension);
        mTitleTextPaint.setColor(titleTextColor);
        mGainTextPaint = new TextPaint();
        mGainTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mGainTextPaint.setTextAlign(Paint.Align.CENTER);
        mGainTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.state_completed));
        mGainTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mGainTextPaint.setTextSize(scoreTextDimension);
        mGainBackgroundPaint = new TextPaint();
        mGainBackgroundPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mGainBackgroundPaint.setTextAlign(Paint.Align.CENTER);
        mGainBackgroundPaint.setColor(Color.WHITE);
        mGainBackgroundPaint.setTextSize(scoreTextDimension);
        mGainBackgroundPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mGainBackgroundPaint.setStyle(Paint.Style.STROKE);
        mGainBackgroundPaint.setStrokeWidth(outlineWidth);
        mLossTextPaint = new TextPaint();
        mLossTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mLossTextPaint.setTextAlign(Paint.Align.CENTER);
        mLossTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.state_rejected));
        mLossTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mLossTextPaint.setTextSize(scoreTextDimension);
        mLossBackgroundPaint = new TextPaint();
        mLossBackgroundPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mLossBackgroundPaint.setTextAlign(Paint.Align.CENTER);
        mLossBackgroundPaint.setColor(Color.WHITE);
        mLossBackgroundPaint.setTextSize(scoreTextDimension);
        mLossBackgroundPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mLossBackgroundPaint.setStyle(Paint.Style.STROKE);
        mLossBackgroundPaint.setStrokeWidth(outlineWidth);

        /* Update TextPaint and text measurements from attributes */
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
        int wid = (int) (getWidth() * 0.9);
        nameTitleLayout = new StaticLayout(nameTitleString, mTitleTextPaint, wid, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        nameTextLayout = new StaticLayout(nameString, mTextPaint, wid, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        addressTitleLayout = new StaticLayout(addressTitleString, mTitleTextPaint, wid, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        addressTextLayout = new StaticLayout(addressString, mTextPaint, wid, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        photosTitleLayout = new StaticLayout(photosTitleString, mTitleTextPaint, wid, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        photosTextLayout = new StaticLayout(photosString, mTextPaint, wid, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

        float hei = nameTitleLayout.getHeight() + nameTextLayout.getHeight()
                + photosTitleLayout.getHeight() + photosTextLayout.getHeight() + 2 * verticalGap;
        showAddress = hei + addressTitleLayout.getHeight() + addressTextLayout.getHeight()
                + verticalGap <= getHeight() * 0.9f;
        showName = showAddress || hei <= getHeight() * 0.9f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /* Draw image on background */
        if (backgroundDrawable != null && ((BitmapDrawable) backgroundDrawable).getBitmap() != null
                && !((BitmapDrawable) backgroundDrawable).getBitmap().isRecycled()) {
            backgroundDrawable.setBounds(0, 0, getWidth(), getHeight());
            backgroundDrawable.draw(canvas);
            paintedTargetID = mTarget.getPlaceID();
            paintedPhotoIndex = mTarget.getPhotoIndex();
            mTarget.setIsPhotoDrawn(true);
        }

        if (mTarget.getState() == Target.TargetState.UNAVAILABLE && !mTarget.isRotationDrawn()) {
            /* Add mask on the background, so that the text is readable */
            canvas.drawColor(ContextCompat.getColor(getContext(), R.color.shadow_unavailable));
        }

        /* Draw text on the opposite side of tile */
        if (mTarget.isRotationDrawn()) {
            /* Add mask on the background, so that the text is readable */
            canvas.drawColor(ContextCompat.getColor(getContext(), R.color.shadow_rotated));

            /* Draw the text */
            canvas.save();
            canvas.translate(getWidth() / 2, verticalGap);

            /* Show the additional information only when there is room for it */
            if (showName) {
                nameTitleLayout.draw(canvas);
                canvas.translate(0, nameTitleLayout.getHeight());
                nameTextLayout.draw(canvas);
                canvas.translate(0, nameTextLayout.getHeight() + verticalGap);
            }
            if (showAddress) {
                addressTitleLayout.draw(canvas);
                canvas.translate(0, addressTitleLayout.getHeight());
                addressTextLayout.draw(canvas);
                canvas.translate(0, addressTextLayout.getHeight() + verticalGap);
            }
            /* The number of photos is showed always */
            photosTitleLayout.draw(canvas);
            canvas.translate(0, photosTitleLayout.getHeight());
            photosTextLayout.draw(canvas);
            canvas.restore();
        } else {
            /* Drawings specific to the front side of the tile */
            switch (mTarget.getState()) {
                case COMPLETED:
                    canvas.drawText(gainString, getWidth() / 2, (getHeight() / 2 - gainTextHeight), mGainBackgroundPaint);
                    canvas.drawText(gainString, getWidth() / 2, (getHeight() / 2 - gainTextHeight), mGainTextPaint);
                    mTarget.setIsStateInvalidated(false);
                    break;
                case REJECTED:
                    canvas.drawText(lossString, getWidth() / 2, (getHeight() / 2 - lossTextHeight), mLossBackgroundPaint);
                    canvas.drawText(lossString, getWidth() / 2, (getHeight() / 2 - lossTextHeight), mLossTextPaint);
                    mTarget.setIsStateInvalidated(false);
                    break;
            }
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

        int color = getState().getColor(getContext());
        int darker = darkenColor(color);

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

    private int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
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
        mPaint.setColor(ContextCompat.getColor(getContext(), R.color.shadow_frame));
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
            if (!Objects.equals(paintedTargetID, mTarget.getPlaceID()) || paintedPhotoIndex != mTarget.getPhotoIndex()) {
                this.backgroundDrawable = getCroppedSelected(mTarget.getPhoto(mTarget.getPhotoIndex()));
                mTarget.setIsPhotoDrawn(false);
            }
        }
        /* Score text needs to know the measurements of this view */
        if (mTarget.isStateInvalidated()) {
            getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    try {
                        updateScore();
                        invalidate();
                        return true;
                    } finally {
                        getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                }
            });
        }
        invalidate();
    }

    /**
     * Updates score information on the tile.
     */
    public void updateScore() {
        gainString = mTarget.getDiscoveryGain() + "+" + mTarget.getSimilarityGain();
        lossString = mTarget.getRejectLoss() + "";

        setTextSizeForWidth(mGainTextPaint, getWidth() * .8f, gainString);
        float minSize = mGainTextPaint.getTextSize();
        setTextSizeForWidth(mLossTextPaint, getWidth() * .6f, lossString);
        if (mLossTextPaint.getTextSize() < minSize) {
            minSize = mLossTextPaint.getTextSize();
            mGainTextPaint.setTextSize(minSize);
        }
        mLossTextPaint.setTextSize(minSize);
        mGainBackgroundPaint.setTextSize(minSize);
        mLossBackgroundPaint.setTextSize(minSize);

        gainTextHeight = (mGainTextPaint.descent() + mGainTextPaint.ascent()) / 2;
        lossTextHeight = (mLossTextPaint.descent() + mLossTextPaint.ascent()) / 2;
    }

    private void setTextSizeForWidth(Paint paint, float desiredSize, String text) {
        /* Get the bounds of the text, using testTextSize */
        final float testTextSize = 48f;
        paint.setTextSize(testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        /* Calculate the desired size as a proportion of our testTextSize */
        float desiredTextSize = Math.min(
                testTextSize * desiredSize / bounds.width(),
                testTextSize * desiredSize / bounds.height());

        /* Set the paint for that size */
        paint.setTextSize(desiredTextSize);
    }

    /**
     * Sets the target for this tile view, invalidates the target state for repaint.
     *
     * @param target The target for this tile view.
     */
    public void setTarget(Target target) {
        this.mTarget = target;

        /* Change the place only when it's necessary */
        if (target == null) {
            return;
        }

        if (!Objects.equals(paintedTargetID, mTarget.getPlaceID()) || paintedPhotoIndex != mTarget.getPhotoIndex()) {
            this.backgroundDrawable = getCroppedSelected(mTarget.getPhoto(mTarget.getPhotoIndex()));
            mTarget.setIsPhotoDrawn(false);
        }
        this.nameString = target.getGField("name");
        this.addressString = target.getGField("formatted_address");
        this.photosString = target.getNumberOfPhotos() + "";

        mTarget.setIsStateInvalidated(true);

        /* Texts need to know the measurements of the view */
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                try {
                    invalidateTextPaintAndMeasurements();
                    invalidate();
                    return true;
                } finally {
                    getViewTreeObserver().removeOnPreDrawListener(this);
                }
            }
        });
        invalidate();
    }

    /**
     * NOTE: async task neni vhodny, nebot je pri stahovani cilu blokovan jinymi async-tasky...
     */
    private BitmapDrawable getCroppedSelected(Photo photo) {
        if (photo == null) {
            return null;
        }
        Bitmap selected = mTarget.getSelectedPhotoPreview();
        if (selected == null || selected.isRecycled()) {
            Bitmap photoBitmap = Utils.toBitmap(photo.sImage, MAX_PREVIEW_SIZE);
            Bitmap cropped = cropBitmap(photoBitmap);
            if (photoBitmap != cropped && photoBitmap != null && !photoBitmap.isRecycled()) {
                photoBitmap.recycle();
            }
            mTarget.setSelectedPhoto(cropped);
            selected = cropped;
        }
        return new BitmapDrawable(getResources(), selected);
    }

    /**
     * Crop the image so that the center is aligned to the tile center and no background is visible.
     * Preserve size ratio.
     *
     * @param bitmap The bitmap to crop.
     * @return Drawable witch preserved size ratio that is cropped and fills rectangle.
     */
    private Bitmap cropBitmap(Bitmap bitmap) {
        Bitmap cropped;
        if (bitmap.getWidth() >= bitmap.getHeight()) {
            cropped = Bitmap.createBitmap(bitmap, bitmap.getWidth() / 2 - bitmap.getHeight() / 2, 0,
                    bitmap.getHeight(), bitmap.getHeight()
            );
        } else {
            cropped = Bitmap.createBitmap(bitmap, 0, bitmap.getHeight() / 2 - bitmap.getWidth() / 2,
                    bitmap.getWidth(), bitmap.getWidth()
            );
        }
        Bitmap resized = Bitmap.createScaledBitmap(cropped, MAX_PREVIEW_SIZE, MAX_PREVIEW_SIZE, true);
        if (cropped != resized && !cropped.isRecycled()) {
            cropped.recycle();
        }

        return resized;
    }

    /**
     * Gets the unique place ID string value.
     *
     * @return The unique place ID string value.
     */
    public String getPlaceID() {
        return mTarget.getPlaceID();
    }

    /**
     * Gets the target associated with this tile.
     *
     * @return The target associated with this tile.
     */
    public Target getTarget() {
        return mTarget;
    }

    /**
     * Gets the state of the target associated with this tile.
     *
     * @return The state of the target associated with this tile.
     */
    public Target.TargetState getState() {
        return mTarget.getState();
    }

    /**
     * Changes the highlight around this tile.
     *
     * @param highlighted True to enable the highlight, false to disable.
     */
    public void setHighlighted(boolean highlighted) {
        if (mTarget.isHighlighted() != highlighted) {
            mTarget.setHighlighted(highlighted);
            invalidate();
        }
    }

}
