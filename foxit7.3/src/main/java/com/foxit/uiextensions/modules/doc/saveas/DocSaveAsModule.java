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
package com.foxit.uiextensions.modules.doc.saveas;


import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.link.LinkModule;
import com.foxit.uiextensions.controls.dialog.FxProgressDialog;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UIDialog;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UISaveAsDialog;
import com.foxit.uiextensions.controls.dialog.saveas.SaveAsBean;
import com.foxit.uiextensions.controls.dialog.saveas.UIDocSaveAsDialog;
import com.foxit.uiextensions.controls.menu.MoreMenuModule;
import com.foxit.uiextensions.controls.menu.MoreMenuView;
import com.foxit.uiextensions.home.local.LocalModule;
import com.foxit.uiextensions.modules.doc.FlattenPDF;
import com.foxit.uiextensions.modules.doc.OptimizerPDF;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppTheme;
import com.foxit.uiextensions.utils.UIToast;
import com.foxit.uiextensions.utils.thread.AppThreadManager;

import java.io.File;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class DocSaveAsModule implements Module {

    private PDFViewCtrl mPdfViewCtrl;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;
    private Context mContext;
    private FxProgressDialog mProgressDialog;
    private UISaveAsDialog mSaveAsDialog;
    private UIDocSaveAsDialog mDocSaveAsDialog;
    private CompositeDisposable mDisposable;

    private String mFileExt = "pdf";
    private String mCacheSavePath;
    private String mSavePath;
    private String mPassword;
    private SaveAsBean mSaveAsBean;
    private int mFormat;
    private long mOriginSize;
    private boolean isSaveDocInCurPath = false;
    private boolean mIsSavingDoc = false;

    public DocSaveAsModule(Context context, PDFViewCtrl pdfViewCtrl, UIExtensionsManager uiExtensionsManager) {
        this.mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return MODULE_NAME_SAVE_AS;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null) {
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
            ((UIExtensionsManager) mUiExtensionsManager).registerConfigurationChangedListener(mConfigurationChangedListener);
        }
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mDisposable != null)
            mDisposable.clear();

        if (mUiExtensionsManager != null) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterConfigurationChangedListener(mConfigurationChangedListener);
        }
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        return true;
    }

    public void showSaveAsDialog() {
        mDocSaveAsDialog = new UIDocSaveAsDialog(((UIExtensionsManager) mUiExtensionsManager).getAttachedActivity());
        int formats = UIDocSaveAsDialog.FORMAT_ORIGINAL;
        DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
        if (documentManager.canAddAnnot() && documentManager.canModifyContents()) {
            formats |= UIDocSaveAsDialog.FORMAT_FLATTEN;
        }
        if (!documentManager.isXFA()
                && !documentManager.simpleCheckPDFA(mPdfViewCtrl.getDoc())
                && documentManager.canModifyContents()
                && documentManager.canAddAnnot()
                && documentManager.canFillForm()) {
            formats |= UIDocSaveAsDialog.FORMAT_OPTIMIZE;
        }
        mDocSaveAsDialog.setFormatItems(formats);
        mDocSaveAsDialog.setFileName(AppFileUtil.getFileNameWithoutExt(mPdfViewCtrl.getFilePath()));
        mDocSaveAsDialog.setListener(new MatchDialog.DialogListener() {

            @Override
            public void onResult(long btType) {
                if (btType == UIMatchDialog.DIALOG_OK) {
                    MoreMenuModule moreMenuModule = (MoreMenuModule) ((UIExtensionsManager) mUiExtensionsManager).getModuleByName(Module.MODULE_MORE_MENU);
                    if (moreMenuModule != null) {
                        MoreMenuView view = moreMenuModule.getView();
                        if (view != null)
                            view.hide();
                    }

                    mFormat = mDocSaveAsDialog.getFormat();
                    mFileExt = mDocSaveAsDialog.getFileExt();
                    mSaveAsBean = mDocSaveAsDialog.getSaveAsBean();
                    showSaveDocumentDialog(mDocSaveAsDialog.getFileName(), mFileExt);
                }
                mDocSaveAsDialog.dismiss();
            }

            @Override
            public void onBackClick() {
            }
        });
        mDocSaveAsDialog.showDialog();
    }

    private void showSaveDocumentDialog(final String fileName, String fileExt) {
        mSaveAsDialog = new UISaveAsDialog(((UIExtensionsManager) mUiExtensionsManager).getAttachedActivity(), fileName, fileExt,
                new UISaveAsDialog.ISaveAsOnOKClickCallBack() {
                    @Override
                    public void onOkClick(String newFilePath) {
                        if (newFilePath.equalsIgnoreCase(mPdfViewCtrl.getFilePath())) {
                            isSaveDocInCurPath = true;
                            mSavePath = mPdfViewCtrl.getFilePath();
                            mCacheSavePath = getCacheFile();
                            doSaveDoc(mCacheSavePath);
                        } else {
                            isSaveDocInCurPath = false;
                            mSavePath = newFilePath;
                            doSaveDoc(newFilePath);
                        }
                    }

                    @Override
                    public void onCancelClick() {
                    }
                });
        mSaveAsDialog.showDialog(true, fileName);
    }


    private String getCacheFile() {
        File file = new File(mPdfViewCtrl.getFilePath());
        String dir = file.getParent() + "/";
        String path = null;
        while (file.exists()) {
            path = dir + AppDmUtil.randomUUID(null) + "." + mFileExt;
            file = new File(path);
        }
        return path;
    }

    private void doSaveDoc(final String path) {
        final Activity activity = ((UIExtensionsManager) mUiExtensionsManager).getAttachedActivity();
        if (mFormat == UIDocSaveAsDialog.FORMAT_ORIGINAL) {
            showProgressDialog(AppResource.getString(mContext, R.string.fx_string_saving));

            int saveFlag = PDFDoc.e_SaveFlagNormal;
            try {
                saveFlag = mPdfViewCtrl.getDoc().getSignatureCount() > 0 ? PDFDoc.e_SaveFlagIncremental : PDFDoc.e_SaveFlagNormal;
            } catch (PDFException e) {
                e.printStackTrace();
            }
            mIsSavingDoc = true;
            mPdfViewCtrl.saveDoc(path, saveFlag);
        } else if (mFormat == UIDocSaveAsDialog.FORMAT_FLATTEN) {
            final UITextEditDialog dialog = new UITextEditDialog(activity);
            dialog.getInputEditText().setVisibility(View.GONE);
            dialog.setTitle(AppResource.getString(mContext, R.string.rv_saveas_flatten));
            dialog.getPromptTextView().setText(AppResource.getString(mContext, R.string.fx_flatten_doc_toast));
            dialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mIsSavingDoc = true;
                    showProgressDialog(AppResource.getString(mContext, R.string.fx_string_saving));
                    if (mDisposable == null)
                        mDisposable = new CompositeDisposable();

                    if (mPdfViewCtrl.isDynamicXFA()) {
                        mDisposable.add(FlattenPDF.doFlattenXFADoc(mPdfViewCtrl.getXFADoc(), path)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<Boolean>() {
                                    @Override
                                    public void accept(Boolean b) throws Exception {
                                        if (b != null && b) {
                                            openNewFile();
                                        } else {
                                            mIsSavingDoc = false;
                                            dismissProgressDialog();
                                            UIToast.getInstance(mContext).show(AppResource.getString(mContext, R.string.fx_save_file_failed));
                                        }
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        mIsSavingDoc = false;
                                        dismissProgressDialog();
                                        UIToast.getInstance(mContext).show(AppResource.getString(mContext, R.string.fx_save_file_failed));
                                    }
                                }));
                    } else {
                        mDisposable.add(FlattenPDF.doFlattenPDFDoc(mPdfViewCtrl.getDoc(), true, PDFPage.e_FlattenAll)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<Boolean>() {
                                    @Override
                                    public void accept(Boolean b) throws Exception {
                                        if (b != null && b) {
                                            mPdfViewCtrl.saveDoc(path, PDFDoc.e_SaveFlagNormal);
                                        } else {
                                            mIsSavingDoc = false;
                                            dismissProgressDialog();
                                            UIToast.getInstance(mContext).show(AppResource.getString(mContext, R.string.fx_save_file_failed));
                                        }
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        mIsSavingDoc = false;
                                        dismissProgressDialog();
                                        UIToast.getInstance(mContext).show(AppResource.getString(mContext, R.string.fx_save_file_failed));
                                    }
                                }));
                    }

                    dialog.dismiss();
                }
            });
            dialog.show();
        } else if (mFormat == UIDocSaveAsDialog.FORMAT_OPTIMIZE) {
            if (mSaveAsBean == null)
                return;

            mIsSavingDoc = true;
            mOriginSize = AppFileUtil.getFileSize(mPdfViewCtrl.getFilePath());

            showProgressDialog(AppResource.getString(mContext, R.string.rv_saveas_optimize_optimizing));
            if (mDisposable == null)
                mDisposable = new CompositeDisposable();

            mDisposable.add(OptimizerPDF.doOptimizerPDF(mPdfViewCtrl.getDoc(), mSaveAsBean.imageSettings.quality, mSaveAsBean.monoSettings.quality)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean b) throws Exception {
                            if (b != null && b) {
                                mPdfViewCtrl.saveDoc(path, PDFDoc.e_SaveFlagRemoveRedundantObjects);
                            } else {
                                mIsSavingDoc = false;
                                dismissProgressDialog();
                                showOptimizerToast(2, AppResource.getString(mContext, R.string.fx_save_file_failed));
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            mIsSavingDoc = false;
                            dismissProgressDialog();
                            showOptimizerToast(2, AppResource.getString(mContext, R.string.fx_save_file_failed));
                        }
                    }));
        }

    }

    private void showProgressDialog(String message) {
        if (mProgressDialog == null) {
            final Activity activity = ((UIExtensionsManager) mUiExtensionsManager).getAttachedActivity();
            mProgressDialog = new FxProgressDialog(activity, AppResource.getString(mContext, R.string.rv_panel_annot_loading_start));
        }
        mProgressDialog.setTips(message);
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
    }

    private void openNewFile() {
        if (isSaveDocInCurPath) {
            if (mCacheSavePath == null) return;

            File file = new File(mCacheSavePath);
            File docFile = new File(mPdfViewCtrl.getFilePath());
            if (file.exists()) {
                docFile.delete();
                if (!file.renameTo(docFile)) {
                    UIToast.getInstance(mContext).show(AppResource.getString(mContext, R.string.fx_save_file_failed));
                    return;
                }
            } else {
                UIToast.getInstance(mContext).show(AppResource.getString(mContext, R.string.fx_save_file_failed));
                return;
            }
        }

        mPdfViewCtrl.cancelAllTask();
        ((UIExtensionsManager) mUiExtensionsManager).triggerDismissMenuEvent();
        ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().setCurrentAnnot(null);
        ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().clearUndoRedo();
        ((UIExtensionsManager) mUiExtensionsManager).getDocumentManager().setDocModified(false);
        updateThumbnail(mSavePath);

        LinkModule linkModule = (LinkModule) ((UIExtensionsManager) mUiExtensionsManager).getModuleByName(Module.MODULE_NAME_LINK);
        if (linkModule != null)
            linkModule.clear();
        byte[] password = null;
        if (getPassword() != null)
            password = getPassword().getBytes();
        mPdfViewCtrl.openDoc(mSavePath, password);
    }

    private void updateThumbnail(String path) {
        LocalModule module = (LocalModule) ((UIExtensionsManager) mUiExtensionsManager)
                .getModuleByName(Module.MODULE_NAME_LOCAL);
        if (module != null)
            module.updateThumbnail(path);
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        this.mPassword = password;
    }

    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            dismissProgressDialog();
            if (mIsSavingDoc) {
                mIsSavingDoc = false;
                if (mFormat == UIDocSaveAsDialog.FORMAT_OPTIMIZE) {
                    long newSize = AppFileUtil.getFileSize(mSavePath);
                    String strOrigSize = AppFileUtil.formatFileSize(mOriginSize);
                    String strNewSize = AppFileUtil.formatFileSize(newSize);
                    String strFormat = AppResource.getString(mContext, R.string.rv_saveas_optimize_toast);
                    String strToast = String.format(strFormat, strOrigSize, strNewSize);
                    int state = newSize <= mOriginSize ? 0 : 1;
                    showOptimizerToast(state, strToast);
                    mFormat = UIDocSaveAsDialog.FORMAT_UNKNOWN;
                }
            }
        }

        @Override
        public void onDocWillClose(PDFDoc document) {
        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
        }

        @Override
        public void onDocWillSave(PDFDoc document) {
        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {
            if (mIsSavingDoc) {
                if (errCode == Constants.e_ErrSuccess) {
                    openNewFile();
                } else {
                    UIToast.getInstance(mContext).show(AppResource.getString(mContext, R.string.fx_save_file_failed));
                }
            }
        }
    };

    private void showOptimizerToast(int stateCode, String msg) {
        UIDialog dlg = new UIDialog(((UIExtensionsManager) mUiExtensionsManager).getAttachedActivity(),
                R.layout.fx_saveas_optimize_toast, AppTheme.getDialogTheme());
        View contentView = dlg.getContentView();
        ImageView iv = (ImageView) contentView.findViewById(R.id.saveas_optimize_icon);
        if (stateCode == 0) {
            iv.setImageResource(R.drawable.fx_saveas_optimize_success);
        } else if (stateCode == 1) {
            iv.setImageResource(R.drawable.fx_saveas_optimize_reduce_failed);
        } else {
            iv.setImageResource(R.drawable.fx_saveas_optimize_save_failed);
        }
        TextView tv = (TextView) contentView.findViewById(R.id.saveas_optimize_toast);
        tv.setText(msg);
        if (dlg.getDialog().getWindow() != null)
            dlg.getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dlg.show();
    }

    private UIExtensionsManager.ConfigurationChangedListener mConfigurationChangedListener = new UIExtensionsManager.ConfigurationChangedListener() {
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            if (mSaveAsDialog != null && mSaveAsDialog.isShowing()) {
                mSaveAsDialog.setHeight(mSaveAsDialog.getDialogHeight());
                mSaveAsDialog.showDialog();
            }

            if (mDocSaveAsDialog != null && mDocSaveAsDialog.isShowing()) {
                AppThreadManager.getInstance().getMainThreadHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDocSaveAsDialog.setFileName(mDocSaveAsDialog.getFileName());
                    }
                }, 200);
            }
        }
    };

}
