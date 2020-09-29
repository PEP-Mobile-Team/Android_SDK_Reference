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


import android.content.Context;
import android.graphics.Bitmap;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.security.certificate.CertificateFileInfo;
import com.foxit.uiextensions.security.certificate.CertificateFragment;
import com.foxit.uiextensions.security.certificate.CertificateSupport;
import com.foxit.uiextensions.security.certificate.CertificateViewSupport;
import com.foxit.uiextensions.utils.IResult;
import com.foxit.uiextensions.utils.thread.AppThreadManager;

import java.util.List;

public class TrustCertUtil {

    private Context mContext;
    private PDFViewCtrl mPDFViewCtrl;
    public CertificateSupport mCertSupport;
    public CertificateViewSupport mViewSupport;

    public TrustCertUtil(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPDFViewCtrl = pdfViewCtrl;
        mCertSupport = new CertificateSupport(mContext);
        mViewSupport = new CertificateViewSupport(context, pdfViewCtrl, mCertSupport);
    }

    public List<CertificateFileInfo> getTrustCertList() {
        return mViewSupport.getDataSupport().getTrustCertList();
    }

    public boolean insertTrustCert(CertificateFileInfo trustCertInfo) {
        return mViewSupport.getDataSupport().insertTrustCert(trustCertInfo);
    }

    public CertificateFileInfo queryTrustCert(String serialNumber) {
        return mViewSupport.getDataSupport().queryTrustCert(serialNumber);
    }

    public boolean removeTrustCert(String serialNumber) {
        return mViewSupport.getDataSupport().removeTrustCert(serialNumber);
    }

    public void showCertDialog(final IResult<CertificateFileInfo, Object, Object> callback) {
        AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                mViewSupport.showAllPfxFileDialog(true, true, new CertificateFragment.ICertDialogCallback() {

                    @Override
                    public void result(boolean succeed, Object result, Bitmap forSign) {
                        if (succeed) {
                            callback.onResult(true, (CertificateFileInfo) result, null, null);
                        } else {
                            callback.onResult(false, null, null, null);
                            mViewSupport.dismissPfxDialog();
                        }
                    }
                });
            }
        });
    }

    public void showCertDetailDialog(final CertificateFileInfo fileInfo) {
        AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                mViewSupport.showPermissionDialog(fileInfo);
            }
        });
    }

}
