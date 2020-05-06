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
package com.foxit.uiextensions.annots.screen.image;


import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Screen;
import com.foxit.sdk.pdf.objects.PDFDictionary;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

public abstract class PDFImageUndoItem extends AnnotUndoItem {

    PDFDictionary mPDFDictionary;
    int mRotation;
    String mImgPath;

    public PDFImageUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }
}

class PDFImageAddUndoItem extends PDFImageUndoItem {

    public PDFImageAddUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        final PDFImageDeleteUndoItem delUndoItem = new PDFImageDeleteUndoItem(mPdfViewCtrl);
        delUndoItem.mNM = this.mNM;
        delUndoItem.mPageIndex = this.mPageIndex;
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Screen)) return false;

            PDFImageAnnotHandler annotHandler = (PDFImageAnnotHandler) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(AnnotHandler.TYPE_SCREEN_IMAGE);
            if (annotHandler == null) return false;
            annotHandler.deleteAnnot(annot, delUndoItem, null);
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
            final Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Screen, AppUtil.toFxRectF(mBBox)), Annot.e_Screen);

            PDFImageEvent addEvent = new PDFImageEvent(EditAnnotEvent.EVENTTYPE_ADD, this, (Screen) annot, mPdfViewCtrl);
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
}

class PDFImageDeleteUndoItem extends PDFImageUndoItem {

    public PDFImageDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo(Event.Callback callback) {
        final PDFImageAddUndoItem addUndoItem = new PDFImageAddUndoItem(mPdfViewCtrl);
        addUndoItem.mPageIndex = this.mPageIndex;
        addUndoItem.mNM = this.mNM;
        addUndoItem.mOpacity = this.mOpacity;
        addUndoItem.mBBox = new RectF(mBBox);
        addUndoItem.mAuthor = this.mAuthor;
        addUndoItem.mContents = this.mContents;
        addUndoItem.mModifiedDate = this.mModifiedDate;
        addUndoItem.mFlags = this.mFlags;

        addUndoItem.mRotation = this.mRotation;
        addUndoItem.mPDFDictionary = this.mPDFDictionary;
        addUndoItem.mIntent = mIntent;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Screen annot = (Screen) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Screen, AppUtil.toFxRectF(mBBox)), Annot.e_Screen);

            PDFImageEvent addEvent = new PDFImageEvent(EditAnnotEvent.EVENTTYPE_ADD, addUndoItem, annot, mPdfViewCtrl);
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
    public boolean redo(Event.Callback callback) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Screen)) return false;
            PDFImageAnnotHandler annotHandler = (PDFImageAnnotHandler) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(AnnotHandler.TYPE_SCREEN_IMAGE);
            if (annotHandler == null) return false;
            annotHandler.deleteAnnot(annot, this, callback);
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

class PDFImageModifyUndoItem extends PDFImageUndoItem {

    PDFDictionary mOldPDFDictionary;
    int mOldRotation;

    public PDFImageModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        final PDFImageModifyUndoItem modifyUndoItem = new PDFImageModifyUndoItem(mPdfViewCtrl);
        modifyUndoItem.mPageIndex = this.mPageIndex;
        modifyUndoItem.mNM = this.mNM;
        modifyUndoItem.mPDFDictionary = mOldPDFDictionary;
        modifyUndoItem.mRotation = this.mOldRotation;
        modifyUndoItem.mOpacity = this.mOldOpacity;
        modifyUndoItem.mContents = this.mOldContents;
        modifyUndoItem.mBBox = new RectF(this.mOldBBox);
        modifyUndoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();

        return modifyAnnot(modifyUndoItem);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(this);
    }

    private boolean modifyAnnot(PDFImageModifyUndoItem undoItem) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Screen)) return false;
            final RectF oldBbox = AppUtil.toRectF(annot.getRect());

            PDFImageEvent modifyEvent = new PDFImageEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Screen) annot, mPdfViewCtrl);
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
