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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Class <CODE>MenuViewImpl</CODE> represents the more menu
 * Now, the more menu only include file group, which contains a menu item "properties".
 */
public class MenuViewImpl implements IMenuView {
    private static final int VALUE_IF_KEY_NOT_FOUND= -1;

    private Context mContext;
    private View mView;
    private ArrayList<MenuGroup> mMenuGroups;

    private LinearLayout mMenuList_ly;
    private RelativeLayout mMenuTitleLayout;
    private BaseBar mMenuTitleBar;
    private ComparatorGroupByTag comparator;

    private SparseIntArray mGroupVisibleMap = new SparseIntArray();

    public interface MenuCallback {
        void onClosed();
    }

    private MenuCallback mCallback;

    public MenuViewImpl(Context context, MenuCallback callback) {
        mContext = context;
        mCallback = callback;
        mView = View.inflate(mContext, R.layout.view_menu_more, null);
        mMenuList_ly = (LinearLayout) mView.findViewById(R.id.menu_more_content_ly);
        mMenuGroups = new ArrayList<MenuGroup>();
        comparator = new ComparatorGroupByTag();

        initMenuTitleView();
    }

    private void initMenuTitleView() {
        mMenuTitleBar = new TopBarImpl(mContext);
        mMenuTitleBar.setBackgroundResource(R.color.ux_text_color_subhead_colour);
        BaseItemImpl mTitleTextItem = new BaseItemImpl(mContext);
        mTitleTextItem.setText(AppResource.getString(mContext.getApplicationContext(), R.string.fx_more_menu_title));
        mTitleTextItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimension(R.dimen.ux_text_height_title)));
        mTitleTextItem.setTextColorResource(R.color.ux_text_color_menu_light);
        if (AppDisplay.getInstance(mContext).isPad()) {
            mMenuTitleBar.addView(mTitleTextItem, BaseBar.TB_Position.Position_LT);
        } else {
            BaseItemImpl mMenuCloseItem = new BaseItemImpl(mContext);
            mMenuCloseItem.setImageResource(R.drawable.cloud_back);
            mMenuCloseItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCallback != null) {
                        mCallback.onClosed();
                    }
                }
            });
            mMenuTitleBar.addView(mMenuCloseItem, BaseBar.TB_Position.Position_LT);
            mMenuTitleBar.addView(mTitleTextItem, BaseBar.TB_Position.Position_LT);
        }
        mMenuTitleLayout = (RelativeLayout) mView.findViewById(R.id.menu_more_title_ly);
        mMenuTitleLayout.removeAllViews();
        mMenuTitleLayout.addView(mMenuTitleBar.getContentView());
    }

    @Override
    public void addMenuGroup(MenuGroup menuGroup) {
        if (menuGroup == null)
            return;

        for (MenuGroup group : mMenuGroups) {
            if (group.getTag() == menuGroup.getTag()) {
                return;
            }
        }

        int visibility = mGroupVisibleMap.get(menuGroup.getTag(),VALUE_IF_KEY_NOT_FOUND);
        if (VALUE_IF_KEY_NOT_FOUND != visibility) {

            if (View.VISIBLE == visibility) {
                menuGroup.getView().setVisibility(View.VISIBLE);
            } else if (View.INVISIBLE == visibility) {
                menuGroup.getView().setVisibility(View.INVISIBLE);
            } else {
                menuGroup.getView().setVisibility(View.GONE);
            }
        } else {
            mGroupVisibleMap.put(menuGroup.getTag(), menuGroup.getView().getVisibility());
        }

        mMenuGroups.add(menuGroup);
        Collections.sort(mMenuGroups, comparator);

        resetView();
    }

    private class ComparatorGroupByTag implements Comparator<Object> {
        @Override
        public int compare(Object lhs, Object rhs) {
            if (lhs instanceof MenuGroup && rhs instanceof MenuGroup) {
                MenuGroup lItem = (MenuGroup) lhs;
                MenuGroup rItem = (MenuGroup) rhs;
                return lItem.getTag() - rItem.getTag();
            } else {
                return 0;
            }
        }
    }

    @Override
    public void removeMenuGroup(int tag) {
        if (mMenuGroups.size() > 0) {
            for (MenuGroup group : mMenuGroups) {
                if (group.getTag() == tag) {
                    mMenuGroups.remove(group);
                    mMenuList_ly.removeView(group.getView());
                    mGroupVisibleMap.delete(group.getTag());
                    return;
                }
            }
        }
    }

    @Override
    public void setGroupVisibility(int visibility, int tag) {
        if (mMenuGroups.size() > 0) {
            for (MenuGroup group : mMenuGroups) {
                if (group.getTag() == tag) {
                    group.getView().setVisibility(visibility);
                    break;
                }
            }
        }

        mGroupVisibleMap.put(tag, visibility);
    }

    @Override
    public int getGroupVisibility(int tag) {
        if (mMenuGroups.size() > 0) {
            for (MenuGroup group : mMenuGroups) {
                if (group.getTag() == tag) {
                    return group.getView().getVisibility();
                }
            }
        }
        return  -1;
    }

    @Override
    public MenuGroup getMenuGroup(int tag) {
        if (mMenuGroups.size() > 0) {
            for (MenuGroup group : mMenuGroups) {
                if (group.getTag() == tag) {
                    return group;
                }
            }
        }

        return null;
    }

    @Override
    public void addMenuItem(int groupTag, MenuItem item) {
        if (mMenuGroups.size() > 0) {
            for (MenuGroup group : mMenuGroups) {
                if (group.getTag() == groupTag) {
                    //add item
                    group.addItem(item);
                    return;
                }
            }
        }
    }

    @Override
    public void removeMenuItem(int groupTag, int itemTag) {
        if (mMenuGroups.size() > 0) {
            for (MenuGroup group : mMenuGroups) {
                if (group.getTag() == groupTag) {
                    //remove
                    group.removeItem(itemTag);
                    break;
                }
            }
        }
    }

    @Override
    public void setItemVisibility(int visibility, int groupTag, int itemTag) {
        if (mMenuGroups.size() > 0) {
            for (MenuGroup group : mMenuGroups) {
                if (group.getTag() == groupTag) {
                    group.setItemVisibility(visibility, itemTag);
                    break;
                }
            }
        }
    }

    @Override
    public int getItemVisibility(int groupTag, int itemTag) {
        if (mMenuGroups.size() > 0) {
            for (MenuGroup group : mMenuGroups) {
                if (group.getTag() == groupTag) {
                    return group.getItemVisibility(itemTag);
                }
            }
        }
        return -1;
    }

    @Override
    public View getContentView() {
        return mView;
    }

    private void resetView() {
        mMenuList_ly.removeAllViews();
        for (MenuGroup group : mMenuGroups) {
            mMenuList_ly.addView(group.getView());
        }
    }
}
