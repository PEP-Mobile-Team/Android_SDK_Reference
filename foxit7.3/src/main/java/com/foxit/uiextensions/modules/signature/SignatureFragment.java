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
package com.foxit.uiextensions.modules.signature;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.controls.dialog.BaseDialogFragment;
import com.foxit.uiextensions.utils.AppDisplay;


public class SignatureFragment extends BaseDialogFragment {

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

    public void init(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, boolean isFromSignatureField) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mIsFromSignatureField = isFromSignatureField;
        mDisplay = AppDisplay.getInstance(mContext);
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
    protected View onCreateView(LayoutInflater inflater, ViewGroup container) {
        if (mSupport == null) {
            getActivity().getSupportFragmentManager().popBackStack();
            return null;
        }
        ViewGroup view = (ViewGroup) mSupport.getView().getParent();
        if (view != null)
            view.removeView(mSupport.getView());

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

    @NonNull
    @Override
    protected PDFViewCtrl getPDFViewCtrl() {
        return mPdfViewCtrl;
    }

    @Override
    protected void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
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
        if (mSupport != null) {
            mSupport.unInit();
        }
        mAttach = false;
    }

}
