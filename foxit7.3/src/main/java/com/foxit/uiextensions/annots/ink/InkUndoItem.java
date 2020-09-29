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
package com.foxit.uiextensions.annots.ink;

import android.graphics.PointF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Path;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Ink;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.multiselect.GroupManager;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public abstract class InkUndoItem extends AnnotUndoItem {
    ArrayList<String> mGroupNMList = new ArrayList<>();
    ArrayList<ArrayList<PointF>> mInkLists;
    ArrayList<ArrayList<PointF>> mOldInkLists;
    Path mPath;
    Path mOldPath;
    InkAnnotHandler mAnnotHandler;
    boolean isFromEraser;

    public InkUndoItem(InkAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        mAnnotHandler = annotHandler;
        mPdfViewCtrl = pdfViewCtrl;
    }
}

class InkAddUndoItem extends InkUndoItem {

    public InkAddUndoItem(InkAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        super(annotHandler, pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        InkDeleteUndoItem undoItem = new InkDeleteUndoItem(mAnnotHandler, mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mPageIndex = mPageIndex;
        undoItem.mInkLists = InkAnnotUtil.cloneInkList(mInkLists);
        try {
            undoItem.mPath = new Path();
            for (int li = 0; li < mInkLists.size(); li++) { //li: line index
                ArrayList<PointF> line = mInkLists.get(li);
                int size = line.size();
                if (size == 1) {
                    undoItem.mPath.moveTo(AppUtil.toFxPointF(line.get(0)));
                    undoItem.mPath.lineTo(new com.foxit.sdk.common.fxcrt.PointF(line.get(0).x + 0.1f, line.get(0).y + 0.1f));
                } else {
                    for (int pi = 0; pi < size; pi++) {//pi: point index
                        if (pi == 0) {
                            undoItem.mPath.moveTo(AppUtil.toFxPointF(line.get(pi)));
                        } else {
                            undoItem.mPath.lineTo(AppUtil.toFxPointF(line.get(pi)));
                        }
                    }
                }
            }
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Ink)) {
                return false;
            }
            ArrayList<String> groupNMList = GroupManager.getInstance().getGroupUniqueIDs(mPdfViewCtrl, annot);
            undoItem.mGroupNMList = groupNMList;
            mGroupNMList = groupNMList;

            mAnnotHandler.removeAnnot(annot, undoItem, false, null);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean redo() {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Ink, AppUtil.toFxRectF(mBBox)), Annot.e_Ink);

            if (mInkLists == null) return false;
            mPath = new Path();
            for (int li = 0; li < mInkLists.size(); li++) { //li: line index
                ArrayList<PointF> line = mInkLists.get(li);
                int size = line.size();
                if (size == 1) {
                    mPath.moveTo(AppUtil.toFxPointF(line.get(0)));
                    mPath.lineTo(new com.foxit.sdk.common.fxcrt.PointF(line.get(0).x + 0.1f, line.get(0).y + 0.1f));
                } else {
                    for (int pi = 0; pi < size; pi++) {//pi: point index
                        if (pi == 0) {
                            mPath.moveTo(AppUtil.toFxPointF(line.get(pi)));
                        } else {
                            mPath.lineTo(AppUtil.toFxPointF(line.get(pi)));
                        }
                    }
                }
            }
            mAnnotHandler.addAnnot(mPageIndex, (Ink) annot, this, false, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (mGroupNMList.size() > 0) {
                            GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, mGroupNMList);
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotGrouped(page, AppAnnotUtil.getAnnotsByNMs(page, mGroupNMList));
                        }
                    }
                }
            });
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}

class InkModifyUndoItem extends InkUndoItem {
    public InkModifyUndoItem(InkAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        super(annotHandler, pdfViewCtrl);
    }

    @Override
    public boolean undo(Event.Callback callback) {
        return modifyAnnot(true, callback);
    }

    @Override
    public boolean redo(Event.Callback callback) {
        return modifyAnnot(false, callback);
    }

    private boolean modifyAnnot(boolean userOldValue, Event.Callback callback) {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Ink)) {
                return false;
            }
            if (userOldValue) {
                if (mOldInkLists != null) {
                    mOldPath = new Path();
                    for (int li = 0; li < mOldInkLists.size(); li++) { //li: line index
                        ArrayList<PointF> line = mOldInkLists.get(li);
                        int size = line.size();
                        if (size == 1) {
                            mOldPath.moveTo(AppUtil.toFxPointF(line.get(0)));
                            mOldPath.lineTo(new com.foxit.sdk.common.fxcrt.PointF(line.get(0).x + 0.1f, line.get(0).y + 0.1f));
                        } else {
                            for (int pi = 0; pi < size; pi++) {//pi: point index
                                if (pi == 0) {
                                    mOldPath.moveTo(AppUtil.toFxPointF(line.get(pi)));
                                } else {
                                    mOldPath.lineTo(AppUtil.toFxPointF(line.get(pi)));
                                }
                            }
                        }
                    }
                }
            } else {
                if (mInkLists != null) {
                    mPath = new Path();
                    for (int li = 0; li < mInkLists.size(); li++) { //li: line index
                        ArrayList<PointF> line = mInkLists.get(li);
                        int size = line.size();
                        if (size == 1) {
                            mPath.moveTo(AppUtil.toFxPointF(line.get(0)));
                            mPath.lineTo(new com.foxit.sdk.common.fxcrt.PointF(line.get(0).x + 0.1f, line.get(0).y + 0.1f));
                        } else {
                            for (int pi = 0; pi < size; pi++) {//pi: point index
                                if (pi == 0) {
                                    mPath.moveTo(AppUtil.toFxPointF(line.get(pi)));
                                } else {
                                    mPath.lineTo(AppUtil.toFxPointF(line.get(pi)));
                                }
                            }
                        }
                    }
                }
            }
            mAnnotHandler.modifyAnnot((Ink) annot, this, userOldValue, false, true, callback);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean undo() {
        return undo(null);
    }

    @Override
    public boolean redo() {
        return redo(null);
    }
}

class InkDeleteUndoItem extends InkUndoItem {

    public InkDeleteUndoItem(InkAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        super(annotHandler, pdfViewCtrl);
    }

    @Override
    public boolean undo(final Event.Callback callback) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Ink annot = (Ink) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Ink, AppUtil.toFxRectF(mBBox)), Annot.e_Ink);
            InkAddUndoItem undoItem = new InkAddUndoItem(mAnnotHandler, mPdfViewCtrl);
            undoItem.mNM = mNM;
            undoItem.mPageIndex = mPageIndex;
            undoItem.mAuthor = mAuthor;
            undoItem.mFlags = mFlags;
            undoItem.mSubject = mSubject;
            undoItem.mCreationDate = mCreationDate;
            undoItem.mModifiedDate = mModifiedDate;
            undoItem.mColor = mColor;
            undoItem.mOpacity = mOpacity;
            undoItem.mLineWidth = mLineWidth;
            undoItem.mIntent = mIntent;
            undoItem.mBBox = mBBox;
            undoItem.mContents = mContents;
            undoItem.isFromEraser = isFromEraser;
            undoItem.mGroupNMList = mGroupNMList;

            undoItem.mPath = new Path();
            if (mInkLists != null) {
                for (int li = 0; li < mInkLists.size(); li++) { //li: line index
                    ArrayList<PointF> line = mInkLists.get(li);
                    int size = line.size();
                    if (size == 1) {
                        mPath.moveTo(AppUtil.toFxPointF(line.get(0)));
                        mPath.lineTo(new com.foxit.sdk.common.fxcrt.PointF(line.get(0).x + 0.1f, line.get(0).y + 0.1f));
                    } else {
                        for (int pi = 0; pi < size; pi++) {//pi: point index
                            if (pi == 0) {
                                undoItem.mPath.moveTo(AppUtil.toFxPointF(line.get(pi)));
                            } else {
                                undoItem.mPath.lineTo(AppUtil.toFxPointF(line.get(pi)));
                            }
                        }
                    }
                }
            }
            mAnnotHandler.addAnnot(mPageIndex, annot, undoItem, false, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (mGroupNMList.size() > 0) {
                        GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, mGroupNMList);
                        DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
                        documentManager.onAnnotGrouped(page, AppAnnotUtil.getAnnotsByNMs(page, mGroupNMList));
                    }

                    if (callback != null)
                        callback.result(event, success);
                }
            });
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean redo(Event.Callback callback) {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Ink)) {
                return false;
            }

            mAnnotHandler.removeAnnot(annot, this, false, callback);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean undo() {
        return undo(null);
    }

    @Override
    public boolean redo() {
        return redo(null);
    }
}