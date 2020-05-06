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
package com.foxit.uiextensions.annots.multiselect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DrawFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Caret;
import com.foxit.sdk.pdf.annots.QuadPointsArray;
import com.foxit.sdk.pdf.annots.Redact;
import com.foxit.sdk.pdf.annots.StrikeOut;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.AnnotUndoItem;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.controls.dialog.FxProgressDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.MoreTools;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public class MultiSelectToolHandler implements ToolHandler {
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private UIExtensionsManager mUiExtensionsManager;

    private boolean mIsContinuousCreate;

    private IBaseItem mOKItem;
    private IBaseItem mContinuousCreateItem;

    private int mSelectAreaColor = 0x2da5da;
    private int mSelectAreaOpacity = 30;
    private int mSelectAreaBound = 5;

    private Paint mPaint;
    private Paint mMaskPaint;
    private Paint mFrmPaint;// outline
    private Paint mCtlPtPaint;

    private boolean mTouchCaptured = false;
    private int mLastPageIndex = -1;

    private PointF mStartPoint = new PointF(0, 0);
    private PointF mStopPoint = new PointF(0, 0);
    private PointF mDownPoint = new PointF(0, 0);// whether moving point
    private PointF mLastPoint = new PointF(0, 0);
    private Rect mTempRectInTouch = new Rect(0, 0, 0, 0);
    private RectF mInvalidateRect = new RectF(0, 0, 0, 0);

    private RectF mNowRect = new RectF(0, 0, 0, 0);

    private float mThickness = 5.0f;
    private int mControlPtEx = 5;// Refresh the scope expansion width
    private float mCtlPtLineWidth = 5;
    private float mCtlPtRadius = 10;


    private int mSelectState;
    private static final int SELECT_START = 0;
    private static final int SELECT_ANNOT = 1; // just one annotation
    private static final int SELECT_ANNOTS = 2; // more than one annotation
    private static final int SELECT_NONE = 3;

    private int mState;
    private static final int STATE_NONE = 0;
    private static final int STATE_MOVE = 1;
    private static final int STATE_DRAG_MOVE = 2;

    public MultiSelectToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();

        initPaint();

        mSelectState = SELECT_START;
        initToolItem(context, mUiExtensionsManager);
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(mSelectAreaColor);
        mPaint.setStrokeWidth(mSelectAreaBound);
        mPaint.setAlpha(255);

        mMaskPaint = new Paint();
        mMaskPaint.setStyle(Paint.Style.FILL);
        mMaskPaint.setColor(mSelectAreaColor);
        mMaskPaint.setAlpha(AppDmUtil.opacity100To255(mSelectAreaOpacity));

        PathEffect effect = AppAnnotUtil.getAnnotBBoxPathEffect();
        mFrmPaint = new Paint();
        mFrmPaint.setPathEffect(effect);
        mFrmPaint.setStyle(Paint.Style.STROKE);
        mFrmPaint.setAntiAlias(true);
        mFrmPaint.setColor(mSelectAreaColor);
        mFrmPaint.setAlpha(255);
        mFrmPaint.setStrokeWidth(mCtlPtLineWidth);

        mCtlPtPaint = new Paint();
        mCtlPtPaint.setStrokeWidth(mCtlPtLineWidth);
    }

    @Override
    public String getType() {
        return TH_TYPE_SELECT_ANNOTATIONS;
    }

    @Override
    public void onActivate() {
        mLastPageIndex = -1;
        mSelectState = SELECT_START;
        mState = STATE_NONE;
        mNowRect.setEmpty();
        resetMultiSelectBar();
    }

    @Override
    public void onDeactivate() {
        if (mSelectAnnots.size() > 1) {
            mSelectAnnots.clear();

            RectF rect = new RectF(mSelectAnnotsRect);
            mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, mLastPageIndex);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rect, rect, mLastPageIndex);
            mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rect));
            mLastPageIndex = -1;
            mSelectState = SELECT_START;
            mState = STATE_NONE;
            if (mAnnotationMenu != null) {
                mAnnotationMenu.setListener(null);
                if (mAnnotationMenu.isShowing()) {
                    mAnnotationMenu.dismiss();
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        if (mSelectState == SELECT_START) {
            return onTouchEvent_SelectStart(pageIndex, motionEvent);
        } else if (mSelectState == SELECT_ANNOTS) {
            return onTouchEvent_SelectAnnots(pageIndex, motionEvent);
        }

        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        if (mSelectAnnots.size() == 0) return false;
        PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        RectF selectRect = new RectF(mSelectAnnotsRect);
        mPdfViewCtrl.convertPdfRectToPageViewRect(selectRect, selectRect, pageIndex);
        if (!isHitSelectRect(selectRect, point)) {
            // configure annotation menu
            mAnnotationMenu.setListener(null);
            mAnnotationMenu.dismiss();
            RectF rectF = new RectF(selectRect);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
            mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectF));
            mUiExtensionsManager.setCurrentToolHandler(null);

            mLastPageIndex = -1;
            mSelectState = SELECT_START;
            mState = STATE_NONE;
            mSelectAnnots.clear();
            mSelectAnnotsRect.setEmpty();
        }
        return true;
    }

    @Override
    public boolean isContinueAddAnnot() {
        return mIsContinuousCreate;
    }

    @Override
    public void setContinueAddAnnot(boolean continueAddAnnot) {
        mIsContinuousCreate = continueAddAnnot;
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (mStartPoint == null || mStopPoint == null) return;
        if (mLastPageIndex == pageIndex) {

            if (mSelectState == SELECT_START) {
                if (mNowRect.isEmpty()) return;
                canvas.save();
                canvas.drawRect(mNowRect, mMaskPaint);
                canvas.restore();

                canvas.save();
                canvas.drawRect(mNowRect, mPaint);
                canvas.restore();
            } else if (mSelectState == SELECT_ANNOTS) {
                if (mSelectAnnotsRect.left >= mSelectAnnotsRect.right || mSelectAnnotsRect.top <= mSelectAnnotsRect.bottom) return;
                RectF selectRect = new RectF(mSelectAnnotsRect);
                mPdfViewCtrl.convertPdfRectToPageViewRect(selectRect, selectRect, pageIndex);
                if (mState == STATE_DRAG_MOVE) {
                    onDraw_Drag_Move(pageIndex, canvas);
                } else { //if (mState == STATE_MOVE || mState == STATE_NONE)
                    mBBoxInOnDraw.set(selectRect);
                    if (mLastOper == OPER_DEFAULT) {
                        float deltaXY = mCtlPtLineWidth;// Judging border value
                        normalize(pageIndex, mBBoxInOnDraw, deltaXY);
                    }
                    if (mState == STATE_MOVE) {
                        float dx = mLastPoint.x - mDownPoint.x;
                        float dy = mLastPoint.y - mDownPoint.y;

                        mBBoxInOnDraw.offset(dx, dy);
                    }

                    canvas.save();
                    canvas.drawRect(mBBoxInOnDraw, mPaint);
                    canvas.restore();
                }
            }
        }

    }

    private RectF mBBoxInOnDraw = new RectF();
    private RectF mViewDrawRectInOnDraw = new RectF();
    private DrawFilter mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private void onDraw_Drag_Move(int pageIndex, Canvas canvas){
        if (mSelectAnnots.size() == 0) return;
        canvas.save();
        canvas.setDrawFilter(mDrawFilter);
        RectF selectRect = new RectF(mSelectAnnotsRect);
        mPdfViewCtrl.convertPdfRectToPageViewRect(selectRect, selectRect, pageIndex);
        mViewDrawRectInOnDraw.set(selectRect);
        if (mLastOper == OPER_SCALE_LT) {// SCALE
            mBBoxInOnDraw.set(mLastPoint.x, mLastPoint.y, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
        } else if (mLastOper == OPER_SCALE_T) {
            mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mLastPoint.y, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
        } else if (mLastOper == OPER_SCALE_RT) {
            mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mLastPoint.y, mLastPoint.x, mViewDrawRectInOnDraw.bottom);
        } else if (mLastOper == OPER_SCALE_R) {
            mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mLastPoint.x, mViewDrawRectInOnDraw.bottom);
        } else if (mLastOper == OPER_SCALE_RB) {
            mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mLastPoint.x, mLastPoint.y);
        } else if (mLastOper == OPER_SCALE_B) {
            mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mLastPoint.y);
        } else if (mLastOper == OPER_SCALE_LB) {
            mBBoxInOnDraw.set(mLastPoint.x, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mLastPoint.y);
        } else if (mLastOper == OPER_SCALE_L) {
            mBBoxInOnDraw.set(mLastPoint.x, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
        }
        if (mLastOper == OPER_TRANSLATE || mLastOper == OPER_DEFAULT) {// TRANSLATE or DEFAULT
            mBBoxInOnDraw.set(selectRect);
            if (mLastOper == OPER_DEFAULT) {
                float deltaXY = mCtlPtLineWidth + mCtlPtRadius * 2 + 2;// Judging border value
                normalize(pageIndex, mBBoxInOnDraw, deltaXY);
            }
            float dx = mLastPoint.x - mDownPoint.x;
            float dy = mLastPoint.y - mDownPoint.y;

            mBBoxInOnDraw.offset(dx, dy);
        }
        if (mSelectAnnots.size() > 1) {
            drawControlPoints(canvas, mBBoxInOnDraw, mSelectAreaColor);
            // add Control Imaginary
            drawControlImaginary(canvas, mBBoxInOnDraw);
        }

        canvas.restore();
    }

    Path mImaginaryPath = new Path();

    private void drawControlImaginary(Canvas canvas, RectF rectBBox) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mImaginaryPath.reset();
        // set path
        pathAddLine(mImaginaryPath, ctlPts[0].x + mCtlPtRadius, ctlPts[0].y, ctlPts[1].x - mCtlPtRadius, ctlPts[1].y);
        pathAddLine(mImaginaryPath, ctlPts[1].x + mCtlPtRadius, ctlPts[1].y, ctlPts[2].x - mCtlPtRadius, ctlPts[2].y);
        pathAddLine(mImaginaryPath, ctlPts[2].x, ctlPts[2].y + mCtlPtRadius, ctlPts[3].x, ctlPts[3].y - mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[3].x, ctlPts[3].y + mCtlPtRadius, ctlPts[4].x, ctlPts[4].y - mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[4].x - mCtlPtRadius, ctlPts[4].y, ctlPts[5].x + mCtlPtRadius, ctlPts[5].y);
        pathAddLine(mImaginaryPath, ctlPts[5].x - mCtlPtRadius, ctlPts[5].y, ctlPts[6].x + mCtlPtRadius, ctlPts[6].y);
        pathAddLine(mImaginaryPath, ctlPts[6].x, ctlPts[6].y - mCtlPtRadius, ctlPts[7].x, ctlPts[7].y + mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[7].x, ctlPts[7].y - mCtlPtRadius, ctlPts[0].x, ctlPts[0].y + mCtlPtRadius);

        canvas.drawPath(mImaginaryPath, mFrmPaint);
    }

    private void pathAddLine(Path path, float start_x, float start_y, float end_x, float end_y) {
        path.moveTo(start_x, start_y);
        path.lineTo(end_x, end_y);
    }

    private void drawControlPoints(Canvas canvas, RectF rectBBox, int color) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        for (PointF ctlPt : ctlPts) {
            mCtlPtPaint.setColor(Color.WHITE);
            mCtlPtPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
            mCtlPtPaint.setColor(color);
            mCtlPtPaint.setAlpha(255);
            mCtlPtPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
        }
    }

    /*
     *   1-----2-----3
     *   |	         |
     *   |	         |
     *   8           4
     *   |           |
     *   |           |
     *   7-----6-----5
     *   */
    RectF mMapBounds = new RectF();

    private PointF[] calculateControlPoints(RectF rect) {
        rect.sort();
        mMapBounds.set(rect);
        mMapBounds.inset(-mCtlPtRadius - mCtlPtLineWidth / 2f, -mCtlPtRadius - mCtlPtLineWidth / 2f);// control rect
        PointF p1 = new PointF(mMapBounds.left, mMapBounds.top);
        PointF p2 = new PointF((mMapBounds.right + mMapBounds.left) / 2, mMapBounds.top);
        PointF p3 = new PointF(mMapBounds.right, mMapBounds.top);
        PointF p4 = new PointF(mMapBounds.right, (mMapBounds.bottom + mMapBounds.top) / 2);
        PointF p5 = new PointF(mMapBounds.right, mMapBounds.bottom);
        PointF p6 = new PointF((mMapBounds.right + mMapBounds.left) / 2, mMapBounds.bottom);
        PointF p7 = new PointF(mMapBounds.left, mMapBounds.bottom);
        PointF p8 = new PointF(mMapBounds.left, (mMapBounds.bottom + mMapBounds.top) / 2);

        return new PointF[]{p1, p2, p3, p4, p5, p6, p7, p8};
    }

    private void initToolItem(Context context, final UIExtensionsManager uiExtensionsManager) {
        uiExtensionsManager.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                uiExtensionsManager.setCurrentToolHandler(MultiSelectToolHandler.this);
                uiExtensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_MULTI_SELECT;
            }
        });
    }

    private void resetMultiSelectBar() {
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
        mContinuousCreateItem = new BaseItemImpl(mContext);
        mContinuousCreateItem.setTag(ToolbarItemConfig.ITEM_ANNOT_BAR_CONTINUE);
        mContinuousCreateItem.setImageResource(getContinuousIcon(mIsContinuousCreate));

        mContinuousCreateItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }

                mIsContinuousCreate = !mIsContinuousCreate;
                mContinuousCreateItem.setImageResource(getContinuousIcon(mIsContinuousCreate));
                AppAnnotUtil.getInstance(mContext).showAnnotContinueCreateToast(mIsContinuousCreate);
            }
        });

        mUiExtensionsManager.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
        mUiExtensionsManager.getMainFrame().getToolSetBar().addView(mContinuousCreateItem, BaseBar.TB_Position.Position_CENTER);
    }

    private int getContinuousIcon(boolean isContinuous){
        int iconId;
        if (isContinuous) {
            iconId = R.drawable.rd_annot_create_continuously_true_selector;
        } else {
            iconId = R.drawable.rd_annot_create_continuously_false_selector;
        }
        return iconId;
    }

    private void getDrawRect(float x1, float y1, float x2, float y2) {
        float minx = Math.min(x1, x2);
        float miny = Math.min(y1, y2);
        float maxx = Math.max(x1, x2);
        float maxy = Math.max(y1, y2);

        mNowRect.left = minx;
        mNowRect.top = miny;
        mNowRect.right = maxx;
        mNowRect.bottom = maxy;
    }

    private RectF mPageViewThickness = new RectF(0, 0, 0, 0);
    private float thicknessOnPageView(int pageIndex, float thickness) {
        mPageViewThickness.set(0, 0, thickness, thickness);
        mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewThickness, mPageViewThickness, pageIndex);
        return Math.abs(mPageViewThickness.width());
    }

    private RectF getBBox(int pageIndex) {
        RectF bboxRect = new RectF(mNowRect);
        bboxRect.inset(-thicknessOnPageView(pageIndex, mThickness) / 2f, -thicknessOnPageView(pageIndex, mThickness) / 2f);
        return bboxRect;
    }

    private boolean onTouchEvent_SelectStart(int pageIndex, MotionEvent motionEvent) {
        PointF disPoint = new PointF(motionEvent.getX(), motionEvent.getY());
        PointF pvPoint = new PointF();
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(disPoint, pvPoint, pageIndex);
        float x = pvPoint.x;
        float y = pvPoint.y;

        int action = motionEvent.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mTouchCaptured && mLastPageIndex == -1 || mLastPageIndex == pageIndex) {
                    mTouchCaptured = true;
                    mStartPoint.x = x;
                    mStartPoint.y = y;
                    mStopPoint.x = x;
                    mStopPoint.y = y;
                    mDownPoint.set(x, y);
                    mTempRectInTouch.setEmpty();
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
                    float thickness = thicknessOnPageView(pageIndex, mThickness);
                    float deltaXY = thickness / 2 + mCtlPtLineWidth + mCtlPtRadius * 2 + 2;// Judging border value
                    float line_k = (y - mStartPoint.y) / (x - mStartPoint.x);
                    float line_b = mStartPoint.y - line_k * mStartPoint.x;
                    if (y <= deltaXY && line_k != 0) {
                        // whether select annot beyond a PDF page(pageView)
                        mStopPoint.y = deltaXY;
                        mStopPoint.x = (mStopPoint.y - line_b) / line_k;
                    } else if (y >= (mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY) && line_k != 0) {
                        mStopPoint.y = (mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY);
                        mStopPoint.x = (mStopPoint.y - line_b) / line_k;
                    }
                    if (mStopPoint.x <= deltaXY) {
                        mStopPoint.x = deltaXY;
                    } else if (mStopPoint.x >= mPdfViewCtrl.getPageViewWidth(pageIndex) - deltaXY) {
                        mStopPoint.x = mPdfViewCtrl.getPageViewWidth(pageIndex) - deltaXY;
                    }

                    getDrawRect(mStartPoint.x, mStartPoint.y, mStopPoint.x, mStopPoint.y);

                    mInvalidateRect.set((int) mNowRect.left, (int) mNowRect.top, (int) mNowRect.right, (int) mNowRect.bottom);
                    mInvalidateRect.inset((int) (-mThickness * 12f - mControlPtEx), (int) (-mThickness * 12f - mControlPtEx));
                    if (!mTempRectInTouch.isEmpty()) {
                        mInvalidateRect.union(AppDmUtil.rectToRectF(mTempRectInTouch));
                    }
                    mTempRectInTouch.set(AppDmUtil.rectFToRect(mInvalidateRect));
                    RectF _rect = new RectF(mInvalidateRect);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(_rect, _rect, pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(_rect));
                    mDownPoint.set(x, y);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mTouchCaptured || mLastPageIndex != pageIndex) {
                    if (!mInvalidateRect.isEmpty()) {
                        RectF _rect = new RectF(mInvalidateRect);
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(_rect, _rect, pageIndex);
                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(_rect));
                    }

                    mStartPoint.set(0, 0);
                    mStopPoint.set(0, 0);
                    mNowRect.setEmpty();
                    mStartPoint.set(0, 0);
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    mTouchCaptured = false;
                    mLastPageIndex = -1;
                    mSelectState = SELECT_START;
                    mState = STATE_NONE;
                    break;
                }
                boolean ret = false;
                if (!mStartPoint.equals(mStopPoint.x, mStopPoint.y)) {
                    mSelectState = hasSelectAnnots(pageIndex, getBBox(pageIndex));
                    mState = getFinalizeState(mCanDelete);

                    RectF _rect = new RectF(mInvalidateRect);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(_rect, _rect, pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(_rect));
                    ret = true;
                }

                mStartPoint.set(0, 0);
                mStopPoint.set(0, 0);
                mNowRect.setEmpty();
                mStartPoint.set(0, 0);
                mDownPoint.set(0, 0);
                mLastPoint.set(0, 0);
                mTouchCaptured = false;

                if (mSelectState == SELECT_ANNOT || mSelectState == SELECT_NONE) {
                    if (mSelectState == SELECT_ANNOT) {
                        mUiExtensionsManager.setCurrentToolHandler(null);
                        mUiExtensionsManager.getDocumentManager().setCurrentAnnot(mSelectAnnots.get(0));
                    }

                    mLastPageIndex = -1;
                    mSelectState = SELECT_START;
                    mState = STATE_NONE;
                } else if(mSelectState == SELECT_ANNOTS){
                    onAnnotsSelected(mSelectAnnots, pageIndex);
                } else if (mSelectState == SELECT_START) {
                    mLastPageIndex = -1;
                }
                return ret;
            default:
                return true;
        }

        return true;
    }

    private boolean mCanDelete = true;
    private ArrayList<Annot> mSelectAnnots = new ArrayList<>();
    private RectF mLastSelectAnnotsRect = new RectF();// page view rect;
    private RectF mSelectAnnotsRect = new RectF(); // pdf rect;
    private int hasSelectAnnots(int pageIndex, RectF rect) {
        if (!mPdfViewCtrl.isPageVisible(pageIndex)) return SELECT_NONE;
        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            int annotCount = page.getAnnotCount();
            if (annotCount == 0) return SELECT_NONE;
            mSelectAnnots.clear();
            mCanDelete = true;
            for (int i = 0; i < annotCount; i ++) {
                Annot annot = AppAnnotUtil.createAnnot(page.getAnnot(i));
                if (annot == null || (annot.getFlags() & Annot.e_FlagHidden) != 0
                        || !AppAnnotUtil.isSupportEditAnnot(annot)) continue;
                if (!mUiExtensionsManager.isLoadAnnotModule(annot)) continue;
                if (annot.getType() == Annot.e_Link) continue;
                Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
                RectF pvRect = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                if (AppAnnotUtil.isReplaceCaret(annot)) {
                    StrikeOut strikeOut = AppAnnotUtil.getStrikeOutFromCaret((Caret) annot);
                    if (strikeOut != null) {
                        RectF sto_Rect = AppUtil.toRectF(strikeOut.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                        pvRect.union(sto_Rect);
                    }
                }
                if (RectF.intersects(rect, pvRect)) {
                    mSelectAnnots.add(annot);
                    if (mSelectAnnots.size() == 1) {
                        mSelectAnnotsRect.set(pvRect);
                    } else {
                        mSelectAnnotsRect.union(pvRect);
                    }

                    if (mCanDelete) {
                        mCanDelete = !(AppAnnotUtil.isReadOnly(annot) || AppAnnotUtil.isLocked(annot));
                    }

                    if (AppUtil.isEmpty(annot.getUniqueID())) {
                        annot.setUniqueID(AppDmUtil.randomUUID(null));
                    }
                }
            }

            if (!mSelectAnnotsRect.isEmpty()) {
                mPdfViewCtrl.convertPageViewRectToPdfRect(mSelectAnnotsRect, mSelectAnnotsRect, pageIndex);
            }
            int state = SELECT_NONE;
            if (mSelectAnnots.size() == 1) {
                state = SELECT_ANNOT;
            } else if (mSelectAnnots.size() > 1) {
                state = SELECT_ANNOTS;
            }
            return state;
        } catch (PDFException e) {

        }
        return SELECT_NONE;
    }

    private int getState(Annot annot) {
        int annotType = AppAnnotUtil.getAnnotHandlerType(annot);
        int state = STATE_NONE;
        switch (annotType) {
            case Annot.e_Line://Arrow,Distance
            case Annot.e_Ink:
            case Annot.e_Square:
            case Annot.e_Circle:
            case Annot.e_PolyLine:
            case Annot.e_Polygon:
            case Annot.e_Stamp:
//            case Annot.e_Screen:
            case AnnotHandler.TYPE_SCREEN_IMAGE://Image
            case AnnotHandler.TYPE_SCREEN_MULTIMEDIA://Video,audio
            case AnnotHandler.TYPE_FREETEXT_CALLOUT:
            case AnnotHandler.TYPE_FREETEXT_TEXTBOX:
                state = STATE_DRAG_MOVE;
                break;
            case Annot.e_Redact:
                Redact redact = (Redact) annot;
                try {
                    QuadPointsArray quadPointsArray = redact.getQuadPoints();
                    if (quadPointsArray.getSize() > 0) {
                        state =  STATE_NONE;
                    } else {
                        state = STATE_DRAG_MOVE;
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }
                break;
            case Annot.e_Note:
            case Annot.e_FreeText://Typewriter
            case Annot.e_FileAttachment:
                state = STATE_MOVE;
                break;
            default://Annot.e_Highlight,Annot.e_Underline,Annot.e_StrikeOut,Annot.e_Squiggly,Annot.e_Caret(Insert/Replace)
                state = STATE_NONE;
                break;
        }

        return state;
    }

    private int getFinalizeState(boolean canEdit) {
        if (mSelectAnnots.size() == 0 || !canEdit) return STATE_NONE;
        int count_none = 0;
        int count_move = 0;
        int count_drag_move = 0;
        for (int i = 0; i< mSelectAnnots.size(); i ++) {
            if (getState(mSelectAnnots.get(i)) == STATE_NONE) {
                count_none ++;
            } else if (getState(mSelectAnnots.get(i)) == STATE_MOVE) {
                count_move ++;
            } else {//STATE_DRAG_MOVE
                count_drag_move ++;
            }
        }

        int state;
        if (count_none > 0) {
            state = STATE_NONE;
        } else if (count_none == 0 && count_move > 0) {
            state = STATE_MOVE;
        } else { // (count_move == 0 && count_none == 0 && count_drag_move > 0)
            state = STATE_DRAG_MOVE;
        }

        return state;
    }

    private ArrayList<Integer> mMenuText = new ArrayList<Integer>();;
    private AnnotMenu mAnnotationMenu;

    /**
     * reset mAnnotationMenu text
     */
    private void resetAnnotationMenuResource(boolean canDelete) {
        mMenuText.clear();

        if (mUiExtensionsManager.getDocumentManager().canAddAnnot()) {
            if (canDelete) {
                mMenuText.add(AnnotMenu.AM_BT_DELETE);
            }

            mMenuText.add(AnnotMenu.AM_BT_FLATTEN);
        }
    }

    private void prepareAnnotMenu(final ArrayList<Annot> annots, final int pageIndex) {
        resetAnnotationMenuResource(mCanDelete);
        mAnnotationMenu.setMenuItems(mMenuText);
        mAnnotationMenu.setListener(new AnnotMenu.ClickListener() {
            @Override
            public void onAMClick(int btType) {
                if (btType == AnnotMenu.AM_BT_DELETE) {
                    deleteAnnots(annots, pageIndex, true);
                } else if (btType == AnnotMenu.AM_BT_FLATTEN) {
                    flattenAnnots(annots, pageIndex);
                }
            }
        });
    }

    private void onAnnotsSelected(final ArrayList<Annot> annots, int pageIndex) {
        if (mAnnotationMenu == null) {
            mAnnotationMenu = new AnnotMenuImpl(mContext, mPdfViewCtrl);
        }
        prepareAnnotMenu(annots, pageIndex);
        RectF menuRect = new RectF(mSelectAnnotsRect);
        mPdfViewCtrl.convertPdfRectToPageViewRect(menuRect, menuRect, pageIndex);
        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(menuRect, menuRect, pageIndex);
        mAnnotationMenu.show(menuRect);
    }

    private boolean isHitSelectRect(RectF rectF, PointF point) {
        return rectF.contains(point.x, point.y);
    }

    private int isTouchControlPoint(RectF rect, float x, float y) {
        PointF[] ctlPts = calculateControlPoints(rect);
        RectF area = new RectF();
        int ret = -1;
        for (int i = 0; i < ctlPts.length; i++) {
            area.set(ctlPts[i].x, ctlPts[i].y, ctlPts[i].x, ctlPts[i].y);
            area.inset(-mCtlPtTouchExt, -mCtlPtTouchExt);
            if (area.contains(x, y)) {
                ret = i + 1;
            }
        }
        return ret;
    }

    private PointF mAdjustPointF = new PointF(0, 0);

    private PointF adjustScalePointF(int pageIndex, RectF rectF, float dxy) {
        float adjustx = 0;
        float adjusty = 0;
        if (mLastOper != OPER_TRANSLATE) {
            rectF.inset(-mThickness / 2f, -mThickness / 2f);
        }

        if ((int) rectF.left < dxy) {
            adjustx = -rectF.left + dxy;
            rectF.left = dxy;
        }
        if ((int) rectF.top < dxy) {
            adjusty = -rectF.top + dxy;
            rectF.top = dxy;
        }

        if ((int) rectF.right > mPdfViewCtrl.getPageViewWidth(pageIndex) - dxy) {
            adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - rectF.right - dxy;
            rectF.right = mPdfViewCtrl.getPageViewWidth(pageIndex) - dxy;
        }
        if ((int) rectF.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex) - dxy) {
            adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - rectF.bottom - dxy;
            rectF.bottom = mPdfViewCtrl.getPageViewHeight(pageIndex) - dxy;
        }
        mAdjustPointF.set(adjustx, adjusty);
        return mAdjustPointF;
    }

    /*
     *  LT     T     RT
     *   1-----2-----3
     *   |	         |
     *   |	         |
     * L 8           4 R
     *   |           |
     *   |           |
     *   7-----6-----5
     *   LB    B     RB
     *   */

    public static final int CTR_NONE = -1;
    public static final int CTR_LT = 1;
    public static final int CTR_T = 2;
    public static final int CTR_RT = 3;
    public static final int CTR_R = 4;
    public static final int CTR_RB = 5;
    public static final int CTR_B = 6;
    public static final int CTR_LB = 7;
    public static final int CTR_L = 8;
    private int mCurrentCtr = CTR_NONE;

    public static final int OPER_DEFAULT = -1;
    public static final int OPER_SCALE_LT = 1;// old:start at 0
    public static final int OPER_SCALE_T = 2;
    public static final int OPER_SCALE_RT = 3;
    public static final int OPER_SCALE_R = 4;
    public static final int OPER_SCALE_RB = 5;
    public static final int OPER_SCALE_B = 6;
    public static final int OPER_SCALE_LB = 7;
    public static final int OPER_SCALE_L = 8;
    public static final int OPER_TRANSLATE = 9;
    private int mLastOper = OPER_DEFAULT;

    private float mCtlPtTouchExt = 20;
    private float mCtlPtDeltyXY = 20;// Additional refresh range

    private PointF mDocViewerPt = new PointF(0, 0);
    private RectF mPageViewRect = new RectF(0, 0, 0, 0);

    private RectF mPageDrawRect = new RectF();
    private RectF mAnnotMenuRect = new RectF(0, 0, 0, 0);
    private boolean onTouchEvent_SelectAnnots(int pageIndex, MotionEvent motionEvent) {
        // in pageView evX and evY
        PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        float evX = point.x;
        float evY = point.y;
        RectF selectRect = new RectF(mSelectAnnotsRect);
        mPdfViewCtrl.convertPdfRectToPageViewRect(selectRect, selectRect, pageIndex);
        if (!isHitSelectRect(selectRect, point) && mLastOper == OPER_DEFAULT && mCurrentCtr == CTR_NONE) {
            if (mState == STATE_DRAG_MOVE) {
                mCurrentCtr = isTouchControlPoint(selectRect, evX, evY);
            }

            if (mCurrentCtr == CTR_NONE) {
                mSelectState = SELECT_START;
                mAnnotationMenu.dismiss();

                RectF rectF = new RectF(selectRect);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, pageIndex);
                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rectF));

                mSelectAnnotsRect.setEmpty();
                return onTouchEvent_SelectStart(pageIndex, motionEvent); // re-select annots
            }
        }

        if (mState == STATE_NONE) return true;
        int action = motionEvent.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:

                if (mLastPageIndex == pageIndex && mSelectAnnots.size() > 1) {
                    mThickness = thicknessOnPageView(pageIndex, mSelectAreaBound);
                    mPageViewRect.set(selectRect);
                    mPageViewRect.inset(mThickness / 2f, mThickness / 2f);

                    if (mState == STATE_DRAG_MOVE) {
                        mCurrentCtr = isTouchControlPoint(selectRect, evX, evY);
                    }

                    mDownPoint.set(evX, evY);
                    mLastPoint.set(evX, evY);
                    mDocViewerPt.set(motionEvent.getX(), motionEvent.getY());

                    if (mCurrentCtr == CTR_LT) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_LT;
                        return true;
                    } else if (mCurrentCtr == CTR_T) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_T;
                        return true;
                    } else if (mCurrentCtr == CTR_RT) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_RT;
                        return true;
                    } else if (mCurrentCtr == CTR_R) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_R;
                        return true;
                    } else if (mCurrentCtr == CTR_RB) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_RB;
                        return true;
                    } else if (mCurrentCtr == CTR_B) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_B;
                        return true;
                    } else if (mCurrentCtr == CTR_LB) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_LB;
                        return true;
                    } else if (mCurrentCtr == CTR_L) {
                        mTouchCaptured = true;
                        mLastOper = OPER_SCALE_L;
                        return true;
                    } else if (isHitSelectRect(selectRect, point)) {
                        mTouchCaptured = true;
                        mLastOper = OPER_TRANSLATE;
                        return true;
                    }
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                if (pageIndex == mLastPageIndex && mTouchCaptured && mSelectAnnots.size() > 1
                        && ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                        float deltaXY = mCtlPtLineWidth + mCtlPtRadius * 2 + 2;// Judging border value
                        switch (mLastOper) {
                            case OPER_TRANSLATE: {
                                mInvalidateRect.set(selectRect);
                                mAnnotMenuRect.set(selectRect);
                                mInvalidateRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                mAnnotMenuRect.offset(evX - mDownPoint.x, evY - mDownPoint.y);
                                PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                mInvalidateRect.union(mAnnotMenuRect);

                                mInvalidateRect.inset(-deltaXY - mCtlPtDeltyXY, -deltaXY - mCtlPtDeltyXY);
                                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                if (mAnnotationMenu.isShowing()) {
                                    mAnnotationMenu.dismiss();
                                    mAnnotationMenu.update(mAnnotMenuRect);
                                }

                                mLastPoint.set(evX, evY);
                                mLastPoint.offset(adjustXY.x, adjustXY.y);
                                break;
                            }
                            case OPER_SCALE_LT: {
                                if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                    mInvalidateRect.set(mLastPoint.x, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
                                    mAnnotMenuRect.set(evX, evY, mPageViewRect.right, mPageViewRect.bottom);
                                    mInvalidateRect.sort();
                                    mAnnotMenuRect.sort();
                                    mInvalidateRect.union(mAnnotMenuRect);
                                    mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotationMenu.isShowing()) {
                                        mAnnotationMenu.dismiss();
                                        mAnnotationMenu.update(mAnnotMenuRect);
                                    }

                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                }
                                break;
                            }
                            case OPER_SCALE_T: {
                                if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                    mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
                                    mAnnotMenuRect.set(mPageViewRect.left, evY, mPageViewRect.right, mPageViewRect.bottom);
                                    mInvalidateRect.sort();
                                    mAnnotMenuRect.sort();
                                    mInvalidateRect.union(mAnnotMenuRect);
                                    mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotationMenu.isShowing()) {
                                        mAnnotationMenu.dismiss();
                                        mAnnotationMenu.update(mAnnotMenuRect);
                                    }

                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                }
                                break;
                            }
                            case OPER_SCALE_RT: {
                                if (evX != mLastPoint.x && evY != mLastPoint.y) {

                                    mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mLastPoint.x, mPageViewRect.bottom);
                                    mAnnotMenuRect.set(mPageViewRect.left, evY, evX, mPageViewRect.bottom);
                                    mInvalidateRect.sort();
                                    mAnnotMenuRect.sort();
                                    mInvalidateRect.union(mAnnotMenuRect);
                                    mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotationMenu.isShowing()) {
                                        mAnnotationMenu.dismiss();
                                        mAnnotationMenu.update(mAnnotMenuRect);
                                    }
                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                }
                                break;
                            }
                            case OPER_SCALE_R: {
                                if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                    mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mPageViewRect.bottom);
                                    mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, evX, mPageViewRect.bottom);
                                    mInvalidateRect.sort();
                                    mAnnotMenuRect.sort();
                                    mInvalidateRect.union(mAnnotMenuRect);
                                    mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotationMenu.isShowing()) {
                                        mAnnotationMenu.dismiss();
                                        mAnnotationMenu.update(mAnnotMenuRect);
                                    }

                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                }
                                break;
                            }
                            case OPER_SCALE_RB: {
                                if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                    mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mLastPoint.y);
                                    mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, evX, evY);
                                    mInvalidateRect.sort();
                                    mAnnotMenuRect.sort();
                                    mInvalidateRect.union(mAnnotMenuRect);
                                    mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotationMenu.isShowing()) {
                                        mAnnotationMenu.dismiss();
                                        mAnnotationMenu.update(mAnnotMenuRect);
                                    }

                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                }
                                break;
                            }
                            case OPER_SCALE_B: {
                                if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                    mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                    mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, evY);
                                    mInvalidateRect.sort();
                                    mAnnotMenuRect.sort();
                                    mInvalidateRect.union(mAnnotMenuRect);
                                    mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotationMenu.isShowing()) {
                                        mAnnotationMenu.dismiss();
                                        mAnnotationMenu.update(mAnnotMenuRect);
                                    }

                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                }
                                break;
                            }
                            case OPER_SCALE_LB: {
                                if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                    mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                    mAnnotMenuRect.set(evX, mPageViewRect.top, mPageViewRect.right, evY);
                                    mInvalidateRect.sort();
                                    mAnnotMenuRect.sort();
                                    mInvalidateRect.union(mAnnotMenuRect);
                                    mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotationMenu.isShowing()) {
                                        mAnnotationMenu.dismiss();
                                        mAnnotationMenu.update(mAnnotMenuRect);
                                    }

                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                }
                                break;
                            }
                            case OPER_SCALE_L: {
                                if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                    mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
                                    mAnnotMenuRect.set(evX, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
                                    mInvalidateRect.sort();
                                    mAnnotMenuRect.sort();
                                    mInvalidateRect.union(mAnnotMenuRect);
                                    mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotationMenu.isShowing()) {
                                        mAnnotationMenu.dismiss();
                                        mAnnotationMenu.update(mAnnotMenuRect);
                                    }

                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                }
                                break;

                            }
                            default:
                                break;
                        }
                    }
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean ret = false;
                if (mTouchCaptured && mSelectAnnots.size() >1 && pageIndex == mLastPageIndex) {
                    RectF pageViewRect = new RectF(selectRect);
                    pageViewRect.inset(mThickness / 2, mThickness / 2);
                    switch (mLastOper) {
                        case OPER_TRANSLATE: {
                            mPageDrawRect.set(pageViewRect);
                            mPageDrawRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                            break;
                        }
                        case OPER_SCALE_LT: {
                            if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                mPageDrawRect.set(mLastPoint.x, mLastPoint.y, pageViewRect.right, pageViewRect.bottom);
                            }
                            break;
                        }
                        case OPER_SCALE_T: {
                            if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                mPageDrawRect.set(pageViewRect.left, mLastPoint.y, pageViewRect.right, pageViewRect.bottom);
                            }
                            break;
                        }
                        case OPER_SCALE_RT: {
                            if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                mPageDrawRect.set(pageViewRect.left, mLastPoint.y, mLastPoint.x, pageViewRect.bottom);
                            }
                            break;
                        }
                        case OPER_SCALE_R: {
                            if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, pageViewRect.bottom);
                            }
                            break;
                        }
                        case OPER_SCALE_RB: {
                            if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, mLastPoint.y);
                            }
                            break;
                        }
                        case OPER_SCALE_B: {
                            if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                mPageDrawRect.set(pageViewRect.left, pageViewRect.top, pageViewRect.right, mLastPoint.y);
                            }
                            break;
                        }
                        case OPER_SCALE_LB: {
                            if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, mLastPoint.y);
                            }
                            break;
                        }
                        case OPER_SCALE_L: {
                            if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, pageViewRect.bottom);
                            }
                            break;
                        }
                        default:
                            break;
                    }

                    RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
                    viewDrawBox.inset(-thicknessOnPageView(pageIndex, mSelectAreaBound) / 2, -thicknessOnPageView(pageIndex, mSelectAreaBound) / 2);
                    normalize(viewDrawBox);
                    if (mLastOper != OPER_DEFAULT && !mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                        RectF bboxRect = new RectF(viewDrawBox);
                        mLastSelectAnnotsRect.set(selectRect);
                        modifyAnnots(mLastSelectAnnotsRect, bboxRect, pageIndex, true);
                        mPdfViewCtrl.convertPageViewRectToPdfRect(bboxRect, mSelectAnnotsRect, pageIndex);
//                        mSelectAnnotsRect.set(bboxRect);
                    }
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
                    if (mAnnotationMenu.isShowing()) {
                        mAnnotationMenu.update(viewDrawBox);
                    } else {
                        mAnnotationMenu.show(viewDrawBox);
                    }
                    ret = true;
                }

                mTouchCaptured = false;
                mDownPoint.set(0, 0);
                mLastPoint.set(0, 0);
                mLastOper = OPER_DEFAULT;
                mCurrentCtr = CTR_NONE;
                return ret;
        }
        return false;
    }

    private RectF mViewDrawRect = new RectF(0, 0, 0, 0);
    private RectF mDocViewerBBox = new RectF(0, 0, 0, 0);

    public void onDrawForControls(Canvas canvas) {
        if (mSelectAnnots.size() > 1 && mUiExtensionsManager.getCurrentToolHandler() == this) {
            if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
                float thickness = thicknessOnPageView(mLastPageIndex, mSelectAreaBound);
                RectF selectAnnotsRect = new RectF(mSelectAnnotsRect);
                mPdfViewCtrl.convertPdfRectToPageViewRect(selectAnnotsRect, selectAnnotsRect, mLastPageIndex);
                mViewDrawRect.set(selectAnnotsRect);
                mViewDrawRect.inset(thickness / 2f, thickness / 2f);
                if (mLastOper == OPER_SCALE_LT) {
                    mDocViewerBBox.left = mLastPoint.x;
                    mDocViewerBBox.top = mLastPoint.y;
                    mDocViewerBBox.right = mViewDrawRect.right;
                    mDocViewerBBox.bottom = mViewDrawRect.bottom;
                } else if (mLastOper == OPER_SCALE_T) {
                    mDocViewerBBox.left = mViewDrawRect.left;
                    mDocViewerBBox.top = mLastPoint.y;
                    mDocViewerBBox.right = mViewDrawRect.right;
                    mDocViewerBBox.bottom = mViewDrawRect.bottom;
                } else if (mLastOper == OPER_SCALE_RT) {
                    mDocViewerBBox.left = mViewDrawRect.left;
                    mDocViewerBBox.top = mLastPoint.y;
                    mDocViewerBBox.right = mLastPoint.x;
                    mDocViewerBBox.bottom = mViewDrawRect.bottom;
                } else if (mLastOper == OPER_SCALE_R) {
                    mDocViewerBBox.left = mViewDrawRect.left;
                    mDocViewerBBox.top = mViewDrawRect.top;
                    mDocViewerBBox.right = mLastPoint.x;
                    mDocViewerBBox.bottom = mViewDrawRect.bottom;
                } else if (mLastOper == OPER_SCALE_RB) {
                    mDocViewerBBox.left = mViewDrawRect.left;
                    mDocViewerBBox.top = mViewDrawRect.top;
                    mDocViewerBBox.right = mLastPoint.x;
                    mDocViewerBBox.bottom = mLastPoint.y;
                } else if (mLastOper == OPER_SCALE_B) {
                    mDocViewerBBox.left = mViewDrawRect.left;
                    mDocViewerBBox.top = mViewDrawRect.top;
                    mDocViewerBBox.right = mViewDrawRect.right;
                    mDocViewerBBox.bottom = mLastPoint.y;
                } else if (mLastOper == OPER_SCALE_LB) {
                    mDocViewerBBox.left = mLastPoint.x;
                    mDocViewerBBox.top = mViewDrawRect.top;
                    mDocViewerBBox.right = mViewDrawRect.right;
                    mDocViewerBBox.bottom = mLastPoint.y;
                } else if (mLastOper == OPER_SCALE_L) {
                    mDocViewerBBox.left = mLastPoint.x;
                    mDocViewerBBox.top = mViewDrawRect.top;
                    mDocViewerBBox.right = mViewDrawRect.right;
                    mDocViewerBBox.bottom = mViewDrawRect.bottom;
                }
                mDocViewerBBox.inset(-thickness / 2f, -thickness / 2f);
                if (mLastOper == OPER_TRANSLATE || mLastOper == OPER_DEFAULT) {
                    mDocViewerBBox.set(selectAnnotsRect);
                    float dx = mLastPoint.x - mDownPoint.x;
                    float dy = mLastPoint.y - mDownPoint.y;

                    mDocViewerBBox.offset(dx, dy);
                }

                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDocViewerBBox, mDocViewerBBox, mLastPageIndex);
                mAnnotationMenu.update(mDocViewerBBox);

            }

        }

    }

    private void modifyAnnots(final RectF oldRect, final RectF newRect, final int pageIndex, final boolean addUndo) {
        RectF oldSelectRect = new RectF();
        RectF newSelectRect = new RectF();

        mPdfViewCtrl.convertPageViewRectToPdfRect(oldRect, oldSelectRect, pageIndex);
        mPdfViewCtrl.convertPageViewRectToPdfRect(newRect, newSelectRect, pageIndex);

        float offsetX = newSelectRect.left;
        float offsetY = newSelectRect.bottom;
        float scaleX = (newSelectRect.right - newSelectRect.left) / (oldSelectRect.right - oldSelectRect.left);
        float scaleY = (newSelectRect.bottom - newSelectRect.top) / (oldSelectRect.bottom - oldSelectRect.top);

        final MultiSelectModifyUndoItem modifyUndoItem = new MultiSelectModifyUndoItem(mPdfViewCtrl);
        modifyUndoItem.mPageIndex = pageIndex;
        modifyUndoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
        RectF tmpRect = new RectF();
        try {
            for (int i = 0; i < mSelectAnnots.size(); i ++) {
                Annot annot = mSelectAnnots.get(i);
                RectF annontRect = AppUtil.toRectF(annot.getRect());
                AppUtil.normalizePDFRect(annontRect);
                String nm = annot.getUniqueID();
                modifyUndoItem.mNMList.add(nm);
                modifyUndoItem.mLastAnnots.put(nm, annontRect);
                tmpRect.set(annontRect);
                tmpRect.offset(-oldSelectRect.left, -oldSelectRect.bottom);
                RectF newAnnotRect = new RectF(
                        tmpRect.left * scaleX + offsetX,
                        tmpRect.top * scaleY + offsetY,
                        tmpRect.right * scaleX + offsetX,
                        tmpRect.bottom * scaleY + offsetY);

                modifyUndoItem.mCurrentAnnots.put(nm, newAnnotRect);
            }

            mUiExtensionsManager.getDocumentManager().setHasModifyTask(addUndo);
            EditAnnotEvent modifyEvent = new MultiSelectEvent(EditAnnotEvent.EVENTTYPE_MODIFY, modifyUndoItem, mSelectAnnots, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(modifyEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {

                        try {
                            for (int i = 0; i < mSelectAnnots.size(); i ++) {
                                mUiExtensionsManager.getDocumentManager().onAnnotModified(mSelectAnnots.get(i).getPage(), mSelectAnnots.get(i));
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }

                        if (addUndo) {
                            mUiExtensionsManager.getDocumentManager().addUndoItem(modifyUndoItem);
                            mUiExtensionsManager.getDocumentManager().setHasModifyTask(false);
                        }
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            RectF invalidateRect = new RectF(newRect);
                            invalidateRect.union(oldRect);
                            invalidateRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(invalidateRect));
                        }
                    }
                }
            });

            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void deleteAnnots(final ArrayList<Annot> annots, final int pageIndex, final boolean addUndo) {
        final ArrayList<EditAnnotEvent> eventList = new ArrayList<>();
        final ArrayList<AnnotUndoItem> undoItems = new ArrayList<>();
        final MultiSelectDeleteUndoItem undoItem = new MultiSelectDeleteUndoItem(mPdfViewCtrl);
        mUiExtensionsManager.getDocumentManager().setMultipleSelectAnnots(true);
        for (Annot annot: annots) {
            mUiExtensionsManager.getDocumentManager().removeAnnot(annot, false, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success && event instanceof EditAnnotEvent) {
                        eventList.add((EditAnnotEvent) event);
                        undoItems.add(((EditAnnotEvent) event).mUndoItem);
                        undoItem.mNMList.add(((EditAnnotEvent) event).mUndoItem.mNM);
                    }
                }
            });
        }
        undoItem.mUndoItemList = undoItems;
        undoItem.mPageIndex = pageIndex;
        mUiExtensionsManager.getDocumentManager().setHasModifyTask(addUndo);
        MultiSelectEvent event = new MultiSelectEvent(EditAnnotEvent.EVENTTYPE_DELETE, eventList, mPdfViewCtrl);
        EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                mUiExtensionsManager.getDocumentManager().setMultipleSelectAnnots(false);
                if (success) {
                    try {
                        for (int i = 0; i < annots.size(); i ++) {
                            mUiExtensionsManager.getDocumentManager().onAnnotDeleted(mPdfViewCtrl.getDoc().getPage(pageIndex), annots.get(i));
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                    if (addUndo) {
                        mUiExtensionsManager.getDocumentManager().addUndoItem(undoItem);
                        mUiExtensionsManager.getDocumentManager().setHasModifyTask(false);
                    }
                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        RectF invalidateRect = new RectF(mSelectAnnotsRect);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(invalidateRect, invalidateRect, mLastPageIndex);
                        invalidateRect.union(mLastSelectAnnotsRect);
                        invalidateRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                        mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(invalidateRect));
                    }
                }
                if (mIsContinuousCreate) {
                    reset();
                } else {
                    mUiExtensionsManager.setCurrentToolHandler(null);
                }
            }
        });
        mPdfViewCtrl.addTask(task);
    }

    private void flattenAnnots(ArrayList<Annot> annots, final int pageIndex) {
        flattenAnnot(mPdfViewCtrl, annots, pageIndex);
    }

    private void flattenAnnot(PDFViewCtrl pdfViewCtrl, final ArrayList<Annot> annots, final int pageIndex) {
        if (annots == null || annots.size() == 0) return;
        final UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager();
        final Context context = uiExtensionsManager.getAttachedActivity().getApplicationContext();

        final UITextEditDialog dialog = new UITextEditDialog(uiExtensionsManager.getAttachedActivity());
        dialog.getInputEditText().setVisibility(View.GONE);
        dialog.setTitle(AppResource.getString(context, R.string.fx_string_flatten));
        dialog.getPromptTextView().setText(AppResource.getString(context, R.string.fx_flatten_toast));
        dialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.getOKButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                final FxProgressDialog progressDialog = new FxProgressDialog(uiExtensionsManager.getAttachedActivity(),
                        AppResource.getString(context, R.string.fx_string_processing));
                progressDialog.show();
                doAnnotsFlatten(annots, pageIndex, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        progressDialog.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }

    private void doAnnotsFlatten(final ArrayList<Annot> annots, final int pageIndex, final Event.Callback callback) {
        final ArrayList<EditAnnotEvent> eventList = new ArrayList<>();
        mUiExtensionsManager.getDocumentManager().setMultipleSelectAnnots(true);
        for (Annot annot: annots) {
            UIAnnotFlatten.flatten(mPdfViewCtrl, annot, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success && event instanceof EditAnnotEvent) {
                        eventList.add((EditAnnotEvent) event);
                    }
                }
            });
        }

        MultiSelectEvent event = new MultiSelectEvent(EditAnnotEvent.EVENTTYPE_FLATTEN, eventList, mPdfViewCtrl);
        EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                mUiExtensionsManager.getDocumentManager().setMultipleSelectAnnots(false);
                if (success) {
                    try {
                        for (int i = 0; i < annots.size(); i ++) {
                            mUiExtensionsManager.getDocumentManager().onAnnotFlattened(mPdfViewCtrl.getDoc().getPage(pageIndex), annots.get(i));
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                    mUiExtensionsManager.getDocumentManager().setDocModified(true);
                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        RectF invalidateRect = new RectF(mSelectAnnotsRect);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(invalidateRect, invalidateRect, mLastPageIndex);
                        invalidateRect.union(mLastSelectAnnotsRect);
                        invalidateRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                        mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(invalidateRect));
                    }
                }
                if (mIsContinuousCreate) {
                    reset();
                } else {
                    mUiExtensionsManager.setCurrentToolHandler(null);
                }

                if (callback != null) {
                    callback.result(null, success);
                }
            }
        });
        mPdfViewCtrl.addTask(task);
    }

    private void reset() {
        mSelectAnnots.clear();
        mLastPageIndex = -1;
        mSelectState = SELECT_START;
        mState = STATE_NONE;
        if (mAnnotationMenu != null) {
            mAnnotationMenu.setListener(null);
            if (mAnnotationMenu.isShowing()) {
                mAnnotationMenu.dismiss();
            }
        }
    }

    private void normalize(int pageIndex, RectF rectF, float dxy) {
        if ((int) rectF.left < dxy) {
            rectF.left = dxy;
        }
        if ((int) rectF.top < dxy) {
            rectF.top = dxy;
        }

        if ((int) rectF.right > mPdfViewCtrl.getPageViewWidth(pageIndex) - dxy) {
            rectF.right = mPdfViewCtrl.getPageViewWidth(pageIndex) - dxy;
        }
        if ((int) rectF.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex) - dxy) {
            rectF.bottom = mPdfViewCtrl.getPageViewHeight(pageIndex) - dxy;
        }
    }

    private void normalize(RectF rectF) {
        if (rectF.left > rectF.right) {
            float tmp = rectF.left;
            rectF.left = rectF.right;
            rectF.right = tmp;
        }

        if (rectF.top > rectF.bottom) {
            float tmp = rectF.top;
            rectF.top = rectF.bottom;
            rectF.bottom = tmp;
        }

        if (rectF.left == rectF.right) rectF.right += 1;
        if (rectF.top == rectF.bottom) rectF.bottom += 1;
    }
}
