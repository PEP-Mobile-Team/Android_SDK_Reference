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

import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.annots.Ink;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;


class InkAnnotUtil {
    public long getSupportedProperties() {
        return PropertyBar.PROPERTY_COLOR
                | PropertyBar.PROPERTY_OPACITY
                | PropertyBar.PROPERTY_LINEWIDTH;
    }


    protected ArrayList<ArrayList<PointF>> docLinesFromPageView(PDFViewCtrl pdfViewCtrl, int pageIndex,
                                                                ArrayList<ArrayList<PointF>> lines, RectF bbox) {
        RectF bboxF = null;
        ArrayList<ArrayList<PointF>> docLines = new ArrayList<ArrayList<PointF>>();
        for (int i = 0; i < lines.size(); i++) {
            ArrayList<PointF> newLine = new ArrayList<PointF>();
            for (int j = 0; j < lines.get(i).size(); j++) {
                PointF curPoint = new PointF();
                curPoint.set(lines.get(i).get(j));
                if (bboxF == null) {
                    bboxF = new RectF(curPoint.x, curPoint.y, curPoint.x, curPoint.y);
                } else {
                    bboxF.union(curPoint.x, curPoint.y);
                }
                pdfViewCtrl.convertPageViewPtToPdfPt(curPoint, curPoint, pageIndex);
                newLine.add(curPoint);
            }
            docLines.add(newLine);
        }
        if (bboxF != null) {
            pdfViewCtrl.convertPageViewRectToPdfRect(bboxF, bboxF, pageIndex);
            bbox.set(bboxF.left, bboxF.top, bboxF.right, bboxF.bottom);
        }
        return docLines;
    }

    protected static ArrayList<Path> generatePathData(PDFViewCtrl pdfViewCtrl, int pageIndex, Ink annot) {
        return generateInkPaths(pdfViewCtrl, pageIndex, annot);
    }

    protected static ArrayList<Path> generateInkPaths(PDFViewCtrl pdfViewCtrl, int pageIndex, Ink annot) {
        try {
            com.foxit.sdk.common.Path pdfPath = annot.getInkList();
            if (pdfPath == null) return null;
            ArrayList<Path> paths = new ArrayList<Path>();
            PointF pointF = new PointF();

            int ptCount = pdfPath.getPointCount();
            if (ptCount == 1) {
                Path path = new Path();
                pointF.set(pdfPath.getPoint(0).getX(), pdfPath.getPoint(0).getY());
                pdfViewCtrl.convertPdfPtToPageViewPt(pointF, pointF, pageIndex);
                path.moveTo(pointF.x, pointF.y);
                path.lineTo(pointF.x + 0.1f, pointF.y + 0.1f);
                paths.add(path);
                return paths;
            }
            float cx = 0, cy = 0, ex, ey;
            Path path = null;
            for (int i = 0; i < ptCount; i++) {
                pointF.set(pdfPath.getPoint(i).getX(), pdfPath.getPoint(i).getY());
                pdfViewCtrl.convertPdfPtToPageViewPt(pointF, pointF, pageIndex);
                if (pdfPath.getPointType(i) == com.foxit.sdk.common.Path.e_TypeMoveTo) {
                    path = new Path();
                    path.moveTo(pointF.x, pointF.y);
                    cx = pointF.x;
                    cy = pointF.y;
                } else {
                    ex = (cx + pointF.x) / 2;
                    ey = (cy + pointF.y) / 2;
                    path.quadTo(cx, cy, ex, ey);
                    cx = pointF.x;
                    cy = pointF.y;
                }

                if (i == ptCount - 1 || ((i + 1) < ptCount && pdfPath.getPointType(i + 1) == com.foxit.sdk.common.Path.e_TypeMoveTo)) {
                    ex = pointF.x;
                    ey = pointF.y;
                    path.lineTo(ex, ey);

                    paths.add(path);
                }
            }
            return paths;
        } catch (PDFException e) {
            return null;
        }

    }

    public static void correctPvPoint(PDFViewCtrl pdfViewCtrl, int pageIndex, PointF pt) {
        pt.x = Math.max(0, pt.x);
        pt.y = Math.max(0, pt.y);
        pt.x = Math.min(pdfViewCtrl.getPageViewWidth(pageIndex), pt.x);
        pt.y = Math.min(pdfViewCtrl.getPageViewHeight(pageIndex), pt.y);
    }

    public static ArrayList<ArrayList<PointF>> cloneInkList(ArrayList<ArrayList<PointF>> lines) {
        if (lines == null) return null;
        ArrayList<ArrayList<PointF>> newLines = new ArrayList<ArrayList<PointF>>();
        for (int i = 0; i < lines.size(); i ++) {
            ArrayList<PointF> line = lines.get(i);
            ArrayList<PointF> newLine = new ArrayList<PointF>();
            for (int j = 0; j < line.size(); j ++) {
                newLine.add(new PointF(line.get(j).x, line.get(j).y));
            }
            newLines.add(newLine);
        }
        return newLines;
    }

    public static ArrayList<ArrayList<PointF>> generateInkList(com.foxit.sdk.common.Path pdfPath) {
        if (pdfPath == null) return null;
        ArrayList<ArrayList<PointF>> newLines = new ArrayList<ArrayList<PointF>>();
        ArrayList<PointF> newLine = null;

        try {
            int ptCount = pdfPath.getPointCount();
            for (int i = 0; i < ptCount; i ++) {
                if (pdfPath.getPointType(i) == com.foxit.sdk.common.Path.e_TypeMoveTo) {
                    newLine = new ArrayList<PointF>();
                }
                newLine.add(AppUtil.toPointF(pdfPath.getPoint(i)));

                if (i == ptCount - 1 || ((i + 1) < ptCount && pdfPath.getPointType(i + 1) == com.foxit.sdk.common.Path.e_TypeMoveTo)) {
                    newLines.add(newLine);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return newLines;
    }
}
