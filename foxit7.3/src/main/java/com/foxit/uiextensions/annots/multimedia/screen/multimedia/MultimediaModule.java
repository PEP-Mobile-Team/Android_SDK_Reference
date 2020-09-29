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
package com.foxit.uiextensions.annots.multimedia.screen.multimedia;


import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.config.Config;
import com.foxit.uiextensions.config.modules.annotations.AnnotationsConfig;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.impl.LifecycleEventListener;

public class MultimediaModule implements Module {

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    private MultimediaToolHandler mAudioToolHandler;
    private MultimediaToolHandler mVideoToolHandler;
    private MultimediaAnnotHandler mAnnotHandler;

    public MultimediaModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_MEDIA;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            Config config = ((UIExtensionsManager) mUiExtensionsManager).getConfig();
            AnnotationsConfig annotConfig = config.modules.getAnnotConfig();

            //audio
            if (annotConfig.isLoadAudio()) {
                mAudioToolHandler = new MultimediaToolHandler(mContext, mPdfViewCtrl, ToolHandler.TH_TYPE_SCREEN_AUDIO);
                ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mAudioToolHandler);
            }

            //video
            if (annotConfig.isLoadVideo()) {
                mVideoToolHandler = new MultimediaToolHandler(mContext, mPdfViewCtrl, ToolHandler.TH_TYPE_SCREEN_VIDEO);
                ((UIExtensionsManager) mUiExtensionsManager).registerToolHandler(mVideoToolHandler);
            }

            mAnnotHandler = new MultimediaAnnotHandler(mContext, mPdfViewCtrl);
            ((UIExtensionsManager) mUiExtensionsManager).registerAnnotHandler(mAnnotHandler);
            ((UIExtensionsManager) mUiExtensionsManager).registerLifecycleListener(mLifecycleEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }

        mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {

            if (mAudioToolHandler != null) {
                ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mAudioToolHandler);
            }
            if (mVideoToolHandler != null) {
                ((UIExtensionsManager) mUiExtensionsManager).unregisterToolHandler(mVideoToolHandler);
            }
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLifecycleListener(mLifecycleEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterAnnotHandler(mAnnotHandler);
        }

        mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        return true;
    }

    private PDFViewCtrl.IRecoveryEventListener memoryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
        @Override
        public void onWillRecover() {
            if (mAnnotHandler.getAnnotMenu() != null && mAnnotHandler.getAnnotMenu().isShowing()) {
                mAnnotHandler.getAnnotMenu().dismiss();
            }
        }

        @Override
        public void onRecovered() {
        }
    };

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
