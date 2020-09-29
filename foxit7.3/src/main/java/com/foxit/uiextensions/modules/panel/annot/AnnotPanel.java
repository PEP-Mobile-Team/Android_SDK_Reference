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

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.MarkupArray;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotEventListener;
import com.foxit.uiextensions.annots.IFlattenEventListener;
import com.foxit.uiextensions.annots.IGroupEventListener;
import com.foxit.uiextensions.annots.IImportAnnotsEventListener;
import com.foxit.uiextensions.annots.IRedactionEventListener;
import com.foxit.uiextensions.annots.multiselect.GroupManager;
import com.foxit.uiextensions.controls.dialog.FxProgressDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.UIToast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class AnnotPanel implements View.OnClickListener, AnnotAdapter.CheckBoxChangeListener {

    public static final int STATUS_LOADING = 1;
    public static final int STATUS_CANCEL = 2;
    public static final int STATUS_PAUSED = 3;
    public static final int STATUS_DONE = 4;
    public static final int STATUS_FAILED = 5;
    public static final int STATUS_DELETING = 6;
    public static final int STATUS_REDACTING = 7;

    private static final int DELETE_CAN = 0;
    private static final int DELETE_SRCAN = 1;
    private static final int DELETE_UNCAN = 2;

    private final PDFViewCtrl mPdfViewCtrl;
    private final Context mContext;
    private final AnnotAdapter mAdapter;
    private final List<AnnotNode> mCheckedNodes;
    private final List<AnnotNode> mDeleteTemps;
    private final List<AnnotNode> mRedactTemps;

    private View mMainLayout;
    private TextView mChangedTextView;

    private int mLoadedState;
    private int mLoadedIndex;

    private AnnotPanelModule mPanel;
    private FxProgressDialog mDeleteDialog;
    private UITextEditDialog mSRDeleteDialog;
    private Handler mHandle;

    AnnotPanel(AnnotPanelModule panelModule, Context context, PDFViewCtrl pdfViewCtrl, View layout, ArrayList<Boolean> itemMoreViewShow) {
        mPanel = panelModule;
        mPdfViewCtrl = pdfViewCtrl;
        mContext = context;
        mMainLayout = layout;
        mCheckedNodes = new ArrayList<AnnotNode>();
        mDeleteTemps = new ArrayList<AnnotNode>();
        mRedactTemps = new ArrayList<>();
        mAdapter = new AnnotAdapter(layout.getContext(), mPdfViewCtrl, itemMoreViewShow);
        mAdapter.setPopupWindow(mPanel.getPopupWindow());
        mAdapter.setCheckBoxChangeListener(this);
        init();
        mHandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        if (mAdapter != null) {
//                            mAdapter.updateAnnotationListFromCheck();
                        }
                        break;
                    case 2://onAnnotAdded()
                        mAdapter.notifyDataSetChanged();
                        mPanel.hideNoAnnotsView();
                        break;
                    case 3://onAnnotModified()
                        Annot annot = (Annot) msg.obj;
                        mAdapter.updateNode(annot);
                        mAdapter.notifyDataSetChanged();
                        break;
                    case 4://onAnnotDeleted()
                        mAdapter.notifyDataSetChanged();
                        mPanel.resetApplyStatus();
                        if (mLoadedState == STATUS_DONE && mAdapter.getCount() == 0) {
                            mPanel.showNoAnnotsView();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    int getRedactCount() {
        return mRedactTemps.size();
    }

    private void prepareDeleteNodeList() {
        mDeleteTemps.clear();
        for (AnnotNode node : mCheckedNodes) {
            if (node.getParent() != null && mCheckedNodes.contains(node.getParent())) continue;
            if (node.canDelete()) {
                mDeleteTemps.add(node);
            }
        }
    }

    void clearAllNodes() {
        if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
        final Context context = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return;
        if (mDeleteDialog == null) {
            mDeleteDialog = new FxProgressDialog(context, context.getApplicationContext().getString(R.string.rv_panel_annot_deleting));
        }
        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
        if (mAdapter.selectAll()) {
            resetDeleteDialog();
            Collections.sort(mCheckedNodes);
            if (checkDeleteStatus() == DELETE_SRCAN) {
                if (mSRDeleteDialog == null || mSRDeleteDialog.getDialog().getOwnerActivity() == null) {
                    mSRDeleteDialog = new UITextEditDialog(context);
                    mSRDeleteDialog.getPromptTextView().setText(context.getApplicationContext().getString(R.string.rv_panel_annot_delete_tips));
                    mSRDeleteDialog.setTitle(context.getApplicationContext().getString(R.string.cloud_delete_tv));
                    mSRDeleteDialog.getInputEditText().setVisibility(View.GONE);
                }
                mSRDeleteDialog.getOKButton().setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mSRDeleteDialog.dismiss();
                        mDeleteDialog.show();
                        prepareDeleteNodeList();
                        deleteItems();
                    }
                });
                mSRDeleteDialog.show();
            } else if (checkDeleteStatus() == DELETE_UNCAN) {
                UIToast.getInstance(mContext).show(context.getApplicationContext().getString(R.string.rv_panel_annot_failed));
            } else {
                mDeleteDialog.show();
                prepareDeleteNodeList();
                deleteItems();
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    private boolean isPageLoaded(PDFPage page) {
        if (page == null) return false;
        try {
            int pageIndex = page.getIndex();
            return pageIndex < mLoadedIndex || mLoadedState == STATUS_DONE;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }


    private boolean mPause;

    public void setStatusPause(boolean status) {
        mPause = status;
    }

    public AnnotEventListener getAnnotEventListener() {
        return mAnnotEventListener;
    }

    private AnnotEventListener mAnnotEventListener = new AnnotEventListener() {
        @Override
        public void onAnnotAdded(PDFPage page, Annot annot) {
            //After adding an annotation, add a node that corresponds to the annotation in the Annotation list.
            if (page == null || annot == null || AppAnnotUtil.isSupportGroupElement(annot)) return;
            try {
                if (!AppAnnotUtil.isSupportEditAnnot(annot) || (annot.getFlags() & Annot.e_FlagHidden) != 0 || !isPageLoaded(page))
                    return;
                AnnotNode node = mAdapter.getAnnotNode(page, AppAnnotUtil.getAnnotUniqueID(annot));
                if (node == null) {
                    Annot replyToAnnot = AppAnnotUtil.getReplyToAnnot(annot);
                    boolean isLocked = AppAnnotUtil.isLocked(annot);
                    boolean isReadOnly = AppAnnotUtil.isReadOnly(annot);
                    node = new AnnotNode(page.getIndex(), AppAnnotUtil.getAnnotUniqueID(annot), AppAnnotUtil.getAnnotUniqueID(replyToAnnot));
                    if (annot.isMarkup())
                        node.setAuthor(((Markup) annot).getTitle());
                    node.setType(AppAnnotUtil.getTypeString(annot));
                    node.setContent(annot.getContent());
                    node.setLocked(isLocked);
                    String modifiedDate = AppDmUtil.getLocalDateString(annot.getModifiedDateTime());
                    String createdDate = AppDmUtil.dateOriValue;
                    if (annot.isMarkup())
                        createdDate = AppDmUtil.getLocalDateString(((Markup) annot).getCreationDateTime());
                    if (modifiedDate == null || modifiedDate.equals(AppDmUtil.dateOriValue)) {
                        modifiedDate = createdDate;
                    }
                    node.setModifiedDate(modifiedDate);
                    node.setCreationDate(createdDate);
                    node.setReadOnlyFlag(isReadOnly);
                    node.setEditable(!isReadOnly);
                    node.setApplyRedaction(!annot.isEmpty() && annot.getType() == Annot.e_Redact);
                    node.setDeletable(AppAnnotUtil.isSupportDelete(mPdfViewCtrl, annot));
                    node.setCanReply(AppAnnotUtil.isSupportReply(mPdfViewCtrl, annot));
                    node.setCanModifyContents(AppAnnotUtil.contentsModifiable(mPdfViewCtrl, annot));
                    node.setCanComment(AppAnnotUtil.isSupportComment(mPdfViewCtrl, annot));
                    if (annot.isMarkup())
                        node.setIntent(((Markup) annot).getIntent());
                    mAdapter.addNode(node);
                    if (node.canApplyRedaction()) {
                        mRedactTemps.add(node);
                    }
                }
            } catch (PDFException e) {
                e.printStackTrace();
                return;
            }
            mAdapter.establishNodeList();

            mHandle.sendEmptyMessage(2);
        }

        @Override
        public void onAnnotWillDelete(PDFPage page, Annot annot) {
            removeNode(page, annot);
        }

        @Override
        public void onAnnotDeleted(PDFPage page, Annot annot) {
            //Refresh the page after the delete
            mAdapter.establishNodeList();
            mHandle.sendEmptyMessage(4);
        }

        @Override
        public void onAnnotModified(PDFPage page, Annot annot) {
            //After modifying an annotation, modify information of the node that corresponds to the annotation in the Annotation list.
            if (page == null || annot == null || AppAnnotUtil.isSupportGroupElement(annot)) return;
            try {
                if (!AppAnnotUtil.isSupportEditAnnot(annot) || (annot.getFlags() & Annot.e_FlagHidden) != 0 || !isPageLoaded(page))
                    return;

                Message message = Message.obtain();
                message.what = 3;
                message.obj = annot;
                mHandle.sendMessage(message);

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
            removeNode(page, annot);
        }

        @Override
        public void onAnnotFlattened(PDFPage page, Annot annot) {
            mAdapter.establishNodeList();
            mHandle.sendEmptyMessage(4);
        }
    };

    private IGroupEventListener mGroupEventListener = new IGroupEventListener() {

        @Override
        public void onAnnotGrouped(PDFPage page, List<Annot> groupAnnots) {
            updateAnnotNode(page, groupAnnots);
        }

        @Override
        public void onAnnotUnGrouped(PDFPage page, List<Annot> unGroupAnnots) {
            updateAnnotNode(page, unGroupAnnots);
        }

        private void updateAnnotNode(PDFPage page, List<Annot> groupAnnots) {
            for (int i = 0; i < groupAnnots.size(); i++) {
                Annot annot = groupAnnots.get(i);
                try {
                    if (!AppAnnotUtil.isSupportEditAnnot(annot) || (annot.getFlags() & Annot.e_FlagHidden) != 0 || !isPageLoaded(page))
                        return;

                    Message message = Message.obtain();
                    message.what = 3;
                    message.obj = annot;
                    mHandle.sendMessage(message);
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        }

    };

    private IRedactionEventListener mRedactionEventListener = new IRedactionEventListener() {
        @Override
        public void onAnnotWillApply(PDFPage page, Annot annot) {
        }

        @Override
        public void onAnnotApplied(PDFPage page, Annot annot) {
            //reloaded the annot list
            prepare();
            startSearch(0);
        }
    };

    IFlattenEventListener getFlattenEventListener() {
        return mFlattenEventListener;
    }

    IGroupEventListener getGroupEventListener() {
        return mGroupEventListener;
    }

    IRedactionEventListener getRedactionEventListener() {
        return mRedactionEventListener;
    }

    private IImportAnnotsEventListener mImportAnnotsListener = new IImportAnnotsEventListener() {
        @Override
        public void onAnnotsImported() {
            //reloaded the annot list
            prepare();
            startSearch(0);
        }
    };

    IImportAnnotsEventListener getImportAnnotsListener() {
        return mImportAnnotsListener;
    }

    private void removeNode(PDFPage page, Annot annot) {
        //After removing an annotation, remove the node that corresponds to the annotation in the Annotation list.
        if (page == null || annot == null || AppAnnotUtil.isSupportGroupElement(annot)) return;
        try {
            if (!AppAnnotUtil.isSupportEditAnnot(annot) || (annot.getFlags() & Annot.e_FlagHidden) != 0 || !isPageLoaded(page))
                return;
            for (int i = mCheckedNodes.size() - 1; i > -1; i--) {
                AnnotNode node = mCheckedNodes.get(i);
                if (node.getUID().equals(AppAnnotUtil.getAnnotUniqueID(annot))) {
                    node.setChecked(false);
                    onChecked(false, node);
                }
                AnnotNode parent = node.getParent();
                while (parent != null) {
                    if (parent.getUID().equals(AppAnnotUtil.getAnnotUniqueID(annot))) {
                        node.setChecked(false);
                        onChecked(false, node);
                        break;
                    }
                    parent = parent.getParent();
                }
            }
            if (annot.getType() == Annot.e_Redact) {
                for (AnnotNode node : mRedactTemps) {
                    if (node.getUID().equals(AppAnnotUtil.getAnnotUniqueID(annot))) {
                        mRedactTemps.remove(node);
                        break;
                    }
                }
            }

            UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
            if (GroupManager.getInstance().isGrouped(mPdfViewCtrl, annot)) {
                MarkupArray markupArray = ((Markup) annot).getGroupElements();
                long markupSize = markupArray.getSize();
                if (markupSize > 0) {
                    ArrayList<Annot> groupAnnots = new ArrayList<>();
                    boolean canModifyContent = true;
                    boolean canReply = true;
                    boolean canComment = true;
                    boolean canDelete = true;

                    for (long i = 0; i < markupSize; i++) {
                        Annot groupAnnot = AppAnnotUtil.createAnnot(markupArray.getAt(i));
                        if (groupAnnot == null)
                            continue;
                        String annotNM = AppAnnotUtil.getAnnotUniqueID(groupAnnot);
                        if (!AppUtil.isEmpty(annotNM) && annotNM.equals(AppAnnotUtil.getAnnotUniqueID(annot)))
                            continue;

                        if (uiExtensionsManager.isLoadAnnotModule(groupAnnot)) {
                            if (canModifyContent)
                                canModifyContent = AppAnnotUtil.contentsModifiable(AppAnnotUtil.getTypeString(groupAnnot));
                            if (canReply)
                                canReply = AppAnnotUtil.isAnnotSupportReply(groupAnnot) && !AppAnnotUtil.isReadOnly(groupAnnot);
                            if (canComment)
                                canComment = !(AppAnnotUtil.isLocked(groupAnnot) || AppAnnotUtil.isReadOnly(groupAnnot));
                            if (canDelete)
                                canDelete = AppAnnotUtil.isSupportDeleteAnnot(groupAnnot) && !(AppAnnotUtil.isLocked(groupAnnot) || AppAnnotUtil.isReadOnly(groupAnnot));

                            groupAnnots.add(groupAnnot);
                        }
                    }

                    String groupHeaderNM = AppAnnotUtil.getAnnotUniqueID(((Markup) annot).getGroupHeader());
                    if (!AppUtil.isEmpty(groupHeaderNM) && groupHeaderNM.equals(AppAnnotUtil.getAnnotUniqueID(annot))) {
                        if (groupAnnots.size() > 0)
                            groupHeaderNM = AppAnnotUtil.getAnnotUniqueID(groupAnnots.get(0));
                    }
                    if (AppUtil.isEmpty(groupHeaderNM)) {
                        groupHeaderNM = AppDmUtil.randomUUID(null);
                    }

                    for (int i = 0; i < groupAnnots.size(); i++) {
                        String annotNM = AppAnnotUtil.getAnnotUniqueID(groupAnnots.get(i));
                        AnnotNode node = mAdapter.getAnnotNode(page, annotNM);
                        if (node != null) {
                            node.setCanModifyContents(canModifyContent);
                            node.setCanReply(canReply);
                            node.setCanComment(canComment);
                            node.setDeletable(canDelete);
                            node.setGroupHeaderNM(groupHeaderNM);
                        }
                    }
                }
            }

            mAdapter.removeNode(page, AppAnnotUtil.getAnnotUniqueID(annot));
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        mMainLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mChangedTextView = (TextView) mMainLayout.findViewById(R.id.rv_panel_annot_notify);
        mChangedTextView.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {

    }

    private void resetDeleteDialog() {
        if (mDeleteDialog != null) {
            mDeleteDialog.dismiss();
        }
    }

    public void applyAllRedaction(Event.Callback callback) {
        final int size = mRedactTemps.size();
        if (size == 0) {
            if (mLoadedState == STATUS_REDACTING) {
                startSearch(mLoadedIndex);
            }
            if (mAdapter.getAnnotCount() == 0) {
                mPanel.showNoAnnotsView();
            }
            mAdapter.notifyDataSetChanged();
            return;
        }
        if (mLoadedState != STATUS_DONE) {
            mLoadedState = STATUS_REDACTING;
        }
        if (mPdfViewCtrl.getDoc() == null) return;
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
        }

        Iterator<AnnotNode> iterator = mRedactTemps.iterator();
        while (iterator.hasNext()) {
            AnnotNode node = iterator.next();
            if (node == null || node.isPageDivider()) {
                iterator.remove();
                continue;
            }

            if (!node.canApplyRedaction()) {
                iterator.remove();
                continue;
            }
        }

        mAdapter.applyAllRedactionNode(mRedactTemps, callback);
    }

    private void deleteItems() {
        final int size = mDeleteTemps.size();
        if (size == 0) {
            if (mLoadedState == STATUS_DELETING) {
                startSearch(mLoadedIndex);
            }
            resetDeleteDialog();
            if (mAdapter.getAnnotCount() == 0) {
                mPanel.showNoAnnotsView();
            }
            mAdapter.notifyDataSetChanged();
            return;
        }
        if (mLoadedState != STATUS_DONE) {
            mLoadedState = STATUS_DELETING;
        }
        if (mPdfViewCtrl.getDoc() == null) return;
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
        }

        final AnnotNode node = mDeleteTemps.get(size - 1);
        if (node == null || node.isPageDivider()) {
            mDeleteTemps.remove(node);
            deleteItems();
            return;
        }
        if (!node.canDelete()) {
            node.setChecked(false);
            onChecked(false, node);
            mDeleteTemps.remove(node);
            deleteItems();
            return;
        }
        mAdapter.removeNode(node, new AnnotAdapter.DeleteCallback() {

            @Override
            public void result(boolean success, AnnotNode n) {
                if (success) {
                    mDeleteTemps.remove(n);
                    onChecked(false, n);
                    deleteItems();
                } else {
                    resetDeleteDialog();
                }
            }
        });
    }

    private int checkDeleteStatus() {
        int status = DELETE_CAN;
        for (AnnotNode node : mCheckedNodes) {
            if (!node.canDelete()) {
                status = DELETE_CAN;
                AnnotNode parent = node.getParent();
                while (parent != null) {
                    if (parent.isChecked() && parent.canDelete()) {
                        status = DELETE_SRCAN;
                        break;
                    }
                    parent = parent.getParent();
                }
                if (status == DELETE_UNCAN) break;
            }
        }
        return status;
    }

    @Override
    public void onChecked(boolean isChecked, AnnotNode node) {
        if (isChecked) {
            if (!mCheckedNodes.contains(node)) {
                mCheckedNodes.add(node);
            }
        } else {
            mCheckedNodes.remove(node);
        }
    }

    public boolean jumpToPage(int position) {
        final AnnotNode node = (AnnotNode) mAdapter.getItem(position);
        if (node != null && !node.isPageDivider() && node.isRootNode() && !AppUtil.isEmpty(node.getUID())) {
            Task.CallBack callBack = new Task.CallBack() {
                @Override
                public void result(Task task) {
                    try {
                        PDFPage page = mPdfViewCtrl.getDoc().getPage(node.getPageIndex());
                        Annot annot = AppAnnotUtil.getAnnot(page, node.getUID());
                        if (annot == null || annot.isEmpty()) return;
                        RectF rect = AppUtil.toRectF(annot.getRect());
                        RectF rectPageView = new RectF();

                        //Covert rect from the PDF coordinate system to the page view coordinate system,
                        // and show the annotation to the middle of the screen as possible.
                        if (mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rectPageView, node.getPageIndex())) {
                            RectF pageRect = new RectF(0, 0, page.getWidth(), page.getHeight());
                            RectF pageViewRect = new RectF();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(pageRect, pageViewRect, node.getPageIndex());

                            float devX = 0;
                            float devY = 0;
                            boolean intersect = rectPageView.intersect(pageViewRect.left, pageViewRect.top, pageViewRect.right, pageViewRect.bottom);
                            if (intersect) {
                                devX = rectPageView.left - (mPdfViewCtrl.getWidth() - rectPageView.width()) / 2;
                                devY = rectPageView.top - (mPdfViewCtrl.getHeight() - rectPageView.height()) / 2;
                            }
                            mPdfViewCtrl.gotoPage(node.getPageIndex(), devX, devY);
                        } else {
                            mPdfViewCtrl.gotoPage(node.getPageIndex(), new PointF(rect.left, rect.top));
                        }
                        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                        }
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(annot);
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                }
            };

            mPdfViewCtrl.addTask(new Task(callBack) {
                @Override
                protected void execute() {

                }
            });

            return true;
        }
        return false;
    }

    public int getCount() {
        return mAdapter.getCount();
    }

    public int getCurrentStatus() {
        return mLoadedState;
    }


    public AnnotAdapter getAdapter() {
        return mAdapter;
    }

    public void prepare() {
        mAdapter.preparePageNodes();
        reset();
    }

    public void onDocWillClose() {
        reset();
        resetDeleteDialog();
        mSRDeleteDialog = null;
    }

    private void reset() {
        mChangedTextView.setVisibility(View.GONE);
        mPanel.hideNoAnnotsView();
        mLoadedState = STATUS_CANCEL;
        mPause = false;
        mLoadedIndex = 0;
        mCheckedNodes.clear();
        mRedactTemps.clear();
        mAdapter.clearNodes();
        mAdapter.notifyDataSetChanged();
    }

    public void startSearch(final int pageIndex) {
        mLoadedIndex = pageIndex;
        searchPage(pageIndex, new OnSearchPageEndListener() {
            @Override
            public void onResult(boolean success, ArrayList<AnnotNode> nodeList) {
                int pageCount = mPdfViewCtrl.getPageCount();
                //If search the current page  failed, then all subsequent pages will no longer continue to search.
                if (!success) {
                    mLoadedState = STATUS_FAILED;
                    mPanel.updateLoadedPage(pageIndex + 1, pageCount);
                    return;
                }

                if (mPause) {
                    mPanel.pauseSearch(pageIndex);
                    mLoadedState = STATUS_PAUSED;
                    return;
                }

                mPanel.updateLoadedPage(pageIndex + 1, pageCount);

                int nCount = nodeList.size();
                for (int i = 0; i < nCount; i++) {
                    AnnotNode annotNode = nodeList.get(i);
                    if (annotNode.canApplyRedaction()) {
                        if (!mRedactTemps.contains(annotNode)) {
                            mRedactTemps.add(annotNode);
                        }
                    }
                    mAdapter.addNode(annotNode);
                }
                mAdapter.establishNodeList();
                mAdapter.notifyDataSetChanged();
                if (pageIndex < pageCount - 1) {
                    if (mLoadedState != STATUS_CANCEL) {
                        startSearch(pageIndex + 1);
                    }
                } else {
                    mLoadedState = STATUS_DONE;
                    mPanel.updateLoadedPage(0, 0);
                }
            }
        });
    }

    public interface OnSearchPageEndListener {
        void onResult(boolean success, ArrayList<AnnotNode> list);
    }

    private void searchPage(int pageIndex, final OnSearchPageEndListener result) {
        SearchPageTask searchPageTask = new SearchPageTask(mPdfViewCtrl, pageIndex, result);
        mPdfViewCtrl.addTask(searchPageTask);
    }

    class SearchPageTask extends Task {
        private int mPageIndex;
        private PDFViewCtrl mPdfView;
        private ArrayList<AnnotNode> mSearchResults;
        private boolean mSearchRet;

        public SearchPageTask(PDFViewCtrl pdfView, int pageIndex, final OnSearchPageEndListener onSearchPageEndListener) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    SearchPageTask task1 = (SearchPageTask) task;
                    onSearchPageEndListener.onResult(task1.mSearchRet, task1.mSearchResults);
                }
            });
            mPdfView = pdfView;
            mPageIndex = pageIndex;
        }

        @Override
        protected void execute() {
            if (mSearchResults == null) {
                mSearchResults = new ArrayList<AnnotNode>();
            }
            mLoadedState = STATUS_LOADING;
            mSearchRet = searchPage();
        }

        private boolean searchPage() {
            try {
                PDFPage page = mPdfView.getDoc().getPage(mPageIndex);
                if (page == null || page.isEmpty()) return false;

                int nCount = page.getAnnotCount();
                if (nCount > 0) {
                    for (int i = 0; i < nCount; i++) {
                        Annot annot = AppAnnotUtil.createAnnot(page.getAnnot(i));
                        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
                        if (annot == null
                                || (annot.getFlags() & Annot.e_FlagHidden) != 0
                                || !AppAnnotUtil.isSupportEditAnnot(annot))
                            continue;

                        if (!uiExtensionsManager.isLoadAnnotModule(annot))
                            continue;

                        Annot replyToAnnot = AppAnnotUtil.getReplyToAnnot(annot);
                        //must getPage again.
                        AnnotNode node = new AnnotNode(mPageIndex, AppAnnotUtil.getAnnotUniqueID(annot), AppAnnotUtil.getAnnotUniqueID(replyToAnnot));
                        String modifiedDate = AppDmUtil.dateOriValue;
                        try {
                            modifiedDate = AppDmUtil.getLocalDateString(annot.getModifiedDateTime());
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                        String creationDate = AppDmUtil.dateOriValue;
                        if (annot.isMarkup()) {
                            String title = "";
                            try {
                                title = ((Markup) annot).getTitle();
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                            node.setAuthor(title);

                            String intent = "";
                            try {
                                intent = ((Markup) annot).getIntent();
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                            node.setIntent(intent);

                            try {
                                creationDate = AppDmUtil.getLocalDateString(((Markup) annot).getCreationDateTime());
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                        }
                        node.setType(AppAnnotUtil.getTypeString(annot));
                        node.setContent(annot.getContent());
                        node.setLocked(AppAnnotUtil.isLocked(annot));
                        node.setReadOnlyFlag(AppAnnotUtil.isReadOnly(annot));
                        node.setEditable(!node.isReadOnly());
                        node.setApplyRedaction(!annot.isEmpty() && annot.getType() == Annot.e_Redact);
                        if (modifiedDate == null || modifiedDate.equals(AppDmUtil.dateOriValue)) {
                            modifiedDate = creationDate;
                        }
                        node.setModifiedDate(modifiedDate);
                        node.setCreationDate(creationDate);
                        node.setDeletable(AppAnnotUtil.isSupportDelete(mPdfViewCtrl, annot));
                        node.setCanReply(AppAnnotUtil.isSupportReply(mPdfViewCtrl, annot));
                        node.setCanModifyContents(AppAnnotUtil.contentsModifiable(mPdfViewCtrl, annot));
                        node.setCanComment(AppAnnotUtil.isSupportComment(mPdfViewCtrl, annot));
                        if (GroupManager.getInstance().isGrouped(mPdfViewCtrl, annot))
                            node.setGroupHeaderNM(GroupManager.getInstance().getHeaderUniqueID(mPdfViewCtrl, annot));
                        mSearchResults.add(node);
                    }
                }

            } catch (PDFException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

}
