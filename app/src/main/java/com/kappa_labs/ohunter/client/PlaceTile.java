package com.kappa_labs.ohunter.client;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.kappa_labs.ohunter.lib.entities.Place;

/**
 * TODO: document your custom view class.
 */
public class PlaceTile extends View {

    private String mExampleString; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private Drawable mExampleDrawable;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    public enum TILE_STATE {
        RED, GREEN, ACTIVATED, PHOTOGENIC, LOCKED, COMPLETED
    }

    private boolean rotated;
    private boolean selected;
    private TILE_STATE mState = TILE_STATE.LOCKED;
    private Place mPlace;


    public PlaceTile(Context context) {
        super(context);

        init(null, 0);
    }

    public PlaceTile(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(attrs, 0);
    }

    public PlaceTile(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.PlaceTile, defStyle, 0);

        mExampleString = a.getString(
                R.styleable.PlaceTile_exampleString);
        mExampleColor = a.getColor(
                R.styleable.PlaceTile_exampleColor,
                mExampleColor);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mExampleDimension = a.getDimension(
                R.styleable.PlaceTile_exampleDimension,
                mExampleDimension);

        if (a.hasValue(R.styleable.PlaceTile_exampleDrawable)) {
            mExampleDrawable = a.getDrawable(
                    R.styleable.PlaceTile_exampleDrawable);
            if (mExampleDrawable != null) {
                mExampleDrawable.setCallback(this);
            }
        }

        a.recycle();
        mExampleString = "String";
        mExampleColor = Color.BLUE;
        mExampleDimension = 24;
        mExampleDrawable = getResources().getDrawable(R.drawable.ic_media_play, null);

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mExampleDimension);
        mTextPaint.setColor(mExampleColor);
        mTextWidth = mTextPaint.measureText(mExampleString);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        // TODO: consider storing these as member variables to reduce
//        // allocations per draw cycle.
//        int paddingLeft = getPaddingLeft();
//        int paddingTop = getPaddingTop();
//        int paddingRight = getPaddingRight();
//        int paddingBottom = getPaddingBottom();
//
//        int contentWidth = getWidth() - paddingLeft - paddingRight;
//        int contentHeight = getHeight() - paddingTop - paddingBottom;
//
//        // Draw the example drawable on top of the text.
//        if (mExampleDrawable != null) {
//            mExampleDrawable.setBounds(paddingLeft, paddingTop,
//                    paddingLeft + contentWidth, paddingTop + contentHeight);
//            mExampleDrawable.draw(canvas);
//        }
//
//        // Draw the text.
//        canvas.drawText(mExampleString,
//                paddingLeft + (contentWidth - mTextWidth) / 2,
//                paddingTop + (contentHeight + mTextHeight) / 2,
//                mTextPaint);

        // Draw the example drawable on top of the text.
        if (mExampleDrawable != null) {
            mExampleDrawable.setBounds(0, 0, getWidth(),  getHeight());
            mExampleDrawable.draw(canvas);
        }

        // Draw the text.
        canvas.drawText(mExampleString, (getWidth() - mTextWidth) / 2, (getHeight() + mTextHeight) / 2,
                mTextPaint);

        /* Draw state mark in the corner */
        drawCorner(canvas);
    }

    private Paint mPaint = new Paint();
    private Path mPath = new Path();

    private void drawCorner(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int twoThirds = (int)((double) width / 3 * 2);

        int color = stateToColor();
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        int darker = Color.HSVToColor(hsv);

        //TODO: lze staticky - vzdy stejne
        mPath.rewind();
        mPath.moveTo(twoThirds, height);
        mPath.lineTo(width, height);
        mPath.lineTo(width, twoThirds);
        mPath.lineTo(twoThirds, height);
        mPath.close();

        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(mPath, mPaint);

        //TODO: lze staticky - vzdy stejne
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

    private int stateToColor() {
        switch (mState) {
            case GREEN:
                return Color.GREEN;
            case RED:
                return Color.RED;
            case ACTIVATED:
                return Color.YELLOW;
            case COMPLETED:
                return Color.BLUE;
            case LOCKED:
                return Color.GRAY;
            case PHOTOGENIC:
                return Color.WHITE;
            default:
                return Color.BLACK;
        }
    }

    @Override
    protected void onMeasure(int wid, int _) {
        /* We want the tile to be square */
        super.onMeasure(wid, wid);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        rotated = !rotated;
        invalidate();

        return super.onTouchEvent(event);
    }

    public void setPlace(Place place) {
        this.mPlace = place;
        this.mExampleDrawable = new BitmapDrawable(getResources(), Utils.toBitmap(place.photos.get(0).sImage));
        this.mExampleString = place.gfields.get("name");
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    public Place getPlace() {
        return mPlace;
    }

    public void setState(TILE_STATE state) {
        this.mState = state;
        invalidate();
    }

    /**
     * Gets the example string attribute value.
     *
     * @return The example string attribute value.
     */
    public String getExampleString() {
        return mExampleString;
    }

    /**
     * Sets the view's example string attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param exampleString The example string attribute value to use.
     */
    public void setExampleString(String exampleString) {
        mExampleString = exampleString;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example color attribute value.
     *
     * @return The example color attribute value.
     */
    public int getExampleColor() {
        return mExampleColor;
    }

    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param exampleColor The example color attribute value to use.
     */
    public void setExampleColor(int exampleColor) {
        mExampleColor = exampleColor;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getExampleDimension() {
        return mExampleDimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setExampleDimension(float exampleDimension) {
        mExampleDimension = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example drawable attribute value.
     *
     * @return The example drawable attribute value.
     */
    public Drawable getExampleDrawable() {
        return mExampleDrawable;
    }

    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.
     *
     * @param exampleDrawable The example drawable attribute value to use.
     */
    public void setExampleDrawable(Drawable exampleDrawable) {
        mExampleDrawable = exampleDrawable;
    }
}
