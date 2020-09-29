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
package com.foxit.uiextensions.modules.panel.filespec;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.MarkupArray;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotEventListener;
import com.foxit.uiextensions.annots.IFlattenEventListener;
import com.foxit.uiextensions.annots.IGroupEventListener;
import com.foxit.uiextensions.annots.IImportAnnotsEventListener;
import com.foxit.uiextensions.annots.IRedactionEventListener;
import com.foxit.uiextensions.annots.multiselect.GroupManager;
import com.foxit.uiextensions.config.Config;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UIFileSelectDialog;
import com.foxit.uiextensions.controls.panel.PanelSpec;
import com.foxit.uiextensions.modules.panel.PanelWindow;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.OnPageEventListener;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FileSpecPanelModule implements Module, PanelSpec, FileSpecModuleCallback {
    private static final int ADD_ATTACCHMENT_ANNOT = 111;
    private static final int MODIFIED_ATTACCHMENT_ANNOT = 222;
    private static final int DELETTE_ATTACCHMENT_ANNOT = 333;
    private static final int GROUP_ATTACCHMENT_ANNOT = 444;
    private static final int UPDATE_ATTACCHMENT_BY_DELETE_GROUP_ANNOT = 555;

    private PDFViewCtrl mPdfViewCtrl;
    private Context mContext;
    private ViewGroup mParent;
    private AppDisplay mDisplay;
    private View mTopBarView;
    private Boolean mIsPad;
    private View mAddView;

    private View mContentView;
    private TextView mNoInfoView;
    private TextView mLoadingView;
    private View listContentView;

    private FileSpecOpenView openView;

    private FileAttachmentAdapter mFileAttachmentAdapter;
    private boolean mIsLoadAnnotation = true;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    private boolean isNeedRefreshPanel = true;
    private boolean bDocClosed = false;

    private PanelWindow mPanelWindow;

    public FileSpecPanelModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        if (context == null || pdfViewCtrl == null) {
            throw new NullPointerException();
        }
        mContext = context.getApplicationContext();
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
        mParent = parent;
        mDisplay = new AppDisplay(mContext);
        mIsPad = mDisplay.isPad();
        mFileAttachmentAdapter = new FileAttachmentAdapter(mContext, new ArrayList(), mPdfViewCtrl, this);
        if (uiExtensionsManager instanceof UIExtensionsManager) {
            Config config = ((UIExtensionsManager) mUiExtensionsManager).getConfig();
            mIsLoadAnnotation = config.modules.isLoadAnnotations();
        }

    }

    private UIExtensionsManager.ConfigurationChangedListener mConfigurationChangedListener = new UIExtensionsManager.ConfigurationChangedListener() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            if (mPanelWindow != null && mPanelWindow.isShowing() && mPanelWindow.getCurrentPanelSpec() == FileSpecPanelModule.this) {
                mPanelWindow.update();
            }

            if (mfileSelectDialog != null && mfileSelectDialog.isShowing()) {
                mfileSelectDialog.setHeight(mfileSelectDialog.getDialogHeight());
                mfileSelectDialog.showDialog(false);
            }

            if (mFileAttachmentAdapter != null) {
                mFileAttachmentAdapter.onConfigurationChanged(newConfig);
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager instanceof UIExtensionsManager) {
            mPanelWindow = new PanelWindow(mContext, mPdfViewCtrl, mUiExtensionsManager);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }

        mTopBarView = View.inflate(mContext, R.layout.panel_filespec_topbar, null);
        View closeView = mTopBarView.findViewById(R.id.panel_filespec_top_close_iv);
        TextView topTitle = mTopBarView.findViewById(R.id.rv_panel_files_pec_title);
        mAddView = mTopBarView.findViewById(R.id.panel_filespec_top_clear_tv);

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
        View topNormalView = mTopBarView.findViewById(R.id.panel_filespec_top_normal);
        topNormalView.setVisibility(View.VISIBLE);

        if (mIsPad) {
            FrameLayout.LayoutParams topNormalLayoutParams = (FrameLayout.LayoutParams) topNormalView.getLayoutParams();
            topNormalLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_toolbar_height_pad);
            topNormalView.setLayoutParams(topNormalLayoutParams);

            RelativeLayout.LayoutParams topCloseLayoutParams = (RelativeLayout.LayoutParams) closeView.getLayoutParams();
            topCloseLayoutParams.leftMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
            closeView.setLayoutParams(topCloseLayoutParams);
            RelativeLayout.LayoutParams topClearLayoutParams = (RelativeLayout.LayoutParams) mAddView.getLayoutParams();
            topClearLayoutParams.rightMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
            mAddView.setLayoutParams(topClearLayoutParams);
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
            RelativeLayout.LayoutParams topClearLayoutParams = (RelativeLayout.LayoutParams) mAddView.getLayoutParams();
            topClearLayoutParams.rightMargin = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
            mAddView.setLayoutParams(topClearLayoutParams);
        }


        mContentView = View.inflate(mContext, R.layout.panel_filespec_content, null);
        mNoInfoView = (TextView) mContentView.findViewById(R.id.rv_panel_filespec_noinfo);
        mLoadingView = (TextView) mContentView.findViewById(R.id.rv_panel_filespec_loading);
        listContentView = mContentView.findViewById(R.id.rv_panel_attachment_layout);
        RecyclerView recyclerView = (RecyclerView) mContentView.findViewById(R.id.rv_panel_filespec_list);

        mAddView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mFileAttachmentAdapter.reset();
                mFileAttachmentAdapter.notifyDataSetChanged();
                showFileSelectDialog();
            }
        });

        listContentView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mFileAttachmentAdapter.reset();
                mFileAttachmentAdapter.notifyDataSetChanged();
                return true;
            }
        });

        recyclerView.setAdapter(mFileAttachmentAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int index = mFileAttachmentAdapter.getSelectedIndex();
                if (index != -1) {
                    mFileAttachmentAdapter.reset();
                    mFileAttachmentAdapter.notifyItemChanged(index);
                }
                return false;
            }
        });

        //mRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL_LIST));
        mPanelWindow.addPanelSpec(this);
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        mPdfViewCtrl.registerPageEventListener(mPageEventListener);
        mPdfViewCtrl.registerRecoveryEventListener(recoveryEventListener);
        if (mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().registerAnnotEventListener(mAnnotEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().registerImportedAnnotsEventListener(mImportAnnotEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().registerFlattenEventListener(mFlattenEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().registerRedactionEventListener(mRedactionEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().registerGroupEventListener(mGroupEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerConfigurationChangedListener(mConfigurationChangedListener);
        }
        return true;
    }

    private String mPath;
    private String name;
    private static final int MAX_ATTACHMENT_FILE_SIZE = 1024 * 1024 * 300;
    private int MaxFileSize;
    private UIFileSelectDialog mfileSelectDialog;

    private void showFileSelectDialog() {
        if (mfileSelectDialog != null && mfileSelectDialog.isShowing()) return;

        MaxFileSize = MAX_ATTACHMENT_FILE_SIZE;
        Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) {
            return;
        }

        mfileSelectDialog = new UIFileSelectDialog(context);
        mfileSelectDialog.init(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isHidden() || !pathname.canRead()) return false;
                return true;
            }
        }, true);
        mfileSelectDialog.setTitle(context.getApplicationContext().getString(R.string.fx_string_open));
        mfileSelectDialog.setButton(UIMatchDialog.DIALOG_CANCEL | UIMatchDialog.DIALOG_OK);
        mfileSelectDialog.setButtonEnable(false, UIMatchDialog.DIALOG_OK);
        mfileSelectDialog.setListener(new UIMatchDialog.DialogListener() {
            @Override
            public void onResult(long btType) {
                if (btType == UIMatchDialog.DIALOG_OK) {

                    mPath = mfileSelectDialog.getSelectedFiles().get(0).path;
                    name = mfileSelectDialog.getSelectedFiles().get(0).name;
                    if (mPath == null || mPath.length() < 1) return;

                    //check file size
                    if (new File(mPath).length() > MaxFileSize) {
                        String msg = String.format(AppResource.getString(mContext, R.string.annot_fat_filesizelimit_meg),
                                MaxFileSize / (1024 * 1024));
                        Toast toast = Toast.makeText(mContext,
                                msg, Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }

                    mFileAttachmentAdapter.add(name, mPath);
                    mfileSelectDialog.dismiss();
                } else if (btType == UIMatchDialog.DIALOG_CANCEL) {
                    mfileSelectDialog.dismiss();
                }
            }

            @Override
            public void onBackClick() {
            }
        });
        mfileSelectDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    mfileSelectDialog.dismiss();
                }
                return true;
            }
        });

        mfileSelectDialog.showDialog(false);
    }


    private PDFViewCtrl.IPageEventListener mPageEventListener = new OnPageEventListener() {

        @Override
        public void onPagesRemoved(boolean success, int[] pageIndexes) {
            if (mNoInfoView.getVisibility() == View.GONE) {
                refreshPanel(false);
            }
        }

        @Override
        public void onPageMoved(boolean success, int index, int dstIndex) {
            if (mNoInfoView.getVisibility() == View.GONE) {
                refreshPanel(false);
            }
        }

        @Override
        public void onPagesInserted(boolean success, int dstIndex, int[] range) {
            if (mNoInfoView.getVisibility() == View.GONE) {
                refreshPanel(false);
            }
        }
    };

    private void refreshPanel(boolean reInitNameTree) {
        mFileAttachmentAdapter.initPDFNameTree(reInitNameTree);

        mFileAttachmentAdapter.init(mIsLoadAnnotation);
        mFileAttachmentAdapter.notifyUpdateData();
    }

    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
            mFileAttachmentAdapter.clearItems();
        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode == Constants.e_ErrSuccess) {
                refreshPanel(true);
                bDocClosed = false;
            }
        }

        @Override
        public void onDocWillClose(PDFDoc document) {
            mFileAttachmentAdapter.clearItems();
        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
            bDocClosed = true;
        }

        @Override
        public void onDocWillSave(PDFDoc document) {

        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {

        }
    };


    private PDFViewCtrl.IRecoveryEventListener recoveryEventListener = new PDFViewCtrl.IRecoveryEventListener() {
        @Override
        public void onWillRecover() {

        }

        @Override
        public void onRecovered() {
            mFileAttachmentAdapter.reInit();
            mFileAttachmentAdapter.init(mIsLoadAnnotation);
        }
    };

    private AnnotEventListener mAnnotEventListener = new AnnotEventListener() {
        @Override
        public void onAnnotAdded(PDFPage page, Annot annot) {
            try {
                if (annot.getType() == Annot.e_FileAttachment) {
                    Message message = Message.obtain();
                    message.what = ADD_ATTACCHMENT_ANNOT;
                    message.obj = annot;
                    mHandler.sendMessage(message);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAnnotWillDelete(PDFPage page, Annot annot) {
            try {
                if (GroupManager.getInstance().isGrouped(mPdfViewCtrl, annot)) {

                    MarkupArray markupArray = ((Markup) annot).getGroupElements();
                    if (markupArray.getSize() == 2) {
                        ArrayList<Annot> groupAnnots = new ArrayList<>();
                        for (int i = 0; i < markupArray.getSize(); i++) {
                            Annot groupAnnot = markupArray.getAt(i);
                            if (AppAnnotUtil.getAnnotUniqueID(groupAnnot).equals(AppAnnotUtil.getAnnotUniqueID(annot)))
                                continue;
                            groupAnnots.add(groupAnnot);
                        }

                        if (groupAnnots.size() == 1 && groupAnnots.get(0).getType() == Annot.e_FileAttachment) {
                            Message message = Message.obtain();
                            message.what = UPDATE_ATTACCHMENT_BY_DELETE_GROUP_ANNOT;
                            message.obj = groupAnnots.get(0);
                            mHandler.sendMessage(message);
                        }
                    }
                }

                if (annot.getType() == Annot.e_FileAttachment) {
                    Message message = Message.obtain();
                    message.what = DELETTE_ATTACCHMENT_ANNOT;
                    message.obj = annot;
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
            try {
                if (annot.getType() == Annot.e_FileAttachment) {
                    Message message = Message.obtain();
                    message.what = MODIFIED_ATTACCHMENT_ANNOT;
                    message.obj = annot;
                    mHandler.sendMessage(message);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAnnotChanged(Annot lastAnnot, Annot currentAnnot) {

        }
    };

    private IFlattenEventListener mFlattenEventListener = new IFlattenEventListener() {
        @Override
        public void onAnnotWillFlatten(PDFPage page, Annot annot) {
            try {
                if (GroupManager.getInstance().isGrouped(mPdfViewCtrl, annot)) {

                    MarkupArray markupArray = ((Markup) annot).getGroupElements();
                    if (markupArray.getSize() == 2) {
                        ArrayList<Annot> groupAnnots = new ArrayList<>();
                        for (int i = 0; i < markupArray.getSize(); i++) {
                            Annot groupAnnot = markupArray.getAt(i);
                            if (AppAnnotUtil.getAnnotUniqueID(groupAnnot).equals(AppAnnotUtil.getAnnotUniqueID(annot)))
                                continue;
                            groupAnnots.add(groupAnnot);
                        }

                        if (groupAnnots.size() == 1 && groupAnnots.get(0).getType() == Annot.e_FileAttachment) {
                            Message message = Message.obtain();
                            message.what = UPDATE_ATTACCHMENT_BY_DELETE_GROUP_ANNOT;
                            message.obj = groupAnnots.get(0);
                            mHandler.sendMessage(message);
                        }
                    }
                }

                if (annot.getType() == Annot.e_FileAttachment) {
                    Message message = Message.obtain();
                    message.what = DELETTE_ATTACCHMENT_ANNOT;
                    message.obj = annot;
                    mHandler.sendMessage(message);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAnnotFlattened(PDFPage page, Annot annot) {
        }
    };

    private IRedactionEventListener mRedactionEventListener = new IRedactionEventListener() {
        @Override
        public void onAnnotWillApply(PDFPage page, Annot annot) {
        }

        @Override
        public void onAnnotApplied(PDFPage page, Annot annot) {
            if (mNoInfoView.getVisibility() == View.GONE) {
                refreshPanel(false);
            }
        }
    };

    private IImportAnnotsEventListener mImportAnnotEventListener = new IImportAnnotsEventListener() {
        @Override
        public void onAnnotsImported() {
            refreshPanel(true);
        }
    };

    private IGroupEventListener mGroupEventListener = new IGroupEventListener() {
        @Override
        public void onAnnotGrouped(PDFPage page, List<Annot> groupAnnots) {
            updateFileBean(groupAnnots);
        }

        @Override
        public void onAnnotUnGrouped(PDFPage page, List<Annot> unGroupAnnots) {
            updateFileBean(unGroupAnnots);
        }

        private void updateFileBean(List<Annot> annots) {
            try {
                for (int i = 0; i < annots.size(); i++) {
                    Annot annot = annots.get(i);
                    if (annot != null
                            && !annot.isEmpty()
                            && annot.getType() == Annot.e_FileAttachment) {
                        Message message = Message.obtain();
                        message.what = GROUP_ATTACCHMENT_ANNOT;
                        message.obj = annot;
                        mHandler.sendMessage(message);
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case ADD_ATTACCHMENT_ANNOT:
                    isNeedRefreshPanel = true;
                    break;
                case MODIFIED_ATTACCHMENT_ANNOT:
                    if (!bDocClosed) {
                        mFileAttachmentAdapter.updateByOutside((Annot) msg.obj);
                    }
                    break;
                case GROUP_ATTACCHMENT_ANNOT:
                    if (!bDocClosed) {
                        mFileAttachmentAdapter.updateByGroup((Annot) msg.obj);
                    }
                    break;
                case DELETTE_ATTACCHMENT_ANNOT:
                    if (!bDocClosed) {
                        mFileAttachmentAdapter.deleteByOutside((Annot) msg.obj);
                    }
                    break;
                case UPDATE_ATTACCHMENT_BY_DELETE_GROUP_ANNOT:
                    if (!bDocClosed) {
                        mFileAttachmentAdapter.updateByDeleteGroupAnnot((Annot) msg.obj);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (openView == null) {
            return false;
        } else if (openView.getVisibility() == View.VISIBLE && keyCode == KeyEvent.KEYCODE_BACK) {
            openView.closeAttachment();
            openView.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    public boolean onKeyBack() {
        if (openView == null) {
            return false;
        } else if (openView.getVisibility() == View.VISIBLE) {
            openView.closeAttachment();
            openView.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    @Override
    public boolean unloadModule() {
        mPanelWindow.removePanelSpec(this);
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mPdfViewCtrl.unregisterPageEventListener(mPageEventListener);
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterAnnotEventListener(mAnnotEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterImportedAnnotsEventListener(mImportAnnotEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterFlattenEventListener(mFlattenEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterRedactionEventListener(mRedactionEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().unregisterGroupEventListener(mGroupEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).unregisterConfigurationChangedListener(mConfigurationChangedListener);
        }
        return true;
    }

    @Override
    public String getName() {
        return MODULE_NAME_FILE_PANEL;
    }

    @Override
    public PanelType getPanelType() {
        return PanelType.Attachments;
    }

    @Override
    public int getIcon() {
        return R.drawable.panel_tabimg_attachment_seletor;
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
        mFileAttachmentAdapter.initPDFNameTree(false);
        if (isNeedRefreshPanel) {
            mLoadingView.setVisibility(View.VISIBLE);
            mNoInfoView.setVisibility(View.GONE);
            mFileAttachmentAdapter.init(mIsLoadAnnotation);
            isNeedRefreshPanel = false;
        }
        if (openView == null) {
            openView = new FileSpecOpenView(mContext, mPdfViewCtrl, mParent);
        }
        boolean enable = !mPdfViewCtrl.isDynamicXFA() && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyContents();
        mAddView.setEnabled(enable);
    }

    @Override
    public void onDeactivated() {
        openView = null;
    }

    @Override
    public void success() {
        mLoadingView.setVisibility(View.GONE);
        mNoInfoView.setVisibility(View.GONE);
    }

    @Override
    public void fail() {
        mLoadingView.setVisibility(View.GONE);
        mNoInfoView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDocOpenPrepare() {
        showProgressDlg();
    }

    @Override
    public void onDocOpenStart(String path, String filename) {
        openView.openAttachment(path, filename, new IAttachmentDocEvent() {
            @Override
            public void onAttachmentDocWillOpen() {
                showProgressDlg();
            }

            @Override
            public void onAttachmentDocOpened(PDFDoc document, int errCode) {
                dismissProgressDlg();
            }

            @Override
            public void onAttachmentDocWillClose() {
            }

            @Override
            public void onAttachmentDocClosed() {
            }
        });
        openView.setVisibility(View.VISIBLE);
        mPanelWindow.dismiss();
    }

    @Override
    public void onDocOpenFinished() {
        dismissProgressDlg();
    }

    private ProgressDialog mProgressDlg;

    private void showProgressDlg() {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();

        if (mProgressDlg == null && uiExtensionsManager.getAttachedActivity() != null) {
            mProgressDlg = new ProgressDialog(uiExtensionsManager.getAttachedActivity());
            mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDlg.setCancelable(false);
            mProgressDlg.setIndeterminate(false);
        }

        if (mProgressDlg != null && !mProgressDlg.isShowing()) {
            mProgressDlg.setMessage(mContext.getString(R.string.fx_string_opening));
            AppDialogManager.getInstance().showAllowManager(mProgressDlg, null);
        }
    }

    private void dismissProgressDlg() {
        if (mProgressDlg != null && mProgressDlg.isShowing()) {
            AppDialogManager.getInstance().dismiss(mProgressDlg);
            mProgressDlg = null;
        }
    }

}
