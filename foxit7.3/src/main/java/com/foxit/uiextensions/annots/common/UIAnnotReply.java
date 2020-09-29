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
package com.foxit.uiextensions.annots.common;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.MessageQueue.IdleHandler;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.note.NoteAnnotContent;
import com.foxit.uiextensions.annots.redaction.UIAnnotRedaction;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.UIToast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.view.MotionEventCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

public class UIAnnotReply {
    public static final int TITLE_COMMENT_ID = R.string.fx_string_reply;
    public static final int TITLE_EDIT_ID = R.string.fx_string_comment;

    public interface ReplyCallback {
        void result(String content);

        String getContent();
    }

    public static void replyToAnnot(final PDFViewCtrl pdfViewCtrl, final ViewGroup parent, final Annot annot) {
        replyToAnnot(pdfViewCtrl, parent, annot, false);
    }

    public static void replyToAnnot(final PDFViewCtrl pdfViewCtrl, final ViewGroup parent, final Annot annot, final boolean isReplyGroup) {
        if (annot == null || pdfViewCtrl.getUIExtensionsManager() == null) {
            return;
        }
        Activity activity = ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (activity == null) {
            return;
        }
        if (!(activity instanceof FragmentActivity)) {
            UIToast.getInstance(activity.getApplicationContext()).show(activity.getApplicationContext().getString(R.string.the_attached_activity_is_not_fragmentActivity));
            return;
        }
        boolean editable = !AppAnnotUtil.isReadOnly(annot);//true;

        FragmentActivity act = (FragmentActivity) activity;
        ReplyDialog fragment = (ReplyDialog) act.getSupportFragmentManager().findFragmentByTag("ReplyDialog");
        if (fragment == null) {
            fragment = new ReplyDialog();
        }

        fragment.init(pdfViewCtrl, parent, editable, TITLE_COMMENT_ID, new ReplyCallback() {

            @Override
            public void result(String content) {
                PDFPage page = null;
                try {
                    page = annot.getPage();
                    addReplyAnnot(pdfViewCtrl,
                            annot,
                            page,
                            AppDmUtil.randomUUID(null),
                            content,
                            new Event.Callback() {

                                @Override
                                public void result(Event event, boolean success) {
                                    showComments(pdfViewCtrl, parent, annot, isReplyGroup);
                                }
                            });
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public String getContent() {
                return null;
            }
        });

        AppDialogManager.getInstance().showAllowManager(fragment, act.getSupportFragmentManager(), "ReplyDialog", null);
    }

    public static void replyToAnnot(final PDFViewCtrl pdfViewCtrl, final ViewGroup parent, boolean editable, int titleId, ReplyCallback callback) {
        if (callback == null || pdfViewCtrl.getUIExtensionsManager() == null) {
            return;
        }

        Activity activity = ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (activity == null) {
            return;
        }
        if (!(activity instanceof FragmentActivity)) {
            UIToast.getInstance(activity.getApplicationContext()).show(activity.getApplicationContext().getString(R.string.the_attached_activity_is_not_fragmentActivity));
            return;
        }
        FragmentActivity act = (FragmentActivity) activity;
        ReplyDialog fragment = (ReplyDialog) act.getSupportFragmentManager().findFragmentByTag("ReplyDialog");
        if (fragment == null) {
            fragment = new ReplyDialog();
        }
        fragment.init(pdfViewCtrl, parent, editable, titleId, callback);
        AppDialogManager.getInstance().showAllowManager(fragment, act.getSupportFragmentManager(), "ReplyDialog", null);
    }

    public static void showComments(final PDFViewCtrl pdfViewCtrl, ViewGroup parent, Annot annot) {
        showComments(pdfViewCtrl, parent, annot, false);
    }

    public static void showComments(final PDFViewCtrl pdfViewCtrl, ViewGroup parent, Annot annot, boolean isReplyGroup) {
        if (annot == null || pdfViewCtrl.getUIExtensionsManager() == null) {
            return;
        }
        Activity activity = ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (activity == null) {
            return;
        }
        if (!(activity instanceof FragmentActivity)) {
            UIToast.getInstance(activity.getApplicationContext()).show(activity.getApplicationContext().getString(R.string.the_attached_activity_is_not_fragmentActivity));
            return;
        }
        FragmentActivity act = (FragmentActivity) activity;
        CommentsFragment fragment = (CommentsFragment) act.getSupportFragmentManager().findFragmentByTag("CommentsFragment");
        if (fragment == null) {
            fragment = new CommentsFragment();
        }
        fragment.init(pdfViewCtrl, parent, annot, isReplyGroup);
        AppDialogManager.getInstance().showAllowManager(fragment, act.getSupportFragmentManager(), "CommentsFragment", null);
    }

    public static void addReplyAnnot(final PDFViewCtrl pdfViewCtrl,
                                     final Annot annot,
                                     final PDFPage page,
                                     final String nm,
                                     final String content,
                                     final Event.Callback callback) {
        try {
            String parentNM = AppAnnotUtil.getAnnotUniqueID(annot);
            ((UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addAnnot(page, new ReplyContent(page.getIndex(), nm, content, parentNM), true, callback);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public static class CommentContent implements AnnotContent {

        String content;
        Annot annot;

        public CommentContent(Annot annot, String content) {
            this.annot = annot;
            this.content = content;
        }

        @Override
        public int getPageIndex() {
            try {
                return annot.getPage().getIndex();
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        public int getType() {
            try {
                return annot.getType();
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return Annot.e_UnknownType;
        }

        @Override
        public String getNM() {
            return AppAnnotUtil.getAnnotUniqueID(annot);
        }

        @Override
        public RectF getBBox() {
            try {
                return AppUtil.toRectF(annot.getRect());
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public int getColor() {
            try {
                return annot.getBorderColor();
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        public int getOpacity() {
            try {
                return (int) (((Markup) annot).getOpacity() * 255f + 0.5f);
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        public float getLineWidth() {
            try {
                if (annot.getBorderInfo() != null) {
                    return annot.getBorderInfo().getWidth();
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        public String getSubject() {
            try {
                return ((Markup) annot).getSubject();
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public DateTime getModifiedDate() {
            return AppDmUtil.currentDateToDocumentDate();
        }

        @Override
        public String getContents() {
            return content;
        }

        @Override
        public String getIntent() {
            try {
                return ((Markup) annot).getIntent();
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    static class ReplyContent implements NoteAnnotContent {
        int pageIndex;
        String nm;
        String content;
        String parentNM;

        public ReplyContent(int pageIndex, String nm, String content, String parentNM) {
            this.pageIndex = pageIndex;
            this.nm = nm;
            this.content = content;
            this.parentNM = parentNM;
        }

        @Override
        public String getIcon() {
            return "";
        }

        @Override
        public String getFromType() {
            return Module.MODULE_NAME_REPLY;
        }

        @Override
        public String getParentNM() {
            return parentNM;
        }

        @Override
        public int getPageIndex() {
            return pageIndex;
        }

        @Override
        public int getType() {
            return Annot.e_Note;
        }

        @Override
        public String getNM() {
            return nm;
        }

        @Override
        public RectF getBBox() {
            return new RectF();
        }

        @Override
        public int getColor() {
            return 0;
        }

        @Override
        public int getOpacity() {
            return 0;
        }

        @Override
        public float getLineWidth() {
            return 0;
        }

        @Override
        public String getSubject() {
            return null;
        }

        @Override
        public DateTime getModifiedDate() {
            return AppDmUtil.currentDateToDocumentDate();
        }

        @Override
        public String getContents() {
            return content;
        }

        @Override
        public String getIntent() {
            return null;
        }

    }

    public static class ReplyDialog extends DialogFragment {

        private EditText mEditText;
        private Context mContext;
        private AppDisplay mDisplay;
        private PDFViewCtrl mPDFViewerCtrl;
        private ViewGroup mParent;
        private ReplyCallback mCallback;
        private int mTitleID;
        private boolean mDialogEditable = false;

        public void init(PDFViewCtrl pdfViewCtrl, ViewGroup parent, boolean dialogEditable, int titleId, ReplyCallback callback) {
            mPDFViewerCtrl = pdfViewCtrl;
            mParent = parent;
            mDialogEditable = dialogEditable;
            mTitleID = titleId;
            mCallback = callback;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            mContext = this.getActivity().getApplicationContext();
            mDisplay = AppDisplay.getInstance(mContext);
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            if (mCallback == null || mTitleID == 0) {
                ReplyDialog.this.dismiss();
                return;
            }
            setCancelable(true);
        }

        @Override
        public void onDetach() {
            super.onDetach();
            if (mEditText != null) {
                AppUtil.dismissInputSoft(mEditText);
            }
            AppDialogManager.getInstance().remove(this);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (mCallback == null || getActivity() == null || mTitleID == 0) {
                return super.onCreateDialog(savedInstanceState);
            }
            Dialog dialog = new Dialog(getActivity(), R.style.rv_dialog_style);

            dialog.setContentView(createView());
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dlg_title_bg_4circle_corner_white);

            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = mDisplay.getDialogWidth();
            dialog.getWindow().setAttributes(params);

            dialog.setCanceledOnTouchOutside(true);
            return dialog;
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);

            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mEditText.setMaxLines(5);
            } else {
                mEditText.setMaxLines(10);
            }
        }

        @Override
        public void onActivityCreated(Bundle arg0) {
            super.onActivityCreated(arg0);
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public View createView() {
            View view = View.inflate(mContext, R.layout.rd_note_dialog_edit, null);
            ((TextView) view.findViewById(R.id.rd_note_dialog_edit_title)).setText(mContext.getString(mTitleID));
            mEditText = (EditText) view.findViewById(R.id.rd_note_dialog_edit);
            Button bt_close = (Button) view.findViewById(R.id.rd_note_dialog_edit_cancel);
            Button bt_ok = (Button) view.findViewById(R.id.rd_note_dialog_edit_ok);
            String content = mCallback.getContent() == null ? "" : mCallback.getContent();

            mEditText.setText(content);

            if (!mDialogEditable) {
                bt_ok.setEnabled(false);
                bt_ok.setTextColor(getActivity().getApplicationContext().getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));

                mEditText.setFocusable(false);
                mEditText.setLongClickable(false);
                if (Build.VERSION.SDK_INT >= 11) {
                    mEditText.setCustomSelectionActionModeCallback(new ActionMode.Callback() {

                        @Override
                        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                            return false;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode mode) {
                        }

                        @Override
                        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                            return false;
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                            return false;
                        }
                    });
                } else {
                    mEditText.setEnabled(false);
                }
            } else {
                mEditText.setEnabled(true);
                bt_ok.setEnabled(true);
                bt_ok.setTextColor(getActivity().getApplicationContext().getResources().getColor(R.color.dlg_bt_text_selector));

                mEditText.setCursorVisible(true);
                mEditText.setFocusable(true);
                mEditText.setSelection(content.length());
                AppUtil.showSoftInput(mEditText);
            }
            bt_close.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (((UIExtensionsManager) mPDFViewerCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
                        AppUtil.dismissInputSoft(mEditText);
                    }
                    AppDialogManager.getInstance().dismiss(ReplyDialog.this);
                }
            });
            bt_ok.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    AppUtil.dismissInputSoft(mEditText);
                    AppDialogManager.getInstance().dismiss(ReplyDialog.this);
                    if (mCallback != null && mEditText.getText() != null) {
                        String Content = mEditText.getText().toString().trim();
                        if (!Content.equals(mCallback.getContent())) {
                            mCallback.result(Content);
                        }
                    }
                }
            });

            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mEditText.setMaxLines(5);
            } else {
                mEditText.setMaxLines(10);
            }

            RelativeLayout.LayoutParams editParams = (RelativeLayout.LayoutParams) mEditText.getLayoutParams();
            editParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            mEditText.setLayoutParams(editParams);

            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(mDisplay.getDialogWidth(), ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(params);

            return view;
        }
    }

    public static class CommentsFragment extends DialogFragment {
        private Markup mAnnot;
        private Context mContext;
        private AppDisplay mDisplay;
        private PDFViewCtrl mPDFViewCtrl;
        private CommentsAdapter mAdapter;
        private final List<AnnotNode> mCheckedNodes = new ArrayList<AnnotNode>();

        private TextView mReplyClear;
        private ProgressDialog mDeleteDialog;
        private AnnotNode mRootNode;
        private UITextEditDialog mSRDeleteDialog;
        private View mDialogContentView;
        private UITextEditDialog mClearDialog;

        private ArrayList<Boolean> mItemMoreViewShow;
        private boolean isTouchHold;
        private ViewGroup mParent;

        private boolean mIsReplyGroup;

        public CommentsFragment() {
            mItemMoreViewShow = new ArrayList<Boolean>();
            mAdapter = new CommentsAdapter(mItemMoreViewShow);
            mContext = null;
        }

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);

            if (mAdapter == null) {
                CommentsFragment.this.dismiss();
                return;
            }

            Looper.myQueue().addIdleHandler(new IdleHandler() {

                @Override
                public boolean queueIdle() {
                    init();
                    return false;
                }
            });
        }

        private void init() {
            if (mAnnot == null) {
                CommentsFragment.this.dismiss();
                return;
            }

            mAdapter.clearNodes();
            mCheckedNodes.clear();

            try {
                PDFPage page = mAnnot.getPage();
                int pageIndex = page.getIndex();
                int count = page.getAnnotCount();
                boolean canAddAnnot = ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot();
                boolean isReadOnly = AppAnnotUtil.isReadOnly(mAnnot);
                boolean isLocked = AppAnnotUtil.isLocked(mAnnot);

                for (int i = 0; i < count; i++) {
                    Annot annot = AppAnnotUtil.createAnnot(page.getAnnot(i));

                    if (annot == null || annot.isEmpty() || AppAnnotUtil.isSupportGroupElement(annot) ||
                            !AppAnnotUtil.isSupportEditAnnot(annot))
                        continue;

                    if (annot.getType() == Annot.e_Screen) continue;

                    String name = AppAnnotUtil.getAnnotUniqueID(annot);
                    if (AppUtil.isEmpty(name)) {
                        continue;
                    }
                    String mName = AppAnnotUtil.getAnnotUniqueID(mAnnot);
                    boolean isMatch = name.equals(mName);
                    if (!AppAnnotUtil.isSupportEditAnnot(annot) || isMatch) {
                        continue;
                    }

                    Markup dmAnnot = (Markup) annot;

                    AnnotNode node = new AnnotNode(pageIndex, dmAnnot, AppAnnotUtil.getReplyToAnnot(dmAnnot));
                    node.setAuthor(dmAnnot.getTitle());
                    node.setContent(dmAnnot.getContent());
                    String date = AppDmUtil.getLocalDateString(dmAnnot.getModifiedDateTime());

                    if (date == null || date.equals(AppDmUtil.dateOriValue)) {
                        date = AppDmUtil.getLocalDateString(dmAnnot.getCreationDateTime());
                    }
                    node.setDate(date);
                    node.setCreationDate(AppDmUtil.getLocalDateString(dmAnnot.getCreationDateTime()));
                    node.setModifyDate(AppDmUtil.getLocalDateString(dmAnnot.getModifiedDateTime()));
                    node.setType(dmAnnot.getType());
                    boolean _isReadOnly = AppAnnotUtil.isReadOnly(dmAnnot);
                    boolean _isLocked = AppAnnotUtil.isLocked(dmAnnot);
                    boolean enable = canAddAnnot && !_isReadOnly;
                    node.setEditEnable(enable);
                    node.setDeletable(enable && !_isLocked);
                    node.setApplyRedaction(!dmAnnot.isEmpty() && dmAnnot.getType() == Annot.e_Redact);
                    node.setCanReply(AppAnnotUtil.isAnnotSupportReply(dmAnnot) && !_isReadOnly);
                    node.setCanComment(!(_isLocked || _isReadOnly));
                    mAdapter.addNode(node);
                }

                mRootNode = new AnnotNode(pageIndex, mAnnot, AppAnnotUtil.getReplyToAnnot(mAnnot));
                mRootNode.setAuthor(mAnnot.getTitle());
                mRootNode.setContent(mAnnot.getContent());
                String date = AppDmUtil.getLocalDateString(mAnnot.getModifiedDateTime());

                if (date == null || date.equals(AppDmUtil.dateOriValue)) {
                    date = AppDmUtil.getLocalDateString(mAnnot.getCreationDateTime());
                }

                mRootNode.setDate(date);
                mRootNode.setCreationDate(AppDmUtil.getLocalDateString(mAnnot.getCreationDateTime()));
                mRootNode.setModifyDate(AppDmUtil.getLocalDateString(mAnnot.getModifiedDateTime()));

                mRootNode.setEditEnable(canAddAnnot && !isReadOnly);
                mRootNode.setApplyRedaction(mAnnot != null && !mAnnot.isEmpty() && mAnnot.getType() == Annot.e_Redact);
                mRootNode.setDeletable(canAddAnnot && !(isReadOnly || isLocked));
                mRootNode.setCanComment(!(isLocked || isReadOnly));
                mRootNode.setCanReply(AppAnnotUtil.isAnnotSupportReply(mAnnot) && !isReadOnly);
                mRootNode.setType(mAnnot.getType());
                mAdapter.addNode(mRootNode);
                mAdapter.establishReplyList(mRootNode);
                mReplyClear.setEnabled(canAddAnnot && !(isReadOnly || isLocked));

                mAdapter.notifyDataSetChanged();
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        public boolean canEdit(@NonNull AnnotNode node) {
            if (node.isRoot()) {
                return node.isEditEnable();
            } else {
                return node.isEditEnable() && canEdit(node.getParent());
            }
        }

        public boolean canDelete(@NonNull AnnotNode node) {
            if (canReply(node)) return node.canDelete();
            if (node.isRoot()) {
                return node.canDelete();
            } else {
                return node.canDelete() && canDelete(node.getParent());
            }
        }

        public boolean canReply(@NonNull AnnotNode node) {
            if (node.isRoot()) {
                return node.canReply();
            } else {
                return node.canReply() && canReply(node.getParent());
            }
        }

        public boolean canComment(@NonNull AnnotNode node) {
            if (node.isRoot()) {
                return node.canComment();
            } else {
                return node.canComment() && canComment(node.getParent());
            }
        }

        public void init(PDFViewCtrl pdfViewCtrl, ViewGroup parent, Annot dmAnnot, boolean isReplyGroup) {
            mPDFViewCtrl = pdfViewCtrl;
            mParent = parent;
            mAnnot = (Markup) dmAnnot;
            mAdapter.clearNodes();
            mCheckedNodes.clear();
            mIsReplyGroup = isReplyGroup;
            mAdapter.setIsReplyGroup(mIsReplyGroup);
        }

        @SuppressLint("NewApi")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mContext = this.getActivity().getApplicationContext();
            mDisplay = AppDisplay.getInstance(mContext);

            if (!AppDisplay.getInstance(mContext).isPad()) {
                int theme;
                if (Build.VERSION.SDK_INT >= 21) {
                    theme = android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen;
                } else if (Build.VERSION.SDK_INT >= 14) {
                    theme = android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen;
                } else if (Build.VERSION.SDK_INT >= 11) {
                    theme = android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen;
                } else {
                    theme = android.R.style.Theme_Light_NoTitleBar_Fullscreen;
                }

                setStyle(STYLE_NO_TITLE, theme);
            }
        }

        @NonNull
        @SuppressLint("NewApi")
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (mAdapter == null) {
                return super.onCreateDialog(savedInstanceState);
            }
            if (AppDisplay.getInstance(mContext).isPad()) {
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
                Dialog dialog = new Dialog(getActivity(), theme);
                int width = mDisplay.getDialogWidth();
                int height = mDisplay.getDialogHeight();
                dialog.setContentView(createView());
                WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
                params.width = width;
                params.height = height;
                dialog.getWindow().setAttributes(params);
                dialog.setCanceledOnTouchOutside(true);

                dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.dlg_title_bg_4circle_corner_white);

                return dialog;
            }
            return super.onCreateDialog(savedInstanceState);
        }

        private View createView() {
            mDialogContentView = View.inflate(mContext, R.layout.annot_reply_main, null);

            RelativeLayout replyTop = (RelativeLayout) mDialogContentView.findViewById(R.id.annot_reply_top);
            ImageView replyBack = (ImageView) mDialogContentView.findViewById(R.id.annot_reply_back);
            TextView replyTitle = (TextView) mDialogContentView.findViewById(R.id.annot_reply_list_title);
            ListView listView = (ListView) mDialogContentView.findViewById(R.id.annot_reply_list);
            mReplyClear = (TextView) mDialogContentView.findViewById(R.id.annot_reply_clear);
            mReplyClear.setEnabled(false);
            LinearLayout replyContent = (LinearLayout) mDialogContentView.findViewById(R.id.annot_reply_list_ll_content);

            RelativeLayout.LayoutParams replyTopLayoutParams = (RelativeLayout.LayoutParams) replyTop.getLayoutParams();
            RelativeLayout.LayoutParams replyBackLayoutParams = (RelativeLayout.LayoutParams) replyBack.getLayoutParams();
            RelativeLayout.LayoutParams replyTitleLayoutParams = (RelativeLayout.LayoutParams) replyTitle.getLayoutParams();
            RelativeLayout.LayoutParams replyClearLayoutParams = (RelativeLayout.LayoutParams) mReplyClear.getLayoutParams();
            if (mDisplay.isPad()) {
                replyTop.setBackgroundResource(R.drawable.dlg_title_bg_circle_corner_gray);

                replyTopLayoutParams.height = (int) getActivity().getApplicationContext().getResources().getDimension(R.dimen.ux_toolbar_height_pad);
                replyBackLayoutParams.leftMargin = (int) getActivity().getApplicationContext().getResources().getDimension(R.dimen.ux_horz_left_margin_pad);

                replyTitleLayoutParams.leftMargin = 0;
                replyTitleLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);

                replyClearLayoutParams.rightMargin = (int) getActivity().getApplicationContext().getResources().getDimension(R.dimen.ux_horz_left_margin_pad);

                replyContent.setBackgroundResource(R.drawable.dlg_title_bg_cc_bottom_yellow);

                replyBack.setVisibility(View.GONE);
            } else {
                replyTop.setBackgroundResource(R.color.ux_bg_color_toolbar_light);

                replyTopLayoutParams.height = (int) getActivity().getApplicationContext().getResources().getDimension(R.dimen.ux_toolbar_height_phone);
                replyBackLayoutParams.leftMargin = (int) getActivity().getApplicationContext().getResources().getDimension(R.dimen.ux_horz_left_margin_phone);

                replyTitleLayoutParams.leftMargin = mDisplay.dp2px(70.0f);
                replyTitleLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);

                replyClearLayoutParams.rightMargin = (int) getActivity().getApplicationContext().getResources().getDimension(R.dimen.ux_horz_left_margin_phone);

                replyContent.setBackgroundResource(R.color.ux_color_yellow);

                replyBack.setVisibility(View.VISIBLE);
            }
            replyTop.setLayoutParams(replyTopLayoutParams);
            replyBack.setLayoutParams(replyBackLayoutParams);
            replyTitle.setLayoutParams(replyTitleLayoutParams);
            mReplyClear.setLayoutParams(replyClearLayoutParams);

            mDialogContentView.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            mReplyClear.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (AppUtil.isFastDoubleClick()) return;

                    if (mPDFViewCtrl.getUIExtensionsManager() == null) return;
                    Context context = ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                    if (context == null) return;
                    mClearDialog = new UITextEditDialog(context);
                    mClearDialog.setTitle(mContext.getString(R.string.hm_clear));
                    mClearDialog.getPromptTextView().setText(mContext.getString(R.string.rv_panel_annot_delete_tips));
                    mClearDialog.getInputEditText().setVisibility(View.GONE);
                    mClearDialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (AppUtil.isFastDoubleClick()) return;

                            mAdapter.resetCheckedNodes();
                            Collections.sort(mCheckedNodes);
                            mClearDialog.dismiss();

                            mDeleteDialog = new ProgressDialog(getActivity());
                            mDeleteDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                            mDeleteDialog.setCancelable(false);
                            mDeleteDialog.setIndeterminate(false);
                            mDeleteDialog.setMessage(mContext.getString(R.string.rv_panel_annot_deleting));
                            mDeleteDialog.show();
                            deleteItems();
                        }
                    });
                    mClearDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mClearDialog.dismiss();
                        }
                    });
                    mClearDialog.show();
                }
            });

            listView.setAdapter(mAdapter);
            listView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int action = MotionEventCompat.getActionMasked(event);
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            boolean show = false;
                            int position = 0;
                            for (int i = 0; i < mAdapter.getCount(); i++) {
                                if (mItemMoreViewShow.get(i)) {
                                    show = true;
                                    position = i;
                                    break;
                                }
                            }
                            if (show) {
                                mItemMoreViewShow.set(position, false);
                                mAdapter.notifyDataSetChanged();
                                isTouchHold = true;
                                return true;
                            }

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (isTouchHold) {
                                isTouchHold = false;
                                return true;
                            }
                        default:
                    }
                    return false;
                }
            });


            mAdapter.notifyDataSetChanged();
            replyBack.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    AppDialogManager.getInstance().dismiss(CommentsFragment.this);
                }
            });
            return mDialogContentView;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            if (mDisplay.isPad()) {
                return super.onCreateView(inflater, container, savedInstanceState);
            }
            return createView();
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mCheckedNodes.clear();
            if (mAdapter != null) {
                mAdapter.clearNodes();
            }
            if (mSRDeleteDialog != null && mSRDeleteDialog.getDialog().isShowing()) {
                mSRDeleteDialog.dismiss();
            }
            mSRDeleteDialog = null;
            resetDeleteDialog();
            AppDialogManager.getInstance().remove(this);
        }

        private void beginToDelete() {
            if (checkDeleteStatus() == DELETE_SRCAN) {
                if (mSRDeleteDialog == null || mSRDeleteDialog.getDialog().getOwnerActivity() == null) {
                    mSRDeleteDialog = new UITextEditDialog(getActivity());
                    mSRDeleteDialog.getPromptTextView().setText(mContext.getString(R.string.rv_panel_annot_delete_tips));
                    mSRDeleteDialog.setTitle(mContext.getString(R.string.cloud_delete_tv));
                    mSRDeleteDialog.getInputEditText().setVisibility(View.GONE);
                }
                mSRDeleteDialog.getOKButton().setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mSRDeleteDialog.dismiss();
                        mDeleteDialog = new ProgressDialog(getActivity());
                        mDeleteDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        mDeleteDialog.setCancelable(false);
                        mDeleteDialog.setIndeterminate(false);
                        mDeleteDialog.setMessage(mContext.getString(R.string.rv_panel_annot_deleting));
                        mDeleteDialog.show();
                        deleteItems();
                    }
                });
                mSRDeleteDialog.show();
            } else {
                mDeleteDialog = new ProgressDialog(getActivity());
                mDeleteDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mDeleteDialog.setCancelable(false);
                mDeleteDialog.setIndeterminate(false);
                mDeleteDialog.setMessage(mContext.getString(R.string.rv_panel_annot_deleting));
                mDeleteDialog.show();
                deleteItems();
            }
        }

        private void resetDeleteDialog() {
            if (mDeleteDialog != null) {
                if (mDeleteDialog.isShowing()) {
                    AppDialogManager.getInstance().dismiss(mDeleteDialog);
                }
                mDeleteDialog = null;
            }
        }

        private void deleteItems() {
            int size = mCheckedNodes.size();
            if (size == 0) {
                resetDeleteDialog();
                notifyCounter();
                mAdapter.notifyDataSetChanged();
                return;
            }

            AnnotNode node = mCheckedNodes.get(size - 1);
            if (node == null) {
                mCheckedNodes.remove(node);
                deleteItems();
                return;
            }

            if (!canDelete(node)) {
                node.setChecked(false);
                mCheckedNodes.remove(node);
                notifyCounter();
                deleteItems();
                return;
            }
            mAdapter.removeNode(node);
        }

        private void flattenItems() {
            int size = mCheckedNodes.size();
            if (size == 0) {
                resetDeleteDialog();
                notifyCounter();
                mAdapter.notifyDataSetChanged();
                return;
            }
            AnnotNode node = mCheckedNodes.get(size - 1);
            if (node == null) {
                mCheckedNodes.remove(node);
                flattenItems();
                return;
            }
            mAdapter.flattenNode(node);
        }

        private void redactionItems() {
            int size = mCheckedNodes.size();
            if (size == 0) {
                notifyCounter();
                mAdapter.notifyDataSetChanged();
                return;
            }
            AnnotNode node = mCheckedNodes.get(size - 1);
            if (node == null) {
                mCheckedNodes.remove(node);
                redactionItems();
                return;
            }
            mAdapter.redactionNode(node);
        }

        private static final int DELETE_CAN = 0;
        private static final int DELETE_SRCAN = 1;

        private int checkDeleteStatus() {
            return DELETE_CAN;
        }

        private void notifyCounter() {
            if (mAdapter.getCount() > 0) {

                mReplyClear.setVisibility(View.VISIBLE);

            } else {

                mReplyClear.setEnabled(false);
            }
        }

        class CommentsAdapter extends BaseAdapter {
            private final List<AnnotNode> mNodes;
            private final List<AnnotNode> mNodesTmp;

            private ArrayList<Boolean> mMoreViewShow;
            private boolean mIsReplyGroup;

            public CommentsAdapter(ArrayList<Boolean> moreViewShow) {
                mNodes = new ArrayList<AnnotNode>();
                mNodesTmp = new ArrayList<AnnotNode>();
                mMoreViewShow = moreViewShow;
            }

            public void setIsReplyGroup(boolean isReplyGroup){
                mIsReplyGroup = isReplyGroup;
            }

            public void removeNode(final AnnotNode node) {
                if (node.getChildren() != null) {
                    node.clearChildren();
                }

                try {
                    ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getDocumentManager().removeAnnot(node.replyAnnot, mIsReplyGroup, true, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mNodesTmp.remove(node);
                if (node.getParent() != null) {
                    node.getParent().removeChildNode(node);
                }
                mCheckedNodes.remove(node);
                notifyCounter();
                deleteItems();

                establishReplyList(mRootNode);
                notifyDataSetChanged();
                if (node.equals(mRootNode)) {
                    AppDialogManager.getInstance().dismiss(CommentsFragment.this);
                }
            }

            private void flattenNode(final AnnotNode node) {
                UIAnnotFlatten.flattenAnnot(mPDFViewCtrl, node.replyAnnot, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            if (node.getChildren() != null) {
                                node.clearChildren();
                            }

                            mNodesTmp.remove(node);
                            if (node.getParent() != null) {
                                node.getParent().removeChildNode(node);
                            }
                            mCheckedNodes.remove(node);
                            notifyCounter();
                            flattenItems();

                            establishReplyList(mRootNode);
                            notifyDataSetChanged();
                            if (node.equals(mRootNode)) {
                                AppDialogManager.getInstance().dismiss(CommentsFragment.this);
                            }
                        }
                    }
                });
            }

            private void redactionNode(final AnnotNode node) {
                UIAnnotRedaction.apply(mPDFViewCtrl, node.replyAnnot, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            if (node.getChildren() != null) {
                                node.clearChildren();
                            }

                            mNodesTmp.remove(node);
                            if (node.getParent() != null) {
                                node.getParent().removeChildNode(node);
                            }
                            mCheckedNodes.remove(node);
                            notifyCounter();
                            redactionItems();

                            establishReplyList(mRootNode);
                            notifyDataSetChanged();
                            if (node.equals(mRootNode)) {
                                AppDialogManager.getInstance().dismiss(CommentsFragment.this);
                            }
                        }
                    }
                });
            }

            private void resetCheckedNodes() {
                mCheckedNodes.clear();
                for (int i = 0; i < mNodes.size(); i++) {
                    AnnotNode node = mNodes.get(i);
                    if (!mCheckedNodes.contains(node) && (node.replyToAnnot == null || node.replyToAnnot.isEmpty())) {
                        mCheckedNodes.add(node);
                    }
                }
                notifyCounter();
            }

            void clearNodes() {
                mNodes.clear();
                mNodesTmp.clear();
            }

            private void establishReplyList(AnnotNode node) {
                mNodes.clear();
                int index = mNodesTmp.indexOf(node);
                if (index == -1) return;
                AnnotNode n = mNodesTmp.get(index);
                mNodes.add(n);
                establishNodeRoot(n);

                mMoreViewShow.clear();
                for (int i = 0; i < mNodes.size(); i++) {
                    mMoreViewShow.add(false);
                }
            }

            private void addNode(AnnotNode node) {
                if (mNodesTmp.contains(node)) return;
                if ((node.replyAnnot == null || node.replyAnnot.isEmpty()) && (node.replyToAnnot == null || node.replyToAnnot.isEmpty())) {
                    return;
                }
                boolean needFind = (node.replyToAnnot != null && !node.replyToAnnot.isEmpty());
                for (AnnotNode an : mNodesTmp) {
                    if (needFind) {
                        if (AppAnnotUtil.getAnnotUniqueID(an.replyAnnot).equals(AppAnnotUtil.getAnnotUniqueID(node.replyToAnnot))) {
                            node.setParent(an);
                            an.addChildNode(node);
                            needFind = false;
                            continue;
                        }
                    }
                    if (an.replyToAnnot != null && !an.replyToAnnot.isEmpty()
                            && AppAnnotUtil.getAnnotUniqueID(node.replyAnnot).equals(AppAnnotUtil.getAnnotUniqueID(an.replyToAnnot))) {
                        an.setParent(node);
                        node.addChildNode(an);
                    }
                }
                mNodesTmp.add(node);
            }

            private void establishNodeRoot(AnnotNode parent) {
                if (parent == null) return;
                if (parent.isLeafNode() || !parent.isExpanded()) return;
                if (parent.getChildren() == null) return;
                if (parent.getChildren().size() > 1) {
                    Collections.sort(parent.getChildren());
                }
                for (AnnotNode child : parent.getChildren()) {
                    mNodes.add(child);
                    establishNodeRoot(child);
                }
            }

            @Override
            public int getCount() {
                return mNodes.size();
            }

            @Override
            public Object getItem(int position) {
                return mNodes.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                if (convertView == null) {
                    holder = new ViewHolder();
                    convertView = View.inflate(mContext, R.layout.annot_reply_item, null);
                    holder.mReplyRoot = (LinearLayout) convertView.findViewById(R.id.annot_reply_top_layout);
                    holder.mReplyListRL = (RelativeLayout) convertView.findViewById(R.id.annot_reply_list_rl);
                    holder.mAuthorTextView = (TextView) convertView.findViewById(R.id.annot_reply_author_tv);
                    holder.mContentsTextView = (TextView) convertView.findViewById(R.id.annot_reply_contents_tv);
                    holder.mDateTextView = (TextView) convertView.findViewById(R.id.annot_reply_date_tv);
                    holder.mIconImageView = (ImageView) convertView.findViewById(R.id.annot_iv_reply_icon);
                    holder.mExpandImageView = (ImageView) convertView.findViewById(R.id.annot_reply_expand_iv);

                    holder.mIv_relist_more = (ImageView) convertView.findViewById(R.id.rd_annot_relist_item_more);
                    holder.mLl_relist_moreview = (LinearLayout) convertView.findViewById(R.id.rd_annot_relist_item_moreview);

                    holder.mLl_relist_reply = (LinearLayout) convertView.findViewById(R.id.rd_annot_relist_item_ll_reply);
                    holder.mLl_relist_comment = (LinearLayout) convertView.findViewById(R.id.rd_annot_item_ll_comment);
                    holder.mLl_relist_apply = (LinearLayout) convertView.findViewById(R.id.rd_annot_item_ll_redaction);
                    holder.mLl_relist_flatten = (LinearLayout) convertView.findViewById(R.id.rd_annot_item_ll_flatten);
                    holder.mLl_relist_delete = (LinearLayout) convertView.findViewById(R.id.rd_annot_item_ll_delete);

                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }
                final AnnotNode node = mNodes.get(position);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.mContentsTextView.getLayoutParams();

                if (node.isRoot()) {
                    int drawable = AppAnnotUtil.getIconId(AppAnnotUtil.getTypeString(mAnnot));
                    holder.mIconImageView.setImageResource(drawable);
                } else {
                    holder.mIconImageView.setImageResource(R.drawable.annot_reply_selector);
                }
                if (node.isRoot()) {
                    holder.mExpandImageView.setVisibility(View.GONE);

                    params.leftMargin = mDisplay.dp2px(0);
                } else if (node.getLevel() == 1 && !node.isLeafNode()) {
                    holder.mExpandImageView.setVisibility(View.VISIBLE);
                    params.leftMargin = mDisplay.dp2px(24 + 24 + 5);
                    if (node.isExpanded()) {
                        holder.mExpandImageView.setImageResource(R.drawable.annot_reply_item_minus_selector);
                    } else {
                        holder.mExpandImageView.setImageResource(R.drawable.annot_reply_item_add_selector);
                    }
                    holder.mExpandImageView.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            node.setExpanded(!node.isExpanded());
                            establishReplyList(mRootNode);
                            notifyDataSetChanged();
                        }
                    });
                } else {
                    holder.mExpandImageView.setVisibility(View.GONE);
                    holder.mIconImageView.setVisibility(View.VISIBLE);
                    params.leftMargin = mDisplay.dp2px(24 + 5);
                }
                holder.mContentsTextView.setLayoutParams(params);

                int level = node.getLevel() > 2 ? 2 : node.getLevel();
                int ax = mDisplay.dp2px(32);//annotIconWidth
                int px = mDisplay.dp2px(5);//paddingWidth
                int ex = mDisplay.dp2px(21);//expandWidth
                int dx = mDisplay.dp2px(24);//replyIconWidth
                if (level == 0) {
                    convertView.setPadding(0, 0, 0, 0);
                } else if (level == 1) {
                    if (node.isLeafNode()) {
                        convertView.setPadding((ax + px) + (dx + px) * (level - 1), 0, 0, 0);
                    } else {
                        convertView.setPadding((ax + px - ex) + (dx + px) * (level - 1), 0, 0, 0);
                    }
                } else {
                    convertView.setPadding((ax + px) + (dx + px) * (level - 1), 0, 0, 0);
                }

                holder.mDateTextView.setText(node.getDate());
                holder.mAuthorTextView.setText(node.getAuthor());
                holder.mContentsTextView.setText(node.getContent());


                holder.mIv_relist_more.setTag(position);
                holder.mIv_relist_more.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (AppUtil.isFastDoubleClick()) return;

                        int tag = (Integer) v.getTag();
                        for (int i = 0; i < mMoreViewShow.size(); i++) {
                            if (i == tag) {
                                mMoreViewShow.set(i, true);
                            } else {
                                mMoreViewShow.set(i, false);
                            }
                        }
                        notifyDataSetChanged();
                    }
                });

                if (!((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
                    holder.mIv_relist_more.setEnabled(true);
                    holder.mLl_relist_comment.setVisibility(View.VISIBLE);
                    holder.mLl_relist_reply.setVisibility(View.GONE);
                    holder.mLl_relist_apply.setVisibility(View.GONE);
                    holder.mLl_relist_flatten.setVisibility(View.GONE);
                    holder.mLl_relist_delete.setVisibility(View.GONE);

                    holder.mLl_relist_comment.setTag(position);
                    OnClickListener commentListener = new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (AppUtil.isFastDoubleClick()) return;

                            ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                            int tag = (Integer) v.getTag();
                            mMoreViewShow.set(tag, false);
                            replyToAnnot(mPDFViewCtrl, mParent, false, TITLE_EDIT_ID, new ReplyCallback() {
                                @Override
                                public void result(final String Content) {

                                }

                                @Override
                                public String getContent() {
                                    return (String) node.getContent();
                                }
                            });
                        }
                    };
                    holder.mLl_relist_comment.setOnClickListener(commentListener);
                } else {

                    if (node.canReply()) {
                        holder.mLl_relist_reply.setVisibility(View.VISIBLE);
                        holder.mLl_relist_reply.setTag(position);
                        OnClickListener replyListener = new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (AppUtil.isFastDoubleClick()) return;

                                ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                                int tag = (Integer) v.getTag();
                                mMoreViewShow.set(tag, false);
                                replyToAnnot(mPDFViewCtrl, mParent, true, TITLE_COMMENT_ID, new ReplyCallback() {
                                    @Override
                                    public void result(final String content) {
                                        final int pageIndex = node.getPageIndex();
                                        final String uid = AppDmUtil.randomUUID(null);

                                        try {
                                            final PDFPage page = mPDFViewCtrl.getDoc().getPage(pageIndex);

                                            addReplyAnnot(mPDFViewCtrl, node.replyAnnot, page, uid, content, new Event.Callback() {

                                                @Override
                                                public void result(Event event, boolean success) {
                                                    AnnotNode annotNode = new AnnotNode(pageIndex, AppAnnotUtil.getAnnot(page, uid), node.replyAnnot);
                                                    annotNode.setAuthor(AppDmUtil.getAnnotAuthor());
                                                    String dateTemp = AppDmUtil.getLocalDateString(AppDmUtil.currentDateToDocumentDate());
                                                    annotNode.setDate(dateTemp);
//                                                node.setCreationDate(dateTemp);
                                                    node.setModifyDate(dateTemp);
                                                    annotNode.setContent(content);
                                                    annotNode.setType(Annot.e_Note);
                                                    annotNode.setEditEnable(true);
                                                    annotNode.setDeletable(true);
                                                    annotNode.setCanReply(true);
                                                    annotNode.setCanComment(true);
                                                    annotNode.setApplyRedaction(false);

                                                    mAdapter.addNode(annotNode);
                                                    mAdapter.establishReplyList(mRootNode);

                                                    node.setChecked(false);
                                                    mCheckedNodes.clear();

                                                    mAdapter.notifyDataSetChanged();
                                                }
                                            });
                                        } catch (PDFException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public String getContent() {
                                        return null;
                                    }
                                });
                            }
                        };
                        holder.mLl_relist_reply.setOnClickListener(replyListener);
                    } else {
                        holder.mLl_relist_reply.setVisibility(View.GONE);
                    }

                    if (node.canComment()) {
                        holder.mLl_relist_comment.setVisibility(View.VISIBLE);
                        holder.mLl_relist_comment.setTag(position);
                        OnClickListener commentListener = new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (AppUtil.isFastDoubleClick()) return;

                                ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                                int tag = (Integer) v.getTag();
                                mMoreViewShow.set(tag, false);

                                boolean editable = canEdit(node);//node.isEditEnable();
                                replyToAnnot(mPDFViewCtrl, mParent, editable, TITLE_EDIT_ID, new ReplyCallback() {
                                    @Override
                                    public void result(final String content) {
                                        final Annot annot = node.replyAnnot;
                                        if (annot == null) return;
                                        ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getDocumentManager().modifyAnnot(node.replyAnnot, new CommentContent(node.replyAnnot, content),
                                                true, new Event.Callback() {
                                                    @Override
                                                    public void result(Event event, boolean success) {
                                                        if (success) {
                                                            try {
                                                                node.setAuthor(((Markup) annot).getTitle());
                                                                node.setDate(AppDmUtil.getLocalDateString(annot.getModifiedDateTime()));
                                                                node.setModifyDate(AppDmUtil.getLocalDateString(annot.getModifiedDateTime()));
                                                                node.setContent(annot.getContent());

                                                                mAdapter.notifyDataSetChanged();
                                                            } catch (PDFException e) {
                                                                e.printStackTrace();
                                                            }

                                                        }
                                                    }
                                                });
                                    }

                                    @Override
                                    public String getContent() {
                                        return (String) node.getContent();
                                    }
                                });
                            }
                        };
                        holder.mLl_relist_comment.setOnClickListener(commentListener);
                    } else {
                        holder.mLl_relist_comment.setVisibility(View.GONE);
                    }

                    if (node.canDelete()/*node.isEditEnable()*/) {
                        holder.mLl_relist_delete.setVisibility(View.VISIBLE);
                        OnClickListener deleteListener = new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (AppUtil.isFastDoubleClick()) return;
                                mCheckedNodes.clear();
                                if (!mCheckedNodes.contains(node)) {
                                    mCheckedNodes.add(node);
                                }
                                Collections.sort(mCheckedNodes);
                                beginToDelete();
                            }
                        };
                        holder.mLl_relist_delete.setOnClickListener(deleteListener);

                    } else {
                        holder.mLl_relist_delete.setVisibility(View.GONE);
                    }

                    if (!((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getDocumentManager().isSign() &&
                            ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyContents() &&
                            node.canApplyRedaction()) {
                        if (level > 0) {
                            holder.mLl_relist_apply.setVisibility(View.GONE);
                        } else {
                            holder.mLl_relist_apply.setVisibility(View.VISIBLE);
                            holder.mLl_relist_apply.setTag(position);
                            OnClickListener applyListener = new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (AppUtil.isFastDoubleClick()) return;
                                    mCheckedNodes.clear();
                                    if (!mCheckedNodes.contains(node)) {
                                        mCheckedNodes.add(node);
                                    }
                                    Collections.sort(mCheckedNodes);
                                    redactionItems();
                                }
                            };
                            holder.mLl_relist_apply.setOnClickListener(applyListener);
                        }
                    } else {
                        holder.mLl_relist_apply.setVisibility(View.GONE);
                    }

                    if (level > 0) {
                        holder.mLl_relist_flatten.setVisibility(View.GONE);
                    } else {
                        holder.mLl_relist_flatten.setVisibility(View.VISIBLE);
                        holder.mLl_relist_flatten.setTag(position);
                        OnClickListener flattenListener = new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (AppUtil.isFastDoubleClick()) return;
                                mCheckedNodes.clear();
                                if (!mCheckedNodes.contains(node)) {
                                    mCheckedNodes.add(node);
                                }
                                Collections.sort(mCheckedNodes);
                                flattenItems();
                            }
                        };
                        holder.mLl_relist_flatten.setOnClickListener(flattenListener);
                    }

                    boolean enabled = View.VISIBLE == holder.mLl_relist_reply.getVisibility() ||
                            View.VISIBLE == holder.mLl_relist_comment.getVisibility() ||
                            View.VISIBLE == holder.mLl_relist_apply.getVisibility() ||
                            View.VISIBLE == holder.mLl_relist_flatten.getVisibility() ||
                            View.VISIBLE == holder.mLl_relist_delete.getVisibility();
                    holder.mIv_relist_more.setEnabled(enabled);

                    int visibility = mIsReplyGroup ? View.GONE : View.VISIBLE;
                    holder.mIv_relist_more.setVisibility(visibility);
                }

                holder.mContentsTextView.setTag(position);
                holder.mContentsTextView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (AppUtil.isFastDoubleClick()) return;

                        boolean hasShow = false;
                        int index = 0;
                        for (int i = 0; i < mMoreViewShow.size(); i++) {
                            if (mMoreViewShow.get(i)) {
                                hasShow = true;
                                index = i;
                                break;
                            }
                        }
                        if (hasShow) {
                            mMoreViewShow.set(index, false);
                            mAdapter.notifyDataSetChanged();
                            return;
                        }

                        boolean editable = node.canComment();//node.isEditEnable();
                        replyToAnnot(mPDFViewCtrl, mParent, editable, TITLE_EDIT_ID, new ReplyCallback() {
                            @Override
                            public void result(final String content) {
                                final Annot annot = node.replyAnnot;
                                if (annot == null) return;
                                ((UIExtensionsManager) mPDFViewCtrl.getUIExtensionsManager()).getDocumentManager().modifyAnnot(node.replyAnnot, new CommentContent(annot, content), true, new Event.Callback() {
                                    @Override
                                    public void result(Event event, boolean success) {
                                        if (success) {
                                            try {
                                                node.setAuthor(((Markup) annot).getTitle());
                                                node.setDate(AppDmUtil.getLocalDateString(annot.getModifiedDateTime()));
                                                node.setModifyDate(AppDmUtil.getLocalDateString(annot.getModifiedDateTime()));
                                                node.setContent(annot.getContent());

                                                mAdapter.notifyDataSetChanged();
                                            } catch (PDFException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                });
                            }

                            @Override
                            public String getContent() {
                                return (String) node.getContent();
                            }
                        });
                    }
                });

                LinearLayout.LayoutParams replyListRLLayoutParams = (LinearLayout.LayoutParams) holder.mReplyListRL.getLayoutParams();
                RelativeLayout.LayoutParams replyMoreLayoutParams = (RelativeLayout.LayoutParams) holder.mIv_relist_more.getLayoutParams();
                LinearLayout.LayoutParams contentLayoutParams = (LinearLayout.LayoutParams) holder.mContentsTextView.getLayoutParams();
                if (AppDisplay.getInstance(mContext).isPad()) {
                    replyListRLLayoutParams.leftMargin = (int) getActivity().getApplicationContext().getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
                    replyMoreLayoutParams.rightMargin = (int) getActivity().getApplicationContext().getResources().getDimension(R.dimen.ux_horz_right_margin_pad);
                    contentLayoutParams.rightMargin = (int) getActivity().getApplicationContext().getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
                } else {
                    replyListRLLayoutParams.leftMargin = (int) getActivity().getApplicationContext().getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
                    replyMoreLayoutParams.rightMargin = (int) getActivity().getApplicationContext().getResources().getDimension(R.dimen.ux_horz_right_margin_phone);
                    contentLayoutParams.rightMargin = (int) getActivity().getApplicationContext().getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
                }

                holder.mReplyListRL.setLayoutParams(replyListRLLayoutParams);
                holder.mIv_relist_more.setLayoutParams(replyMoreLayoutParams);
                holder.mContentsTextView.setLayoutParams(contentLayoutParams);

                RelativeLayout.LayoutParams paramsMoreView = (RelativeLayout.LayoutParams) holder.mLl_relist_moreview.getLayoutParams();
                paramsMoreView.height = holder.mReplyRoot.getMeasuredHeight();
                holder.mLl_relist_moreview.setLayoutParams(paramsMoreView);
                if (mMoreViewShow.get(position)) {
                    holder.mLl_relist_moreview.setVisibility(View.VISIBLE);
                } else {
                    holder.mLl_relist_moreview.setVisibility(View.GONE);
                }

                return convertView;
            }

            final class ViewHolder {
                public LinearLayout mReplyRoot;
                public RelativeLayout mReplyListRL;
                public ImageView mExpandImageView;
                public ImageView mIconImageView;
                public TextView mAuthorTextView;
                public TextView mDateTextView;
                public TextView mContentsTextView;

                public ImageView mIv_relist_more;
                public LinearLayout mLl_relist_moreview;

                public LinearLayout mLl_relist_reply;
                public LinearLayout mLl_relist_comment;
                public LinearLayout mLl_relist_apply;
                public LinearLayout mLl_relist_flatten;
                public LinearLayout mLl_relist_delete;
            }
        }

        class AnnotNode implements Comparable<AnnotNode> {
            private Annot replyAnnot;
            private Annot replyToAnnot;
            private int pageIndex;

            private boolean canReply;
            private boolean canComment;
            private boolean canDelete;
            private boolean editEnable;
            private boolean isChecked;
            private boolean isExpanded;
            private boolean canApplyRedaction;

            private int type;
            private String author;
            private CharSequence content;
            private String date;
            private String mModifyDate;
            private String mCreationDate;
            private List<AnnotNode> childNodes;
            private AnnotNode parent;

            AnnotNode(int pageIndex, Annot annot, Annot replyTo) {
                this.pageIndex = pageIndex;
                this.replyAnnot = annot;
                this.replyToAnnot = replyTo;
            }

            public void clearChildren() {
                if (this.childNodes != null) {
                    this.childNodes.clear();
                }
            }

            void addChildNode(AnnotNode node) {
                if (this.childNodes == null) {
                    this.childNodes = new ArrayList<AnnotNode>();
                }
                if (!this.childNodes.contains(node)) {
                    this.childNodes.add(node);
                }
            }

            void removeChildNode(AnnotNode node) {
                if (this.childNodes != null) {
                    this.childNodes.remove(node);
                }
            }

            public int getPageIndex() {
                return this.pageIndex;
            }

            public boolean isEditEnable() {
                return this.editEnable;
            }

            public void setEditEnable(boolean editEnable) {
                this.editEnable = editEnable;
            }

            public boolean canDelete() {
                return canDelete;
            }

            public void setDeletable(boolean canDelete) {
                this.canDelete = canDelete;
            }

            public boolean canReply() {
                return canReply;
            }

            public boolean canApplyRedaction() {
                return canApplyRedaction;
            }

            public void setApplyRedaction(boolean canApplyRedaction) {
                this.canApplyRedaction = canApplyRedaction;
            }

            public void setCanReply(boolean canReply) {
                this.canReply = canReply;
            }

            public boolean canComment() {
                return canComment;
            }

            public void setCanComment(boolean canComment) {
                this.canComment = canComment;
            }

            public List<AnnotNode> getChildren() {
                return this.childNodes;
            }

            public int getType() {
                return this.type;
            }

            public void setType(int type) {
                this.type = type;
            }

            public String getAuthor() {
                return this.author == null ? "" : this.author;
            }

            public void setAuthor(String author) {
                this.author = author;
            }

            public void setContent(CharSequence content) {
                this.content = content;
            }

            public CharSequence getContent() {
                return this.content == null ? "" : this.content;
            }

            public void setDate(String date) {
                this.date = date;
            }

            public String getDate() {
                return this.date == null ? AppDmUtil.dateOriValue : this.date;
            }

            public void setModifyDate(String modifyDate) {
                this.mModifyDate = modifyDate;
            }

            public String getModifyDate() {
                return this.mModifyDate == null ? AppDmUtil.dateOriValue : this.mModifyDate;
            }

            public void setCreationDate(String creationDate) {
                this.mCreationDate = creationDate;
            }

            public String getCreationDate() {
                return this.mCreationDate == null ? AppDmUtil.dateOriValue : this.mCreationDate;
            }

            public void setChecked(boolean isChecked) {
                this.isChecked = isChecked;
            }

            public boolean isChecked() {
                return this.isChecked;
            }

            public void setParent(AnnotNode parent) {
                this.parent = parent;
            }

            public AnnotNode getParent() {
                return this.parent;
            }

            public boolean isRoot() {
                return this.parent == null;
            }

            public boolean isLeafNode() {
                return this.childNodes == null || this.childNodes.size() == 0;
            }

            public int getLevel() {
                return this.parent == null ? 0 : parent.getLevel() + 1;
            }

            public boolean isExpanded() {
                return this.isExpanded || this.parent == null || this.getLevel() != 1;
            }

            public void setExpanded(boolean isExpanded) {
                this.isExpanded = isExpanded;
            }

            @Override
            public int compareTo(AnnotNode another) {
                if (another == null) return 0;
                if (getLevel() != another.getLevel()) {
                    return getLevel() - another.getLevel();
                }
                try {
                    String lCreationDate = getCreationDate();
                    if (lCreationDate == null || AppDmUtil.dateOriValue.equals(lCreationDate)) {
                        lCreationDate = getModifyDate();
                    }
                    String rCreationDate = another.getCreationDate();
                    if (rCreationDate == null || AppDmUtil.dateOriValue.equals(rCreationDate)) {
                        rCreationDate = another.getModifyDate();
                    }
                    Date lDate = AppDmUtil.documentDateToJavaDate(AppDmUtil.parseDocumentDate(lCreationDate));
                    Date rDate = AppDmUtil.documentDateToJavaDate(AppDmUtil.parseDocumentDate(rCreationDate));
                    if (lDate == null || rDate == null)
                        return 0;

                    return lDate.before(rDate) ? -1 : (lDate.after(rDate) ? 1 : 0);
                } catch (Exception e) {
                }
                return 0;
            }
        }
    }
}
