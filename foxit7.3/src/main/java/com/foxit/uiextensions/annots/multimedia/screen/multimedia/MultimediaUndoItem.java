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
package com.foxit.uiextensions.annots.multimedia.screen.multimedia;


import android.graphics.Bitmap;
import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Screen;
import com.foxit.sdk.pdf.objects.PDFDictionary;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

public abstract class MultimediaUndoItem extends AnnotUndoItem {

    String mFileName;
    String mFilePath;
    String mMediaClipContentType;

    PDFDictionary mPDFDictionary;
    Bitmap mPreviewBitmap;
    int mRotation;

    public MultimediaUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    protected void deleteAnnot(final Annot annot, final MultimediaDeleteUndoItem undoItem, final Event.Callback result) {
        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
        }

        try {
            final PDFPage page = annot.getPage();
            final int pageIndex = page.getIndex();
            final RectF annotRectF = AppUtil.toRectF(annot.getRect());

            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            final MultimediaEvent deleteEvent = new MultimediaEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Screen) annot, mPdfViewCtrl);
            if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
                if (result != null) {
                    result.result(deleteEvent, true);
                }
                return;
            }
            EditAnnotTask task = new EditAnnotTask(deleteEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            RectF deviceRectF = new RectF();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRectF, pageIndex);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(deviceRectF));
                        }
                    }
                    if (result != null) {
                        result.result(deleteEvent, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

}

class MultimediaAddUndoItem extends MultimediaUndoItem {
    public MultimediaAddUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean redo() {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Screen, AppUtil.toFxRectF(mBBox)), Annot.e_Screen);

            MultimediaEvent addEvent = new MultimediaEvent(EditAnnotEvent.EVENTTYPE_ADD, this, (Screen) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotAdded(page, annot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                RectF annotRect = AppUtil.toRectF(annot.getRect());
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

    @Override
    public boolean undo() {
        final MultimediaDeleteUndoItem delUndoItem = new MultimediaDeleteUndoItem(mPdfViewCtrl);
        delUndoItem.mNM = this.mNM;
        delUndoItem.mPageIndex = this.mPageIndex;
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Screen)) {
                return false;
            }

            deleteAnnot(annot, delUndoItem, null);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}

class MultimediaModifyUndoItem extends MultimediaUndoItem {
    public MultimediaModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(this);
    }

    @Override
    public boolean undo() {
        final MultimediaModifyUndoItem modifyUndoItem = new MultimediaModifyUndoItem(mPdfViewCtrl);
        modifyUndoItem.mBBox = new RectF(this.mOldBBox);
        modifyUndoItem.mColor = this.mOldColor;
        modifyUndoItem.mLineWidth = this.mOldLineWidth;
        modifyUndoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        return modifyAnnot(modifyUndoItem);
    }

    private boolean modifyAnnot(MultimediaModifyUndoItem undoItem) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Screen)) {
                return false;
            }

            final RectF oldBbox = AppUtil.toRectF(annot.getRect());

            MultimediaEvent modifyEvent = new MultimediaEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Screen) annot, mPdfViewCtrl);
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
                                RectF annotRect = AppUtil.toRectF(annot.getRect());
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, mPageIndex);
                                annotRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3,
                                        -AppAnnotUtil.getAnnotBBoxSpace() - 3);
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(annotRect));

                                mPdfViewCtrl.convertPdfRectToPageViewRect(oldBbox, oldBbox, mPageIndex);
                                oldBbox.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3,
                                        -AppAnnotUtil.getAnnotBBoxSpace() - 3);
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

class MultimediaDeleteUndoItem extends MultimediaUndoItem {

    public MultimediaDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean redo(Event.Callback callback) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Screen)) return false;

            deleteAnnot(annot, this, callback);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean undo(Event.Callback callback) {
        final MultimediaAddUndoItem addUndoItem = new MultimediaAddUndoItem(mPdfViewCtrl);

        addUndoItem.mPageIndex = this.mPageIndex;
        addUndoItem.mNM = this.mNM;
        addUndoItem.mBBox = new RectF(mBBox);
        addUndoItem.mModifiedDate = this.mModifiedDate;
        addUndoItem.mFlags = this.mFlags;
        addUndoItem.mContents = this.mContents;
        addUndoItem.mColor = this.mColor;
        addUndoItem.mLineWidth = this.mLineWidth;

        addUndoItem.mFileName = this.mFileName;
        addUndoItem.mFilePath = this.mFilePath;
        addUndoItem.mMediaClipContentType = this.mMediaClipContentType;
        addUndoItem.mPDFDictionary = this.mPDFDictionary;
        addUndoItem.mRotation = this.mRotation;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Screen annot = (Screen) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Screen, AppUtil.toFxRectF(mBBox)), Annot.e_Screen);

            MultimediaEvent addEvent = new MultimediaEvent(EditAnnotEvent.EVENTTYPE_ADD, addUndoItem, annot, mPdfViewCtrl);
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
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotAdded(page, annot);

                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            try {
                                RectF annotRectF = AppUtil.toRectF(annot.getRect());
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
    public boolean undo() {
        return undo(null);
    }

    @Override
    public boolean redo() {
        return redo(null);
    }
}