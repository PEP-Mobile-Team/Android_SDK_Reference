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
package com.foxit.uiextensions.controls.toolbar;

import android.view.View;

/**
 * Interface that defines information about bar.
 */
public interface BaseBar {
    /**
     * Horizontal layout direction of this bar.
     */
    public static final int HORIZONTAL = 0;
    /**
     * Vertical layout direction of this bar.
     */
    public static final int VERTICAL = 1;

    /**
     * Interface that defines layout direction of the item on the bar.
     */
    public enum TB_Position {
        /**
         * Horizontal layout direction of the item on the bar: Left-Top.
         */
        Position_LT,
        /**
         * Horizontal layout direction of the item on the bar: Center.
         */
        Position_CENTER,
        /**
         * Horizontal layout direction of the item on the bar: Right-Bottom.
         */
        Position_RB;
    }

    /**
     * Adds a item.
     *
     * @param item The item to add.
     * @param position The {@link TB_Position} to add the item.
     * @return {@code true} if success, {@code false} otherwise.
     */
    public boolean addView(IBaseItem item, TB_Position position);

    /**
     * Adds a item.
     *
     * @param item The item to add.
     * @param position The {@link TB_Position} to add the item.
     * @param index The position at which to add the item.
     * @return {@code true} if success, {@code false} otherwise.
     */
    public boolean addView(IBaseItem item, TB_Position position, int index);

    /**
     * Removes a item by the specified tag.
     *
     * @param tag The tag to remove the item.
     * @return {@code true} if success, {@code false} otherwise.
     */
    public boolean removeItemByTag(int tag);

    /**
     * Removes the specified item.
     *
     * @param item The item to remove.
     * @return {@code true} if success, {@code false} otherwise.
     */
    public boolean removeItemByItem(IBaseItem item);

    /**
     * Removes a item by the specified index.
     *
     * @param position The {@link TB_Position} to remove the item.
     * @param index The position at which to remove the item.
     *
     * @return {@code true} if success, {@code false} otherwise.
     */
    public boolean removeItemByIndex(TB_Position position, int index);

    /**
     * Remove all items of the bar.
     */
    public void removeAllItems();

    /**
     * Set the visibility state of this bar.
     *
     * @param visible {@code true}: This bar is visible, {@code false}: This bar is invisible.
     */
    public void setBarVisible(boolean visible);

    /**
     * Retrieve the {@link View} attached to this bar, if present.
     *
     * @return The View attached to the bar or null if no View is present.
     */
    public View getContentView();

    /**
     * Get a item.
     *
     * @param location The {@link TB_Position} to get the item.
     * @param tag The tag to get the item.
     * @return The item at the specified layout direction and tag.
     */
    public IBaseItem getItem(TB_Position location, int tag);

    /**
     * Get a item at the specified position in the bar.
     * @param location The {@link TB_Position} to get the item.
     * @param index The position at which to get the bar
     * @return The item at the specified layout direction and position.
     */
    public IBaseItem getItemByIndex(TB_Position location, int index);

    /**
     * Returns the visibility status for this item.
     *
     * @param location The {@link TB_Position} to get the item visibility.
     * @param tag The tag to get the item visibility.
     *
     * @return {@code -1} while the item can`t be found, or should be
     *         one of {@link View#VISIBLE}, {@link View#INVISIBLE}, or {@link View#GONE}.
     */
    public int getItemVisibility(TB_Position location, int tag);

    /**
     * Returns the visibility status for this item.
     *
     * @param location The {@link TB_Position} to get the item visibility.
     * @param index The position at which to get the bar visibility.
     *
     * @return {@code -1} while the item can`t be found, or should be
     *         one of {@link View#VISIBLE}, {@link View#INVISIBLE}, or {@link View#GONE}.
     */
    public int getItemVisibilityByIndex(TB_Position location, int index);

    /**
     * Set the visibility state of this item.
     *
     * @param location The {@link TB_Position} to set the item visibility.
     * @param tag The tag to set the item visibility.
     * @param visibility should be one of {@link View#VISIBLE}, {@link View#INVISIBLE}, or {@link View#GONE}.
     */
    public void setItemVisibility(TB_Position location, int tag, int visibility);

    /**
     * Set the visibility state of this item by the specified index.
     *
     * @param location The {@link TB_Position} to set the item visibility.
     * @param index The position at which to set the bar visibility.
     * @param visibility should be one of {@link View#VISIBLE}, {@link View#INVISIBLE}, or {@link View#GONE}.
     */
    public void setItemVisibilityByIndex(TB_Position location, int index, int visibility);

    /**
     * Returns the number of item in the bar at the specified layout direction.
     *
     * @param location The {@link TB_Position} to get the the number of item.
     * @return The number of item in the bar
     */
    public int getItemsCount(TB_Position location);

    /**
     * Sets a layout direction of this bar view.
     *
     * @param orientation should one of {@link #HORIZONTAL} or {@link #VERTICAL}.
     */
    public void setOrientation(int orientation);

    /**
     * Sets a layout direction of this bar view.
     *
     * @param orientation should one of {@link #HORIZONTAL} or {@link #VERTICAL}.
     * @param width  the width of this bar
     * @param height the height of this bar
     */
    void setOrientation(int orientation,  int width, int height);

    /**
     * Sets the background color for this bar.
     *
     * @param color the color of the background
     */
    public void setBackgroundColor(int color);

    /**
     * Set the background to a given resource. The resource should refer to
     * a Drawable object or 0 to remove the background.
     * @param res The identifier of the resource.
     *
     */
    public void setBackgroundResource(int res);

    /**
     * Whether uses interval between items for the center direction layout bar.
     * Only for {@link TB_Position#Position_CENTER}.
     *
     * Note: This method is only used within RDK
     *
     * @param interval {@code true}: use interval between items, {@code false} otherwise.
     */
    public void setInterval(boolean interval);

    /**
     * Set space between the items.
     * Must use it after {@link #setOrientation(int)}.
     *
     * @param space The space between the items.
     */
    void setItemInterval(int space);

    /**
     * Sets the width of the bar.
     *
     * @param width how wide the view wants to be.
     */
    public void setWidth(int width);

    /**
     * Sets the height of the bar.
     *
     * @param height how tall the bar wants to be.
     */
    public void setHeight(int height);

    /**
     * Sets the {@link View} attached to this bar
     *
     * @param v The {@link View} to set.
     */
    public void setContentView(View v);

    /**
     * Whether intercepts the touch event.
     *
     * @param isInterceptTouch {@code true}: Intercept the touch event. {@code false} otherwise.
     */
    public void setInterceptTouch(boolean isInterceptTouch);

    /**
     * Whether reset the size of the item in the bar.
     *
     * @param needResetItemSize {@code true}: Reset the size of the item in the bar, {@code false} otherwise.
     */
    public void setNeedResetItemSize(boolean needResetItemSize);

    /**
     * Update the layout of the toolbar
     */
    public void updateLayout();
}
