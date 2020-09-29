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
package com.foxit.uiextensions.annots.caret;

import android.graphics.Rect;
import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Caret;
import com.foxit.sdk.pdf.annots.StrikeOut;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContent;
import com.foxit.uiextensions.annots.textmarkup.TextSelector;
import com.foxit.uiextensions.annots.textmarkup.strikeout.StrikeoutEvent;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

public abstract class CaretUndoItem extends AnnotUndoItem {
    int mRotate;
    public CaretUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }
}

class CaretAddUndoItem extends CaretUndoItem {
    TextSelector mTextSelector;
    EditAnnotEvent strikeOutEvent; // for replace caret;
    boolean mIsReplace;
    public CaretAddUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        CaretDeleteUndoItem undoItem = new CaretDeleteUndoItem(mPdfViewCtrl);
        undoItem.mNM = mNM;
        undoItem.mPageIndex = mPageIndex;
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Caret)) {
                return false;
            }

            CaretAnnotHandler annotHandler = (CaretAnnotHandler) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(Annot.e_Caret);
            if (annotHandler == null) {
                return false;
            }

            return annotHandler.deleteAnnot(annot, undoItem, false, null);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean redo() {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Caret, AppUtil.toFxRectF(mBBox)), Annot.e_Caret);

            CaretAnnotHandler annotHandler = (CaretAnnotHandler) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(Annot.e_Caret);
            if (annotHandler == null) {
                return false;
            }
            CaretToolHandler toolHandler = (CaretToolHandler) annotHandler.getToolHandler(mIntent);
            if (toolHandler == null) return false;
            toolHandler.addAnnot(annot, this, false, null);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}

class CaretModifyUndoItem extends CaretUndoItem {
    public int mLastColor;
    public float mLastOpacity;
    public String mLastContent;

    public CaretModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        CaretModifyUndoItem undoItem = new CaretModifyUndoItem(mPdfViewCtrl);
        undoItem.mPageIndex = this.mPageIndex;
        undoItem.mNM = this.mNM;
        undoItem.mColor = this.mLastColor;
        undoItem.mOpacity = this.mLastOpacity;
        undoItem.mBBox = this.mBBox;
        undoItem.mAuthor = this.mAuthor;
        undoItem.mContents = this.mLastContent;
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        undoItem.mIntent = mIntent;
        return modifyAnnot(undoItem);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(this);
    }

    private boolean modifyAnnot(final CaretModifyUndoItem undoItem) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof Caret)) {
                return false;
            }

            CaretEvent modifyEvent = new CaretEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Caret) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(modifyEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {

                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(page, annot);

                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            RectF viewRect = null;
                            try {
                                viewRect = AppUtil.toRectF(annot.getRect());
                            } catch (PDFException e) {
                                return;
                            }
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, mPageIndex);
                            viewRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3, -AppAnnotUtil.getAnnotBBoxSpace() - 3);
                            mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(viewRect));
                        }

                        if (AppAnnotUtil.isReplaceCaret(annot)) {
                            final StrikeOut subAnnot = AppAnnotUtil.getStrikeOutFromCaret((Caret) annot);
                            if (subAnnot == null)
                                return;
                            AnnotHandler annotHandler = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(Annot.e_StrikeOut);

                            AnnotContent annotContent = new AnnotContent() {
                                @Override
                                public int getPageIndex() {
                                    return undoItem.mPageIndex;
                                }

                                @Override
                                public int getType() {
                                    return Annot.e_StrikeOut;
                                }

                                @Override
                                public String getNM() {
                                    return AppAnnotUtil.getAnnotUniqueID(subAnnot);
                                }

                                @Override
                                public RectF getBBox() {
                                    try {
                                        return AppUtil.toRectF(subAnnot.getRect());
                                    } catch (PDFException e) {
                                    }
                                    return null;
                                }

                                @Override
                                public int getColor() {
                                    return (int) undoItem.mColor;
                                }

                                @Override
                                public int getOpacity() {
                                    return (int) (undoItem.mOpacity * 255f + 0.5f);
                                }

                                @Override
                                public float getLineWidth() {
                                    try {
                                        return subAnnot.getBorderInfo().getWidth();
                                    } catch (PDFException e) {
                                    }
                                    return 0;
                                }

                                @Override
                                public String getSubject() {
                                    try {
                                        return subAnnot.getSubject();
                                    } catch (PDFException e) {
                                    }
                                    return null;
                                }

                                @Override
                                public DateTime getModifiedDate() {
                                    try {
                                        return annot.getModifiedDateTime();
                                    } catch (PDFException e) {
                                    }
                                    return null;
                                }

                                @Override
                                public String getContents() {
                                    try {
                                        return annot.getContent();
                                    } catch (PDFException e) {
                                    }
                                    return null;
                                }


                                @Override
                                public String getIntent() {
                                    try {
                                        return subAnnot.getIntent();
                                    } catch (PDFException e) {
                                    }
                                    return null;
                                }

                            };
                            annotHandler.modifyAnnot(subAnnot, annotContent, false, null);
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

class CaretDeleteUndoItem extends CaretUndoItem {
    TextMarkupContent mTMContent;
    boolean mIsReplace;
    public CaretDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo(Event.Callback callback) {
        final CaretAddUndoItem undoItem = new CaretAddUndoItem(mPdfViewCtrl);
        undoItem.mPageIndex = mPageIndex;
        undoItem.mNM = this.mNM;
        undoItem.mColor = this.mColor;
        undoItem.mOpacity = this.mOpacity;
        undoItem.mBBox = this.mBBox;
        undoItem.mAuthor = this.mAuthor;
        undoItem.mContents = this.mContents;
        undoItem.mModifiedDate = mModifiedDate;
        undoItem.mCreationDate = mCreationDate;
        undoItem.mFlags = this.mFlags;
        undoItem.mSubject = mSubject;
        undoItem.mIntent = mIntent;
        undoItem.mIsReplace = mIsReplace;
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Caret, AppUtil.toFxRectF(mBBox)), Annot.e_Caret);

            if (mIsReplace) {
                AnnotHandler annotHandler = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(Annot.e_StrikeOut);
                annotHandler.addAnnot(mPageIndex, mTMContent, false, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (!(event instanceof StrikeoutEvent)) return;
                        undoItem.strikeOutEvent = (StrikeoutEvent) event;
                    }
                });
            }

            CaretEvent event = new CaretEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, (Caret) annot, mPdfViewCtrl);
            if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
                if (callback != null) {
                    callback.result(event, true);
                }
                return true;
            }
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        try {
                            final PDFPage page = annot.getPage();
                            final int pageIndex = page.getIndex();
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotAdded(page, annot);

                            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                                RectF viewRect = AppUtil.toRectF(annot.getRect());
                                if (mIsReplace) {
                                    StrikeOut strikeOut = AppAnnotUtil.getStrikeOutFromCaret((Caret) annot);
                                    if (strikeOut != null && !strikeOut.isEmpty()) {
                                        RectF sto_Rect = AppUtil.toRectF(strikeOut.getRect());
                                        sto_Rect.union(viewRect);

                                        viewRect.set(sto_Rect);
                                    }
                                }
                                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                                Rect rect = new Rect();
                                viewRect.roundOut(rect);
                                rect.inset(-10, -10);
                                mPdfViewCtrl.refresh(pageIndex, rect);
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
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
            if (!(annot instanceof Caret)) {
                return false;
            }

            CaretAnnotHandler annotHandler = (CaretAnnotHandler) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(Annot.e_Caret);
            if (annotHandler == null) {
                return false;
            }

            return annotHandler.deleteAnnot(annot, this, false, callback);
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