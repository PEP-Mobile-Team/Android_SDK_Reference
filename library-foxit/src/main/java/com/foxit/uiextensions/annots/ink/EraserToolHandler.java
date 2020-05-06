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
package com.foxit.uiextensions.annots.ink;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Ink;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AbstractToolHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.controls.propertybar.MoreTools;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.PropertyCircleItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.PropertyCircleItemImp;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;
import java.util.Collections;

import static com.foxit.uiextensions.utils.AppDmUtil.rectFToRect;


class EraserToolHandler extends AbstractToolHandler {
    private Paint mEraserPaint;
    private Paint mPaint;
    protected float mRadius = 15.0f;
    private int mCtlPtRadius = 5;

    private ArrayList<AnnotInfo> mRootList;
    private ArrayList<Path> mPaths;

    private boolean mTouchCaptured = false;
    private int mCapturedPage = -1;
    private PointF mLastPt = new PointF();

    private PropertyCircleItem mPropertyItem;
    private IBaseItem mOKItem;

    public EraserToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        super(context, pdfViewCtrl, ToolHandler.TH_TYPE_ERASER, "ERASER");

        mCtlPtRadius = AppDisplay.getInstance(context).dp2px((float) mCtlPtRadius);
        mRootList = new ArrayList<AnnotInfo>();
        mPaths = new ArrayList<Path>();

        mEraserPaint = new Paint();
        mEraserPaint.setStyle(Style.STROKE);
        mEraserPaint.setAntiAlias(true);
        mEraserPaint.setDither(true);
        mEraserPaint.setColor(Color.RED);

        mPaint = new Paint();
        mPaint.setStyle(Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mColor = Color.LTGRAY;

        mUiExtensionsManager.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                if (!mUiExtensionsManager.getDocumentManager().canAddAnnot()) return;
                if (EraserToolHandler.this != mUiExtensionsManager.getCurrentToolHandler()) {
                    mUiExtensionsManager.setCurrentToolHandler(EraserToolHandler.this);
                } else {
                    mUiExtensionsManager.setCurrentToolHandler(null);
                }

                if (mUiExtensionsManager.getCurrentToolHandler() != null) {
                    mUiExtensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                } else if (mUiExtensionsManager.getState() == ReadStateConfig.STATE_ANNOTTOOL){
                    mUiExtensionsManager.changeState(ReadStateConfig.STATE_EDIT);
                }
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_ERASER;
            }
        });
    }

    void initUiElements() {
    }

    @Override
    public void onActivate() {
        mCapturedPage = -1;
        mRootList.clear();
        mPaths.clear();
        resetPropertyBar();
        resetAnnotBar();
    }

    private void resetPropertyBar() {
        mPropertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, mThickness);
        mPropertyBar.reset(getSupportedProperties());
        mPropertyBar.setPropertyChangeListener(this);
    }

    private void resetAnnotBar(){
        mUiExtensionsManager.getMainFrame().getToolSetBar().removeAllItems();

        mOKItem = new BaseItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ITEM_ANNOT_BAR_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUiExtensionsManager.changeState(ReadStateConfig.STATE_EDIT);
                mUiExtensionsManager.setCurrentToolHandler(null);
            }
        });

        mPropertyItem = new PropertyCircleItemImp(mContext) {

            @Override
            public void onItemLayout(int l, int t, int r, int b) {

                if (EraserToolHandler.this == mUiExtensionsManager.getCurrentToolHandler()) {
                    if (mPropertyBar.isShowing()) {
                        Rect rect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                        mPropertyBar.update(new RectF(rect));
                    }
                }
            }
        };
        mPropertyItem.setTag(ToolbarItemConfig.ITEM_ANNOT_PROPERTY);
        mPropertyItem.setCentreCircleColor(mColor);

        final Rect rect = new Rect();
        mPropertyItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mPropertyBar.setArrowVisible(AppDisplay.getInstance(mContext).isPad());
                mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                mPropertyBar.show(new RectF(rect), true);
            }
        });

        mIsContinuousCreate = true;
        mUiExtensionsManager.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mUiExtensionsManager.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
    }

    private Ink tempAnnot;
    private RectF mInvalidateRect = new RectF();
    @Override
    public void onDeactivate() {
        // if the ink annotation is not really modified,
        // remove it from the list, and update data in JNI.
        RectF noModifyRect = new RectF();
        boolean hasNoModify = false;
        if (mRootList.size() > 1) {
            for (int k = 0; k < mRootList.size(); k++) {
                AnnotInfo annotInfo = mRootList.get(k);
                if (!annotInfo.mModifyFlag) {
                    mRootList.remove(k);
                    invalidateJniAnnots(annotInfo, 1, null);
                    try {
                        if (!hasNoModify) {
                            noModifyRect.set(AppUtil.toRectF(annotInfo.mAnnot.getRect()));
                        } else {
                            union(noModifyRect, AppUtil.toRectF(annotInfo.mAnnot.getRect()));
                        }

                        hasNoModify = true;
                    } catch (PDFException e) {
                        return;
                    }
                }
            }
        }

        if (!mRootList.isEmpty()) {
            Collections.sort(mRootList);
            try {
                final int pageIndex = mRootList.get(0).mAnnot.getPage().getIndex();
                RectF rect = AppUtil.toRectF(mRootList.get(0).mAnnot.getRect());
                RectF rmRect = new RectF(rect);
                EraserUndoItem undoItems = new EraserUndoItem(mPdfViewCtrl);
                undoItems.mPageIndex = pageIndex;
                boolean isLast = false;
                for (int i = mRootList.size() - 1; i >= 0; i--) {
                    if (i == 0) {
                        isLast = true;
                    }
                    final AnnotInfo annotInfo = mRootList.get(i);
                    tempAnnot = annotInfo.mAnnot;
                    if (annotInfo.mModifyFlag) {
                        if (!annotInfo.mNewLines.isEmpty()) {
                            modifyAnnot(pageIndex, annotInfo, undoItems, isLast);
                        } else {
                            deleteAnnot(tempAnnot, null, undoItems, isLast);
                        }
                    }
                    invalidateJniAnnots(annotInfo, 1, null);
                    union(rmRect, AppUtil.toRectF(annotInfo.mAnnot.getRect()));
                }

                if (hasNoModify) {
                    union(rmRect, noModifyRect);
                }
                mInvalidateRect.set(rmRect);

                mCapturedPage = -1;
                mRootList.clear();
                mPaths.clear();
            } catch (PDFException e) {

            }
        }
    }


    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e) {

        if (!((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
            return false;
        }
        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);

        float x = point.x;
        float y = point.y;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mTouchCaptured) {
                    if (mCapturedPage == -1) {
                        mTouchCaptured = true;
                        mCapturedPage = pageIndex;
                    } else if (pageIndex == mCapturedPage) {
                        mTouchCaptured = true;
                    }
                    if (mTouchCaptured) {
                        mLastPt.set(x, y);
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mTouchCaptured && mCapturedPage == pageIndex) {
                    if (!mLastPt.equals(x, y)) {
                        calculateNewLines(mPdfViewCtrl, pageIndex, point);
                        RectF invaRect = getEraserBBox(mLastPt, point);
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(invaRect, invaRect, pageIndex);
                        mPdfViewCtrl.invalidate(rectFToRect(invaRect));
                        mLastPt.set(x, y);
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mTouchCaptured) {
                    mTouchCaptured = false;
                    RectF invaRect2 = getEraserBBox(mLastPt, point);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(invaRect2, invaRect2, pageIndex);
                    mPdfViewCtrl.invalidate(rectFToRect(invaRect2));
                    mLastPt.set(-mRadius, -mRadius);
                }
                return true;
            default:
        }
        return true;
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
    public void onDraw(int pageIndex, Canvas canvas) {
        if (mCapturedPage == pageIndex) {
            canvas.drawCircle(mLastPt.x, mLastPt.y, mRadius, mEraserPaint);
            if (mRootList.size() == 0) return;
            for (int i = 0; i < mRootList.size(); i++) {
                AnnotInfo annotInfo = mRootList.get(i);
                if (!annotInfo.mDrawAtJava) continue;
                if (!annotInfo.mIsPSIMode) {
                    setPaint(annotInfo.mAnnot);
                    mPaths = getNewPaths(mPdfViewCtrl, pageIndex, annotInfo);
                    if (mPaths != null) {
                        int count = mPaths.size();
                        for (int j = 0; j < count; j++) {
                            canvas.drawPath(mPaths.get(j), mPaint);
                        }
                    }
                }
            }
        }
    }


    class AnnotInfo implements Comparable<AnnotInfo> {
        Ink mAnnot;
        boolean mModifyFlag;
        boolean mDrawAtJava;
        ArrayList<LineInfo> mNewLines;
        boolean mIsPSIMode = false; //true : psi, false: ink

        public AnnotInfo() {
            mModifyFlag = false;
            mDrawAtJava = false;
            mNewLines = new ArrayList<LineInfo>();
            mIsPSIMode = false;
        }

        //sort mRootlist :delete  and  modify  from small to big by annotation index
        @Override
        public int compareTo(AnnotInfo another) {
            try {
                if (another.mNewLines.isEmpty()) {
                    if (mNewLines.isEmpty()) {
                        if (another.mAnnot.getIndex() > mAnnot.getIndex()) {
                            return -1;
                        } else if (another.mAnnot.getIndex() == mAnnot.getIndex()) {
                            return 0;
                        } else {
                            return 1;
                        }
                    } else {
                        return 1;
                    }
                } else {
                    if (mNewLines.isEmpty()) {
                        return -1;
                    } else {
                        if (another.mAnnot.getIndex() > mAnnot.getIndex()) {
                            return -1;
                        } else if (another.mAnnot.getIndex() == mAnnot.getIndex()) {
                            return 0;
                        } else {
                            return 1;
                        }
                    }
                }
            } catch (PDFException e) {

            }
            return 1;
        }

    }

    class LineInfo {
        RectF mLineBBox;
        ArrayList<PointF> mLine;
        ArrayList<Float> mPresses;

        public LineInfo() {
            mLineBBox = new RectF();
            mLine = new ArrayList<PointF>();
            mPresses = new ArrayList<Float>();
        }
    }

    private void calculateNewLines(final PDFViewCtrl pdfViewCtrl, final int pageIndex, PointF point) {
        RectF rect = new RectF(point.x, point.y, point.x, point.y);
        rect.union(mLastPt.x, mLastPt.y);
        rect.inset(-mRadius, -mRadius);
        mPdfViewCtrl.convertPageViewRectToPdfRect(rect, rect, pageIndex);
        try {
            PDFPage page = pdfViewCtrl.getDoc().getPage(pageIndex);
            ArrayList<Annot> annotList = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getAnnotsInteractRect(page, new RectF(rect), Annot.e_Ink);
            PointF tv_pt = new PointF();
            RectF tv_rectF = new RectF();
            RectF eraseBBox = getEraserBBox(mLastPt, point);
            mPdfViewCtrl.convertPageViewRectToPdfRect(eraseBBox, eraseBBox, pageIndex);
            for (final Annot annot : annotList) {
                if (AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)) continue;
                RectF annotBBox = AppUtil.toRectF(annot.getRect());
                if (DocumentManager.intersects(annotBBox, eraseBBox)) {
                    boolean isExist = false;
                    for (int i = 0; i < mRootList.size(); i++) {
                        if (mRootList.get(i).mAnnot.getUniqueID().equalsIgnoreCase(annot.getUniqueID())) {
                            isExist = true;
                            break;
                        }
                    }
                    if (!isExist) {
                        final AnnotInfo annotInfo = new AnnotInfo();
                        annotInfo.mAnnot = (Ink) annot;
                        com.foxit.sdk.common.Path path = ((Ink) annot).getInkList();

                        LineInfo lineInfo = null;
                        int ptCount = path.getPointCount();
                        for (int j = 0; j < ptCount; j++) {
                            if (path.getPointType(j) == com.foxit.sdk.common.Path.e_TypeMoveTo) {
                                lineInfo = new LineInfo();
                            }
                            lineInfo.mLine.add(AppUtil.toPointF(path.getPoint(j)));
                            if (j == ptCount - 1 || ((j + 1) < ptCount && path.getPointType(j + 1) == com.foxit.sdk.common.Path.e_TypeMoveTo)) {
                                lineInfo.mLineBBox = getLineBBox(lineInfo.mLine, annot.getBorderInfo().getWidth());
                                annotInfo.mNewLines.add(lineInfo);
                            }
                        }
                        mRootList.add(annotInfo);

                        invalidateJniAnnots(annotInfo, 0, new Event.Callback() {
                            @Override
                            public void result(Event event, boolean success) {
                                try {
                                    RectF viewRect = AppUtil.toRectF(annot.getRect());
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                                    mPdfViewCtrl.refresh(pageIndex, rectFToRect(viewRect));
                                } catch (PDFException e) {
                                    e.printStackTrace();
                                }
                                annotInfo.mDrawAtJava = true;
                            }
                        });
                    }
                }
            }

            if (mRootList.size() == 0) return;
            PointF pdfDP = new PointF(mLastPt.x, mLastPt.y);
            PointF pdfCP = new PointF(point.x, point.y);
            RectF eBBox = getEraserBBox(mLastPt, point);
            RectF radiusRect = new RectF(0, 0, mRadius, mRadius);
            mPdfViewCtrl.convertPageViewPtToPdfPt(pdfDP, pdfDP, pageIndex);
            mPdfViewCtrl.convertPageViewPtToPdfPt(pdfCP, pdfCP, pageIndex);
            mPdfViewCtrl.convertPageViewRectToPdfRect(eBBox, eBBox, pageIndex);
            mPdfViewCtrl.convertPageViewRectToPdfRect(radiusRect, radiusRect, pageIndex);

            float pdfR = radiusRect.width();
            PointF intersectPoint = new PointF();
            PointF pdfPoint1 = new PointF();
            PointF pdfPoint2 = new PointF();

            for (int i = 0; i < mRootList.size(); i++) {
                AnnotInfo annotNode = mRootList.get(i);
                if (!DocumentManager.intersects(AppUtil.toRectF(annotNode.mAnnot.getRect()), eBBox))
                    continue;
                for (int lineIndex = 0; lineIndex < annotNode.mNewLines.size(); lineIndex++) {
                    LineInfo lineNode = annotNode.mNewLines.get(lineIndex);
                    ArrayList<PointF> pdfLine = lineNode.mLine;
                    ArrayList<Float> presses = lineNode.mPresses;
                    int end1_PointIndex = -1, begin2_PointIndex = -1;

                    //if lineRect intersect eraseRect
                    RectF lineRect = lineNode.mLineBBox;
                    if (!DocumentManager.intersects(lineRect, eBBox))
                        continue;

                    for (int j = 0; j < pdfLine.size(); j++) {
                        // out of circle point  or  first point
                        pdfPoint1.set(pdfLine.get(j).x, pdfLine.get(j).y);
                        boolean createNewLine = false;
                        boolean reachEnd = false;
                        if (j == pdfLine.size() - 1) {
                            reachEnd = true;
                        } else {
                            // in circle point  or  second point
                            pdfPoint2.set(pdfLine.get(j + 1).x, pdfLine.get(j + 1).y);
                        }

                        int type = getIntersection(pdfPoint1, pdfPoint2, pdfDP, pdfCP, intersectPoint);
                        if (!reachEnd && type == 1) {
                            createNewLine = true;
                            tv_rectF.set(intersectPoint.x, intersectPoint.y, intersectPoint.x, intersectPoint.y);
                            for (int p = j; p >= 0; p--) {
                                tv_pt.set(pdfLine.get(p).x, pdfLine.get(p).y);
                                tv_rectF.union(tv_pt.x, tv_pt.y);
                                if (getDistanceOfTwoPoints(tv_pt.x, tv_pt.y, intersectPoint.x, intersectPoint.y) > pdfR) {
                                    end1_PointIndex = p;
                                    if (p > 0) {
                                        tv_rectF.union(pdfLine.get(p - 1).x, pdfLine.get(p - 1).y);
                                    }
                                    break;
                                }
                            }
                            for (int q = j + 1; q < pdfLine.size(); q++) {
                                tv_pt.set(pdfLine.get(q).x, pdfLine.get(q).y);
                                tv_rectF.union(tv_pt.x, tv_pt.y);
                                if (getDistanceOfTwoPoints(tv_pt.x, tv_pt.y, intersectPoint.x, intersectPoint.y) > pdfR) {
                                    begin2_PointIndex = q;
                                    if (q < pdfLine.size() - 1) {
                                        tv_rectF.union(pdfLine.get(q + 1).x, pdfLine.get(q + 1).y);
                                    }
                                    break;
                                }
                            }
                        } else if (getDistanceOfPointToLine(pdfPoint1.x, pdfPoint1.y, pdfDP.x, pdfDP.y, pdfCP.x, pdfCP.y) < pdfR) {
                            if (isIntersectPointInLine(pdfPoint1.x, pdfPoint1.y, pdfDP.x, pdfDP.y, pdfCP.x, pdfCP.y)
                                    || getDistanceOfTwoPoints(pdfPoint1.x, pdfPoint1.y, pdfDP.x, pdfDP.y) < pdfR
                                    || getDistanceOfTwoPoints(pdfPoint1.x, pdfPoint1.y, pdfCP.x, pdfCP.y) < pdfR) {
                                createNewLine = true;
                                for (int p = j; p >= 0; p--) {
                                    tv_pt.set(pdfLine.get(p).x, pdfLine.get(p).y);
                                    tv_rectF.union(tv_pt.x, tv_pt.y);
                                    if (getDistanceOfPointToLine(tv_pt.x, tv_pt.y, pdfDP.x, pdfDP.y, pdfCP.x, pdfCP.y) < pdfR &&
                                            (isIntersectPointInLine(tv_pt.x, tv_pt.y, pdfDP.x, pdfDP.y, pdfCP.x, pdfCP.y) ||
                                                    getDistanceOfTwoPoints(tv_pt.x, tv_pt.y, pdfDP.x, pdfDP.y) < pdfR ||
                                                    getDistanceOfTwoPoints(tv_pt.x, tv_pt.y, pdfCP.x, pdfCP.y) < pdfR)) {
                                        continue;
                                    }
                                    end1_PointIndex = p;
                                    if (p > 0) {
                                        tv_rectF.union(pdfLine.get(p - 1).x, pdfLine.get(p - 1).y);
                                    }
                                    break;
                                }
                                for (int q = j + 1; q < pdfLine.size(); q++) {
                                    tv_pt.set(pdfLine.get(q).x, pdfLine.get(q).y);
                                    tv_rectF.union(tv_pt.x, tv_pt.y);
                                    if (getDistanceOfPointToLine(tv_pt.x, tv_pt.y, pdfDP.x, pdfDP.y, pdfCP.x, pdfCP.y) < pdfR &&
                                            (isIntersectPointInLine(tv_pt.x, tv_pt.y, pdfDP.x, pdfDP.y, pdfCP.x, pdfCP.y) ||
                                                    getDistanceOfTwoPoints(tv_pt.x, tv_pt.y, pdfDP.x, pdfDP.y) < pdfR ||
                                                    getDistanceOfTwoPoints(tv_pt.x, tv_pt.y, pdfCP.x, pdfCP.y) < pdfR)) {
                                        continue;
                                    }
                                    begin2_PointIndex = q;
                                    if (q < pdfLine.size() - 1) {
                                        tv_rectF.union(pdfLine.get(q + 1).x, pdfLine.get(q + 1).y);
                                    }
                                    break;
                                }
                            }
                        }

                        if (createNewLine) {
                            createNewLine = false;

                            ArrayList<PointF> newLine1 = new ArrayList<PointF>();
                            ArrayList<Float> newPresses1 = new ArrayList<Float>();
                            if (0 <= end1_PointIndex && end1_PointIndex < pdfLine.size()) {
                                for (int k = 0; k <= end1_PointIndex; k++) {
                                    newLine1.add(pdfLine.get(k));
                                }
                            }

                            ArrayList<PointF> newLine2 = new ArrayList<PointF>();
                            ArrayList<Float> newPresses2 = new ArrayList<Float>();
                            if (0 <= begin2_PointIndex && begin2_PointIndex < pdfLine.size()) {
                                for (int k = pdfLine.size() - 1; k >= begin2_PointIndex; k--) {
                                    newLine2.add(pdfLine.get(k));
                                }
                            }

                            annotNode.mNewLines.remove(lineIndex);
                            if (newLine1.size() == 0 && newLine2.size() == 0) {
                                // current line is removed, and no new line is added
                                // lineIndex -- adjust index continue to erase next line
                                lineIndex--;
                            } else {
                                // insert line2 first, then line1
                                // make sure the line1 is before line2
                                if (newLine2.size() != 0) {
                                    LineInfo info = new LineInfo();
                                    info.mLine = newLine2;
                                    info.mPresses = newPresses2;
                                    info.mLineBBox = getLineBBox(newLine2, annotNode.mAnnot.getBorderInfo().getWidth());
                                    annotNode.mNewLines.add(lineIndex, info);
                                }
                                if (newLine1.size() != 0) {
                                    LineInfo info = new LineInfo();
                                    info.mLine = newLine1;
                                    info.mPresses = newPresses1;
                                    info.mLineBBox = getLineBBox(newLine1, annotNode.mAnnot.getBorderInfo().getWidth());
                                    annotNode.mNewLines.add(lineIndex, info);
                                } else {
                                    // if line1 have no point, add index -- for continue erase line2
                                    lineIndex--;
                                }
                            }
                            annotNode.mModifyFlag = true;
                            invalidateNewLine(pdfViewCtrl, pageIndex, annotNode.mAnnot, tv_rectF);
                            break;
                        }
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void invalidateNewLine(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot, RectF rect) {
        try {
            rect.inset(annot.getBorderInfo().getWidth(), annot.getBorderInfo().getWidth());
            float tmp = rect.top;
            rect.top = rect.bottom;
            rect.bottom = tmp;

            pdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, pageIndex);
            pdfViewCtrl.convertPageViewRectToDisplayViewRect(rect, rect, pageIndex);
            pdfViewCtrl.invalidate(rectFToRect(rect));
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private double getDistanceOfPointToLine(float x, float y, float x1, float y1, float x2, float y2) {
        float k = 0;
        float b = 0;

        if (x1 == x2) {
            return Math.abs(x - x1);
        } else if (y1 == y2) {
            return Math.abs(y - y1);
        } else {
            k = (y2 - y1) / (x2 - x1);
            b = y2 - k * x2;
            return Math.abs(k * x - y + b) / Math.sqrt(k * k + 1);
        }
    }

    private boolean isIntersectPointInLine(float x, float y, float x1, float y1, float x2, float y2) {
        boolean result = false;
        double cross = (x2 - x1) * (x - x1) + (y2 - y1) * (y - y1);
        double d = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        double r = cross / d;
        if (r > 0 && r < 1) {
            result = true;
        }
        return result;
    }


    private int getIntersection(PointF a, PointF b, PointF c, PointF d, PointF intersection) {
        if (Math.abs(b.y - a.y) + Math.abs(b.x - a.x) + Math.abs(d.y - c.y) + Math.abs(d.x - c.x) == 0) {
            if ((c.x - a.x) + (c.y - a.y) == 0) {
                //System.out.println("A B C D are the same point");
            } else {
                //System.out.println("A B are the same point.C D are the same point, and different from AC.");
            }
            return 0;
        }

        if (Math.abs(b.y - a.y) + Math.abs(b.x - a.x) == 0) {
            if ((a.x - d.x) * (c.y - d.y) - (a.y - d.y) * (c.x - d.x) == 0) {
                //System.out.println("A、B are the same point,and in line CD.");
            } else {
                //System.out.println("A、B are the same point,and not in line CD.");
            }
            return 0;
        }

        if (Math.abs(d.y - c.y) + Math.abs(d.x - c.x) == 0) {
            if ((d.x - b.x) * (a.y - b.y) - (d.y - b.y) * (a.x - b.x) == 0) {
                //System.out.println("C、D are the same point,and in line AB.");
            } else {
                //System.out.println("C、D are the same point,and not in line AB.");
            }
            return 0;
        }

        if ((b.y - a.y) * (c.x - d.x) - (b.x - a.x) * (c.y - d.y) == 0) {
            //System.out.println("Parallel lines, no intersection!");
            return 0;
        }

        intersection.x = ((b.x - a.x) * (c.x - d.x) * (c.y - a.y) - c.x
                * (b.x - a.x) * (c.y - d.y) + a.x * (b.y - a.y) * (c.x - d.x))
                / ((b.y - a.y) * (c.x - d.x) - (b.x - a.x) * (c.y - d.y));
        intersection.y = ((b.y - a.y) * (c.y - d.y) * (c.x - a.x) - c.y
                * (b.y - a.y) * (c.x - d.x) + a.y * (b.x - a.x) * (c.y - d.y))
                / ((b.x - a.x) * (c.y - d.y) - (b.y - a.y) * (c.x - d.x));

        if ((intersection.x - a.x) * (intersection.x - b.x) <= 0
                && (intersection.x - c.x) * (intersection.x - d.x) <= 0
                && (intersection.y - a.y) * (intersection.y - b.y) <= 0
                && (intersection.y - c.y) * (intersection.y - d.y) <= 0) {
            //System.out.println("Lines intersect at the intersection point(" + intersection.x + "," + intersection.y + ")!");
            return 1;
        } else {
            //System.out.println("Lines intersect at the virtual intersection point(" + intersection.x + "," + intersection.y + ")!");
            return -1;
        }
    }

    private RectF getEraserBBox(PointF downPoint, PointF point) {
        RectF eraserBBox = new RectF();
        eraserBBox.left = Math.min(downPoint.x, point.x);
        eraserBBox.top = Math.min(downPoint.y, point.y);
        eraserBBox.right = Math.max(downPoint.x, point.x);
        eraserBBox.bottom = Math.max(downPoint.y, point.y);
        eraserBBox.inset(-mRadius - 2, -mRadius - 2);
        return eraserBBox;
    }

    private RectF getLineBBox(ArrayList<PointF> line, float thickness) {
        if (line.size() == 0) {
            return new RectF(0, 0, 0, 0);
        }

        RectF lineBBox = new RectF(line.get(0).x, line.get(0).y, line.get(0).x, line.get(0).y);
        for (int i = 0; i < line.size(); i++) {
            lineBBox.left = Math.min(lineBBox.left, line.get(i).x);
            lineBBox.top = Math.max(lineBBox.top, line.get(i).y);
            lineBBox.right = Math.max(lineBBox.right, line.get(i).x);
            lineBBox.bottom = Math.min(lineBBox.bottom, line.get(i).y);
        }
        lineBBox.inset(-thickness / 2, -thickness / 2);
        return lineBBox;
    }


    private float getDistanceOfTwoPoints(float x1, float y1, float x2, float y2) {
        return (float) (Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
    }

    private ArrayList<Path> getNewPaths(PDFViewCtrl pdfViewCtrl, int pageIndex, AnnotInfo info) {
        ArrayList<LineInfo> pdfLines = info.mNewLines;
        ArrayList<Path> paths = new ArrayList<Path>();
        PointF pointF = new PointF();

        float cx = 0, cy = 0, ex, ey;
        for (int i = 0; i < pdfLines.size(); i++) {
            ArrayList<PointF> pdfLine = pdfLines.get(i).mLine;
            int ptCount = pdfLine.size();
            if (ptCount == 0) {
                continue;
            } else if (ptCount == 1) {
                Path path = new Path();
                pointF.set(pdfLine.get(0).x, pdfLine.get(0).y);
                pdfViewCtrl.convertPdfPtToPageViewPt(pointF, pointF, pageIndex);
                path.moveTo(pointF.x, pointF.y);
                path.lineTo(pointF.x + 0.1f, pointF.y + 0.1f);
                paths.add(path);
            } else {
                Path path = new Path();
                for (int j = 0; j < ptCount; j++) {
                    pointF.set(pdfLine.get(j).x, pdfLine.get(j).y);
                    pdfViewCtrl.convertPdfPtToPageViewPt(pointF, pointF, pageIndex);
                    if (j == 0) {
                        path.moveTo(pointF.x, pointF.y);
                        cx = pointF.x;
                        cy = pointF.y;
                    } else {
                        ex = (cx + pointF.x) / 2;
                        ey = (cy + pointF.y) / 2;
                        path.quadTo(cx, cy, ex, ey);
                        cx = pointF.x;
                        cy = pointF.y;
                        if (j == pdfLine.size() - 1) {
                            ex = pointF.x;
                            ey = pointF.y;
                            path.lineTo(ex, ey);
                        }
                    }
                }
                paths.add(path);
            }
        }
        return paths;
    }

    private void invalidateJniAnnots(AnnotInfo annotInfo, int flag, final Event.Callback result) {

        if (flag == 0) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotStartEraser(annotInfo.mAnnot);
        } else if (flag == 1) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotEndEraser(annotInfo.mAnnot);
        }

        if (result != null) {
            result.result(null, true);
        }
    }

    private RectF getNewBBox(AnnotInfo annotInfo) {
        Ink annot = annotInfo.mAnnot;
        ArrayList<ArrayList<PointF>> pdfLines = getNewPdfLines(annotInfo);
        RectF newBBox = null;
        for (int i = 0; i < pdfLines.size(); i++) {
            for (int j = 0; j < pdfLines.get(i).size(); j++) {
                PointF pdfPt = pdfLines.get(i).get(j);
                if (newBBox == null) {
                    newBBox = new RectF(pdfPt.x, pdfPt.y, pdfPt.x, pdfPt.y);
                } else {
                    newBBox.left = Math.min(newBBox.left, pdfPt.x);
                    newBBox.bottom = Math.min(newBBox.bottom, pdfPt.y);
                    newBBox.right = Math.max(newBBox.right, pdfPt.x);
                    newBBox.top = Math.max(newBBox.top, pdfPt.y);
                }
            }
        }
        try {
            newBBox.inset(-annot.getBorderInfo().getWidth() * 0.5f - mCtlPtRadius, -annot.getBorderInfo().getWidth() * 0.5f - mCtlPtRadius);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return newBBox;
    }

    private ArrayList<ArrayList<PointF>> getNewPdfLines(AnnotInfo annotInfo) {
        ArrayList<ArrayList<PointF>> pdfLines = new ArrayList<ArrayList<PointF>>();
        for (int i = 0; i < annotInfo.mNewLines.size(); i++) {
            ArrayList<PointF> oldLine = annotInfo.mNewLines.get(i).mLine;
            ArrayList<PointF> newLine = new ArrayList<PointF>();
            for (int j = 0; j < oldLine.size(); j++) {
                newLine.add(oldLine.get(j));
            }
            pdfLines.add(newLine);
        }
        return pdfLines;
    }


    private void modifyAnnot(final int pageIndex, final AnnotInfo annotInfo, final EraserUndoItem undoItems, final boolean isLast) {
        try {
            final Annot annot = annotInfo.mAnnot;
            InkAnnotHandler annotHandler = (InkAnnotHandler) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(Annot.e_Ink);
            final InkModifyUndoItem undoItem = new InkModifyUndoItem(annotHandler, mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.setOldValue(annot);
            RectF newBBox = getNewBBox(annotInfo);
            undoItem.mBBox = new RectF(newBBox);
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mInkLists = getNewPdfLines(annotInfo);
            if (undoItem.mInkLists != null) {
                undoItem.mPath = new com.foxit.sdk.common.Path();
                for (int i = 0; i < undoItem.mInkLists.size(); i++) {
                    ArrayList<PointF> line = undoItem.mInkLists.get(i);
                    int size = line.size();
                    if (size == 1) {
                        undoItem.mPath.moveTo(AppUtil.toFxPointF(line.get(0)));
                        undoItem.mPath.lineTo(new com.foxit.sdk.common.fxcrt.PointF(line.get(0).x + 0.1f, line.get(0).y + 0.1f));
                    } else {
                        for (int j = 0; j < size; j++) {
                            if (j == 0) {
                                undoItem.mPath.moveTo(AppUtil.toFxPointF(line.get(j)));
                            } else {
                                undoItem.mPath.lineTo(AppUtil.toFxPointF(line.get(j)));
                            }
                        }
                    }
                }
            }

            undoItem.setOldValue(annot);
            undoItem.mOldInkLists = InkAnnotUtil.generateInkList(((Ink)annot).getInkList());
            undoItem.isFromEraser = true;
            InkEvent event = new InkEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Ink) annot, mPdfViewCtrl);
            event.useOldValue = false;

            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(isLast);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        try {
                            undoItems.addUndoItem(undoItem);
                            if (isLast) {
                                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItems);
                                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(false);
                            }

                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(annot.getPage(), annot);
                            if (mPdfViewCtrl.isPageVisible(pageIndex) && isLast) {
                                RectF rectF = new RectF(mInvalidateRect);
                                mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                                mPdfViewCtrl.refresh(pageIndex, rectFToRect(rectF));

                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    private void deleteAnnot(final Annot annot, final Event.Callback result, final EraserUndoItem undoItems, final boolean isLast) {
        // step 1: set current annot to null
        if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
        }

        try {
            final PDFPage page = annot.getPage();
            final int pageIndex = page.getIndex();

            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);

            InkAnnotHandler annotHandler = (InkAnnotHandler) ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(Annot.e_Ink);
            final InkDeleteUndoItem undoItem = new InkDeleteUndoItem(annotHandler, mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.isFromEraser = true;
            try {
                undoItem.mPath = ((Ink)annot).getInkList();
                undoItem.mInkLists = InkAnnotUtil.generateInkList(undoItem.mPath);
            } catch (PDFException e) {
                e.printStackTrace();
            }

            InkEvent event = new InkEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Ink) annot, mPdfViewCtrl);
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(true);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        undoItems.addUndoItem(undoItem);
                        if (isLast) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItems);
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(false);
                        }
                        if (mPdfViewCtrl.isPageVisible(pageIndex) && isLast) {
                            RectF rectF = new RectF(mInvalidateRect);
                            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                            mPdfViewCtrl.refresh(pageIndex, rectFToRect(rectF));
                        }
                    }

                    if (result != null) {
                        result.result(null, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void setPaint(Annot annot) {
        try {
            int pageIndex = annot.getPage().getIndex();
            float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
            mPaint.setColor((int) annot.getBorderColor());
            mPaint.setStrokeWidth(thickness);
            int opacity = (int) (((Ink) annot).getOpacity() * 255f + 0.5f);
            mPaint.setAlpha(opacity);
            mPaint.setStyle(Style.STROKE);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private float thicknessOnPageView(int pageIndex, float thickness) {
        RectF rectF = new RectF(0, 0, thickness, thickness);
        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
        return Math.abs(rectF.width());
    }

    public void setRadius(float radius) {
        mRadius = AppDisplay.getInstance(mContext).dp2px(radius);
    }

    @Override
    public void setThickness(float thickness) {
        super.setThickness(thickness);
        setRadius(thickness);
    }

    @Override
    protected void setPaintProperty(PDFViewCtrl pdfViewCtrl, int pageIndex, Paint paint) {

    }

    @Override
    public long getSupportedProperties() {
        return PropertyBar.PROPERTY_LINEWIDTH;
    }

    @Override
    protected void setPropertyBarProperties(PropertyBar propertyBar) {
        int[] colors = new int[PropertyBar.PB_COLORS_PENCIL.length];
        System.arraycopy(PropertyBar.PB_COLORS_PENCIL, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_PENCIL[0];
        propertyBar.setColors(colors);
        propertyBar.setProperty(PropertyBar.PROPERTY_COLOR, mColor);
        propertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, getThickness());
        if (AppDisplay.getInstance(mContext).isPad()) {
            propertyBar.setArrowVisible(true);
        } else {
            propertyBar.setArrowVisible(false);
        }
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_ERASER;
    }

    // in pdf coordinate
    private static void union(RectF lRect, RectF rRect) {
        if ((rRect.left < rRect.right) && (rRect.bottom < rRect.top)) {
            if ((lRect.left < lRect.right) && (lRect.bottom < lRect.top)) {
                if (lRect.left > rRect.left)
                    lRect.left = rRect.left;
                if (lRect.top < rRect.top)
                    lRect.top = rRect.top;
                if (lRect.right < rRect.right)
                    lRect.right = rRect.right;
                if (lRect.bottom > rRect.bottom)
                    lRect.bottom = rRect.bottom;
            } else {
                lRect.left = rRect.left;
                lRect.top = rRect.top;
                lRect.right = rRect.right;
                lRect.bottom = rRect.bottom;
            }
        }
    }
}
