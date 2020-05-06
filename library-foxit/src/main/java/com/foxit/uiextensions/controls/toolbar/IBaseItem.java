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

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Set the properties of item in the toolbar
 */
public interface IBaseItem {
    /**
     * Used for {@link ItemType#Item_Text_Image}: the text view is on the left of the image.
     * Note: This method is only used within RDK
     */
    public static final int RELATION_LEFT = 10;
    /**
     * Used for {@link ItemType#Item_Text_Image}: the text view is on the top of the image.
     * Note: This method is only used within RDK
     */
    public static final int RELATION_TOP = 11;
    /**
     * Used for {@link ItemType#Item_Text_Image}: the text view is on the right of the image.
     * Note: This method is only used within RDK
     */
    public static final int RELATION_RIGNT = 12;
    /**
     * Used for {@link ItemType#Item_Text_Image}: the text view is blow of the image.
     * Note: This method is only used within RDK
     */
    public static final int RELATION_BELOW = 13;

    /** Retrieve the {@link View} attached to this item, if present. */
    public View getContentView();

    /**
     * Sets the text to be displayed.
     *
     * @param text text to be displayed
     */
    public void setText(CharSequence text);

    /**
     * Sets the text to be displayed using a string resource identifier.
     *
     * @param res the resource identifier of the string resource to be displayed
     */
    public void setText(int res);

    /** Return the text that TextView is displaying. */
    public String getText();

    /**
     * Sets the text color for all the states (normal, selected,
     * focused) to be this color.
     *
     * @param selectedTextColor    A color value in the form 0xAARRGGBB. used when text is selected
     * @param disSelectedTextColor A color value in the form 0xAARRGGBB. used when text is normal state.
     */
    public void setTextColor(int selectedTextColor, int disSelectedTextColor);

    /**
     * Sets the text color for all the states (normal, selected,
     * focused) to be this color.
     *
     * @param color A color value in the form 0xAARRGGBB.
     */
    public void setTextColor(int color);

    /**
     * Sets the typeface and style in which the text should be displayed.
     *
     * @param typeface the {@link Typeface} to use.
     */
    public void setTypeface(Typeface typeface);

    /**
     * Set the text size
     *
     * @param size The desired size
     */
    public void setTextSize(float size);

    /**
     * Sets the text color from a color state list associated with a particular resource ID.
     *
     * @param res a color state list associated with a particular resource ID
     */
    public void setTextColorResource(int res);

    /**
     * Sets a drawable as the content of this item.
     *
     * @param res the resource identifier of the drawable
     * @return true for success.
     */
    public boolean setImageResource(int res);

    /**
     * Sets a drawable as the content of this item.
     *
     * @param drawable the Drawable to set
     * @return true for success.
     */
    public boolean setImageDrawable(@Nullable Drawable drawable);

    /**
     * Sets the padding of the image on the item
     *
     * @param l the left padding in pixels
     * @param t the top padding in pixels
     * @param r the right padding in pixels
     * @param b the bottom padding in pixels
     */
    void setImagePadding(int l, int t, int r, int b);

    /** Set the content view attached to this item */
    public void setContentView(View view);

    /**
     * Set the background to a given resource.
     *
     * @param res The identifier of the resource.
     */
    public void setBackgroundResource(int res);

    /**
     * Sets the list of input filters that will be used if the buffer is
     * Editable. Has no effect otherwise.
     *
     * @see android.widget.TextView#setFilters(InputFilter[])
     */
    public void setFilters(InputFilter[] filters);

    /**
     * Causes words in the text that are longer than the view's width
     * to be ellipsized instead of broken in the middle.
     *
     * @see android.widget.TextView#setEllipsize(TextUtils.TruncateAt)
     */
    public void setEllipsize(TextUtils.TruncateAt where);

    /**
     * Sets the relation between text view and image view for this item.
     * NOTE: Only used for {@link ItemType#Item_Text_Image}.
     *
     * @param relation the relation between text view and image view.
     *                 Should be one of {@link #RELATION_LEFT}, {@link #RELATION_TOP},
     *                 {@link #RELATION_RIGNT} or {@link #RELATION_BELOW}
     */
    public void setRelation(int relation);

    /**
     * Set the enabled state of this item.
     *
     * @param enable True if this item is enabled, false otherwise.
     */
    public void setEnable(boolean enable);

    /**
     * Changes the selection state of this item.
     *
     * @param selected true if the item must be selected, false otherwise
     */
    public void setSelected(boolean selected);

    /**
     * Register a callback to be invoked when this item is clicked.
     *
     * @param l The callback that will run
     */
    public void setOnClickListener(View.OnClickListener l);

    /**
     * Register a callback to be invoked when this view is clicked and held.
     *
     * @param l The callback that will run
     */
    public void setOnLongClickListener(View.OnLongClickListener l);

    /**
     * Register a custom callback to be invoked when this view is clicked and held.
     *
     * @param l The custom callback that will run
     */
    void setOnClickListener(OnClickListener l);

    /**
     * Register a custom callback to be invoked when this view is clicked and held.
     *
     * @param l The custom callback that will run
     */
    void setOnLongPressListener(onLongPressListener l);

    /**
     * <p>Changes the checked state of this item.</p>
     *
     * @param checked true to check the button, false to uncheck it
     */
    void setChecked(boolean checked);

    /**
     * @return The current checked state of the item
     */
    boolean isChecked();

    /**
     * Set the tag for this item.
     *
     * @param tag a number used to identify the item
     */
    public void setTag(int tag);

    /** Return the tag of the item. */
    public int getTag();

    /**
     * Set the identifier for this item view
     *
     * @param id a number used to identify the item view
     */
    public void setId(int id);

    /** Return the identifier of the item view */
    public int getId();

    /**
     * The interval in pixels of the item.
     *
     * @param interval the interval size.
     */
    public void setInterval(int interval);

    /**
     * Set the display style of this item view
     *
     * @param type the specified {@link ItemType} to use.
     */
    public void setDisplayStyle(ItemType type);

    /**
     * Interface definition for a callback to be invoked when a view is clicked.
     */
    interface OnClickListener {
        /**
         * Called when a view has been clicked.
         *
         * @param item The item that was clicked.
         * @param v    The view that was clicked.
         */
        void onClick(IBaseItem item, View v);
    }

    /**
     * Interface definition for a callback to be invoked when a view has been clicked and held.
     */
    interface onLongPressListener {
        /**
         * Called when a view has been clicked and held.
         *
         * @param item The item that was clicked and held.
         * @param v    The view that was clicked and held.
         * @return true if the callback consumed the long click, false otherwise.
         */
        boolean onLongPress(IBaseItem item, View v);
    }

    /**
     * information about item type.
     */
    enum ItemType {
        /** item type: text */
        Item_Text,
        /** item type: image */
        Item_Image,
        /** item type: text and image */
        Item_Text_Image,
        /** item type: customize */
        Item_custom;
    }

    /** information about the type sorting item. */
    enum SortType {
        /** sort item by tag */
        Sort_By_Tag,
        /** sort item by index */
        Sort_By_Index;
    }

    /**
     * Called from layout when this item should
     * assign a size and position to each of its children.
     *
     * @param left   Left position, relative to parent
     * @param top    Top position, relative to parent
     * @param right  Right position, relative to parent
     * @param bottom Bottom position, relative to parent
     */
    public void onItemLayout(int left, int top, int right, int bottom);

    /**
     * Set a {@link IResetParentLayoutListener} to be invoke when the layout has reset.
     *
     * @param listener the callback that will run.
     */
    public void setResetLayoutListener(IResetParentLayoutListener listener);

    /**
     * Interface definition for a callback to be invoked when reset layout of the item`s parent view.
     */
    public interface IResetParentLayoutListener {
        /**
         * Called when reset layout of the specified item`s parent view.
         *
         * @param baseItem the {@link IBaseItem} to use.
         */
        void resetParentLayout(IBaseItem baseItem);
    }
}
