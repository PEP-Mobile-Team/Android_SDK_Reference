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
package com.foxit.uiextensions.annots.redaction;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.addon.Redaction;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.common.fxcrt.RectFArray;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.DefaultAppearance;
import com.foxit.sdk.pdf.annots.QuadPoints;
import com.foxit.sdk.pdf.annots.QuadPointsArray;
import com.foxit.sdk.pdf.annots.Redact;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContentAbs;
import com.foxit.uiextensions.config.uisettings.annotations.annots.RedactConfig;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

public class RedactToolHandler implements ToolHandler {

    private PDFViewCtrl mPdfViewCtrl;
    private Context mContext;

    private Paint mPaint;
    private RectF mSelectRect = new RectF();

    private boolean mTouchCaptured = false;
    private boolean mbAreaSelect = false;
    private int mLastPageIndex = -1;

    private PointF mStartPoint = new PointF(0, 0);
    private PointF mStopPoint = new PointF(0, 0);
    private PointF mDownPoint = new PointF(0, 0);

    public RedactToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        this.mContext = context;
        this.mPdfViewCtrl = pdfViewCtrl;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2f);
        mPaint.setColor(PropertyBar.PB_COLORS_REDACT[6]);
    }

    @Override
    public String getType() {
        return TH_TYPE_REDACT;
    }

    @Override
    public void onActivate() {
        mLastPageIndex = -1;
    }

    @Override
    public void onDeactivate() {
    }

    private RectF mAreaDrawRect = new RectF();
    private RectF mPdfAreaRect = new RectF();
    private int mPageView_W;
    private int mPageView_H;

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        mbAreaSelect = true;
        PointF disPoint = new PointF(motionEvent.getX(), motionEvent.getY());
        PointF pvPoint = new PointF();
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(disPoint, pvPoint, pageIndex);
        float x = pvPoint.x;
        float y = pvPoint.y;

        int action = motionEvent.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mTouchCaptured && mLastPageIndex == -1 || mLastPageIndex == pageIndex) {
                    mStartPoint.x = x;
                    mStartPoint.y = y;
                    mStopPoint.x = x;
                    mStopPoint.y = y;
                    mDownPoint.set(x, y);
                    mSelectRect.set(x, y, 0, 0);

                    mPageView_W = mPdfViewCtrl.getPageViewWidth(pageIndex);
                    mPageView_H = mPdfViewCtrl.getPageViewHeight(pageIndex);

                    mTouchCaptured = true;
                    if (mLastPageIndex == -1) {
                        mLastPageIndex = pageIndex;
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!mTouchCaptured || mLastPageIndex != pageIndex)
                    break;
                if (!mDownPoint.equals(x, y)) {
                    mStopPoint.x = x;
                    mStopPoint.y = y;

                    onTouchMove((int) x, (int) y);
                    mAreaDrawRect.set(mSelectRect);
                    invalidateTouch(mAreaDrawRect, pageIndex);
                    mDownPoint.set(x, y);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mTouchCaptured || mLastPageIndex != pageIndex)
                    break;
                if (!mStartPoint.equals(mStopPoint.x, mStopPoint.y)) {
                    mPdfViewCtrl.convertPageViewRectToPdfRect(mAreaDrawRect, mPdfAreaRect, pageIndex);
                    addAnnot(pageIndex, null, true, true, null);
                } else {
                    mStartPoint.set(0, 0);
                    mStopPoint.set(0, 0);
                    mDownPoint.set(0, 0);

                    mAreaDrawRect.setEmpty();
                    mSelectRect.setEmpty();
                    mPdfAreaRect.setEmpty();

                    mTouchCaptured = false;
                    mLastPageIndex = -1;
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                }
                return true;
            default:
                break;

        }
        return false;
    }


    private RectF mBBoxRect = new RectF();

    private void invalidateTouch(RectF rectF, int pageIndex) {
        if (rectF == null) return;
        RectF rBBox = new RectF(rectF);
        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rBBox, rBBox, pageIndex);
        RectF rCalRectF = AppUtil.calculateRect(mBbox, mBBoxRect);
        rCalRectF.roundOut(mInvalidateRect);
        mPdfViewCtrl.invalidate(mInvalidateRect);
        mBBoxRect.set(rBBox);
    }

    private void onTouchMove(int x, int y) {
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > mPageView_W) x = mPageView_W;
        if (y > mPageView_H) y = mPageView_H;

        if (x >= mStartPoint.x && y >= mStartPoint.y) {
            //4
            mSelectRect.set(mStartPoint.x, mStartPoint.y, x, y);
        }

        if (x >= mStartPoint.x && y <= mStartPoint.y) {
            //2
            mSelectRect.set(mStartPoint.x, y, x, mStartPoint.y);
        }

        if (x <= mStartPoint.x && y >= mStartPoint.y) {
            //3
            mSelectRect.set(x, mStartPoint.y, mStartPoint.x, y);
        }

        if (x <= mStartPoint.x && y <= mStartPoint.y) {
            //1
            mSelectRect.set(x, y, mStartPoint.x, mStartPoint.y);
        }
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean isContinueAddAnnot() {
        return false;
    }

    @Override
    public void setContinueAddAnnot(boolean continueAddAnnot) {
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (mbAreaSelect && mLastPageIndex == pageIndex) {
            canvas.save();
            canvas.drawRect(mSelectRect, mPaint);
            canvas.restore();
        }
    }

    private RectF mBbox = new RectF();
    private Rect mInvalidateRect = new Rect();

    protected void addAnnot(final int pageIndex, AnnotContent contentSupplier, final boolean addUndo, final boolean isAreaSelect, final Event.Callback result) {
        try {
            PDFDoc doc = mPdfViewCtrl.getDoc();
            Redaction redaction = new Redaction(doc);

            final RedactAddUndoItem undoItem = new RedactAddUndoItem(mPdfViewCtrl);
            final Redact annot;
            mbAreaSelect = isAreaSelect;

            if (isAreaSelect) {
                RectFArray rect_array = new RectFArray();
                rect_array.add(AppUtil.toFxRectF(mPdfAreaRect));
                annot = redaction.markRedactAnnot(doc.getPage(pageIndex), rect_array);
                undoItem.mRectFArray = rect_array;
            } else {
                TextMarkupContentAbs tmSelector = TextMarkupContentAbs.class.cast(contentSupplier);
                RectFArray rect_array = new RectFArray();
                QuadPointsArray quadPointsArray = new QuadPointsArray();
                QuadPoints quadPoint = null;
                for (int i = 0; i < tmSelector.getTextSelector().getRectFList().size(); i++) {
                    RectF rect = tmSelector.getTextSelector().getRectFList().get(i);
                    rect_array.add(AppUtil.toFxRectF(rect));

                    quadPoint = new QuadPoints();
                    quadPoint.setFirst(AppUtil.toFxPointF(rect.left, rect.top));
                    quadPoint.setSecond(AppUtil.toFxPointF(rect.right, rect.top));
                    quadPoint.setThird(AppUtil.toFxPointF(rect.left, rect.bottom));
                    quadPoint.setFourth(AppUtil.toFxPointF(rect.right, rect.bottom));
                    quadPointsArray.add(quadPoint);
                }
                undoItem.mQuadPointsArray = quadPointsArray;
                undoItem.mRectFArray = rect_array;

                annot = redaction.markRedactAnnot(doc.getPage(pageIndex), rect_array);
            }
            undoItem.mContents = "Redaction";
            undoItem.mNM = AppDmUtil.randomUUID(null);
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mPageIndex = pageIndex;
            undoItem.mSubject = "Redact";
            undoItem.mFlags = Annot.e_FlagPrint;

            final UIExtensionsManager uiExtensionsManager = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager());
            RedactConfig config = uiExtensionsManager.getConfig().uiSettings.annotations.redaction;
            undoItem.mFillColor = config.fillColor;
            undoItem.mBorderColor = PropertyBar.PB_COLORS_REDACT[6];
            undoItem.mDaFlags = DefaultAppearance.e_FlagFont
                    | DefaultAppearance.e_FlagTextColor
                    | DefaultAppearance.e_FlagFontSize;
            undoItem.mOverlayText = "";
            undoItem.mTextColor = config.textColor;
            undoItem.mFontSize = config.textSize;
            undoItem.mFont = getSupportFont(config.textFace);

            RedactEvent event = new RedactEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        try {
                            uiExtensionsManager.getDocumentManager().onAnnotAdded(mPdfViewCtrl.getDoc().getPage(pageIndex), annot);
                            if (addUndo) {
                                uiExtensionsManager.getDocumentManager().addUndoItem(undoItem);
                            }
                            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                                RectF rectF = AppUtil.toRectF(annot.getRect());
                                mBbox.set(rectF);
                                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, mBbox, pageIndex);
                                mBbox.roundOut(mInvalidateRect);
                                mPdfViewCtrl.refresh(pageIndex, mInvalidateRect);
                            }

                            if (isAreaSelect) {
                                uiExtensionsManager.getDocumentManager().setCurrentAnnot(annot);
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                    }

                    mAreaDrawRect.setEmpty();
                    mSelectRect.setEmpty();
                    mPdfAreaRect.setEmpty();
                    mDownPoint.set(0, 0);
                    mTouchCaptured = false;
                    mLastPageIndex = -1;
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                    if (result != null) {
                        result.result(null, success);
                    }
                }
            });

            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
    }


    private Font getSupportFont(String name) {
        Font font = null;
        try {
            if (name == null) {
                font = new Font(Font.e_StdIDCourier);
            } else if (name.equals("Courier")) {
                font = new Font(Font.e_StdIDCourier);
            } else if (name.equals("Helvetica")) {
                font = new Font(Font.e_StdIDHelvetica);
            } else if (name.equals("Times")) {
                font = new Font(Font.e_StdIDTimes);
            } else if (!name.equalsIgnoreCase("Courier")
                    && !name.equalsIgnoreCase("Helvetica")
                    && !name.equalsIgnoreCase("Times")) {
                font = new Font(Font.e_StdIDCourier);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return font;
    }

}
