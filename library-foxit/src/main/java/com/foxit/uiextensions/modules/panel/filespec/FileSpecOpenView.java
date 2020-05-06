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
package com.foxit.uiextensions.modules.panel.filespec;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.view.KeyEvent;
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
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.modules.connectpdf.account.AccountModule;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.impl.LifecycleEventListener;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.IResult;
import com.foxit.uiextensions.utils.UIToast;


public class FileSpecOpenView extends View{


    private View mOpenView;
    private TextView mOpenView_filenameTV;
    private ImageView mOpenView_backIV;
    private LinearLayout mOpenView_contentLy;
    private LinearLayout mOpenView_titleLy;

    private PDFViewCtrl mAttachPdfViewCtrl;
    private PDFViewCtrl mPdfViewCtrl;


    private Context mContext;
    private ViewGroup mParent;

    public FileSpecOpenView(Context context, PDFViewCtrl pdfViewCtrl, ViewGroup parent){
        super(context);
        this.mContext = context;
        this.mPdfViewCtrl =  pdfViewCtrl;
        this.mParent = parent;

        initOpenView();
    }


    private void initOpenView() {
        mOpenView = View.inflate(mContext, R.layout.attachment_view, null);
        mOpenView_titleLy = (LinearLayout) mOpenView.findViewById(R.id.attachment_view_topbar_ly);
        mOpenView_contentLy = (LinearLayout) mOpenView.findViewById(R.id.attachment_view_content_ly);
        mOpenView_backIV = (ImageView) mOpenView.findViewById(R.id.attachment_view_topbar_back);
        mOpenView_filenameTV = (TextView) mOpenView.findViewById(R.id.attachment_view_topbar_name);
        mParent.addView(mOpenView);
        mOpenView.setVisibility(View.GONE);

        int margin_left = 0;
        int margin_right = 0;
        if (AppDisplay.getInstance(mContext).isPad()) {
            margin_left = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_left_margin_pad);
            margin_right = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_horz_right_margin_pad);
            LinearLayout.LayoutParams clp = (LinearLayout.LayoutParams) mOpenView_titleLy.getLayoutParams();
            clp.setMargins(margin_left, 0, margin_right, 0);
        }

        mOpenView_backIV.setOnClickListener(new View.OnClickListener() {
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

    public void openAttachment(final String filePath, final String filename, final FileSpecModuleCallback callback) {

        mAttachPdfViewCtrl = new PDFViewCtrl(mContext);

        mOpenView_filenameTV.setText(filename);
        mOpenView_contentLy.removeAllViews();
        mOpenView_contentLy.addView(mAttachPdfViewCtrl);
        mOpenView.setVisibility(View.VISIBLE);
        mOpenView_contentLy.setVisibility(View.VISIBLE);
        mAttachPdfViewCtrl.setAttachedActivity(mPdfViewCtrl.getAttachedActivity());
        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).registerLifecycleListener(mLifecycleEventListener);
        mAttachPdfViewCtrl.setConnectedPDFEventListener(mPdfViewCtrl.getConnectedPdfEventListener());
        mAttachPdfViewCtrl.registerDocEventListener(new PDFViewCtrl.IDocEventListener() {
            @Override
            public void onDocWillOpen() {

            }

            @Override
            public void onDocOpened(PDFDoc document, int errCode) {
                callback.onDocOpenFinished();
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).unregisterLifecycleListener(mLifecycleEventListener);
                switch (errCode) {
                    case Constants.e_ErrSuccess:
                        mAttachPdfViewCtrl.setContinuous(true);
                        mAttachPdfViewCtrl.setPageLayoutMode(PDFViewCtrl.PAGELAYOUTMODE_SINGLE);
                        mPasswordError = false;
                        return;
                    case Constants.e_ErrPassword:
                        String tips;
                        if (mPasswordError) {
                            tips = AppResource.getString(mContext.getApplicationContext(), R.string.rv_tips_password_error);
                        } else {
                            tips = AppResource.getString(mContext.getApplicationContext(), R.string.rv_tips_password);
                        }
                        final UITextEditDialog uiTextEditDialog = new UITextEditDialog(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager())
                                .getAttachedActivity());
                        uiTextEditDialog.getDialog().setCanceledOnTouchOutside(false);
                        uiTextEditDialog.getInputEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        uiTextEditDialog.setTitle(AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_password_dialog_title));
                        uiTextEditDialog.getPromptTextView().setText(tips);
                        uiTextEditDialog.show();
                        AppUtil.showSoftInput(uiTextEditDialog.getInputEditText());
                        uiTextEditDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                uiTextEditDialog.dismiss();
                                AppUtil.dismissInputSoft(uiTextEditDialog.getInputEditText());
                                String pw = uiTextEditDialog.getInputEditText().getText().toString();
                                mAttachPdfViewCtrl.openDoc(filePath,  pw.getBytes());
                            }
                        });

                        uiTextEditDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                uiTextEditDialog.dismiss();
                                AppUtil.dismissInputSoft(uiTextEditDialog.getInputEditText());
                                mPasswordError = false;
                                Toast.makeText(mContext.getApplicationContext(), R.string.rv_document_open_failed, Toast.LENGTH_SHORT).show();
                            }
                        });

                        uiTextEditDialog.getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
                            @Override
                            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                if (keyCode == KeyEvent.KEYCODE_BACK) {
                                    uiTextEditDialog.getDialog().cancel();
                                    mPasswordError = false;
                                    Toast.makeText(mContext.getApplicationContext(), R.string.rv_document_open_failed, Toast.LENGTH_SHORT).show();
                                    return true;
                                }
                                return false;
                            }
                        });

                        if (!mPasswordError)
                            mPasswordError = true;
                        return;
                    case Constants.e_ErrCanNotGetUserToken:
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
        mAttachPdfViewCtrl.openDoc(filePath, null);

    }

    public void closeAttachment() {
        if (mAttachPdfViewCtrl == null)
            return;
        mAttachPdfViewCtrl.closeDoc();
        mOpenView.setVisibility(GONE);
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
}
