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

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.security.ICertificateSupport;
import com.foxit.uiextensions.security.certificate.CertificateFileInfo;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.IResult;

import java.util.ArrayList;
import java.util.List;

public class TrustCertificatePresenter {
    public static final int e_ErrSuccess = 0;
    public static final int e_ErrTrustCertExisted = 1;

    private Context mContext;
    private PDFViewCtrl mPDFViewCtrl;
    private TrustCertUtil mTrustCertUtil;

    public TrustCertificatePresenter(Context context, PDFViewCtrl pdfViewCtrl) {
        this.mContext = context;
        this.mPDFViewCtrl = pdfViewCtrl;
        TrustCertificateModule module = (TrustCertificateModule) ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_TRUST_CERTIFICATE);
        if (module != null)
            mTrustCertUtil = module.getTrusetCertUtil();
    }

    public void loadTrustCert(IResult<List<CertificateFileInfo>, Object, Object> result) {
        SearchTrustCertTask trustCertTask = new SearchTrustCertTask(mTrustCertUtil, result);
        mPDFViewCtrl.addTask(trustCertTask);
    }

    private class SearchTrustCertTask extends Task {
        private boolean mRet;
        private List<CertificateFileInfo> mList = new ArrayList<>();
        private TrustCertUtil mTrustCertUtil;

        public SearchTrustCertTask(TrustCertUtil trustCertUtil, final IResult<List<CertificateFileInfo>, Object, Object> listener) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    listener.onResult(((SearchTrustCertTask) task).mRet, ((SearchTrustCertTask) task).mList, null, null);
                }
            });
            mTrustCertUtil = trustCertUtil;
        }

        @Override
        protected void execute() {
            if (mTrustCertUtil == null) {
                mRet = false;
                mList = null;
                return;
            }
            mList = mTrustCertUtil.getTrustCertList();
            mRet = true;
        }
    }

    public void viewCertInfo(CertificateFileInfo fileInfo) {
        if (mTrustCertUtil != null) {
            ICertificateSupport.CertificateInfo certificateInfo = new ICertificateSupport.CertificateInfo();
            certificateInfo.serialNumber = fileInfo.serialNumber;
            certificateInfo.name = fileInfo.certName;
            certificateInfo.issuer = fileInfo.issuer;
            certificateInfo.publisher = fileInfo.publisher;
            certificateInfo.usageCode = fileInfo.keyUsage;
            certificateInfo.startDate = fileInfo.validFrom;
            certificateInfo.expiringDate = fileInfo.validTo;
            certificateInfo.emailAddress = fileInfo.emailAddress;
            fileInfo.certificateInfo = certificateInfo;

            mTrustCertUtil.showCertDetailDialog(fileInfo);
        }
    }

    public void deleteTrustCert(CertificateFileInfo fileInfo, final Event.Callback callback) {
        if (mTrustCertUtil != null) {
            boolean success = mTrustCertUtil.removeTrustCert(fileInfo.serialNumber);
            if (callback != null)
                callback.result(null, success);
        }
    }

    public void addTrustCert(final Event.Callback callback) {
        if (mTrustCertUtil != null) {
            mTrustCertUtil.showCertDialog(new IResult<CertificateFileInfo, Object, Object>() {

                @Override
                public void onResult(boolean success, CertificateFileInfo p1, Object p2, Object p3) {
                    if (success) {
                        CertificateFileInfo certificateFileInfo = mTrustCertUtil.queryTrustCert(p1.certificateInfo.serialNumber);
                        if (certificateFileInfo != null) {
                            if (callback != null)
                                callback.result(new Event(e_ErrTrustCertExisted), false);
                        } else {
                            CertificateFileInfo fileInfo = new CertificateFileInfo();
                            fileInfo.filePath = p1.filePath;
                            fileInfo.fileName = p1.fileName;
                            fileInfo.password = p1.password;

                            fileInfo.serialNumber = p1.certificateInfo.serialNumber;
                            fileInfo.publisher = p1.certificateInfo.publisher;
                            fileInfo.issuer = p1.certificateInfo.issuer;
                            fileInfo.keyUsage = p1.certificateInfo.usageCode;
                            fileInfo.validFrom = p1.certificateInfo.startDate;
                            fileInfo.validTo = p1.certificateInfo.expiringDate;
                            fileInfo.emailAddress = p1.certificateInfo.emailAddress;
                            fileInfo.subject = p1.certificateInfo.subject;
                            fileInfo.certName = p1.certificateInfo.name;

                            boolean bRet = mTrustCertUtil.insertTrustCert(fileInfo);
                            if (callback != null)
                                callback.result(null, bRet);
                        }
                    }
                }
            });
        }
    }

    public void addTrustCert(CertificateFileInfo fileInfo, final Event.Callback callback) {
        CertificateFileInfo certificateFileInfo = mTrustCertUtil.queryTrustCert(fileInfo.serialNumber);
        if (certificateFileInfo != null) {
            if (callback != null)
                callback.result(new Event(e_ErrTrustCertExisted), false);
        } else {
            boolean bRet = mTrustCertUtil.insertTrustCert(fileInfo);
            if (callback != null)
                callback.result(null, bRet);
        }
    }

}
