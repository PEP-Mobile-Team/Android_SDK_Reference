/**
 * Copyright (C) 2003-2019, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.controls.propertybar.IMultiLineBar;
import com.foxit.uiextensions.controls.toolbar.IBarsHandler;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.IMainFrame;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;

/**
 * interface that defines information for uiextensions{@link UIExtensionsManager}.Foxit UIExtensions had implement a default completed PDF
 * Reader, This interface provides UI customization and event listener.
 */
public interface IPDFReader {

    /**
     * Interface used to allow the user to run some code
     * when the ic_title_back button clicked.
     */
    interface BackEventListener {
        /**
         * Called when the ic_title_back button clicked.
         *
         * @return Return <code>true</code> to prevent this event from being propagated
         *         further, or <code>false</code> to indicate that you have not handled
         *         this event and it should continue to be propagated by Foxit.
         */
        boolean onBack();
    }

    /**
     * Register a callback to be invoked when the activity of fragment lifecycle
     * {@link ILifecycleEventListener#onCreate(Activity, Bundle)},
     * {@link ILifecycleEventListener#onStart(Activity)}, {@link ILifecycleEventListener#onPause(Activity)},
     * {@link ILifecycleEventListener#onResume(Activity)}, {@link ILifecycleEventListener#onStop(Activity)},
     * {@link ILifecycleEventListener#onDestroy(Activity)}, {@link ILifecycleEventListener#onSaveInstanceState(Activity, Bundle)},
     * {@link ILifecycleEventListener#onHiddenChanged(boolean)}, {@link ILifecycleEventListener#onActivityResult(Activity, int, int, Intent)}
     * executing.
     *
     * @param listener the specified {@link ILifecycleEventListener}
     * @return {@code true} register success or otherwise.
     */
    boolean registerLifecycleListener(ILifecycleEventListener listener);

    /**
     * Unregister the specified {@link ILifecycleEventListener}
     *
     * @param listener the specified {@link ILifecycleEventListener}
     * @return {@code true} unregister success or otherwise.
     */
    boolean unregisterLifecycleListener(ILifecycleEventListener listener);

    /**
     * Register a callback to be invoked when the state changed.
     *
     * @param listener the {@link IStateChangeListener} to use.
     * @return {@code true} register success or otherwise.
     */
    boolean registerStateChangeListener(IStateChangeListener listener);

    /**
     * Unregister the specified {@link IStateChangeListener}
     *
     * @param listener the specified {@link IStateChangeListener}
     * @return {@code true} unregister success or otherwise.
     */
    boolean unregisterStateChangeListener(IStateChangeListener listener);

    /**
     * Get the read state.
     *
     * @return Return the read state, should be one of <CODE>{@link ReadStateConfig#STATE_NORMAL ReadStateConfig.STATE_XXX}</CODE>
     *
     */
    int getState();

    /**
     * Change the read state
     *
     * @param state should be one of <CODE>{@link ReadStateConfig#STATE_NORMAL ReadStateConfig.STATE_XXX}</CODE>
     */
    void changeState(int state);

    /// @cond DEV
    /**
     * Get the {@link IMainFrame} of the UI extension.
     * @return The {@link IMainFrame} to use.
     */
    IMainFrame getMainFrame();
    /// @endcond

    /**
     * Get the pdf view control
     * @return The {@link PDFViewCtrl} to use.
     */
    PDFViewCtrl getPDFViewCtrl();

    /**
     * Get bar manager
     * @return Return the {@link IBarsHandler} to use.
     */
    IBarsHandler getBarManager();

    /** @return the {@link IMultiLineBar} which used to control the page mode, zoom mode and other settings.*/
    IMultiLineBar getSettingBar();

    /// @cond DEV
    /** @return the {@link DocumentManager} which used to get document property and other operations*/
    DocumentManager getDocumentManager();
    /// @endcond

    /**
     * @return the content view of the UI extension
     */
    RelativeLayout getContentView();

    /** Go ic_title_back to the previous activity.*/
    void backToPrevActivity();

    /**
     * Set a {@link BackEventListener} to be invoked when the ic_title_back button clicked.
     * @param listener The {@link BackEventListener} to use
     */
    void setBackEventListener(BackEventListener listener);


    /**
     * @return Get the {@link BackEventListener}
     */
    BackEventListener getBackEventListener();
}
