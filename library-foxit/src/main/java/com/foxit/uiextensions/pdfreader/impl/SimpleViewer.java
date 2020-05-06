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
package com.foxit.uiextensions.pdfreader.impl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.PasswordDialog;
import com.foxit.uiextensions.modules.connectpdf.account.AccountModule;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.IResult;
import com.foxit.uiextensions.utils.UIToast;

public class SimpleViewer extends View {
    private View mOpenView;
    private TextView mTvFileName;
    private ImageView mIvBack;
    private LinearLayout mLlContent;
    private LinearLayout mLlTitle;

    private PDFViewCtrl mAttachPdfViewCtrl;
    private PDFViewCtrl mPdfViewCtrl;


    private Context mContext;
    private ViewGroup mParent;

    public SimpleViewer(Context context, PDFViewCtrl pdfViewCtrl, ViewGroup parent){
        super(context);
        this.mContext = context;
        this.mPdfViewCtrl =  pdfViewCtrl;
        this.mParent = parent;

        initOpenView();
    }


    private void initOpenView() {
        mOpenView = View.inflate(mContext, R.layout.attachment_view, null);
        mLlTitle = (LinearLayout) mOpenView.findViewById(R.id.attachment_view_topbar_ly);
        mLlContent = (LinearLayout) mOpenView.findViewById(R.id.attachment_view_content_ly);
        mIvBack = (ImageView) mOpenView.findViewById(R.id.attachment_view_topbar_back);
        mTvFileName = (TextView) mOpenView.findViewById(R.id.attachment_view_topbar_name);
        mParent.addView(mOpenView);
        mOpenView.setVisibility(View.GONE);

        int marginLeft = 0;
        int marginRight = 0;
        if (AppDisplay.getInstance(mContext).isPad()) {
            marginLeft = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_left_margin_pad);
            marginRight = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_right_margin_pad);
            LinearLayout.LayoutParams clp = (LinearLayout.LayoutParams) mLlTitle.getLayoutParams();
            clp.setMargins(marginLeft, 0, marginRight, 0);
        }

        mIvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOpenView.setVisibility(View.GONE);
                mAttachPdfViewCtrl.closeDoc();
            }
        });

        mOpenView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        setVisibility(GONE);

    }

    private boolean mPasswordError = false;

    private void create(final String filePath, final String fileName, final ISimpleViewerCallBack callBack) {
        mAttachPdfViewCtrl = new PDFViewCtrl(mContext);
        mTvFileName.setText(fileName);
        mLlContent.removeAllViews();
        mLlContent.addView(mAttachPdfViewCtrl);
        mOpenView.setVisibility(View.VISIBLE);
        mLlContent.setVisibility(View.VISIBLE);
        mAttachPdfViewCtrl.setAttachedActivity(mPdfViewCtrl.getAttachedActivity());
        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).registerLifecycleListener(mLifecycleEventListener);
        mAttachPdfViewCtrl.setConnectedPDFEventListener(mPdfViewCtrl.getConnectedPdfEventListener());
        mAttachPdfViewCtrl.registerDocEventListener(new PDFViewCtrl.IDocEventListener() {
            @Override
            public void onDocWillOpen() {
                if (callBack != null) {
                    callBack.onDocWillOpen();
                }
            }

            @Override
            public void onDocOpened(PDFDoc document, int errCode) {
                if (callBack != null) {
                    callBack.onDocOpened();
                }
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).unregisterLifecycleListener(mLifecycleEventListener);
                switch (errCode) {
                    case Constants.e_ErrSuccess:
                        mAttachPdfViewCtrl.setContinuous(true);
                        mAttachPdfViewCtrl.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_SINGLE);
                        mPasswordError = false;
                        return;
                    case Constants.e_ErrPassword:
                        if (TextUtils.isEmpty(filePath)) {
                            Toast.makeText(mContext.getApplicationContext(), R.string.rv_document_open_failed, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String tips;
                        if (mPasswordError) {
                            tips = AppResource.getString(mContext.getApplicationContext(), R.string.rv_tips_password_error);
                        } else {
                            tips = AppResource.getString(mContext.getApplicationContext(), R.string.rv_tips_password);
                        }

                        Activity activity = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                        PasswordDialog passwordDialog = new PasswordDialog(activity, new PasswordDialog.IPasswordDialogListener() {
                            @Override
                            public void onConfirm(byte[] password) {
                                mAttachPdfViewCtrl.openDoc(filePath,  password);
                            }

                            @Override
                            public void onDismiss() {
                                mPasswordError = false;
                                Toast.makeText(mContext.getApplicationContext(), R.string.rv_document_open_failed, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onKeyBack() {
                                mPasswordError = false;
                                Toast.makeText(mContext.getApplicationContext(), R.string.rv_document_open_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                        passwordDialog.setTitle(AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_password_dialog_title))
                                .setPromptTips(tips)
                                .show();

                        if (!mPasswordError) {
                            mPasswordError = true;
                        }
                        return;
                    case Constants.e_ErrCanNotGetUserToken:
                        if (TextUtils.isEmpty(filePath)) {
                            Toast.makeText(mContext.getApplicationContext(), R.string.rv_document_open_failed, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        openCpdfDoc(filePath);
                        return;
                    default:
                        String message = AppUtil.getMessage(mContext, errCode);
                        UIToast.getInstance(mContext).show(message);
                        break;
                }
            }

            @Override
            public void onDocWillClose(PDFDoc document) {

            }

            @Override
            public void onDocClosed(PDFDoc document, int errCode) {
                mAttachPdfViewCtrl = null;
                if (callBack != null) {
                    callBack.onDocClosed();
                }
            }

            @Override
            public void onDocWillSave(PDFDoc document) {

            }

            @Override
            public void onDocSaved(PDFDoc document, int errCode) {

            }
        });
        mAttachPdfViewCtrl.setContinuous(true);
        mAttachPdfViewCtrl.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_SINGLE);
    }

    public void open(final String filePath, final String fileName, final ISimpleViewerCallBack callBack) {
        create(filePath, fileName, callBack);
        mAttachPdfViewCtrl.openDoc(filePath, null);
        setVisibility(View.VISIBLE);
    }

    public void open(final PDFDoc pdfDoc, final String fileName, final ISimpleViewerCallBack callBack) {
        create(null, fileName, callBack);
        mAttachPdfViewCtrl.setDoc(pdfDoc);
        setVisibility(View.VISIBLE);
    }

    private void openCpdfDoc(final String filePath) {
        AccountModule.getInstance().showLoginDialog(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager())
                .getAttachedActivity(), new IResult<Void, Void, Void>() {
            @Override
            public void onResult(boolean success, Void p1, Void p2, Void p3) {
                if (success) {
                    mAttachPdfViewCtrl.openDoc(filePath,  null);
                } else {
                    Toast.makeText(mContext.getApplicationContext(), R.string.rv_document_open_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void close() {
        if (mAttachPdfViewCtrl != null) {
            mAttachPdfViewCtrl.closeDoc();
            mOpenView.setVisibility(GONE);
        }
        setVisibility(GONE);
    }

    private ILifecycleEventListener mLifecycleEventListener = new LifecycleEventListener() {
        @Override
        public void onActivityResult(Activity act, int requestCode, int resultCode, Intent data) {
            if (mAttachPdfViewCtrl != null) {
                mAttachPdfViewCtrl.handleActivityResult(requestCode, resultCode, data);
            }
        }
    };

    public interface ISimpleViewerCallBack {
        void onDocWillOpen();
        void onDocOpened();
        void onDocClosed();
    }
}
