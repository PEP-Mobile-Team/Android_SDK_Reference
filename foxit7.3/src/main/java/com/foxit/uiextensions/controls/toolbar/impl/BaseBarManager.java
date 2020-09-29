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
package com.foxit.uiextensions.controls.toolbar.impl;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBarsHandler;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.pdfreader.impl.MainFrame;
import com.foxit.uiextensions.utils.AppDisplay;

public class BaseBarManager implements IBarsHandler {

    private MainFrame mMainFrame;
    private Context mContext;

    public BaseBarManager(Context context, MainFrame mainFrame) {
        mMainFrame = mainFrame;
        mContext = context;
    }

    @Override
    public boolean addItem(BarName barName, BaseBar.TB_Position gravity, IBaseItem item, int index) {
        if (null == barName || null == gravity || null == item) {
            return false;
        }

        if (BarName.TOP_BAR.equals(barName)) {
            item.setText(item.getText());
            return mMainFrame.getTopToolbar().addView(item, gravity, index);
        } else if (BarName.BOTTOM_BAR.equals(barName)) {
            item.setText(item.getText());
            return mMainFrame.getBottomToolbar().addView(item, gravity, index);
        }
        return false;
    }

    @Override
    public boolean addItem(BarName barName, BaseBar.TB_Position gravity, CharSequence text, int index, final IItemClickListener clickListener) {
        return addItem(barName, gravity, text, null, index, clickListener);
    }

    @Override
    public boolean addItem(BarName barName, BaseBar.TB_Position gravity, Drawable drawable, int index, final IItemClickListener clickListener) {
        return addItem(barName, gravity, "", drawable, index, clickListener);
    }

    @Override
    public boolean addItem(BarName barName, BaseBar.TB_Position gravity, int textId, int resId, int index, final IItemClickListener clickListener) {
        String text = "";
        if (textId > 0) {
            text = mContext.getString(textId);
        }

        Drawable drawable = null;
        if (resId > 0){
            drawable = mContext.getResources().getDrawable(resId);
        }
        return addItem(barName, gravity, text, drawable, index, clickListener);
    }

    @Override
    public int getItemsCount(BarName barName, BaseBar.TB_Position gravity) {
        if (null == barName || null == gravity) {
            return 0;
        }
        if (BarName.TOP_BAR.equals(barName)) {
            return mMainFrame.getTopToolbar().getItemsCount(gravity);
        } else if (BarName.BOTTOM_BAR.equals(barName)) {
            return mMainFrame.getBottomToolbar().getItemsCount(gravity);
        }
        return 0;
    }

    @Override
    public IBaseItem getItem(BarName barName, BaseBar.TB_Position gravity, int tag) {
        if (null == barName || null == gravity) {
            return null;
        }
        if (BarName.TOP_BAR.equals(barName)) {
            return mMainFrame.getTopToolbar().getItem(gravity, tag);
        } else if (BarName.BOTTOM_BAR.equals(barName)) {
            return mMainFrame.getBottomToolbar().getItem(gravity, tag);
        }
        return null;
    }

    @Override
    public IBaseItem getItemByIndex(BarName barName, BaseBar.TB_Position gravity, int index) {
        if (null == barName || null == gravity) {
            return null;
        }
        if (BarName.TOP_BAR.equals(barName)) {
            return mMainFrame.getTopToolbar().getItemByIndex(gravity, index);
        } else if (BarName.BOTTOM_BAR.equals(barName)) {
            return mMainFrame.getBottomToolbar().getItemByIndex(gravity, index);
        }
        return null;
    }

    @Override
    public void setVisibility(BarName barName, BaseBar.TB_Position gravity, int tag, int visibility) {
        if (null == barName || null == gravity) {
            return;
        }

        if (BarName.TOP_BAR.equals(barName)) {
            mMainFrame.getTopToolbar().setItemVisibility(gravity, tag, visibility);
        } else if (BarName.BOTTOM_BAR.equals(barName)) {
            mMainFrame.getBottomToolbar().setItemVisibility(gravity, tag, visibility);
        }
    }

    @Override
    public void setItemVisibility(BarName barName, BaseBar.TB_Position gravity, int index, int visibility) {
        if (null == barName || null == gravity) {
            return;
        }

        if (BarName.TOP_BAR.equals(barName)) {
            mMainFrame.getTopToolbar().setItemVisibilityByIndex(gravity, index, visibility);
        } else if (BarName.BOTTOM_BAR.equals(barName)) {
            mMainFrame.getBottomToolbar().setItemVisibilityByIndex(gravity, index, visibility);
        }
    }

    @Override
    public int getVisibility(BarName barName, BaseBar.TB_Position gravity, int tag) {
        if (null == barName || null == gravity) {
            return -1;
        }

        if (BarName.TOP_BAR.equals(barName)) {
            return mMainFrame.getTopToolbar().getItemVisibility(gravity, tag);
        } else if (BarName.BOTTOM_BAR.equals(barName)) {
            return mMainFrame.getBottomToolbar().getItemVisibility(gravity, tag);
        }
        return -1;
    }

    @Override
    public int getItemVisibility(BarName barName, BaseBar.TB_Position gravity, int index) {
        if (null == barName || null == gravity) {
            return -1;
        }

        if (BarName.TOP_BAR.equals(barName)) {
            return mMainFrame.getTopToolbar().getItemVisibilityByIndex(gravity, index);
        } else if (BarName.BOTTOM_BAR.equals(barName)) {
            return mMainFrame.getBottomToolbar().getItemVisibilityByIndex(gravity, index);
        }
        return -1;
    }

    @Override
    public boolean removeItem(BarName barName, BaseBar.TB_Position gravity, int index) {
        if (null == barName || null == gravity) {
            return false;
        }

        if (BarName.TOP_BAR.equals(barName)) {
            return mMainFrame.getTopToolbar().removeItemByIndex(gravity, index);
        } else if (BarName.BOTTOM_BAR.equals(barName)) {
            return mMainFrame.getBottomToolbar().removeItemByIndex(gravity, index);
        }
        return false;
    }

    @Override
    public boolean removeItem(BarName barName, BaseBar.TB_Position gravity, IBaseItem item) {
        if (null == barName || null == gravity) {
            return false;
        }

        if (BarName.TOP_BAR.equals(barName)) {
            return mMainFrame.getTopToolbar().removeItemByItem(item);
        } else if (BarName.BOTTOM_BAR.equals(barName)) {
            return mMainFrame.getBottomToolbar().removeItemByItem(item);
        }
        return false;
    }

    @Override
    public void removeAllItems(BarName barName) {
        if (null == barName)
            return;

        if (BarName.TOP_BAR.equals(barName)) {
            mMainFrame.getTopToolbar().removeAllItems();
        } else if (BarName.BOTTOM_BAR.equals(barName)) {
            mMainFrame.getBottomToolbar().removeAllItems();
        }
    }

    @Override
    public boolean addCustomToolBar(BarName barName, View view) {
        if (null == barName || null == view) {
            return false;
        }
        return mMainFrame.addCustomToolBar(barName, view);
    }

    @Override
    public boolean removeToolBar(BarName barName) {
        if (null == barName) {
            return false;
        }
        return mMainFrame.removeBottomBar(barName);
    }

    @Override
    public void enableToolBar(BarName barName, boolean enabled) {
        if (null == barName) {
            return;
        }

        if (BarName.TOP_BAR.equals(barName)) {
            mMainFrame.enableTopToolbar(enabled);
        } else if (BarName.BOTTOM_BAR.equals(barName)) {
            mMainFrame.enableBottomToolbar(enabled);
        }
    }

    @Override
    public void setBackgroundColor(BarName barName, int color) {
        if (null == barName) {
            return;
        }

        if (BarName.TOP_BAR.equals(barName)) {
            mMainFrame.getTopToolbar().setBackgroundColor(color);
        } else if (BarName.BOTTOM_BAR.equals(barName)) {
            mMainFrame.getBottomToolbar().setBackgroundColor(color);
        }
    }

    @Override
    public void setBackgroundResource(BarName barName, int resid) {
        if (null == barName) {
            return;
        }

        if (BarName.TOP_BAR.equals(barName)) {
            mMainFrame.getTopToolbar().setBackgroundResource(resid);
        } else if (BarName.BOTTOM_BAR.equals(barName)) {
            mMainFrame.getBottomToolbar().setBackgroundResource(resid);
        }
    }

    private boolean addItem(BarName barName, BaseBar.TB_Position gravity, CharSequence text, Drawable drawable, int index, final IItemClickListener clickListener) {
        if (null == barName || null == gravity) {
            return false;
        }

        if (BarName.TOP_BAR.equals(barName)) {
            BaseItemImpl item = new BaseItemImpl(mContext);
            item.setText(text);
            item.setImageDrawable(drawable);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (clickListener != null) {
                        clickListener.onClick(v);
                    }
                }
            });
            return mMainFrame.getTopToolbar().addView(item, gravity, index);
        } else if (BarName.BOTTOM_BAR.equals(barName)) {

            int textSize = mContext.getResources().getDimensionPixelSize(R.dimen.ux_text_height_toolbar);
            int textColorResId = R.color.ux_text_color_body2_dark;
            int interval = mContext.getResources().getDimensionPixelSize(R.dimen.ux_toolbar_button_icon_text_vert_interval);

            BaseItemImpl item = new BaseItemImpl(mContext.getApplicationContext());
            item.setText(text);
            item.setImageDrawable(drawable);
            item.setRelation(BaseItemImpl.RELATION_BELOW);
            item.setInterval(interval);
            item.setTextSize(AppDisplay.getInstance(mContext).px2dp(textSize));
            item.setTextColorResource(textColorResId);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (clickListener != null) {
                        clickListener.onClick(v);
                    }
                }
            });
            return mMainFrame.getBottomToolbar().addView(item, gravity, index);
        }
        return false;
    }

}
