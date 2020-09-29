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
package com.foxit.uiextensions.security.trustcertificate;


import android.app.Activity;
import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.utils.UIToast;

public class TrustCertificateModule implements Module {

    private PDFViewCtrl mPDFViewCtrl;
    private Context mContext;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    private TrustCertUtil mTrusetCertUtil;

    public TrustCertificateModule(Context context, PDFViewCtrl pdfViewCtrl, UIExtensionsManager uiExtensionsManager) {
        this.mContext = context;
        mPDFViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return MODULE_NAME_TRUST_CERTIFICATE;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        mTrusetCertUtil = new TrustCertUtil(mContext, mPDFViewCtrl);
        return true;
    }

    public TrustCertUtil getTrusetCertUtil() {
        return mTrusetCertUtil;
    }

    @Override
    public boolean unloadModule() {
        return true;
    }

    public void show() {
        showTrustCertPanel();
    }

    private void showTrustCertPanel() {
        if (mUiExtensionsManager == null)
            return;

        if (((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().getCurrentAnnot() != null)
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().setCurrentAnnot(null);

        Activity activity = ((UIExtensionsManager) mUiExtensionsManager).getAttachedActivity();
        if (activity == null)
            return;

        if (!(activity instanceof FragmentActivity)) {
            UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.the_attached_activity_is_not_fragmentActivity));
            return;
        }

        FragmentActivity act = (FragmentActivity) activity;
        TrustCertificateListFragment trustCertPanel = (TrustCertificateListFragment) act.getSupportFragmentManager().findFragmentByTag("TrustCertPanel");
        if (trustCertPanel == null)
            trustCertPanel = new TrustCertificateListFragment();
        trustCertPanel.init(mPDFViewCtrl);

        AppDialogManager.getInstance().showAllowManager(trustCertPanel, act.getSupportFragmentManager(), "TrustCertPanel", null);
    }

}
