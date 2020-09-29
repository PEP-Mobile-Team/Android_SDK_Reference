/**
 * Copyright (C) 2003-2020, Foxit Software Inc..
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

import android.graphics.drawable.Drawable;
import android.view.View;

import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;

/**
 * Toolbar's Operation control class,you can use it show/hide/add/remove items on the toolbar .
 * (PS:Currently toolbar has only the topbar/bottombar on the main page )
 * <p>
 * you can use it through {@link com.foxit.uiextensions.UIExtensionsManager#getBarManager()}
 */
public interface IBarsHandler {

    /** Interface definition for a callback to be invoked when the item is clicked.*/
    interface IItemClickListener {

        /**
         * You actively add item to click callback
         */
        void onClick(View v);
    }

    /** enum class for bar name*/
    enum BarName {
        /** top bar */
        TOP_BAR,
        /** bottom bar */
        BOTTOM_BAR;
    }

    /**
     * Add an custom item to the toolbar
     * <p>
     * Inserts the specified {@link IBaseItem} at the specified position in the toolbar.
     * Shifts the {@link IBaseItem} currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * <p>
     * Note 1: if you want addItem in the topbar ,the gravity should be {@link  BaseBar.TB_Position#Position_LT} or{@link BaseBar.TB_Position#Position_RB};
     * if you want addItem in the bottombar,the gravity should be {@link BaseBar.TB_Position#Position_CENTER},Otherwise it may they overlap.<br><br>
     * Note 2: If your item has set the tag {@link IBaseItem#setTag(int)},the tag must be unique and the tag must be less than 100, or more than 300,
     * because the tag between 100 and 300 has been used or may be used in the future{@link ToolbarItemConfig}.<br><br>
     *
     * @param barName the toolbar name
     * @param gravity the location of item in the toolbar{@link BaseBar.TB_Position}
     * @param item    The item to add {@link BaseItemImpl}
     * @param index   the position at which to add the item,starting from 0 ,less than or equal to{@link #getItemsCount(BarName, BaseBar.TB_Position)}
     *                and is relative to {@link  BaseBar.TB_Position}.
     * @return true means add success ,otherwise add failure.
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt; getItemsCount()</tt>)
     */
    boolean addItem(BarName barName, BaseBar.TB_Position gravity, IBaseItem item, int index);

    /**
     * Add a default text-only item
     * <p>
     * Inserts the specified {@link IBaseItem} at the specified position in the toolbar.
     * Shifts the {@link IBaseItem} currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * <p>
     * Note 1: if you want addItem in the topbar ,the gravity should be {@link  BaseBar.TB_Position#Position_LT} or{@link BaseBar.TB_Position#Position_RB};
     * if you want addItem in the bottombar,the gravity should be {@link BaseBar.TB_Position#Position_CENTER},Otherwise it may they overlap.<br><br>
     *
     * @param barName the toolbar name
     * @param gravity the location of item in the toolbar{@link BaseBar.TB_Position}
     * @param text    text to be displayed
     * @param index   the position at which to add the item,starting from 0 ,less than or equal to{@link #getItemsCount(BarName, BaseBar.TB_Position)}
     *                and is relative to {@link  BaseBar.TB_Position}.
     * @param clickListener The callback that will run
     * @return true means add success ,otherwise add failure.
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt; getItemsCount()</tt>)
     */
    boolean addItem(BarName barName, BaseBar.TB_Position gravity, CharSequence text, int index, IItemClickListener clickListener);

    /**
     * Add a default image-only item
     * <p>
     * Inserts the specified {@link IBaseItem} at the specified position in the toolbar.
     * Shifts the {@link IBaseItem} currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * <p>
     * Note 1: if you want addItem in the topbar ,the gravity should be {@link  BaseBar.TB_Position#Position_LT} or{@link BaseBar.TB_Position#Position_RB};
     * if you want addItem in the bottombar,the gravity should be {@link BaseBar.TB_Position#Position_CENTER},Otherwise it may they overlap.<br><br>
     *
     * @param barName the toolbar name
     * @param gravity the location of item in the toolbar{@link BaseBar.TB_Position}
     * @param drawable   the Drawable to set, or {@code null} to clear the content
     * @param index   the position at which to add the item,starting from 0 ,less than or equal to{@link #getItemsCount(BarName, BaseBar.TB_Position)}
     *                and is relative to {@link  BaseBar.TB_Position}.
     * @param clickListener The callback that will run
     * @return true means add success ,otherwise add failure.
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt; getItemsCount()</tt>)
     */
    boolean addItem(BarName barName, BaseBar.TB_Position gravity, Drawable drawable, int index, IItemClickListener clickListener);

    /**
     * Add a default item to the toolbar
     * <p>
     * Inserts the specified {@link IBaseItem} at the specified position in the toolbar.
     * Shifts the {@link IBaseItem} currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * <p>
     * Note 1: if you want addItem in the topbar ,the gravity should be {@link  BaseBar.TB_Position#Position_LT} or{@link BaseBar.TB_Position#Position_RB};
     * if you want addItem in the bottombar,the gravity should be {@link BaseBar.TB_Position#Position_CENTER},Otherwise it may they overlap.<br><br>
     *
     * @param barName       the toolbar name
     * @param gravity       the location of item in the toolbar{@link BaseBar.TB_Position}
     * @param textId        the resource identifier of the string resource to be displayed
     * @param resId        the resource identifier of the drawable
     * @param index         the position at which to add the item,starting from 0 ,less than or equal to{@link #getItemsCount(BarName, BaseBar.TB_Position)}
     *                      and is relative to {@link  BaseBar.TB_Position}.
     * @param clickListener The callback that will run
     * @return true means add success ,otherwise add failure.
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt; getItemsCount()</tt>)
     */
    boolean addItem(BarName barName, BaseBar.TB_Position gravity, int textId, int resId, int index, IItemClickListener clickListener);

    /**
     * get the items count by {@link BarName} and {@link BaseBar.TB_Position}
     * <p>
     *
     * @param barName the toolbar name
     * @param gravity the location of item in the toolbar{@link BaseBar.TB_Position}
     * @return the items count
     */
    int getItemsCount(BarName barName, BaseBar.TB_Position gravity);

    /**
     * Get the item by tag, if tag does not exist, it return null
     * <p>
     * @param barName the toolbar name
     * @param gravity the location of item in the toolbar{@link BaseBar.TB_Position}
     * @param tag     the item id and is unique ,it may be the existing tag
     *                <ul>
     *                <li>TOP_BAR</li>
     *                <CODE>{@link ToolbarItemConfig#ITEM_TOPBAR_BACK ToolbarItemConfig.ITEM_TOPBAR_XXX}</CODE><br>
     *                <li>BOTTOM_BAR</li>
     *                <CODE>{@link ToolbarItemConfig#ITEM_BOTTOMBAR_LIST ToolbarItemConfig#ITEM_BOTTOMBAR_XXX}</CODE><br>
     *                </ul>
     *                or you custom tag.
     * @return {@link IBaseItem} If tag does not exist, it return null
     * @deprecated  {@link #getItemByIndex(BarName barName, BaseBar.TB_Position gravity, int index) instead of this method.
     */
    @Deprecated
    IBaseItem getItem(BarName barName, BaseBar.TB_Position gravity, int tag);

    /**
     * Get the item by index
     * <p>
     * @param barName the toolbar name
     * @param gravity the location of item in the toolbar{@link BaseBar.TB_Position}
     * @param index   the index of the {@link IBaseItem} and is relative to {@link  BaseBar.TB_Position}.
     * @return the {@link IBaseItem} at the specified position in the toolbar
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt; getItemsCount()</tt>)
     */
    IBaseItem getItemByIndex(BarName barName, BaseBar.TB_Position gravity, int index);

    /**
     * Set the enabled state of this view. If this toolbar does not contain
     * the tag, it will not work.
     * <p>
     * @param barName    the toolbar name {@link IBarsHandler.BarName}
     * @param gravity    the location of item in the toolbar{@link BaseBar.TB_Position}
     * @param tag        the item id and is unique ,it may be the existing tag
     *                   <ul>
     *                     <li>TOP_BAR</li>
     *                     <CODE>{@link ToolbarItemConfig#ITEM_TOPBAR_BACK ToolbarItemConfig.ITEM_TOPBAR_XXX}</CODE><br>
     *                     <li>BOTTOM_BAR</li>
     *                     <CODE>{@link ToolbarItemConfig#ITEM_BOTTOMBAR_LIST ToolbarItemConfig#ITEM_BOTTOMBAR_XXX}</CODE><br>
     *                   </ul>
     *                   or you custom tag.
     * @param visibility One of {@link  View#VISIBLE}, {@link View#INVISIBLE}, or {@link View#GONE}.
     * @deprecated  {@link #setItemVisibility(BarName barName, BaseBar.TB_Position gravity, int index, int visibility) instead of this method.
     */
    @Deprecated
    void setVisibility(BarName barName, BaseBar.TB_Position gravity, int tag, int visibility);

    /**
     * Set the enabled state of this item view.
     * <p>
     * @param barName    the toolbar name {@link IBarsHandler.BarName}
     * @param gravity    the location of item in the toolbar{@link BaseBar.TB_Position}
     * @param index      the index of the {@link IBaseItem} and is relative to {@link  BaseBar.TB_Position}.
     * @param visibility One of {@link  View#VISIBLE}, {@link View#INVISIBLE}, or {@link View#GONE}.
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt; getItemsCount()</tt>)
     */
    void setItemVisibility(BarName barName, BaseBar.TB_Position gravity, int index, int visibility);

    /**
     * Returns the visibility status for this view.If this toolbar does not contain
     * the tag, it will not work.
     *
     * @param barName the toolbar name {@link IBarsHandler.BarName}
     * @param gravity the location of item in the toolbar{@link BaseBar.TB_Position}
     * @param tag     the item id and is unique ,it may be the existing tag
     *                <ul>
     *                     <li>TOP_BAR</li>
     *                     <CODE>{@link ToolbarItemConfig#ITEM_TOPBAR_BACK ToolbarItemConfig.ITEM_TOPBAR_XXX}</CODE><br>
     *                     <li>BOTTOM_BAR</li>
     *                     <CODE>{@link ToolbarItemConfig#ITEM_BOTTOMBAR_LIST ToolbarItemConfig#ITEM_BOTTOMBAR_XXX}</CODE><br>
     *                </ul>
     *                or you custom tag.
     * @return One of {@link View#VISIBLE}, {@link View#INVISIBLE}, {@link View#GONE} or -1.
     * if return -1,means can't find this item by tag.
     * @deprecated  {@link #getItemVisibility get} instead of this method.
     */
    @Deprecated
    int getVisibility(BarName barName, BaseBar.TB_Position gravity, int tag);

    /**
     * Returns the visibility status for this view.
     *
     * @param barName the toolbar name {@link IBarsHandler.BarName}
     * @param gravity the location of item in the toolbar{@link BaseBar.TB_Position}
     * @param index   the index of the {@link IBaseItem} and is relative to {@link  BaseBar.TB_Position}.
     * @return One of {@link View#VISIBLE}, {@link View#INVISIBLE}, {@link View#GONE}
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt; getItemsCount()</tt>)
     */
    int getItemVisibility(BarName barName, BaseBar.TB_Position gravity, int index);

    /**
     * Removes the {@link IBaseItem} at the specified position in this toolbar
     * Shifts any subsequent elements to the left (subtracts one from their indices).
     * <p>
     *
     * @param barName the toolbar name
     * @param gravity the location of item in the toolbar{@link BaseBar.TB_Position}
     * @param index   the index of the {@link IBaseItem} and is relative to {@link  BaseBar.TB_Position}.
     * @return true means remove success,otherwise means remove failure
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt; getItemsCount()</tt>)
     */
    boolean removeItem(BarName barName, BaseBar.TB_Position gravity, int index);

    /**
     * Removes the {@link IBaseItem} in this toolbar,if it is present.If this toolbar does not contain
     * the {@link IBaseItem}, it is unchanged.
     * <p>
     *
     * @param barName the toolbar name
     * @param gravity the location of item in the toolbar{@link BaseBar.TB_Position}
     * @param item    the specified item in the toolbar
     * @return true means remove success,otherwise means remove failure.
     */
    boolean removeItem(BarName barName, BaseBar.TB_Position gravity, IBaseItem item);

    /**
     * Removes all items  from the toolbar
     *
     * @param barName the toolbar name
     */
    void removeAllItems(BarName barName);

    /**
     * add custom toolbar by BarName
     *
     * @param barName the toolbar name
     * @param view    the custom view
     * @return true means add success,otherwise means add failure.
     */
    boolean addCustomToolBar(BarName barName, View view);

    /**
     * remove toolbar by BarName
     *
     * @param barName the toolbar name
     * @return true means remove success,otherwise means remove failure.
     */
    boolean removeToolBar(BarName barName);

    /**
     * Set the enabled state of this view,and if set the enable to true, the bar is visible, or if set the enable to false,the bar is hide.
     *
     * @param barName the toolbar name
     * @param enabled True if this view is visible, false otherwise.
     */
    void enableToolBar(BarName barName, boolean enabled);

    /**
     * Sets the background color for the toolbar.
     *
     * @param barName the toolbar name
     * @param color   the color of the background
     */
    void setBackgroundColor(BarName barName, int color);

    /**
     * Set the background to a given resource. The resource should refer to
     * a Drawable object or 0 to remove the background.
     *
     * @param barName the toolbar name
     * @param resid   The identifier of the resource.
     */
    void setBackgroundResource(BarName barName, int resid);

}
