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
package com.foxit.uiextensions.annots.polygon;

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
import android.widget.Toast;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.fxcrt.PointFArray;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.BorderInfo;
import com.foxit.sdk.pdf.annots.Polygon;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
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
import com.foxit.uiextensions.utils.UIToast;

import java.util.ArrayList;

public class PolygonToolHandler implements ToolHandler {

    private Context mContext;
    private int mColor;
    private int mOpacity;
    private float mThickness;
    private int mControlPtEx = 5;// Refresh the scope expansion width
    private float mCtlPtLineWidth = 2;
    private float mCtlPtRadius = 5;

    private boolean mTouchCaptured = false;
    private int mLastPageIndex = -1;

    private Paint mPaint;
    private Paint mPathPaint;

    /**
     * toolbar
     */
    private PropertyBar mPropertyBar;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;
    private UIExtensionsManager mUiExtensionsManager;
    private PDFViewCtrl mPdfViewCtrl;

    private PropertyCircleItem mPropertyItem;
    private IBaseItem mOKItem;
    
    private ArrayList<PointF> mVertexList = new ArrayList<PointF>();
    private ArrayList<PointF> mPdfVertexList = new ArrayList<PointF>();
    
    public PolygonToolHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        mPropertyBar = mUiExtensionsManager.getMainFrame().getPropertyBar();
        
        mContext = context;
        mControlPtEx = AppDisplay.getInstance(context).dp2px(mControlPtEx);

        mPaint = new Paint();
        mPaint.setStyle(Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);

        mPathPaint = new Paint();
        mPathPaint.setStyle(Style.STROKE);
        mPathPaint.setAntiAlias(true);
        mPathPaint.setDither(true);
    }

    /**
     * init toolbar
     */
    protected void init() {
        mColor = mUiExtensionsManager.getConfig().uiSettings.annotations.polygon.color;
        mOpacity =(int) (mUiExtensionsManager.getConfig().uiSettings.annotations.polygon.opacity * 100);
        mThickness = mUiExtensionsManager.getConfig().uiSettings.annotations.polygon.thickness;

        mUiExtensionsManager.getMainFrame().getMoreToolsBar().registerListener(new MoreTools.IMT_MoreClickListener() {
            @Override
            public void onMTClick(int type) {
                mUiExtensionsManager.setCurrentToolHandler(PolygonToolHandler.this);
                mUiExtensionsManager.changeState(ReadStateConfig.STATE_ANNOTTOOL);
            }

            @Override
            public int getType() {
                return MoreTools.MT_TYPE_POLYGON;
            }
        });
    }

    protected void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    protected void removePropertyListener() {
        mPropertyChangeListener = null;
    }

    private void setPaint(int pageIndex) {
        mPaint.setColor(mColor);
        mPaint.setAlpha(AppDmUtil.opacity100To255(mOpacity));
        mPaint.setAntiAlias(true);
        PointF tranPt = new PointF(thicknessOnPageView(pageIndex, mThickness), thicknessOnPageView(pageIndex, mThickness));
        mPaint.setStrokeWidth(tranPt.x);
    }

    private RectF mPageViewThickness = new RectF(0, 0, 0, 0);

    private float thicknessOnPageView(int pageIndex, float thickness) {
        mPageViewThickness.set(0, 0, thickness, thickness);
        mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewThickness, mPageViewThickness, pageIndex);
        return Math.abs(mPageViewThickness.width());
    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_POLYGON;
    }

    @Override
    public void onActivate() {
        mLastPageIndex = -1;
        mCtlPtRadius = 5;
        mCtlPtRadius = AppDisplay.getInstance(mContext).dp2px(mCtlPtRadius);

        resetPropertyBar();
        resetAnnotBar();
    }

    private void resetPropertyBar() {
        int[] colors = new int[PropertyBar.PB_COLORS_POLYGON.length];
        System.arraycopy(PropertyBar.PB_COLORS_POLYGON, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_POLYGON[0];
        mPropertyBar.setColors(colors);

        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, mColor);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, mOpacity);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, mThickness);
        mPropertyBar.setArrowVisible(true);
        mPropertyBar.reset(getSupportedProperties());
        mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
    }

    private long getSupportedProperties() {
        return PropertyBar.PROPERTY_COLOR
                | PropertyBar.PROPERTY_OPACITY
                | PropertyBar.PROPERTY_LINEWIDTH;
    }

    private void resetAnnotBar() {
        mUiExtensionsManager.getMainFrame().getToolSetBar().removeAllItems();
        mPropertyItem = new PropertyCircleItemImp(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (PolygonToolHandler.this == mUiExtensionsManager.getCurrentToolHandler()) {
                    if (mPropertyBar.isShowing()) {
                        Rect mProRect = new Rect();
                        mPropertyItem.getContentView().getGlobalVisibleRect(mProRect);
                        mPropertyBar.update(new RectF(mProRect));
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
                mPropertyBar.setArrowVisible(true);
                mPropertyItem.getContentView().getGlobalVisibleRect(rect);
                mPropertyBar.show(new RectF(rect), true);
            }
        });

        mOKItem = new BaseItemImpl(mContext);
        mOKItem.setTag(ToolbarItemConfig.ITEM_ANNOT_BAR_OK);
        mOKItem.setImageResource(R.drawable.rd_annot_create_ok_selector);
        mOKItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUiExtensionsManager.setCurrentToolHandler(null);
                mUiExtensionsManager.changeState(ReadStateConfig.STATE_EDIT);
            }
        });

        mUiExtensionsManager.getMainFrame().getToolSetBar().addView(mPropertyItem, BaseBar.TB_Position.Position_CENTER);
        mUiExtensionsManager.getMainFrame().getToolSetBar().addView(mOKItem, BaseBar.TB_Position.Position_CENTER);
    }

    @Override
    public void onDeactivate() {
        int size = mVertexList.size();
        if (size < 3) {
            if (size != 0) {
                UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.add_polygon_failed_hints), Toast.LENGTH_LONG);
            }
            mTouchCaptured = false;
            mLastPageIndex = -1;
            if (mVertexList.size() > 0) {
                RectF rect = new RectF(mVertexList.get(0).x, mVertexList.get(0).y, mVertexList.get(0).x, mVertexList.get(0).y);
                for (int i = 1; i < mVertexList.size(); i ++) {
                    rect.union(mVertexList.get(i).x, mVertexList.get(i).y);
                }
                rect.inset(-mCtlPtRadius, -mCtlPtRadius);
                mPdfViewCtrl.invalidate();
                mVertexList.clear();
            }
            mPdfVertexList.clear();
            return;
        }
        createAnnot();
    }

    private void drawControls(Canvas canvas, ArrayList<PointF> vertexList, int color, int opacity) {
        if (vertexList.size() == 0) return;
        for (int i = 0; i < vertexList.size(); i ++) {
            PointF p = vertexList.get(i);
            mPaint.setColor(Color.WHITE);
            mPaint.setAlpha(255);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(p.x, p.y, mCtlPtRadius, mPaint);
            mPaint.setColor(color);
            mPaint.setAlpha(opacity);
            mPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(p.x, p.y, mCtlPtRadius, mPaint);
        }
    }

    private void drawPath(Canvas canvas, int pageIndex, ArrayList<PointF> vertexList) {
        int size = vertexList.size();
        if (size == 0 || size == 1) return;
        mPathPaint.setColor(mColor);
        mPathPaint.setAlpha(AppDmUtil.opacity100To255(mOpacity));
        mPathPaint.setStrokeWidth(thicknessOnPageView(pageIndex, mThickness));
        for (int i = 0; i < size; i ++) {
            if (i == size - 1) return;
            PointF p1 = vertexList.get(i);
            PointF p2 = vertexList.get(i + 1);

            Path path = new Path();
            path.moveTo(p1.x, p1.y);
            path.lineTo(p2.x, p2.y);
            canvas.drawPath(path, mPathPaint);
        }
    }


    private RectF mInvalidateRect = new RectF(0, 0, 0, 0);

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
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
                    mVertexList.add(new PointF(x, y));
                    PointF temp = new PointF();
                    mPdfViewCtrl.convertPageViewPtToPdfPt(pvPoint, temp, pageIndex);
                    mPdfVertexList.add(temp);
                    if (mLastPageIndex == -1) {
                        mLastPageIndex = pageIndex;
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mTouchCaptured || mLastPageIndex != pageIndex || mVertexList.size() == 0)
                    break;
                float deltaXY = mCtlPtLineWidth + mCtlPtRadius * 2 + 2;
                if (mVertexList.size() == 1) {
                    mInvalidateRect.set(mVertexList.get(0).x, mVertexList.get(0).y, mVertexList.get(0).x, mVertexList.get(0).y);
                } else {
                    int size = mVertexList.size();
                    mInvalidateRect.union(mVertexList.get(size - 1).x, mVertexList.get(size - 1).y);
                }

                mInvalidateRect.inset(-deltaXY, -deltaXY);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));
                return true;
            default:
                return true;
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
    public boolean isContinueAddAnnot() {
        return true;
    }

    @Override
    public void setContinueAddAnnot(boolean continueAddAnnot) {
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (mPdfVertexList.size() == 0) {
            return;
        }
        if (mLastPageIndex == pageIndex) {
            canvas.save();
            ArrayList<PointF> vertexList = new ArrayList<PointF>();
            for (int i = 0; i < mPdfVertexList.size(); i ++) {
                PointF pointF = new PointF();
                pointF.set(mPdfVertexList.get(i));
                mPdfViewCtrl.convertPdfPtToPageViewPt(pointF, pointF, pageIndex);
                vertexList.add(pointF);
            }
            drawPath(canvas, pageIndex, vertexList);

            setPaint(pageIndex);
            drawControls(canvas, vertexList, mColor, AppDmUtil.opacity100To255(mOpacity));
            canvas.restore();
        }
    }

    private void createAnnot() {
        if (mLastPageIndex == -1) return;
        if (mPdfVertexList.size() == 0) return;
        if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {
            try {
                final PDFPage page = mPdfViewCtrl.getDoc().getPage(mLastPageIndex);
                final Polygon newAnnot = (Polygon) AppAnnotUtil.createAnnot(page.addAnnot(Annot.e_Polygon, new com.foxit.sdk.common.fxcrt.RectF(0, 0, 0 ,0)), Annot.e_Polygon);

                final PolygonAddUndoItem undoItem = new PolygonAddUndoItem(mPdfViewCtrl);
                undoItem.mPageIndex = mLastPageIndex;
                undoItem.mColor = mColor;
                undoItem.mNM = AppDmUtil.randomUUID(null);
                undoItem.mOpacity = AppDmUtil.opacity100To255(mOpacity) / 255f;
                undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
                undoItem.mBorderStyle = BorderInfo.e_Solid;
                undoItem.mLineWidth = mThickness;
                undoItem.mFlags = 4;
                undoItem.mSubject = "Polygon Dimension";
                undoItem.mIntent = "PolygonDimension";
                undoItem.mCreationDate = AppDmUtil.currentDateToDocumentDate();
                undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
                undoItem.mVertexes = new PointFArray();
                for (int i = 0; i < mPdfVertexList.size(); i ++) {
                    undoItem.mVertexes.add(new com.foxit.sdk.common.fxcrt.PointF(mPdfVertexList.get(i).x, mPdfVertexList.get(i).y));
                }

                PolygonEvent event = new PolygonEvent(EditAnnotEvent.EVENTTYPE_ADD, undoItem, newAnnot, mPdfViewCtrl);
                EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            mUiExtensionsManager.getDocumentManager().onAnnotAdded(page, newAnnot);
                            mUiExtensionsManager.getDocumentManager().addUndoItem(undoItem);
                            if (mPdfViewCtrl.isPageVisible(mLastPageIndex)) {

                                try {
                                    RectF viewRect = AppUtil.toRectF(newAnnot.getRect());
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, mLastPageIndex);
                                    Rect rect = new Rect();
                                    viewRect.roundOut(rect);
                                    rect.inset(-10, -10);
                                    mPdfViewCtrl.refresh(mLastPageIndex, rect);
                                } catch (PDFException e) {
                                    e.printStackTrace();
                                }

                                mTouchCaptured = false;
                                mLastPageIndex = -1;
                            }
                        }
                    }
                });
                mPdfViewCtrl.addTask(task);
                mVertexList.clear();
                mPdfVertexList.clear();
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    protected void changeCurrentColor(int currentColor) {
        mColor = currentColor;
        setProItemColor(currentColor);
    }

    protected void changeCurrentOpacity(int currentOpacity) {
        mOpacity = currentOpacity;
    }

    protected void changeCurrentThickness(float currentThickness) {
        mThickness = currentThickness;
    }

    private void setProItemColor(int color){
        if (mPropertyItem == null) return;
        mPropertyItem.setCentreCircleColor(color);
    }

}
