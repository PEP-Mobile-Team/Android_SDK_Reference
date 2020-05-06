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
package com.foxit.uiextensions.controls.propertybar;

import android.view.View;


/**
 * This is mainly used to control the display mode of the page, and you can use it to hide/display unnecessary functions.
 * <br/><br/>
 * you can use it through {@link com.foxit.uiextensions.UIExtensionsManager#getSettingBar()}
 */
public interface IMultiLineBar {
    /**
     * Interface definition for a callback to be invoked
     * when a value of a multi-line bar is changed or the bar is dismissed.
     */
    public interface IValueChangeListener {
        /**
         * Called when a value of a multi-line bar is changed.
         * @param type The type of the bar. {@link #getType()}.
         * @param value current value
         */
        public void onValueChanged(int type, Object value);

        /**
         * Called when the bar is dismissed.
         */
        public void onDismiss();

        /**
         * Return a type of the current bar.
         */
        public int getType();
    }

    /** The type of Multi-line bar: Light*/
    public static final int TYPE_LIGHT = 0x0001;
    /** The type of Multi-line bar: Day&Night, used to switch day and night mode*/
    public static final int TYPE_DAYNIGHT = 0x0002;
    /** The type of Multi-line bar: System light, control the brightness of the page */
    public static final int TYPE_SYSLIGHT = 0x0004;
    /** The type of Multi-line bar: Single Page mode*/
    public static final int TYPE_SINGLEPAGE = 0x0008;
    /** The type of Multi-line bar: Continuous Page mode*/
    public static final int TYPE_CONTINUOUSPAGE = 0x0010;
    /** The type of Multi-line bar: Page thumbnail*/
    public static final int TYPE_THUMBNAIL = 0x0020;
    /** The type of Multi-line bar: Screen lock*/
    public static final int TYPE_LOCKSCREEN = 0x0040;
    /** The type of Multi-line bar: Reflow mode*/
    public static final int TYPE_REFLOW = 0x0080;
    /** The type of Multi-line bar: Crop mode*/
    public static final int TYPE_CROP = 0x0100;

    /** The type of Multi-line bar: Crop mode */
    public static final int TYPE_FACINGPAGE = 0x0120;
    /** The type of Multi-line bar: Cover mode*/
    public static final int TYPE_COVERPAGE = 0x140;
    /** The type of Multi-line bar: Pan&Zoom*/
    public static final int TYPE_PANZOOM = 0x0180;

    /** The type of Multi-line bar: Fit page*/
    public static final int TYPE_FITPAGE = 0x0200;
    /**The type of Multi-line bar: Fit Width*/
    public static final int TYPE_FITWIDTH = 0x0220;
    /**The type of Multi-line bar: Rotate View*/
    public static final int TYPE_ROTATEVIEW = 0x0240;

    /**
     * Sets value by property.
     *
     * @param property The type
     * @param value the value to set.
     */
    public void setProperty(int property, Object value);

    /**
     * Whether this multi-line bar is showing.
     */
    public boolean isShowing();

    /**
     * Display this multi-line bar.
     */
    public void show();

    /**
     * Returns the visibility status for this view.
     *
     * @param type the modules tag,  Please refer to {@link #TYPE_DAYNIGHT #TYPE_XXX } values
     *
     * @return One of {@link View#VISIBLE}, {@link View#GONE} or -1.
     * if return -1,means can't find this type.
     *
     * @see #TYPE_DAYNIGHT
     * @see #TYPE_SYSLIGHT
     * @see #TYPE_SINGLEPAGE
     * @see #TYPE_CONTINUOUSPAGE
     * @see #TYPE_THUMBNAIL
     * @see #TYPE_LOCKSCREEN
     * @see #TYPE_REFLOW
     * @see #TYPE_CROP
     * @see #TYPE_FACINGPAGE
     */
    public int getVisibility(int type);

    /**
     * Set the enabled state of this view.
     *
     * @param type       the modules tag,  Please refer to {@link #TYPE_DAYNIGHT #TYPE_XXX } values
     * @param visibility One of {@link View#VISIBLE}, {@link View#GONE}.
     *                   <></>
     * @see #TYPE_DAYNIGHT
     * @see #TYPE_SYSLIGHT
     * @see #TYPE_SINGLEPAGE
     * @see #TYPE_CONTINUOUSPAGE
     * @see #TYPE_THUMBNAIL
     * @see #TYPE_LOCKSCREEN
     * @see #TYPE_REFLOW
     * @see #TYPE_CROP
     * @see #TYPE_FACINGPAGE
     * @see #TYPE_COVERPAGE
     *
     */
    public void setVisibility(int type, int visibility);

    /**
     * Disposes of the multi-line bar.
     */
    public void dismiss();

    /** Return the content view of this multi-line bar.*/
    public View getContentView();

    /**
     * Sets the listener to be called when the multi-line bar is dismissed or its value is changed.
     * @param listener the listener
     */
    public void registerListener(IValueChangeListener listener);

    /**
     * Unregister the specified listener.
     */
    public void unRegisterListener(IValueChangeListener listener);

    /**
     * Set the enabled state of the specified bar.
     *
     * NOW: only for TYPE_CROP, TYPE_REFLOW and TYPE_PANZOOM
     *
     * @param property TYPE_CROP, TYPE_REFLOW or TYPE_PANZOOM
     * @param enable True if this bar is enabled, false otherwise.
     */
    public void enableBar(int property, boolean enable);
}
