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
package com.foxit.uiextensions.security.digitalsignature;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.DateTime;
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
import com.foxit.uiextensions.controls.dialog.FxDialog;
import com.foxit.uiextensions.controls.dialog.FxProgressDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.security.certificate.CertificateFileInfo;
import com.foxit.uiextensions.security.certificate.CertificateSupport;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppTheme;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.UIToast;

import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.util.Store;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collection;
import java.util.TimeZone;


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

    private SignatureVerifyResult mLtvVerifyResult;
    private int mLtvState = 0;
    private String mTrustCertificateInfo = "";
    private String mCertInfoForVerify = "";

    public DigitalSignatureSecurityHandler(Context context, PDFViewCtrl pdfViewCtrl, CertificateSupport support) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
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

                Control control = signature.getControl(0);
                if (!control.isEmpty()) {
                    Widget widget = control.getWidget();
                    if (!widget.isEmpty()) {
                        String nm = widget.getUniqueID();
                        if (TextUtils.isEmpty(nm)) {
                            widget.setUniqueID(AppDmUtil.randomUUID(null));
                        }
                    }
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
                if (progress == Progressive.e_Error)  {
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
        if (mPdfViewCtrl.getUIExtensionsManager() == null)  return false;
        Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null)  return false;

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
        private boolean mIsSetTrustCert = false;

        public VerifySignatureTask(final Signature signature, final Event.Callback callback) {
            this(signature, false, callback);
        }

        public VerifySignatureTask(final Signature signature, boolean isSetTrustCert, final Event.Callback callback) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    if (callback != null) {
                        callback.result(null, true);
                    }

                }
            });
            mSignature = signature;
            mIsSetTrustCert = isSetTrustCert;
        }


        @Override
        protected void execute() {
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
                X509CertificateHolder x509CertificateHolder = getCertificate(mSignature);
                if (mIsSetTrustCert) {
                    ltvVerifier.setTrustedCertStoreCallback(new FxTrustedCertStoreCallback(x509CertificateHolder));
                }
                SignatureVerifyResultArray resultArray = ltvVerifier.verifySignature(mSignature);
                if (resultArray.getSize() == 0) {
                    UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_error));
                }

                mLtvVerifyResult = resultArray.getAt(0);
                mSigState = mLtvVerifyResult.getSignatureState();
                mLtvState = mLtvVerifyResult.getLTVState();
                mIsFileChanged = hasModifiedDocument();

                if ((mSigState & Signature.e_StateUnknown) == Signature.e_StateUnknown && !mIsSetTrustCert) {
                    mTrustCertificateInfo = getTrustCertificateInformation(mSignature, x509CertificateHolder);
                }

                mCertInfoForVerify = getCertificateInformationForVerify(x509CertificateHolder);
            } catch (PDFException e) {
                e.printStackTrace();
                UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_error));
            }
        }

        private X509CertificateHolder getCertificate(Signature signature) {
            try {
                byte[] sigContent = signature.getSignatureDict().getElement("Contents").getString();
                CMSSignedData data = new CMSSignedData(sigContent);
                Store<X509CertificateHolder> x509CertificateHolderStore = data.getCertificates();
                SignerInformationStore signerInformationStore = data.getSignerInfos();
                for (SignerInformation signerInformation : signerInformationStore.getSigners()) {
                    @SuppressWarnings("unchecked")
                    Collection cert = x509CertificateHolderStore.getMatches(signerInformation.getSID());
                    int certCount = cert.size();
                    if (certCount > 0) {
                        return (X509CertificateHolder) cert.iterator().next();
                    }
                }

            } catch (PDFException e) {
                e.printStackTrace();
            } catch (CMSException e) {
                e.printStackTrace();
            }
            return null;
        }

        private String getTrustCertificateInformation(Signature signature, X509CertificateHolder x509CertificateHolder) {
            if (signature == null || x509CertificateHolder == null) return "";
            String content = mContext.getApplicationContext().getString(R.string.rv_security_dsg_trust_cert_tip) + "\n\n";

            try {
                String signer = signature.getKeyValue(Signature.e_KeyNameSigner);
                String issuer = AppUtil.getEntryName(x509CertificateHolder.getIssuer().toString(), "CN=");
                if (TextUtils.isEmpty(signer)) {
                    signer = issuer;
                }
                content += mContext.getApplicationContext().getString(R.string.rv_security_dsg_trust_cert_name) + signer + "\n";
                content += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_publisher) + issuer + "\n";
                content += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_validityStarts) + AppDmUtil.getLocalDateString(x509CertificateHolder.getNotBefore()) + "\n";
                content += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_validityEnds) + AppDmUtil.getLocalDateString(x509CertificateHolder.getNotAfter()) + "\n";
                String usage = getIntendedKeyUsage(x509CertificateHolder);
                content += mContext.getApplicationContext().getString(R.string.rv_security_dsg_trust_cert_usage) + usage + "\n";
            } catch (PDFException e) {

            }
            return content;
        }

        String getIntendedKeyUsage(X509CertificateHolder x509CertificateHolder) {
            if (x509CertificateHolder == null) return "";
            String usage = "";
            KeyUsage keyUsage = KeyUsage.fromExtensions(x509CertificateHolder.getExtensions());
            if (keyUsage != null) {
                if (keyUsage.hasUsages(KeyUsage.dataEncipherment)) {
                    usage += mContext.getApplicationContext().getString(R.string.cert_key_usage_data_encipherment);
                }

                if (keyUsage.hasUsages(KeyUsage.digitalSignature)) {
                    if (!TextUtils.isEmpty(usage)) {
                        usage += ", ";
                    }
                    usage += mContext.getApplicationContext().getString(R.string.cert_key_usage_digital_signature);
                }

                if (keyUsage.hasUsages(KeyUsage.keyAgreement)) {
                    if (!TextUtils.isEmpty(usage)) {
                        usage += ", ";
                    }
                    usage += mContext.getApplicationContext().getString(R.string.cert_key_usage_key_agreement);
                }

                if (keyUsage.hasUsages(KeyUsage.keyCertSign)) {
                    if (!TextUtils.isEmpty(usage)) {
                        usage += ", ";
                    }
                    usage += mContext.getApplicationContext().getString(R.string.cert_key_usage_key_cert_sign);
                }

                if (keyUsage.hasUsages(KeyUsage.keyEncipherment)) {
                    if (!TextUtils.isEmpty(usage)) {
                        usage += ", ";
                    }
                    usage += mContext.getApplicationContext().getString(R.string.cert_key_usage_key_encipherment);
                }

                if (keyUsage.hasUsages(KeyUsage.nonRepudiation)) {
                    if (!TextUtils.isEmpty(usage)) {
                        usage += ", ";
                    }
                    usage += mContext.getApplicationContext().getString(R.string.cert_key_usage_non_repudiation);
                }

                if (keyUsage.hasUsages(KeyUsage.cRLSign)) {
                    if (!TextUtils.isEmpty(usage)) {
                        usage += ", ";
                    }
                    usage += mContext.getApplicationContext().getString(R.string.cert_key_usage_crl_sign);
                }
            }

            if (TextUtils.isEmpty(usage)) {
                usage = mContext.getApplicationContext().getString(R.string.cert_key_usage);
            }
            return usage;
        }

        private String getCertificateInformationForVerify(X509CertificateHolder x509CertificateHolder) {
            if (x509CertificateHolder == null) return "";

            String issuer = AppUtil.getEntryName(x509CertificateHolder.getIssuer().toString(), "CN=");
            BigInteger bigInteger = x509CertificateHolder.getSerialNumber();
            if (bigInteger.compareTo(BigInteger.ZERO) < 0) {
                bigInteger = new BigInteger(1, bigInteger.toByteArray());
            }
            String sn = bigInteger.toString(16).toUpperCase();
            String email = AppUtil.getEntryName(x509CertificateHolder.getSubject().toString(), "E=");

            String content = mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_publisher) + issuer + "\n";
            content += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_serialNumber) + sn + "\n";
            content += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_emailAddress) + email + "\n";
            content += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_validityStarts) + AppDmUtil.getLocalDateString(x509CertificateHolder.getNotBefore()) + "\n";
            content += mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_validityEnds) + AppDmUtil.getLocalDateString(x509CertificateHolder.getNotAfter()) + "\n";

            return content;
        }
    }

    public void verifySignature(final Signature signature) {
        verifySignature(signature, false);
    }

    private void verifySignature(final Signature signature, boolean isSetTrustCert) {
        if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
        Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return;
        mProgressDialog = new FxProgressDialog(context, mContext.getApplicationContext().getString(R.string.rv_sign_waiting));
        mProgressDialog.show();
        mPdfViewCtrl.addTask(new VerifySignatureTask(signature, isSetTrustCert, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                if ((mSigState & Signature.e_StateUnknown) == Signature.e_StateUnknown){
                    showTrustCertificateDialog(signature);
                } else {
                    showVerifyResult(signature);
                }
            }
        }));
    }

    private void showVerifyResult(Signature signature) {
        dismissProgressDialog();
        if (mPdfViewCtrl.getUIExtensionsManager() == null)return;
        final Activity context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return;
        final Dialog dialog = new FxDialog(context, AppTheme.getDialogTheme());

        View view = View.inflate(mContext, R.layout.rv_security_dsg_verify, null);
        dialog.setContentView(view, new LayoutParams(AppDisplay.getInstance(mContext).getDialogWidth(), LayoutParams.WRAP_CONTENT));
        TextView tv = (TextView) view.findViewById(R.id.rv_security_dsg_verify_result);
        String resultText = "";

        if ((mSigState & Signature.e_StateVerifyValid) == Signature.e_StateVerifyValid){
            if (mIsFileChanged) {
                resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_perm) + "\n\n";
            } else {
                resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_valid) + "\n\n";
            }
        } else if ((mSigState & Signature.e_StateVerifyInvalid) == Signature.e_StateVerifyInvalid){
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_invalid) + "\n\n";
        } else if ((mSigState & Signature.e_StateVerifyErrorByteRange) == Signature.e_StateVerifyErrorByteRange){
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_errorByteRange) + "\n\n";
        } else if ((mSigState & Signature.e_StateUnknown) == Signature.e_StateUnknown){
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_unknown) + "\n\n";
        } else if ((mSigState & Signature.e_StateVerifyIssueExpire) == Signature.e_StateVerifyIssueExpire){
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_expired) + "\n\n";
        } else if ((mSigState & Signature.e_StateVerifyErrorData) == Signature.e_StateVerifyErrorData){
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_error_data) + "\n\n";
        } else {
            resultText += mContext.getApplicationContext().getString(R.string.rv_security_dsg_verify_otherState) + "\n\n";
        }

        if ((mLtvState & SignatureVerifyResult.e_LTVStateEnable) == SignatureVerifyResult.e_LTVStateEnable) {
            resultText += mContext.getApplicationContext().getString(R.string.signature_ltv_attribute) + "\n\n";
        }

        try {
            resultText += mCertInfoForVerify;

            String signedDate = mContext.getApplicationContext().getString(R.string.rv_security_dsg_cert_signedTime)
                    + AppDmUtil.getLocalDateString(signature.getSignTime());

            resultText += signedDate + "\n";
        } catch (PDFException e) {
            e.printStackTrace();
        }

        tv.setText(resultText);
        dialog.setCanceledOnTouchOutside(true);
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        });
    }

    private UITextEditDialog mTrustCertDialog = null;
    private void showTrustCertificateDialog(final Signature signature) {
        dismissProgressDialog();
        if (mPdfViewCtrl.getUIExtensionsManager() == null)return;
        final Activity context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return;
        mTrustCertDialog = new UITextEditDialog(context);
        mTrustCertDialog.setTitle(R.string.rv_security_dsg_trust_cert_title);
        mTrustCertDialog.getPromptTextView().setText(mTrustCertificateInfo);
        mTrustCertDialog.getInputEditText().setVisibility(View.GONE);
        mTrustCertDialog.getDialog().setCanceledOnTouchOutside(false);
        mTrustCertDialog.setHeight(mTrustCertDialog.getDialogHeight());
        mTrustCertDialog.show();

        mTrustCertDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               mTrustCertDialog.dismiss();
               mTrustCertDialog = null;
                verifySignature(signature, true);
            }
        });

        mTrustCertDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTrustCertDialog.dismiss();
                mTrustCertDialog = null;
                showVerifyResult(signature);
            }
        });
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        if (mTrustCertDialog != null && mTrustCertDialog.isShowing()) {
            mTrustCertDialog.setHeight(mTrustCertDialog.getDialogHeight());
            mTrustCertDialog.show();
        }
    }
}
