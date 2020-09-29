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
package com.foxit.uiextensions.modules.crop;

import android.content.Context;
import android.view.KeyEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.propertybar.IMultiLineBar;
/**  Crop module is for cutting white edge of PDF page, so the page content would become more prominent. */
public class CropModule implements Module {
    private Context mContext = null;
    private PDFViewCtrl mPdfViewCtrl = null;
    private ViewGroup mParent = null;
    private IMultiLineBar mSettingBar;
    private CropView mCropView;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public CropModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return MODULE_NAME_CROP;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
            mSettingBar = ((UIExtensionsManager)mUiExtensionsManager).getMainFrame().getSettingBar();
        }
        mPdfViewCtrl.registerRecoveryEventListener(memoryEventListener);
        mPdfViewCtrl.registerDocEventListener(docEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        mPdfViewCtrl.unregisterDocEventListener(docEventListener);
        mPdfViewCtrl.unregisterRecoveryEventListener(memoryEventListener);
        return true;
    }

    PDFViewCtrl.IDocEventListener docEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode != Constants.e_ErrSuccess) return;
            if (mPdfViewCtrl.isDynamicXFA()) {
                mSettingBar.enableBar(IMultiLineBar.TYPE_CROP, false);
                return;
            }

            mSettingBar.enableBar(IMultiLineBar.TYPE_CROP, true);
            registerMLListener();
            mCropView = new CropView(mContext, mParent, mPdfViewCtrl);
            mCropView.setSettingBar(mSettingBar);
            mCropView.changeState(false);
        }

        @Override
        public void onDocWillClose(PDFDoc document) {

        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
            exitCrop();
            unRegisterMLListener();
        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };

    public boolean isCropMode(){
        return mCropView != null && mCropView.isCropMode();
    }

    public void exitCrop(){
        if (mCropView != null) {
            if (mCropView.isShow()) {
                mCropView.closeCropView();
            }

            if (mCropView.isCropMode()) {
                mPdfViewCtrl.setCropMode(PDFViewCtrl.CROPMODE_NONE);
                mCropView.changeState(false);
            }
        }
    }

    public void restoreCrop(){
        if (mCropView != null) {
            int cropMode = mCropView.getLastCropMode();
            if (cropMode == PDFViewCtrl.CROPMODE_NONE) return;
            if (PDFViewCtrl.CROPMODE_CUSTOMIZE == cropMode) {
                mPdfViewCtrl.setCropRect(-1, mCropView.getLastCropRect());
            }
            mPdfViewCtrl.setCropMode(cropMode);
            mCropView.changeState(true);
        }
    }

    private void registerMLListener() {
        mSettingBar.registerListener(mCropChangeListener);
    }

    private void unRegisterMLListener() {
        mSettingBar.unRegisterListener(mCropChangeListener);
    }

    private PDFViewCtrl.IRecoveryEventListener memoryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
        @Override
        public void onWillRecover() {
           exitCrop();
        }

        @Override
        public void onRecovered() {
        }
    };

    private IMultiLineBar.IValueChangeListener mCropChangeListener = new IMultiLineBar.IValueChangeListener() {

        @Override
        public void onValueChanged(int type, Object value) {
            if (type == IMultiLineBar.TYPE_CROP) {
                mCropView.openCropView();
                ((UIExtensionsManager)mUiExtensionsManager).getMainFrame().hideSettingBar();
                if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() != null) {
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                }
            }
        }

        @Override
        public void onDismiss() {

        }

        @Override
        public int getType() {
            return IMultiLineBar.TYPE_CROP;
        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mCropView != null) {
            return mCropView.onKeyDown(keyCode, event);
        }
        return false;
    }

    public boolean onKeyBack() {
        if (mCropView != null) {
            return mCropView.onKeyBack();
        }
        return false;
    }
}
