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
package com.foxit.uiextensions.modules.compare;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.objects.PDFArray;
import com.foxit.sdk.pdf.objects.PDFDictionary;
import com.foxit.sdk.pdf.objects.PDFObject;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.foxit.uiextensions.utils.AppAnnotUtil.ANNOT_SELECT_TOLERANCE;

public class CompareHandler {

    class DrawInfo {
        public static final int RESULTTYPE_DELETE = 1;
        public static final int RESULTTYPE_INSERT = 2;
        public static final int RESULTTYPE_REPLACEMENT = 3;

        int pageIndex;
        int resultType;
        String filterType;
        RectF rectF;
    }

    private Context mContext;
    private ViewGroup mParent;
    private PDFViewCtrl mPdfViewCtrl;
    private Annot mFocusAnnot;

    private CompareResultWindow mCompareResultWindow;
    private HashMap<Integer, List<DrawInfo>> mMapDiff = new HashMap<>();
    private int mMapCurIndex = -1;
    private List<DrawInfo> mMapDrawRect = new ArrayList<>();
    private Paint mPathPaint;
    private int mLineWidth = 3;

    public CompareHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;

        mPathPaint = new Paint();
        mPathPaint.setStyle(Paint.Style.STROKE);
        mPathPaint.setAntiAlias(true);
        mPathPaint.setDither(true);
        mPathPaint.setAlpha(255);
        mPathPaint.setStrokeWidth(mLineWidth);

        mCompareResultWindow = new CompareResultWindow(context, parent);
        mCompareResultWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
//                mFocusAnnot = null;
//                mMapDrawRect.clear();
//                mPdfViewCtrl.invalidate();
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getMainFrame().hideMaskView();
            }
        });
    }

    protected void fillDocDiffMap() {
        PDFViewCtrl.lock();
        if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) return;
        try {
            PDFDictionary root = mPdfViewCtrl.getDoc().getCatalog();
            if (root != null) {
                boolean bExistPieceInfo = root.hasKey("PieceInfo");
                if (!bExistPieceInfo) return;
                PDFDictionary pieceInfo = root.getElement("PieceInfo").getDict();
                if (pieceInfo == null) return;
                PDFDictionary comparePDF = pieceInfo.getElement("ComparePDF").getDict();
                if (comparePDF == null) return;
                PDFDictionary priDict = comparePDF.getElement("Private").getDict();
                if (priDict == null) return;
                PDFDictionary differences = priDict.getElement("Differences").getDict();
                if (differences == null) return;
                PDFArray numArray = differences.getElement("Nums").getArray();
                if (numArray == null) return;
                int count = numArray.getElementCount();
                for (int i = 0; i < count; i = i + 2) {
                    PDFObject object = numArray.getElement(i);
                    if (object == null) continue;
                    int index = object.getInteger();

                    DrawInfo oldInfo = new DrawInfo();
                    DrawInfo newInfo = new DrawInfo();

                    oldInfo.rectF = new RectF();
                    newInfo.rectF = new RectF();

                    PDFObject object1 = numArray.getElement(i + 1).getDict();
                    PDFArray cArray = ((PDFDictionary) object1).getElement("C").getArray();
                    if (cArray == null) continue;
                    PDFArray cArrayOld = cArray.getElement(0).getArray();
                    if (cArrayOld == null) continue;
                    int cArrayOldCount = cArrayOld.getElementCount();
                    for (int oldIndex = 0; oldIndex < cArrayOldCount; oldIndex ++) {
                        PDFObject oldObject = cArrayOld.getElement(oldIndex);
                        if (oldObject == null) continue;
                        float _index = oldObject.getFloat();
                        if (oldIndex == 0) {
                            oldInfo.rectF.left = _index;
                        } else if (oldIndex == 1) {
                            oldInfo.rectF.top = _index;
                        } else if (oldIndex == 2) {
                            oldInfo.rectF.right = _index;
                        } else if (oldIndex == 3) {
                            oldInfo.rectF.bottom = _index;
                        }
                    }
                    AppUtil.normalizePDFRect(oldInfo.rectF);

                    PDFArray cArrayNew = cArray.getElement(1).getArray();
                    if (cArrayNew == null) continue;
                    int cArrayNewCount = cArrayNew.getElementCount();
                    for (int newIndex = 0; newIndex < cArrayNewCount; newIndex ++) {
                        PDFObject newObject = cArrayNew.getElement(newIndex);
                        if (newObject == null) continue;
                        float _index = newObject.getFloat();
                        if (newIndex == 0) {
                            newInfo.rectF.left = _index;
                        } else if (newIndex == 1) {
                            newInfo.rectF.top = _index;
                        } else if (newIndex == 2) {
                            newInfo.rectF.right = _index;
                        } else if (newIndex == 3) {
                            newInfo.rectF.bottom = _index;
                        }
                    }
                    AppUtil.normalizePDFRect(newInfo.rectF);

                    String outD = ((PDFDictionary) object1).getElement("D").getWideString();
                    int resultType = 0;
                    if (outD.equals("D")) {
                        resultType = DrawInfo.RESULTTYPE_DELETE;
                    } else if (outD.equals("I")) {
                        resultType = DrawInfo.RESULTTYPE_INSERT;
                    } else if (outD.equals("R")) {
                        resultType = DrawInfo.RESULTTYPE_REPLACEMENT;
                    }

                    oldInfo.resultType = resultType;
                    newInfo.resultType = resultType;

                    String outT = ((PDFDictionary) object1).getElement("T").getWideString();
                    oldInfo.filterType = outT;
                    newInfo.filterType = outT;

                    PDFArray pageArray = ((PDFDictionary) object1).getElement("Pg").getArray();
                    if (pageArray == null) continue;
                    int pageCount = pageArray.getElementCount();
                    if (pageCount != 2) continue;
                    PDFObject oldIndexObject = pageArray.getElement(0);
                    if (oldIndexObject == null) continue;
                    oldInfo.pageIndex = oldIndexObject.getInteger();
                    PDFObject newIndexObject = pageArray.getElement(1);
                    if (newIndexObject == null) continue;
                    newInfo.pageIndex = newIndexObject.getInteger();

                    ArrayList<DrawInfo> list = new ArrayList<>();
                    list.add(oldInfo);
                    list.add(newInfo);
                    mMapDiff.put(index, list);
                }
            }
        } catch (PDFException e) {

        } finally {
            PDFViewCtrl.unlock();
        }
        return;
    }

    private void onAnnotSetFocus(Annot annot) {
        try {
            if (mMapDiff.size() == 0) return;
            RectF annotRect = AppUtil.toRectF(annot.getRect());
            AppUtil.normalizePDFRect(annotRect);
            mMapDrawRect.clear();
            for (Map.Entry<Integer, List<DrawInfo>> entry : mMapDiff.entrySet()) {
                List<DrawInfo> list = entry.getValue();
                DrawInfo oldInfo = list.get(0);
                DrawInfo newInfo = list.get(1);

                if (annotRect.equals(oldInfo.rectF) || annotRect.equals(newInfo.rectF)) {
                    int curIndex = entry.getKey();

                    mMapCurIndex = curIndex;
                    mMapDrawRect.add(oldInfo);
                    mMapDrawRect.add(newInfo);
                    return;
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onDraw(int pageIndex, Canvas canvas) {
        if (mMapDrawRect.size() == 0 || mFocusAnnot == null || mFocusAnnot.isEmpty()) return;
        try {
            mPathPaint.setColor(mFocusAnnot.getBorderColor());

            DrawInfo oldInfo = mMapDrawRect.get(0);
            RectF rectF = new RectF();
            if (pageIndex == oldInfo.pageIndex) {
                rectF.set(oldInfo.rectF);
            }

            DrawInfo newInfo = mMapDrawRect.get(1);
            if (pageIndex == newInfo.pageIndex) {
                rectF.set(newInfo.rectF);
            }

            if (!rectF.equals(new RectF())) {
                RectF annotRect = AppUtil.toRectF(mFocusAnnot.getRect());
                AppUtil.normalizePDFRect(annotRect);
                Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
                if (annotRect.equals(rectF)) {
                    rectF = AppUtil.toRectF(mFocusAnnot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                } else {
                    //It is not the focus annot.
                    PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
                    ArrayList<PointF> pts = new ArrayList<>(4);
                    pts.add(new PointF(rectF.left, rectF.top));
                    pts.add(new PointF(rectF.left, rectF.bottom));
                    pts.add(new PointF(rectF.right, rectF.top));
                    pts.add(new PointF(rectF.right, rectF.bottom));
                    Annot annot = null;
                    for (int i = 0; i < 4; i ++) {
                        PointF pdfPoint = pts.get(i);
                        annot = AppAnnotUtil.createAnnot(page.getAnnotAtPoint(AppUtil.toFxPointF(pdfPoint), 0));
                        if (annot != null && !annot.isEmpty()) {
                            if (AppUtil.toRectF(annot.getRect()).equals(rectF)) {
                                break;
                            } else {
                                annot = null;
                            }
                        }
                    }
                    if (annot != null && !annot.isEmpty()) {
                        rectF = AppUtil.toRectF(annot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                    } else {
                        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
                    }
                }

                canvas.save();
                rectF.inset(-10, -10);
                canvas.drawRect(rectF, mPathPaint);
                canvas.restore();
            }

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {
        try {
            PointF pdfPoint = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            Annot annot = null;
            if (page != null && !page.isEmpty()) {
                annot = AppAnnotUtil.createAnnot(page.getAnnotAtDevicePoint(AppUtil.toFxPointF(pdfPoint), ANNOT_SELECT_TOLERANCE,
                        AppUtil.toMatrix2D(mPdfViewCtrl.getDisplayMatrix(pageIndex))));
            }
            if (annot != null && !annot.isEmpty() && AppAnnotUtil.isSupportGroup(annot)) {
                annot = AppAnnotUtil.createAnnot(((Markup) annot).getGroupHeader());
            }

            if (annot != null && !annot.isEmpty() && mMapDiff.size() > 0) {
                onAnnotSetFocus(annot);
                if (mMapDrawRect.size() != 0) {
                    showCompareResult(annot);

                    RectF oldRect = new RectF(mMapDrawRect.get(0).rectF);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(oldRect, oldRect, mMapDrawRect.get(0).pageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(oldRect, oldRect, mMapDrawRect.get(0).pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(oldRect));

                    RectF newRect = new RectF(mMapDrawRect.get(1).rectF);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(newRect, newRect, mMapDrawRect.get(1).pageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(newRect, newRect, mMapDrawRect.get(0).pageIndex);
                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(newRect));
                }
                mFocusAnnot = annot;
                return true;
            }

            if (mFocusAnnot != null) {
                mFocusAnnot = null;
                mMapDrawRect.clear();
                mPdfViewCtrl.invalidate();
                return true;
            }

        } catch (PDFException e) {
        }
        return false;
    }
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        return true;
    }

    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent) {
        return true;
    }

    private void showCompareResult(Annot annot) {
        int type = mMapDrawRect.get(0).resultType;
        String title = getCompareTitle(type);
        mCompareResultWindow.setTitle(title);
        try {
            String content = annot.getContent();
            mCompareResultWindow.setContent(content);
        } catch (PDFException e) {
        }

        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getMainFrame().showMaskView();

        int width = (int) (mParent.getWidth() * 0.4);
        mCompareResultWindow.setWidth(width);
        mCompareResultWindow.show();
    }

    private String getCompareTitle(int type) {
        String title = "";
        if (type == DrawInfo.RESULTTYPE_DELETE) {
            title = mContext.getString(R.string.deleted);
        } else if (type == DrawInfo.RESULTTYPE_INSERT) {
            title = mContext.getString(R.string.inserted);
        } else if (type == DrawInfo.RESULTTYPE_REPLACEMENT){
            title = mContext.getString(R.string.replaced);
        }

        return title;
    }

    protected void reset() {
        mFocusAnnot = null;
        mMapDrawRect.clear();
    }
}
