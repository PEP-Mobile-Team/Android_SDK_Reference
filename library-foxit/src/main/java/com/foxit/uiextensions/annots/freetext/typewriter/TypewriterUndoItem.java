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
package com.foxit.uiextensions.annots.freetext.typewriter;

import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

public abstract class TypewriterUndoItem extends AnnotUndoItem {
    Font mFont;
    float mFontSize;
    int mTextColor;
    int mDaFlags;
    int mRotation;
    public TypewriterUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }
}

class TypewriterAddUndoItem extends TypewriterUndoItem {

    public TypewriterAddUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        TypewriterDeleteUndoItem undoItem = new TypewriterDeleteUndoItem(mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mPageIndex = mPageIndex;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof FreeText)) {
                return false;
            }

            if (((FreeText) annot).getIntent() == null
                    || !((FreeText) annot).getIntent().equals("FreeTextTypewriter")) {
                return false;
            }

            TypewriterAnnotHandler annotHandler = (TypewriterAnnotHandler) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(Annot.e_FreeText);
            if (annotHandler == null) {
                return false;
            }
            annotHandler.deleteAnnot(annot, undoItem, null);
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
            final Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_FreeText, AppUtil.toFxRectF(mBBox)), Annot.e_FreeText);

            TypewriterEvent addEvent = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_ADD, this, (FreeText) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(addEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotAdded(page, annot);
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

class TypewriterModifyUndoItem extends TypewriterUndoItem {
    Font mOldFont;
    float mOldFontSize;
    int mOldTextColor;
    public TypewriterModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        TypewriterModifyUndoItem undoItem = new TypewriterModifyUndoItem(mPdfViewCtrl);
        undoItem.mPageIndex = this.mPageIndex;
        undoItem.mNM = this.mNM;
        undoItem.mTextColor = this.mOldTextColor;
        undoItem.mOpacity = this.mOldOpacity;
        undoItem.mFont = this.mOldFont;
        undoItem.mFontSize = this.mOldFontSize;
        undoItem.mContents = this.mOldContents;
        undoItem.mBBox = new RectF(this.mOldBBox);
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();

        return modifyAnnot(undoItem);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(this);
    }

    private boolean modifyAnnot(TypewriterModifyUndoItem undoItem) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof FreeText)) {
                return false;
            }

            if (((FreeText) annot).getIntent() == null
                    || !((FreeText) annot).getIntent().equals("FreeTextTypewriter")) {
                return false;
            }

            final RectF oldBbox = AppUtil.toRectF(annot.getRect());

            TypewriterEvent modifyEvent = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (FreeText) annot, mPdfViewCtrl);
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

class TypewriterDeleteUndoItem extends TypewriterUndoItem {

    public TypewriterDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo(Event.Callback callback) {
        TypewriterAddUndoItem undoItem = new TypewriterAddUndoItem(mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mAuthor = mAuthor;
        undoItem.mBBox = new RectF(mBBox);
        undoItem.mColor = mColor;
        undoItem.mContents = mContents;
        undoItem.mModifiedDate = mModifiedDate;
        undoItem.mOpacity = mOpacity;
        undoItem.mPageIndex = mPageIndex;
        undoItem.mFlags = mFlags;
        undoItem.mFont = mFont;
        undoItem.mFontSize = mFontSize;
        undoItem.mTextColor = mTextColor;
        undoItem.mDaFlags = mDaFlags;
        undoItem.mIntent = mIntent;
        undoItem.mSubject = mSubject;
        undoItem.mRotation = mRotation;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final FreeText annot = (FreeText) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_FreeText, AppUtil.toFxRectF(mBBox)), Annot.e_FreeText);

            TypewriterEvent addEvent = new TypewriterEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
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
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotAdded(page, annot);

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
            final Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof FreeText)) return false;
            String intent = ((FreeText) annot).getIntent();
            if (AppUtil.isEmpty(intent) || !intent.equals("FreeTextTypewriter")) return false;

            TypewriterAnnotHandler annotHandler = (TypewriterAnnotHandler) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(Annot.e_FreeText);
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