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
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;


class SignatureViewController {

    private SignatureFragment.SignatureInkCallback mPSICallback;

    private SignatureViewGroup mViewGroup;
    private SignatureDrawView mDrawView;
    private Context mContext;
    private ViewGroup mParent;
    private PDFViewCtrl mPdfViewCtrl;
    private int mWidth;
    private int mHeight;
    private boolean mOnMoving;

    public SignatureViewController(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, SignatureFragment.SignatureInkCallback callback) {
        mPSICallback = callback;
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;

        mViewGroup = new SignatureViewGroup(mContext);
        mDrawView = new SignatureDrawView(mContext, parent, pdfViewCtrl);
        mDrawView.setOnDrawListener(mDrawListener);
    }

    SignatureDrawView drawView() {
        if (mDrawView == null) {
            mDrawView = new SignatureDrawView(mContext, mParent, mPdfViewCtrl);
            mDrawView.setOnDrawListener(mDrawListener);
        }
        return mDrawView;
    }

    public void init(int width, int height) {
        mWidth = width;
        mHeight = height;
        mViewGroup.init(mWidth, mHeight);
        drawView().init(width, height, null);
        mViewGroup.addView(mDrawView.getView());
    }

    public void init(int width, int height, final String key, final Bitmap bmp, final Rect rect, final int color, final float diameter, String dsgPath) {
        mWidth = width;
        mHeight = height;
        mViewGroup.init(mWidth, mHeight);
        drawView().init(width, height, key, bmp, rect, color, diameter, dsgPath);
        mViewGroup.addView(mDrawView.getView());
    }

    public void unInit() {
        if (mViewGroup.getChildCount() > 0) {
            mViewGroup.removeAllViews();
        }
        mDrawView.unInit();
        mDrawView = null;
    }

    public View getView() {
        return mViewGroup;
    }



    SignatureDrawView.OnDrawListener mDrawListener = new SignatureDrawView.OnDrawListener() {

        @Override
        public void result(Bitmap bitmap, Rect rect, int color, String dsgPath) {
            if (mPSICallback != null) {
                mPSICallback.onSuccess(true, bitmap, rect, color, dsgPath);
            }
        }

        @Override
        public void onBackPressed() {
            if (mPSICallback != null) {
                mPSICallback.onBackPressed();
            }
        }

        @Override
        public void moveToTemplate() {
            mOnMoving = true;
            mViewGroup.moveToTop(new SignatureViewGroup.IMoveCallBack() {

                @Override
                public void onStop() {
                    mOnMoving = false;
                }

                @Override
                public void onStart() {
                }
            });
        }

        @Override
        public boolean canDraw() {
            return !mOnMoving;
        }
    };

    public void resetLanguage() {
        if (mDrawView != null)
            mDrawView.resetLanguage();
    }

    public void setIsFromSignatureField(boolean isFromSignatureField) {
        if (mDrawView != null)
            mDrawView.setIsFromSignatureField(isFromSignatureField);
    }

    public void setActivity(Activity activity) {
        if (mDrawView != null)
            mDrawView.setActivity(activity);
    }

}
