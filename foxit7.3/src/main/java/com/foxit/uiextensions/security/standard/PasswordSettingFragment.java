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
package com.foxit.uiextensions.security.standard;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UIEncryptionDialogFragment;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UISaveAsDialog;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.UIToast;

import java.util.ArrayList;


public class PasswordSettingFragment extends UIMatchDialog {

	public static final String TAG = "PasswordSettingTag";
	private static final String DIALOGTAG = "FOXIT_SAVEDOC_ENCRYPT_STANDARD";

    public static final int SWITCH_TAG_USER = 1;
    public static final int SWITCH_TAG_OWNER = 2;

    public static final int SWITCH_TAG_PRINT = 3;
    public static final int SWITCH_TAG_FILLFORM = 4;
    public static final int SWITCH_TAG_ANNOT = 5;
    public static final int SWITCH_TAG_PAGE = 6;
    public static final int SWITCH_TAG_MODIFYDOC = 7;
    public static final int SWITCH_TAG_TEXTACCESS = 8;
    public static final int SWITCH_TAG_COPY = 9;

    public static final int EYES_TAG_USER = 10;
    public static final int EYES_TAG_OWNER = 11;


	private PasswordStandardSupport mSupport;

	private LinearLayout mLinearLayout;
	private Boolean mSettingUserPassword = false;
	private Boolean mSettingOwnerPassword = false;

	private Boolean mIsPrint = true;
	private Boolean mFillForm = true;
	private Boolean mIsAddAnnot = true;
	private Boolean mIsManagePage = true;
	private Boolean mIsModifyDoc = true;
	private Boolean mTextAccess = true;
	private Boolean mIsCopy = true;

	private EditText mUserEditText = null;
	private EditText mOwnerEditText = null;
	private ImageView mUserEye;
	private ImageView mOwnerEye;
	private ArrayList<ImageView> mImageList = null;
	private ArrayList<View> mViewList = null;

	private String userpassword = null;
	private String ownerpassword = null;

	private PDFViewCtrl mPdfViewCtrl;

	public PasswordSettingFragment(Context context) {
		super(context);
	}


	public void init(PasswordStandardSupport support, PDFViewCtrl pdfViewCtrl) {
		mPdfViewCtrl = pdfViewCtrl;
		mSupport = support;
		createView();

	}


	private View createView() {
		View view = View.inflate(mContext, R.layout.rv_password_setting, null);
		view.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});

		mSettingUserPassword = false;
		mSettingOwnerPassword = false;
		mIsAddAnnot = true;
		mIsCopy = true;
		mIsManagePage = true;
		mIsPrint = true;

		mLinearLayout = (LinearLayout) view.findViewById(R.id.settinglist);

		mImageList = new ArrayList<ImageView>();
		mViewList = new ArrayList<View>();
		mViewList.add(getSwitchItem(mContext.getString(R.string.rv_doc_encrpty_standard_openfile), SWITCH_TAG_USER));
		mViewList.add(getPasswordInputItem(mContext.getString(R.string.rv_doc_encrpty_standard_password), EYES_TAG_USER));
		mViewList.add(getSwitchItem(mContext.getString(R.string.rv_doc_encrpty_standard_owner_permission), SWITCH_TAG_OWNER));
		mViewList.add(getSwitchItem(mContext.getString(R.string.rv_doc_info_permission_print), SWITCH_TAG_PRINT));
		mViewList.add(getSwitchItem(mContext.getString(R.string.rv_doc_info_permission_fillform), SWITCH_TAG_FILLFORM));
		mViewList.add(getSwitchItem(mContext.getString(R.string.rv_doc_info_permission_annotform), SWITCH_TAG_ANNOT));
		mViewList.add(getSwitchItem(mContext.getString(R.string.rv_doc_info_permission_assemble), SWITCH_TAG_PAGE));
		mViewList.add(getSwitchItem(mContext.getString(R.string.rv_doc_info_permission_modify), SWITCH_TAG_MODIFYDOC));
		mViewList.add(getSwitchItem(mContext.getString(R.string.rv_doc_info_permission_extractaccess), SWITCH_TAG_TEXTACCESS));
		mViewList.add(getSwitchItem(mContext.getString(R.string.rv_doc_info_permission_extract), SWITCH_TAG_COPY));
		mViewList.add(getPasswordInputItem(mContext.getString(R.string.rv_doc_encrpty_standard_password), EYES_TAG_OWNER));
		View tip = getOnlyTextItem(mContext.getString(R.string.rv_doc_encrpty_standard_bottom_tip1) + "\r\n\r\n" + mContext.getString(R.string.rv_doc_encrpty_standard_bottom_tip2));
		TextView tv = (TextView) tip.findViewById(R.id.rv_password_item_textview);
		tv.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_body2_gray));
		ImageView iv = (ImageView) tip.findViewById(R.id.rv_password_item_divide);
		iv.setVisibility(View.GONE);
		mViewList.add(tip);
		refreshViewList(mSettingUserPassword, mSettingOwnerPassword);
		setContentView(view);
		setTitle(mContext.getString(R.string.rv_doc_info_security_standard));
		setButton(MatchDialog.DIALOG_OK | MatchDialog.DIALOG_CANCEL);
		setButtonEnable(false, MatchDialog.DIALOG_OK);
		setBackButtonVisible(View.GONE);
		setListener(new DialogListener() {
			@Override
			public void onResult(long btType) {
				if (btType == UIMatchDialog.DIALOG_OK) {
					if (mSettingUserPassword) userpassword = mUserEditText.getText().toString();
					else userpassword = "";
					if (mSettingOwnerPassword) ownerpassword = mOwnerEditText.getText().toString();
					else ownerpassword = "";

					if (userpassword.equals(ownerpassword)) {
						UIToast.getInstance(mContext).show(mContext.getString(R.string.rv_doc_encrpty_standard_same_password));
						return ;
					}
					dismiss();

					if (mPdfViewCtrl == null) {
						return;
					}
					boolean bModified = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isDocModified();

					if((bModified) || (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canSaveAsFile() && !((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyFile())) {//if document is modified, show dialog to confirm save document first, then setting password
						Activity activity = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
						if (activity == null) {
							return;
						}

						if (!(activity instanceof FragmentActivity)) {
							UIToast.getInstance(mContext).show(mContext.getString(R.string.the_attached_activity_is_not_fragmentActivity));
							return;
						}
						FragmentManager fm = ((FragmentActivity) activity).getSupportFragmentManager();
						UIEncryptionDialogFragment newFragment = (UIEncryptionDialogFragment)fm.findFragmentByTag(DIALOGTAG);
						if (newFragment == null) {
							newFragment = UIEncryptionDialogFragment.newInstance(true);
						}

						AppDialogManager.getInstance().showAllowManager(newFragment, fm, DIALOGTAG, new AppDialogManager.CancelListener() {

							@Override
							public void cancel() {
							}
						});
						newFragment.setEncryptionDialogEventListener(new UIEncryptionDialogFragment.UIEncryptionDialogEventListener() {

							@Override
							public void onConfirmed(boolean encrypt) {
								if (mSettingUserPassword) userpassword = mUserEditText.getText().toString();
								else userpassword = "";
								if (mSettingOwnerPassword) ownerpassword = mOwnerEditText.getText().toString();
								else ownerpassword = "";
								if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canSaveAsFile() && !((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyFile()) {
									if (mPdfViewCtrl.getUIExtensionsManager() == null) {
										return;
									}
									Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
									if (context == null) {
										return;
									}
									UISaveAsDialog dialog = new UISaveAsDialog(context, mSupport.getFilePath(), "pdf", new UISaveAsDialog.ISaveAsOnOKClickCallBack() {
										@Override
										public void onOkClick(String newFilePath) {
											mSupport.addPassword(userpassword, ownerpassword, mIsAddAnnot, mIsCopy, mIsManagePage, mIsPrint, mFillForm, mIsModifyDoc, mTextAccess, newFilePath);
										}

										@Override
										public void onCancelClick() {

										}
									});
									dialog.showDialog();
								} else {
									mSupport.addPassword(userpassword, ownerpassword, mIsAddAnnot, mIsCopy, mIsManagePage, mIsPrint, mFillForm, mIsModifyDoc, mTextAccess, null);
								}
							}

							@Override
							public void onCancel() {
							}
						});
					}
					else {
						mSupport.addPassword(userpassword, ownerpassword, mIsAddAnnot, mIsCopy, mIsManagePage, mIsPrint, mFillForm, mIsModifyDoc, mTextAccess, null);
					}
				}
				else if (btType == MatchDialog.DIALOG_CANCEL) {
					dismiss();
				}
			}

			@Override
			public void onBackClick() {

			}
		});
		return view;
	}

	private View getOnlyTextItem(String tip) {
		tip = "\r\n" + tip;
		View item = View.inflate(mContext, R.layout.rv_password_setting_item, null);
		TextView tv = (TextView) item.findViewById(R.id.rv_password_item_textview);
		tv.setTextSize(10);
		tv.setText(tip);
		ImageView iv = (ImageView) item.findViewById(R.id.rv_password_item_imagebutton);
		iv.setVisibility(View.GONE);
		EditText et = (EditText) item.findViewById(R.id.rv_password_item_edittext);
		et.setVisibility(View.GONE);
		return item;
	}

	private View getSwitchItem(String tip, int tag) {
		RelativeLayout item = (RelativeLayout) View.inflate(mContext, R.layout.rv_password_setting_item, null);
		if (AppDisplay.getInstance(mContext).isPad()) {
			item.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_pad)));
		} else {
			item.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_phone)));
		}
		ImageView iv = (ImageView) item.findViewById(R.id.rv_password_item_imagebutton);
		iv.setTag(tag);
		iv.setImageResource(R.drawable.setting_on);
		iv.setOnClickListener(mOnClickListener);
		mImageList.add(iv);
		TextView tv = (TextView) item.findViewById(R.id.rv_password_item_textview);
		tv.setText(tip);
		tv.setPadding(0, 0, AppDisplay.getInstance(mContext).dp2px(60), 0);
		EditText et = (EditText) item.findViewById(R.id.rv_password_item_edittext);
		et.setVisibility(View.GONE);
		refreshSwitchView(tag, iv);
		return item;
	}

	private View getPasswordInputItem(String tip, final int tag) {
		RelativeLayout item = (RelativeLayout) View.inflate(mContext, R.layout.rv_password_setting_item, null);
		if (AppDisplay.getInstance(mContext).isPad()) {
			item.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_pad)));
		} else {
			item.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_phone)));
		}
		ImageView iv = (ImageView) item.findViewById(R.id.rv_password_item_imagebutton);
		iv.setImageResource(R.drawable.rv_password_check_eye_normal);
		iv.setTag(tag);
		iv.setOnTouchListener(mOnTouchListener);
		iv.setVisibility(View.INVISIBLE);
		if(tag == EYES_TAG_USER) mUserEye = iv;
		if(tag == EYES_TAG_OWNER) mOwnerEye = iv;
		TextView tv = (TextView) item.findViewById(R.id.rv_password_item_textview);
		tv.setText(tip);
		final EditText et = (EditText) item.findViewById(R.id.rv_password_item_edittext);
		et.setHeight(AppDisplay.getInstance(mContext).dp2px(30));
		et.setKeyListener(new NumberKeyListener() {

			@Override
			public int getInputType() {
				return InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
			}

			@Override
			protected char[] getAcceptedChars() {
				return PasswordConstants.mAcceptChars;
			}
		});
		et.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				if (et.getText().length() != 0 && et.getText().length() <= 32) {
					setButtonEnable(true, MatchDialog.DIALOG_OK);
					if(tag == EYES_TAG_USER) mUserEye.setVisibility(View.VISIBLE);
					if(tag == EYES_TAG_OWNER) mOwnerEye.setVisibility(View.VISIBLE);
				} else {
					setButtonEnable(false, MatchDialog.DIALOG_OK);
					if(tag == EYES_TAG_USER) mUserEye.setVisibility(View.INVISIBLE);
					if(tag == EYES_TAG_OWNER) mOwnerEye.setVisibility(View.INVISIBLE);
				}
				refreshButton(tag);
			}
		});
		et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
		et.setHint(mContext.getString(R.string.rv_doc_encrpty_standard_must_input));
		if(tag == EYES_TAG_USER) mUserEditText = et;
		if(tag == EYES_TAG_OWNER) mOwnerEditText = et;
		return item;
	}

	//i = 0 to 9 means row 1 to 10 of password setting UI
	private void refreshViewList(boolean user, boolean owner) {
		mLinearLayout.removeAllViews();
		if(user && owner) {
			for (int i = 0; i < mViewList.size(); i++) {
				mLinearLayout.addView(mViewList.get(i));
			}
		}
		else if (user && !owner) {
			for (int i = 0; i < mViewList.size(); i++) {
				if((i >= 0 && i <= 2) || i == 11)
					mLinearLayout.addView(mViewList.get(i));
			}
		}
		else if (!user && owner) {
			for (int i = 0; i < mViewList.size(); i++) {
				if(i == 0  || (i >= 2 && i <= 11))
					mLinearLayout.addView(mViewList.get(i));
			}
		}
		else if(!user && !owner) {
			for (int i = 0; i < mViewList.size(); i++) {
				if(i == 0 || i == 2 || i == 11)
					mLinearLayout.addView(mViewList.get(i));
			}
		}
	}

	private View.OnClickListener mOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			int tag = (Integer) v.getTag();

			if (tag == SWITCH_TAG_USER) {
				mSettingUserPassword = !mSettingUserPassword;
			}
			else if (tag == SWITCH_TAG_OWNER) {
				mSettingOwnerPassword = !mSettingOwnerPassword;
			}
			else if (tag == SWITCH_TAG_PRINT) {
				mIsPrint = !mIsPrint;
			}
			else if (tag == SWITCH_TAG_FILLFORM) {
				mFillForm = !mFillForm;
				if (mFillForm == false) {
					mIsAddAnnot = false;
					mIsModifyDoc = false;
				}
			}
			else if (tag == SWITCH_TAG_ANNOT) {
				mIsAddAnnot = !mIsAddAnnot;
				if (mIsAddAnnot) mFillForm = true;
			}
			else if (tag == SWITCH_TAG_PAGE) {
				mIsManagePage = !mIsManagePage;
				if (mIsManagePage == false) mIsModifyDoc = false;
			}
			else if (tag == SWITCH_TAG_MODIFYDOC) {
				mIsModifyDoc = !mIsModifyDoc;
				if (mIsModifyDoc) {
					mFillForm = true;
					mIsManagePage = true;
				}
			}
			else if (tag == SWITCH_TAG_COPY) {
				mIsCopy = !mIsCopy;
				if (mIsCopy) mTextAccess = true;
			}
			else if (tag == SWITCH_TAG_TEXTACCESS) {
				mTextAccess = !mTextAccess;
				if (mTextAccess == false) mIsCopy = false;
			}

			refreshButton(EYES_TAG_USER);
			refreshButton(EYES_TAG_OWNER);
			refreshAllSwitchView();
			refreshViewList(mSettingUserPassword, mSettingOwnerPassword);
		}
	};

	private OnTouchListener mOnTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			EditText editText = null;
			int tag = (Integer) v.getTag();
			if(tag == EYES_TAG_USER) editText = mUserEditText;
			if(tag == EYES_TAG_OWNER) editText = mOwnerEditText;
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				((ImageView)v).setImageResource(R.drawable.rv_password_check_eye_pressed);
				editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
				editText.setHeight(AppDisplay.getInstance(mContext).dp2px(30));
				CharSequence text = editText.getText();
				if (text instanceof Spannable) {
					Spannable spanText = (Spannable)text;
					Selection.setSelection(spanText, text.length());
				}
				break;
			}
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL: {
				((ImageView)v).setImageResource(R.drawable.rv_password_check_eye_normal);
				editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
				editText.setKeyListener(new NumberKeyListener() {

					@Override
					public int getInputType() {
						return InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
					}

					@Override
					protected char[] getAcceptedChars() {
						return PasswordConstants.mAcceptChars;
					}
				});

				CharSequence text = editText.getText();
				if (text instanceof Spannable) {
					Spannable spanText = (Spannable)text;
					Selection.setSelection(spanText, text.length());
				}
				break;
			}

			default:
				break;
			}
			return true;

		}
	};

	private void refreshSwitchView(int tag, ImageView iv) {
		if (tag == SWITCH_TAG_USER) {
			if (mSettingUserPassword) iv.setImageResource(R.drawable.setting_on);
			else iv.setImageResource(R.drawable.setting_off);
		}
		else if (tag == SWITCH_TAG_OWNER) {
			if (mSettingOwnerPassword) iv.setImageResource(R.drawable.setting_on);
			else iv.setImageResource(R.drawable.setting_off);
		}
		else if (tag == SWITCH_TAG_ANNOT) {
			if (mIsAddAnnot) iv.setImageResource(R.drawable.setting_on);
			else iv.setImageResource(R.drawable.setting_off);
		}
		else if (tag == SWITCH_TAG_COPY) {
			if (mIsCopy) iv.setImageResource(R.drawable.setting_on);
			else iv.setImageResource(R.drawable.setting_off);
		}
		else if (tag == SWITCH_TAG_PAGE) {
			if (mIsManagePage) iv.setImageResource(R.drawable.setting_on);
			else iv.setImageResource(R.drawable.setting_off);
		}
		else if (tag == SWITCH_TAG_PRINT) {
			if (mIsPrint) iv.setImageResource(R.drawable.setting_on);
			else iv.setImageResource(R.drawable.setting_off);
		}
		else if (tag == SWITCH_TAG_FILLFORM) {
			if (mFillForm) iv.setImageResource(R.drawable.setting_on);
			else iv.setImageResource(R.drawable.setting_off);
		}
		else if (tag == SWITCH_TAG_MODIFYDOC) {
			if (mIsModifyDoc) iv.setImageResource(R.drawable.setting_on);
			else iv.setImageResource(R.drawable.setting_off);
		}
		else if (tag == SWITCH_TAG_TEXTACCESS) {
			if (mTextAccess) iv.setImageResource(R.drawable.setting_on);
			else iv.setImageResource(R.drawable.setting_off);
		}
	}

	private void refreshAllSwitchView() {
		if (mSettingUserPassword) ((ImageView)mImageList.get(0)).setImageResource(R.drawable.setting_on);
		else ((ImageView)mImageList.get(0)).setImageResource(R.drawable.setting_off);

		if (mSettingOwnerPassword) ((ImageView)mImageList.get(1)).setImageResource(R.drawable.setting_on);
		else ((ImageView)mImageList.get(1)).setImageResource(R.drawable.setting_off);

		if (mIsPrint) ((ImageView)mImageList.get(2)).setImageResource(R.drawable.setting_on);
		else ((ImageView)mImageList.get(2)).setImageResource(R.drawable.setting_off);

		if (mFillForm) ((ImageView)mImageList.get(3)).setImageResource(R.drawable.setting_on);
		else ((ImageView)mImageList.get(3)).setImageResource(R.drawable.setting_off);

		if (mIsAddAnnot) ((ImageView)mImageList.get(4)).setImageResource(R.drawable.setting_on);
		else ((ImageView)mImageList.get(4)).setImageResource(R.drawable.setting_off);

		if (mIsManagePage) ((ImageView)mImageList.get(5)).setImageResource(R.drawable.setting_on);
		else ((ImageView)mImageList.get(5)).setImageResource(R.drawable.setting_off);

		if (mIsModifyDoc) ((ImageView)mImageList.get(6)).setImageResource(R.drawable.setting_on);
		else ((ImageView)mImageList.get(6)).setImageResource(R.drawable.setting_off);

		if (mTextAccess) ((ImageView)mImageList.get(7)).setImageResource(R.drawable.setting_on);
		else ((ImageView)mImageList.get(7)).setImageResource(R.drawable.setting_off);

		if (mIsCopy) ((ImageView)mImageList.get(8)).setImageResource(R.drawable.setting_on);
		else ((ImageView)mImageList.get(8)).setImageResource(R.drawable.setting_off);
	}

	private void refreshButton(int tag) {
		if (tag == EYES_TAG_USER) {
			if(mSettingUserPassword == true) {
				if(mUserEditText.getText().toString().length() == 0) {
					setButtonEnable(false, MatchDialog.DIALOG_OK);
				}
				else {
					setButtonEnable(true, MatchDialog.DIALOG_OK);
				}
			}
		}
		if (tag == EYES_TAG_OWNER) {
			if(mSettingOwnerPassword == true) {
				if(mOwnerEditText.getText().toString().length() == 0) {
					setButtonEnable(false, MatchDialog.DIALOG_OK);
				}
				else {
					setButtonEnable(true, MatchDialog.DIALOG_OK);
				}
			}
		}
		if(mSettingOwnerPassword == false && mSettingUserPassword == false) {
			setButtonEnable(false, MatchDialog.DIALOG_OK);
		}
	}


}

