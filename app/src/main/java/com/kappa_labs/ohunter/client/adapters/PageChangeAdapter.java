package com.kappa_labs.ohunter.client.adapters;

/**
 * Interface for letting the pages in Pager know about its visibility.
 */
public interface PageChangeAdapter {

    /**
     * Called when page/fragment is selected in the main activity.
     */
    void onPageSelected();

}
