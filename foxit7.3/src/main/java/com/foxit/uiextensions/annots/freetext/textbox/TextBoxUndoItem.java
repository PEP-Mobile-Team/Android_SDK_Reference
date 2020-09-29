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
package com.foxit.uiextensions.annots.freetext.textbox;


import android.graphics.Color;
import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.multiselect.GroupManager;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public abstract class TextBoxUndoItem extends AnnotUndoItem {
    Font mFont;
    float mFontSize;
    int mTextColor;
    int mDaFlags;
    RectF mTextRectF;
    int mRotation;
    ArrayList<String> mGroupNMList = new ArrayList<>();

    public TextBoxUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }
}

class TextBoxAddUndoItem extends TextBoxUndoItem {

    public TextBoxAddUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        final TextBoxDeleteUndoItem delUndoItem = new TextBoxDeleteUndoItem(mPdfViewCtrl);
        delUndoItem.mNM = this.mNM;
        delUndoItem.mPageIndex = this.mPageIndex;
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof FreeText)) {
                return false;
            }

            if (((FreeText) annot).getIntent() != null) {
                return false;
            }

            TextBoxAnnotHandler annotHandler = (TextBoxAnnotHandler) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(AnnotHandler.TYPE_FREETEXT_TEXTBOX);
            if (annotHandler == null) {
                return false;
            }
            if (AppAnnotUtil.isGrouped(annot)) {
                ArrayList<String> groupNMList = GroupManager.getInstance().getGroupUniqueIDs(mPdfViewCtrl, annot);
                delUndoItem.mGroupNMList = groupNMList;
                mGroupNMList = groupNMList;
            }
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
            final Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_FreeText, AppUtil.toFxRectF(mBBox)), Annot.e_FreeText);

            TextBoxEvent addEvent = new TextBoxEvent(EditAnnotEvent.EVENTTYPE_ADD, this, (FreeText) annot, mPdfViewCtrl);
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

class TextBoxDeleteUndoItem extends TextBoxUndoItem {

    public TextBoxDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo(Event.Callback callback) {
        final TextBoxAddUndoItem addUndoItem = new TextBoxAddUndoItem(mPdfViewCtrl);
        addUndoItem.mPageIndex = this.mPageIndex;
        addUndoItem.mNM = this.mNM;
        addUndoItem.mColor = Color.RED;
        addUndoItem.mOpacity = this.mOpacity;
        addUndoItem.mFont = this.mFont;
        addUndoItem.mFontSize = this.mFontSize;
        addUndoItem.mBBox =  new RectF(mBBox);
        addUndoItem.mTextRectF = new RectF(mTextRectF);
        addUndoItem.mAuthor = this.mAuthor;
        addUndoItem.mContents = this.mContents;
        addUndoItem.mModifiedDate = this.mModifiedDate;
        addUndoItem.mFlags = this.mFlags;
        addUndoItem.mTextColor = mTextColor;
        addUndoItem.mDaFlags = mDaFlags;
        addUndoItem.mIntent = mIntent;
        addUndoItem.mSubject = mSubject;
        addUndoItem.mRotation = mRotation;
        addUndoItem.mGroupNMList = mGroupNMList;

        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final FreeText annot = (FreeText) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_FreeText, AppUtil.toFxRectF(mBBox)), Annot.e_FreeText);

            TextBoxEvent addEvent = new TextBoxEvent(EditAnnotEvent.EVENTTYPE_ADD, addUndoItem, annot, mPdfViewCtrl);
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
            if (!(annot instanceof FreeText)) return false;
            if (((FreeText) annot).getIntent() != null)return false;

            TextBoxAnnotHandler annotHandler = (TextBoxAnnotHandler) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(AnnotHandler.TYPE_FREETEXT_TEXTBOX);
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

class TextBoxModifyUndoItem extends TextBoxUndoItem {

    Font mOldFont;
    float mOldFontSize;
    int mOldTextColor;
    RectF mOldTextRectF;

    public TextBoxModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        final TextBoxModifyUndoItem modifyUndoItem = new TextBoxModifyUndoItem(mPdfViewCtrl);
        modifyUndoItem.mPageIndex = this.mPageIndex;
        modifyUndoItem.mNM = this.mNM;
        modifyUndoItem.mTextColor = this.mOldTextColor;
        modifyUndoItem.mOpacity = this.mOldOpacity;
        modifyUndoItem.mFont = this.mOldFont;
        modifyUndoItem.mFontSize = this.mOldFontSize;
        modifyUndoItem.mContents = this.mOldContents;
        modifyUndoItem.mAuthor = this.mAuthor;
        modifyUndoItem.mBBox = new RectF(mOldBBox);
        modifyUndoItem.mTextRectF = new RectF(mOldTextRectF);
        modifyUndoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();

        return modifyAnnot(modifyUndoItem);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(this);
    }

    private boolean modifyAnnot(TextBoxModifyUndoItem undoItem) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof FreeText)) {
                return false;
            }

            if (((FreeText) annot).getIntent() != null) {
                return false;
            }

            final RectF oldBbox = AppUtil.toRectF(annot.getRect());

            TextBoxEvent modifyEvent = new TextBoxEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (FreeText) annot, mPdfViewCtrl);
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
