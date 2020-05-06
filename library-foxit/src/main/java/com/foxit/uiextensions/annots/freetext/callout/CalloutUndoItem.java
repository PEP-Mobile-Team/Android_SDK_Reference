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
package com.foxit.uiextensions.annots.freetext.callout;

import android.graphics.PointF;
import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;


public abstract class CalloutUndoItem extends AnnotUndoItem {
    private static final long serialVersionUID = 1L;

    public Font mFont = null;
    public int mFontId;
    public float mFontSize = 0.0f;
    public int mTextColor;
    public int mDaFlags;
    public PointF mStartingPt = new PointF();
    public PointF mKneePt = new PointF();
    public PointF mEndingPt = new PointF();
    public RectF mTextBBox = new RectF();
    public int mBorderType;
    public ArrayList<String> mComposedText = new ArrayList<String>();
    public int mTextLineCount;
    public int mRotation;

    public CalloutUndoItem(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    public int getFontStdID() {
        return mFontId;
    }

    public void setFontStdID(int fontStdId) {
        mFontId = fontStdId;
    }

    public float getFontSize() {
        return mFontSize;
    }

    public void setFontSize(float fontSize) {
        mFontSize = fontSize;
    }

    public PointF getStartingPt() {
        return mStartingPt;
    }

    public void setStartingPt(PointF point) {
        mStartingPt = point;
    }

    public PointF getKneePt() {
        return mKneePt;
    }

    public void setKneePt(PointF kneePt) {
        mEndingPt.set(kneePt.x, kneePt.y);
    }

    public PointF getEndingPt() {
        return mEndingPt;
    }

    public void setEndingPt(PointF endPt) {
        mEndingPt.set(endPt.x, endPt.y);
    }

    public void setTextBBox(RectF rect) {
        mTextBBox.set(rect);
    }

    public RectF getTextBBox() {
        return mTextBBox;
    }

    public void setBorderType(int borderType) {
        mBorderType = borderType;
    }

    public int getBorderType() {
        return mBorderType;
    }

    public void setComposedText(ArrayList<String> composedText) {
        mComposedText = composedText;
        mTextLineCount = mComposedText.size();
    }

    public String getTextByIndex(int index) {
        return mComposedText.get(index);
    }

    @Override
    public void setCurrentValue(AnnotContent content) {
        super.setCurrentValue(content);
        ICalloutAnnotContent lContent = (ICalloutAnnotContent) content;
        if (lContent.getContents() == null || lContent.getContents().equals("")) {
            mContents = " ";
        } else {
            mContents = lContent.getContents();
        }
        if (lContent.getFontName() == null || lContent.getFontName().equals("")) {
            mFontId = Font.e_StdIDCourier;
        } else {
            if (!lContent.getFontName().startsWith("Cour") && !lContent.getFontName().equalsIgnoreCase("Courier")
                    && !lContent.getFontName().startsWith("Helv") && !lContent.getFontName().equalsIgnoreCase("Helvetica")
                    && !lContent.getFontName().startsWith("Time") && !lContent.getFontName().equalsIgnoreCase("Times")) {
                mFontId = Font.e_StdIDCourier;
            } else {
                if (lContent.getFontName().equals("Courier")){
                    mFontId = Font.e_StdIDCourier;
                } else if(lContent.getFontName().equals("Helvetica")){
                    mFontId = Font.e_StdIDHelvetica;
                } else if(lContent.getFontName().equals("Times")){
                    mFontId = Font.e_StdIDTimes;
                } else {
                    mFontId = Font.e_StdIDCourier;
                }
            }
        }
        if (lContent.getFontSize() == 0) {
            mFontSize = 24;
        } else {
            mFontSize = lContent.getFontSize();
        }
        mStartingPt = lContent.getStartPoint();
        mKneePt = lContent.getKneePoint();
        mEndingPt = lContent.getEndPoint();
        mTextBBox = lContent.getTextBBox();
        mBorderType = lContent.getBorderType();
    }
}

class CalloutAddUndoItem extends CalloutUndoItem {
    private static final long serialVersionUID = 1L;

    public CalloutAddUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        final CalloutDeleteUndoItem delUndoItem = new CalloutDeleteUndoItem(mPdfViewCtrl);
        delUndoItem.mNM = this.mNM;
        delUndoItem.mPageIndex = this.mPageIndex;
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof FreeText)) {
                return false;
            }

            if (((FreeText) annot).getIntent() == null
                    || !((FreeText) annot).getIntent().equals("FreeTextCallout")) {
                return false;
            }
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);

            final CalloutEvent delEvent = new CalloutEvent(EditAnnotEvent.EVENTTYPE_DELETE, delUndoItem, (FreeText) annot, mPdfViewCtrl);
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            EditAnnotTask task = new EditAnnotTask(delEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
                        }
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            RectF annotRect = new RectF(mBBox);
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, mPageIndex);
                            mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(annotRect));
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {

        }
        return false;
    }

    @Override
    public boolean redo() {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_FreeText, AppUtil.toFxRectF(mBBox)), Annot.e_FreeText);
            CalloutEvent event = new CalloutEvent(EditAnnotEvent.EVENTTYPE_ADD, this, (FreeText) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
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
            });
            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {

        }
        return false;
    }

}

class CalloutDeleteUndoItem extends CalloutUndoItem {
    private static final long serialVersionUID = 1L;

    public CalloutDeleteUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo(Event.Callback callback) {
        final CalloutAddUndoItem addUndoItem = new CalloutAddUndoItem(mPdfViewCtrl);
//        addUndoItem.setCurrentValue(this);
        addUndoItem.mPageIndex = this.mPageIndex;
        addUndoItem.mNM = this.mNM;
        addUndoItem.mColor = this.mColor;
        addUndoItem.mOpacity = this.mOpacity;
        addUndoItem.mFont = this.mFont;
        addUndoItem.mFontSize = this.mFontSize;
        addUndoItem.mBorderType = this.mBorderType;
        addUndoItem.mTextColor = mTextColor;
        addUndoItem.mDaFlags = mDaFlags;
        addUndoItem.mBBox = this.mBBox;
        addUndoItem.mAuthor = this.mAuthor;
        addUndoItem.mContents = this.mContents;
        addUndoItem.mTextBBox = this.mTextBBox;
        addUndoItem.mStartingPt = this.mStartingPt;
        addUndoItem.mKneePt = this.mKneePt;
        addUndoItem.mEndingPt = this.mEndingPt;
        addUndoItem.mComposedText = this.mComposedText;
        addUndoItem.mTextLineCount = this.mTextLineCount;
        addUndoItem.mModifiedDate = this.mModifiedDate;
        addUndoItem.mFlags = this.mFlags;
        addUndoItem.mSubject = this.mSubject;
        addUndoItem.mIntent = this.mIntent;
        addUndoItem.mRotation = mRotation;
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final FreeText annot = (FreeText) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_FreeText, AppUtil.toFxRectF(mBBox)), Annot.e_FreeText);

            CalloutEvent addEvent = new CalloutEvent(EditAnnotEvent.EVENTTYPE_ADD, addUndoItem, annot, mPdfViewCtrl);
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
            if (AppUtil.isEmpty(intent) || !intent.equals("FreeTextCallout")) return false;

            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            final CalloutEvent delEvent = new CalloutEvent(EditAnnotEvent.EVENTTYPE_DELETE, this, (FreeText) annot, mPdfViewCtrl);
            if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
                if (callback != null) {
                    callback.result(delEvent, true);
                }
                return true;
            }
            EditAnnotTask task = new EditAnnotTask(delEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {

                        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
                        }
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            RectF annotRect = new RectF(mBBox);
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRect, annotRect, mPageIndex);
                            mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(annotRect));
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

class CalloutModifyUndoItem extends CalloutUndoItem {
    private static final long serialVersionUID = 1L;
    public int mLastColor;
    public float mLastOpacity;
    public Font mLastFont;
    public float mLastFontSize;
    public RectF mLastBBox;
    public String mLastContent;
    public RectF mLastTextBBox;
    public PointF mLastStartingPt;
    public PointF mLastKneePt;
    public PointF mLastEndingPt;
    public int mLastBorderType;
    public ArrayList<String> mLastComposedText = new ArrayList<String>();

    public CalloutModifyUndoItem(PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);
    }

    @Override
    public boolean undo() {
        final CalloutModifyUndoItem modifyUndoItem = new CalloutModifyUndoItem(mPdfViewCtrl);
        modifyUndoItem.mPageIndex = this.mPageIndex;
        modifyUndoItem.mNM = this.mNM;
        modifyUndoItem.mColor = this.mLastColor;
        modifyUndoItem.mTextColor = this.mLastColor;
        modifyUndoItem.mOpacity = this.mLastOpacity;
        modifyUndoItem.mFont = this.mLastFont;
        modifyUndoItem.mFontSize = this.mLastFontSize;
        modifyUndoItem.mAuthor = this.mAuthor;
        modifyUndoItem.mContents = this.mLastContent;
        modifyUndoItem.mBBox = this.mLastBBox;
        modifyUndoItem.mModifiedDate = this.mModifiedDate;
        modifyUndoItem.mTextBBox = this.mLastTextBBox;
        modifyUndoItem.mStartingPt = this.mLastStartingPt;
        modifyUndoItem.mKneePt = this.mLastKneePt;
        modifyUndoItem.mEndingPt = this.mLastEndingPt;
        modifyUndoItem.mBorderType = this.mLastBorderType;
        modifyUndoItem.mComposedText = this.mLastComposedText;
        modifyUndoItem.mTextLineCount = this.mLastComposedText.size();
        modifyUndoItem.mSubject = this.mOldSubject;
        return modifyAnnot(modifyUndoItem);
    }

    @Override
    public boolean redo() {
        return modifyAnnot(this);
    }

    private boolean modifyAnnot(CalloutModifyUndoItem undoItem) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(mPageIndex);
            final Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnot(page, mNM);
            if (!(annot instanceof FreeText)) {
                return false;
            }

            if (((FreeText) annot).getIntent() == null
                    || !((FreeText) annot).getIntent().equals("FreeTextCallout")) {
                return false;
            }

            final RectF oldBbox = AppUtil.toRectF(annot.getRect());

            CalloutEvent modifyEvent = new CalloutEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (FreeText) annot, mPdfViewCtrl);
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
