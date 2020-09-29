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
package com.foxit.uiextensions.annots.fileattachment;

import android.graphics.Matrix;
import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FileAttachment;
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


public abstract class FileAttachmentUndoItem extends AnnotUndoItem {
    String mIconName;
    String mPath = "";
    String attacheName;
    ArrayList<String> mGroupNMList = new ArrayList<>();
}

class FileAttachmentAddUndoItem extends FileAttachmentUndoItem {

    public FileAttachmentAddUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean redo() {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_FileAttachment, AppUtil.toFxRectF(this.mBBox)), Annot.e_FileAttachment);

            FileAttachmentEvent addEvent = new FileAttachmentEvent(EditAnnotEvent.EVENTTYPE_ADD, this, (FileAttachment) annot, mPdfViewCtrl);
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
                                Matrix matrix = mPdfViewCtrl.getDisplayMatrix(mPageIndex);
                                RectF annotRect = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
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

    @Override
    public boolean undo() {
        final FileAttachmentDeleteUndoItem undoItem = new FileAttachmentDeleteUndoItem(mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mPageIndex = mPageIndex;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof FileAttachment)) {
                return false;
            }

            Matrix matrix = mPdfViewCtrl.getDisplayMatrix(mPageIndex);
            final RectF annotRectF = new RectF();
            if (matrix != null) {
                annotRectF.set(AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix))));
            }

            if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
            }
            if (AppAnnotUtil.isGrouped(annot)) {
                ArrayList<String> groupNMList = GroupManager.getInstance().getGroupUniqueIDs(mPdfViewCtrl, annot);
                undoItem.mGroupNMList = groupNMList;
                mGroupNMList = groupNMList;
            }
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            FileAttachmentEvent deleteEvent = new FileAttachmentEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (FileAttachment) annot, mPdfViewCtrl);
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

                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(annotRectF));
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

class FileAttachmentModifyUndoItem extends FileAttachmentUndoItem {
    public int mUndoColor;
    public float mUndoOpacity;
    public String mUndoIconName;
    public String mUndoContents;
    public RectF mUndoBbox;

    public int mRedoColor;
    public float mRedoOpacity;
    public String mRedoIconName;
    public String mRedoContents;
    public RectF mRedoBbox;

    public FileAttachmentModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo() {
        return modifyAnnot(mUndoColor, mUndoOpacity, mUndoIconName, mUndoContents, mUndoBbox);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(mRedoColor, mRedoOpacity, mRedoIconName, mRedoContents, mRedoBbox);
    }

    private boolean modifyAnnot(int color, float opacity, String iconName, String contents, RectF bbox) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof FileAttachment)) {
                return false;
            }

            final RectF oldBbox = AppUtil.toRectF(annot.getRect());
            mBBox = new RectF(bbox);
            mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            mColor = color;
            mOpacity = opacity;
            mIconName = iconName;
            mContents = contents;

            FileAttachmentEvent modifyEvent = new FileAttachmentEvent(EditAnnotEvent.EVENTTYPE_MODIFY, this, (FileAttachment) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(modifyEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        }

                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(page, annot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                Matrix matrix = mPdfViewCtrl.getDisplayMatrix(mPageIndex);
                                RectF annotRect = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                                annotRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3, -AppAnnotUtil.getAnnotBBoxSpace() - 3);

                                mPdfViewCtrl.convertPdfRectToPageViewRect(oldBbox, oldBbox, mPageIndex);
                                oldBbox.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3, -AppAnnotUtil.getAnnotBBoxSpace() - 3);

                                oldBbox.union(annotRect);
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

class FileAttachmentDeleteUndoItem extends FileAttachmentUndoItem {

    public FileAttachmentDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    @Override
    public boolean undo(Event.Callback callback) {
        FileAttachmentAddUndoItem undoItem = new FileAttachmentAddUndoItem(mPdfViewCtrl);
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
        undoItem.mPath = mPath;
        undoItem.attacheName = attacheName;
        undoItem.mGroupNMList = mGroupNMList;
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final FileAttachment annot = (FileAttachment) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_FileAttachment, AppUtil.toFxRectF(mBBox)), Annot.e_FileAttachment);
            FileAttachmentEvent addEvent = new FileAttachmentEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
            if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
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
                                Matrix matrix = mPdfViewCtrl.getDisplayMatrix(mPageIndex);
                                RectF annotRectF = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
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
            final Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof FileAttachment)) {
                return false;
            }

            if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
            }

            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            Matrix matrix = mPdfViewCtrl.getDisplayMatrix(mPageIndex);
            final RectF annotRectF = new RectF();
            if (matrix != null) {
                annotRectF.set(AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix))));
            }
            FileAttachmentEvent deleteEvent = new FileAttachmentEvent(EditAnnotEvent.EVENTTYPE_DELETE, this, (FileAttachment) annot, mPdfViewCtrl);
            if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
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

                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(annotRectF));
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
