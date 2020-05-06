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
package com.foxit.uiextensions.modules;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.ReadingBookmark;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ReadingBookmarkSupport {

    interface IReadingBookmarkListener {
        void onMoreClick(int position);

        void onRename(int position);

        void onDelete(int position);
    }

    interface IReadingBookmarkCallback {
        void result();
    }

    private final ReadingBookmarkModule mReadingBookmarkModule;
    private final ReadingBookmarkAdapter mAdapter;
    private boolean mNeedRelayout = false;

    public ReadingBookmarkSupport(ReadingBookmarkModule panelModule) {
        mReadingBookmarkModule = panelModule;
        mAdapter = new ReadingBookmarkAdapter();
    }

    public ReadingBookmarkAdapter getAdapter() {
        return mAdapter;
    }

    public void clearAllNodes() {
        mAdapter.clearAllNodes();
    }

    public void addReadingBookmarkNode(int index, String title) {
        mAdapter.addBookmarkNode(index, title);
    }

    public boolean needRelayout(){
        return mNeedRelayout;
    }

    public void removeReadingBookmarkNode(int index) {
        mAdapter.removeBookmarkNode(index);
    }


    public class ReadingBookmarkNode {
        private ReadingBookmark mBookrmak;
        private String mTitle;
        private int mIndex;
        private DateTime mModifiedDateTime;

        public ReadingBookmarkNode(ReadingBookmark bookmark) {
            mBookrmak = bookmark;
            try {
                mTitle = mBookrmak.getTitle();
                mIndex = mBookrmak.getPageIndex();
                mModifiedDateTime = mBookrmak.getDateTime(false) == null ? mBookrmak.getDateTime(true) : mBookrmak.getDateTime(false);
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        public void setTitle(String title) {
            mTitle = title;
            try {
                mBookrmak.setTitle(title);
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        public void setModifiedDateTime(DateTime dateTime) {
            mModifiedDateTime = dateTime;
            try {
                mBookrmak.setDateTime(mModifiedDateTime, false);
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }

        public DateTime getModifiedDateTime() {
            return mModifiedDateTime;
        }

        public String getTitle() {
            return mTitle;
        }

        public int getIndex() {
            return mIndex;
        }

        public void setIndex(int index) {
            mIndex = index;
            try {
                mBookrmak.setPageIndex(index);
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    public class ReadingBookmarkAdapter extends BaseAdapter {

        private final Context mContext;
        private final PDFViewCtrl mPdfViewCtrl;

        private final ArrayList<ReadingBookmarkNode> mNodeList;
        private PDFDoc mPdfDoc;
        private final ArrayList<Boolean> mItemMoreViewShow;
        private final IReadingBookmarkListener mBookmarkListener;

        class RBViewHolder {
            public TextView mRMContent;
            public TextView mRMCreateTime;

            public ImageView mRMMore;
            public LinearLayout mRMMoreView;

            public LinearLayout mLlRename;
            public ImageView mRMRename;
            public TextView mRMTvRename;

            public LinearLayout mLlDelete;
            public ImageView mRMDelete;
            public TextView mRMTvDelete;
        }


        class RemoveReadingBookmarkTask extends Task {
            public RemoveReadingBookmarkTask(final IReadingBookmarkCallback callback) {
                super(new CallBack() {
                    @Override
                    public void result(Task task) {
                        callback.result();
                    }
                });
            }

            @Override
            protected void execute() {
                try {
                    ArrayList<ReadingBookmark> mTmpReadingBookmark = new ArrayList<ReadingBookmark>();
                    for (int i = 0; i < mPdfDoc.getReadingBookmarkCount(); i++) {
                        mTmpReadingBookmark.add(mPdfDoc.getReadingBookmark(i));
                    }

                    for (ReadingBookmark readingBookmark : mTmpReadingBookmark) {
                        mPdfDoc.removeReadingBookmark(readingBookmark);
                        updateLayout();
                    }
                    mTmpReadingBookmark.clear();
                    mNodeList.clear();

                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }

            private void updateLayout() {
                if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
                Activity activity = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                if (activity == null) return;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        }

        public ReadingBookmarkAdapter() {
            mContext = mReadingBookmarkModule.mContentView.getContext();
            mPdfViewCtrl = mReadingBookmarkModule.mPdfViewCtrl;
            mNodeList = new ArrayList<ReadingBookmarkNode>();
            mItemMoreViewShow = mReadingBookmarkModule.mItemMoreViewShow;
            mBookmarkListener = new IReadingBookmarkListener() {
                @Override
                public void onMoreClick(int position) {
                    for (int i = 0; i < mItemMoreViewShow.size(); i++) {
                        if (i == position) {

                            mItemMoreViewShow.set(i, true);
                        } else {
                            mItemMoreViewShow.set(i, false);
                        }
                    }
                    notifyDataSetChanged();
                }

                @Override
                public void onRename(final int position) {
                    if (!AppUtil.isFastDoubleClick()) {
                        if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
                        Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                        if (context == null) return;
                        final UITextEditDialog renameDlg = new UITextEditDialog(context);
                        renameDlg.getPromptTextView().setVisibility(View.GONE);
                        renameDlg.setTitle(mContext.getApplicationContext().getString(R.string.fx_string_rename));
                        renameDlg.getDialog().setCanceledOnTouchOutside(false);
                        final InputMethodManager mInputManager;
                        final EditText renameDlgEt = renameDlg.getInputEditText();
                        final Button renameDlgOk = renameDlg.getOKButton();
                        final Button renameDlgCancel = renameDlg.getCancelButton();

                        renameDlgEt.setTextSize(17.3f);
                        renameDlgEt.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});
                        renameDlgEt.setTextColor(Color.BLACK);
                        mInputManager = (InputMethodManager) renameDlgEt.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                        ReadingBookmarkNode node = mNodeList.get(position);
                        renameDlgEt.setText(node.getTitle());
                        renameDlgEt.selectAll();
                        renameDlgOk.setEnabled(false);
                        renameDlgOk.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));
                        renameDlgEt.addTextChangedListener(new TextWatcher() {

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                if (renameDlgEt.getText().toString().trim().length() > 199) {
                                    final Toast toast = Toast.makeText(mContext.getApplicationContext(), R.string.rv_panel_readingbookmark_tips_limited, Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    final Timer timer = new Timer();
                                    timer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            toast.show();
                                        }
                                    }, 0, 3000);
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            toast.cancel();
                                            timer.cancel();
                                        }
                                    }, 5000);

                                } else if (renameDlgEt.getText().toString().trim().length() == 0) {
                                    renameDlgOk.setEnabled(false);
                                    renameDlgOk.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));

                                } else {
                                    renameDlgOk.setEnabled(true);
                                    renameDlgOk.setTextColor(mContext.getResources().getColor(R.color.dlg_bt_text_selector));

                                }
                            }

                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                                for (int i = s.length(); i > 0; i--) {
                                    if (s.subSequence(i - 1, i).toString().equals("\n"))
                                        s.replace(i - 1, i, "");
                                }
                            }
                        });

                        renameDlgEt.setOnKeyListener(new View.OnKeyListener() {

                            @Override
                            public boolean onKey(View v, int keyCode, KeyEvent event) {
                                if (KeyEvent.KEYCODE_ENTER == keyCode && event.getAction() == KeyEvent.ACTION_DOWN) {
                                    mInputManager.hideSoftInputFromWindow(renameDlgEt.getWindowToken(), 0);
                                    return true;
                                }
                                return false;
                            }
                        });

                        renameDlgOk.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                String newContent = renameDlgEt.getText().toString().trim();
                                updateBookmarkNode(position, newContent, AppDmUtil.currentDateToDocumentDate());
                                notifyDataSetChanged();
                                mInputManager.hideSoftInputFromWindow(renameDlgEt.getWindowToken(), 0);
                                renameDlg.dismiss();
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
                            }
                        });

                        renameDlgCancel.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                mInputManager.hideSoftInputFromWindow(renameDlgEt.getWindowToken(), 0);
                                renameDlg.dismiss();
                            }
                        });
                        renameDlg.show();
                        renameDlgEt.setFocusable(true);
                        renameDlgEt.setFocusableInTouchMode(true);
                        renameDlgEt.requestFocus();
                        Timer timer = new Timer();
                        timer.schedule(new TimerTask() {

                            @Override
                            public void run() {
                                mInputManager.showSoftInput(renameDlgEt, 0);
                            }
                        }, 500);
                    }
                }

                @Override
                public void onDelete(int position) {
                    ReadingBookmarkNode node = mNodeList.get(position);
                    mAdapter.removeBookmarkNode(node.getIndex());
                    if (node.getIndex() == mPdfViewCtrl.getCurrentPage()) {
                        mReadingBookmarkModule.changeMarkItemState(false);
                    }
                    mReadingBookmarkModule.changeViewState(mNodeList.size() != 0);
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
                }
            };
        }

        public boolean isMarked(int index){
            for(ReadingBookmarkNode node:mNodeList){
                if(node.getIndex() == index)
                    return true;
            }
            return false;
        }

        protected void initBookmarkList() {
            try {
                mPdfDoc = mPdfViewCtrl.getDoc();
                if (mPdfDoc == null) {
                    return;
                }
                mNodeList.clear();
                mItemMoreViewShow.clear();
                int nCount = mPdfDoc.getReadingBookmarkCount();
                for (int i = 0; i < nCount; i++) {
                    ReadingBookmark readingBookmark = mPdfDoc.getReadingBookmark(i);
                    if (readingBookmark == null)
                        continue;
                    ReadingBookmarkNode node = new ReadingBookmarkNode(readingBookmark);
                    mNodeList.add(node);
                    mItemMoreViewShow.add(false);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }


        public void addBookmarkNode(int pageIndex, String title) {
            try {
                ReadingBookmark readingBookmark = mPdfDoc.insertReadingBookmark(0, title, pageIndex);
                DateTime dateTime = AppDmUtil.currentDateToDocumentDate();
                readingBookmark.setDateTime(dateTime, true);
                readingBookmark.setDateTime(dateTime, false);
                mNodeList.add(0, new ReadingBookmarkNode(readingBookmark));
            } catch (PDFException e) {
                e.printStackTrace();
            }
            mItemMoreViewShow.add(0, false);
            notifyDataSetChanged();
        }

        public void removeBookmarkNode(int pageIndex) {
            for (int position = 0; position < mNodeList.size(); position++) {
                if (mNodeList.get(position).getIndex() == pageIndex) {
                    mNodeList.remove(position);
                    mItemMoreViewShow.remove(position);
                    break;
                }
            }
            try {
                int nCount = mPdfDoc.getReadingBookmarkCount();
                for (int i = 0; i < nCount; i++) {
                    ReadingBookmark readingMark = mPdfDoc.getReadingBookmark(i);
                    if (readingMark.getPageIndex() == pageIndex) {
                        mPdfDoc.removeReadingBookmark(readingMark);
                        break;
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
            notifyDataSetChanged();
        }

        public void updateBookmarkNode(int position, String title, DateTime dateTime) {
            mNodeList.get(position).setTitle(title);
            mNodeList.get(position).setModifiedDateTime(dateTime);
        }

        public void clearAllNodes() {
            if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
            final Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
            if (context == null) return;
            final ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(context.getApplicationContext().getString(R.string.rv_panel_annot_deleting));
            progressDialog.setCancelable(false);
            progressDialog.show();
            RemoveReadingBookmarkTask bookmarkTask = new RemoveReadingBookmarkTask(new IReadingBookmarkCallback() {
                @Override
                public void result() {
                    progressDialog.dismiss();
                    notifyDataSetChanged();
                }
            });
            mPdfViewCtrl.addTask(bookmarkTask);
        }

        public void onPageRemoved(boolean success, int index) {
            if (!success)
                return;
            ArrayList<ReadingBookmarkNode> invalidList = new ArrayList<>();
            for (ReadingBookmarkNode node : mNodeList) {
                if (node.getIndex() == index) {
                    invalidList.add(node);
                } else if (node.getIndex() > index) {
                    node.setIndex(node.getIndex() - 1);
                }
            }
            for (ReadingBookmarkNode node : invalidList) {
                mNodeList.remove(node);
                try {
                    mPdfViewCtrl.getDoc().removeReadingBookmark(node.mBookrmak);
                }catch (PDFException e) {
                    e.printStackTrace();
                }
            }
            invalidList.clear();
            mNeedRelayout = true;
        }

        public void onPageMoved(boolean success, int index, int dstIndex) {
            if (!success)
                return;

            for (int i = 0; i < mNodeList.size(); i++) {
                ReadingBookmarkNode node = mNodeList.get(i);
                if (index < dstIndex) {
                    if (node.getIndex() <= dstIndex && node.getIndex() > index) {
                        node.setIndex(node.getIndex() - 1);
                    }else if (node.getIndex() == index){
                        node.setIndex(dstIndex);
                    }

                } else {
                    if (node.getIndex() >= dstIndex && node.getIndex() < index) {
                        node.setIndex(node.getIndex() + 1);
                    }else if (node.getIndex() == index){
                        node.setIndex(dstIndex);
                    }
                }
            }
        }

        protected void onPagesInsert(boolean success, int dstIndex, int[] range) {
            if(!success)
                return;
            int offsetIndex = 0;
            for (int i = 0; i < range.length / 2; i++) {
                offsetIndex += range[2*i+1];
            }
            updateReadingBookmarkItems(dstIndex,offsetIndex);
        }

        private void updateReadingBookmarkItems(int dstIndex,int offsetIndex){
            for(int i = 0;i<mNodeList.size();i++) {
                ReadingBookmarkNode node = mNodeList.get(i);
                if (node.getIndex() >= dstIndex) {
                    node.setIndex(node.getIndex() + offsetIndex);
                }
            }
        }


        @Override
        public int getCount() {
            return mNodeList.size();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            RBViewHolder bkHolder;
            if (null == convertView) {
                bkHolder = new RBViewHolder();
                convertView = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.rd_readingmark_item, null);

                bkHolder.mRMContent = (TextView) convertView.findViewById(R.id.rd_bookmark_item_content);
                bkHolder.mRMCreateTime = (TextView) convertView.findViewById(R.id.rd_bookmark_item_date);

                bkHolder.mRMMore = (ImageView) convertView.findViewById(R.id.rd_panel_item_more);
                bkHolder.mRMMoreView = (LinearLayout) convertView.findViewById(R.id.rd_bookmark_item_moreView);

                bkHolder.mLlRename = (LinearLayout) convertView.findViewById(R.id.rd_bookmark_item_ll_rename);
                bkHolder.mRMRename = (ImageView) convertView.findViewById(R.id.rd_bookmark_item_rename);
                bkHolder.mRMTvRename = (TextView) convertView.findViewById(R.id.rd_bookmark_item_tv_rename);

                bkHolder.mLlDelete = (LinearLayout) convertView.findViewById(R.id.rd_bookmark_item_ll_delete);
                bkHolder.mRMDelete = (ImageView) convertView.findViewById(R.id.rd_bookmark_item_delete);
                bkHolder.mRMTvDelete = (TextView) convertView.findViewById(R.id.rd_bookmark_item_tv_delete);

                if (AppDisplay.getInstance(mContext).isPad()) {
                    convertView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_2l_pad)));
                    int paddingLeft = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
                    convertView.setPadding(paddingLeft, 0, 0, 0);

                    int paddingRight = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_pad);
                    bkHolder.mRMMore.setPadding(bkHolder.mRMMore.getPaddingLeft(), bkHolder.mRMMore.getPaddingTop(), paddingRight, bkHolder.mRMMore.getPaddingBottom());
                } else {
                    convertView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_2l_phone)));
                    int paddingLeft = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
                    convertView.setPadding(paddingLeft, 0, 0, 0);

                    int paddingRight = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_phone);
                    bkHolder.mRMMore.setPadding(bkHolder.mRMMore.getPaddingLeft(), bkHolder.mRMMore.getPaddingTop(), paddingRight, bkHolder.mRMMore.getPaddingBottom());
                }

                convertView.setTag(bkHolder);
            } else {
                bkHolder = (RBViewHolder) convertView.getTag();
            }

            final ReadingBookmarkNode node = (ReadingBookmarkNode) getItem(position);
            bkHolder.mRMContent.setText(node.getTitle());
            String time = AppDmUtil.dateOriValue;
            if (node.getModifiedDateTime() != null) {
                time = AppDmUtil.getLocalDateString(node.getModifiedDateTime());
            }
            bkHolder.mRMCreateTime.setText(time);

            if (mItemMoreViewShow.get(position)) {
                bkHolder.mRMMoreView.setVisibility(View.VISIBLE);
            } else {
                bkHolder.mRMMoreView.setVisibility(View.GONE);
            }

            boolean canAssemble = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAssemble();
            bkHolder.mRMMore.setEnabled(canAssemble);
            bkHolder.mRMMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBookmarkListener.onMoreClick(position);
                }
            });

            View.OnClickListener renameListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((LinearLayout) (v.getParent()).getParent()).setVisibility(View.GONE);
                    mItemMoreViewShow.set(position, false);
                    mBookmarkListener.onRename(position);
                }
            };
            bkHolder.mRMRename.setOnClickListener(renameListener);
            bkHolder.mRMTvRename.setOnClickListener(renameListener);

            View.OnClickListener deleteListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((LinearLayout) (v.getParent()).getParent()).setVisibility(View.GONE);
                    mItemMoreViewShow.set(position, false);
                    mBookmarkListener.onDelete(position);
                }
            };
            bkHolder.mRMDelete.setOnClickListener(deleteListener);
            bkHolder.mRMTvDelete.setOnClickListener(deleteListener);

            View.OnTouchListener listener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        v.setPressed(true);
                        return true;
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        v.setPressed(false);
                    }
                    return false;
                }
            };
            bkHolder.mLlRename.setOnTouchListener(listener);
            bkHolder.mLlDelete.setOnTouchListener(listener);

            RelativeLayout.LayoutParams paramsMoreView = (RelativeLayout.LayoutParams) bkHolder.mRMMoreView.getLayoutParams();
            paramsMoreView.height = RelativeLayout.LayoutParams.MATCH_PARENT;
            bkHolder.mRMMoreView.setLayoutParams(paramsMoreView);

            return convertView;
        }

        @Override
        public Object getItem(int position) {
            return mNodeList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

    }

}
