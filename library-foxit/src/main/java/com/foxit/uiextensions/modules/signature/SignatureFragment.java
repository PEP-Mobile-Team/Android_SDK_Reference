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
package com.foxit.uiextensions.modules.signature;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.utils.AppDisplay;

import androidx.fragment.app.DialogFragment;


public class SignatureFragment extends DialogFragment {

    public interface SignatureInkCallback {
        void onSuccess(boolean isFromFragment, Bitmap bitmap, Rect rect, int color, String dsgPath);

        void onBackPressed();
    }

    private Context mContext;
    private ViewGroup mParent;
    private PDFViewCtrl mPdfViewCtrl;
    private SignatureViewController mSupport;
    private SignatureInkCallback mCallback;
    private int mOrientation;
    private boolean mAttach;
    private AppDisplay mDisplay;
    private SignatureInkItem mInkItem;
    private boolean mIsFromSignatureField = false;

    public boolean isAttached() {
        return mAttach;
    }

    public void setInkCallback(SignatureInkCallback callback) {
        this.mCallback = callback;
    }

    void setInkCallback(SignatureInkCallback callback, SignatureInkItem item) {
        this.mCallback = callback;
        mInkItem = item;
    }

    private boolean checkInit() {

        return mCallback != null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int theme;
        if (Build.VERSION.SDK_INT >= 21) {
            theme = android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen;
        } else if (Build.VERSION.SDK_INT >= 14) {
            theme = android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen;
        } else if (Build.VERSION.SDK_INT >= 13) {
            theme = android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen;
        } else {
            theme = android.R.style.Theme_Light_NoTitleBar_Fullscreen;
        }
        setStyle(STYLE_NO_TITLE, theme);

    }

    public void init(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, boolean isFromSignatureField) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mIsFromSignatureField = isFromSignatureField;
        mDisplay = AppDisplay.getInstance(mContext);

        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).registerLayoutChangeListener(mLayoutChangeListener);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mOrientation = activity.getRequestedOrientation();
        if (android.os.Build.VERSION.SDK_INT <= 8) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        if (!checkInit()) {
            getActivity().getSupportFragmentManager().popBackStack();
            return;
        }
        if (mSupport == null) {
            mSupport = new SignatureViewController(mContext, mParent, mPdfViewCtrl, mCallback);
        }
        mAttach = true;
    }

    private boolean mCheckCreateView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (mSupport == null) {
            getActivity().getSupportFragmentManager().popBackStack();
            return super.onCreateView(inflater, container, savedInstanceState);
        }
        ViewGroup view = (ViewGroup) mSupport.getView().getParent();
        if (view != null) {

            view.removeView(mSupport.getView());
        }

        this.getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    mCallback.onBackPressed();
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        mSupport.resetLanguage();
        mSupport.setIsFromSignatureField(mIsFromSignatureField);
        mSupport.setActivity(getActivity());
        mCheckCreateView = true;
        int screenWidth = mDisplay.getScreenWidth();
        int screenHeight = mDisplay.getScreenHeight();
        if (screenWidth < screenHeight) {
            screenWidth = mDisplay.getScreenHeight();
            screenHeight = mDisplay.getScreenHeight();
        }

        if (mInkItem == null) {
            mSupport.init(screenWidth, screenHeight);
        } else {
            mSupport.init(screenWidth,
                    screenHeight,
                    mInkItem.key,
                    mInkItem.bitmap,
                    mInkItem.rect,
                    mInkItem.color,
                    mInkItem.diameter,
                    mInkItem.dsgPath);
        }

        return mSupport.getView();
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshWindowLayout();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (!mCheckCreateView && mDisplay.getScreenWidth() > mDisplay.getScreenHeight()) {
            mCheckCreateView = true;

            if (mInkItem == null) {
                mSupport.init(mDisplay.getScreenWidth(), mDisplay.getScreenHeight());
            } else {
                mSupport.init(mDisplay.getScreenWidth(),
                        mDisplay.getScreenHeight(),
                        mInkItem.key,
                        mInkItem.bitmap,
                        mInkItem.rect,
                        mInkItem.color,
                        mInkItem.diameter,
                        mInkItem.dsgPath);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().setRequestedOrientation(mOrientation);
        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).unregisterLayoutChangeListener(mLayoutChangeListener);

        if (mSupport != null) {
            mSupport.unInit();
        }
        mAttach = false;
    }

    private ILayoutChangeListener mLayoutChangeListener = new ILayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
            if (null != getDialog() && getDialog().isShowing()){

                if (oldWidth != newWidth || oldHeight != newHeight) {
                    refreshWindowLayout();
                }
            }
        }
    };

    private void refreshWindowLayout(){
        View rootView = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView();
        int[] location = new int[2];
        rootView.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        int width = rootView.getWidth();
        int height = rootView.getHeight();

        Window window = getDialog().getWindow();
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowParams.dimAmount = 0.0f;
        windowParams.x = x;
        windowParams.y = y;
        windowParams.width = width;
        windowParams.height = height;
        window.setAttributes(windowParams);
        getDialog().setCanceledOnTouchOutside(true);
    }

}
