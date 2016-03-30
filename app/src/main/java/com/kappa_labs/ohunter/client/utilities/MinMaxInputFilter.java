package com.kappa_labs.ohunter.client.utilities;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * This class provides a simple filter for given number range.
 */
public class MinMaxInputFilter implements InputFilter {

    private int min, max;


    /**
     * Creates a number filter on whole numbers from range [min; max].
     *
     * @param min The lower bound of the filter.
     * @param max The upper bound of the filter.
     */
    public MinMaxInputFilter(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            int input = Integer.parseInt(dest.toString() + source.toString());
            if (isInRange(min, max, input)) {
                return null;
            }
        } catch (NumberFormatException ignored) { }
        /* Non-numerical input is not allowed */
        return "";
    }

    private boolean isInRange(int a, int b, int c) {
        return b > a ? c >= a && c <= b : c >= b && c <= a;
    }

}
