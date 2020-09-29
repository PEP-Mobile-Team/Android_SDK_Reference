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
package com.foxit.uiextensions.modules;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.Bookmark;
import com.foxit.sdk.pdf.actions.Destination;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.modules.OutlineModule.OutlineItem;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;

public abstract class OutlineSupport {
    private static final int UPDATEUI = 100;

    public static final int STATE_NORMAL = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_LOAD_FINISH = 2;

    private int mCurrentState = STATE_NORMAL;

    private Context mContext;
    private PDFViewCtrl mPDFViewCtrl;
    private PopupWindow mPanelPopupWindow;
    private MyHandler mHandler;
    private OutlineAdapter mAdapter;
    private OutlineItem mOutlineItem = new OutlineItem();
    private ArrayList<OutlineItem> mOutlineList = new ArrayList<OutlineItem>();
    private ArrayList<OutlineItem> mShowOutlineList = new ArrayList<OutlineItem>();

    private int mLevel = 0;
    private ImageView mBack;
    private ArrayList<OutlineItem> mParents = new ArrayList<OutlineItem>();
    private int mPosition = -1;
    private AppDisplay mDisplay;

    public int getCurrentState() {
        return mCurrentState;
    }

    public OutlineSupport(Context context, PDFViewCtrl pdfViewCtrl, AppDisplay display, PopupWindow popup, ImageView back) {
        mContext = context;
        mDisplay = display;

        mPanelPopupWindow = popup;
        this.mPDFViewCtrl = pdfViewCtrl;
        mBack = back;
        mHandler = new MyHandler();
        mAdapter = new OutlineAdapter();
        mCurrentState = STATE_LOADING;
        updateUI(mLevel, mCurrentState);
        outlineBindingListView(mAdapter);


        mBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLevel = mLevel - 1;
                mShowOutlineList.clear();
                mShowOutlineList.addAll(mParents.get(mPosition).mChildren);
                getShowOutline(mShowOutlineList);
                updateUI(mLevel, STATE_NORMAL);
                mAdapter.notifyDataSetChanged();

                mPosition = mShowOutlineList.get(0).mParentPos;
            }
        });
    }

    protected void init() {
        try {
            mOutlineList.clear();
            mShowOutlineList.clear();
            mParents.clear();
            mOutlineItem.mBookmark = mPDFViewCtrl.getDoc().getRootBookmark();
        } catch (PDFException e) {
            e.printStackTrace();
        }

        init(mOutlineItem.mBookmark, 0, 0);
    }

    interface ITaskResult<T1, T2, T3> {
        void onResult(boolean success, T1 t1, T2 t2, T3 t3);
    }

    class OutlineTask extends Task {
        OutlineItem mOutlineItem;
        Bookmark mBookmark;
        int mIdx;
        int mLevel;
        boolean bSuccess;

        public OutlineTask(Bookmark bookmark, int idx, int level, CallBack callBack) {
            super(callBack);
            mBookmark = bookmark;
            mIdx = idx;
            mLevel = level;
        }

        @Override
        protected void execute() {
            try {
                mOutlineItem = new OutlineItem();
                if (mBookmark == null || mBookmark.isEmpty()) {
                    bSuccess = true;
                    return;
                }
                Bookmark current = mBookmark.getFirstChild();//

                while (current != null && !current.isEmpty()) {
                    OutlineItem childItem = new OutlineItem();

                    boolean hasChild = current.hasChild();
                    childItem.mHaveChild = hasChild;
                    childItem.mParentPos = mIdx;
                    childItem.mTitle = current.getTitle();
                    childItem.mBookmark = current;
                    childItem.mLevel = mLevel;

                    Destination dest = current.getDestination();
                    if (dest != null && !dest.isEmpty()) {
                        childItem.mPageIndex = dest.getPageIndex(mPDFViewCtrl.getDoc());

                        PointF destPt = AppUtil.getDestinationPoint(mPDFViewCtrl, dest);

                        childItem.mX = destPt.x;
                        childItem.mY = destPt.y;
                    }

                    current = current.getNextSibling();
                    mOutlineItem.mChildren.add(childItem);
                }
                mOutlineItem.mLevel = mLevel - 1;
                bSuccess = true;
            } catch (PDFException e) {
                if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                    mPDFViewCtrl.recoverForOOM();
                    return;
                }
                bSuccess = false;
            }
        }
    }

    private void getOutlineInfo(Bookmark bookmark, int idx, int level, final ITaskResult<OutlineItem, Integer, Integer> result) {
        Task.CallBack callBack = new Task.CallBack() {
            @Override
            public void result(Task task) {
                OutlineTask task1 = (OutlineTask) task;
                if (result != null) {
                    result.onResult(task1.bSuccess, task1.mOutlineItem, task1.mIdx, task1.mLevel);
                }
            }
        };

        Task task = new OutlineTask(bookmark, idx, level, callBack);
        mPDFViewCtrl.addTask(task);
    }

    private void init(Bookmark bookmark, int idx, int level) {
        if (bookmark == null) {
            mCurrentState = STATE_LOAD_FINISH;
            Message msg = new Message();
            msg.arg1 = mCurrentState;
            msg.what = UPDATEUI;
            mHandler.sendMessage(msg);
            return;
        }

        getOutlineInfo(bookmark, idx, level, new ITaskResult<OutlineItem, Integer, Integer>() {
            @Override
            public void onResult(boolean success, OutlineItem outlineItem, Integer idx, Integer level) {
                if (success) {
                    if (mOutlineList.size() == 0) {
                        mOutlineList.addAll(outlineItem.mChildren);
                        for (int i = 0; i < outlineItem.mChildren.size(); i++) {
                            mParents.add(outlineItem);
                        }
                        mShowOutlineList.clear();
                        mShowOutlineList.addAll(mOutlineList);
                        mCurrentState = STATE_LOAD_FINISH;
                        getShowOutline(mShowOutlineList);
                        if (mAdapter != null) {
                            Message msg = new Message();
                            msg.arg1 = mCurrentState;
                            msg.what = UPDATEUI;
                            mHandler.sendMessage(msg);
                        }
                        return;
                    }

                    if (idx < 0) {
                        return;
                    }
                    mOutlineList.addAll(idx + 1, outlineItem.mChildren);
                    for (int i = 0; i < outlineItem.mChildren.size(); i++) {
                        mParents.add(idx + 1 + i, outlineItem);
                    }

                    mShowOutlineList.addAll(outlineItem.mChildren);
                    mCurrentState = STATE_NORMAL;
                    getShowOutline(mShowOutlineList);
                    if (mAdapter != null) {
                        Message msg = new Message();
                        msg.arg1 = mCurrentState;
                        msg.what = UPDATEUI;
                        mHandler.sendMessage(msg);
                    }
                }
            }
        });
    }

    public abstract void outlineBindingListView(BaseAdapter adapter);

    public abstract void getShowOutline(ArrayList<OutlineItem> mOutlineList);

    public abstract void updateUI(int level, int state);

    //outline item class
    static class Outline {
        LinearLayout sd_outline_layout_ll;
        TextView tvChapter = null;//chapter
        ImageView ivMore = null;// opened or closed
    }

    private void getOutList(OutlineItem outlineItem, int pos) {
        Bookmark current = null;
        current = outlineItem.mBookmark;
        init(current, pos, mLevel);
    }

    private class OutlineAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mShowOutlineList != null ? mShowOutlineList.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return mShowOutlineList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final Outline outline;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.rv_panel_outline_item, null, false);

                outline = new Outline();
                outline.sd_outline_layout_ll = (LinearLayout) convertView.findViewById(R.id.sd_outline_layout_ll);
                outline.tvChapter = (TextView) convertView.findViewById(R.id.sd_outline_chapter);
                outline.ivMore = (ImageView) convertView.findViewById(R.id.sd_outline_more);

                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) outline.sd_outline_layout_ll.getLayoutParams();
                if (mDisplay.isPad()) {
                    convertView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_pad)));
                    int paddingLeft = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
                    convertView.setPadding(paddingLeft, 0, 0, 0);

                    layoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_pad);
                    int paddingRight = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_pad);
                    outline.ivMore.setPadding(outline.ivMore.getPaddingLeft(), outline.ivMore.getPaddingTop(), paddingRight, outline.ivMore.getPaddingBottom());
                } else {
                    convertView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_phone)));
                    int paddingLeft = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
                    convertView.setPadding(paddingLeft, 0, 0, 0);

                    layoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_1l_phone);
                    int paddingRight = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_phone);
                    outline.ivMore.setPadding(outline.ivMore.getPaddingLeft(), outline.ivMore.getPaddingTop(), paddingRight, outline.ivMore.getPaddingBottom());
                }
                outline.sd_outline_layout_ll.setLayoutParams(layoutParams);

                convertView.setTag(outline);
            } else {
                outline = (Outline) convertView.getTag();
            }

            outline.ivMore.setVisibility(mShowOutlineList.get(position).mHaveChild ? View.VISIBLE : View.INVISIBLE);
            outline.tvChapter.setText(mShowOutlineList.get(position).mTitle);
            outline.ivMore.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    OutlineItem currentNode = mShowOutlineList.get(position);
                    mLevel = currentNode.mLevel + 1;
                    mPosition = mOutlineList.indexOf(currentNode);

                    boolean mIsShowNext = mOutlineList.get(mPosition).mIsExpanded;

                    mOutlineList.get(mPosition).mIsExpanded = !mIsShowNext;
                    mShowOutlineList.clear();
                    mCurrentState = STATE_LOADING;
                    getOutList(currentNode, mPosition);

                }
            });
            convertView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    int index = mShowOutlineList.get(position).mPageIndex;
                    float mX = mShowOutlineList.get(position).mX;
                    float mY = mShowOutlineList.get(position).mY;
                    mPDFViewCtrl.gotoPage(index, new PointF(mX, mY));

                    if (mPanelPopupWindow.isShowing()) {
                        mPanelPopupWindow.dismiss();
                    }
                }
            });
            return convertView;
        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATEUI:
                    updateUI(mLevel, msg.arg1);
                    mAdapter.notifyDataSetChanged();
                    break;
                default:
                    break;
            }
        }
    }
}
