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
package com.foxit.uiextensions.modules.print;


import android.content.Context;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
/**  This module provides function to print the PDF pages to wireless printer.*/
public class PrintModule implements Module {

    private PrintSettingOptions settingOptions;
    private PDFViewCtrl mPDFViewCtrl;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public PrintModule(Context context, PDFViewCtrl pdfViewCtrl, UIExtensionsManager uiExtensionsManager) {
        mPDFViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_PRINT;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        return true;
    }

    @Override
    public boolean unloadModule() {
        return true;
    }

    public void showPrintSettingOptions() {
        if (settingOptions == null) {
            settingOptions = new PrintSettingOptions(((UIExtensionsManager)mPDFViewCtrl.getUIExtensionsManager()).getAttachedActivity(), mPDFViewCtrl);
        }
        settingOptions.showDialog();
    }
}
