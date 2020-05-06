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
package com.foxit.uiextensions.annots.line;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Line;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AbstractAnnotHandler;
import com.foxit.uiextensions.annots.AbstractToolHandler;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.DefaultAnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.IAnnotTaskResult;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.annots.common.UIAnnotFrame;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public class LineAnnotHandler implements AnnotHandler {
    protected LineRealAnnotHandler mRealAnnotHandler;
//	private LineDefaultAnnotHandler	mDefAnnotHandler;

    private PDFViewCtrl mPdfViewCtrl;
    private LineUtil    mUtil;

    public LineAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl, LineUtil util) {
        mPdfViewCtrl = pdfViewCtrl;
        mUtil = util;
        mRealAnnotHandler = new LineRealAnnotHandler(context, pdfViewCtrl, util);
//		mDefAnnotHandler = new LineDefaultAnnotHandler(context, pdfViewCtrl);
    }

    AnnotHandler getHandler(String intent) {
//		if (intent != null && intent.equals(LineConstants.INTENT_LINE_DIMENSION)) {
//			return mDefAnnotHandler;
//		}
        return mRealAnnotHandler;
    }

    public void setAnnotMenu(String intent, AnnotMenu annotMenu) {
        ((AbstractAnnotHandler) getHandler(intent)).setAnnotMenu(annotMenu);
    }

    public AnnotMenu getAnnotMenu(String intent) {
        return ((AbstractAnnotHandler) getHandler(intent)).getAnnotMenu();
    }

    public void setPropertyBar(String intent, PropertyBar propertyBar) {
        ((AbstractAnnotHandler) getHandler(intent)).setPropertyBar(propertyBar);
    }

    public PropertyBar getPropertyBar(String intent) {
        return ((AbstractAnnotHandler) getHandler(intent)).getPropertyBar();
    }

    @Override
    public int getType() {
        return mRealAnnotHandler.getType();
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        try {
            return getHandler(((Line) annot).getIntent()).annotCanAnswer(annot);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public RectF getAnnotBBox(Annot annot) {
        try {
            return getHandler(((Line) annot).getIntent()).getAnnotBBox(annot);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        try {
            return getHandler(((Line) annot).getIntent()).isHitAnnot(annot, point);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onAnnotSelected(Annot annot, boolean reRender) {
        try {
            getHandler(((Line) annot).getIntent()).onAnnotSelected(annot, reRender);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
        try {
            getHandler(((Line) annot).getIntent()).onAnnotDeselected(annot, reRender);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result) {
        getHandler(content.getIntent()).addAnnot(pageIndex, content, addUndo, result);
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        try {
            getHandler(((Line) annot).getIntent()).modifyAnnot(annot, content, addUndo, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
        try {
            getHandler(((Line) annot).getIntent()).removeAnnot(annot, addUndo, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e, Annot annot) {

        try {
            if (null == mUtil.getToolHandler(((Line) annot).getIntent())) {
                return false;
            }
            return getHandler(((Line) annot).getIntent()).onTouchEvent(pageIndex, e, annot);
        } catch (PDFException e1) {
            e1.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {

        try {
            if (null == mUtil.getToolHandler(((Line) annot).getIntent())) {
                return false;
            }
            return getHandler(((Line) annot).getIntent()).onLongPress(pageIndex, motionEvent, annot);
        } catch (PDFException e1) {
            e1.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {

        try {
            if (null == mUtil.getToolHandler(((Line) annot).getIntent())) {
                return false;
            }
            return getHandler(((Line) annot).getIntent()).onSingleTapConfirmed(pageIndex, motionEvent, annot);
        } catch (PDFException e1) {
            e1.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            return getHandler(((Line) curAnnot).getIntent()).shouldViewCtrlDraw(annot);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (annot == null || annot.getType() != Annot.e_Line) {
                return;
            }
            getHandler(((Line) annot).getIntent()).onDraw(pageIndex, canvas);
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }


    public void onDrawForControls(Canvas canvas) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this) {
            try {
                ((AbstractAnnotHandler) getHandler(((Line) annot).getIntent())).onDrawForControls(canvas);
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    public void onLanguageChanged() {
        mRealAnnotHandler.onLanguageChanged();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mRealAnnotHandler.onKeyDown(keyCode, event);
    }
}

class LineDefaultAnnotHandler extends DefaultAnnotHandler {

    public LineDefaultAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        super(context, pdfViewCtrl);
    }

    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotMenu = annotMenu;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    public void setPropertyBar(PropertyBar propertyBar) {
        mPropertyBar = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mPropertyBar;
    }

    public void onDrawForControls(Canvas canvas) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (annot != null && annot.getType() == Annot.e_Line) {
                int pageIndex = annot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    // sometimes op = scale but ctl == none, will cause crash.
                    // haven't track the reason.
                    if (mOp == UIAnnotFrame.OP_SCALE && mCtl == UIAnnotFrame.CTL_NONE)
                        return;
                    RectF bbox = UIAnnotFrame.mapBounds(mPdfViewCtrl, pageIndex, annot, mOp, mCtl,
                            mLastPt.x - mDownPt.x, mLastPt.y - mDownPt.y);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
                    mAnnotMenu.update(bbox);
                    if (mPropertyBar.isShowing()) {
                        RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), bbox);
                        mPropertyBar.update(rectF);
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        return AppAnnotUtil.isSameAnnot(curAnnot, annot) ? false : true;
    }
}

class LineRealAnnotHandler extends AbstractAnnotHandler {
    protected LineUtil           mUtil;
    protected ArrayList<Integer> mMenuText;

    protected int    mBackColor;
    protected float  mBackOpacity;
    protected PointF mBackStartPt = new PointF();
    protected PointF mBackEndPt   = new PointF();

    protected int    mBackStartingStyle;
    protected int    mBackEndingStyle;
    protected String mBackContent;

    private TextPaint mTextPaint;

    private PointF dStart, dEnd;

    public LineRealAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl, LineUtil util) {
        super(context, pdfViewCtrl, Annot.e_Line);
        mUtil = util;
        mMenuText = new ArrayList<Integer>();
        mTextPaint = new TextPaint();
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextSize(40f);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        dStart = new PointF();
        dEnd = new PointF();
    }

    protected void initialize(String intent) {
        LineToolHandler toolHandler = mUtil.getToolHandler(intent);
        mColor = toolHandler.getColor();
        mOpacity = toolHandler.getOpacity();
        mThickness = toolHandler.getThickness();
    }

    @Override
    protected AbstractToolHandler getToolHandler() {
        if (mPdfViewCtrl != null) {
            Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
            try {
                if (annot != null && annot.getType() == mType) {
                    return mUtil.getToolHandler(((Line) annot).getIntent());
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
        return mUtil.getToolHandler(LineConstants.INTENT_LINE_DEFAULT);
    }

    @Override
    public void setThickness(float thickness) {
        super.setThickness(thickness);
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        boolean isHit = false;
        try {
            PointF  startPt  = AppUtil.toPointF(((Line) annot).getStartPoint());
            PointF  stopPt   = AppUtil.toPointF(((Line) annot).getEndPoint());
            float   distance = AppDmUtil.distanceFromPointToLine(point, startPt, stopPt);
            boolean isOnLine = AppDmUtil.isPointVerticalIntersectOnLine(point, startPt, stopPt);
            if (distance < annot.getBorderInfo().getWidth() * LineUtil.ARROW_WIDTH_SCALE / 2) {
                if (isOnLine) {
                    isHit = true;
                } else if (AppDmUtil.distanceOfTwoPoints(startPt, stopPt) < annot.getBorderInfo().getWidth() * LineUtil.ARROW_WIDTH_SCALE / 2) {
                    isHit = true;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return isHit;
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean reRender) {
        try {
            mColor = (int) annot.getBorderColor();
            mOpacity = AppDmUtil.opacity255To100((int) (((Line) annot).getOpacity() * 255f + 0.5f));
            mThickness = annot.getBorderInfo().getWidth();

            mBackColor = mColor;
            mBackOpacity = ((Line) annot).getOpacity();
            mBackStartPt.set(AppUtil.toPointF(((Line) annot).getStartPoint()));
            mBackEndPt.set(AppUtil.toPointF(((Line) annot).getEndPoint()));
            mBackStartingStyle = ((Line) annot).getLineStartStyle();
            mBackEndingStyle = ((Line) annot).getLineEndStyle();
            mBackContent = annot.getContent();
            super.onAnnotSelected(annot, reRender);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
        if (!mIsModified) {
            super.onAnnotDeselected(annot, reRender);
        } else {
            LineModifyUndoItem undoItem = new LineModifyUndoItem(this, mPdfViewCtrl);
            undoItem.setCurrentValue(mSelectedAnnot);
            try {
                undoItem.mStartPt = AppUtil.toPointF(((Line) mSelectedAnnot).getStartPoint());
                undoItem.mEndPt = AppUtil.toPointF(((Line) mSelectedAnnot).getEndPoint());
                undoItem.mStartingStyle = ((Line) mSelectedAnnot).getLineStartStyle();
                undoItem.mEndingStyle = ((Line) mSelectedAnnot).getLineEndStyle();

                undoItem.mOldColor = mBackColor;
                undoItem.mOldOpacity = mBackOpacity;
                undoItem.mOldBBox = new RectF(mBackRect);
                undoItem.mOldLineWidth = mBackThickness;
                undoItem.mOldStartPt.set(mBackStartPt);
                undoItem.mOldEndPt.set(mBackEndPt);
                undoItem.mOldStartingStyle = mBackStartingStyle;
                undoItem.mOldEndingStyle = mBackEndingStyle;
                undoItem.mOldContents = mBackContent;

                if (LineConstants.INTENT_LINE_DIMENSION.equals(((Line) mSelectedAnnot).getIntent())) {
                    if (!undoItem.mStartPt.equals(mBackStartPt) || !undoItem.mEndPt.equals(mBackEndPt)) {
                        float distance = AppDmUtil.distanceOfTwoPoints(undoItem.mStartPt, undoItem.mEndPt);
                        undoItem.mContents = String.valueOf((float) (Math.round(distance * 100 * ((Line) mSelectedAnnot).getMeasureConversionFactor(0))) / 100 + " " + ((Line) mSelectedAnnot).getMeasureUnit(0));
                    }
                }

            } catch (PDFException e) {
                e.printStackTrace();
            }


            modifyAnnot(mSelectedAnnot, undoItem, false, true, reRender, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (mSelectedAnnot != ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                        resetStatus();
                    }
                }
            });
            dismissPopupMenu();
            hidePropertyBar();
        }
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e, Annot annot) {
        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);

        try {
            Line lAnnot = (Line) annot;
            int  action = e.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (pageIndex == lAnnot.getPage().getIndex()
                            && lAnnot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                        PointF startPt = new PointF(lAnnot.getStartPoint().getX(), lAnnot.getStartPoint().getY());
                        PointF stopPt  = new PointF(lAnnot.getEndPoint().getX(), lAnnot.getEndPoint().getY());

                        mPdfViewCtrl.convertPdfPtToPageViewPt(startPt, startPt, pageIndex);
                        mPdfViewCtrl.convertPdfPtToPageViewPt(stopPt, stopPt, pageIndex);
                        mCtl = mUtil.hitControlTest(startPt, stopPt, point);
                        if (mCtl != UIAnnotFrame.CTL_NONE) {
                            mTouchCaptured = true;
                            mOp = UIAnnotFrame.OP_SCALE;
                            mDownPt.set(point);
                            mLastPt.set(point);
                            return true;
                        } else {
                            PointF docPt = new PointF(point.x, point.y);
                            mPdfViewCtrl.convertPageViewPtToPdfPt(docPt, docPt, pageIndex);
                            if (isHitAnnot(lAnnot, docPt)) {
                                mTouchCaptured = true;
                                mOp = UIAnnotFrame.OP_TRANSLATE;
                                mDownPt.set(point);
                                mLastPt.set(point);
                                return true;
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mTouchCaptured && pageIndex == lAnnot.getPage().getIndex()
                            && lAnnot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                        if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
                            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                                mTouchCaptured = false;
                                mDownPt.set(0, 0);
                                mLastPt.set(0, 0);
                                mOp = UIAnnotFrame.OP_DEFAULT;
                                mCtl = UIAnnotFrame.CTL_NONE;
                                if (mSelectedAnnot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                                    RectF bbox = UIAnnotFrame.mapBounds(mPdfViewCtrl, pageIndex, lAnnot, mOp, mCtl,
                                            mLastPt.x - mDownPt.x, mLastPt.y - mDownPt.y);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
                                    mAnnotMenu.show(bbox);
                                }
                            }
                            return true;
                        } else {
                            if (mOp == UIAnnotFrame.OP_TRANSLATE) {
                                return super.onTouchEvent(pageIndex, e, annot);
                            } else if (mOp == UIAnnotFrame.OP_SCALE) {
                                float  thickness = UIAnnotFrame.getPageViewThickness(mPdfViewCtrl, pageIndex, lAnnot.getBorderInfo().getWidth());
                                PointF pointBak  = new PointF(point.x, point.y);
                                mUtil.correctPvPoint(mPdfViewCtrl, pageIndex, pointBak, thickness);
                                if (pointBak.x != mLastPt.x || pointBak.y != mLastPt.y) {
                                    if (mAnnotMenu.isShowing()) {
                                        mAnnotMenu.dismiss();
                                    }
                                    RectF  rect0, rect1;
                                    PointF startPt = new PointF(lAnnot.getStartPoint().getX(), lAnnot.getStartPoint().getY());
                                    PointF stopPt  = new PointF(lAnnot.getEndPoint().getX(), lAnnot.getEndPoint().getY());
                                    mPdfViewCtrl.convertPdfPtToPageViewPt(startPt, startPt, pageIndex);
                                    mPdfViewCtrl.convertPdfPtToPageViewPt(stopPt, stopPt, pageIndex);
                                    if (mCtl == 0) {
                                        rect0 = mUtil.getArrowBBox(mLastPt, stopPt, thickness);
                                        rect1 = mUtil.getArrowBBox(pointBak, stopPt, thickness);
                                    } else {
                                        rect0 = mUtil.getArrowBBox(startPt, mLastPt, thickness);
                                        rect1 = mUtil.getArrowBBox(startPt, pointBak, thickness);
                                    }
                                    rect1.union(rect0);
                                    mUtil.extentBoundsToContainControl(rect1);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rect1, rect1, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rect1));
                                    mLastPt.set(pointBak);
                                }

                                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                                    if (!mLastPt.equals(mDownPt)) {
                                        PointF startPt = new PointF(lAnnot.getStartPoint().getX(), lAnnot.getStartPoint().getY());
                                        PointF stopPt  = new PointF(lAnnot.getEndPoint().getX(), lAnnot.getEndPoint().getY());
                                        mPdfViewCtrl.convertPdfPtToPageViewPt(startPt, startPt, pageIndex);
                                        mPdfViewCtrl.convertPdfPtToPageViewPt(stopPt, stopPt, pageIndex);
                                        if (mCtl == 0) {
                                            startPt.set(mUtil.calculateEndingPoint(stopPt, mLastPt));
                                            mPdfViewCtrl.convertPageViewPtToPdfPt(startPt, startPt, pageIndex);
                                            lAnnot.setStartPoint(AppUtil.toFxPointF(startPt));
                                        } else {
                                            stopPt.set(mUtil.calculateEndingPoint(startPt, mLastPt));
                                            mPdfViewCtrl.convertPageViewPtToPdfPt(stopPt, stopPt, pageIndex);
                                            lAnnot.setEndPoint(AppUtil.toFxPointF(stopPt));
                                        }
                                        lAnnot.resetAppearanceStream();
                                        mIsModified = true;
                                    }
                                    mTouchCaptured = false;
                                    mDownPt.set(0, 0);
                                    mLastPt.set(0, 0);
                                    mOp = UIAnnotFrame.OP_DEFAULT;
                                    mCtl = UIAnnotFrame.CTL_NONE;
                                    if (mSelectedAnnot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                                        RectF bbox = UIAnnotFrame.mapBounds(mPdfViewCtrl, pageIndex, lAnnot, mOp, mCtl,
                                                mLastPt.x - mDownPt.x, mLastPt.y - mDownPt.y);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
                                        mAnnotMenu.show(bbox);
                                    }
                                }
                            }
                        }
                        return true;
                    }
                    break;
            }
        } catch (PDFException e1) {
            e1.printStackTrace();
        }


        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return super.onLongPress(pageIndex, motionEvent, annot);
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return super.onSingleTapConfirmed(pageIndex, motionEvent, annot);
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        return AppAnnotUtil.isSameAnnot(curAnnot, annot) ? false : true;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (annot == null || annot.getType() != mType)
                return;

            if (AppAnnotUtil.equals(mSelectedAnnot, annot) && annot.getPage().getIndex() == pageIndex) {
                Line   lAnnot  = (Line) annot;
                PointF startPt = new PointF(lAnnot.getStartPoint().getX(), lAnnot.getStartPoint().getY());
                PointF stopPt  = new PointF(lAnnot.getEndPoint().getX(), lAnnot.getEndPoint().getY());
                mPdfViewCtrl.convertPdfPtToPageViewPt(startPt, startPt, pageIndex);
                mPdfViewCtrl.convertPdfPtToPageViewPt(stopPt, stopPt, pageIndex);
                if (mOp == UIAnnotFrame.OP_TRANSLATE) {
                    float dx = mLastPt.x - mDownPt.x;
                    float dy = mLastPt.y - mDownPt.y;
                    startPt.offset(dx, dy);
                    stopPt.offset(dx, dy);
                } else if (mOp == UIAnnotFrame.OP_SCALE) {
                    if (mCtl == 0) {
                        startPt.set(mUtil.calculateEndingPoint(stopPt, mLastPt));
                    } else {
                        stopPt.set(mUtil.calculateEndingPoint(startPt, mLastPt));
                    }
                }
                float thickness = lAnnot.getBorderInfo().getWidth();
                thickness = thickness < 1.0f ? 1.0f : thickness;
                thickness = (thickness + 3) * 15.0f / 8.0f;
                thickness = UIAnnotFrame.getPageViewThickness(mPdfViewCtrl, pageIndex, thickness);
                Path path = mUtil.getLinePath(lAnnot.getIntent(), startPt, stopPt, thickness);
                setPaintProperty(mPdfViewCtrl, pageIndex, mPaint, mSelectedAnnot);
                canvas.drawPath(path, mPaint);

                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    int color   = (int) (annot.getBorderColor() | 0xFF000000);
                    int opacity = (int) (lAnnot.getOpacity() * 255f);
                    mUtil.drawControls(canvas, startPt, stopPt, color, opacity);
                }

                if (lAnnot.getIntent() != null && lAnnot.getIntent().equals(LineConstants.INTENT_LINE_DIMENSION)) {
                    mTextPaint.setTextAlign(Paint.Align.CENTER);
                    mTextPaint.setSubpixelText(true);
                    mPdfViewCtrl.convertPageViewPtToPdfPt(startPt, dStart, pageIndex);
                    mPdfViewCtrl.convertPageViewPtToPdfPt(stopPt, dEnd, pageIndex);
                    float distance = AppDmUtil.distanceOfTwoPoints(dStart, dEnd);

                    String text = String.valueOf((float) (Math.round(distance * 100 * ((Line) annot).getMeasureConversionFactor(0))) / 100
                            + " " + ((Line) annot).getMeasureUnit(0));
                    if (mCtl == 0) {
                        PointF pointF = calculateTextPosition(stopPt, mTextPaint, pageIndex, text);
                        canvas.drawText(text, pointF.x, pointF.y, mTextPaint);
                    } else {
                        PointF pointF = calculateTextPosition(startPt, mTextPaint, pageIndex, text);
                        canvas.drawText(text, pointF.x, pointF.y, mTextPaint);
                    }

                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected PointF calculateTextPosition(PointF pointF, Paint paint, int pageIndex, String str) {
        Rect rect = new Rect();
        paint.getTextBounds(str, 0, str.length(), rect);
        float fontWith   = rect.width();
        float fontHeight = rect.height();

        PointF p = new PointF();
        p.x = pointF.x;
        p.y = pointF.y - 32;

        int space = 5;
        //the text on the left
        if ((pointF.x - fontWith - space) < 0) {
            p.x = fontWith / 2 + space;
        }
        //the text on the right
        if ((mPdfViewCtrl.getPageViewWidth(pageIndex) - pointF.x) - space < fontWith) {
            p.x = pointF.x - (fontWith / 2 - (mPdfViewCtrl.getPageViewWidth(pageIndex) - pointF.x)) - space;
        }
        //the text on the top
        if (pointF.y - fontHeight - space < 0) {
            p.y = fontHeight + space;
        }
        return p;
    }

    @Override
    public void onValueChanged(long property, int value) {
        super.onValueChanged(property, value);
        if (property == PropertyBar.PROPERTY_DISTANCE) {
            ((LineToolHandler) getToolHandler()).setScaleFromUnitIndex(value);
        }
    }


    public void onDrawForControls(Canvas canvas) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (annot != null && annot.getType() == mType) {
                int pageIndex = annot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    // sometimes op = scale but ctl == none, will cause crash.
                    // haven't track the reason.
                    if (mOp == UIAnnotFrame.OP_SCALE && mCtl == UIAnnotFrame.CTL_NONE)
                        return;
                    RectF bbox = AppUtil.toRectF(annot.getRect());

                    mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
                    mAnnotMenu.update(bbox);
                    if (mPropertyBar.isShowing()) {
                        RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), bbox);
                        mPropertyBar.update(rectF);
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void addAnnot(int pageIndex, final AnnotContent content, boolean addUndo, final Event.Callback result) {
        try {
            PDFPage    page  = mPdfViewCtrl.getDoc().getPage(pageIndex);
            final Line annot = (Line) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Line, AppUtil.toFxRectF(content.getBBox())), Annot.e_Line);

            LineAddUndoItem undoItem = new LineAddUndoItem(this, mPdfViewCtrl);
            undoItem.mPageIndex = pageIndex;
            undoItem.mNM = content.getNM();
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mFlags = Annot.e_FlagPrint;
            undoItem.mColor = content.getColor();
            undoItem.mOpacity = content.getOpacity() / 255f;
            undoItem.mBBox = new RectF(content.getBBox());
            undoItem.mIntent = content.getIntent();
            undoItem.mLineWidth = content.getLineWidth();
            undoItem.mSubject = mUtil.getSubject(content.getIntent());
            if (content instanceof LineAnnotContent) {
                if (((LineAnnotContent) content).getEndingPoints().size() == 2) {
                    undoItem.mStartPt.set(((LineAnnotContent) content).getEndingPoints().get(0));
                    undoItem.mEndPt.set(((LineAnnotContent) content).getEndingPoints().get(1));
                }

                if (((LineAnnotContent) content).getEndingStyles().size() == 2) {
                    undoItem.mStartingStyle = ((LineAnnotContent) content).getEndingStyles().get(0);
                    undoItem.mEndingStyle = ((LineAnnotContent) content).getEndingStyles().get(1);
                }
            }

            // sometimes the rect from share review server is not right
            if (undoItem.mStartPt != null && undoItem.mEndPt != null) {
                RectF bbox = mUtil.getArrowBBox(undoItem.mStartPt, undoItem.mEndPt, undoItem.mLineWidth);
                undoItem.mBBox.set(new RectF(bbox.left, bbox.bottom, bbox.right, bbox.top));

                if (content.getIntent() == LineConstants.INTENT_LINE_DIMENSION) {
                    float distance = AppDmUtil.distanceOfTwoPoints(undoItem.mStartPt, undoItem.mEndPt);
                    undoItem.mContents = String.valueOf((float) (Math.round(distance * 100 * undoItem.factor)) / 100 + " " + undoItem.unit);
                } else {
                    undoItem.mContents = "";
                }
            }

            addAnnot(pageIndex, annot, undoItem, addUndo, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    protected Line addAnnot(int pageIndex, LineAddUndoItem addUndoItem, RectF bbox, final int color, final int opacity, final float thickness,
                            final PointF startPt, final PointF stopPt, final String intent, float factor, String unit, String ratio,
                            Event.Callback result) {

        addUndoItem.factor = factor;
        addUndoItem.ratio = ratio;
        addUndoItem.unit = unit;
        float distance = AppDmUtil.distanceOfTwoPoints(startPt, stopPt);
        addUndoItem.mContents = String.valueOf((float) (Math.round(distance * 100 * factor)) / 100 + " " + unit);
        return addAnnot(pageIndex, addUndoItem, bbox, color, opacity, thickness, startPt, stopPt, intent, result);

    }


    protected Line addAnnot(int pageIndex, LineAddUndoItem undoItem, RectF bbox, final int color, final int opacity, final float thickness,
                            final PointF startPt, final PointF stopPt, final String intent, Event.Callback callback) {

        try {
            PDFPage    page  = mPdfViewCtrl.getDoc().getPage(pageIndex);
            final Line annot = (Line) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Line, AppUtil.toFxRectF(bbox)), Annot.e_Line);
            undoItem.mPageIndex = pageIndex;
            undoItem.mNM = AppDmUtil.randomUUID(null);
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mFlags = Annot.e_FlagPrint;
            undoItem.mColor = color;
            undoItem.mOpacity = opacity / 255f;
            undoItem.mBBox = new RectF(bbox);
            undoItem.mIntent = intent;
            undoItem.mLineWidth = thickness;
            undoItem.mSubject = mUtil.getSubject(intent);
            undoItem.mStartPt.set(startPt);
            undoItem.mEndPt.set(stopPt);
            ArrayList<Integer> endingStyles = mUtil.getEndingStyles(intent);
            if (endingStyles != null) {
                undoItem.mStartingStyle = endingStyles.get(0);
                undoItem.mEndingStyle = endingStyles.get(1);
            }

            addAnnot(pageIndex, annot, undoItem, true, callback);
            return annot;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void addAnnot(int pageIndex, Line annot, LineUndoItem undoItem, boolean addUndo, final Event.Callback result) {

        final LineEvent event = new LineEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, annot, mPdfViewCtrl);
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
            if (result != null) {
                result.result(event, true);
            }
            return;
        }
        handleAddAnnot(pageIndex, annot, event, addUndo, new IAnnotTaskResult<PDFPage, Annot, Void>() {
            @Override
            public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
                if (result != null) {
                    result.result(event, success);
                }
            }
        });
    }

    @Override
    public void modifyAnnot(final Annot annot, final AnnotContent content, boolean addUndo, Event.Callback result) {
        LineModifyUndoItem undoItem = new LineModifyUndoItem(this, mPdfViewCtrl);
        undoItem.setCurrentValue(content);

        try {
            Line line = (Line) annot;

            undoItem.mStartPt.set(AppUtil.toPointF(line.getStartPoint()));
            undoItem.mEndPt.set(AppUtil.toPointF(line.getEndPoint()));
            undoItem.mStartingStyle = line.getLineStartStyle();
            undoItem.mEndingStyle = line.getLineEndStyle();

            undoItem.mOldContents = annot.getContent();
            undoItem.mOldColor = (int) line.getBorderColor();
            undoItem.mOldOpacity = line.getOpacity();
            undoItem.mOldBBox = new RectF(AppUtil.toRectF(line.getRect()));
            undoItem.mOldLineWidth = line.getBorderInfo().getWidth();
            undoItem.mOldStartPt.set(AppUtil.toPointF(line.getStartPoint()));
            undoItem.mOldEndPt.set(AppUtil.toPointF(line.getEndPoint()));
            undoItem.mOldStartingStyle = line.getLineStartStyle();
            undoItem.mOldEndingStyle = line.getLineEndStyle();
        } catch (PDFException e) {
            e.printStackTrace();
        }

        modifyAnnot(annot, undoItem, false, addUndo, true, result);
    }

    protected void modifyAnnot(Annot annot, LineUndoItem undoItem, boolean useOldValue, boolean addUndo, boolean reRender,
                               final Event.Callback result) {

        LineEvent event = new LineEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Line) annot, mPdfViewCtrl);
        event.useOldValue = useOldValue;
        handleModifyAnnot(annot, event, addUndo, reRender,
                new IAnnotTaskResult<PDFPage, Annot, Void>() {
                    @Override
                    public void onResult(boolean success, PDFPage p1, Annot p2, Void p3) {
                        if (result != null) {
                            result.result(null, success);
                        }
                    }
                });
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, final Event.Callback result) {
        LineDeleteUndoItem undoItem = new LineDeleteUndoItem(this, mPdfViewCtrl);
        undoItem.setCurrentValue(annot);

        try {
            String intent = ((Line) annot).getIntent();
            if (intent != null && intent.equals(LineConstants.INTENT_LINE_DIMENSION)) {
                undoItem.unit = ((Line) annot).getMeasureUnit(0);
                undoItem.ratio = ((Line) annot).getMeasureRatio();
                undoItem.factor = ((Line) annot).getMeasureConversionFactor(0);
            }
            undoItem.mStartPt = AppUtil.toPointF(((Line) annot).getStartPoint());
            undoItem.mEndPt = AppUtil.toPointF(((Line) annot).getEndPoint());
            undoItem.mStartingStyle = ((Line) annot).getLineStartStyle();
            undoItem.mEndingStyle = ((Line) annot).getLineEndStyle();
        } catch (PDFException e) {
            e.printStackTrace();
        }

        removeAnnot(annot, undoItem, addUndo, result);
    }

    protected void removeAnnot(Annot annot, LineDeleteUndoItem undoItem, boolean addUndo, final Event.Callback result) {
        LineEvent event = new LineEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Line) annot, mPdfViewCtrl);
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
            try {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(annot.getPage(), annot);
                if (result != null) {
                    result.result(event, true);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
            return;
        }
        handleRemoveAnnot(annot, event, addUndo,
                new IAnnotTaskResult<PDFPage, Void, Void>() {
                    @Override
                    public void onResult(boolean success, PDFPage p1, Void p2, Void p3) {
                        if (result != null) {
                            result.result(null, success);
                        }
                    }
                });
    }

    @Override
    protected ArrayList<Path> generatePathData(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot) {
        Line lAnnot = (Line) annot;
        try {
            float thickness = lAnnot.getBorderInfo().getWidth();
            thickness = thickness < 1.0f ? 1.0f : thickness;
            thickness = (thickness + 3) * 15.0f / 8.0f;
            thickness = UIAnnotFrame.getPageViewThickness(mPdfViewCtrl, pageIndex, thickness);
            PointF startPt = new PointF();
            PointF stopPt  = new PointF();
            startPt.set(AppUtil.toPointF(lAnnot.getStartPoint()));
            stopPt.set(AppUtil.toPointF(lAnnot.getEndPoint()));

            pdfViewCtrl.convertPdfPtToPageViewPt(startPt, startPt, pageIndex);
            pdfViewCtrl.convertPdfPtToPageViewPt(stopPt, stopPt, pageIndex);
            Path            path  = mUtil.getLinePath(lAnnot.getIntent(), startPt, stopPt, thickness);
            ArrayList<Path> paths = new ArrayList<Path>();
            paths.add(path);
            return paths;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void transformAnnot(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot, Matrix matrix) {
        try {

            float[] pts = {0, 0};

            Line   lAnnot  = (Line) annot;
            PointF startPt = AppUtil.toPointF(lAnnot.getStartPoint());
            PointF stopPt  = AppUtil.toPointF(lAnnot.getEndPoint());

            pdfViewCtrl.convertPdfPtToPageViewPt(startPt, startPt, pageIndex);
            pdfViewCtrl.convertPdfPtToPageViewPt(stopPt, stopPt, pageIndex);

            pts[0] = startPt.x;
            pts[1] = startPt.y;
            matrix.mapPoints(pts);
            startPt.set(pts[0], pts[1]);
            pdfViewCtrl.convertPageViewPtToPdfPt(startPt, startPt, pageIndex);

            pts[0] = stopPt.x;
            pts[1] = stopPt.y;
            matrix.mapPoints(pts);
            stopPt.set(pts[0], pts[1]);
            pdfViewCtrl.convertPageViewPtToPdfPt(stopPt, stopPt, pageIndex);

            ((Line) annot).setStartPoint(AppUtil.toFxPointF(startPt));
            ((Line) annot).setEndPoint(AppUtil.toFxPointF(stopPt));
            annot.resetAppearanceStream();
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void resetStatus() {
        mBackRect = null;
        mSelectedAnnot = null;
        mIsModified = false;
    }

    @Override
    protected void showPopupMenu() {
        try {
            Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
            if (curAnnot == null || curAnnot.isEmpty() || curAnnot.getType() != Annot.e_Line)
                return;

            reloadPopupMenuString((Line) curAnnot);
            mAnnotMenu.setMenuItems(mMenuText);
            RectF bbox      = AppUtil.toRectF(curAnnot.getRect());
            int   pageIndex = curAnnot.getPage().getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, pageIndex);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(bbox, bbox, pageIndex);
            mAnnotMenu.show(bbox);
            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int flag) {
                    if (mSelectedAnnot == null) return;
                    if (flag == AnnotMenu.AM_BT_COMMENT) { // comment
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        UIAnnotReply.showComments(mPdfViewCtrl, ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mSelectedAnnot);
                    } else if (flag == AnnotMenu.AM_BT_REPLY) { // reply
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        UIAnnotReply.replyToAnnot(mPdfViewCtrl, ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mSelectedAnnot);
                    } else if (flag == AnnotMenu.AM_BT_DELETE) { // delete
                        if (mSelectedAnnot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                            removeAnnot(mSelectedAnnot, true, null);
                        }
                    } else if (flag == AnnotMenu.AM_BT_STYLE) { // line color
                        dismissPopupMenu();
                        showPropertyBar(PropertyBar.PROPERTY_COLOR);
                    } else if (flag == AnnotMenu.AM_BT_FLATTEN) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        UIAnnotFlatten.flattenAnnot(mPdfViewCtrl, mSelectedAnnot);
                    }
                }
            });
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void dismissPopupMenu() {
        mAnnotMenu.setListener(null);
        mAnnotMenu.dismiss();
    }

    @Override
    protected long getSupportedProperties() {
        if (((LineToolHandler) getToolHandler()).getIntent().equals(LineConstants.INTENT_LINE_DIMENSION)) {
            return mUtil.getSupportedProperties() | PropertyBar.PROPERTY_DISTANCE_DISPLAY;
        }
        return mUtil.getSupportedProperties();
    }

    @Override
    protected void setPropertyBarProperties(PropertyBar propertyBar) {
        try {
            if (mPdfViewCtrl != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() != null) {
                Annot  annot  = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
                String intent = ((Line) annot).getIntent();

                if (intent != null && (intent.equals(LineConstants.INTENT_LINE_ARROW) || intent.equals(LineConstants.INTENT_LINE_DIMENSION))) {
                    int[] colors = new int[PropertyBar.PB_COLORS_ARROW.length];
                    System.arraycopy(PropertyBar.PB_COLORS_ARROW, 0, colors, 0, colors.length);
                    colors[0] = mPropertyBar.PB_COLORS_ARROW[0];
                    propertyBar.setColors(colors);
                    if (intent.equals(LineConstants.INTENT_LINE_DIMENSION)) {
                        propertyBar.setDistanceScale(((Line) annot).getMeasureRatio().split(" "));
                    }
                } else {
                    int[] colors = new int[PropertyBar.PB_COLORS_LINE.length];
                    System.arraycopy(PropertyBar.PB_COLORS_LINE, 0, colors, 0, colors.length);
                    colors[0] = mPropertyBar.PB_COLORS_LINE[0];
                    propertyBar.setColors(colors);
                }
            }
            super.setPropertyBarProperties(propertyBar);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void reloadPopupMenuString(Line line) {
        mMenuText.clear();
//		if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
//			mMenuText.add(AnnotMenu.AM_BT_STYLE);
//			mMenuText.add(AnnotMenu.AM_BT_COMMENT);
//			mMenuText.add(AnnotMenu.AM_BT_REPLY);
//			mMenuText.add(AnnotMenu.AM_BT_FLATTEN);
//			if (!(AppAnnotUtil.isLocked(line) || AppAnnotUtil.isReadOnly(line))) {
//				mMenuText.add(AnnotMenu.AM_BT_DELETE);
//			}
//		} else {
//			mMenuText.add(AnnotMenu.AM_BT_COMMENT);
//		}
        mMenuText.add(AnnotMenu.AM_BT_DELETE);
    }

    public void onLanguageChanged() {
        mMenuText.clear();
    }

    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotMenu = annotMenu;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    public void setPropertyBar(PropertyBar propertyBar) {
        mPropertyBar = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mPropertyBar;
    }
}
