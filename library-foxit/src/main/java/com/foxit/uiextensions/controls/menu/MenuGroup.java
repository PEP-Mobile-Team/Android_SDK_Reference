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
package com.foxit.uiextensions.controls.menu;

import android.content.Context;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.foxit.uiextensions.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Class <CODE>MenuGroup</CODE> represents the menu group.
 * The menu group can contains a menu item.
 */
public class MenuGroup {
    private static final int VALUE_IF_KEY_NOT_FOUND = -1;

    private int tag;
    private Context mContext;
    private ArrayList<MenuItem> mMenuItems;
    private View mView;
    private LinearLayout mContentList_ly;
    private ComparatorItemByTag comparator;

    private SparseIntArray mItemVisibleMap = new SparseIntArray();

    /**
     * Constructs a menu group.
     * @param context the context.
     * @param tag The group tag, it`s can be one of {@link MoreMenuConfig#GROUP_FILE},
     *      {@link MoreMenuConfig#GROUP_PROTECT}, {@link MoreMenuConfig#GROUP_ANNOTATION} and {@link MoreMenuConfig#GROUP_FORM}
     * @param title The title of the menu group.
     */
    public MenuGroup(Context context, int tag, String title) {
        this.tag = tag;

        mContext = context;
        mMenuItems = new ArrayList<MenuItem>();
        comparator = new ComparatorItemByTag();
        mView = View.inflate(mContext, R.layout.view_menu_more_group, null);
        mContentList_ly = (LinearLayout) mView.findViewById(R.id.menu_more_group_content_ly);

        //init title
        TextView titleTV = (TextView) mView.findViewById(R.id.menu_more_group_title);
        if (title == null) {
            title = "";
        }
        titleTV.setText(title);
    }

    /**
     * Add menu item.
     * @param item The specified menu item to be added.
     */
    public void addItem(MenuItem item) {
        if (item == null)
            return;

        for (MenuItem menuItem : mMenuItems) {
            if (menuItem.getTag() == item.getTag()) {
                return;
            }
        }

        int visibility = mItemVisibleMap.get(item.getTag(),VALUE_IF_KEY_NOT_FOUND);
        if (VALUE_IF_KEY_NOT_FOUND != visibility) {

            if (View.VISIBLE == visibility) {
                item.getView().setVisibility(View.VISIBLE);
            } else if (View.INVISIBLE == visibility) {
                item.getView().setVisibility(View.INVISIBLE);
            } else {
                item.getView().setVisibility(View.GONE);
            }
        } else {
            mItemVisibleMap.put(item.getTag(), item.getView().getVisibility());
        }

        mMenuItems.add(item);
        Collections.sort(mMenuItems, comparator);

        resetItems();
    }

    private class ComparatorItemByTag implements Comparator<Object> {
        @Override
        public int compare(Object lhs, Object rhs) {
            if (lhs instanceof MenuItem && rhs instanceof MenuItem) {
                MenuItem lItem = (MenuItem) lhs;
                MenuItem rItem = (MenuItem) rhs;
                return lItem.getTag() - rItem.getTag();
            } else {
                return 0;
            }
        }
    }

    private void resetItems() {
        mContentList_ly.removeAllViews();
        for (MenuItem item : mMenuItems) {
            addItemToMenu(item);
        }
    }

    /**
     * Remove the specified menu item.
     * @param item The specified menu item to be removed.
     */
    public void removeItem(MenuItem item) {
        if (mMenuItems.size() > 0) {
            mMenuItems.remove(item);
            mContentList_ly.removeView(item.getView());
            mItemVisibleMap.delete(item.getTag());
        }
    }

    /**
     * Remove the menu item by the specified tag. it`s can be one of {@link MoreMenuConfig#GROUP_FILE},
     * {@link MoreMenuConfig#GROUP_PROTECT}, {@link MoreMenuConfig#GROUP_ANNOTATION} and {@link MoreMenuConfig#GROUP_FORM}
     * @param tag The specified tag that a menu item will be removed.
     */
    public void removeItem(int tag) {
        if (mMenuItems.size() > 0) {
            for (MenuItem item : mMenuItems) {
                if (item.getTag() == tag) {
                    mContentList_ly.removeView(item.getView());
                    mMenuItems.remove(item);
                    mItemVisibleMap.delete(tag);
                    break;
                }
            }
        }
    }

    void setItemVisibility(int visibility, int tag) {
        if (mMenuItems.size() > 0) {
            for (MenuItem item : mMenuItems) {
                if (item.getTag() == tag) {
                    item.getView().setVisibility(visibility);
                    break;
                }
            }
        }

        mItemVisibleMap.put(tag, visibility);
    }

    int getItemVisibility(int tag){
        if (mMenuItems.size() > 0) {
            for (MenuItem item : mMenuItems) {
                if (item.getTag() == tag) {
                    return item.getView().getVisibility();
                }
            }
        }
        return -1;
    }

    private void addItemToMenu(MenuItem item) {
        if (item.getView().getParent() != null) {
            ((ViewGroup) item.getView().getParent()).removeView(item.getView());
        }
        mContentList_ly.addView(item.getView(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        item.setDividerVisible(true);
    }

    /**
     * Returns the current menu group tag
     * @return the current menu group tag,
     *         see constants in {@link MoreMenuConfig}, such as: {@link MoreMenuConfig#GROUP_FILE}.
     */
    public int getTag() {
        return tag;
    }

    /**
     * Returns the root view of the current menu group.
     * @return the root view of the current menu group.
     */
    public View getView() {
        return mView;
    }

}
