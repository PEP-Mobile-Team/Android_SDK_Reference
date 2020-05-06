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
package com.foxit.uiextensions.security.standard;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.Progressive;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.SecurityHandler;
import com.foxit.sdk.pdf.StdEncryptData;
import com.foxit.sdk.pdf.StdSecurityHandler;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.utils.AppUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class PasswordStandardSupport {

	private PasswordDialog mDialog;
	private boolean 				mIsOwner = false;
	private boolean					mIsDocOpenAuthEvent = true;

	private UITextEditDialog mCheckOwnerPWD;
	private EditText mEditText;

	private PasswordSettingFragment mSettingDialog;
	private PDFViewCtrl mPdfViewCtrl;
	private Context mContext;
	private String mFilePath = null;
	private boolean bSuccess = false;

	public PasswordStandardSupport(Context context, PDFViewCtrl pdfViewCtrl){
		mContext = context;
		mPdfViewCtrl = pdfViewCtrl;
	}

	public void setFilePath(String filePath) {
		this.mFilePath = filePath;
	}

	public String getFilePath() {
		return this.mFilePath;
	}

	public boolean checkOwnerPassword(String password) {
		if (password == null) return false;
		try {
			return mPdfViewCtrl.getDoc().checkPassword(password.getBytes()) == PDFDoc.e_PwdOwner;
		} catch (PDFException e) {
			e.printStackTrace();
		}
		return false;
	}


	public boolean isOwner() {
		return mIsOwner = mPdfViewCtrl.isOwner();
	}

	public void showCheckOwnerPasswordDialog(final int operatorType) {
		if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
		Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
		if (context == null) return;
		mCheckOwnerPWD = new UITextEditDialog(context);
		mEditText = mCheckOwnerPWD.getInputEditText();
		TextView tv = mCheckOwnerPWD.getPromptTextView();
		mCheckOwnerPWD.setTitle(mContext.getApplicationContext().getString(R.string.rv_doc_encrpty_standard_ownerpassword_title));
		tv.setText(mContext.getApplicationContext().getString(R.string.rv_doc_encrypt_standard_ownerpassword_content));

		final Button button_ok = mCheckOwnerPWD.getOKButton();
		final Button button_cancel = mCheckOwnerPWD.getCancelButton();

		mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

		mEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				if (mEditText.getText().length() != 0 && mEditText.getText().length() <= 32) {
					button_ok.setEnabled(true);
				} else {
					button_ok.setEnabled(false);
				}
			}
		});

		mEditText.setKeyListener(new NumberKeyListener() {

			@Override
			public int getInputType() {
				return InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
			}

			@Override
			protected char[] getAcceptedChars() {
				return PasswordConstants.mAcceptChars;
			}
		});

		button_ok.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mIsDocOpenAuthEvent) {
					mIsOwner = checkOwnerPassword(mEditText.getText().toString());
					if (mIsOwner) {
						mCheckOwnerPWD.dismiss();
						if (operatorType == PasswordConstants.OPERATOR_TYPE_REMOVE)
							removePassword();
					} else {
						mEditText.setText("");
						Toast.makeText(mContext,
								mContext.getApplicationContext().getString(R.string.rv_doc_encrpty_standard_ownerpassword_failed), Toast.LENGTH_SHORT).show();
					}
				}
			}
		});

		button_cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mCheckOwnerPWD.dismiss();
			}
		});

		mCheckOwnerPWD.show();
		AppUtil.showSoftInput(mEditText);

	}

	public void passwordManager(final int operatorType) {
		int type = 0;
		try {
			type = mPdfViewCtrl.getDoc().getEncryptionType();
		} catch (PDFException e) {
			e.printStackTrace();
		}
		if ((type == PDFDoc.e_EncryptPassword && !mIsOwner) || !mIsDocOpenAuthEvent) {
			showCheckOwnerPasswordDialog(operatorType);

		} else {
			switch (operatorType) {
				case PasswordConstants.OPERATOR_TYPE_CREATE:
					showSettingDialog();
					break;
				case PasswordConstants.OPERATOR_TYPE_REMOVE:
					removePassword();
					break;
				default:
					break;
			}
		}
	}

	public void showSettingDialog() {
		mSettingDialog = new PasswordSettingFragment(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity());
		mSettingDialog.init(this, mPdfViewCtrl);
		mSettingDialog.showDialog();
	}

	public void addPassword(final String userPassword, final String ownerPassword, boolean isAddAnnot, boolean isCopy, boolean isManagePage, boolean isPrint, boolean isFillForm, boolean isModifyDoc, boolean isTextAccess, final String newFilePath) {
		showDialog();
		int userPermission = 0xFFFFFFFC;
		if (isAddAnnot) {
			userPermission = userPermission | PDFDoc.e_PermAnnotForm;
		} else {
			userPermission = userPermission & (~PDFDoc.e_PermAnnotForm);
		}

		if (isCopy) {
			userPermission = userPermission | PDFDoc.e_PermExtract;
		} else {
			userPermission = userPermission & (~PDFDoc.e_PermExtract);
		}

		if (isManagePage) {
			userPermission = userPermission | PDFDoc.e_PermAssemble;
		} else {
			userPermission = userPermission & (~PDFDoc.e_PermAssemble);
		}

		if (isPrint) {
			userPermission = userPermission | PDFDoc.e_PermPrint | PDFDoc.e_PermPrintHigh;
		} else {
			userPermission = userPermission & (~(PDFDoc.e_PermPrint | PDFDoc.e_PermPrintHigh));
		}

		if (isFillForm) {
			userPermission = userPermission | PDFDoc.e_PermFillForm;
		} else {
			userPermission = userPermission & (~PDFDoc.e_PermFillForm);
		}

		if (isModifyDoc) {
			userPermission = userPermission | PDFDoc.e_PermModify;
		} else {
			userPermission = userPermission & (~PDFDoc.e_PermModify);
		}

		if (isTextAccess) {
			userPermission = userPermission | PDFDoc.e_PermExtractAccess;
		} else {
			userPermission = userPermission & (~PDFDoc.e_PermExtractAccess);
		}


		try {
			StdSecurityHandler securityHandler = new StdSecurityHandler();
			StdEncryptData encryptData = new StdEncryptData();
			encryptData.set(true, userPermission, SecurityHandler.e_CipherAES, 16);
			byte[] up = userPassword == null ? null : userPassword.getBytes();
			byte[] op = ownerPassword == null ? null : ownerPassword.getBytes();
			securityHandler.initialize(encryptData, up, op);
			if (mPdfViewCtrl.getDoc().isEncrypted()) {
				mPdfViewCtrl.getDoc().removeSecurity();
			}
			mPdfViewCtrl.getDoc().setSecurityHandler(securityHandler);
			String path = mFilePath + "fsencrypt.pdf";
			reopenDoc(path, userPassword);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void removePassword() {
		if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
		Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
		if (context == null) return;
		final UITextEditDialog removePassworDialog = new UITextEditDialog(context);
		removePassworDialog.setTitle(mContext.getApplicationContext().getString(R.string.rv_doc_encrpty_standard_remove));
		removePassworDialog.getPromptTextView().setText(mContext.getApplicationContext().getString(R.string.rv_doc_encrpty_standard_removepassword_confirm));
		removePassworDialog.getInputEditText().setVisibility(View.GONE);
		removePassworDialog.getOKButton().setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDialog();
				removePassworDialog.dismiss();
				try {
					mPdfViewCtrl.getDoc().removeSecurity();
					mIsOwner = true;
				} catch (PDFException e) {
					e.printStackTrace();
				}
				String path = mFilePath + "fsencrypt.pdf";
				reopenDoc(path, null);
			}
		});

		removePassworDialog.getCancelButton().setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				removePassworDialog.dismiss();
			}
		});

		removePassworDialog.show();

	}

	private static int getDialogTheme() {
		int theme;
		if (Build.VERSION.SDK_INT >= 21) {
			theme = android.R.style.Theme_Holo_Light_Dialog_NoActionBar;
		} else if (Build.VERSION.SDK_INT >= 14) {
			theme = android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar;
		} else if (Build.VERSION.SDK_INT >= 11) {
			theme = android.R.style.Theme_Holo_Light_Dialog_NoActionBar;
		} else {
			theme = R.style.rv_dialog_style;
		}
		return theme;
	}

	public void showDialog() {
		if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
		final Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
		if (context == null) return;
		((Activity)context).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (null == mDialog) {
					mDialog = new PasswordDialog(context, getDialogTheme());
					mDialog.getWindow().setBackgroundDrawableResource(R.color.ux_color_translucent);
				}
				mDialog.show();
			}
		});
	}

	public void hideDialog() {
		if (null != mDialog && mDialog.isShowing()) {
			mDialog.dismiss();
			mDialog = null;
		}
		if (null != mSettingDialog && mSettingDialog.isShowing()) {
			mSettingDialog.dismiss();
			mSettingDialog = null;
		}
	}

	public boolean getIsOwner() {
		return mIsOwner;
	}

	public void setIsOwner(boolean isOwner) {
		mIsOwner = isOwner;
	}

	public boolean isDocOpenAuthEvent() {
		return mIsDocOpenAuthEvent;
	}

	public void setDocOpenAuthEvent(boolean mIsDocOpenAuthEvent) {
		this.mIsDocOpenAuthEvent = mIsDocOpenAuthEvent;
	}

	private static boolean copyFile(String oriPath, String desPath) {
		if (oriPath == null || desPath == null) return false;
		OutputStream os = null;
		try {
			os = new FileOutputStream(desPath);
			byte[] buffer = new byte[1 << 13];
			InputStream is = new FileInputStream(oriPath);
			int len = is.read(buffer);
			while (len != -1) {
				os.write(buffer, 0, len);
				len = is.read(buffer);
			}
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (os != null) {
					os.flush();
					os.close();
				}
			} catch (IOException e) {
				return false;
			}
		}
		return true;
	}

	private void reopenDoc(final String path, final String password) {
		Task.CallBack callBack = new Task.CallBack() {
			@Override
			public void result(Task task) {
				if (!bSuccess) return;
				byte[] up = password == null ? null : password.getBytes();
				mPdfViewCtrl.openDoc(mFilePath, up);
				if (password == null) {
					mIsDocOpenAuthEvent = true;
				} else {
					((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().clearUndoRedo();
				}
				mIsOwner = true;
				hideDialog();
			}
		};

		Task task = new Task(callBack) {
			@Override
			protected void execute() {
				try {
					Progressive progressive = mPdfViewCtrl.getDoc().startSaveAs(path, PDFDoc.e_SaveFlagNormal, null);
					int state = Progressive.e_ToBeContinued;
					while (state == Progressive.e_ToBeContinued){
						state = progressive.resume();
					}
					progressive.delete();

					bSuccess = (state == Progressive.e_Finished);
					if (!bSuccess) return;
					File oriFile = new File(mFilePath);
					if (oriFile.exists()) {
						oriFile.delete();
					}

					File newFile = new File(path);
					if (!newFile.exists()) return;
					bSuccess = copyFile(path, mFilePath);
					if (!bSuccess) return;
					newFile.delete();
					bSuccess = true;
				} catch (Exception e) {
					bSuccess = false;
				}
			}
		};
		mPdfViewCtrl.addTask(task);
	}

}

