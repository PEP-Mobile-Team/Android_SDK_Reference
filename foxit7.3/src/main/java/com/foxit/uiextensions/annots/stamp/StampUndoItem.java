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
package com.foxit.uiextensions.annots.stamp;

import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Stamp;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.multiselect.GroupManager;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public abstract class StampUndoItem extends AnnotUndoItem {
    int mStampType;
    String mIconName;
    int mRotation;
    ArrayList<String> mGroupNMList = new ArrayList<>();
}

class StampAddUndoItem extends StampUndoItem {

    public StampAddUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        final StampDeleteUndoItem undoItem = new StampDeleteUndoItem(mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mPageIndex = mPageIndex;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Stamp)) {
                return false;
            }

            final RectF annotRectF = AppUtil.toRectF(annot.getRect());

            if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
            }
            if (AppAnnotUtil.isGrouped(annot)) {
                ArrayList<String> groupNMList = GroupManager.getInstance().getGroupUniqueIDs(mPdfViewCtrl, annot);
                undoItem.mGroupNMList = groupNMList;
                mGroupNMList = groupNMList;
            }

            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);

            StampEvent deleteEvent = new StampEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Stamp) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(deleteEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (undoItem.mGroupNMList.size() >= 2) {
                            ArrayList<String> newGroupList = new ArrayList<>(undoItem.mGroupNMList);
                            newGroupList.remove(undoItem.mNM);
                            if (newGroupList.size() >= 2)
                                GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, newGroupList);
                            else
                                GroupManager.getInstance().unGroup(page, newGroupList.get(0));
                        }

                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            RectF deviceRectF = new RectF();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRectF, mPageIndex);
                            mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(deviceRectF));
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
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
            final Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Stamp, AppUtil.toFxRectF(mBBox)), Annot.e_Stamp);

            StampEvent addEvent = new StampEvent(EditAnnotEvent.EVENTTYPE_ADD, this, (Stamp) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
                        if (mGroupNMList.size() > 0) {
                            GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, mGroupNMList);
                            documentManager.onAnnotGrouped(page, AppAnnotUtil.getAnnotsByNMs(page, mGroupNMList));
                        }
                        documentManager.onAnnotAdded(page, annot);

                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                RectF annotRect = new RectF(AppUtil.toRectF(annot.getRect()));
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, mPageIndex);
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(annotRect));
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}

class StampModifyUndoItem extends StampUndoItem {

    protected RectF mUndoBox;
    protected String mUndoContent;
    protected int mUndoRotation;
    protected String mUndoIconName;

    protected RectF mRedoBox;
    protected String mRedoContent;
    protected int mRedoRotation;
    protected String mRedoIconName;

    public StampModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        return modifyAnnot(mUndoBox, mUndoContent, mUndoRotation, mUndoIconName);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(mRedoBox, mRedoContent, mRedoRotation, mRedoIconName);
    }

    private boolean modifyAnnot(RectF bbox, String content, int rotation, String iconName) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Stamp)) {
                return false;
            }

            final RectF oldBbox = new RectF(AppUtil.toRectF(annot.getRect()));
            mBBox = new RectF(bbox);
            mContents = content;
            mRotation = rotation;
            mIconName = iconName;
            mModifiedDate = AppDmUtil.currentDateToDocumentDate();

            StampEvent modifyEvent = new StampEvent(EditAnnotEvent.EVENTTYPE_MODIFY, this, (Stamp) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(modifyEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        }

                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(page, annot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                RectF annotRect = new RectF(AppUtil.toRectF(annot.getRect()));
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, mPageIndex);
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(annotRect));

                                mPdfViewCtrl.convertPdfRectToPageViewRect(oldBbox, oldBbox, mPageIndex);
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(oldBbox));
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}

class StampDeleteUndoItem extends StampUndoItem {

    public StampDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo(Event.Callback callback) {
        StampAddUndoItem undoItem = new StampAddUndoItem(mPdfViewCtrl);
        undoItem.mPageIndex = mPageIndex;
        undoItem.mStampType = mStampType;
        undoItem.mNM = mNM;
        undoItem.mAuthor = mAuthor;
        undoItem.mFlags = mFlags;
        undoItem.mSubject = mSubject;
        undoItem.mIconName = mIconName;
        undoItem.mCreationDate = mCreationDate;
        undoItem.mModifiedDate = mModifiedDate;
        undoItem.mBBox = new RectF(mBBox);
        undoItem.mContents = mContents;
        undoItem.mRotation = mRotation;
        undoItem.mGroupNMList = mGroupNMList;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Stamp annot = (Stamp) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Stamp, AppUtil.toFxRectF(mBBox)), Annot.e_Stamp);
            StampEvent addEvent = new StampEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
            if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
                if (callback != null) {
                    callback.result(addEvent, true);
                }
                return true;
            }
            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
                        if (mGroupNMList.size() > 0) {
                            GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, mGroupNMList);
                            documentManager.onAnnotGrouped(page, AppAnnotUtil.getAnnotsByNMs(page, mGroupNMList));
                        }
                        documentManager.onAnnotAdded(page, annot);

                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                RectF annotRectF = new RectF(AppUtil.toRectF(annot.getRect()));
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, mPageIndex);
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(annotRectF));
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean redo(Event.Callback callback) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Stamp)) return false;

            if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
            }

            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            final RectF annotRectF = AppUtil.toRectF(annot.getRect());
            StampEvent deleteEvent = new StampEvent(EditAnnotEvent.EVENTTYPE_DELETE, this, (Stamp) annot, mPdfViewCtrl);
            if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
                if (callback != null) {
                    callback.result(deleteEvent, true);
                }
                return true;
            }
            EditAnnotTask task = new EditAnnotTask(deleteEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (mGroupNMList.size() >= 2) {
                            ArrayList<String> newGroupList = new ArrayList<>(mGroupNMList);
                            newGroupList.remove(mNM);
                            if (newGroupList.size() >= 2)
                                GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, newGroupList);
                            else
                                GroupManager.getInstance().unGroup(page, newGroupList.get(0));
                        }

                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            RectF deviceRectF = new RectF();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRectF, mPageIndex);
                            mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(deviceRectF));
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
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