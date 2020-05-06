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
package com.foxit.uiextensions.security.certificate;

import android.content.Context;
import android.content.DialogInterface;
import androidx.fragment.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.UIToast;

import java.util.ArrayList;
import java.util.List;

public class CertificateViewSupport {

    private List<CertificateFileInfo> mCertInfos;

    private Context mContext;
    private CertificateSupport mCertSupport;
    private CertificateDataSupport mDataSupport;
    public static final int			MESSAGE_UPDATE = 0x11;
    public static final int			MESSAGE_FINISH = 0x12;
    public static final int			MESSAGE_HIDEALLPFXFILEDLG = 0x13;
    private boolean					mDoEncrypt;
    private PDFViewCtrl             mPdfViewCtrl;

    public CertificateViewSupport(Context context, PDFViewCtrl pdfViewCtrl, CertificateSupport support) {
        mCertInfos = new ArrayList<CertificateFileInfo>();
        mDataSupport = new CertificateDataSupport(context);

        mCertSupport = support;
        mPdfViewCtrl = pdfViewCtrl;
        mContext = context;
    }

    CertificateDataSupport getDataSupport() {
        return mDataSupport;
    }

    CertificateSupport getCertSupport() {
        return mCertSupport;
    }

    CertificateFragment mCertDialog;
    public void showAllPfxFileDialog(boolean isOnlyPFX, boolean x ,final CertificateFragment.ICertDialogCallback callback) {
        if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
        Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return;
        FragmentActivity act = (FragmentActivity) context;

        mCertDialog = new CertificateFragment(act);


        if (isOnlyPFX) {
            if (x) {
                mCertDialog.init(this, callback, CertificateFragment.CERLIST_TYPE_SIGNATURE);
            } else {
                mCertDialog.init(this, callback, CertificateFragment.CERLIST_TYPE_DECRYPT);
            }
        } else {
            mCertDialog.init(this, callback, CertificateFragment.CERLIST_TYPE_ENCRYPT);
        }
        mDoEncrypt = !isOnlyPFX;
        mCertDialog.showDialog();
        mCertDialog.setCanceledOnTouchOutside(false);

    }


    public void dismissPfxDialog() {
        if (mPdfViewCtrl.getUIExtensionsManager() == null) return;

        if (mCertDialog != null && mCertDialog.isShowing()) {
            mCertDialog.dismiss();
        }
    }

    private UIMatchDialog mPasswordDialog;
    void showPasswordDialog(final CertificateFileInfo info, final CertificateFragment.ICertDialogCallback callback) {
        if (mPasswordDialog != null) {
            mPasswordDialog.dismiss();
        }
        if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
        Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return;
        mPasswordDialog = new UIMatchDialog(context, 0);
        View view = View.inflate(mContext, R.layout.rv_password_dialog, null);
        final EditText editText = (EditText) view.findViewById(R.id.rv_document_password);
        editText.setVisibility(View.VISIBLE);
        TextView tips = (TextView) view.findViewById(R.id.rv_tips);
        tips.setText(AppResource.getString(mContext.getApplicationContext(),R.string.rv_security_certlist_inputpasswd));

        LinearLayout layout = (LinearLayout) view.findViewById(R.id.rv_document_password_ly);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (AppDisplay.getInstance(mContext).isPad()) {
            lp.setMargins(
                    AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_left_margin_pad),
                    0,
                    AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_right_margin_pad),
                    0);
        } else {
            lp.setMargins(
                    AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_left_margin_phone),
                    0,
                    AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_right_margin_phone),
                    0);
        }
        layout.setLayoutParams(lp);
        AppUtil.showSoftInput(editText);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mPasswordDialog == null) return;
                String passwdStr = s.toString();
                if (passwdStr != null && passwdStr.length() > 0) {
                    mPasswordDialog.setButtonEnable(true, MatchDialog.DIALOG_OK);
                    return;
                }
                mPasswordDialog.setButtonEnable(false, MatchDialog.DIALOG_OK);
            }
        });
        mPasswordDialog.setContentView(view);
        mPasswordDialog.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        mPasswordDialog.setTitle(AppResource.getString(mContext.getApplicationContext(), R.string.rv_password_dialog_title));
        mPasswordDialog.setButton(MatchDialog.DIALOG_OK | MatchDialog.DIALOG_CANCEL);
        mPasswordDialog.setButtonEnable(false, MatchDialog.DIALOG_OK);
        mPasswordDialog.setBackButtonVisible(View.GONE);
        mPasswordDialog.setListener(new MatchDialog.DialogListener() {
            @Override
            public void onResult(long btType) {
                if (btType == MatchDialog.DIALOG_OK) {
                    String psd = editText.getText().toString();
                    info.certificateInfo = mCertSupport.verifyPassword(info.filePath, psd);
                    if (info.certificateInfo != null) {
                        info.password = psd;
                        info.issuer = info.certificateInfo.issuer;
                        info.publisher = info.certificateInfo.publisher;
                        info.serialNumber = info.certificateInfo.serialNumber;
                        if (!mCertInfos.contains(info)) {
                            updateInfo(info);
                        }
                        mPasswordDialog.dismiss();
                        mPasswordDialog = null;
                        if (callback != null) {
                            callback.result(true, null, null);
                        }
                    } else {
                        editText.setText("");
                        editText.setFocusable(true);

                        UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.rv_security_certlist_invalidpasswd));
                    }
                } else if (btType == MatchDialog.DIALOG_CANCEL) {
                    mPasswordDialog.dismiss();
                    mPasswordDialog = null;
                    if (callback != null) {
                        callback.result(false, null, null);
                    }
                }
            }

            @Override
            public void onBackClick() {

            }
        });

        mPasswordDialog.showDialog();

        mPasswordDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    mPasswordDialog.dismiss();
                    mPasswordDialog = null;
                    if (callback != null) {
                        callback.result(false, null, null);
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    String psd = editText.getText().toString();
                    info.certificateInfo = mCertSupport.verifyPassword(info.filePath, psd);
                    if (info.certificateInfo != null) {
                        info.password = psd;
                        info.issuer = info.certificateInfo.issuer;
                        info.publisher = info.certificateInfo.publisher;
                        info.serialNumber = info.certificateInfo.serialNumber;
                        if (!mCertInfos.contains(info)) {
                            updateInfo(info);
                        }
                        mPasswordDialog.dismiss();
                        mPasswordDialog = null;
                        if (callback != null) {
                            callback.result(true, null, null);
                        }
                    } else {
                        editText.setText("");
                        editText.setFocusable(true);
                        UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.rv_security_certlist_invalidpasswd));
                    }
                }
                return false;
            }
        });
        mPasswordDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mPasswordDialog.dismiss();
                mPasswordDialog = null;
            }
        });

        mPasswordDialog.setCanceledOnTouchOutside(false);
    }

    CertificateDetailDialog mDetailDialog;
    void showPermissionDialog(final CertificateFileInfo info) {
        if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
        Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return;
        FragmentActivity act = (FragmentActivity) context;

        int dlgType = CertificateDetailDialog.PERMDLG_TYPE_ENCRYPT;
        if (!mDoEncrypt) {
            dlgType = CertificateDetailDialog.PERMDLG_TYPE_DECRYPT;
            mDetailDialog = new CertificateDetailDialog(act, true);
        } else {
            mDetailDialog = new CertificateDetailDialog(act, false);
        }
        mDetailDialog.init(dlgType, info);
        mDetailDialog.showDialog();

    }

    private void updateInfo(CertificateFileInfo info) {
        if (info.isCertFile) {
            mDataSupport.insertCert(info.issuer, info.publisher, info.serialNumber, info.filePath, info.fileName);
        } else {
            mDataSupport.insertPfx(info.issuer, info.publisher, info.serialNumber, info.filePath, info.fileName, info.password);
        }
    }

    public void onConfigurationChanged() {

    }

}

