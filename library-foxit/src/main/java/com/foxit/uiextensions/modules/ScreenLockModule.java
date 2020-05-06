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
package com.foxit.uiextensions.modules;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.UIDialog;
import com.foxit.uiextensions.controls.propertybar.IMultiLineBar;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.impl.LifecycleEventListener;
import com.foxit.uiextensions.pdfreader.impl.MainFrame;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppTheme;

import java.util.ArrayList;

import static com.foxit.uiextensions.controls.propertybar.IMultiLineBar.IValueChangeListener;
import static com.foxit.uiextensions.controls.propertybar.IMultiLineBar.TYPE_LOCKSCREEN;
/** Lock or unlock the screen. When the screen is locked, the view control will not rotate when the device is rotated.*/
public class ScreenLockModule implements Module {
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    private MainFrame mMainFrame;
    private Context mContext;
    private boolean mIsLockScreen = false;

    public ScreenLockModule(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mUiExtensionsManager = pdfViewCtrl.getUIExtensionsManager();
    }

    @Override
    public String getName() {
        return MODULE_NAME_SCREENLOCK;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerLifecycleListener(mLifecycleEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLifecycleListener(mLifecycleEventListener);
        }
        return true;
    }

    private void initApplyValue() {
        setOrientation(2);
        mMainFrame.getSettingBar().setProperty(IMultiLineBar.TYPE_LOCKSCREEN, mIsLockScreen);
    }

    private int mScreenOrientation;
    private void setOrientation(int orientation) {
        if (mMainFrame == null || mMainFrame.getAttachedActivity() == null) {
            return;
        }
        switch (orientation) {
            case 0:
                mIsLockScreen = true;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case 1:
                mIsLockScreen = true;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case 2:
                mIsLockScreen = false;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                break;
            default:
                break;
        }
    }

    public int getRequestedOrientation(){
        return mScreenOrientation;
    }

    public void setRequestedOrientation(int requestedOrientation){
        mScreenOrientation = requestedOrientation;
        mMainFrame.getAttachedActivity().setRequestedOrientation(requestedOrientation);
    }

    private IValueChangeListener mScreenLockListener = new IValueChangeListener() {
        @Override
        public void onValueChanged(int type, Object value) {
            if (TYPE_LOCKSCREEN == type) {
                final ScreenLockDialog dialog = new ScreenLockDialog(mMainFrame.getAttachedActivity());
                dialog.setCurOption(getScreenLockPosition());
                dialog.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (position == getScreenLockPosition()) {
                            return;
                        }
                        setOrientation(position);
                        mMainFrame.getSettingBar().setProperty(IMultiLineBar.TYPE_LOCKSCREEN, mIsLockScreen);
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        }

        @Override
        public void onDismiss() {

        }

        @Override
        public int getType() {
            return TYPE_LOCKSCREEN;
        }
    };

    private int getScreenLockPosition() {
        if (mMainFrame.getAttachedActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            return 0;
        } else if (mMainFrame.getAttachedActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            return 1;
        } else {
            return 2;
        }
    }

    private ILifecycleEventListener mLifecycleEventListener = new LifecycleEventListener() {
        @Override
        public void onCreate(Activity act, Bundle savedInstanceState) {
            mMainFrame = (MainFrame) ((UIExtensionsManager) mUiExtensionsManager).getMainFrame();
            mMainFrame.getSettingBar().registerListener(mScreenLockListener);
            initApplyValue();
        }

        @Override
        public void onDestroy(Activity act) {
            mMainFrame.getSettingBar().unRegisterListener(mScreenLockListener);
        }

    };

}

class ScreenLockDialog extends UIDialog {
    private ArrayList<String> mOptionList;
    private int mCurOption = -1;
    private ListView mScreenLockList;
    private final Context mContext;

    public ScreenLockDialog(Context context) {
        super(context, R.layout.screen_lock_dialog, AppTheme.getDialogTheme(), AppDisplay.getInstance(context).getUITextEditDialogWidth());
        mContext = context.getApplicationContext();
        mScreenLockList = (ListView) mContentView.findViewById(R.id.rd_screen_lock_listview);
        if (AppDisplay.getInstance(mContext).isPad()) {
            usePadDimes();
        }
        setTitle(AppResource.getString(mContext.getApplicationContext(), R.string.rv_screen_rotation_pad));
        mOptionList = new ArrayList<String>();
        mOptionList.add(AppResource.getString(mContext.getApplicationContext(), R.string.rv_screen_rotation_pad_landscape));
        mOptionList.add(AppResource.getString(mContext.getApplicationContext(), R.string.rv_screen_rotation_pad_portrait));
        mOptionList.add(AppResource.getString(mContext.getApplicationContext(), R.string.rv_screen_rotation_pad_auto));
        mScreenLockList.setAdapter(screenLockAdapter);
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        mScreenLockList.setOnItemClickListener(listener);
    }

    public void setCurOption(int position) {
        mCurOption = position;
        screenLockAdapter.notifyDataSetChanged();
    }

    private void usePadDimes() {
        try {
            ((LinearLayout.LayoutParams) mTitleView.getLayoutParams()).leftMargin = AppDisplay.getInstance(mContext).dp2px(24);
            ((LinearLayout.LayoutParams) mTitleView.getLayoutParams()).rightMargin = AppDisplay.getInstance(mContext).dp2px(24);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static class ViewHolder {
        public TextView optionName;
        public ImageView checkedCircle;
    }

    private BaseAdapter screenLockAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mOptionList.size();
        }

        @Override
        public String getItem(int position) {
            return mOptionList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(mContext, R.layout.screen_lock_item, null);
                holder.optionName = (TextView) convertView.findViewById(R.id.rd_screen_lock_textview);
                holder.checkedCircle = (ImageView) convertView.findViewById(R.id.rd_screen_lock_imageview);
                if (AppDisplay.getInstance(mContext).isPad()) {
                    ((RelativeLayout.LayoutParams) holder.optionName.getLayoutParams()).leftMargin = (int) AppResource.getDimension(mContext, R.dimen.ux_horz_left_margin_pad);
                    ((RelativeLayout.LayoutParams) holder.checkedCircle.getLayoutParams()).rightMargin = (int) AppResource.getDimension(mContext, R.dimen.ux_horz_right_margin_pad);
                }
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.optionName.setText(mOptionList.get(position));
            if (position == mCurOption) {
                holder.checkedCircle.setImageResource(R.drawable.rd_circle_checked);
            } else {
                holder.checkedCircle.setImageResource(R.drawable.rd_circle_normal);
            }
            if (position == mOptionList.size() - 1) {
                convertView.setBackgroundResource(R.drawable.dialog_button_background_selector);
            } else {
                convertView.setBackgroundResource(R.drawable.rd_menu_item_selector);
            }
            return convertView;
        }
    };
}
