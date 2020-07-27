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
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.annots.AbstractToolHandler;
import com.foxit.uiextensions.annots.common.UIAnnotFrame;
import com.foxit.uiextensions.controls.propertybar.MoreTools;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.PropertyCircleItem;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.PropertyCircleItemImp;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;


public class InkToolHandler extends AbstractToolHandler {
    public static final int IA_MIN_DIST = 2;
    protected static final String PROPERTY_KEY = "INK";

    protected InkAnnotHandler mAnnotHandler;
    protected InkAnnotUtil mUtil;

    private boolean mTouchCaptured = false;
    private int mCapturedPage = -1;

    private ArrayList<ArrayList<PointF>> mLineList;
    private ArrayList<PointF> mLine;
    private ArrayList<Path> mPathList;
    private Path mPath;
    private PointF mLastPt = new PointF(0, 0);
    private Paint mPaint;

    private PropertyCircleItem mPropertyItem;
    private IBaseItem mOKItem;

    public InkToolHandler(Context context, PDFViewCtrl pdfViewCtrl, InkAnnotUtil util) {
        super(context, pdfViewCtrl, Module.MODULE_NAME_INK, PROPERTY_KEY);
        mColor = PropertyBar.PB_COLORS_PENCIL[6];

        mUtil = util;
        mLineList = new ArrayList<ArrayList<PointF>>();
        mPathList = new ArrayList<Path>();

        mPaint = new Paint();
        mPaint.setStyle(Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);

        if (mUiExtensionsManager.getConfig().modules.getAnnotConfig().isLoadDrawPencil()){
            mColor = mUiExtensionsManager.getConfig().uiSettings.annotations.pencil.color;
            mOpacity =(int) (mUiExtensionsManager.getConfig().uiSettings.annotations.pencil.opacity * 100);
            mThickness = mUiExtensionsManager.getConfig().uiSettings.annotations.pencil.thickness;

            mUiExtensionsManager.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
                @Override
                public void onMTClick(int type) {
                    mUiExtensionsManager.setCurrentToolHandler(InkToolHandler.this);
                    mUiExtensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
                }

                @Override
                public int getType() {
                    return MoreTools.MT_TYPE_INK;
                }
            });
        }
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_INK;
    }

    protected void initUiElements() {

    }

    protected void uninitUiElements() {
        removeToolButton();
    }

    @Override
    public void updateToolButtonStatus() {

    }

    @Override
    public void setColor(int color) {
        if (mColor == color) return;
        addAnnot(null);
        mColor = color;
    }

    @Override
    public void setOpacity(int opacity) {
        if (mOpacity == opacity) return;
        addAnnot(null);
        mOpacity = opacity;
    }

    @Override
    public void setThickness(float thickness) {
        if (mThickness == thickness) return;
        addAnnot(null);
        mThickness = thickness;
    }

    @Override
    public void onActivate() {
        mCapturedPage = -1;
        mLineList.clear();
        mPathList.clear();

        resetPropertyBar();
        resetAnnotBar();
    }

    private void resetPropertyBar() {
        int[] colors = new int[PropertyBar.PB_COLORS_PENCIL.length];
        System.arraycopy(PropertyBar.PB_COLORS_PENCIL, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_PENCIL[0];
        mPropertyBar.setColors(colors);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, mColor);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, mOpacity);
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

                if (InkToolHandler.this == mUiExtensionsManager.getCurrentToolHandler()) {
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

        setColorChangeListener(new ColorChangeListener() {
            @Override
            public void onColorChange(int color) {
                mPropertyItem.setCentreCircleColor(color);
            }
        });

        mIsContinuousCreate = true;
        mUiExtensionsManager.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mUiExtensionsManager.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
    }

    @Override
    public void onDeactivate() {

        if (mTouchCaptured) {
            mTouchCaptured = false;

            if (mLine != null) {
                mLineList.add(mLine);
                mLine = null;
            }
            mLastPt.set(0, 0);
        }
        addAnnot(null);

    }

    float mbx, mby, mcx, mcy, mex, mey;
    PointF tv_pt = new PointF();
    RectF tv_invalid = new RectF();
    boolean isEndTouch;
    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e) {
        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        float thickness = UIAnnotFrame.getPageViewThickness(mPdfViewCtrl, pageIndex, mThickness);
        int action = e.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isEndTouch= false;
                if (!mTouchCaptured) {
                    if (mCapturedPage == -1) {
                        mTouchCaptured = true;
                        mCapturedPage = pageIndex;
                    } else if (pageIndex == mCapturedPage) {
                        mTouchCaptured = true;
                    } else if (mPdfViewCtrl.getPageLayoutMode()== PDFViewCtrl.PAGELAYOUTMODE_FACING){
                        // addAnnot(null);
                        mPathList.clear();
                        mTouchCaptured = true;
                        mCapturedPage = pageIndex;
                    }
                    if (mTouchCaptured) {
                        mPath = new Path();
                        mPath.moveTo(point.x, point.y);
                        mbx = point.x;
                        mby = point.y;
                        mcx = point.x;
                        mcy = point.y;

                        mLine = new ArrayList<PointF>();
                        mLine.add(new PointF(point.x, point.y));
                        mLastPt.set(point.x, point.y);
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mTouchCaptured) {
                    tv_pt.set(point);
                    InkAnnotUtil.correctPvPoint(mPdfViewCtrl, pageIndex, tv_pt);
                    float dx = Math.abs(tv_pt.x - mcx);
                    float dy = Math.abs(tv_pt.y - mcy);
                    if (mCapturedPage == pageIndex && (dx >= IA_MIN_DIST || dy >= IA_MIN_DIST)) {
                        // history points
                        tv_invalid.set(tv_pt.x, tv_pt.y, tv_pt.x, tv_pt.y);
                        for (int i = 0; i < e.getHistorySize(); i++) {
                            tv_pt.set(e.getHistoricalX(i), e.getHistoricalY(i));
                            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(tv_pt, tv_pt, pageIndex);
                            InkAnnotUtil.correctPvPoint(mPdfViewCtrl, pageIndex, tv_pt);
                            if (tv_pt.x - mLastPt.x >= IA_MIN_DIST || tv_pt.y - mLastPt.y >= IA_MIN_DIST) {
                                mex = (mcx + tv_pt.x) / 2;
                                mey = (mcy + tv_pt.y) / 2;
                                mLine.add(new PointF(tv_pt.x, tv_pt.y));
                                mPath.quadTo(mcx, mcy, mex, mey);
                                mLastPt.set(tv_pt);
                                tv_invalid.union(mbx, mby);
                                tv_invalid.union(mcx, mcy);
                                tv_invalid.union(mex, mey);
                                mbx = mex;
                                mby = mey;
                                mcx = tv_pt.x;
                                mcy = tv_pt.y;
                            }
                        }
                        // current point
                        tv_pt.set(point);
                        InkAnnotUtil.correctPvPoint(mPdfViewCtrl, pageIndex, tv_pt);
                        mex = (mcx + tv_pt.x) / 2;
                        mey = (mcy + tv_pt.y) / 2;
                        mLine.add(new PointF(tv_pt.x, tv_pt.y));
                        mPath.quadTo(mcx, mcy, mex, mey);
                        mLastPt.set(tv_pt.x, tv_pt.y);
                        tv_invalid.union(mbx, mby);
                        tv_invalid.union(mcx, mcy);
                        tv_invalid.union(mex, mey);
                        mbx = mex;
                        mby = mey;
                        mcx = tv_pt.x;
                        mcy = tv_pt.y;
                        tv_invalid.inset(-thickness, -thickness);
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(tv_invalid, tv_invalid, pageIndex);
                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(tv_invalid));
                    }
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        tv_pt.set(point);
                        InkAnnotUtil.correctPvPoint(mPdfViewCtrl, pageIndex, tv_pt);
                        if (mLine.size() == 1) {
                            if (tv_pt.equals(mLine.get(0))) {
                                tv_pt.x += 0.1;
                                tv_pt.y += 0.1;
                            }
                            mex = (mcx + tv_pt.x) / 2;
                            mey = (mcy + tv_pt.y) / 2;
                            mLine.add(new PointF(tv_pt.x, tv_pt.y));
                            mPath.quadTo(mcx, mcy, mex, mey);
                            mLastPt.set(tv_pt.x, tv_pt.y);
                        }
                        mPath.lineTo(mLastPt.x, mLastPt.y);
                        mPathList.add(mPath);
                        mPath = null;
                        tv_invalid.set(mbx, mby, mbx, mby);
                        tv_invalid.union(mcx, mcy);
                        tv_invalid.union(mex, mey);
                        tv_invalid.union(mLastPt.x, mLastPt.y);
                        tv_invalid.inset(-thickness, -thickness);
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(tv_invalid, tv_invalid, pageIndex);
                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(tv_invalid));
                        mLineList.add(mLine);
                        mLine = null;
                        mTouchCaptured = false;
                        mLastPt.set(0, 0);
                        isEndTouch = true;
                    }
                    return true;
                }
                break;
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

        if (mPathList != null && mCapturedPage == pageIndex) {
            // draw current creating annotation
            setPaintProperty(mPdfViewCtrl, pageIndex, mPaint);
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));

            int count = mPathList.size();
            for (int i = 0; i < count; i++) {
                canvas.drawPath(mPathList.get(i), mPaint);
            }
            if (mPath != null) {
                canvas.drawPath(mPath, mPaint);
            }
            if(isEndTouch){
                addAnnot(null);
                isEndTouch= false;
            }
        }
    }

    @Override
    protected void setPaintProperty(PDFViewCtrl pdfViewCtrl, int pageIndex, Paint paint) {
        paint.setColor(mColor);
        paint.setAlpha(AppDmUtil.opacity100To255(mOpacity));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(UIAnnotFrame.getPageViewThickness(pdfViewCtrl, pageIndex, mThickness));
    }

    @Override
    public long getSupportedProperties() {
        return mUtil.getSupportedProperties();
    }

    @Override
    protected void setPropertyBarProperties(PropertyBar propertyBar) {
        int[] colors = new int[PropertyBar.PB_COLORS_PENCIL.length];
        System.arraycopy(PropertyBar.PB_COLORS_PENCIL, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_PENCIL[0];
        propertyBar.setColors(colors);
        propertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, mOpacity);
        super.setPropertyBarProperties(propertyBar);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        addAnnot(null);
    }

    private void addAnnot(final Event.Callback result) {
        if (mCapturedPage == -1 || mLineList.size() == 0) return;
        RectF bbox = new RectF();
        ArrayList<ArrayList<PointF>> docLines = mUtil.docLinesFromPageView(mPdfViewCtrl, mCapturedPage, mLineList, bbox);
        bbox.inset(-mThickness, -mThickness);
        mAnnotHandler.addAnnot(mCapturedPage, new RectF(bbox),
                mColor,
                AppDmUtil.opacity100To255(mOpacity),
                mThickness,
                docLines,
                result);

        mCapturedPage = -1;
        mLineList.clear();
        mPathList.clear();
    }
}
