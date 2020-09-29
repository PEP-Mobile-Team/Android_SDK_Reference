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
package com.foxit.uiextensions.controls.dialog.fileselect;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;

import java.io.File;
import java.io.FileFilter;


public class UISaveAsDialog {

    private Context mContext;

    private String mCurrentFilePath;
    private String mSaveExpandedName;
    private ISaveAsOnOKClickCallBack mCallback;

    private UIFolderSelectDialog mFolderSelectDialog;
    private boolean mOnlySelectFolder = false;
    private String mOrigName = "";

    public UISaveAsDialog(Context context, String currentFilePath, String saveExpandedName, ISaveAsOnOKClickCallBack callback) {
        mContext = context;
        mCurrentFilePath = currentFilePath;
        mSaveExpandedName = saveExpandedName;
        mCallback = callback;

        mFolderSelectDialog = new UIFolderSelectDialog(context);
        mFolderSelectDialog.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !(pathname.isHidden() || !pathname.canRead()) && !pathname.isFile();
            }
        });
        mFolderSelectDialog.setTitle(AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_saveas));
        mFolderSelectDialog.setButton(UIMatchDialog.DIALOG_OK | UIMatchDialog.DIALOG_CANCEL);
        mFolderSelectDialog.setListener(new UIMatchDialog.DialogListener() {
            @Override
            public void onResult(long btType) {
                if (btType == UIMatchDialog.DIALOG_OK) {
                    String fileFolder = mFolderSelectDialog.getCurrentPath();
                    if (mOnlySelectFolder) {
                        String newPath = fileFolder + "/" + mOrigName + "." + mSaveExpandedName;
                        File file = new File(newPath);
                        if (file.exists()) {
                            showAskReplaceDialog(newPath, fileFolder);
                        } else {
                            mCallback.onOkClick(newPath);
                        }
                    } else {
                        String fileName = AppFileUtil.getFileNameWithoutExt(mCurrentFilePath);
                        String newPath = fileFolder + "/" + fileName + "." + mSaveExpandedName;
                        showInputFileNameDialog(newPath);
                    }
                } else if (btType == UIMatchDialog.DIALOG_CANCEL) {
                    mCallback.onCancelClick();
                }
                mFolderSelectDialog.dismiss();
            }

            @Override
            public void onBackClick() {
            }
        });
        mFolderSelectDialog.showDialog();
    }

    public void showDialog() {
        mOnlySelectFolder = false;
        mFolderSelectDialog.showDialog();
    }

    public void showDialog(boolean onlySelectFolder, String origName) {
        mOnlySelectFolder = onlySelectFolder;
        mOrigName = origName;
        mFolderSelectDialog.showDialog();
    }

    public boolean isShowing() {
        return mFolderSelectDialog.isShowing();
    }

    public int getDialogHeight() {
        return mFolderSelectDialog.getDialogHeight();
    }

    public void setHeight(int height) {
        mFolderSelectDialog.setHeight(height);
    }

    private void showInputFileNameDialog(final String filePath) {
        final String newFilePath = AppFileUtil.getFileDuplicateName(filePath);
        final String fileName = AppFileUtil.getFileNameWithoutExt(newFilePath);
        final String fileFolder = AppFileUtil.getFileFolder(newFilePath);

        final UITextEditDialog rmDialog = new UITextEditDialog(mContext);
        rmDialog.setPattern("[/\\:*?<>|\"\n\t]");
        rmDialog.setTitle(AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_saveas));
        rmDialog.getPromptTextView().setVisibility(View.GONE);
        rmDialog.getInputEditText().setText(fileName);
        rmDialog.getInputEditText().selectAll();
        rmDialog.show();
        AppUtil.showSoftInput(rmDialog.getInputEditText());

        rmDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rmDialog.dismiss();
                String inputName = rmDialog.getInputEditText().getText().toString();
                String newPath = fileFolder + "/" + inputName + "." + mSaveExpandedName;
                File file = new File(newPath);
                if (file.exists()) {
                    showAskReplaceDialog(newPath, fileFolder);
                } else {
                    mCallback.onOkClick(newPath);
                }
            }
        });
        rmDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rmDialog.dismiss();
                mCallback.onCancelClick();
            }
        });
        rmDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mCancelListener != null) {
                    mCancelListener.onCancelListener();
                }
            }
        });
    }

    private void showAskReplaceDialog(final String filePath, final String fileFolder) {
        final UITextEditDialog rmDialog = new UITextEditDialog(mContext);
        rmDialog.setTitle(mContext.getApplicationContext().getString(R.string.fx_string_saveas));
        rmDialog.getPromptTextView().setText(mContext.getApplicationContext().getString(R.string.fx_string_filereplace_warning));
        rmDialog.getInputEditText().setVisibility(View.GONE);
        rmDialog.show();

        rmDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rmDialog.dismiss();
                mCallback.onOkClick(filePath);
            }
        });

        rmDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rmDialog.dismiss();
                showInputFileNameDialog(filePath);
            }
        });

        rmDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mCancelListener != null) {
                    mCancelListener.onCancelListener();
                }
            }
        });
    }

    public interface ISaveAsOnOKClickCallBack {
        void onOkClick(String newFilePath);

        void onCancelClick();
    }

    public interface ICancelListener {
        void onCancelListener();
    }

    private ICancelListener mCancelListener;

    public void setOnCancelListener(final ICancelListener listener) {
        mCancelListener = listener;
        mFolderSelectDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (listener != null) {
                    listener.onCancelListener();
                }
            }
        });
    }
}


