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
package com.foxit.uiextensions.annots.line;

import android.graphics.PointF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Line;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.multiselect.GroupManager;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public abstract class LineUndoItem extends AnnotUndoItem {
    PointF mStartPt = new PointF();
    PointF mEndPt = new PointF();

    int mStartingStyle;
    int mEndingStyle;

    PointF mOldStartPt = new PointF();
    PointF mOldEndPt = new PointF();

    int mOldStartingStyle;
    int mOldEndingStyle;

    String unit;
    String ratio;
    float factor;

    ArrayList<String> mGroupNMList = new ArrayList<>();

    LineRealAnnotHandler mAnnotHandler;

    public LineUndoItem(LineRealAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        mAnnotHandler = annotHandler;
        mPdfViewCtrl = pdfViewCtrl;
    }
}

class LineAddUndoItem extends LineUndoItem {

    public LineAddUndoItem(LineRealAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        super(annotHandler, pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        LineDeleteUndoItem undoItem = new LineDeleteUndoItem(mAnnotHandler, mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mPageIndex = mPageIndex;
        undoItem.mStartPt.set(mStartPt);
        undoItem.mEndPt.set(mEndPt);
        undoItem.mStartingStyle = mStartingStyle;
        undoItem.mEndingStyle = mEndingStyle;

        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Line)) {
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
            Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Line, AppUtil.toFxRectF(mBBox)), Annot.e_Line);

            mAnnotHandler.addAnnot(mPageIndex, (Line) annot, this, false, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (mGroupNMList.size() > 0) {
                            GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, mGroupNMList);
                            DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
                            documentManager.onAnnotGrouped(page, AppAnnotUtil.getAnnotsByNMs(page, mGroupNMList));
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

class LineModifyUndoItem extends LineUndoItem {

    public LineModifyUndoItem(LineRealAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        super(annotHandler, pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        return modifyAnnot(true);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(false);
    }

    private boolean modifyAnnot(boolean userOldValue) {
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Line)) {
                return false;
            }

            mAnnotHandler.modifyAnnot((Line) annot, this, userOldValue, false, true, null);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}

class LineDeleteUndoItem extends LineUndoItem {

    public LineDeleteUndoItem(LineRealAnnotHandler annotHandler, PDFViewCtrl pdfViewCtrl) {
        super(annotHandler, pdfViewCtrl);
    }

    @Override
    public boolean undo(final Event.Callback callback) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Line annot = (Line) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Line, AppUtil.toFxRectF(mBBox)), Annot.e_Line);
            LineAddUndoItem undoItem = new LineAddUndoItem(mAnnotHandler, mPdfViewCtrl);
            undoItem.mNM = mNM;
            undoItem.mPageIndex = mPageIndex;
            undoItem.mStartPt.set(mStartPt);
            undoItem.mEndPt.set(mEndPt);
            undoItem.mStartingStyle = mStartingStyle;
            undoItem.mEndingStyle = mEndingStyle;
            undoItem.mAuthor = mAuthor;
            undoItem.mFlags = mFlags;
            undoItem.mSubject = mSubject;
            undoItem.mCreationDate = mCreationDate;
            undoItem.mModifiedDate = mModifiedDate;
            undoItem.mColor = mColor;
            undoItem.mOpacity = mOpacity;
            undoItem.mLineWidth = mLineWidth;
            undoItem.mIntent = mIntent;
            undoItem.mContents = mContents;
            undoItem.mGroupNMList = mGroupNMList;
            if (LineConstants.INTENT_LINE_DIMENSION.equals(mIntent)) {
                undoItem.ratio = ratio;
                undoItem.factor = factor;
                undoItem.unit = unit;
            }

            mAnnotHandler.addAnnot(mPageIndex, annot, undoItem, false, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (mGroupNMList.size() > 0) {
                            GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, mGroupNMList);
                            DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
                            documentManager.onAnnotGrouped(page, AppAnnotUtil.getAnnotsByNMs(page, mGroupNMList));
                        }
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
            if (!(annot instanceof Line)) {
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