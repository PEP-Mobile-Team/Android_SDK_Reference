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
package com.foxit.uiextensions.modules.thumbnail;

import android.app.Activity;
import android.content.Context;
import androidx.fragment.app.FragmentActivity;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.utils.UIToast;
/** All the page thumbnails are placed on a list view, and this is convenient for the user to add/edit/delete pages on this view..*/
public class ThumbnailModule implements Module {
    private final Context mContext;
    private final PDFViewCtrl mPdfView;
    private int mPageLayout = PDFViewCtrl.PAGELAYOUTMODE_SINGLE;//true:SINGLE_PAGE,false:CONTINUOUS_PAGE
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    public ThumbnailModule(Context context, PDFViewCtrl pdfView, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfView = pdfView;
        mUiExtensionsManager = uiExtensionsManager;
    }

    public void show() {
        showThumbnailDialog();
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_THUMBNAIL;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        return true;
    }

    @Override
    public boolean unloadModule() {
        return true;
    }

    private void showThumbnailDialog() {
        if (mUiExtensionsManager == null) {
            return;
        }

        if(((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().getCurrentAnnot() != null){
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().setCurrentAnnot(null);
        }

        Activity activity = ((UIExtensionsManager)mUiExtensionsManager).getAttachedActivity();
        if (activity == null) {
            return;
        }

        if (!(activity instanceof FragmentActivity)) {
            UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.the_attached_activity_is_not_fragmentActivity));
            return;
        }

        FragmentActivity act = (FragmentActivity) activity;
        ThumbnailSupport support = (ThumbnailSupport) act.getSupportFragmentManager().findFragmentByTag("ThumbnailSupport");
        if (support == null) {
            support = new ThumbnailSupport();
        }
        support.init(mPdfView);
     
        AppDialogManager.getInstance().showAllowManager(support, act.getSupportFragmentManager(), "ThumbnailSupport", null);
    }
}



