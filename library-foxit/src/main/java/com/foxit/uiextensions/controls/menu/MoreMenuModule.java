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
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotEventListener;
import com.foxit.uiextensions.annots.IRedactionEventListener;
import com.foxit.uiextensions.annots.form.FormFillerModule;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.security.standard.PasswordModule;

public class MoreMenuModule implements Module {
    private Context mContext;
    private PDFViewCtrl mPdfViewer;
    private ViewGroup mParent = null;
    private MoreMenuView mMoreMenuView = null;
    private boolean mHasFormFillerModule = false;
    private boolean mHasDocInfoModule = false;
    private FormFillerModule mFormFillerModule = null;

    //for password
    private boolean mHasPasswordModule = false;
    private PasswordModule mPasswordModule = null;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public MoreMenuModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewer, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewer = pdfViewer;
        mParent = parent;
        mUiExtensionsManager = uiExtensionsManager;
    }

    /**
     * Note: This method is only used within RDK
     */
    public MoreMenuView getView() {
        return mMoreMenuView;
    }

    /**
     * get more menu view
     *
     * @return {@link IMenuView}
     */
    public IMenuView getMenuView() {
        return mMoreMenuView.getMoreMenu();
    }

    @Override
    public String getName() {
        return Module.MODULE_MORE_MENU;
    }

    @Override
    public boolean loadModule() {
        if (mMoreMenuView == null) {
            mMoreMenuView = new MoreMenuView(mContext, mParent, mPdfViewer);
        }
        mMoreMenuView.initView();
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mUiExtensionsManager;

            uiExtensionsManager.registerModule(this);
            uiExtensionsManager.registerConfigurationChangedListener(mConfigurationChangedListener);
            uiExtensionsManager.getDocumentManager().registerAnnotEventListener(mAnnotEventListener);
            uiExtensionsManager.getDocumentManager().registerRedactionEventListener(mRedactionListener);
            uiExtensionsManager.registerLayoutChangeListener(mLayoutChangeListener);

            configDocInfoModule(uiExtensionsManager.getModuleByName(MODULE_NAME_DOCINFO));
            configFormFillerModule(uiExtensionsManager.getModuleByName(MODULE_NAME_FORMFILLER));
            configPasswordModule(uiExtensionsManager.getModuleByName(MODULE_NAME_PASSWORD));
        }

        if (mHasDocInfoModule) {
            mMoreMenuView.addDocInfoItem();
        }

        if (mHasPasswordModule) {
            mMoreMenuView.addPasswordItems(mPasswordModule);
        }

        mMoreMenuView.addAnnotItem();

        if (mHasFormFillerModule) {
            mMoreMenuView.addFormItem(mFormFillerModule);
        }

        mPdfViewer.registerDocEventListener(mDocumentEventListener);
        mPdfViewer.registerPageEventListener(mPageEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        mPdfViewer.unregisterDocEventListener(mDocumentEventListener);
        mPdfViewer.unregisterPageEventListener(mPageEventListener);

        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager)mUiExtensionsManager).unregisterConfigurationChangedListener(mConfigurationChangedListener);
            ((UIExtensionsManager)mUiExtensionsManager).getDocumentManager().unregisterAnnotEventListener(mAnnotEventListener);
            ((UIExtensionsManager)mUiExtensionsManager).getDocumentManager().unregisterRedactionEventListener(mRedactionListener);
            ((UIExtensionsManager)mUiExtensionsManager).unregisterLayoutChangeListener(mLayoutChangeListener);
        }

        mDocumentEventListener = null;
        mPageEventListener = null;
        return true;
    }

    /**
     * Note: This method is only used within RDK
     */
    public void setFilePath(String filePath) {
        mMoreMenuView.setFilePath(filePath);
    }

    private PDFViewCtrl.IDocEventListener mDocumentEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {

        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode != Constants.e_ErrSuccess) {
                return;
            }
            if (mHasFormFillerModule) {
                mMoreMenuView.reloadFormItems();
            }

            mMoreMenuView.reloadAnnotItems();
            mMoreMenuView.reloadDocInfoItems();

            if (mHasPasswordModule) {
                mMoreMenuView.reloadPasswordItem(mPasswordModule);

                try {
                    if (mPdfViewer.getDoc().getEncryptionType() == PDFDoc.e_EncryptPassword) {
                        mPasswordModule.getPasswordSupport().isOwner();
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onDocWillClose(PDFDoc document) {

        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
            if (errCode != Constants.e_ErrSuccess) {
                return;
            }

            if (mHasPasswordModule) {
                mPasswordModule.getPasswordSupport().setDocOpenAuthEvent(true);
                mPasswordModule.getPasswordSupport().setIsOwner(false);
            }
        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };

    private UIExtensionsManager.ConfigurationChangedListener mConfigurationChangedListener = new UIExtensionsManager.ConfigurationChangedListener() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            if (mMoreMenuView != null)
                mMoreMenuView.onConfigurationChanged(newConfig);
        }
    };

    private PDFViewCtrl.IPageEventListener mPageEventListener = new PDFViewCtrl.IPageEventListener() {
        @Override
        public void onPageVisible(int index) {

        }

        @Override
        public void onPageInvisible(int index) {

        }

        @Override
        public void onPageChanged(int oldPageIndex, int curPageIndex) {

        }

        @Override
        public void onPageJumped() {

        }

        @Override
        public void onPagesWillRemove(int[] pageIndexes) {

        }

        @Override
        public void onPageWillMove(int index, int dstIndex) {

        }

        @Override
        public void onPagesWillRotate(int[] pageIndexes, int rotation) {

        }

        @Override
        public void onPagesRemoved(boolean success, int[] pageIndexes) {

        }

        @Override
        public void onPageMoved(boolean success, int index, int dstIndex) {

        }

        @Override
        public void onPagesRotated(boolean success, int[] pageIndexes, int rotation) {

        }

        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] pageRanges) {
            if (success) {
                if (mHasFormFillerModule) {
                    mMoreMenuView.reloadFormItems();
                }
            }
        }

        @Override
        public void onPagesWillInsert(int dstIndex, int[] pageRanges) {

        }
    };

    private void configFormFillerModule(Module module) {
        if (module == null)
            return;
        mHasFormFillerModule = true;
        mFormFillerModule = (FormFillerModule) module;
    }

    private void configDocInfoModule(Module module) {
        if (module == null) {
            return;
        }
        mHasDocInfoModule = true;
    }

    private void configPasswordModule(Module module) {
        if (module == null) {
            return;
        }

        mHasPasswordModule = true;
        mPasswordModule = (PasswordModule) module;
    }

    private  AnnotEventListener mAnnotEventListener = new AnnotEventListener() {
        @Override
        public void onAnnotAdded(PDFPage page, Annot annot) {
            try {
                if (annot!= null && !annot.isEmpty()){
                    int type = annot.getType();
                    if (Annot.e_Widget == type && mHasFormFillerModule) {
                        mMoreMenuView.reloadFormItems();
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAnnotWillDelete(PDFPage page, Annot annot) {
        }

        @Override
        public void onAnnotDeleted(PDFPage page, Annot annot) {
            try {
                if (annot!= null && !annot.isEmpty()){
                    int type = annot.getType();
                    if (Annot.e_Widget == type && mHasFormFillerModule) {
                        mMoreMenuView.reloadFormItems();
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAnnotModified(PDFPage page, Annot annot) {
        }

        @Override
        public void onAnnotChanged(Annot lastAnnot, Annot currentAnnot) {
        }
    };

    private IRedactionEventListener mRedactionListener = new IRedactionEventListener() {
        @Override
        public void onAnnotWillApply(PDFPage page, Annot annot) {

        }

        @Override
        public void onAnnotApplied(PDFPage page, Annot annot) {
            mMoreMenuView.reloadFormItems();
        }
    };

    private ILayoutChangeListener mLayoutChangeListener = new ILayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
            mMoreMenuView.onLayoutChange(v, newWidth, newHeight, oldWidth, oldHeight);
        }
    };
}

