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
package com.foxit.uiextensions.security.digitalsignature;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.common.Library;
import com.foxit.sdk.common.Progressive;
import com.foxit.sdk.pdf.LTVVerifier;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.Signature;
import com.foxit.sdk.pdf.SignatureVerifyResult;
import com.foxit.sdk.pdf.SignatureVerifyResultArray;
import com.foxit.sdk.pdf.annots.Widget;
import com.foxit.sdk.pdf.interform.Control;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.dialog.FxDialog;
import com.foxit.uiextensions.controls.dialog.FxProgressDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.security.KeyUsageUtil;
import com.foxit.uiextensions.security.certificate.CertificateFileInfo;
import com.foxit.uiextensions.security.trustcertificate.CertificateViewerFragment;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppTheme;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.IResult;
import com.foxit.uiextensions.utils.UIToast;

import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.util.Store;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

import androidx.fragment.app.FragmentActivity;


public class DigitalSignatureSecurityHandler {

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private FxProgressDialog mProgressDialog;
    private Event.Callback mCallback;
    private boolean mSuccess;
    private int mSigState = 0;
    private boolean mIsFileChanged = false;

    private boolean mUseLtvVerify = true;

    //ltv
    private boolean mIsVerifySig = true;
    private boolean mUseExpire = true;
    private boolean mIgnoreDocInfo = false;
    private int mSigTimeType = LTVVerifier.e_SignatureCreationTime;
    private int mVerifyMode = LTVVerifier.e_VerifyModeAcrobat;

    private int mLtvState = 0;
    private String mTrustCertificateInfo = "";
    private String mCertInfoForVerify = "";
    private DigitalSignatureUtil mDSUtil;

    public DigitalSignatureSecurityHandler(Context context, PDFViewCtrl pdfViewCtrl, DigitalSignatureUtil util) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mDSUtil = util;
    }


    class AddSignatureTask extends Task {
        private int mPageIndex;
        private Bitmap mBitmap;
        private RectF mRect;
        private CertificateFileInfo mInfo;
        private String mDocPath;
        private Signature mSignature;
        private boolean mIsCustom = false;

        private AddSignatureTask(String docPath, CertificateFileInfo info, int pageIndex) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }

                    if (mCallback != null) {
                        mCallback.result(null, mSuccess);
                    }
                }
            });
            mDocPath = docPath;
            mInfo = info;
            mPageIndex = pageIndex;
        }

        public AddSignatureTask(String docPath, CertificateFileInfo info, int pageIndex, Bitmap bitmap, RectF rect) {
            this(docPath, info, pageIndex);
            mBitmap = bitmap;
            mRect = rect;
        }

        public AddSignatureTask(String docPath, CertificateFileInfo info, int pageIndex, Signature signature, boolean isCustom) {
            this(docPath, info, pageIndex);
            mSignature = signature;
            mIsCustom = isCustom;
        }


        @Override
        protected void execute() {
            try {
                mSuccess = false;
                String filter = "Adobe.PPKLite";
                String subfilter = "adbe.pkcs7.detached";
                String dn = "dn";
                String location = "location";
                String reason = "reason";
                String contactInfo = "contactInfo";
                String signer = "signer";
                String text = "text";
                long state = 0;

                //set current time to dateTime.
                DateTime dateTime = new DateTime();
                Calendar c = Calendar.getInstance();
                TimeZone timeZone = c.getTimeZone();
                int offset = timeZone.getRawOffset();
                int tzHour = offset / (3600 * 1000);
                int tzMinute = (offset / (1000 * 60)) % 60;
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH) + 1;
                int day = c.get(Calendar.DATE);
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);
                int second = c.get(Calendar.SECOND);
                dateTime.set(year, month, day, hour, minute, second, 0, (short) tzHour, tzMinute);


                PDFPage pdfPage = mPdfViewCtrl.getDoc().getPage(mPageIndex);
                Signature signature;
                boolean isSignatureEmpty = mSignature == null || mSignature.isEmpty();
                if (isSignatureEmpty) {
                    signature = pdfPage.addSignature(AppUtil.toFxRectF(mRect));
                    signature.setBitmap(mBitmap);
                } else {
                    signature = mSignature;
                }

                signature.setFilter(filter);
                signature.setSubFilter(subfilter);
                signature.setKeyValue(Signature.e_KeyNameDN, dn);
                signature.setKeyValue(Signature.e_KeyNameLocation, location);
                signature.setKeyValue(Signature.e_KeyNameReason, reason);
                signature.setKeyValue(Signature.e_KeyNameContactInfo, contactInfo);
                signature.setKeyValue(Signature.e_KeyNameSigner, signer);
                signature.setKeyValue(Signature.e_KeyNameText, text);
                signature.setSignTime(dateTime);

                long flags;
                if (isSignatureEmpty || mIsCustom) {
                    flags = Signature.e_APFlagBitmap;
                } else {
                    flags = Signature.e_APFlagLabel | Signature.e_APFlagSigner | Signature.e_APFlagReason
                            | Signature.e_APFlagDN | Signature.e_APFlagLocation | Signature.e_APFlagText
                            | Signature.e_APFlagSigningTime;
                }
                signature.setAppearanceFlags((int) flags);
                Progressive progressive = signature.startSign(mInfo.filePath, mInfo.password.getBytes(), Signature.e_DigestSHA1, mDocPath, null, null);
                int progress = Progressive.e_ToBeContinued;
                while (progress == Progressive.e_ToBeContinued) {
                    progress = progressive.resume();
                }
                if (progress == Progressive.e_Error) {
                    if (isSignatureEmpty) {
                        mPdfViewCtrl.getDoc().removeSignature(signature);
                    }
                    return;
                }

                state = signature.getState();
                if (state != Signature.e_StateSigned || !signature.isSigned()) return;
                mSuccess = true;
            } catch (PDFException e) {
                mSuccess = false;
            }
        }
    }

    public void addSignature(final String docPath, final CertificateFileInfo info, final Bitmap bitmap, int pageIndex, final RectF rect, Event.Callback callback) {
        if (!showSignatureProgressDialog()) return;
        mCallback = callback;

        mPdfViewCtrl.addTask(new AddSignatureTask(docPath, info, pageIndex, bitmap, rect));

    }

    public void addSignature(final String docPath, final CertificateFileInfo info, int pageIndex, Signature signature, boolean isCustom, Event.Callback callback) {
        if (!showSignatureProgressDialog()) return;
        mCallback = callback;

        mPdfViewCtrl.addTask(new AddSignatureTask(docPath, info, pageIndex, signature, isCustom));

    }

    private boolean showSignatureProgressDialog() {
        if (mPdfViewCtrl.getUIExtensionsManager() == null) return false;
        Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return false;

        mProgressDialog = new FxProgressDialog(context, context.getApplicationContext().getString(R.string.rv_sign_waiting));
        mProgressDialog.show();
        return true;
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    class VerifySignatureTask extends Task {
        private Signature mSignature;
        private CertificateFileInfo mVerifyResultInfo;
        private List<CertificateFileInfo> mVerifyCertList = new ArrayList<>();

        public VerifySignatureTask(final Signature signature, final IResult<List<CertificateFileInfo>, CertificateFileInfo, Object> callback) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    if (callback != null) {
                        callback.onResult(true, ((VerifySignatureTask) task).mVerifyCertList, ((VerifySignatureTask) task).mVerifyResultInfo, null);
                    }
                }
            });
            mSignature = signature;
        }

        @Override
        protected void execute() {
            try {
                mUseLtvVerify = !Library.isFipsMode();
            } catch (PDFException e) {
                mUseLtvVerify = true;
            }

            if (mUseLtvVerify) {
                doLTVVerify();
            } else {
                doNormalVerify();
            }
        }

        boolean hasModifiedDocument() {
            try {
                int[] byteRanges = new int[4];
                mSignature.getByteRangeArray(byteRanges);
                long fileLength = mPdfViewCtrl.getDoc().getFileSize();

                return fileLength != (byteRanges[2] + byteRanges[3]);
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return false;
        }

        private void doNormalVerify() {
            try {
                if (mSignature == null) return;
                Progressive progressive = mSignature.startVerify(null, null);
                int state = Progressive.e_ToBeContinued;
                while (state == Progressive.e_ToBeContinued) {
                    state = progressive.resume();
                }

                mSigState = mSignature.getState();
                mIsFileChanged = hasModifiedDocument();
            } catch (PDFException e) {
                e.printStackTrace();
                UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_error));
            }
        }

        private void doLTVVerify() {
            try {
                if (mSignature == null) return;
                LTVVerifier ltvVerifier = new LTVVerifier(mPdfViewCtrl.getDoc(), mIsVerifySig, mUseExpire, mIgnoreDocInfo, mSigTimeType);
                ltvVerifier.setVerifyMode(mVerifyMode);
                byte[] sigContent = mSignature.getSignatureDict().getElement("Contents").getString();

                try {
                    CMSSignedData data = new CMSSignedData(sigContent);
                    mVerifyResultInfo = getCertificateVerifyInfo(data);
                    mVerifyCertList = getCertificateList(data);
                } catch (CMSException e) {
                    e.printStackTrace();
                }
                ltvVerifier.setTrustedCertStoreCallback(new FxTrustedCertStoreCallback(mDSUtil));
                SignatureVerifyResultArray resultArray = ltvVerifier.verifySignature(mSignature);
                if (resultArray.getSize() == 0) {
                    UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_error));
                }
                SignatureVerifyResult verifyResult = resultArray.getAt(0);
                mSigState = verifyResult.getSignatureState();
                mLtvState = verifyResult.getLTVState();
                mIsFileChanged = hasModifiedDocument();
                mCertInfoForVerify = getCertificateInformationForVerify(mVerifyResultInfo);
            } catch (PDFException e) {
                e.printStackTrace();
                UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_error));
            }
        }

        private CertificateFileInfo getCertificateVerifyInfo(CMSSignedData data) {
            Store<X509CertificateHolder> x509CertificateHolderStore = data.getCertificates();
            SignerInformationStore signerInformationStore = data.getSignerInfos();
            for (SignerInformation signerInformation : signerInformationStore.getSigners()) {
                @SuppressWarnings("unchecked")
                Collection cert = x509CertificateHolderStore.getMatches(signerInformation.getSID());
                int certCount = cert.size();
                if (certCount > 0) {
                    X509CertificateHolder holder = (X509CertificateHolder) cert.iterator().next();
                    return getCertificateInfo(holder);
                }
            }
            return null;
        }

        private List<CertificateFileInfo> getCertificateList(CMSSignedData data) {
            List<CertificateFileInfo> infos = new ArrayList<>();
            Store<X509CertificateHolder> x509CertificateHolderStore = data.getCertificates();
            Collection certs = x509CertificateHolderStore.getMatches(null);
            for (Object o : certs) {
                X509CertificateHolder certificateHolder = (X509CertificateHolder) o;
                CertificateFileInfo vertifyInfo = getCertificateInfo(certificateHolder);
                infos.add(vertifyInfo);
            }

            if (infos.size() > 1)
                infos = sortCertificateList(infos);
            return infos;
        }

        private List<CertificateFileInfo> sortCertificateList(List<CertificateFileInfo> infos) {
            CertificateFileInfo rootCertInfo = null;
            for (CertificateFileInfo info : infos) {
                boolean isRoot = true;
                for (CertificateFileInfo info2 : infos) {
                    if (info == info2)
                        continue;
                    if (info.issuer.equals(info2.subject)) {
                        isRoot = false;
                    }
                }

                if (isRoot) {
                    rootCertInfo = info;
                    break;
                }
            }

            if (rootCertInfo == null)
                rootCertInfo = infos.get(0);

            List<CertificateFileInfo> sortInfos = new ArrayList<>();
            List<CertificateFileInfo> tempFileInfos = new ArrayList<>(infos);
            while (rootCertInfo != null) {
                sortInfos.add(rootCertInfo);
                tempFileInfos.remove(rootCertInfo);
                rootCertInfo = getChildCertFileInfo(rootCertInfo, tempFileInfos);
            }
            return sortInfos;
        }

        private CertificateFileInfo getChildCertFileInfo(CertificateFileInfo parentFileInfo, List<CertificateFileInfo> infos) {
            for (CertificateFileInfo info : infos) {
                if (parentFileInfo == info)
                    continue;

                if (parentFileInfo.subject.equals(info.issuer)) {
                    return info;
                }
            }
            return null;
        }

        private CertificateFileInfo getCertificateInfo(X509CertificateHolder x509CertificateHolder) {
            if (x509CertificateHolder == null) return null;

            CertificateFileInfo certificateFileInfo = new CertificateFileInfo();
            certificateFileInfo.issuer = AppUtil.getEntryName(x509CertificateHolder.getIssuer().toString(), "CN=");
            certificateFileInfo.publisher = AppUtil.getEntryName(x509CertificateHolder.getIssuer().toString(), "CN=");
            BigInteger bigInteger = x509CertificateHolder.getSerialNumber();
            if (bigInteger.compareTo(BigInteger.ZERO) < 0) {
                bigInteger = new BigInteger(1, bigInteger.toByteArray());
            }
            String sn = bigInteger.toString(16).toUpperCase();
            certificateFileInfo.serialNumber = sn;
            certificateFileInfo.isTrustCert = mDSUtil.getCertDataSupport().queryTrustCert(sn) != null;
            certificateFileInfo.emailAddress = AppUtil.getEntryName(x509CertificateHolder.getSubject().toString(), "E=");
            certificateFileInfo.validFrom = AppDmUtil.getLocalDateString(x509CertificateHolder.getNotBefore());
            certificateFileInfo.validTo = AppDmUtil.getLocalDateString(x509CertificateHolder.getNotAfter());
            certificateFileInfo.keyUsage = getIntendedKeyUsage(x509CertificateHolder);
            certificateFileInfo.subject = AppUtil.getEntryName(x509CertificateHolder.getSubject().toString(), "CN=");
            StringBuilder sb = new StringBuilder();
            sb.append(AppUtil.getEntryName(x509CertificateHolder.getSubject().toString(), "CN="));
            String email = AppUtil.getEntryName(x509CertificateHolder.getSubject().toString(), "E=");
            if (!TextUtils.isEmpty(email)) {
                sb.append(" <");
                sb.append(email);
                sb.append("> ");
            }
            certificateFileInfo.certName = sb.toString();
            return certificateFileInfo;

        }

        int getIntendedKeyUsage(X509CertificateHolder x509CertificateHolder) {
            if (x509CertificateHolder == null) return KeyUsageUtil.unknowon;

            int usageCode = 0;
            KeyUsage keyUsage = KeyUsage.fromExtensions(x509CertificateHolder.getExtensions());
            if (keyUsage != null) {
                if (keyUsage.hasUsages(KeyUsage.dataEncipherment)) {
                    usageCode = usageCode | KeyUsageUtil.dataEncipherment;
                }

                if (keyUsage.hasUsages(KeyUsage.digitalSignature)) {
                    usageCode = usageCode | KeyUsageUtil.digitalSignature;
                }

                if (keyUsage.hasUsages(KeyUsage.keyAgreement)) {
                    usageCode = usageCode | KeyUsageUtil.keyAgreement;
                }

                if (keyUsage.hasUsages(KeyUsage.keyCertSign)) {
                    usageCode = usageCode | KeyUsageUtil.keyCertSign;
                }

                if (keyUsage.hasUsages(KeyUsage.keyEncipherment)) {
                    usageCode = usageCode | KeyUsageUtil.keyEncipherment;
                }

                if (keyUsage.hasUsages(KeyUsage.nonRepudiation)) {
                    usageCode = usageCode | KeyUsageUtil.nonRepudiation;
                }

                if (keyUsage.hasUsages(KeyUsage.cRLSign)) {
                    usageCode = usageCode | KeyUsageUtil.cRLSign;
                }
            }
            return usageCode;
        }

        private String getCertificateInformationForVerify(CertificateFileInfo verifyResultInfo) {
            if (verifyResultInfo == null) return "";
            String issuer = verifyResultInfo.issuer;
            String sn = verifyResultInfo.serialNumber;
            String email = verifyResultInfo.emailAddress;
            String startDate = verifyResultInfo.validFrom;
            String expiredDate = verifyResultInfo.validTo;
            String content = mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_publisher) + issuer + "\n";
            content += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_serialNumber) + sn + "\n";
            content += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_emailAddress) + email + "\n";
            content += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_validityStarts) + startDate + "\n";
            content += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_validityEnds) + expiredDate + "\n";
            return content;
        }
    }

    public void verifySignature(final Signature signature) {
        if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
        Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return;
        mProgressDialog = new FxProgressDialog(context, mContext.getApplicationContext().getString(R.string.rv_sign_waiting));
        mProgressDialog.show();

        mPdfViewCtrl.addTask(new VerifySignatureTask(signature, new IResult<List<CertificateFileInfo>, CertificateFileInfo, Object>() {
            @Override
            public void onResult(boolean success, List<CertificateFileInfo> p1, CertificateFileInfo p2, Object p3) {
                if (mUseLtvVerify) {
                    showVerifyResult(signature, p1);
                } else {
                    showNormalVerifyResult(signature);
                }
            }
        }));
    }

    private UITextEditDialog mVerifyResultDialog;

    private void showVerifyResult(Signature signature, final List<CertificateFileInfo> fileInfos) {
        dismissProgressDialog();
        if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
        final Activity context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return;

        String resultText = "";
        if ((mSigState & Signature.e_StateVerifyValid) == Signature.e_StateVerifyValid) {
            if (mIsFileChanged) {
                resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_perm) + "\n\n";
            } else {
                resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_valid) + "\n\n";
            }
        } else if ((mSigState & Signature.e_StateVerifyInvalid) == Signature.e_StateVerifyInvalid) {
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_invalid) + "\n\n";
        } else if ((mSigState & Signature.e_StateVerifyErrorByteRange) == Signature.e_StateVerifyErrorByteRange) {
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_errorByteRange) + "\n\n";
        } else if ((mSigState & Signature.e_StateVerifyIssueUnknown) == Signature.e_StateVerifyIssueUnknown) {
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_unknown) + "\n\n";
        } else if ((mSigState & Signature.e_StateVerifyIssueExpire) == Signature.e_StateVerifyIssueExpire) {
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_expired) + "\n\n";
        } else if ((mSigState & Signature.e_StateVerifyErrorData) == Signature.e_StateVerifyErrorData) {
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_error_data) + "\n\n";
        } else {
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_otherState) + "\n\n";
        }

        if ((mLtvState & SignatureVerifyResult.e_LTVStateEnable) == SignatureVerifyResult.e_LTVStateEnable) {
            resultText += mContext.getApplicationContext().getString(R.string.signature_ltv_attribute) + "\n\n";
        }

        resultText += mCertInfoForVerify;
        try {
            String signedDate = mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_signedTime)
                    + AppDmUtil.getLocalDateString(signature.getSignTime());
            resultText += signedDate + "\n";
        } catch (PDFException e) {
            e.printStackTrace();
        }

        mVerifyResultDialog = new UITextEditDialog(context);
        mVerifyResultDialog.getInputEditText().setVisibility(View.GONE);
        mVerifyResultDialog.setTitle(AppResource.getString(context, R.string.rv_security_dsg_verify_title));
        mVerifyResultDialog.getPromptTextView().setText(resultText);
        mVerifyResultDialog.getContentView().measure(0, 0);
        int dialogHeight = mVerifyResultDialog.getContentView().getMeasuredHeight();
        if (dialogHeight > mVerifyResultDialog.getDialogHeight())
            mVerifyResultDialog.setHeight(mVerifyResultDialog.getDialogHeight());
        mVerifyResultDialog.getOKButton().setText(AppResource.getString(mContext, R.string.rv_view_certificate_info));
        mVerifyResultDialog.getOKButton().setEnabled(fileInfos.size() > 0);
        mVerifyResultDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showVeriryCertListDialog(fileInfos);
                mVerifyResultDialog.dismiss();
            }
        });
        mVerifyResultDialog.show();
    }

    private void showVeriryCertListDialog(List<CertificateFileInfo> fileInfos) {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        if (uiExtensionsManager == null)
            return;

        Activity activity = uiExtensionsManager.getAttachedActivity();
        if (activity == null)
            return;

        if (!(activity instanceof FragmentActivity)) {
            UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.the_attached_activity_is_not_fragmentActivity));
            return;
        }

        FragmentActivity act = (FragmentActivity) activity;
        CertificateViewerFragment viewCertificate = (CertificateViewerFragment) act.getSupportFragmentManager().findFragmentByTag("ViewCertificate");
        if (viewCertificate == null)
            viewCertificate = new CertificateViewerFragment();
        viewCertificate.init(mPdfViewCtrl, fileInfos);

        AppDialogManager.getInstance().showAllowManager(viewCertificate, act.getSupportFragmentManager(), "ViewCertificate", null);
    }

    private void showNormalVerifyResult(Signature signature) {
        dismissProgressDialog();
        if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
        final Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return;
        final Dialog dialog = new FxDialog(context, AppTheme.getDialogTheme());

        View view = View.inflate(mContext, R.layout.rv_security_dsg_verify, null);
        dialog.setContentView(view, new ViewGroup.LayoutParams(AppDisplay.getInstance(mContext).getDialogWidth(), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView tv = (TextView) view.findViewById(R.id.rv_security_dsg_verify_result);
        String resultText = "";

        if ((mSigState & Signature.e_StateVerifyValid) == Signature.e_StateVerifyValid ||
                (mSigState & Signature.e_StateVerifyNoChange) == Signature.e_StateVerifyNoChange) {
            if (mIsFileChanged) {
                resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_perm) + "\n\n";
            } else {
                resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_valid) + "\n\n";
            }
        } else if ((mSigState & Signature.e_StateVerifyInvalid) == Signature.e_StateVerifyInvalid) {
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_invalid) + "\n\n";
        } else if ((mSigState & Signature.e_StateVerifyErrorByteRange) == Signature.e_StateVerifyErrorByteRange) {
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_errorByteRange) + "\n\n";
        } else if ((mSigState & Signature.e_StateVerifyIssueExpire) == Signature.e_StateVerifyIssueExpire) {
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_expired) + "\n\n";
        } else {
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_otherState) + "\n\n";
        }

        try {
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_publisher) + AppUtil.getEntryName(signature.getCertificateInfo("Issuer"), "CN=") + "\n";
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_serialNumber) + signature.getCertificateInfo("SerialNumber") + "\n";
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_emailAddress) + AppUtil.getEntryName(signature.getCertificateInfo("Subject"), "E=") + "\n";
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_validityStarts) + signature.getCertificateInfo("ValidPeriodFrom") + "\n";
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_validityEnds) + signature.getCertificateInfo("ValidPeriodTo") + "\n";

            String signedDate = mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_signedTime)
                    + AppDmUtil.getLocalDateString(signature.getSignTime());

            resultText += signedDate + "\n";
        } catch (PDFException e) {
            e.printStackTrace();
        }

        tv.setText(resultText);
        dialog.setCanceledOnTouchOutside(true);
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        });
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        if (mVerifyResultDialog != null && mVerifyResultDialog.isShowing()) {
            mVerifyResultDialog.setHeight(mVerifyResultDialog.getDialogHeight());
            mVerifyResultDialog.show();
        }
    }
}
