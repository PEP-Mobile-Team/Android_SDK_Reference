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
package com.foxit.uiextensions.annots.freetext;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.SparseArray;
import android.view.View;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.common.Font;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.DefaultAppearance;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;

import java.util.ArrayList;


public class FtTextUtil {
    private static final float NEWLINE_BORDER_REDUNDANT_THRESHOLDS = 10f;
    private Context mContext;

    private PDFViewCtrl mPdfViewCtrl;
    private int mCurrentPageIndex;
    private String mTextContent = "";
    private boolean mEditState;
    private float mMaxTextWidth = 0;
    private float mMaxTextHeight = 0;
    private float mStartPointX = 0;
    private float mStartPointY = 0;
    private float mEditPointX = 0;
    private float mEditPointY = 0;
    private int mTextColor;
    private int mTextOpacity;
    private String mFontName;
    private float mFontSize;
    private int mCurrentSelectIndex;

    private int mSpace;
    private float mStartToBorderX;
    private float mStartToBorderY;

    private float mFontHeight = 0;
    private int mPageLineNum = 0;
    private int mRealLine = 0;
    private int mCurrentLine = 0;
    private ArrayList<String> mStringList = new ArrayList<String>();
    private Paint mTextPaint = new Paint();
    private Paint mCursorPaint = new Paint();
    private float mCurrentLineWidth;
    private boolean mResetEdit;
    private FontMetrics mFontMetrics;
    private Blink mBlink;
    private boolean mInvalidate = true;
    private float mPageOffset;
    private String mTempTextContent = "";
    private SparseArray<String> mFontIDMap;

    private OnTextValuesChangedListener mTextValuesChangedListener;

    public interface OnTextValuesChangedListener {
        public void onMaxWidthChanged(float maxWidth);

        public void onMaxHeightChanged(float maxHeight);

        public void onCurrentSelectIndex(int selectIndex);

        public void onEditPointChanged(float editPointX, float editPointY);
    }

    public void setOnWidthChanged(OnTextValuesChangedListener listener) {
        if (mTextValuesChangedListener == null) {
            mTextValuesChangedListener = listener;
        }
    }

    public FtTextUtil(Context context, PDFViewCtrl pdfViewCtrl) {

        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;

        mCursorPaint.setColor(Color.BLACK);
        mCursorPaint.setStyle(Style.STROKE);
        mCursorPaint.setAntiAlias(true);
        mCursorPaint.setDither(true);
        mCursorPaint.setStrokeWidth(AppDisplay.getInstance(context).dp2px(2));

        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setAntiAlias(true);
        initFontIdMap();
    }

    private void initFontIdMap() {
        mFontIDMap = new SparseArray<>();
        mFontIDMap.put(Font.e_StdIDCourier, "Courier");
        mFontIDMap.put(Font.e_StdIDHelvetica, "Helvetica");
        mFontIDMap.put(Font.e_StdIDTimes, "Times");
    }

    public String getSupportFontName(int id) {
        return mFontIDMap.get(id) != null ? mFontIDMap.get(id) : "Courier";
    }

    public Font getSupportFont(int id) {
        Font font = null;
        try {
            if (id < Font.e_StdIDCourier || id > Font.e_StdIDZapfDingbats) {
                font = new Font(Font.e_StdIDCourier);
            } else if (id == Font.e_StdIDCourier) {
                font = new Font(Font.e_StdIDCourier);
            } else if (id == Font.e_StdIDHelvetica) {
                font = new Font(Font.e_StdIDHelvetica);
            } else if (id == Font.e_StdIDTimes) {
                font = new Font(Font.e_StdIDTimes);
            } else {
                font = new Font(Font.e_StdIDCourier);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return font;
    }

    public Font getStandardFont(int id) {
        Font font = null;
        try {
            if (id >= Font.e_StdIDCourier && id <= Font.e_StdIDZapfDingbats)
                font = new Font(id);
            else
                font = new Font(Font.e_StdIDCourier);
        } catch (PDFException e){
            e.printStackTrace();
        }
        return font;
    }

    public int getSupportFontID(DefaultAppearance da, PDFDoc doc) {
        int id = Font.e_StdIDCourier;
        try {
            Font font = da != null ? da.getFont() : null;
            if (font != null && !font.isEmpty())
                id = font.getStandard14Font(doc);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return id;
    }

    public int getSupportFontID(String name) {
        int fontId;
        if (name == null || name.equals("")) {
            fontId = Font.e_StdIDCourier;
        } else {
            if (!name.startsWith("Cour") && !name.equalsIgnoreCase("Courier")
                    && !name.startsWith("Helv") && !name.equalsIgnoreCase("Helvetica")
                    && !name.startsWith("Time") && !name.equalsIgnoreCase("Times")) {
                fontId = Font.e_StdIDCourier;
            } else {
                if (name.equals("Courier")){
                    fontId = Font.e_StdIDCourier;
                } else if(name.equals("Helvetica")){
                    fontId = Font.e_StdIDHelvetica;
                } else if(name.equals("Times")){
                    fontId = Font.e_StdIDTimes;
                } else {
                    fontId = Font.e_StdIDCourier;
                }
            }
        }
        return fontId;
    }

    public void setTextString(int pageIndex, String text, boolean editState) {
        mCurrentPageIndex = pageIndex;
        mTextContent = text;
        mEditState = editState;
    }

    public void setStartPoint(PointF startPoint) {
        mStartPointX = startPoint.x;
        mStartPointY = startPoint.y;
    }

    public void setEditPoint(PointF editPoint) {
        mEditPointX = editPoint.x;
        mEditPointY = editPoint.y;
    }

    public void setMaxRect(float width, float height) {
        mMaxTextWidth = width;
        mMaxTextHeight = height;
    }

    public void setTextColor(int textColor, int opacity) {
        mTextColor = textColor;
        mTextOpacity = opacity;
    }

    public void setFont(String fontName, float fontSize) {
        mFontName = fontName;
        mFontSize = fontSize;
    }

    public void setEndSelection(int endSelection) {
        mCurrentSelectIndex = endSelection;
    }

    public void loadText(boolean isCalc) {
        mStringList.clear();
        mRealLine = 0;
        mCurrentLineWidth = 0;

        setPaintFont(mTextPaint, mFontName);
        mTextPaint.setTextSize(PdfSize2PageViewSize(mPdfViewCtrl, mCurrentPageIndex, mFontSize));
        mTextPaint.setColor(mTextColor);
        mTextPaint.setAlpha(mTextOpacity);

        mFontHeight = getFontHeight(mPdfViewCtrl, mCurrentPageIndex, mFontName, mFontSize);
        mPageLineNum = (int) (mMaxTextHeight / mFontHeight);
        mSpace = (int) mFontHeight * 5;

        if (mTextContent != null && !mTextContent.equals("")) {
            if (isCalc) {
                mStringList = getComposedText(mPdfViewCtrl, mCurrentPageIndex, new RectF(0, 0, mMaxTextWidth, mMaxTextHeight),
                        mTextContent, mFontName, mFontSize, true);
            } else {
                mStringList = getComposedText(mPdfViewCtrl, mCurrentPageIndex, new RectF(0, 0, mMaxTextWidth, mMaxTextHeight),
                        mTextContent, mFontName, mFontSize);
            }
        }
        jumpPageWithKeyBoard();
        if (mTextContent != null && !mTextContent.equals("")) {
            mRealLine = mStringList.size();
            adjustEditPoint(mPdfViewCtrl, mCurrentPageIndex, mStringList, mFontName, mFontSize);
            float maxWidth = getTextMaxWidth(mPdfViewCtrl, mCurrentPageIndex, mStringList, mFontName, mFontSize);
            mTextValuesChangedListener.onMaxHeightChanged(mFontHeight * mRealLine);
            mTextValuesChangedListener.onMaxWidthChanged(maxWidth);

            PointF point = new PointF(mStartPointX, mStartPointY);
            mPdfViewCtrl.convertPageViewPtToDisplayViewPt(point, point, mCurrentPageIndex);


            if (mCurrentPageIndex == mPdfViewCtrl.getPageCount() - 1 ||
                    (!mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE)) {
                mStartToBorderX = AppDisplay.getInstance(mContext).getRawScreenWidth() - point.x;
                mStartToBorderY = AppDisplay.getInstance(mContext).getRawScreenHeight() - point.y + mPageOffset;
            } else {
                mStartToBorderX = AppDisplay.getInstance(mContext).getRawScreenWidth() - point.x;
                mStartToBorderY = AppDisplay.getInstance(mContext).getRawScreenHeight() - point.y;
            }
            if (!mTextContent.equals(mTempTextContent)) {
                jumpPageWithHorizontal(maxWidth);
                jumpPageWithVertical(maxWidth);
            }
        }
    }

    public void loadText() {
        loadText(false);
    }

    public ArrayList<String> getComposedText(PDFViewCtrl pdfViewCtrl, int pageIndex, RectF annotRect, String textContent, String fontName, float fontSize, int annotRotation, boolean isCalc) {
        ArrayList<String> composedText = new ArrayList<String>();
        float tCurrentLineWidth = 0;
        Paint paint = new Paint();
        setPaintFont(paint, fontName);
        paint.setTextSize(fontSize * 100);
        RectF rect = new RectF(annotRect);
        PDFPage page = null;
        int rotate;
        try {
            page = pdfViewCtrl.getDoc().getPage(pageIndex);
            rotate = (page.getRotation() + mPdfViewCtrl.getViewRotation()) % 4;
        } catch (PDFException e) {
            e.printStackTrace();
            rotate = Constants.e_Rotation0;
        }

        int delRoataion = rotate - annotRotation;
        boolean isVertical = delRoataion == 1 || delRoataion == 3;
        pdfViewCtrl.convertPageViewRectToPdfRect(rect, rect, pageIndex);
        float width = 0;
        float height = 0;
        if (isVertical) {
            width = Math.abs(rect.height());
            height = Math.abs(rect.width());
        } else {
            height = Math.abs(rect.height());
            width = Math.abs(rect.width());
        }

        char ch;
        int iStart = 0;
        if (textContent == null)
            textContent = "";
        int count = textContent.length();
        for (int i = 0; i < count; i++) {
            ch = textContent.charAt(i);
            String str = String.valueOf(ch);
            float chWidth = paint.measureText(str);
            if (ch == '\n' || ch == '\r') {
                composedText.add(textContent.substring(iStart, i + 1));
                iStart = i + 1;
                if (i == count - 1) {
                    composedText.add("");
                }
                tCurrentLineWidth = 0;
            } else {
                tCurrentLineWidth += chWidth;

                if (!isCalc) {
                    if (i == count - 1) {
                        composedText.add(textContent.substring(iStart, count));
                    }
                    continue;
                }

                if (tCurrentLineWidth > (((rotate == Constants.e_Rotation90) || (rotate == Constants.e_Rotation270)) ? (height * 100 + NEWLINE_BORDER_REDUNDANT_THRESHOLDS) : width * 100 + NEWLINE_BORDER_REDUNDANT_THRESHOLDS)
                        && (((rotate == Constants.e_Rotation90) || (rotate == Constants.e_Rotation270)) ? (height * 100 + NEWLINE_BORDER_REDUNDANT_THRESHOLDS) : width * 100 + NEWLINE_BORDER_REDUNDANT_THRESHOLDS) > chWidth) {
                    composedText.add(textContent.substring(iStart, i));
                    iStart = i;
                    i--;
                    tCurrentLineWidth = 0;
                } else {
                    if (i == count - 1) {
                        composedText.add(textContent.substring(iStart, count));
                    }
                }
            }
        }
        mCurrentLineWidth = PdfSize2PageViewSize(pdfViewCtrl, pageIndex, tCurrentLineWidth) / 100;
        return composedText;
    }

    public ArrayList<String> getComposedText(PDFViewCtrl pdfViewCtrl, int pageIndex, RectF annotRect, String textContent, String fontName, float fontSize, boolean isCalc) {
        ArrayList<String> composedText = new ArrayList<String>();
        float tCurrentLineWidth = 0;
        Paint paint = new Paint();
        setPaintFont(paint, fontName);
        paint.setTextSize(fontSize * 100);
        RectF rect = new RectF(annotRect);
        PDFPage page = null;
        int rotate;
        try {
            page = pdfViewCtrl.getDoc().getPage(pageIndex);
            rotate = (page.getRotation() + mPdfViewCtrl.getViewRotation()) % 4;
        } catch (PDFException e) {
            e.printStackTrace();
            rotate = Constants.e_Rotation0;
        }

        pdfViewCtrl.convertPageViewRectToPdfRect(rect, rect, pageIndex);

        char ch;
        int iStart = 0;
        if (textContent == null)
            textContent = "";
        int count = textContent.length();
        if (count > 0){
            for (int i = 0; i < count; i++) {
                ch = textContent.charAt(i);
                String str = String.valueOf(ch);
                float chWidth = paint.measureText(str);
                if (ch == '\n' || ch == '\r') {
                    composedText.add(textContent.substring(iStart, i + 1));
                    iStart = i + 1;
                    if (i == count - 1) {
                        composedText.add("");
                    }
                    tCurrentLineWidth = 0;
                } else {
                    tCurrentLineWidth += chWidth;

                    if (!isCalc) {
                        if (i == count - 1) {
                            composedText.add(textContent.substring(iStart, count));
                        }
                        continue;
                    }

                    if (tCurrentLineWidth > (((rotate == Constants.e_Rotation90) || (rotate == Constants.e_Rotation270)) ? (Math.abs(rect.height()) * 100 + NEWLINE_BORDER_REDUNDANT_THRESHOLDS) : (Math.abs(rect.width())) * 100 + NEWLINE_BORDER_REDUNDANT_THRESHOLDS)
                            && (((rotate == Constants.e_Rotation90) || (rotate == Constants.e_Rotation270)) ? (Math.abs(rect.height()) * 100 + NEWLINE_BORDER_REDUNDANT_THRESHOLDS) : (Math.abs(rect.width())) * 100 + NEWLINE_BORDER_REDUNDANT_THRESHOLDS) > chWidth) {
                        composedText.add(textContent.substring(iStart, i));
                        iStart = i;
                        i--;
                        tCurrentLineWidth = 0;
                    } else {
                        if (i == count - 1) {
                            composedText.add(textContent.substring(iStart, count));
                        }
                    }
                }
            }
        } else {
            composedText.add("");
        }
        mCurrentLineWidth = PdfSize2PageViewSize(pdfViewCtrl, pageIndex, tCurrentLineWidth) / 100;
        return composedText;
    }

    public ArrayList<String> getComposedText(PDFViewCtrl pdfViewCtrl, int pageIndex, RectF annotRect, String textContent, String fontName, float fontSize) {
        return getComposedText(pdfViewCtrl, pageIndex, annotRect, textContent, fontName, fontSize, mEditState);
    }

    public void adjustTextRect(PDFViewCtrl pdfViewCtrl, int pageIndex, String fontName, float fontSize, RectF rect, ArrayList<String> composedText) {
        float rectHeight = getFontHeight(pdfViewCtrl, pageIndex, fontName, fontSize) * composedText.size();
        if (rectHeight > rect.height()) {
            rect.bottom = rect.top + rectHeight;
        }
    }

    private float getTextMaxWidth(PDFViewCtrl pdfViewCtrl, int pageIndex, ArrayList<String> textStrings, String fontName, float fontSize) {
        Paint paint = new Paint();
        setPaintFont(paint, fontName);
        paint.setTextSize(fontSize * 100);
        float maxWidth = 0.0f;
        for (int i = 0; i < textStrings.size(); i++) {
            String currentLineStr = textStrings.get(i);
            maxWidth = Math.max(maxWidth, paint.measureText(currentLineStr));
        }
        maxWidth = PdfSize2PageViewSize(pdfViewCtrl, pageIndex, maxWidth);
        maxWidth = maxWidth / 100;//to pageView width
        maxWidth = Math.max(maxWidth, mCurrentLineWidth);
        return maxWidth;
    }


    private void adjustEditPoint(PDFViewCtrl pdfViewCtrl, int pageIndex, ArrayList<String> textStrings, String fontName, float fontSize) {

        int editSelectIndex = 0;
        Paint paint = new Paint();
        setPaintFont(paint, fontName);
        paint.setTextSize(fontSize * 100);

        if (!mResetEdit) {
            for (int i = 0; i < textStrings.size(); i++) {
                String currentLineStr = textStrings.get(i);
                if (i > 0 && currentLineStr.contains("\n")) {
                    editSelectIndex++;
                }
                if (i == textStrings.size() - 1) {
                    editSelectIndex++;
                }
                currentLineStr = currentLineStr.replace("\n", "");
                float currentlinewidth = PdfSize2PageViewSize(pdfViewCtrl, pageIndex, paint.measureText(currentLineStr)) / 100;
                RectF rect = new RectF(mStartPointX, mStartPointY + getFontHeight(pdfViewCtrl, pageIndex, fontName, fontSize) * i,
                        mStartPointX + currentlinewidth, mStartPointY + getFontHeight(pdfViewCtrl, pageIndex, fontName, fontSize) * (i + 1));
                if (rect.contains(mEditPointX, mEditPointY)) {
                    float currentWidth = 0.0f;
                    for (int j = 0; j < currentLineStr.length(); j++) {
                        char ch = currentLineStr.charAt(j);
                        String str = String.valueOf(ch);
                        float charWidth = paint.measureText(str);
                        currentWidth += charWidth;
                        float currentWidthPage = PdfSize2PageViewSize(pdfViewCtrl, pageIndex, currentWidth) / 100;
                        float charWidthPage = PdfSize2PageViewSize(pdfViewCtrl, pageIndex, charWidth) / 100;
                        if (mEditPointX >= mStartPointX + currentWidthPage - charWidthPage
                                && mEditPointX < mStartPointX + currentWidthPage - charWidthPage / 2) {
                            mEditPointX = mStartPointX + currentWidthPage - charWidthPage;
                            mEditPointY = mStartPointY + mFontHeight * i;
                            mCurrentSelectIndex = editSelectIndex + j;
                            mTextValuesChangedListener.onCurrentSelectIndex(mCurrentSelectIndex);
                            mResetEdit = true;
                            mTextValuesChangedListener.onEditPointChanged(mEditPointX, mEditPointY);
                            break;
                        } else if (mEditPointX >= mStartPointX + currentWidthPage - charWidthPage / 2
                                && mEditPointX < mStartPointX + currentWidthPage) {
                            mEditPointX = mStartPointX + currentWidthPage;
                            mEditPointY = mStartPointY + mFontHeight * i;
                            mCurrentSelectIndex = editSelectIndex + j + 1;
                            mTextValuesChangedListener.onCurrentSelectIndex(mCurrentSelectIndex);
                            mTextValuesChangedListener.onEditPointChanged(mEditPointX, mEditPointY);
                            mResetEdit = true;
                            break;
                        }
                    }
                    break;
                } else {
                    if (mEditPointY >= mStartPointY + getFontHeight(pdfViewCtrl, pageIndex, fontName, fontSize) * i
                            && mEditPointY < mStartPointY + getFontHeight(pdfViewCtrl, pageIndex, fontName, fontSize) * (i + 1)) {
                        mEditPointX = mStartPointX + currentlinewidth;
                        mEditPointY = mStartPointY + mFontHeight * i;
                        mCurrentSelectIndex = editSelectIndex + currentLineStr.length();
                        if (mCurrentSelectIndex > mTextContent.length()) {
                            mCurrentSelectIndex = mTextContent.length();
                        }
                        mTextValuesChangedListener.onCurrentSelectIndex(mCurrentSelectIndex);
                        mTextValuesChangedListener.onEditPointChanged(mEditPointX, mEditPointY);
                        mResetEdit = true;
                        break;
                    }
                }
                editSelectIndex += currentLineStr.length();
            }
        } else {
            if (mTempTextContent.length() != mTextContent.length()) {
                for (int i = 0; i < textStrings.size(); i++) {
                    String currentLineStr = textStrings.get(i);
                    if (i > 0 && currentLineStr.contains("\n")) {
                        editSelectIndex++;
                    }
                    if (i == textStrings.size() - 1) {
                        editSelectIndex++;
                    }
                    currentLineStr = currentLineStr.replace("\n", "");
                    if (mCurrentSelectIndex >= editSelectIndex
                            && mCurrentSelectIndex <= editSelectIndex + currentLineStr.length()) {
                        float currentWidth = 0.0f;
                        if (currentLineStr.length() > 0) {
                            for (int j = 0; j < currentLineStr.length(); j++) {
                                char ch = currentLineStr.charAt(j);
                                String str = String.valueOf(ch);
                                float charWidth = paint.measureText(str);
                                currentWidth += charWidth;
                                float currentWidthPage = PdfSize2PageViewSize(pdfViewCtrl, pageIndex, currentWidth) / 100;
                                if (mCurrentSelectIndex == editSelectIndex) {
                                    mEditPointX = mStartPointX;
                                    mEditPointY = mStartPointY + mFontHeight * i;
                                    mTextValuesChangedListener.onEditPointChanged(mEditPointX, mEditPointY);
                                    break;
                                } else if (mCurrentSelectIndex == editSelectIndex + j + 1) {
                                    mEditPointX = mStartPointX + currentWidthPage;
                                    mEditPointY = mStartPointY + mFontHeight * i;
                                    mTextValuesChangedListener.onEditPointChanged(mEditPointX, mEditPointY);
                                    break;
                                }
                            }
                        } else {
                            mEditPointX = mStartPointX;
                            mEditPointY = mStartPointY + mFontHeight * i;
                            mTextValuesChangedListener.onEditPointChanged(mEditPointX, mEditPointY);
                        }
                    }
                    editSelectIndex += currentLineStr.length();
                }
            }
        }
    }

    private void jumpPageWithHorizontal(float tempmaxWidth) {
        RectF rectF = new RectF(0, 0, tempmaxWidth, mFontHeight * mRealLine);
        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentPageIndex);

        if (rectF.width() > mStartToBorderX && mEditState && getKeyboardHeight() > 0) {
            mPdfViewCtrl.gotoPage(mCurrentPageIndex,
                    getPageViewOrigin(mPdfViewCtrl, mCurrentPageIndex, mStartPointX, mStartPointY).x
                            + (rectF.width() - mStartToBorderX + 2),
                    getPageViewOrigin(mPdfViewCtrl, mCurrentPageIndex, mStartPointX, mStartPointY).y);
        }
    }

    private void jumpPageWithVertical(float tempmaxWidth) {
        RectF rectF = new RectF(0, 0, tempmaxWidth, mFontHeight * mRealLine);
        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rectF, rectF, mCurrentPageIndex);

        if (mEditState && rectF.height() > mStartToBorderY - getKeyboardHeight()
                && getKeyboardHeight() > AppDisplay.getInstance(mContext).getRawScreenHeight() / 5) {
            float offset = rectF.height() - (mStartToBorderY - getKeyboardHeight()) + 2;
            mPageOffset += offset;

            if (mCurrentPageIndex == mPdfViewCtrl.getPageCount() - 1 ||
                    (!mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE)) {
                if (mPageOffset < getKeyboardHeight()) {
                    mPdfViewCtrl.layout(0, 0 - (int) mPageOffset, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight() - (int) mPageOffset);
                }
            }
            mPdfViewCtrl.gotoPage(mCurrentPageIndex,
                    getPageViewOrigin(mPdfViewCtrl, mCurrentPageIndex, mStartPointX, mStartPointY).x,
                    getPageViewOrigin(mPdfViewCtrl, mCurrentPageIndex, mStartPointX, mStartPointY).y + offset);
        }
    }

    private void jumpPageWithKeyBoard() {
        PointF point = new PointF(mStartPointX, mStartPointY);
        mPdfViewCtrl.convertPageViewPtToDisplayViewPt(point, point, mCurrentPageIndex);

        if (mEditState && getKeyboardHeight() > AppDisplay.getInstance(mContext).getRawScreenHeight() / 5) {
            if (AppDisplay.getInstance(mContext).getRawScreenHeight() - (point.y - mPageOffset + mFontHeight * (mStringList.size() == 0 ? 1 : mStringList.size())) < getKeyboardHeight()) {
                if (mTextContent != null && !mTextContent.equals("")) {
                    int realine = getComposedText(mPdfViewCtrl, mCurrentPageIndex, new RectF(0, 0, mMaxTextWidth, mMaxTextHeight),
                            mTextContent, mFontName, mFontSize).size();
                    mPageOffset = getKeyboardHeight() + mFontHeight * realine - (AppDisplay.getInstance(mContext).getRawScreenHeight() - point.y) + 2;
                } else {
                    mPageOffset = getKeyboardHeight() + mFontHeight - (AppDisplay.getInstance(mContext).getRawScreenHeight() - point.y) + 2;
                }
                if (mCurrentPageIndex == mPdfViewCtrl.getPageCount() - 1
                        || (!mPdfViewCtrl.isContinuous() && mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE)
                        || mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_FACING
                        || mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_COVER) {
                    mPdfViewCtrl.layout(0, 0 - (int) mPageOffset, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight() - (int) mPageOffset);
                }
                mPdfViewCtrl.gotoPage(mCurrentPageIndex,
                        getPageViewOrigin(mPdfViewCtrl, mCurrentPageIndex, mStartPointX, mStartPointY).x,
                        getPageViewOrigin(mPdfViewCtrl, mCurrentPageIndex, mStartPointX, mStartPointY).y + mPageOffset);
            }
        }
        if (mEditState && getKeyboardHeight() < AppDisplay.getInstance(mContext).getRawScreenHeight() / 5
                && mCurrentPageIndex == mPdfViewCtrl.getPageCount() - 1/* || mPdfViewCtrl.getPageLayoutMode() == PDFViewCtrl.PAGELAYOUTMODE_SINGLE*/) {
            if (mPageOffset > 0) {
                mPdfViewCtrl.layout(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
                mPageOffset = 0;
                mPdfViewCtrl.gotoPage(mCurrentPageIndex,
                        getPageViewOrigin(mPdfViewCtrl, mCurrentPageIndex, mStartPointX, mStartPointY).x,
                        getPageViewOrigin(mPdfViewCtrl, mCurrentPageIndex, mStartPointX, mStartPointY).y);

            }
        }
    }

    public void resetEditState() {
        mResetEdit = false;
    }

    public void drawText(final Canvas canvas) {
        if (mTextContent != null && !mTextContent.equals("")) {
            mTempTextContent = mTextContent;
            if (mEditState && mInvalidate) {
                if (mEditPointX != 0 || mEditPointY != 0) {
                    //draw cursor
                    canvas.drawLine(mEditPointX, mEditPointY, mEditPointX, mEditPointY + mFontHeight, mCursorPaint);

                } else {
                    //draw cursor
                    canvas.drawLine(mStartPointX + mCurrentLineWidth, mStartPointY + mFontHeight * (mRealLine - 1),
                            mStartPointX + mCurrentLineWidth, mStartPointY + mFontHeight * mRealLine, mCursorPaint);
                }
            }
            for (int i = mCurrentLine, j = 0; i < mRealLine; i++, j++) {
                if (j > mPageLineNum) {
                    break;
                }
                float textBaseY = mStartPointY + mFontHeight * i + mFontHeight / 2
                        - (mFontMetrics.ascent + mFontMetrics.descent) / 2;
                String drawStr = mStringList.get(i);
                int count = drawStr.length();
                if (count > 0 && drawStr.charAt(count - 1) == '\n') {
                    StringBuffer buffer = new StringBuffer(drawStr);
                    buffer.deleteCharAt(buffer.length() - 1);
                    drawStr = buffer.toString();
                }
                canvas.drawText(drawStr, mStartPointX, textBaseY, mTextPaint);
            }
        } else {
            if (mEditState && mInvalidate) {
                //draw cursor
                canvas.drawLine(mStartPointX + mCurrentLineWidth, mStartPointY + mFontHeight * mRealLine, mStartPointX
                        + mCurrentLineWidth, mStartPointY + mFontHeight * (mRealLine + 1), mCursorPaint);
            }
        }
    }

    public PointF getPageViewOrigin(PDFViewCtrl pdfViewCtrl, int pageIndex, float x, float y) {
        PointF pagePt = new PointF(x, y);
        pdfViewCtrl.convertPageViewPtToDisplayViewPt(pagePt, pagePt, pageIndex);
        RectF rect = new RectF(0, 0, pagePt.x, pagePt.y);
        pdfViewCtrl.convertDisplayViewRectToPageViewRect(rect, rect, pageIndex);
        PointF originPt = new PointF(x - rect.width(), y - rect.height());
        return originPt;
    }

    @SuppressLint("NewApi")
    private int getKeyboardHeight() {
        Rect r = new Rect();
        Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context != null) {
            View rootView = ((Activity)context).getWindow().getDecorView();
            rootView.getWindowVisibleDisplayFrame(r);
        }

        int height = AppDisplay.getInstance(mContext).getRawScreenHeight() - r.bottom;
        return height;
    }

    public Handler getBlink() {
        if (mBlink == null) {
            mBlink = new Blink();
        }
        return mBlink;
    }

    private class Blink extends Handler implements Runnable {
        @Override
        public void run() {
            mInvalidate = !mInvalidate;
            if (mPdfViewCtrl != null && mPdfViewCtrl.isPageVisible(mCurrentPageIndex)) {
                RectF rect = new RectF();
                if (mTextContent != null && !mTextContent.equals("")) {
                    if (mEditPointX != 0 || mEditPointY != 0) {
                        rect.set(mEditPointX - mSpace, mEditPointY - mSpace, mEditPointX + mSpace, mEditPointY
                                + mFontHeight + mSpace);
                    } else {
                        rect.set(mStartPointX - mSpace + mCurrentLineWidth, mStartPointY + mFontHeight
                                * (mRealLine - 1) - mSpace, mStartPointX + mCurrentLineWidth + mSpace, mStartPointY
                                + mFontHeight * mRealLine + mSpace);
                    }
                } else {
                    rect.set(mStartPointX - mSpace + mCurrentLineWidth,
                            mStartPointY + mFontHeight * mRealLine - mSpace, mStartPointX + mCurrentLineWidth + mSpace,
                            mStartPointY + mFontHeight * (mRealLine + 1) + mSpace);
                }
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rect, rect, mCurrentPageIndex);
                mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rect));
            }
            postDelayed(Blink.this, 500);
        }

    }

    private void setPaintFont(Paint paint, String fontname) {
        if (fontname.equals("Courier")) {
            paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        } else if (fontname.equals("Helvetica")) {
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        } else if (fontname.equals("Times")) {
            paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
        } else {
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        }
    }

    public float getFontHeight(PDFViewCtrl pdfViewCtrl, int pageIndex, String fontName, float fontSize) {
        setPaintFont(mTextPaint, fontName);
        mTextPaint.setTextSize(PdfSize2PageViewSize(pdfViewCtrl, pageIndex, fontSize));
        mFontMetrics = mTextPaint.getFontMetrics();
        float fontHeight = (int) (Math.ceil(mFontMetrics.descent - mFontMetrics.ascent));
        return fontHeight;
    }

    public float getFontWidth(PDFViewCtrl pdfViewCtrl, int pageIndex, String fontName, float fontSize) {
        return getFontHeight(pdfViewCtrl, pageIndex, fontName, fontSize);
    }

    private float PdfSize2PageViewSize(PDFViewCtrl pdfViewCtrl, int pageIndex, float PdfFontSize) {
        RectF rectF = new RectF(0, 0, PdfFontSize, PdfFontSize);
        pdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
        return rectF.width();
    }

    public void setKeyboardOffset(int keyboardOffset) {
        mPageOffset = keyboardOffset;
    }

    public float getKeyboardOffset() {
        return mPageOffset;
    }

    /**
     * Filter emoji character.
     */
    public static String filterEmoji(String source) {

        if (!containsEmoji(source)) {
            return source;
        }

        StringBuilder buf = null;
        int len = source.length();
        for (int i = 0; i < len; i++) {
            int codePoint = source.codePointAt(i);
            if (!isEmojiCharacter(codePoint)) {
                if (buf == null) {
                    buf = new StringBuilder(source.length());
                }
                buf.append(source.charAt(i));
            }
        }

        if (buf == null) {
            return "";
        } else {
            if (buf.length() == len) {
                buf = null;
                return source;
            } else {
                return buf.toString();
            }
        }

    }

    /**
     * Checking whether the string contains emoji character.
     */
    private static boolean containsEmoji(String source) {
        int len = source.length();
        for (int i = 0; i < len; i++) {
            int codePoint = source.codePointAt(i);
            if (isEmojiCharacter(codePoint)) {
                return true;
            }
        }
        return false;

    }

    /**
     * Checking whether the code point is emoji character.
     */
    private static boolean isEmojiCharacter(int codePoint) {
        return (codePoint == 0x0) || (codePoint == 0x9)
                || (codePoint == 0xa9) || (codePoint == 0xae) || (codePoint == 0x303d)
                || (codePoint == 0x3030) || (codePoint == 0x2b55) || (codePoint == 0x2b1c)
                || (codePoint == 0x2b1b) || (codePoint == 0x2b50)
                || ((codePoint >= 0x1F0CF) && (codePoint <= 0x1F6B8))
                || (codePoint == 0xD) || (codePoint == 0xDE0D)
                || ((codePoint >= 0x2100) && (codePoint <= 0x27FF))
                || ((codePoint >= 0x2B05) && (codePoint <= 0x2B07))
                || ((codePoint >= 0x2934) && (codePoint <= 0x2935))
                || ((codePoint >= 0x203C) && (codePoint <= 0x2049))
                || ((codePoint >= 0x3297) && (codePoint <= 0x3299))
                || ((codePoint >= 0x1F600) && (codePoint <= 0x1F64F))
                || ((codePoint >= 0xDC00) && (codePoint <= 0xE678));

    }
}