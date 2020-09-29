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
package com.foxit.uiextensions.annots;

import android.content.Context;
import android.widget.Toast;

import com.foxit.sdk.ActionCallback;
import com.foxit.sdk.IdentityProperties;
import com.foxit.sdk.MenuListArray;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Range;
import com.foxit.sdk.common.fxcrt.RectF;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.Signature;
import com.foxit.uiextensions.BuildConfig;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.utils.thread.AppThreadManager;

/**
 * Class that define the action handler associated with an annotation. Annotation such as Widget
 * annotation, may contains its own action, and this need to be handled when the use tap on it or input text on it.
 */
public class AnnotActionHandler extends ActionCallback {
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl = null;

    public AnnotActionHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        this.mContext = context;
        this.mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public void release() {
        mPdfViewCtrl = null;
    }

    @Override
    public boolean invalidateRect(PDFDoc document, int page_index, RectF pdf_rect) {
        return false;
    }

    @Override
    public int getCurrentPage(PDFDoc document) {
        return 0;
    }

    @Override
    public void setCurrentPage(PDFDoc document, int page_index) {

    }

    @Override
    public int getPageRotation(PDFDoc document, int page_index) {
        return 0;
    }

    @Override
    public boolean setPageRotation(PDFDoc document, int page_index, int rotation) {
        return false;
    }

    @Override
    public boolean executeNamedAction(PDFDoc document, String named_action) {
        return false;
    }

    @Override
    public boolean setDocChangeMark(PDFDoc document, boolean change_mark) {
        if (mPdfViewCtrl != null) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
        }
        return true;
    }

    @Override
    public boolean getDocChangeMark(PDFDoc document) {
        return true;
    }

    @Override
    public int getOpenedDocCount() {
        return 0;
    }

    @Override
    public PDFDoc getOpenedDoc(int index) {
        return null;
    }

    @Override
    public boolean beep(int type) {
        return false;
    }

    @Override
    public String response(String question, String title, String default_value, String label, boolean is_password) {
        return "";
    }

    @Override
    public String getFilePath(PDFDoc document) {
        return null;
    }

    @Override
    public boolean print(PDFDoc document, boolean is_ui, Range page_range, boolean is_silent, boolean is_shrunk_to_fit, boolean is_printed_as_image, boolean is_reversed, boolean is_to_print_annots) {
        return false;
    }

    @Override
    public boolean submitForm(PDFDoc document, byte[] form_data, String url) {
        AppThreadManager.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext.getApplicationContext(),
                        mContext.getApplicationContext().getString(R.string.unsupported_to_submit_form_tip), Toast.LENGTH_SHORT).show();
            }
        });
        return false;
    }

    @Override
    public boolean launchURL(String url) {
        AppThreadManager.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext.getApplicationContext(),
                        mContext.getApplicationContext().getString(R.string.unsupported_to_launch_url_tip), Toast.LENGTH_SHORT).show();
            }
        });
        return false;
    }

    @Override
    public String browseFile() {
        return "";
    }

    @Override
    public int getLanguage() {
        return e_LanguageENU;
    }

    @Override
    public int alert(final String msg, String title, int type, int icon) {
        AppThreadManager.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext.getApplicationContext(),
                        mContext.getApplicationContext().getString(R.string.action_alert_msg, msg), Toast.LENGTH_SHORT).show();
            }
        });

        return 1;
    }

    @Override
    public IdentityProperties getIdentityProperties() {
        IdentityProperties identityProperties = new IdentityProperties();
        identityProperties.setName("Foxit");

        return identityProperties;
    }

    @Override
    public String popupMenu(MenuListArray menus) {
        if (menus == null) {
            return "";
        }
        return menus.getAt(0).getName();
    }

    @Override
    public String getAppInfo(int type) {
        String info = "";
        switch (type) {
            case e_AppInfoTypeFormsVersion:
                info = "11";
                break;
            case e_AppInfoTypeViewerType:
                info = "Exchange-Pro";
                break;
            case e_AppInfoTypeViewerVariation:
                info = "Full";
                break;
            case e_AppInfoTypeViewerVersion:
                info = "11.007";
                break;
            case e_AppInfoTypeAppVersion:
                info = BuildConfig.VERSION_NAME;
                break;
            default:
        }
        return info;
    }

    @Override
    public boolean mailData(java.lang.Object data, boolean is_ui, String to, String subject, String cc, String bcc, String message) {
        return false;
    }
    
    @Override
    public int verifySignature(PDFDoc document, Signature pdf_signature) {
        return -1;
    }
}
