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
package com.foxit.uiextensions.annots.multimedia.sound;


import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.impl.LifecycleEventListener;

public class SoundModule implements Module {
    private Context mContext;
    private PDFViewCtrl mPDFViewCtrl;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    private SoundAnnotHandler mAnnotHandler;

    public SoundModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        this.mContext = context;
        mPDFViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return MODULE_NAME_SOUND;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
            mAnnotHandler = new SoundAnnotHandler(mContext, mPDFViewCtrl);
            ((UIExtensionsManager) mUiExtensionsManager).registerAnnotHandler(mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerLifecycleListener(mLifecycleEventListener);
        }
        mPDFViewCtrl.registerDrawEventListener(mDrawEventListener);
        mPDFViewCtrl.registerDocEventListener(mDocEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterAnnotHandler(mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLifecycleListener(mLifecycleEventListener);
        }
        mPDFViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        mPDFViewCtrl.unregisterDocEventListener(mDocEventListener);
        return true;
    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {

        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            mAnnotHandler.onDrawForControls(canvas);
        }
    };

    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
        }

        @Override
        public void onDocWillClose(PDFDoc document) {
        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
            mAnnotHandler.release();
        }

        @Override
        public void onDocWillSave(PDFDoc document) {
        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {
        }
    };

    private ILifecycleEventListener mLifecycleEventListener = new LifecycleEventListener() {

        @Override
        public void onResume(Activity act) {
            mAnnotHandler.changeUIState();
        }

        @Override
        public void onHiddenChanged(boolean hidden) {
            if (!hidden) {
                mAnnotHandler.changeUIState();
            }
        }

        @Override
        public void onDestroy(Activity act) {
            mAnnotHandler.release();
        }
    };

}
