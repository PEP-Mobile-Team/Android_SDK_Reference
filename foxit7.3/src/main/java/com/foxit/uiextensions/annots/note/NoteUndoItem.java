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
package com.foxit.uiextensions.annots.note;

import android.graphics.Matrix;
import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.Note;
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

public abstract class NoteUndoItem extends AnnotUndoItem {
    String mIconName;
    boolean mOpenStatus;
    boolean mIsFromReplyModule = false;
    public String mParentNM;// use nm get annot, and addReply
    ArrayList<String> mGroupNMList = new ArrayList<>();
}

class NoteAddUndoItem extends NoteUndoItem {

    public NoteAddUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }
    @Override
    public boolean undo() {
        final NoteDeleteUndoItem undoItem = new NoteDeleteUndoItem(mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mPageIndex = mPageIndex;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Note)) {
                return false;
            }

            Matrix matrix = mPdfViewCtrl.getDisplayMatrix(mPageIndex);
            final RectF deviceRectF = new RectF();
            if (matrix != null) {
                deviceRectF.set(AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix))));
            }
            if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
            }

            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);

            if (AppAnnotUtil.isGrouped(annot)) {
                ArrayList<String> groupNMList = GroupManager.getInstance().getGroupUniqueIDs(mPdfViewCtrl, annot);
                undoItem.mGroupNMList = groupNMList;
                mGroupNMList = groupNMList;
            }
            NoteEvent deleteEvent = new NoteEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Note) annot, mPdfViewCtrl);
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
            Annot annot = null;
            if (mIsFromReplyModule) {
                Annot note = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mParentNM);
                if (note == null) return false;
                annot = ((Markup) note).addReply();
            } else {
                annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Note, AppUtil.toFxRectF(this.mBBox)), Annot.e_Note);
            }

            NoteEvent addEvent = new NoteEvent(EditAnnotEvent.EVENTTYPE_ADD, this, (Note) annot, mPdfViewCtrl);
            final Annot finalAnnot = annot;
            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
                        if (mGroupNMList.size() > 0) {
                            GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, mGroupNMList);
                            documentManager.onAnnotGrouped(page, AppAnnotUtil.getAnnotsByNMs(page, mGroupNMList));
                        }
                        documentManager.onAnnotAdded(page, finalAnnot);

                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                Matrix matrix = mPdfViewCtrl.getDisplayMatrix(mPageIndex);
                                RectF deviceRectF = AppUtil.toRectF(finalAnnot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(deviceRectF));
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

class NoteModifyUndoItem extends NoteUndoItem {
    public int 		mUndoColor;
    public float 	mUndoOpacity;
    public String   mUndoIconName;
    public RectF    mUndoBbox;
    public String   mUndoContent;


    public int		mRedoColor;
    public float	mRedoOpacity;
    public String   mRedoIconName;
    public RectF    mRedoBbox;
    public String   mRedoContent;

    public NoteModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        return modifyAnnot(mUndoColor, mUndoOpacity, mUndoContent, mUndoIconName, mUndoBbox);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(mRedoColor, mRedoOpacity, mRedoContent, mRedoIconName, mRedoBbox);
    }

    private boolean modifyAnnot(int color, float opacity, String content, String iconName, RectF bbox) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Note)) {
                return false;
            }

            final RectF oldBbox = AppUtil.toRectF(annot.getRect());
            mBBox = new RectF(bbox);
            mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            mColor = color;
            mOpacity = opacity;
            mIconName = iconName;
            mContents = content;

            NoteEvent modifyEvent = new NoteEvent(EditAnnotEvent.EVENTTYPE_MODIFY, this, (Note) annot, mPdfViewCtrl);
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
                                Matrix matrix = mPdfViewCtrl.getDisplayMatrix(mPageIndex);
                                RectF deviceRectF = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                                deviceRectF.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3, -AppAnnotUtil.getAnnotBBoxSpace() - 3);

                                mPdfViewCtrl.convertPdfRectToPageViewRect(oldBbox, oldBbox, mPageIndex);
                                oldBbox.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3, -AppAnnotUtil.getAnnotBBoxSpace() - 3);

                                oldBbox.union(deviceRectF);
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

class NoteDeleteUndoItem extends NoteUndoItem {

    public NoteDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo(Event.Callback callback) {
        NoteAddUndoItem undoItem = new NoteAddUndoItem(mPdfViewCtrl);
        undoItem.mPageIndex = mPageIndex;
        undoItem.mNM = mNM;
        undoItem.mAuthor = mAuthor;
        undoItem.mFlags = mFlags;
        undoItem.mSubject = mSubject;
        undoItem.mIconName = mIconName;
        undoItem.mCreationDate = mCreationDate;
        undoItem.mModifiedDate = mModifiedDate;
        undoItem.mBBox = new RectF(mBBox);
        undoItem.mContents = mContents;
        undoItem.mColor = mColor;
        undoItem.mOpacity = mOpacity;
        undoItem.mIsFromReplyModule = mIsFromReplyModule;
        undoItem.mParentNM = mParentNM;
        undoItem.mGroupNMList = mGroupNMList;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            Note annot = null;
            if (mIsFromReplyModule) {
                Annot note = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mParentNM);
                if (note == null || note.isEmpty()) return false;
                annot = ((Markup) note).addReply();
            } else {
                annot = (Note) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Note, AppUtil.toFxRectF(this.mBBox)), Annot.e_Note);
            }
            NoteEvent addEvent = new NoteEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
            if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
                if (callback != null) {
                    callback.result(addEvent, true);
                }
                return true;
            }
            final Note finalAnnot = annot;
            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        DocumentManager documentManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
                        if (mGroupNMList.size() > 0) {
                            GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, mGroupNMList);
                            documentManager.onAnnotGrouped(page, AppAnnotUtil.getAnnotsByNMs(page, mGroupNMList));
                        }
                        documentManager.onAnnotAdded(page, finalAnnot);

                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                Matrix matrix = mPdfViewCtrl.getDisplayMatrix(mPageIndex);
                                RectF deviceRectF = AppUtil.toRectF(finalAnnot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(deviceRectF));
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
            if (!(annot instanceof Note)) {
                return false;
            }

            if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
            }

            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            Matrix matrix = mPdfViewCtrl.getDisplayMatrix(mPageIndex);
            final RectF deviceRectF = new RectF();
            if (matrix != null) {
                deviceRectF.set(AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix))));
            }
            NoteEvent deleteEvent = new NoteEvent(EditAnnotEvent.EVENTTYPE_DELETE, this, (Note) annot, mPdfViewCtrl);
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