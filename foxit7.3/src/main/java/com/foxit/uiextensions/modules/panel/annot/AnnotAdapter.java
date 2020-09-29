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
package com.foxit.uiextensions.modules.panel.annot;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.annots.multiselect.GroupManager;
import com.foxit.uiextensions.annots.redaction.UIAnnotRedaction;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;


public class AnnotAdapter extends BaseAdapter {
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private List<AnnotNode> mNodeList;
    private List<AnnotNode> mSelectedGroupNodeList;
    private CheckBoxChangeListener mCheckBoxChangeListener;
    private ArrayList<List<AnnotNode>> mPageNodesList;

    private final AppDisplay mDisplay;
    private final ArrayList<Boolean> mItemMoreViewShow;
    private PopupWindow mPopupWindow;
    private boolean mDateChanged = false;
    private ProgressDialog mProgressDialog;
    private String mProgressMsg;

    public interface CheckBoxChangeListener {
        void onChecked(boolean isChecked, AnnotNode node);
    }

    public interface DeleteCallback {
        void result(boolean success, AnnotNode node);
    }

    public void setCheckBoxChangeListener(CheckBoxChangeListener listener) {
        mCheckBoxChangeListener = listener;
    }

    List<AnnotNode> getSelectedGroupNodeList() {
        return mSelectedGroupNodeList;
    }

    protected boolean hasDataChanged() {
        return mDateChanged;
    }

    protected void resetDataChanged() {
        mDateChanged = false;
    }

    public AnnotAdapter(Context context, PDFViewCtrl pdfViewCtrl, ArrayList<Boolean> itemMoreViewShow) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mDisplay = new AppDisplay(mContext);
        mItemMoreViewShow = itemMoreViewShow;
        mNodeList = new ArrayList<AnnotNode>();
        mPageNodesList = new ArrayList<>();
        mSelectedGroupNodeList = new ArrayList<>();
    }

    protected void preparePageNodes() {
        for (int i = 0; i < mPdfViewCtrl.getPageCount(); i++) {
            mPageNodesList.add(new ArrayList<AnnotNode>());
        }
    }

    public void setPopupWindow(PopupWindow popupWindow) {
        mPopupWindow = popupWindow;
    }

    public void establishNodeList() {
        mNodeList.clear();
        for (int i = 0; i < mPageNodesList.size(); i++) {
            List<AnnotNode> nodes = mPageNodesList.get(i);
            if (nodes != null && nodes.size() > 0) {
                int index = mNodeList.size();
                boolean addPageNode = false;
                int count = 0;
                for (AnnotNode an : nodes) {
                    if (an.isRootNode() && !an.isRedundant()) {
                        addPageNode = true;
                        mNodeList.add(an);
                        count++;
                        establishNodeRoot(an);
                    }
                }
                if (addPageNode) {
                    AnnotNode pageNode = new AnnotNode(i);
                    pageNode.counter = count;
                    mNodeList.add(index, pageNode);
                }
            }
        }
        mItemMoreViewShow.clear();
        mSelectedGroupNodeList.clear();
        for (int i = 0; i < mNodeList.size(); i++) {
            mItemMoreViewShow.add(false);
        }
    }

    private void establishNodeRoot(AnnotNode parent) {
        if (parent.isLeafNode()) return;
        for (AnnotNode child : parent.getChildren()) {
            mNodeList.add(child);
            establishNodeRoot(child);
        }
    }

    public void clearNodes() {
        mNodeList.clear();
        mItemMoreViewShow.clear();
        mSelectedGroupNodeList.clear();

        for (int i = 0; i < mPageNodesList.size(); i++) {
            List<AnnotNode> nodes = mPageNodesList.get(i);
            if (nodes != null) {
                nodes.clear();
            }
            mPageNodesList.set(i, null);
        }
        notifyDataSetChanged();
    }

    public void addNode(AnnotNode node) {
        List<AnnotNode> nodes = mPageNodesList.get(node.getPageIndex());
        if (nodes == null) {
            nodes = new ArrayList<AnnotNode>();
            mPageNodesList.set(node.getPageIndex(), nodes);
        }
        if (nodes.contains(node)) return;
        if (node.getReplyTo().equals("") && node.getUID().equals("")) {
            return;
        }
        boolean needFind = !node.getReplyTo().equals("");
        for (AnnotNode an : nodes) {
            if (needFind) {
                if (an.getUID().equals(node.getReplyTo())) {
                    node.setParent(an);
                    an.addChildNode(node);
                    needFind = false;
                    continue;
                }
            }
            if (!an.getReplyTo().equals("") && an.getReplyTo().equals(node.getUID())) {
                an.setParent(node);
                node.addChildNode(an);
            }
        }
        nodes.add(node);
    }

    public void updateNode(Annot annot) {
        try {
            if (!AppAnnotUtil.isSupportEditAnnot(annot) || AppUtil.isEmpty(AppAnnotUtil.getAnnotUniqueID(annot)))
                return;
            List<AnnotNode> nodes = mPageNodesList.get(annot.getPage().getIndex());
            if (nodes == null) return;
            for (AnnotNode node : nodes) {
                if (node.getUID().equals(AppAnnotUtil.getAnnotUniqueID(annot))) {
                    if (annot.isMarkup())
                        node.setAuthor(((Markup) annot).getTitle());
                    node.setContent(annot.getContent());
                    node.setLocked(AppAnnotUtil.isLocked(annot));
                    node.setReadOnlyFlag(AppAnnotUtil.isReadOnly(annot));
                    node.setEditable(!node.isReadOnly());
                    node.setApplyRedaction(!annot.isEmpty() && annot.getType() == Annot.e_Redact);
                    node.setCanReply(AppAnnotUtil.isSupportReply(mPdfViewCtrl, annot));
                    node.setCanModifyContents(AppAnnotUtil.contentsModifiable(mPdfViewCtrl, annot));
                    node.setCanComment(AppAnnotUtil.isSupportComment(mPdfViewCtrl, annot));
                    node.setDeletable(AppAnnotUtil.isSupportDelete(mPdfViewCtrl, annot));
                    String date = AppDmUtil.getLocalDateString(annot.getModifiedDateTime());
                    if (date == null || date.equals(AppDmUtil.dateOriValue)) {
                        if (annot.isMarkup())
                            date = AppDmUtil.getLocalDateString(((Markup) annot).getCreationDateTime());
                    }
                    if (GroupManager.getInstance().isGrouped(mPdfViewCtrl, annot))
                        node.setGroupHeaderNM(GroupManager.getInstance().getHeaderUniqueID(mPdfViewCtrl, annot));
                    node.setModifiedDate(date);
                }
            }
            for (int i = 0; i < mItemMoreViewShow.size(); i++) {
                mItemMoreViewShow.set(i, false);
            }
            mSelectedGroupNodeList.clear();
            notifyDataSetChanged();
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void removeNode(PDFPage page, String uid) {
        if (uid == null || uid.equals("")) return;

        try {
            List<AnnotNode> nodes = mPageNodesList.get(page.getIndex());
            if (nodes == null) return;
            for (int i = nodes.size() - 1; i >= 0; i--) {
                AnnotNode node = nodes.get(i);
                if (node.getUID().equals(uid)) {
                    removeNodeFromPageNode(node, nodes);
                    if (node.getParent() != null) {
                        node.getParent().removeChild(node);
                    } else {
                        node.removeChildren();
                    }
                    break;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void removeNodeFromPageNode(final AnnotNode node, final List<AnnotNode> pageNodes) {
        if (pageNodes == null || node == null || !pageNodes.contains(node))
            return;
        List<AnnotNode> children = node.getChildren();
        if (children != null && children.size() != 0) {
            for (AnnotNode child : children) {
                removeNodeFromPageNode(child, pageNodes);
            }
        }
        pageNodes.remove(node);
    }

    public void applyAllRedactionNode(final List<AnnotNode> redactNodeList, Event.Callback callback) {
        Iterator<AnnotNode> iterator = redactNodeList.iterator();
        List<Annot> redactAnnotList = new ArrayList<>();
        while (iterator.hasNext()) {
            AnnotNode node = iterator.next();
            final List<AnnotNode> nodes = mPageNodesList.get(node.getPageIndex());
            if (nodes == null || !nodes.contains(node)) {
                iterator.remove();
                continue;
            }

            PDFPage page = getAnnotNodePage(node);
            if (page == null || page.isEmpty()) {
                iterator.remove();
                continue;
            }

            Annot annot = AppAnnotUtil.getAnnot(page, node.getUID());
            if (annot == null || annot.isEmpty()) {
                iterator.remove();
                continue;
            }

            redactAnnotList.add(annot);
        }

        UIAnnotRedaction.applyAll(mPdfViewCtrl, redactAnnotList, callback);
    }

    public void applyRedactionNode(final AnnotNode node, final DeleteCallback callback) {
        final List<AnnotNode> nodes = mPageNodesList.get(node.getPageIndex());
        if (nodes == null || !nodes.contains(node)) {
            if (callback != null) callback.result(true, node);
            return;
        }

        PDFPage page = getAnnotNodePage(node);
        if (page == null || page.isEmpty()) {
            if (callback != null) callback.result(true, node);
            return;
        }

        Annot annot = AppAnnotUtil.getAnnot(page, node.getUID());
        if (annot == null || annot.isEmpty()) {
            if (callback != null) {
                //first remove children from page nodes list.
                removeNodeFromPageNode(node, nodes);
                // clear children list.
                node.removeChildren();
                callback.result(true, node);

                establishNodeList();
                notifyDataSetChanged();
            }
            return;
        }

        UIAnnotRedaction.apply(mPdfViewCtrl, annot, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                callback.result(success, node);
            }
        });
    }

    private void flattenNode(AnnotNode node, final Event.Callback callback) {
        final List<AnnotNode> nodes = mPageNodesList.get(node.getPageIndex());
        if (nodes == null || !nodes.contains(node)) {
            if (callback != null) callback.result(null, true);
            return;
        }

        PDFPage page = getAnnotNodePage(node);
        if (page == null || page.isEmpty()) {
            if (callback != null) callback.result(null, false);
            return;
        }

        Annot annot = AppAnnotUtil.getAnnot(page, node.getUID());
        if (annot == null || annot.isEmpty()) {
            if (callback != null) {
                //first remove children from page nodes list.
                removeNodeFromPageNode(node, nodes);
                // clear children list.
                node.removeChildren();
                callback.result(null, true);

                establishNodeList();
                notifyDataSetChanged();
            }
            return;
        }
        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().flattenAnnot(annot, callback);
    }

    public void removeNode(final AnnotNode node, final DeleteCallback callback) {
        final List<AnnotNode> nodes = mPageNodesList.get(node.getPageIndex());
        if (nodes == null || !nodes.contains(node)) {
            if (callback != null) {
                callback.result(true, node);
            }
            return;
        }

        PDFPage page = getAnnotNodePage(node);
        if (page == null || page.isEmpty())
            return;
        Annot annot = AppAnnotUtil.getAnnot(page, node.getUID());

        if (annot == null || annot.isEmpty()) {
            if (callback != null) {
                //first remove children from page nodes list.
                removeNodeFromPageNode(node, nodes);
                // clear children list.
                node.removeChildren();
                callback.result(true, node);

                establishNodeList();
                notifyDataSetChanged();
            }
            return;
        }

        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().removeAnnot(annot, true, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                callback.result(success, node);
            }
        });
    }

    public PDFPage getAnnotNodePage(AnnotNode node) {
        try {
            return mPdfViewCtrl.getDoc().getPage(node.getPageIndex());
        } catch (PDFException e) {
            return null;
        }
    }

    @Override
    public int getCount() {
        return mNodeList.size();
    }

    @Override
    public Object getItem(int position) {
        if (position < 0 || position >= mNodeList.size()) return null;
        return mNodeList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final AnnotNode node = mNodeList.get(position);
        if (node == null) return null;
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = View.inflate(mContext, R.layout.panel_annot_item, null);

            holder.mPageLayout = convertView.findViewById(R.id.rv_panel_annot_item_page_layout);
            holder.mPageIndexTextView = (TextView) holder.mPageLayout.findViewById(R.id.rv_panel_annot_item_page_tv);
            holder.mCounterTextView = (TextView) convertView.findViewById(R.id.rv_panel_annot_item_page_count_tv);

            holder.mRlMain = (RelativeLayout) convertView.findViewById(R.id.rd_panel_rl_main);
            holder.mMainLayout = convertView.findViewById(R.id.rv_panel_annot_item_main_layout);
            holder.mAuthorTextView = (TextView) holder.mMainLayout.findViewById(R.id.rv_panel_annot_item_author_tv);
            holder.mContentsTextView = (TextView) holder.mMainLayout.findViewById(R.id.rv_panel_annot_item_contents_tv);

            holder.mDateTextView = (TextView) holder.mMainLayout.findViewById(R.id.rv_panel_annot_item_date_tv);
            holder.mIconImageView = (ImageView) holder.mMainLayout.findViewById(R.id.rv_panel_annot_item_icon_iv);
            holder.mRedImageView = (ImageView) holder.mMainLayout.findViewById(R.id.rv_panel_annot_item_icon_red);

            holder.mItemMore = (ImageView) holder.mMainLayout.findViewById(R.id.rd_panel_annot_item_more);
            holder.mItemMoreView = (LinearLayout) convertView.findViewById(R.id.rd_annot_item_moreview);

            holder.mItem_ll_reply = (LinearLayout) convertView.findViewById(R.id.rd_annot_item_ll_reply);
            holder.mItem_ll_comment = (LinearLayout) convertView.findViewById(R.id.rd_annot_item_ll_comment);
            holder.mItem_ll_apply = (LinearLayout) convertView.findViewById(R.id.rd_annot_item_ll_redaction);
            holder.mItem_ll_flatten = (LinearLayout) convertView.findViewById(R.id.rd_annot_item_ll_flatten);
            holder.mItem_ll_delete = (LinearLayout) convertView.findViewById(R.id.rd_annot_item_ll_delete);

            LinearLayout.LayoutParams rlMainLayoutParams = (LinearLayout.LayoutParams) holder.mRlMain.getLayoutParams();
            RelativeLayout.LayoutParams mainLayoutParams = (RelativeLayout.LayoutParams) holder.mMainLayout.getLayoutParams();
            LinearLayout.LayoutParams contentLayoutParams = (LinearLayout.LayoutParams) holder.mContentsTextView.getLayoutParams();

            if (mDisplay.isPad()) {
                rlMainLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_2l_pad);

                int marginLeft = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);
                int marginRight = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_pad);

                holder.mPageLayout.setPadding(marginLeft, holder.mPageLayout.getPaddingTop(), marginRight, holder.mPageLayout.getPaddingBottom());
                mainLayoutParams.leftMargin = marginLeft;
                contentLayoutParams.rightMargin = marginRight;

                int paddingRightSmall = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_pad);
                holder.mItemMore.setPadding(holder.mItemMore.getPaddingLeft(), holder.mItemMore.getPaddingTop(), paddingRightSmall, holder.mItemMore.getPaddingBottom());
            } else {
                rlMainLayoutParams.height = (int) mContext.getResources().getDimension(R.dimen.ux_list_item_height_2l_phone);

                int marginLeft = (int) mContext.getResources().getDimension(R.dimen.ux_horz_left_margin_phone);
                int marginRight = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_phone);

                holder.mPageLayout.setPadding(marginLeft, holder.mPageLayout.getPaddingTop(), marginRight, holder.mPageLayout.getPaddingBottom());
                mainLayoutParams.leftMargin = marginLeft;
                contentLayoutParams.rightMargin = marginRight;

                int paddingRightSmall = (int) mContext.getResources().getDimension(R.dimen.ux_horz_right_margin_phone);
                holder.mItemMore.setPadding(holder.mItemMore.getPaddingLeft(), holder.mItemMore.getPaddingTop(), paddingRightSmall, holder.mItemMore.getPaddingBottom());
            }
            holder.mRlMain.setLayoutParams(rlMainLayoutParams);
            holder.mMainLayout.setLayoutParams(mainLayoutParams);
            holder.mContentsTextView.setLayoutParams(contentLayoutParams);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (node.isPageDivider()) {
            holder.mRlMain.setVisibility(View.GONE);
            holder.mItemMoreView.setVisibility(View.GONE);
            holder.mPageLayout.setVisibility(View.VISIBLE);
            holder.mCounterTextView.setText("" + node.counter);
            holder.mPageIndexTextView.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_panel_annot_item_pagenum) + " " + (node.getPageIndex() + 1));
            return convertView;
        } else {
            holder.mRlMain.setVisibility(View.VISIBLE);
            holder.mPageLayout.setVisibility(View.GONE);
        }

        int level = node.getLevel();
        holder.mDateTextView.setText(node.getModifiedDate());
        holder.mAuthorTextView.setText(node.getAuthor());
        holder.mContentsTextView.setText(node.getContent());

        if (node.isRootNode()) {
            int drawable = AppAnnotUtil.getIconId(node.getType());
            holder.mIconImageView.setImageResource(drawable);
        } else {
            holder.mIconImageView.setImageResource(R.drawable.annot_reply_selector);
        }

        holder.mItemMore.setVisibility(View.VISIBLE);
        if (mPdfViewCtrl.getDoc() != null) {
            if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {

                if (node.isCanModifyContents()) {
                    holder.mItemMore.setEnabled(true);
                    holder.mItem_ll_comment.setVisibility(View.VISIBLE);
                    holder.mItem_ll_reply.setVisibility(View.GONE);
                    holder.mItem_ll_flatten.setVisibility(View.GONE);
                    holder.mItem_ll_apply.setVisibility(View.GONE);
                    holder.mItem_ll_delete.setVisibility(View.GONE);

                    holder.mItem_ll_comment.setTag(position);
                    View.OnClickListener commentListener = new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (AppUtil.isFastDoubleClick()) return;

                            ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                            int tag = (Integer) v.getTag();
                            mItemMoreViewShow.set(tag, false);
                            if (mSelectedGroupNodeList.size() > 0) {
                                mSelectedGroupNodeList.clear();
                                notifyDataSetChanged();
                            }

                            UIAnnotReply.replyToAnnot(mPdfViewCtrl, ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), false, UIAnnotReply.TITLE_EDIT_ID, new UIAnnotReply.ReplyCallback() {

                                @Override
                                public void result(final String contents) {
                                }

                                @Override
                                public String getContent() {
                                    return (String) node.getContent();
                                }
                            });
                        }
                    };
                    holder.mItem_ll_comment.setOnClickListener(commentListener);
                } else {
                    holder.mItemMore.setEnabled(false);
                }

            } else {
                if (node.isCanModifyContents()) {
                    if (node.canReply()) {
                        holder.mItem_ll_reply.setVisibility(View.VISIBLE);
                        holder.mItem_ll_reply.setTag(position);
                        View.OnClickListener replyListener = new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                if (AppUtil.isFastDoubleClick()) return;

                                ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                                int tag = (Integer) v.getTag();
                                mItemMoreViewShow.set(tag, false);
                                if (mSelectedGroupNodeList.size() > 0) {
                                    mSelectedGroupNodeList.clear();
                                    notifyDataSetChanged();
                                }

                                UIAnnotReply.replyToAnnot(mPdfViewCtrl, ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), true, UIAnnotReply.TITLE_COMMENT_ID, new UIAnnotReply.ReplyCallback() {

                                    @Override
                                    public void result(final String contents) {
                                        PDFPage page = getAnnotNodePage(node);
                                        if (page == null) return;
                                        Annot annot = AppAnnotUtil.getAnnot(page, node.getUID());
                                        if (annot == null) return;
                                        try {
                                            if (GroupManager.getInstance().isGrouped(mPdfViewCtrl, annot)) {
                                                annot = AppAnnotUtil.createAnnot(((Markup) annot).getGroupHeader());
                                            }
                                            if (annot == null) return;
                                            ;
                                        } catch (PDFException e) {
                                            e.printStackTrace();
                                        }
                                        final String uid = AppDmUtil.randomUUID(null);
                                        UIAnnotReply.addReplyAnnot(mPdfViewCtrl, annot, page, uid, contents, null);
                                    }

                                    @Override
                                    public String getContent() {
                                        return null;
                                    }
                                });
                            }
                        };
                        holder.mItem_ll_reply.setOnClickListener(replyListener);
                    } else {
                        holder.mItem_ll_reply.setVisibility(View.GONE);
                    }

                    if (node.canComment()) {
                        holder.mItem_ll_comment.setVisibility(View.VISIBLE);
                        holder.mItem_ll_comment.setTag(position);
                        View.OnClickListener commentListener = new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                if (AppUtil.isFastDoubleClick()) return;
                                ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                                int tag = (Integer) v.getTag();
                                mItemMoreViewShow.set(tag, false);
                                if (mSelectedGroupNodeList.size() > 0) {
                                    mSelectedGroupNodeList.clear();
                                    notifyDataSetChanged();
                                }

                                UIAnnotReply.replyToAnnot(mPdfViewCtrl, ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), true, UIAnnotReply.TITLE_EDIT_ID, new UIAnnotReply.ReplyCallback() {
                                    @Override
                                    public void result(final String content) {
                                        PDFPage page = getAnnotNodePage(node);
                                        if (page == null) return;
                                        final Annot annot = AppAnnotUtil.getAnnot(page, node.getUID());
                                        if (!AppAnnotUtil.isSupportEditAnnot(annot))
                                            return;

                                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().modifyAnnot(annot, new UIAnnotReply.CommentContent(annot, content), true, new Event.Callback() {
                                            @Override
                                            public void result(Event event, boolean success) {
                                                try {
                                                    if (success) {
                                                        node.setContent(content);
                                                        node.setLocked(AppAnnotUtil.isLocked(annot));
                                                        node.setReadOnlyFlag(AppAnnotUtil.isReadOnly(annot));
                                                        node.setEditable(!node.isReadOnly());
                                                        node.setApplyRedaction(!annot.isEmpty() && annot.getType() == Annot.e_Redact);
                                                        node.setCanReply(AppAnnotUtil.isSupportReply(mPdfViewCtrl, annot));
                                                        node.setCanModifyContents(AppAnnotUtil.contentsModifiable(mPdfViewCtrl, annot));
                                                        node.setCanComment(AppAnnotUtil.isSupportComment(mPdfViewCtrl, annot));
                                                        node.setDeletable(AppAnnotUtil.isSupportDelete(mPdfViewCtrl, annot));
                                                        try {
                                                            node.setModifiedDate(AppDmUtil.getLocalDateString(annot.getModifiedDateTime()));
                                                        } catch (PDFException e) {
                                                            e.printStackTrace();
                                                        }
                                                        notifyDataSetChanged();
                                                    }
                                                } catch (PDFException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public String getContent() {
                                        return node.getContent().toString();
                                    }
                                });
                            }
                        };
                        holder.mItem_ll_comment.setOnClickListener(commentListener);
                    } else {
                        holder.mItem_ll_comment.setVisibility(View.GONE);
                    }

                } else {
                    holder.mItem_ll_reply.setVisibility(View.GONE);
                    holder.mItem_ll_comment.setVisibility(View.GONE);
                }

                if (node.canDelete()) {
                    holder.mItem_ll_delete.setVisibility(View.VISIBLE);
                    holder.mItem_ll_delete.setTag(position);
                    View.OnClickListener deleteListener = new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (AppUtil.isFastDoubleClick()) return;
                            ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                            int tag = (Integer) v.getTag();
                            mItemMoreViewShow.set(tag, false);
                            if (mSelectedGroupNodeList.size() > 0) {
                                mSelectedGroupNodeList.clear();
                                notifyDataSetChanged();
                            }

                            mProgressMsg = mContext.getApplicationContext().getString(R.string.rv_panel_annot_deleting);
                            showProgressDlg();
                            removeNode(node, new DeleteCallback() {
                                @Override
                                public void result(boolean success, AnnotNode node) {
                                    if (success) {
                                        notifyDataSetChanged();
                                    }
                                    dismissProgressDlg();
                                }
                            });
                        }
                    };
                    holder.mItem_ll_delete.setOnClickListener(deleteListener);
                } else {
                    holder.mItem_ll_delete.setVisibility(View.GONE);
                }

                if (AppAnnotUtil.hasModuleLicenseRight(Constants.e_ModuleNameRedaction)
                        && !((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isSign()
                        && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canModifyContents()
                        && node.canApplyRedaction()) {

                    if (level > 0) {
                        holder.mItem_ll_apply.setVisibility(View.GONE);
                    } else {
                        holder.mItem_ll_apply.setVisibility(View.VISIBLE);
                        holder.mItem_ll_apply.setTag(position);
                        View.OnClickListener applyListener = new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (AppUtil.isFastDoubleClick()) return;
                                ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                                int tag = (Integer) v.getTag();
                                mItemMoreViewShow.set(tag, false);
                                if (mSelectedGroupNodeList.size() > 0) {
                                    mSelectedGroupNodeList.clear();
                                    notifyDataSetChanged();
                                }

                                applyRedactionNode(node, new DeleteCallback() {
                                    @Override
                                    public void result(boolean success, AnnotNode node) {
                                        if (success) {
                                            notifyDataSetChanged();
                                        }
                                    }
                                });
                            }
                        };
                        holder.mItem_ll_apply.setOnClickListener(applyListener);
                    }
                } else {
                    holder.mItem_ll_apply.setVisibility(View.GONE);
                }

                if (level > 0) {
                    holder.mItem_ll_flatten.setVisibility(View.GONE);
                } else {
                    holder.mItem_ll_flatten.setVisibility(View.VISIBLE);
                    holder.mItem_ll_flatten.setTag(position);
                    View.OnClickListener flattenListener = new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (AppUtil.isFastDoubleClick()) return;
                            ((LinearLayout) v.getParent()).setVisibility(View.GONE);
                            int tag = (Integer) v.getTag();
                            mItemMoreViewShow.set(tag, false);
                            if (mSelectedGroupNodeList.size() > 0) {
                                mSelectedGroupNodeList.clear();
                                notifyDataSetChanged();
                            }

                            flattenNode(node, new Event.Callback() {
                                @Override
                                public void result(Event event, boolean success) {
                                    if (success) {
                                        notifyDataSetChanged();
                                    }
                                }
                            });
                        }
                    };
                    holder.mItem_ll_flatten.setOnClickListener(flattenListener);
                }

                boolean enabled = View.VISIBLE == holder.mItem_ll_reply.getVisibility() ||
                        View.VISIBLE == holder.mItem_ll_comment.getVisibility() ||
                        View.VISIBLE == holder.mItem_ll_apply.getVisibility() ||
                        View.VISIBLE == holder.mItem_ll_flatten.getVisibility() ||
                        View.VISIBLE == holder.mItem_ll_delete.getVisibility();
                holder.mItemMore.setEnabled(enabled);
            }

            holder.mContentsTextView.setBackgroundResource(R.color.ux_color_translucent);
            if (AppAnnotUtil.contentsModifiable(node.getType())) {
                holder.mContentsTextView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (AppUtil.isFastDoubleClick()) return;

                        boolean show = false;
                        for (int i = 0; i < mNodeList.size(); i++) {
                            if (mItemMoreViewShow.get(i)) {
                                show = true;
                                break;
                            }
                        }

                        if (show) {
                            for (int i = 0; i < mItemMoreViewShow.size(); i++) {
                                mItemMoreViewShow.set(i, false);
                            }
                            mSelectedGroupNodeList.clear();
                            notifyDataSetChanged();
                        } else {
                            PDFPage page = getAnnotNodePage(node);
                            if (page == null) return;
                            final Annot annot = AppAnnotUtil.getAnnot(page, node.getUID());
                            if (annot == null || annot.isEmpty()) return;

                            if (AppAnnotUtil.isAnnotSupportReply(annot) && !node.isReadOnly()) {
                                boolean canEdit = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot() &&
                                        !(node.isReadOnly() || node.isLocked());
                                UIAnnotReply.replyToAnnot(mPdfViewCtrl, ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), canEdit, UIAnnotReply.TITLE_EDIT_ID, new UIAnnotReply.ReplyCallback() {
                                    @Override
                                    public void result(final String content) {
                                        if (!AppAnnotUtil.isSupportEditAnnot(annot))
                                            return;
                                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().modifyAnnot(annot, new UIAnnotReply.CommentContent(annot, content), true, new Event.Callback() {
                                            @Override
                                            public void result(Event event, boolean success) {
                                                try {
                                                    if (success) {
                                                        node.setContent(content);
                                                        node.setLocked(AppAnnotUtil.isLocked(annot));
                                                        node.setReadOnlyFlag(AppAnnotUtil.isReadOnly(annot));
                                                        node.setEditable(!node.isReadOnly());
                                                        node.setApplyRedaction(!annot.isEmpty() && annot.getType() == Annot.e_Redact);
                                                        node.setCanReply(AppAnnotUtil.isSupportReply(mPdfViewCtrl, annot));
                                                        node.setCanModifyContents(AppAnnotUtil.contentsModifiable(mPdfViewCtrl, annot));
                                                        node.setCanComment(AppAnnotUtil.isSupportComment(mPdfViewCtrl, annot));
                                                        node.setDeletable(AppAnnotUtil.isSupportDelete(mPdfViewCtrl, annot));
                                                        try {
                                                            node.setModifiedDate(AppDmUtil.getLocalDateString(annot.getModifiedDateTime()));
                                                        } catch (PDFException e) {
                                                            e.printStackTrace();
                                                        }
                                                        notifyDataSetChanged();
                                                    }
                                                } catch (PDFException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public String getContent() {
                                        return node.getContent().toString();
                                    }
                                });
                            } else {
                                if (!node.isPageDivider() && node.isRootNode()) {
                                    if (!AppUtil.isEmpty(node.getUID())) {
                                        mPdfViewCtrl.gotoPage(node.getPageIndex(), 0, 0);
                                        if (mPopupWindow != null && mPopupWindow.isShowing()) {
                                            mPopupWindow.dismiss();
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
                holder.mContentsTextView.setClickable(true);
            } else {
                holder.mContentsTextView.setClickable(false);
            }
        }

        //noinspection deprecation
        holder.mContentsTextView.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_body2_dark));
        holder.mRedImageView.setVisibility(View.GONE);

        int dx = mDisplay.dp2px(32 + 5);
        if (level > 0) {
            level = Math.min(level, 2);
            holder.mMainLayout.setPadding(dx * level, 0, 0, 0);
        } else {
            holder.mMainLayout.setPadding(0, 0, 0, 0);
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.mItemMoreView.getLayoutParams();
        params.height = RelativeLayout.LayoutParams.MATCH_PARENT;
        holder.mItemMoreView.setLayoutParams(params);

        holder.mItemMore.setTag(position);
        holder.mItemMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int tag = (Integer) v.getTag();
                for (int i = 0; i < mItemMoreViewShow.size(); i++) {
                    if (i == tag) {
                        mItemMoreViewShow.set(i, true);
                    } else {
                        mItemMoreViewShow.set(i, false);
                    }
                }
                mSelectedGroupNodeList.clear();
                if (!AppUtil.isEmpty(node.getGroupHeaderNM())) {
                    List<AnnotNode> nodes = mPageNodesList.get(node.getPageIndex());
                    if (nodes != null) {
                        for (AnnotNode annotNode : nodes) {
                            if (node.getGroupHeaderNM().equals(annotNode.getGroupHeaderNM())) {
                                mSelectedGroupNodeList.add(annotNode);
                            }
                        }
                    }
                }
                notifyDataSetChanged();
            }
        });
        if (mItemMoreViewShow.get(position)) {
            holder.mItemMoreView.setVisibility(View.VISIBLE);
        } else {
            holder.mItemMoreView.setVisibility(View.GONE);
        }

        int color = mSelectedGroupNodeList.contains(node) ? R.color.ux_bg_list_item_color : R.color.ux_color_translucent;
        holder.mRlMain.setBackgroundColor(mContext.getResources().getColor(color));
        return convertView;
    }

    private static final class ViewHolder {
        public RelativeLayout mRlMain;
        public View mMainLayout;
        public ImageView mIconImageView;
        public ImageView mRedImageView;
        public TextView mContentsTextView;
        public TextView mAuthorTextView;
        public TextView mDateTextView;

        public View mPageLayout;
        public TextView mPageIndexTextView;
        public TextView mCounterTextView;

        public ImageView mItemMore;
        public LinearLayout mItemMoreView;

        public LinearLayout mItem_ll_reply;
        public LinearLayout mItem_ll_comment;
        public LinearLayout mItem_ll_apply;
        public LinearLayout mItem_ll_flatten;
        public LinearLayout mItem_ll_delete;
    }

    public boolean selectAll() {
        int countAnnots = getAnnotCount();
        int selectedAnnots = getSelectedCount();
        if (selectedAnnots == countAnnots) {
            for (int i = 0; i < mPageNodesList.size(); i++) {
                List<AnnotNode> nodes = mPageNodesList.get(i);
                if (nodes != null) {
                    for (AnnotNode node : nodes) {
                        if (node.isRedundant()) continue;
                        if (node.isChecked()) {
                            node.setChecked(false);
                            if (mCheckBoxChangeListener != null) {
                                mCheckBoxChangeListener.onChecked(false, node);
                            }
                        }
                    }
                }
            }
            return false;
        }

        for (int i = 0; i < mPageNodesList.size(); i++) {
            List<AnnotNode> nodes = mPageNodesList.get(i);
            if (nodes != null) {
                for (AnnotNode node : nodes) {
                    if (node.isRedundant()) continue;
                    if (!node.isChecked()) {
                        node.setChecked(true);
                        if (mCheckBoxChangeListener != null) {
                            mCheckBoxChangeListener.onChecked(true, node);
                        }
                    }
                }
            }
        }
        return true;
    }

    protected void onPageRemoved(boolean success, int index) {
        if (!success)
            return;
        try {
            mPageNodesList.remove(index);
            updateNodePageIndex(index, mPageNodesList.size());
            establishNodeList();
            mDateChanged = true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected void updateNodePageIndex(int start, int end) {
        for (int i = start; i < end; i++) {
            List<AnnotNode> nodes = mPageNodesList.get(i);
            if (nodes != null) {
                for (AnnotNode node : nodes) {
                    node.setPageIndex(i);
                }
            }
        }
    }

    protected void onPageMoved(boolean success, int index, int dstIndex) {
        if (!success) return;
        if (index < dstIndex) {
            for (int i = index; i < dstIndex; i++) {
                Collections.swap(mPageNodesList, i, i + 1);
            }
        } else {
            for (int i = index; i > dstIndex; i--) {
                Collections.swap(mPageNodesList, i, i - 1);
            }
        }
        updateNodePageIndex(Math.min(index, dstIndex), Math.max(index, dstIndex) + 1);
        establishNodeList();
        mDateChanged = true;
    }

    protected void onPagesInsert(boolean success, int dstIndex, int[] range) {
        if (success) {
            for (int i = 0; i < range.length / 2; i++) {
                for (int index = 0; index < range[2 * i + 1]; index++) {
                    mPageNodesList.add(dstIndex, null);
                    dstIndex++;
                }
            }
            updateNodePageIndex(dstIndex, mPageNodesList.size());
            establishNodeList();
            mDateChanged = true;
        }
    }

    private int getSelectedCount() {
        int count = 0;
        for (int i = 0; i < mPageNodesList.size(); i++) {
            List<AnnotNode> nodes = mPageNodesList.get(i);
            if (nodes != null) {
                for (AnnotNode node : nodes) {
                    if (node.isChecked()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void showProgressDlg() {
        if (mProgressDialog == null) {
            Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(false);
        }

        if (mProgressDialog != null && !mProgressDialog.isShowing()) {
            mProgressDialog.setMessage(mProgressMsg);
            AppDialogManager.getInstance().showAllowManager(mProgressDialog, null);
        }
    }

    private void dismissProgressDlg() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            AppDialogManager.getInstance().dismiss(mProgressDialog);
            mProgressDialog = null;
        }
    }

    public int getAnnotCount() {
        int count = 0;
        for (int i = 0; i < mPageNodesList.size(); i++) {
            List<AnnotNode> nodes = mPageNodesList.get(i);
            if (nodes != null) {
                for (AnnotNode node : nodes) {
                    if (!node.isRedundant()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public AnnotNode getAnnotNode(PDFPage page, String uid) {
        if (uid == null || uid.equals("")) return null;

        try {
            List<AnnotNode> nodes = mPageNodesList.get(page.getIndex());
            if (nodes == null) return null;
            for (AnnotNode node : nodes) {
                if (node.getUID().equals(uid)) {
                    return node;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean canEdit(@NonNull AnnotNode node) {
        if (node.isRootNode()) {
            return node.canEdit();
        } else {
            return node.canEdit() && canEdit(node.getParent());
        }
    }

    public boolean canDelete(@NonNull AnnotNode node) {
        if (node.isRootNode()) {
            return node.canDelete();
        } else {
            return node.canDelete() && canDelete(node.getParent());
        }
    }

    public boolean canComments(@NonNull AnnotNode node) {
        if (node.isRootNode()) {
            return node.canComment();
        } else {
            return node.canComment() && canComments(node.getParent());
        }
    }

    public boolean canReply(@NonNull AnnotNode node) {
        if (node.isRootNode()) {
            return node.canReply();
        } else {
            return node.canReply() && canReply(node.getParent());
        }
    }

}
