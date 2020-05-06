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
package com.foxit.uiextensions.annots.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.dialog.IPopupDialog;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class UIPopupFragment extends DialogFragment implements IPopupDialog {

    public static UIPopupFragment create(FragmentActivity act, View contentView, String fragName, boolean withMask, boolean withShadow) {
        UIPopupFragment popupDlg = new UIPopupFragment();
        popupDlg.init(act, contentView, fragName, withMask, withShadow);
        return popupDlg;
    }

    private FragmentActivity mAct;

    protected ViewGroup mRootView0;
    protected ViewGroup mRootViewWithShadow;
    private View mContentView;
    private String mFragName;
    private boolean mWithMask;
    private boolean mWithShadow;
    private int mAnimStyle;

    protected int mShadowWidth;
    private int mCornerWidth;

    protected int mWidth;
    protected int mHeight;
    private int mLocation;
    private Point mPosition = new Point();
    private boolean mIsShowing = false;
    private boolean mShowOnKeyboard = false;
    private Context mContext;

    private PopupWindow.OnDismissListener mOnDismissListener;

    void init(FragmentActivity act, View contentView, String fragName, boolean withMask, boolean withShadow) {
        mAct = act;
        mContext = act.getApplicationContext();
        mContentView = contentView;
        mFragName = fragName;
        mWithMask = withMask;
        mWithShadow = withShadow;

        mRootView0 = new RelativeLayout(mContext);
        mRootViewWithShadow = new RelativeLayout(mContext) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    float x = event.getX();
                    float y = event.getY();
                    if (x < mContentView.getLeft()
                            || x > mContentView.getRight()
                            || y < mContentView.getTop()
                            || y > mContentView.getBottom()) {
                        if (UIPopupFragment.this.isShowing()) {
                            UIPopupFragment.this.dismiss();
                        }
                    }
                }
                return super.onTouchEvent(event);
            }
        };

        if (withShadow) {
            mShadowWidth = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_popup_window_shadow_length);
            mCornerWidth = AppDisplay.getInstance(mContext).dp2px(2);
            mRootViewWithShadow.setBackgroundResource(R.drawable.popup_dialog_shadow);
            mRootViewWithShadow.setPadding(mShadowWidth, mShadowWidth/* + mCornerWidth */, mShadowWidth, mShadowWidth/* + mCornerWidth */);
        }

        removeViewFromParent(mContentView);
        mRootViewWithShadow.addView(mContentView, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        mRootView0.addView(mRootViewWithShadow);
    }

    private void removeViewFromParent(View view) {
        if (view != null && view.getParent() != null) {
            ViewGroup vg = (ViewGroup) view.getParent();
            for (int i = 0; i < vg.getChildCount(); i++) {
                if (vg.getChildAt(i) == view) {
                    vg.removeView(view);
                    break;
                }
            }
        }
    }

    public void setShowOnKeyboard(boolean flag) {
        mShowOnKeyboard = flag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mAct = getActivity();
        Dialog dialog = new Dialog(mAct, mWithMask ? R.style.rv_dialog_style : R.style.dialog_style_no_mask);

        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0x00FFFFFF));
        dialog.setCanceledOnTouchOutside(true);

        if (mShowOnKeyboard) {
            //dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
            //        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            dialog.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }

        if (mAnimStyle != 0) {
            dialog.getWindow().getAttributes().windowAnimations = mAnimStyle;
        }

        if (mRootView0 != null) {
            if (mRootView0.getParent() != null) {
                removeViewFromParent(mRootView0);
            }
            dialog.setContentView(mRootView0);
        }
        setDialogPositionAndSize(dialog);
        return dialog;
    }

    @Override
    public View getRootView() {
        return mRootView0;
    }

    @Override
    public void setupDrawerNavLayout(RelativeLayout navLayout) {

    }

    @Override
    public int getShadowLength() {
        return mShadowWidth;
    }

    @Override
    public void setOnDismissListener(PopupWindow.OnDismissListener onDismissListener) {
        mOnDismissListener = onDismissListener;
    }

    private void setDialogPositionAndSize(Dialog dialog) {
        if (dialog == null)
            return;
        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(mLocation);
            WindowManager.LayoutParams lp = window.getAttributes();
            if (lp != null) {
                lp.x = mPosition.x;
                lp.y = mPosition.y;
                lp.width = mWidth;
                lp.height = mHeight;
                window.setAttributes(lp);
            }
        }
    }

    @Override
    public void setWidth(int width) {
        mWidth = width;
        setDialogPositionAndSize(getDialog());
    }

    @Override
    public void setHeight(int height) {
        mHeight = height;
        setDialogPositionAndSize(getDialog());
    }

    @Override
    public void update(int width, int height) {
        mWidth = width;
        mHeight = height;
        setDialogPositionAndSize(getDialog());
    }

    @Override
    public void update(int x, int y, int width, int height) {
        mPosition.x = x;
        mPosition.y = y;
        mWidth = width;
        mHeight = height;
        setDialogPositionAndSize(getDialog());
    }

    @Override
    public void showAtLocation(View parent, int gravity, int x, int y) {
        if ((gravity & Gravity.RIGHT) != 0 && x < 0) {
            mRootView0.setScrollX(x);
            x = 0;
        } else {
            mRootView0.setScrollX(0);
        }

        if ((gravity & Gravity.BOTTOM) != 0 && y < 0) {
            mRootView0.setScrollY(y);
            x = 0;
        } else {
            mRootView0.setScrollY(0);
        }

        mLocation = gravity;
        mPosition.x = x;
        mPosition.y = y;
        setDialogPositionAndSize(getDialog());

        if (!isShowing()) {
            FragmentManager fm = mAct.getSupportFragmentManager();
            if (showInner(fm, this, mFragName)) {
                mIsShowing = true;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mIsShowing = false;
        if (mOnDismissListener != null) {
            mOnDismissListener.onDismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsShowing = false;
    }

    @Override
    public boolean isShowing() {
        return mIsShowing;
    }

    @Override
    public void dismiss() {
        try {
            super.dismissAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mIsShowing = false;
    }

    private boolean showInner(FragmentManager manager, DialogFragment fragment, String tag) {
        if (fragment == null || mContentView == null)
            return false;

        try {
            super.show(manager, tag);
        } catch (Exception e) {
            e.printStackTrace();

            try {
                FragmentTransaction transaction = manager.beginTransaction();
                Fragment targetFragment = manager.findFragmentByTag(tag);
                if (targetFragment != null) {
                    transaction.remove(targetFragment);
                }
                transaction.add(fragment, tag);
                transaction.commitAllowingStateLoss();
            } catch (Exception e2) {
                e2.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private void removeInner(FragmentManager manager, DialogFragment fragment, String tag) {
        if (fragment == null || mContentView == null)
            return;
        try {
            FragmentTransaction transaction = manager.beginTransaction();
            Fragment targetFragment = manager.findFragmentByTag(tag);
            if (targetFragment != null) {
                transaction.remove(targetFragment);
            }
            transaction.commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setAnimationStyle(int animationStyle) {
        mAnimStyle = animationStyle;
        Dialog dlg = getDialog();
        if (dlg == null)
            return;
        dlg.getWindow().getAttributes().windowAnimations = animationStyle;
    }
}