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
package com.foxit.uiextensions.security.standard;

import android.content.Context;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;

public class PasswordModule implements Module {
    private PasswordSecurityHandler mSecurityHandler = null;
    private PasswordStandardSupport mSupport = null;

    private PDFViewCtrl mPdfViewCtrl;
    private Context mContext;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    public PasswordModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return MODULE_NAME_PASSWORD;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        mSupport = new PasswordStandardSupport(mContext, mPdfViewCtrl);
        mSecurityHandler = new PasswordSecurityHandler(mSupport, mPdfViewCtrl);

        return true;
    }

    @Override
    public boolean unloadModule() {
        mSupport = null;
        return true;
    }

    public PasswordStandardSupport getPasswordSupport() {
        return mSupport;
    }

    public PasswordSecurityHandler getSecurityHandler() {
        return mSecurityHandler;
    }
}
