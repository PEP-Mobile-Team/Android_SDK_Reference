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
package com.foxit.uiextensions.modules;

import android.content.Context;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
/** The module provide the detailed info of the document..*/
public class DocInfoModule implements Module {
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private String mFilePath = null;
    private DocInfoView mDocInfo = null;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    public DocInfoModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    public DocInfoView getView() {
        return mDocInfo;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_DOCINFO;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }
        mDocInfo = new DocInfoView(mContext, mPdfViewCtrl);
        mDocInfo.init(mFilePath);
        return true;
    }

    @Override
    public boolean unloadModule() {
        return true;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String filePath) {
        mFilePath = filePath;
        if (mDocInfo != null)
            mDocInfo.init(mFilePath);
    }

}
