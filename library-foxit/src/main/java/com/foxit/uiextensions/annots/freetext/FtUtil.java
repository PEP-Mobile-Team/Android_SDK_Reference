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
package com.foxit.uiextensions.annots.freetext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.fxcrt.PointFArray;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FreeText;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;


public class FtUtil {
    public static final float 			PI = 3.14159265358979323846f;
    public static final float 			RADIUS = 5;
    public static final int DEFAULT_BORDER_WIDTH = 1;
    public static final int DEFAULTTEXTWIDTH = 100;
    public static final int DEFAULT_KENNTOEND_WIDTH = 30;
    public static final int DEFAULT_STARTTOKNEE_WIDTH = 50;

    public static final float CTRLPTTOUCHEXT = 5;

    public static final int CTR_NONE = -1;
    public static final int CTR_LEFT_TOP = 0;
    public static final int CTR_MID_TOP = 1;
    public static final int CTR_RIGHT_TOP = 2;
    public static final int CTR_RIGHT_MID = 3;
    public static final int CTR_RIGHT_BOTTOM = 4;
    public static final int CTR_MID_BOTTOM = 5;
    public static final int CTR_LEFT_BOTTOM = 6;
    public static final int CTR_LEFT_MID = 7;
    public static final int CTR_STARTING = 8;
    public static final int CTR_KNEE = 9;
    public static final int CTR_ENDING = 10;
    public static final int CTR_BORDER = 11;
    public static final int CTR_TEXTBBOX = 12;

    public static final int OPER_DEFAULT = -1;
    public static final int OPER_TRANSLATE = 0;
    public static final int OPER_SCALE = 1;
    public static final int OPER_STARTING = 2;
    public static final int OPER_KNEE = 3;
    public static final int OPER_BORDER = 4;

    public static final int 			RIGHTTOTOP = 1;
    public static final int 			RIGHTTOBOTTOM = 2;
    public static final int 			LEFTTOTOP = 3;
    public static final int 			LEFTTOBOTTOM = 4;
    public static final int 			MIDTOP = 5;
    public static final int 			MIDBOTTOM = 6;

    public static final int 			BORDERUNKNOW = 0;
    public static final int 			BORDERSOLID = 1;
    public static final int 			BORDERDASH1 = 2;
    public static final int 			BORDERDASH2 = 3;
    public static final int 			BORDERDASH3 = 4;
    public static final int 			BORDERDASH4 = 5;
    public static final int 			BORDERDASH5 = 6;
    public static final int 			BORDERCLOUD = 7;
    public static final int 			BORDERCOUNT = 7;

    public static final PointF 			leftPt = new PointF();
    public static final PointF 			rightPt = new PointF();

    public static float widthOnPageView(PDFViewCtrl pdfViewCtrl, int pageIndex, float width) {
        RectF rectF = new RectF(0, 0, width, width);
        pdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
        return Math.abs(rectF.width());
    }

    public static int getTouchControlPoint(PDFViewCtrl pdfViewCtrl, int pageIndex, Annot annot, float x, float y) {
        try {
            RectF textBBox;
            String intent = ((FreeText) annot).getIntent();
            boolean isCallout = (intent != null && intent.equalsIgnoreCase("FreeTextCallout"));
            if (isCallout) {
                textBBox = AppUtil.toRectF(((FreeText) annot).getInnerRect());
            } else {
                textBBox = AppUtil.toRectF(annot.getRect());
            }
            pdfViewCtrl.convertPdfRectToPageViewRect(textBBox, textBBox, pageIndex);
            RectF frmRect = new RectF(textBBox);
            float[] ctlPts = FtUtil.calculateTextControlPoints(frmRect);
            RectF area = new RectF();
            for (int i = 0; i < ctlPts.length / 2; i++) {
                area.set(ctlPts[i * 2], ctlPts[i * 2 + 1], ctlPts[i * 2], ctlPts[i * 2 + 1]);
                area.inset(-widthOnPageView(pdfViewCtrl, pageIndex, CTRLPTTOUCHEXT * 2),
                        -widthOnPageView(pdfViewCtrl, pageIndex, CTRLPTTOUCHEXT * 2));
                if (area.contains(x, y)) {
                    return i;
                }
            }

            if (isCallout) {
                ArrayList<PointF> points = getCalloutLinePoints(((FreeText) annot));
                PointF startingPt = points.get(0);
                PointF kneePt = points.get(1);
                PointF endingPt = points.get(2);

                pdfViewCtrl.convertPdfPtToPageViewPt(startingPt, startingPt, pageIndex);
                pdfViewCtrl.convertPdfPtToPageViewPt(kneePt, kneePt, pageIndex);
                pdfViewCtrl.convertPdfPtToPageViewPt(endingPt, endingPt, pageIndex);

                float space = widthOnPageView(pdfViewCtrl, pageIndex, CTRLPTTOUCHEXT * 2);
                RectF startingRect = new RectF(startingPt.x, startingPt.y, startingPt.x, startingPt.y);
                RectF kneeRect = new RectF(kneePt.x, kneePt.y, kneePt.x, kneePt.y);

                startingRect.inset(-space, -space);
                kneeRect.inset(-space, -space);

                if (startingRect.contains(x, y)) {
                    return CTR_STARTING;
                }
                if (kneeRect.contains(x, y)) {
                    return CTR_KNEE;
                }
                RectF borderRect = getBorderRectByStartKneeAndEnding(startingPt.x, startingPt.y, kneePt.x, kneePt.y,
                        endingPt.x, endingPt.y);
                if (borderRect.contains(x, y)
                        && (isIntersectPointInLine(x, y, startingPt.x, startingPt.y, kneePt.x, kneePt.y) ||
                        isIntersectPointInLine(x, y, kneePt.x, kneePt.y, endingPt.x, endingPt.y))) {
                    return CTR_BORDER;
                }
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
        return FtUtil.CTR_NONE;
    }

    public static float[] calculateTextControlPoints(RectF rectF) {
        float l = rectF.left;
        float t = rectF.top;
        float r = rectF.right;
        float b = rectF.bottom;

        float[] ctlPts = new float[]{l, t, (l + r) / 2, t, r, t, r, (t + b) / 2, r, b, (l + r) / 2, b, l, b, l,
                (t + b) / 2,};
        return ctlPts;
    }

    public static Matrix calculateScaleMatrix(int ctl, RectF rectF, float dx, float dy) {
        Matrix matrix = new Matrix();
        if (ctl > 7)
            return matrix;
        float[] ctlPts = calculateTextControlPoints(rectF);
        float px = ctlPts[ctl * 2];
        float py = ctlPts[ctl * 2 + 1];
        float oppositeX = 0;
        float oppositeY = 0;
        if (ctl < 4 && ctl >= 0) {
            oppositeX = ctlPts[ctl * 2 + 8];
            oppositeY = ctlPts[ctl * 2 + 9];
        } else if (ctl >= 4) {
            oppositeX = ctlPts[ctl * 2 - 8];
            oppositeY = ctlPts[ctl * 2 - 7];
        }

        float scaleh = (px + dx - oppositeX) / (px - oppositeX);
        float scalev = (py + dy - oppositeY) / (py - oppositeY);

        switch (ctl) {
            case CTR_LEFT_TOP:
            case CTR_RIGHT_TOP:
            case CTR_RIGHT_BOTTOM:
            case CTR_LEFT_BOTTOM:
                matrix.postScale(scaleh, scalev, oppositeX, oppositeY);
                break;
            case CTR_RIGHT_MID:
            case CTR_LEFT_MID:
                matrix.postScale(scaleh, 1.0f, oppositeX, oppositeY);
                break;
            case CTR_MID_TOP:
            case CTR_MID_BOTTOM:
                matrix.postScale(1.0f, scalev, oppositeX, oppositeY);
                break;
            case CTR_NONE:
            default:
                break;
        }
        return matrix;
    }

    public static PointF adjustScalePointF(int ctr, PDFViewCtrl pdfViewCtrl, int pageIndex, RectF rectF, float dxy) {
        float adjustx = 0;
        float adjusty = 0;
        switch (ctr) {
            case FtUtil.CTR_LEFT_TOP:
                if (rectF.left < dxy) {
                    adjustx = -rectF.left + dxy;
                    rectF.left = dxy;
                }
                if (rectF.top < dxy) {
                    adjusty = -rectF.top + dxy;
                    rectF.top = dxy;
                }
                break;
            case FtUtil.CTR_MID_TOP:
                if (rectF.top < dxy) {
                    adjusty = -rectF.top + dxy;
                    rectF.top = dxy;
                }
                break;
            case FtUtil.CTR_RIGHT_TOP:
                if (rectF.top < dxy) {
                    adjusty = -rectF.top + dxy;
                    rectF.top = dxy;
                }
                if (rectF.right > pdfViewCtrl.getPageViewWidth(pageIndex) - dxy) {
                    adjustx = pdfViewCtrl.getPageViewWidth(pageIndex) - rectF.right - dxy;
                    rectF.right = pdfViewCtrl.getPageViewWidth(pageIndex) - dxy;
                }
                break;
            case FtUtil.CTR_RIGHT_MID:
                if (rectF.right > pdfViewCtrl.getPageViewWidth(pageIndex) - dxy) {
                    adjustx = pdfViewCtrl.getPageViewWidth(pageIndex) - rectF.right - dxy;
                    rectF.right = pdfViewCtrl.getPageViewWidth(pageIndex) - dxy;
                }
                break;
            case FtUtil.CTR_RIGHT_BOTTOM:
                if (rectF.right > pdfViewCtrl.getPageViewWidth(pageIndex) - dxy) {
                    adjustx = pdfViewCtrl.getPageViewWidth(pageIndex) - rectF.right - dxy;
                    rectF.right = pdfViewCtrl.getPageViewWidth(pageIndex) - dxy;
                }
                if (rectF.bottom > pdfViewCtrl.getPageViewHeight(pageIndex) - dxy) {
                    adjusty = pdfViewCtrl.getPageViewHeight(pageIndex) - rectF.bottom - dxy;
                    rectF.bottom = pdfViewCtrl.getPageViewHeight(pageIndex) - dxy;
                }
                break;
            case FtUtil.CTR_MID_BOTTOM:
                if (rectF.bottom > pdfViewCtrl.getPageViewHeight(pageIndex) - dxy) {
                    adjusty = pdfViewCtrl.getPageViewHeight(pageIndex) - rectF.bottom - dxy;
                    rectF.bottom = pdfViewCtrl.getPageViewHeight(pageIndex) - dxy;
                }
                break;
            case FtUtil.CTR_LEFT_BOTTOM:
                if (rectF.left < dxy) {
                    adjustx = -rectF.left + dxy;
                    rectF.left = dxy;
                }
                if (rectF.bottom > pdfViewCtrl.getPageViewHeight(pageIndex) - dxy) {
                    adjusty = pdfViewCtrl.getPageViewHeight(pageIndex) - rectF.bottom - dxy;
                    rectF.bottom = pdfViewCtrl.getPageViewHeight(pageIndex) - dxy;
                }
                break;
            case FtUtil.CTR_LEFT_MID:
                if (rectF.left < dxy) {
                    adjustx = -rectF.left + dxy;
                    rectF.left = dxy;
                }
                break;
            default:
                break;
        }

        return new PointF(adjustx, adjusty);
    }

    public static int dip2px(Context context, int value) {
        return AppDisplay.getInstance(context).dp2px(value);
    }

    public static DashPathEffect getDashPathEffect(Context context, PDFViewCtrl pdfViewCtrl, int pageIndex, int value, boolean flag) {
        DashPathEffect dashPathEffect = null;
        if (flag) {
            switch (value) {
                case BORDERSOLID:
                    break;
                case BORDERDASH1:
                    dashPathEffect = new DashPathEffect(new float[] { widthOnPageView(pdfViewCtrl, pageIndex, 2),
                            widthOnPageView(pdfViewCtrl, pageIndex, 2) }, 0);
                    break;
                case BORDERDASH2:
                    dashPathEffect = new DashPathEffect(new float[] { widthOnPageView(pdfViewCtrl, pageIndex, 4),
                            widthOnPageView(pdfViewCtrl, pageIndex, 4) }, 0);
                    break;
                case BORDERDASH3:
                    dashPathEffect = new DashPathEffect(new float[] { widthOnPageView(pdfViewCtrl, pageIndex, 4),
                            widthOnPageView(pdfViewCtrl, pageIndex, 3), widthOnPageView(pdfViewCtrl, pageIndex, 2),
                            widthOnPageView(pdfViewCtrl, pageIndex, 3) }, 0);
                    break;
                case BORDERDASH4:
                    dashPathEffect = new DashPathEffect(new float[] { widthOnPageView(pdfViewCtrl, pageIndex, 4),
                            widthOnPageView(pdfViewCtrl, pageIndex, 3), widthOnPageView(pdfViewCtrl, pageIndex, 16),
                            widthOnPageView(pdfViewCtrl, pageIndex, 3) }, 0);
                    break;
                case BORDERDASH5:
                    dashPathEffect = new DashPathEffect(new float[] { widthOnPageView(pdfViewCtrl, pageIndex, 8),
                            widthOnPageView(pdfViewCtrl, pageIndex, 4), widthOnPageView(pdfViewCtrl, pageIndex, 4),
                            widthOnPageView(pdfViewCtrl, pageIndex, 4) }, 0);
                    break;
                case BORDERCLOUD:
                    break;
            }
        } else {
            int size = 5;
            switch (value) {
                case BORDERSOLID:
                    //				dashPathEffect = new DashPathEffect(new float[] {0}, 0);
                    break;
                case FtUtil.BORDERDASH1:
                    dashPathEffect = new DashPathEffect(new float[] { dip2px(context, 2) * size, dip2px(context, 2) * size }, 0);
                    break;
                case BORDERDASH2:
                    dashPathEffect = new DashPathEffect(new float[] { dip2px(context, 4) * size, dip2px(context, 4) * size }, 0);
                    break;
                case BORDERDASH3:
                    dashPathEffect = new DashPathEffect(new float[] { dip2px(context, 4) * size, dip2px(context, 3), dip2px(context, 2) * size,
                            dip2px(context, 3) * size }, 0);
                    break;
                case BORDERDASH4:
                    dashPathEffect = new DashPathEffect(new float[] { dip2px(context, 4) * size, dip2px(context, 3) * size,
                            dip2px(context, 16) * size, dip2px(context, 3) * size }, 0);
                    break;
                case BORDERDASH5:
                    dashPathEffect = new DashPathEffect(new float[] { dip2px(context, 8) * size, dip2px(context, 4) * size, dip2px(context, 4) * size,
                            dip2px(context, 4) * size }, 0);
                    break;
                case BORDERCLOUD:
                    //				getCloudy_Rectangle(pageView, textRectF, fRotateAngle)
                    break;
            }
        }
        return dashPathEffect;
    }

    public static Path getCloudy_Rectangle(PDFViewCtrl pdfViewCtrl, int pageIndex, RectF textRectF, float fRotateAngle) {
        RectF textRect = new RectF(textRectF);
        pdfViewCtrl.convertPageViewRectToPdfRect(textRect, textRect, pageIndex);
        int iCloudyNum = (int) (Math.abs((textRect.width()) + Math.abs(textRect.height())) / 4.0f);
        Path path = new Path();
        RectF cloudyRect = new RectF();
        if (iCloudyNum < 2)
            return null;
        PointF pdfPoint1 = new PointF(0.0f, 0.0f);
        PointF pdfPoint2 = new PointF(0.0f, 0.0f);
        PointF pdfPoint3 = new PointF(0.0f, 0.0f);
        PointF v1 = new PointF(0.0f, 0.0f);
        PointF v2 = new PointF(0.0f, 0.0f);
        ArrayList<PointF> cPath = new ArrayList<PointF>();
        int i;
        float fTotalLen, fStep, xCenter, yCenter, x, y, angle, radius;
        pdfPoint1.x = textRect.left;
        pdfPoint1.y = textRect.bottom;
        pdfPoint2.x = textRect.right;
        pdfPoint2.y = textRect.bottom;
        pdfPoint3.x = textRect.right;
        pdfPoint3.y = textRect.top;
        v1.x = pdfPoint2.x - pdfPoint1.x;
        v1.y = pdfPoint2.y - pdfPoint1.y;

        v2.x = pdfPoint3.x - pdfPoint2.x;
        v2.y = pdfPoint3.y - pdfPoint2.y;
        fTotalLen = (float) (length(v1) + length(v2));
        xCenter = (textRect.right + textRect.left) / 2.0f;
        yCenter = (textRect.top + textRect.bottom) / 2.0f;
        radius = fTotalLen * 2.0f / (float) iCloudyNum;
        fStep = 0.0f;
        for (i = 0; i < iCloudyNum / 2; i++) {
            if (fStep <= length(v1)) {
                x = pdfPoint1.x + fStep - xCenter;
                y = pdfPoint1.y - yCenter;
            } else {
                x = pdfPoint2.x - xCenter;
                y = pdfPoint2.y + (fStep - (float) length(v1)) - yCenter;
            }
            pdfPoint3.x = xCenter + x * (float) Math.cos(fRotateAngle) - y * (float) Math.sin(fRotateAngle);
            pdfPoint3.y = yCenter + x * (float) Math.sin(fRotateAngle) + y * (float) Math.cos(fRotateAngle);
            cPath.add(new PointF(pdfPoint3.x, pdfPoint3.y));
            fStep += radius;
        }
        pdfPoint1.x = textRect.right;
        pdfPoint1.y = textRect.top;
        pdfPoint2.x = textRect.left;
        pdfPoint2.y = textRect.top;
        fStep = 0.0f;
        for (i = 0; i < iCloudyNum / 2; i++) {
            if (fStep <= length(v1)) {
                x = pdfPoint1.x - fStep - xCenter;
                y = pdfPoint1.y - yCenter;
            } else {
                x = pdfPoint2.x - xCenter;
                y = pdfPoint2.y - (fStep - (float) length(v1)) - yCenter;
            }
            pdfPoint3.x = xCenter + x * (float) Math.cos(fRotateAngle) - y * (float) Math.sin(fRotateAngle);
            pdfPoint3.y = yCenter + x * (float) Math.sin(fRotateAngle) + y * (float) Math.cos(fRotateAngle);
            cPath.add(new PointF(pdfPoint3.x, pdfPoint3.y));

            fStep += radius;
        }
        radius = 0.0f;
        pdfPoint1 = new PointF(cPath.get(0).x, cPath.get(0).y);
        iCloudyNum = cPath.size();
        for (i = 1; i <= iCloudyNum; i++) {
            pdfPoint2 = new PointF(cPath.get(i % iCloudyNum).x, cPath.get(i % iCloudyNum).y);
            v1.x = pdfPoint2.x - pdfPoint1.x;
            v1.y = pdfPoint2.y - pdfPoint1.y;
            if (radius < length(v1))
                radius = (float) length(v1);
            pdfPoint1 = new PointF(pdfPoint2.x, pdfPoint2.y);
        }
        radius = radius * 5.0f / 8.0f;
        RectF pdfRect = new RectF(textRect);
        float startAngle, stopAngle;
        boolean bHasM = true;
        for (i = 0; i < iCloudyNum; i++) {
            pdfPoint1 = new PointF(cPath.get(i).x, cPath.get(i).y);
            pdfPoint2 = new PointF(cPath.get((iCloudyNum - 1 + i) % iCloudyNum).x, cPath.get((iCloudyNum - 1 + i)
                    % iCloudyNum).y);
            pdfPoint3 = new PointF(cPath.get((i + 1) % iCloudyNum).x, cPath.get((i + 1) % iCloudyNum).y);
            v1.x = pdfPoint2.x - pdfPoint1.x;
            v1.y = pdfPoint2.y - pdfPoint1.y;
            v2.x = pdfPoint3.x - pdfPoint1.x;
            v2.y = pdfPoint3.y - pdfPoint1.y;

            startAngle = (float) slopeAngle(v1);
            if (v1.y < 0.0f)
                startAngle *= -1.0f;
            angle = (float) Math.acos(length(v1) / (2.0f * radius));
            startAngle += angle - PI / 9.0f;
            stopAngle = (float) slopeAngle(v2);
            if (v2.y < 0.0f)
                stopAngle *= -1.0f;
            angle = (float) Math.acos(length(v2) / (2.0f * radius));
            stopAngle -= angle;
            if (stopAngle < startAngle)
                stopAngle += PI * 2.0f;

            cloudyRect.left = pdfPoint1.x - radius;
            cloudyRect.bottom = pdfPoint1.y - radius;
            cloudyRect.right = pdfPoint1.x + radius;
            cloudyRect.top = pdfPoint1.y + radius;
            GetCloudyAP_Arc(pdfViewCtrl, pageIndex, path, cloudyRect, startAngle, stopAngle, bHasM);
            if (bHasM)
                bHasM = false;
            pdfRect.union(cloudyRect);

            v1.x = pdfPoint1.x - pdfPoint3.x;
            v1.y = pdfPoint1.y - pdfPoint3.y;
            startAngle = (float) slopeAngle(v1);
            if (v1.y < 0.0f)
                startAngle *= -1.0f;
            angle = (float) Math.acos(length(v1) / (2.0f * radius));
            startAngle += angle;
            stopAngle = startAngle - PI / 9.0f;
            cloudyRect.left = pdfPoint3.x - radius;
            cloudyRect.bottom = pdfPoint3.y - radius;
            cloudyRect.right = pdfPoint3.x + radius;
            cloudyRect.top = pdfPoint3.y + radius;
            GetCloudyAP_Arc(pdfViewCtrl, pageIndex, path, cloudyRect, startAngle, stopAngle, false);
        }
        return path;
    }

    public static void GetCloudyAP_Arc(PDFViewCtrl pdfViewCtrl, int pageIndex, Path path, RectF textRect, float fStartAngle,
                                       float fStopAngle, boolean bHasM) {
        RectF pdfRect = new RectF();
        if (Math.abs(fStopAngle - fStartAngle) <= 0.0001f)
            return;

        float fBezier = 0.5522847498308f;
        float x, y, a, b, a1, a2, b1, b2, c1, c2, x0, y0, x1, y1, x2, y2, x3, y3, xx, yy, px, py, angle1, angle2;
        x = (textRect.right + textRect.left) / 2.0f;
        y = (textRect.top + textRect.bottom) / 2.0f;
        a = (float) Math.abs(textRect.right - textRect.left) / 2.0f;
        b = (float) Math.abs(textRect.top - textRect.bottom) / 2.0f;

        angle1 = fStartAngle;
        x0 = a * (float) Math.cos(angle1);
        y0 = b * (float) Math.sin(angle1);
        px = x + x0;
        py = y + y0;
        pdfRect.left = pdfRect.right = px;
        pdfRect.bottom = pdfRect.top = py;
        if (bHasM) {
            PointF point_m = new PointF(px, py);
            pdfViewCtrl.convertPdfPtToPageViewPt(point_m, point_m, pageIndex);
            path.moveTo(point_m.x, point_m.y);
        }

        float iDirection = 1.0f;
        if (fStopAngle < fStartAngle)
            iDirection = -1.0f;
        float fMultiple = (float) Math.floor(fStartAngle * 2.0f / PI);
        if (iDirection > 0.0f)
            fMultiple += 1.0f;
        fMultiple *= PI / 2.0f;
        boolean bBreak = false;
        while (true) {
            angle2 = fMultiple;
            if (iDirection > 0.0f) {
                if (fStopAngle <= fMultiple) {
                    angle2 = fStopAngle;
                    bBreak = true;
                }
            } else {
                if (fStopAngle >= fMultiple) {
                    angle2 = fStopAngle;
                    bBreak = true;
                }
            }
            x0 = a * (float) Math.cos(angle1);
            y0 = b * (float) Math.sin(angle1);
            x3 = a * (float) Math.cos(angle2);
            y3 = b * (float) Math.sin(angle2);

            a1 = b * b * x0;
            b1 = a * a * y0;
            a2 = b * b * x3;
            b2 = a * a * y3;
            c1 = c2 = -a * a * b * b;
            xx = (b1 * c2 - b2 * c1) / (a1 * b2 - a2 * b1);
            yy = (a2 * c1 - a1 * c2) / (a1 * b2 - a2 * b1);

            px = xx - x0;
            py = yy - y0;
            x1 = x0 + px * fBezier;
            y1 = y0 + py * fBezier;
            px = xx - x3;
            py = yy - y3;
            x2 = x3 + px * fBezier;
            y2 = y3 + py * fBezier;

            px = x + x1;
            py = y + y1;
            x1 = px;
            y1 = py;
            if (pdfRect.left > x1)
                pdfRect.left = x1;
            if (pdfRect.bottom > y1)
                pdfRect.bottom = y1;
            if (pdfRect.right < x1)
                pdfRect.right = x1;
            if (pdfRect.top < y1)
                pdfRect.top = y1;
            px = x + x2;
            py = y + y2;
            x2 = px;
            y2 = py;
            if (pdfRect.left > x2)
                pdfRect.left = x2;
            if (pdfRect.bottom > y2)
                pdfRect.bottom = y2;
            if (pdfRect.right < x2)
                pdfRect.right = x2;
            if (pdfRect.top < y2)
                pdfRect.top = y2;
            px = x + x3;
            py = y + y3;
            x3 = px;
            y3 = py;
            if (pdfRect.left > x3)
                pdfRect.left = x3;
            if (pdfRect.bottom > y3)
                pdfRect.bottom = y3;
            if (pdfRect.right < x3)
                pdfRect.right = x3;
            if (pdfRect.top < y3)
                pdfRect.top = y3;
            PointF point_1 = new PointF(x1, y1);
            PointF point_2 = new PointF(x2, y2);
            PointF point_3 = new PointF(x3, y3);
            pdfViewCtrl.convertPdfPtToPageViewPt(point_1, point_1, pageIndex);
            pdfViewCtrl.convertPdfPtToPageViewPt(point_2, point_2, pageIndex);
            pdfViewCtrl.convertPdfPtToPageViewPt(point_3, point_3, pageIndex);
            path.cubicTo(point_1.x, point_1.y, point_2.x, point_2.y, point_3.x, point_3.y);
            if (bBreak)
                break;

            angle1 = angle2;
            fMultiple += iDirection * PI / 2.0f;
        }
    }

    public static double length(PointF point) {
        return Math.sqrt(point.x * point.x + point.y * point.y);
    }

    public static double slopeAngle(PointF point) {
        PointF point1 = new PointF(1.0f, 0.0f);
        return arcCosine(point, point1);
    }

    public static double arcCosine(PointF pointA, PointF pointB) {
        return Math.acos(cosine(pointA, pointB));
    }

    public static double cosine(PointF pointA, PointF pointB) {
        double dotProduct = (double) dotProduct(pointA, pointB);
        double value = dotProduct / (length(pointA) * length(pointB));
        if (value > 1.0) {
            value = 1.0;
        } else if (value < -1) {
            value = -1;
        }
        return value;
    }

    public static double dotProduct(PointF pointA, PointF pointB) {
        return pointA.x * pointB.x + pointA.y * pointB.y;
    }

    @SuppressLint("UseValueOf")
    public static void getArrowControlPt(PDFViewCtrl pdfViewCtrl, int pageindex, float x1, float y1, float x2, float y2) {
        float x3 = 0;
        float y3 = 0;
        float x4 = 0;
        float y4 = 0;

        double arctangent = PI / 6;
        double arrow_len = widthOnPageView(pdfViewCtrl, pageindex, DEFAULT_BORDER_WIDTH) * 6.0f;
        double[] endPoint_1 = rotateVec(x2 - x1, y2 - y1, arctangent, true, arrow_len);
        double[] endPoint_2 = rotateVec(x2 - x1, y2 - y1, -arctangent, true, arrow_len);

        double x_3 = x2 - endPoint_1[0];
        double y_3 = y2 - endPoint_1[1];
        double x_4 = x2 - endPoint_2[0];
        double y_4 = y2 - endPoint_2[1];

        Double X3 = new Double(x_3);
        x3 = X3.floatValue();
        Double Y3 = new Double(y_3);
        y3 = Y3.floatValue();
        Double X4 = new Double(x_4);
        x4 = X4.floatValue();
        Double Y4 = new Double(y_4);
        y4 = Y4.floatValue();

        leftPt.set(x3, y3);
        rightPt.set(x4, y4);
    }

    /**
     * x1 is the first point marker point X,y1 is that Y;
     * x2 is the second point marker point X,y2 is that Y;
     * judge and draw arrow line
     * Pi = 3.14159265358979323846f
     */
    public static Path getArrowPath(PDFViewCtrl pdfViewCtrl, int pageindex, float x1, float y1, float x2, float y2) {
        Path arrowPath = new Path();
        getArrowControlPt(pdfViewCtrl, pageindex, x1, y1, x2, y2);
        arrowPath.moveTo(leftPt.x, leftPt.y);
        arrowPath.lineTo(x2, y2);
        arrowPath.lineTo(rightPt.x, rightPt.y);
        return arrowPath;
    }

    public static double[] rotateVec(float px, float py, double ang, boolean isChlen, double newLen) {
        double rotateResult[] = new double[2];
        double vx = px * Math.cos(ang) - py * Math.sin(ang);
        double vy = px * Math.sin(ang) + py * Math.cos(ang);
        if (isChlen) {
            double d = Math.sqrt(vx * vx + vy * vy);
            vx = vx / d * newLen;
            vy = vy / d * newLen;
            rotateResult[0] = vx;
            rotateResult[1] = vy;
        }
        return rotateResult;
    }

    public static RectF getBorderRectByStartKneeAndEnding(float startX, float startY, float kneeX, float kneeY,
                                                          float endingX, float endingY) {
        RectF borderRect = new RectF();
        borderRect.left = Math.min(Math.min(startX, kneeX), Math.min(kneeX, endingX));
        borderRect.right = Math.max(Math.max(startX, kneeX), Math.max(kneeX, endingX));
        borderRect.top = Math.min(Math.min(startY, kneeY), Math.min(kneeY, endingY));
        borderRect.bottom = Math.max(Math.max(startY, kneeY), Math.max(kneeY, endingY));
        return borderRect;
    }

    public static void adjustKneeAndEndingPt(RectF bbox, RectF textBBox,
                                             PointF startingPt, PointF kneePt, PointF endingPt) {
        if (kneePt.x < textBBox.left) {
            kneePt.y = (textBBox.top + textBBox.bottom) / 2;
            endingPt.x = textBBox.left;
            endingPt.y = kneePt.y;
        }

        if (kneePt.x > textBBox.right) {
            kneePt.y = (textBBox.top + textBBox.bottom) / 2;
            endingPt.x = textBBox.right;
            endingPt.y = kneePt.y;
        }

        if (kneePt.y < textBBox.bottom) {
            kneePt.x = (textBBox.right + textBBox.left) / 2;
            endingPt.x = kneePt.x;
            endingPt.y = textBBox.bottom;
        }

        if (kneePt.y > textBBox.top) {
            kneePt.x = (textBBox.right + textBBox.left) / 2;
            endingPt.x = kneePt.x;
            endingPt.y = textBBox.top;
        }

        if (!bbox.contains(textBBox) || !bbox.contains(startingPt.x, startingPt.y)
                || !bbox.contains(kneePt.x, kneePt.y) || !bbox.contains(endingPt.x, endingPt.y)) {
            RectF borderRect = new RectF();
            borderRect.left = Math.min(Math.min(startingPt.x, kneePt.x), Math.min(kneePt.x, endingPt.x));
            borderRect.right = Math.max(Math.max(startingPt.x, kneePt.x), Math.max(kneePt.x, endingPt.x));
            borderRect.top = Math.max(Math.max(startingPt.y, kneePt.y), Math.max(kneePt.y, endingPt.y));
            borderRect.bottom = Math.min(Math.min(startingPt.y, kneePt.y), Math.min(kneePt.y, endingPt.y));

            bbox.left = Math.min(borderRect.left, textBBox.left);
            bbox.right = Math.max(borderRect.right, textBBox.right);
            bbox.bottom = Math.min(borderRect.bottom, textBBox.bottom);
            bbox.top = Math.max(bbox.top, textBBox.top);
        }
    }

    public static boolean isIntersectPointInLine(float x, float y, float x1, float y1, float x2, float y2) {
        boolean result = false;
        float biasLine = getDistanceOfTwoPoints(x1, y1, x2, y2);
        float biasLine1 = getDistanceOfTwoPoints(x, y, x1, y1);
        float biasLine2 = getDistanceOfTwoPoints(x, y, x2, y2);

        if ((biasLine1 + biasLine2) < biasLine + 3) {
            result = true;
        }
        return result;
    }

    public static float getDistanceOfTwoPoints(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    public static void resetKneeAndEndingPt(PDFViewCtrl pdfViewCtrl, int pageIndex, RectF textBBox,
                                            PointF startingPt, PointF kneePt, PointF endingPt) {
        if (kneePt.x < endingPt.x) {
            if (startingPt.x > textBBox.left && startingPt.x < textBBox.right && startingPt.y < textBBox.top) {
                endingPt.set((textBBox.left + textBBox.right) / 2, textBBox.top);
                kneePt.set(endingPt.x,
                        endingPt.y - widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH));
            } else if (startingPt.x > textBBox.left && startingPt.x < textBBox.right && startingPt.y > textBBox.bottom) {
                endingPt.set((textBBox.left + textBBox.right) / 2, textBBox.bottom);
                kneePt.set(endingPt.x,
                        endingPt.y + widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH));
            } else if (startingPt.x > textBBox.right) {
                endingPt.set(textBBox.right, (textBBox.top + textBBox.bottom) / 2);
                kneePt.set(endingPt.x + widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH),
                        endingPt.y);
            } else {
                endingPt.set(textBBox.left, (textBBox.top + textBBox.bottom) / 2);
                kneePt.set(endingPt.x - widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH),
                        endingPt.y);
            }
        } else if (kneePt.x > endingPt.x) {
            if (startingPt.x < textBBox.right && startingPt.x > textBBox.left && startingPt.y < textBBox.top) {
                endingPt.set((textBBox.left + textBBox.right) / 2, textBBox.top);
                kneePt.set(endingPt.x,
                        endingPt.y - widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH));
            } else if (startingPt.x < textBBox.right && startingPt.x > textBBox.left && startingPt.y > textBBox.bottom) {
                endingPt.set((textBBox.left + textBBox.right) / 2, textBBox.bottom);
                kneePt.set(endingPt.x,
                        endingPt.y + widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH));
            } else if (startingPt.x < textBBox.left) {
                endingPt.set(textBBox.left, (textBBox.top + textBBox.bottom) / 2);
                kneePt.set(endingPt.x - widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH),
                        endingPt.y);
            } else {
                endingPt.set(textBBox.right, (textBBox.top + textBBox.bottom) / 2);
                kneePt.set(endingPt.x + widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH),
                        endingPt.y);
            }
        } else if (kneePt.y < endingPt.y) {
            if (startingPt.x < textBBox.left) {
                endingPt.set(textBBox.left, (textBBox.top + textBBox.bottom) / 2);
                kneePt.set(endingPt.x - widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH),
                        endingPt.y);
            } else if (startingPt.x > textBBox.right) {
                endingPt.set(textBBox.right, (textBBox.top + textBBox.bottom) / 2);
                kneePt.set(endingPt.x + widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH),
                        endingPt.y);
            } else if (startingPt.y > textBBox.bottom && startingPt.x > textBBox.left && startingPt.x < textBBox.right) {
                endingPt.set((textBBox.left + textBBox.right) / 2, textBBox.bottom);
                kneePt.set(endingPt.x,
                        endingPt.y + widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH));
            } else {
                endingPt.set((textBBox.left + textBBox.right) / 2, textBBox.top);
                kneePt.set(endingPt.x,
                        endingPt.y - widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH));
            }
        } else if (kneePt.y > endingPt.y) {
            if (startingPt.x < textBBox.left) {
                endingPt.set(textBBox.left, (textBBox.top + textBBox.bottom) / 2);
                kneePt.set(endingPt.x - widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH),
                        endingPt.y);
            } else if (startingPt.x > textBBox.right) {
                endingPt.set(textBBox.right, (textBBox.top + textBBox.bottom) / 2);
                kneePt.set(endingPt.x + widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH),
                        endingPt.y);
            } else if (startingPt.y < textBBox.top && startingPt.x > textBBox.left && startingPt.x < textBBox.right) {
                endingPt.set((textBBox.left + textBBox.right) / 2, textBBox.top);
                kneePt.set(endingPt.x,
                        endingPt.y - widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH));
            } else {
                endingPt.set((textBBox.left + textBBox.right) / 2, textBBox.bottom);
                kneePt.set(endingPt.x,
                        endingPt.y + widthOnPageView(pdfViewCtrl, pageIndex, DEFAULT_KENNTOEND_WIDTH));
            }
        }
    }

    public static ArrayList<PointF> getCalloutLinePoints(FreeText annot) {
        PointF startingPt = new PointF(0, 0);
        PointF kneePt = new PointF(0, 0);
        PointF endingPt = new PointF(0, 0);
        try {
            String intent = annot.getIntent();
            if (intent != null && intent.equalsIgnoreCase("FreeTextCallout")) {
                PointFArray pointFArray = annot.getCalloutLinePoints();
                int pointCount = pointFArray.getSize();
                if (pointCount > 0) {
                    startingPt = AppUtil.toPointF(pointFArray.getAt(0));
                    if (pointCount > 1) {
                        kneePt = AppUtil.toPointF(pointFArray.getAt(1));
                        endingPt = new PointF(kneePt.x, kneePt.y);
                        if (pointCount == 3) {
                            endingPt = AppUtil.toPointF(pointFArray.getAt(2));
                        }
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        ArrayList<PointF> points = new ArrayList<>();
        points.add(startingPt);
        points.add(kneePt);
        points.add(endingPt);
        return points;
    }
}
