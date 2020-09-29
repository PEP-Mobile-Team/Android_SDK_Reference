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
package com.foxit.uiextensions.controls.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.appcompat.app.AppCompatDialogFragment;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppUtil;


/**
 * before encrypt or decrypt dialog
 * display:
 		FragmentTransaction fragmentTransaction = supportedFragmentManager.beginTransaction();
        Fragment previous = supportedFragmentManager.findFragmentByTag(TAG);
        if (previous != null) {
            fragmentTransaction.remove(previous);
        }
        DialogFragment newFragment = UIEncryptionDialogFragment.newInstance(encrypt);
        fragmentTransaction.add(newFragment, TAG);
        fragmentTransaction.commitAllowingStateLoss();
 */
public class UIEncryptionDialogFragment extends AppCompatDialogFragment {

	public interface UIEncryptionDialogEventListener {
		public void onConfirmed(boolean encrypt);
		public void onCancel();
	}

	private static final String BUNDLE_KEY_ENCRYPT = "BUNDLE_KEY_ENCRYPT";
	private boolean mEncrypt;
	private UIEncryptionDialogEventListener mEncryptionDialogEventListener;
	
	public void setEncryptionDialogEventListener(UIEncryptionDialogEventListener listener) {
		mEncryptionDialogEventListener = listener;
	}
	
	public static UIEncryptionDialogFragment newInstance(boolean encrypt) {
		UIEncryptionDialogFragment fragment = new UIEncryptionDialogFragment();
		Bundle args = new Bundle();
		args.putBoolean(BUNDLE_KEY_ENCRYPT, encrypt);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		if (mEncryptionDialogEventListener != null) {
			mEncryptionDialogEventListener.onCancel();
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstance(getArguments());
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		if (savedInstanceState != null) {
			restoreInstance(savedInstanceState);
		}
		final UITextEditDialog dialog = new UITextEditDialog(getActivity());
		dialog.getInputEditText().setVisibility(View.GONE);
		if (mEncrypt) {
			dialog.getPromptTextView().setText(getActivity().getApplicationContext().getString(R.string.rv_encrypt_dialog_description));
			dialog.setTitle(getActivity().getApplicationContext().getString(R.string.rv_encrypt_dialog_title));
		} else {
			dialog.getPromptTextView().setText(getActivity().getApplicationContext().getString(R.string.rv_decrypt_dialog_description));
			dialog.setTitle(getActivity().getApplicationContext().getString(R.string.rv_decrypt_dialog_title));
		}
		dialog.getOKButton().setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (AppUtil.isFastDoubleClick()) return;
				dismissAllowingStateLoss();
				if (mEncryptionDialogEventListener != null) {
					mEncryptionDialogEventListener.onConfirmed(mEncrypt);
				}
			}
		});
		dialog.getCancelButton().setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismissAllowingStateLoss();
				if (mEncryptionDialogEventListener != null) {
					mEncryptionDialogEventListener.onCancel();
				}
			}
		});
		return dialog.getDialog();
	}
	
	@Override
	public void onSaveInstanceState(Bundle arg0) {
		super.onSaveInstanceState(arg0);
		arg0.putBoolean(BUNDLE_KEY_ENCRYPT, mEncrypt);
	}

	private void restoreInstance(Bundle savedInstance) {
		mEncrypt = savedInstance.getBoolean(BUNDLE_KEY_ENCRYPT);
	}
}
