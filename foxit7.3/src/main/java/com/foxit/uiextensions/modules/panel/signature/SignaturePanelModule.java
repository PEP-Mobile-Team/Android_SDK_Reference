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
package com.foxit.uiextensions.modules.panel.signature;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Widget;
import com.foxit.sdk.pdf.interform.Field;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotEventListener;
import com.foxit.uiextensions.annots.IRedactionEventListener;
import com.foxit.uiextensions.controls.dialog.FxProgressDialog;
import com.foxit.uiextensions.controls.panel.PanelSpec;
import com.foxit.uiextensions.modules.panel.ILoadPanelItemListener;
import com.foxit.uiextensions.modules.panel.PanelWindow;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.pdfreader.impl.SimpleViewer;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.OnPageEventListener;
import com.foxit.uiextensions.utils.UIToast;


public class SignaturePanelModule implements Module, PanelSpec {
    private PDFViewCtrl mPdfViewCtrl;
    private Context mContext;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    private AppDisplay mDisplay;
    private Boolean mIsPad;

    private PanelWindow mPanelWindow;

    private View mTopBarView;
    private View mContentView;
    private TextView mNoInfoView;
    private TextView mLoadingView;

    private SignatureAdapter mSigAdapter;
    private boolean mIsNeedRefreshPanel = true;
    private boolean bDocClosed = false;
    private ViewGroup mParent;
    private SimpleViewer mSignedVersionView;

    public SignaturePanelModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context.getApplicationContext();
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
        mDisplay = new AppDisplay(mContext);
        mIsPad = mDisplay.isPad();
        SignaturePresenter.ISignedVersionCallBack signedVersionCallback = new SignaturePresenter.ISignedVersionCallBack() {
            @Override
            public void onPrepare() {
                showProgressDialog();
            }

            @Override
            public void onOpen(PDFDoc pdfDoc, String fileName) {
                if (mSignedVersionView != null) {
                    mSignedVersionView.open(pdfDoc, fileName, new SimpleViewer.ISimpleViewerCallBack() {
                        @Override
                        public void onDocWillOpen() {

                        }

                        @Override
                        public void onDocOpened() {
                            dismissProgressDialog();
                        }

                        @Override
                        public void onDocClosed() {
                            mPanelWindow.show(PanelType.Signatures);
                        }
                    });
                    mPanelWindow.dismiss();
                }
            }

            @Override
            public void onFinish() {
                dismissProgressDialog();
            }
        };
        mSigAdapter = new SignatureAdapter(mContext, pdfViewCtrl, signedVersionCallback);
    }

    @Override
    public String getName() {
        return MODULE_NAME_SIGNATUREPANEL;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager instanceof UIExtensionsManager) {
            mPanelWindow = new PanelWindow(mContext, mPdfViewCtrl, mUiExtensionsManager);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }

        initTopBar();
        mPanelWindow.addPanelSpec(this);

        //register event listener
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        mPdfViewCtrl.registerPageEventListener(mPageEventListener);

        if (mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().registerAnnotEventListener(mAnnotEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().registerRedactionEventListener(mRedactionEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerLayoutChangeListener(mLayoutListener);
        }
        return true;
    }

    @Override
    public boolean unloadModule() {
        mPanelWindow.removePanelSpec(this);
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterAnnotEventListener(mAnnotEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterRedactionEventListener(mRedactionEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLayoutChangeListener(mLayoutListener);
        }
        return true;
    }

    @Override
    public int getIcon() {
        return R.drawable.panel_tabing_signature_selector;
    }

    @Override
    public PanelType getPanelType() {
        return PanelType.Signatures;
    }

    @Override
    public View getTopToolbar() {
        return mTopBarView;
    }

    @Override
    public View getContentView() {
        return mContentView;
    }

    @Override
    public void onActivated() {
        if (mIsNeedRefreshPanel) {
            mLoadingView.setVisibility(View.VISIBLE);
            mNoInfoView.setVisibility(View.GONE);
            mSigAdapter.load(mItemListener);
            mIsNeedRefreshPanel = false;
        }

        if (mSignedVersionView == null) {
            mSignedVersionView = new SimpleViewer(mContext, mPdfViewCtrl, mParent);
        }
    }

    @Override
    public void onDeactivated() {
        mSignedVersionView = null;
    }

    private ILoadPanelItemListener mItemListener = new ILoadPanelItemListener() {
        @Override
        public void onResult(boolean success) {
            if (success) {
                mLoadingView.setVisibility(View.GONE);
                mNoInfoView.setVisibility(View.GONE);
            } else {
                mLoadingView.setVisibility(View.GONE);
                mNoInfoView.setVisibility(View.VISIBLE);
            }
        }
    };

    public PopupWindow getPopupWindow() {
        return mPanelWindow.getPanelWindow();
    }

    private ILayoutChangeListener mLayoutListener = new ILayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
            if (mPanelWindow != null && mPanelWindow.isShowing() && mPanelWindow.getCurrentPanelSpec() == SignaturePanelModule.this) {
                if (oldWidth != newWidth || oldHeight != newHeight) {
                    mPanelWindow.update(newWidth, newHeight);
                }
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private void initTopBar() {
        mTopBarView = View.inflate(mContext, R.layout.panel_signature_topbar, null);
        View closeView = mTopBarView.findViewById(R.id.panel_signature_top_close_iv);
        TextView topTitle = mTopBarView.findViewById(R.id.rv_panel_signature_title);
        if (mIsPad) {
            closeView.setVisibility(View.GONE);
        } else {
            closeView.setVisibility(View.VISIBLE);
            closeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPanelWindow.isShowing()) {
                        mPanelWindow.dismiss();
                    }
                }
            });
        }
        View topNormalView = mTopBarView.findViewById(R.id.panel_signature_top_normal);
        topNormalView.setVisibility(View.VISIBLE);

        if (mIsPad) {
            FrameLayout.LayoutParams topNormalLayoutParams = (FrameLayout.LayoutParams) topNormalView.getLayoutParams();
            topNormalLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
            topNormalView.setLayoutParams(topNormalLayoutParams);

            RelativeLayout.LayoutParams topCloseLayoutParams = (RelativeLayout.LayoutParams) closeView.getLayoutParams();
            topCloseLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
        } else {
            FrameLayout.LayoutParams topNormalLayoutParams = (FrameLayout.LayoutParams) topNormalView.getLayoutParams();
            topNormalLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_phone);
            topNormalView.setLayoutParams(topNormalLayoutParams);

            RelativeLayout.LayoutParams topTitleLayoutParams = (RelativeLayout.LayoutParams) topTitle.getLayoutParams();
            topTitleLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            topTitleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            topTitleLayoutParams.leftMargin = mDisplay.dp2px(70.0f);
            topTitle.setLayoutParams(topTitleLayoutParams);

            RelativeLayout.LayoutParams topCloseLayoutParams = (RelativeLayout.LayoutParams) closeView.getLayoutParams();
            topCloseLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            closeView.setLayoutParams(topCloseLayoutParams);
        }

        mContentView = View.inflate(mContext, R.layout.panel_signature_content, null);
        mNoInfoView = (TextView) mContentView.findViewById(R.id.rv_panel_signature_noinfo);
        mLoadingView = (TextView) mContentView.findViewById(R.id.rv_panel_signature_loading);
        View listContentView = mContentView.findViewById(R.id.rv_panel_signature_layout);
        RecyclerView recyclerView = (RecyclerView) mContentView.findViewById(R.id.rv_panel_signature_list);

        listContentView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mSigAdapter.reset();
                mSigAdapter.notifyDataSetChanged();
                return true;
            }
        });

        recyclerView.setAdapter(mSigAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int index = mSigAdapter.getIndex();
                if (index != -1) {
                    mSigAdapter.reset();
                    mSigAdapter. notifyItemChanged(index);
                }
                return false;
            }
        });
    }

    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
            mIsNeedRefreshPanel = true;
            mSigAdapter.clearItems();
        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode == Constants.e_ErrSuccess) {
                bDocClosed = false;
            }
        }

        @Override
        public void onDocWillClose(PDFDoc document) {

        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
            bDocClosed = true;
            mSigAdapter.clearItems();
        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };

    private PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener() {

        @Override
        public void onPagesRemoved(boolean success, int[] pageIndexes) {
            if (mNoInfoView.getVisibility() == View.GONE) {
                mSigAdapter.load(mItemListener);
            }
        }

        @Override
        public void onPageMoved(boolean success, int index, int dstIndex) {
            if (mNoInfoView.getVisibility() == View.GONE) {
                mSigAdapter.load(mItemListener);
            }
        }

        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] pageRanges) {
            if (mNoInfoView.getVisibility() == View.GONE) {
                mSigAdapter.load(mItemListener);
            }
        }
    };

    private IRedactionEventListener mRedactionEventListener = new IRedactionEventListener() {
        @Override
        public void onAnnotWillApply(PDFPage page, Annot annot) {
        }

        @Override
        public void onAnnotApplied(PDFPage page, Annot annot) {
            if (mNoInfoView.getVisibility() == View.GONE) {
                mSigAdapter.load(mItemListener);
            }
        }
    };

    private AnnotEventListener mAnnotEventListener = new AnnotEventListener() {
        @Override
        public void onAnnotAdded(PDFPage page, Annot annot) {
            try {
                if (annot.getType() == Annot.e_Widget &&
                        ((Widget) annot).getField().getType() == Field.e_TypeSignature) {
                    Message message = Message.obtain();
                    message.what = SIGNATURE_ADD;
                    mHandler.sendMessage(message);

                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAnnotWillDelete(PDFPage page, Annot annot) {
            try {
                if (annot.getType() == Annot.e_Widget &&
                        ((Widget) annot).getField().getType() == Field.e_TypeSignature) {
                    Message message = Message.obtain();
                    message.what = SIGNATURE_DELETE;
                    message.obj = String.valueOf(annot.getDict().getObjNum());
                    mHandler.sendMessage(message);

                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAnnotDeleted(PDFPage page, Annot annot) {

        }

        @Override
        public void onAnnotModified(PDFPage page, Annot annot) {

        }

        @Override
        public void onAnnotChanged(Annot lastAnnot, Annot currentAnnot) {

        }
    };

    private static final int SIGNATURE_ADD = 0;
    private static final int SIGNATURE_DELETE = 1;
    private Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case SIGNATURE_ADD:
                    mIsNeedRefreshPanel = true;
                    break;
                case SIGNATURE_DELETE:
                    if (!bDocClosed) {
                        mSigAdapter.delete((String) msg.obj);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private FxProgressDialog mProgressDialog;
    private void showProgressDialog() {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        if (uiExtensionsManager.getAttachedActivity() == null) {
            UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.rv_unknown_error));
            return;
        }
        mProgressDialog = new FxProgressDialog(uiExtensionsManager.getAttachedActivity(),
                AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_opening));

        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    public boolean onKeyBack() {
        if (mSignedVersionView == null) {
            return false;
        } else if (mSignedVersionView.getVisibility() == View.VISIBLE) {
            mSignedVersionView.close();
            return true;
        }
        return false;
    }
}
