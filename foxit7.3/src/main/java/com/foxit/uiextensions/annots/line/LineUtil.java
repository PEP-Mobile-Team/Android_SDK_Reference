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
package com.foxit.uiextensions.annots.line;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;

import java.util.ArrayList;

class LineUtil {
    protected static float ARROW_WIDTH_SCALE = 6.0f;
    protected float CTL_EXTENT = 5.0f;
    private float mCtlLineWidth = 2;
    private float mCtlRadius = 5;
    private float mCtlTouchExt = 20;
    private Paint mCtlPaint = new Paint();

    protected LineModule mModule;
    protected PointF tv_left_pt = new PointF();
    protected PointF tv_right_pt = new PointF();

    protected PointF tv_left_start_pt = new PointF();
    protected PointF tv_right_start_pt = new PointF();

    LineUtil(Context context, LineModule module) {
        mModule = module;
        float d2pFactor = AppDisplay.getInstance(context).dp2px(1.0f);
        CTL_EXTENT *= d2pFactor;
        mCtlLineWidth *= d2pFactor;
        mCtlRadius *= d2pFactor;
        mCtlTouchExt *= d2pFactor;
        mCtlPaint.setStrokeWidth(mCtlLineWidth);
    }

    LineToolHandler getToolHandler(String intent) {
        if (intent != null && intent.equals(LineConstants.INTENT_LINE_ARROW)) {
            return mModule.mArrowToolHandler;
        } else if (intent != null && intent.equals(LineConstants.INTENT_LINE_DIMENSION)) {
            return mModule.mDistanceToolHandler;
        } else {
            return mModule.mLineToolHandler;
        }
    }

    String getToolName(String intent) {
        if (intent != null && intent.equals(LineConstants.INTENT_LINE_ARROW)) {
            return ToolHandler.TH_TYPE_ARROW;
        } else if (intent!= null && LineConstants.INTENT_LINE_DIMENSION.equals(intent)){
            return ToolHandler.TH_TYPE_DISTANCE;
        } else {
            return ToolHandler.TH_TYPE_LINE;
        }
    }

    String getToolPropertyKey(String intent) {
        if (intent != null && intent.equals(LineConstants.INTENT_LINE_ARROW)) {
            return LineConstants.PROPERTY_KEY_ARROW;
        } else if(intent!=null && LineConstants.INTENT_LINE_DIMENSION.equals(intent)){
            return LineConstants.PROPERTY_KEY_DISTANCE;
        } else {
            return LineConstants.PROPERTY_KEY_LINE;
        }
    }

    String getSubject(String intent) {
        if (intent != null && intent.equals(LineConstants.INTENT_LINE_ARROW)) {
            return LineConstants.SUBJECT_ARROW;
        } else if (intent != null && intent.equals(LineConstants.INTENT_LINE_DIMENSION)){
            return LineConstants.SUBJECT_DISTANCE;
        }else {
            return LineConstants.SUBJECT_LINE;
        }
    }

    ArrayList<Integer> getEndingStyles(String intent) {
        if (intent != null && intent.equals(LineConstants.INTENT_LINE_ARROW)) {
            ArrayList<Integer> endingStyles = new ArrayList<Integer>();
            endingStyles.add(Markup.e_EndingStyleNone);
            endingStyles.add(Markup.e_EndingStyleOpenArrow);
            return endingStyles;
        } else if (intent != null && intent.equals(LineConstants.INTENT_LINE_DIMENSION)) {
            ArrayList<Integer> endingStyles = new ArrayList<Integer>();
            endingStyles.add(Markup.e_EndingStyleOpenArrow);
            endingStyles.add(Markup.e_EndingStyleOpenArrow);
            return endingStyles;
        }
        return null;
    }

    public long getSupportedProperties() {
        return PropertyBar.PROPERTY_COLOR
                | PropertyBar.PROPERTY_OPACITY
                | PropertyBar.PROPERTY_LINEWIDTH;
    }

    public long getDistanceSupportedProperties() {
        return PropertyBar.PROPERTY_COLOR
                | PropertyBar.PROPERTY_OPACITY
                | PropertyBar.PROPERTY_LINEWIDTH
                | PropertyBar.PROPERTY_DISTANCE;
    }

    protected Path getLinePath(String intent, PointF start, PointF stop, float thickness) {
        if (intent == null)
            return getLinePath(start, stop);
        if (intent.equals(LineConstants.INTENT_LINE_ARROW))
            return getArrowPath(start, stop, thickness);
        if (intent.equals(LineConstants.INTENT_LINE_DIMENSION))
            return getDistancePath(start, stop, thickness);
        return getLinePath(start, stop);
    }

    protected Path getLinePath(PointF start, PointF stop) {
        Path path = new Path();
        path.moveTo(start.x, start.y);
        path.lineTo(stop.x, stop.y);
        return path;
    }

    protected Path getArrowPath(PointF start, PointF stop, float thickness) {
        getArrowControlPt(start, stop, thickness, tv_left_pt, tv_right_pt);
        Path path = new Path();
        path.moveTo(start.x, start.y);
        path.lineTo(stop.x, stop.y);
        path.moveTo(tv_left_pt.x, tv_left_pt.y);
        path.lineTo(stop.x, stop.y);
        path.lineTo(tv_right_pt.x, tv_right_pt.y);
        return path;
    }

    protected RectF getArrowBBox(PointF startPt, PointF stopPt, float thickness) {
        RectF bboxRect = new RectF();
        getArrowControlPt(startPt, stopPt, thickness, tv_left_pt, tv_right_pt);
        bboxRect.left = Math.min(Math.min(startPt.x, stopPt.x), Math.min(tv_left_pt.x, tv_right_pt.x));
        bboxRect.top = Math.min(Math.min(startPt.y, stopPt.y), Math.min(tv_left_pt.y, tv_right_pt.y));
        bboxRect.right = Math.max(Math.max(startPt.x, stopPt.x), Math.max(tv_left_pt.x, tv_right_pt.x));
        bboxRect.bottom = Math.max(Math.max(startPt.y, stopPt.y), Math.max(tv_left_pt.y, tv_right_pt.y));
        bboxRect.inset(-thickness, -thickness);
        return bboxRect;
    }

    protected void getArrowControlPt(PointF start, PointF stop, float thickness, PointF left, PointF right) {
        PointF direction = new PointF(stop.x - start.x, stop.y - start.y);
        double dLenth = Math.sqrt(direction.x * direction.x + direction.y * direction.y);
        if (dLenth < 0.0001f) {
            direction.x = 1.0f;
            direction.y = 0.0f;
        } else {
            direction.x = (float) (direction.x / dLenth);
            direction.y = (float) (direction.y / dLenth);
        }

        direction.x *= -thickness;
        direction.y *= -thickness;
        PointF rotatedVector = Rotate(direction, Math.PI / 6.0f);
        left.x = stop.x + rotatedVector.x;
        left.y = stop.y + rotatedVector.y;
        rotatedVector = Rotate(direction, -Math.PI / 6.0f);
        right.x = stop.x + rotatedVector.x;
        right.y = stop.y + rotatedVector.y;
    }

    protected Path getDistancePath(PointF start, PointF stop, float thickness) {
        getDistanceControlPt(start, stop, thickness, tv_left_pt, tv_right_pt,tv_left_start_pt,tv_right_start_pt);
        Path path = new Path();
        path.moveTo(start.x, start.y);
        path.lineTo(stop.x, stop.y);

        path.moveTo(tv_left_pt.x, tv_left_pt.y);
        path.lineTo(stop.x, stop.y);
        path.lineTo(tv_right_pt.x, tv_right_pt.y);

        path.moveTo(tv_left_start_pt.x, tv_left_start_pt.y);
        path.lineTo(start.x, start.y);
        path.lineTo(tv_right_start_pt.x, tv_right_start_pt.y);
        return path;
    }

    protected void getDistanceControlPt(PointF start, PointF stop, float thickness, PointF left, PointF right,PointF start_left, PointF start_right) {
        PointF direction = new PointF(stop.x - start.x, stop.y - start.y);
        double dLenth = Math.sqrt(direction.x * direction.x + direction.y * direction.y);
        if (dLenth < 0.0001f) {
            direction.x = 1.0f;
            direction.y = 0.0f;
        } else {
            direction.x = (float) (direction.x / dLenth);
            direction.y = (float) (direction.y / dLenth);
        }

        direction.x *= -thickness;
        direction.y *= -thickness;
        PointF rotatedVector = Rotate(direction, Math.PI / 6.0f);
        left.x = stop.x + rotatedVector.x;
        left.y = stop.y + rotatedVector.y;
        start_left.x = start.x-rotatedVector.x;
        start_left.y = start.y-rotatedVector.y;
        rotatedVector = Rotate(direction, -Math.PI / 6.0f);
        right.x = stop.x + rotatedVector.x;
        right.y = stop.y + rotatedVector.y;
        start_right.x = start.x - rotatedVector.x;
        start_right.y = start.y - rotatedVector.y;
    }


    PointF Rotate(PointF direction, double angle) {
        PointF pointF = new PointF();
        double cosValue = Math.cos(angle);
        double sinValue = Math.sin(angle);
        pointF.x = (float) (direction.x * cosValue - direction.y * sinValue);
        pointF.y = (float) (direction.x * sinValue + direction.y * cosValue);
        return pointF;
    }


    public PointF calculateEndingPoint(PointF p1, PointF p3) {
        // p0--p1--------p2--p3
        PointF p2 = new PointF();
        float l = (float) Math.sqrt((p3.x - p1.x) * (p3.x - p1.x) + (p3.y - p1.y) * (p3.y - p1.y));
        p2.x = p3.x - CTL_EXTENT / l * (p3.x - p1.x);
        p2.y = p3.y - CTL_EXTENT / l * (p3.y - p3.y);
        return p2;
    }

    public float[] calculateControls(PointF p1, PointF p2) {
        // p0--p1--------p2--p3
        PointF p0 = new PointF();
        PointF p3 = new PointF();
        float l = (float) Math.sqrt((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y));
        p0.x = p1.x + CTL_EXTENT / l * (p1.x - p2.x);
        p0.y = p1.y + CTL_EXTENT / l * (p1.y - p2.y);
        p3.x = p2.x + CTL_EXTENT / l * (p2.x - p1.x);
        p3.y = p2.y + CTL_EXTENT / l * (p2.y - p1.y);
        float[] ctlPts = new float[]{
                p0.x, p0.y,
                p3.x, p3.y,
        };
        return ctlPts;
    }

    public int hitControlTest(PointF p1, PointF p2, PointF point) {
        float[] ctlPts = calculateControls(p1, p2);
        RectF area = new RectF();
        for (int i = 0; i < ctlPts.length / 2; i++) {
            area.set(ctlPts[i * 2], ctlPts[i * 2 + 1], ctlPts[i * 2], ctlPts[i * 2 + 1]);
            area.inset(-mCtlTouchExt, -mCtlTouchExt);
            if (area.contains(point.x, point.y)) {
                return i;
            }
        }
        return -1;
    }

    public void drawControls(Canvas canvas, PointF p1, PointF p2, int color, int opacity) {
        float[] ctlPts = calculateControls(p1, p2);
        for (int i = 0; i < ctlPts.length; i += 2) {
            mCtlPaint.setColor(Color.WHITE);
            mCtlPaint.setAlpha(255);
            mCtlPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(ctlPts[i], ctlPts[i + 1], mCtlRadius, mCtlPaint);
            mCtlPaint.setColor(color);
            mCtlPaint.setAlpha(opacity);
            mCtlPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(ctlPts[i], ctlPts[i + 1], mCtlRadius, mCtlPaint);
        }
    }

    private float getControlExtent() {
        return mCtlLineWidth + mCtlRadius;
    }

    public void extentBoundsToContainControl(RectF bounds) {
        PointF p1 = new PointF(bounds.left, bounds.top);
        PointF p2 = new PointF(bounds.right, bounds.bottom);
        float[] ctlPts = calculateControls(p1, p2);
        bounds.union(ctlPts[0], ctlPts[1], ctlPts[2], ctlPts[3]);
        bounds.inset(-getControlExtent(), -getControlExtent());
    }

    public void correctPvPoint(PDFViewCtrl pdfViewCtrl, int pageIndex, PointF pt, float thickness) {
        float extent = getControlExtent() + AppAnnotUtil.getAnnotBBoxSpace() + thickness / 2;
        pt.x = Math.max(extent, pt.x);
        pt.y = Math.max(extent, pt.y);
        pt.x = Math.min(pdfViewCtrl.getPageViewWidth(pageIndex) - extent, pt.x);
        pt.y = Math.min(pdfViewCtrl.getPageViewHeight(pageIndex) - extent, pt.y);
    }

}
