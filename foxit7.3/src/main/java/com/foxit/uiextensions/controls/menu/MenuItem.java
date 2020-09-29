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
package com.foxit.uiextensions.controls.menu;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.foxit.uiextensions.R;

/**
 * Class <CODE>MenuItem</CODE> represents the menu item.
 */
public class MenuItem {
    private int mTag;
    public View customView;

    private TextView mText;
    private ImageView mImage;
    private View mView;
    private MenuViewCallback mCallback;

    /**
     * Constructs a menu item.
     * @param context the context.
     * @param tag the tag of the menu item.
     *            constants are defined in {@link MoreMenuConfig}, eg: {@link MoreMenuConfig#ITEM_DOCINFO}.
     *            it should related to the menu group.
     * @param itemText the text of the menu item.
     * @param imageID the image id of the menu item.
     * @param callback the {@link MenuViewCallback} call back.
     */
    public MenuItem(Context context, int tag, String itemText, int imageID, MenuViewCallback callback) {
        mTag = tag;

        mView = View.inflate(context, R.layout.view_menu_more_item, null);
        mText = (TextView) mView.findViewById(R.id.menu_more_item_tv);

        if (itemText == null) {
            mText.setVisibility(View.INVISIBLE);
        } else {
            mText.setText(itemText);
        }
        mImage = (ImageView) mView.findViewById(R.id.menu_more_item_bt);
        if (imageID == 0) {
            mImage.setVisibility(View.GONE);
        } else {
            mImage.setImageResource(imageID);
        }

        mCallback = callback;
        final MenuItem itemSelf = this;

        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onClick(itemSelf);
                }
            }
        });
    }

    /**
     * Constructs a menu item.
     * @param context the context.
     * @param tag the tag of the menu item.
     *            constants are defined in {@link MoreMenuConfig}, eg: {@link MoreMenuConfig#ITEM_DOCINFO}.
     *            it should related to the menu group.
     * @param customView The custom view.
     */
    public MenuItem(Context context, int tag, View customView) {
        mTag = tag;
        this.customView = customView;

        mView = View.inflate(context, R.layout.view_menu_more_item, null);

        LinearLayout ly = (LinearLayout) mView.getRootView();
        ly.removeAllViews();
        ly.addView(customView);
    }

    void setDividerVisible(boolean visibly) {
        View divider = mView.findViewById(R.id.menu_more_item_divider);
        if (divider == null) return;
        if (visibly) {
            divider.setVisibility(View.VISIBLE);
        } else {
            divider.setVisibility(View.GONE);
        }
    }

    protected boolean isCustomView() {
        if (customView != null) {
            return true;
        }

        return false;
    }

    /**
     * Get the root view of the menu item.
     * @return The root view of the menu item.
     */
    public View getView() {
        return mView;
    }

    /**
     * Get the tag of the current menu item.
     * @return
     */
    public int getTag() {
        return mTag;
    }

    public void setEnable(boolean enable) {
        mView.setEnabled(enable);
        mText.setEnabled(enable);
        mImage.setEnabled(enable);
    }
}
