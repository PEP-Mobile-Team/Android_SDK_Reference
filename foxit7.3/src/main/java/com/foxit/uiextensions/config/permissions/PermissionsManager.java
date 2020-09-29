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
package com.foxit.uiextensions.config.permissions;


import android.content.Context;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Library;
import com.foxit.uiextensions.UIExtensionsManager;

public class PermissionsManager {

    private PDFViewCtrl mPDFViewCtrl;
    private Context mContext;
    private PermissionsConfig mPermissions;
    private UIExtensionsManager mUIExtensionsManager;

    public PermissionsManager(Context context, PDFViewCtrl pdfViewCtrl) {
        mPDFViewCtrl = pdfViewCtrl;
        mContext= context;
        mUIExtensionsManager = (UIExtensionsManager)(pdfViewCtrl.getUIExtensionsManager());
        mPermissions = mUIExtensionsManager.getConfig().permissions;
    }

    public void setPermissions(){
        boolean runJavaScript = mPermissions.runJavaScript;
        Library.enableJavaScript(runJavaScript);

        boolean disableLink = mPermissions.disableLink;
        mUIExtensionsManager.enableLinks(!disableLink);
    }

}
