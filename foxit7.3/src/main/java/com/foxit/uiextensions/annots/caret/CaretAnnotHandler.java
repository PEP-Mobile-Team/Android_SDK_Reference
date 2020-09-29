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
package com.foxit.uiextensions.annots.caret;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.DateTime;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Caret;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.QuadPointsArray;
import com.foxit.sdk.pdf.annots.StrikeOut;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.annots.textmarkup.TextMarkupContent;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

public class CaretAnnotHandler implements AnnotHandler {
    private final Context mContext;
    private PDFViewCtrl mPdfViewCtrl;

    private Paint mPaint;
    private Paint mBorderPaint;

    private AnnotMenu mAnnotationMenu;
    private ArrayList<Integer> mMenuItems;
    private PropertyBar mAnnotationProperty;
    private PropertyBar.PropertyChangeListener mPropertyChangeListener;

    private int mBBoxSpace;
    private RectF mDocViewerRectF;
    private Annot mCurrentAnnot;
    private boolean mIsEditProperty;
    private boolean mIsModify;

    private int mTempLastColor;
    private int mTempLastOpacity;
    private String mTempLastContent;

    private CaretToolHandler mIST_ToolHandler;
    private CaretToolHandler mRPL_ToolHandler;

    public CaretAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mBorderPaint = new Paint();
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        AppAnnotUtil annotUtil = new AppAnnotUtil(mContext);
        mBorderPaint.setPathEffect(AppAnnotUtil.getAnnotBBoxPathEffect());
        mBorderPaint.setStrokeWidth(annotUtil.getAnnotBBoxStrokeWidth());
        mMenuItems = new ArrayList<Integer>();
        mCurrentAnnot = null;
        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mDocViewerRectF = new RectF(0, 0, 0, 0);
    }

    protected ToolHandler getToolHandler(String intent) {
        if (intent != null && "Replace".equals(intent)) {
            return mRPL_ToolHandler;
        } else {
            return mIST_ToolHandler;
        }
    }

    protected void setToolHandler(String intent, CaretToolHandler handler) {
        if (intent != null && "Replace".equals(intent)) {
            mRPL_ToolHandler = handler;
        } else {
            mIST_ToolHandler = handler;
        }
    }

    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotationMenu = annotMenu;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotationMenu;
    }

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    public void removePropertyBarListener() {
        mPropertyChangeListener = null;
    }

    public void setPropertyBar(PropertyBar propertyBar) {
        mAnnotationProperty = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mAnnotationProperty;
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        try {
            PointF pageViewPt = new PointF(motionEvent.getX(), motionEvent.getY());
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(pageViewPt, pageViewPt, pageIndex);

            Annot caret = getCaretAnnot(annot);
            if (caret == null || caret.isEmpty()) {
                return false;
            }

            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (caret == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                        if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt)) {
                            return true;
                        }

                    }
                    return false;
                default:
                    return false;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;

    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {

        try {
            PointF pageViewPt = new PointF(motionEvent.getX(), motionEvent.getY());
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(pageViewPt, pageViewPt, pageIndex);
            Annot caret = getCaretAnnot(annot);
            if (caret == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt)) {
                    return true;
                } else {
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    return true;
                }
            } else {
                if (!AppAnnotUtil.isReplaceCaret(caret)) {
                    if (mIST_ToolHandler == null){
                       return false;
                    }
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(annot);
                } else {
                    if (mRPL_ToolHandler == null){
                        return false;
                    }
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(caret);
                }
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        try {
            PointF pageViewPt = new PointF(motionEvent.getX(), motionEvent.getY());
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(pageViewPt, pageViewPt, pageIndex);

            Annot caret = getCaretAnnot(annot);
            if (caret == null || caret.isEmpty()) {
                return false;
            }
            if (AppUtil.isFastDoubleClick()) {
                return true;
            }
            if (caret == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                return pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt);
            } else {
                mTempLastColor = (int) caret.getBorderColor();
                mTempLastOpacity = (int) (((Markup) caret).getOpacity() * 255f + 0.5f);
                mTempLastContent = caret.getContent();
                if (AppAnnotUtil.isReplaceCaret(annot)) {
                    if(mRPL_ToolHandler == null){
                        return false;
                    }
                } else {
                    if(mIST_ToolHandler == null){
                       return false;
                    }
                }
                showDialog(caret);
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;

    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        return true;
    }

    @Override
    public int getType() {
        return Annot.e_Caret;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF pageViewPt) {
        RectF rectF = getAnnotBBox(annot);
        try {
            int pageIndex = annot.getPage().getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
            rectF.inset(-10, 10);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return rectF.contains(pageViewPt.x, pageViewPt.y);
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (content.getIntent() != null && "Replace".equals(content.getIntent())) {
            if (mRPL_ToolHandler != null) {
                mRPL_ToolHandler.addAnnot(pageIndex, (CaretAnnotContent) content, addUndo, result);
            } else {
                if (result != null) {
                    result.result(null, false);
                }
            }
        } else {
            if (mIST_ToolHandler != null) {
                mIST_ToolHandler.addAnnot(pageIndex, (CaretAnnotContent) content, addUndo, result);
            } else {
                if (result != null) {
                    result.result(null, false);
                }
            }
        }

    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
        try {
            int pageIndex = annot.getPage().getIndex();
            delAnnot(pageIndex, annot, addUndo, result);

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void delAnnot(final int pageIndex, final Annot annot, final boolean addUndo, final Event.Callback result) {
        // step 1 : set current annot to null
        if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
        }
        try {
            final PDFPage dmPage = annot.getPage();
            if (dmPage == null || dmPage.isEmpty()) {
                if (result != null) {
                    result.result(null, false);
                }
                return;
            }

            final CaretDeleteUndoItem undoItem = new CaretDeleteUndoItem(mPdfViewCtrl);
            undoItem.mPageIndex = pageIndex;
            undoItem.mNM = AppAnnotUtil.getAnnotUniqueID(annot);
            undoItem.mColor = annot.getBorderColor();
            undoItem.mOpacity = ((Caret)annot).getOpacity();
            undoItem.mBBox = AppUtil.toRectF(annot.getRect());
            undoItem.mAuthor = ((Caret) annot).getTitle();
            undoItem.mContents = annot.getContent();
            undoItem.mModifiedDate = annot.getModifiedDateTime();
            undoItem.mCreationDate = ((Caret) annot).getCreationDateTime();
            undoItem.mFlags = annot.getFlags();
            undoItem.mSubject = ((Caret) annot).getSubject();
            undoItem.mIntent = ((Caret) annot).getIntent();
            if (annot.getDict().getElement("Rotate") != null) {
                undoItem.mRotate = (360 - annot.getDict().getElement("Rotate").getInteger()) / 90;
            } else {
                undoItem.mRotate = 0;
            }

            undoItem.mIsReplace = AppAnnotUtil.isReplaceCaret(annot);
            if (undoItem.mIsReplace) {
                final StrikeOut strikeout = AppAnnotUtil.getStrikeOutFromCaret((Caret) annot);
                if (strikeout != null) {
                    final ArrayList<PointF> quadPoints = new ArrayList<PointF>();
                    final RectF bbox = AppUtil.toRectF(strikeout.getRect());
                    final int color = (int) strikeout.getBorderColor();
                    final int opacity = (int) (strikeout.getOpacity() * 255f + 0.5f);
                    final float linewidth = strikeout.getBorderInfo().getWidth();
                    final String subject = strikeout.getSubject();
                    final String content = strikeout.getContent();
                    final String nm = AppAnnotUtil.getAnnotUniqueID(strikeout);
                    final DateTime dateTime = strikeout.getModifiedDateTime();
                    try {
                        QuadPointsArray quadPointsArray = strikeout.getQuadPoints();
                        long count = quadPointsArray.getSize();
                        for (int i = 0; i < count; i ++) {
                            PointF pointF1 = new PointF();
                            pointF1.set(AppUtil.toPointF(quadPointsArray.getAt(i).getFirst()));
                            PointF pointF2 = new PointF();
                            pointF2.set(AppUtil.toPointF(quadPointsArray.getAt(i).getSecond()));
                            PointF pointF3 = new PointF();
                            pointF3.set(AppUtil.toPointF(quadPointsArray.getAt(i).getThird()));
                            PointF pointF4 = new PointF();
                            pointF4.set(AppUtil.toPointF(quadPointsArray.getAt(i).getFourth()));

                            quadPoints.add(pointF1);
                            quadPoints.add(pointF2);
                            quadPoints.add(pointF3);
                            quadPoints.add(pointF4);
                        }
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                    undoItem.mTMContent = new TextMarkupContent() {
                        @Override
                        public ArrayList<PointF> getQuadPoints() {
                            return quadPoints;
                        }

                        @Override
                        public int getPageIndex() {
                            return pageIndex;
                        }

                        @Override
                        public int getType() {
                            return Annot.e_StrikeOut;
                        }

                        @Override
                        public String getNM() {
                            return nm;
                        }

                        @Override
                        public RectF getBBox() {
                            return bbox;
                        }

                        @Override
                        public int getColor() {
                            return color;
                        }

                        @Override
                        public int getOpacity() {
                            return opacity;
                        }

                        @Override
                        public float getLineWidth() {
                            return linewidth;
                        }

                        @Override
                        public String getSubject() {
                            return subject != null ? subject : "Replace";
                        }

                        @Override
                        public DateTime getModifiedDate() {
                            return dateTime;
                        }

                        @Override
                        public String getContents() {
                            return content;
                        }

                        @Override
                        public String getIntent() {
                            return "StrikeOutTextEdit";
                        }
                    };

                }
            }

            deleteAnnot(annot, undoItem, addUndo, result);

        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    protected boolean deleteAnnot(final Annot annot, final CaretDeleteUndoItem undoItem, final boolean addUndo, final Event.Callback result) {
        try {
            final PDFPage page = annot.getPage();
            final int pageIndex = page.getIndex();
            final RectF viewRect = AppUtil.toRectF(annot.getRect());
            boolean bReplace = AppAnnotUtil.isReplaceCaret(annot);
            StrikeOut strikeOut = AppAnnotUtil.getStrikeOutFromCaret((Caret) annot);
            if (bReplace && strikeOut != null) {
                RectF sto_Rect = AppUtil.toRectF(strikeOut.getRect());
                sto_Rect.union(viewRect);

                viewRect.set(sto_Rect);
            }
            final DocumentManager documentManager = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager();
            documentManager.onAnnotWillDelete(page, annot);
            CaretEvent event = new CaretEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Caret) annot, mPdfViewCtrl);
            if (documentManager.isMultipleSelectAnnots()) {
                if (result != null) {
                    result.result(event, true);
                }
                return true;
            }

            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        documentManager.onAnnotDeleted(page, annot);
                        if (addUndo) {
                            documentManager.addUndoItem(undoItem);
                        }
                        // step 3 refresh view
                        mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                        mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                    }
                    if (result != null) {
                        result.result(null, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (content == null) {
            if (result != null) {
                result.result(null, false);
            }
            return;
        }

        try {
            CaretModifyUndoItem undoItem = new CaretModifyUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(content);
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();

            undoItem.mLastColor = annot.getBorderColor();
            undoItem.mLastOpacity = ((Caret) annot).getOpacity();
            undoItem.mLastContent = annot.getContent();

            modifyAnnot(annot.getPage().getIndex(), annot, undoItem, true, addUndo, "", result);
        } catch (PDFException e) {
            e.getLastError();
        }

    }

    private void modifyAnnot(final int pageIndex, final Annot annot, final int color, final int opacity, String content, boolean modifyJni, boolean addUndo) {

        CaretModifyUndoItem undoItem = new CaretModifyUndoItem(mPdfViewCtrl);
        undoItem.mPageIndex = pageIndex;
        undoItem.setCurrentValue(annot);
        undoItem.mColor = color;
        undoItem.mOpacity = opacity / 255f;
        if (content == null) {
            content = "";
        }
        undoItem.mContents = content;
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();

        undoItem.mLastColor = mTempLastColor;
        undoItem.mLastOpacity = mTempLastOpacity / 255f;
        undoItem.mLastContent = mTempLastContent;

        modifyAnnot(pageIndex, annot, undoItem, modifyJni, addUndo, Module.MODULE_NAME_CARET, null);

        if (AppAnnotUtil.isReplaceCaret(annot)) {
            final StrikeOut subAnnot = AppAnnotUtil.getStrikeOutFromCaret((Caret) annot);
            if (subAnnot == null) return;
            AnnotHandler annotHandler = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getAnnotHandlerByType(Annot.e_StrikeOut);

            AnnotContent annotContent = new AnnotContent() {
                @Override
                public int getPageIndex() {
                    return pageIndex;
                }

                @Override
                public int getType() {
                    return Annot.e_StrikeOut;
                }

                @Override
                public String getNM() {
                    return AppAnnotUtil.getAnnotUniqueID(subAnnot);
                }

                @Override
                public RectF getBBox() {
                    try {
                        return AppUtil.toRectF(subAnnot.getRect());
                    } catch (PDFException e) {
                    }
                    return null;
                }

                @Override
                public int getColor() {
                    return color;
                }

                @Override
                public int getOpacity() {
                    return opacity;
                }

                @Override
                public float getLineWidth() {
                    try {
                        return subAnnot.getBorderInfo().getWidth();
                    } catch (PDFException e) {
                    }
                    return 0;
                }

                @Override
                public String getSubject() {
                    try {
                        return subAnnot.getSubject();
                    } catch (PDFException e) {
                    }
                    return null;
                }

                @Override
                public DateTime getModifiedDate() {
                    try {
                        return annot.getModifiedDateTime();
                    } catch (PDFException e) {
                    }
                    return null;
                }

                @Override
                public String getContents() {
                    try {
                        return annot.getContent();
                    } catch (PDFException e) {
                    }
                    return null;
                }


                @Override
                public String getIntent() {
                    try {
                        return subAnnot.getIntent();
                    } catch (PDFException e) {
                    }
                    return null;
                }

            };
            annotHandler.modifyAnnot(subAnnot, annotContent, false, null);
        }

    }


    private void modifyAnnot(final int pageIndex, final Annot annot, final CaretModifyUndoItem undoItem, final boolean isModifyJni, final boolean addUndo,
                             final String fromType, final Event.Callback result) {
        try {
            final PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);

            if (isModifyJni) {
                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(true);
                CaretEvent event = new CaretEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Caret) annot, mPdfViewCtrl);
                EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            if (addUndo) {
                                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                            }
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(false);
                            try {
                                RectF tempRectF = AppUtil.toRectF(annot.getRect());
                                if ("".equals(fromType)) {
                                    mIsModify = true;
                                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(page, annot);

                                }
                                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                                    RectF viewRect = AppUtil.toRectF(annot.getRect());
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                                    viewRect.union(tempRectF);
                                    viewRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3, -AppAnnotUtil.getAnnotBBoxSpace() - 3);
                                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                                }
                            } catch (PDFException e) {
                                e.printStackTrace();
                            }
                        }
                        if (result != null) {
                            result.result(null, success);
                        }
                    }
                });
                mPdfViewCtrl.addTask(task);
            }

            if (!"".equals(fromType)) {

                ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(page, annot);
                mIsModify = true;
                if (!isModifyJni) {
                    Caret caret = (Caret) annot;
                    RectF tempRectF = AppUtil.toRectF(caret.getRect());
                    caret.setBorderColor(undoItem.mColor);
                    caret.setOpacity(undoItem.mOpacity);
                    caret.setContent(undoItem.mContents);
                    caret.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
                    caret.resetAppearanceStream();
                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        RectF viewRect = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                        viewRect.union(tempRectF);
                        viewRect.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 3, -AppAnnotUtil.getAnnotBBoxSpace() - 3);
                        mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
        mAnnotationMenu.dismiss();
        if (mIsEditProperty) {
            mIsEditProperty = false;
        }
        mAnnotationProperty.dismiss();

        try {
            int pageIndex = annot.getPage().getIndex();
            RectF pdfRect = AppUtil.toRectF(annot.getRect());
            RectF viewRect = new RectF(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
            if (mIsModify && reRender) {
                if (mTempLastColor == annot.getBorderColor() && mTempLastOpacity == (int) ((Markup) annot).getOpacity() * 255f) {
                    modifyAnnot(pageIndex, annot, annot.getBorderColor(), (int) (((Markup) annot).getOpacity() * 255f + 0.5f), annot.getContent(), false, true);
                } else {
                    modifyAnnot(pageIndex, annot, annot.getBorderColor(), (int) (((Markup) annot).getOpacity() * 255f + 0.5f), annot.getContent(), true, true);
                }
            } else if (mIsModify) {
                annot.setBorderColor(mTempLastColor);
                ((Markup) annot).setOpacity(mTempLastOpacity / 255f);
                annot.setContent(mTempLastContent);
            }
            mIsModify = false;
            if (mPdfViewCtrl.isPageVisible(pageIndex) && reRender) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);

                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                mCurrentAnnot = null;
                return;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

        mCurrentAnnot = null;
    }

    @Override
    public void onAnnotSelected(final Annot annot, boolean reRender) {
        final Caret caret = (Caret) annot;
        try {
            final int pageIndex = annot.getPage().getIndex();
            mTempLastColor = caret.getBorderColor();
            mTempLastOpacity = (int) (caret.getOpacity() * 255f + 0.5f);
            mTempLastContent = caret.getContent();

            mCurrentAnnot = annot;
            mAnnotationMenu.dismiss();
            prepareAnnotMenu(caret);
            RectF caretRectF = AppUtil.toRectF(mCurrentAnnot.getRect());
            mPdfViewCtrl.convertPdfRectToPageViewRect(caretRectF, caretRectF, pageIndex);
            if (AppAnnotUtil.isReplaceCaret(annot)) {
                Annot strikeout = AppAnnotUtil.getStrikeOutFromCaret((Caret) annot);
                if (strikeout == null) return;
                RectF strikeoutRectF = AppUtil.toRectF(strikeout.getRect());
                mPdfViewCtrl.convertPdfRectToPageViewRect(strikeoutRectF, strikeoutRectF, pageIndex);
                caretRectF.union(strikeoutRectF);
            }

            // change modify status
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                RectF rectF = new RectF(caretRectF);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(caretRectF, caretRectF, pageIndex);
                mAnnotationMenu.show(caretRectF);

                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(rectF));
                if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    mCurrentAnnot = annot;
                }

            } else {
                mCurrentAnnot = annot;
            }
            mIsModify = false;
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void prepareAnnotMenu(final Annot caret) {
        mMenuItems.clear();

        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
            mMenuItems.add(AnnotMenu.AM_BT_STYLE);
            mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
            mMenuItems.add(AnnotMenu.AM_BT_REPLY);
            mMenuItems.add(AnnotMenu.AM_BT_FLATTEN);
            if (!(AppAnnotUtil.isLocked(caret) || AppAnnotUtil.isReadOnly(caret))) {
                mMenuItems.add(AnnotMenu.AM_BT_DELETE);
            }
        } else {
            mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
        }

        mAnnotationMenu.setMenuItems(mMenuItems);

        mAnnotationMenu.setListener(new AnnotMenu.ClickListener() {
            @Override
            public void onAMClick(int btType) {
                try {

                    int pageIndex = caret.getPage().getIndex();
                    if (btType == AnnotMenu.AM_BT_COMMENT) {
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        UIAnnotReply.showComments(mPdfViewCtrl, ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), caret);
                    } else if (btType == AnnotMenu.AM_BT_REPLY) {
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        UIAnnotReply.replyToAnnot(mPdfViewCtrl, ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), caret);
                    } else if (btType == AnnotMenu.AM_BT_DELETE) {
                        delAnnot(pageIndex, caret, true, null);
                    } else if (btType == AnnotMenu.AM_BT_STYLE) {
                        mAnnotationMenu.dismiss();
                        mIsEditProperty = true;
                        mAnnotationProperty.setEditable(!(AppAnnotUtil.isLocked(caret) || AppAnnotUtil.isReadOnly(caret)));
                        int[] colors = new int[PropertyBar.PB_COLORS_CARET.length];
                        System.arraycopy(PropertyBar.PB_COLORS_CARET, 0, colors, 0, colors.length);
                        colors[0] = PropertyBar.PB_COLORS_CARET[0];
                        mAnnotationProperty.setColors(colors);

                        try {
                            mAnnotationProperty.setProperty(PropertyBar.PROPERTY_COLOR, (int) ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot().getBorderColor());
                            int opacity = AppDmUtil.opacity255To100((int) (((Markup) ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()).getOpacity() * 255f + 0.5f));
                            mAnnotationProperty.setProperty(PropertyBar.PROPERTY_OPACITY, opacity);
                            mAnnotationProperty.reset(PropertyBar.PROPERTY_COLOR | PropertyBar.PROPERTY_OPACITY);
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                        RectF annotRectF = new RectF();

                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(AppUtil.toRectF(caret.getRect()), annotRectF, pageIndex);
                            if (AppAnnotUtil.isReplaceCaret(caret)) {
                                StrikeOut strikeout = AppAnnotUtil.getStrikeOutFromCaret((Caret) caret);
                                if (strikeout == null) return;
                                RectF strikeoutRect = AppUtil.toRectF(strikeout.getRect());
                                mPdfViewCtrl.convertPdfRectToPageViewRect(strikeoutRect, strikeoutRect, pageIndex);
                                annotRectF.union(strikeoutRect);
                            }
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(annotRectF, annotRectF, pageIndex);
                        }
                        RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), annotRectF);
                        mAnnotationProperty.show(rectF, false);
                        mAnnotationProperty.setPropertyChangeListener(mPropertyChangeListener);
                    } else if (AnnotMenu.AM_BT_FLATTEN == btType) {
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        UIAnnotFlatten.flattenAnnot(mPdfViewCtrl, caret);
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        try {
            Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
            if (!(annot instanceof Caret)) {
                return;
            }
            if (!mPdfViewCtrl.isPageVisible(pageIndex)) return;
            if (AppAnnotUtil.equals(mCurrentAnnot, annot) && annot.getPage().getIndex() == pageIndex) {
                canvas.save();
                RectF bBoxRectF = AppUtil.toRectF(annot.getRect());
                RectF bInnerRect = AppUtil.toRectF(((Caret) annot).getInnerRect());
                float lineWidth = (bBoxRectF.right - bBoxRectF.left) / 5;

                mPdfViewCtrl.convertPdfRectToPageViewRect(bBoxRectF, bBoxRectF, pageIndex);
                mPdfViewCtrl.convertPdfRectToPageViewRect(bInnerRect, bInnerRect, pageIndex);
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(LineWidth2PageView(pageIndex, lineWidth));

                mPaint.setColor(AppDmUtil.calColorByMultiply((int) annot.getBorderColor(), (int) (((Markup) annot).getOpacity() * 255f + 0.5f)));

                int color = (int) annot.getBorderColor();
                mBorderPaint.setColor(color | 0xFF000000);
                RectF borderRectF = new RectF();
                borderRectF.set(bBoxRectF.left - mBBoxSpace, bBoxRectF.top - mBBoxSpace, bBoxRectF.right + mBBoxSpace, bBoxRectF.bottom + mBBoxSpace);
                canvas.drawRect(borderRectF, mBorderPaint);
                if (AppAnnotUtil.isReplaceCaret(annot)) {
                    Annot strikeout = AppAnnotUtil.getStrikeOutFromCaret((Caret) annot);
                    if (strikeout == null) return;
                    RectF subRectF = AppUtil.toRectF(strikeout.getRect());
                    RectF parentRectF = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(subRectF, subRectF, pageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(parentRectF, parentRectF, pageIndex);
                    subRectF.union(parentRectF);

                    borderRectF.set(subRectF.left - mBBoxSpace, subRectF.top - mBBoxSpace, subRectF.right + mBBoxSpace, subRectF.bottom + mBBoxSpace);
                    canvas.drawRect(borderRectF, mBorderPaint);
                }
                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (!(curAnnot instanceof Caret)) return;
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() != this)
            return;
        try {
            int pageIndex = curAnnot.getPage().getIndex();
            boolean isReplace = AppAnnotUtil.isReplaceCaret(curAnnot);
            if (curAnnot.getType() == Annot.e_Caret) {
                RectF caretRect = new RectF();
                RectF strikeoutRect = new RectF();
                if (!isReplace) {
                    caretRect.set(AppUtil.toRectF(curAnnot.getRect()));
                } else {
                    StrikeOut strikeOut = AppAnnotUtil.getStrikeOutFromCaret((Caret) curAnnot);
                    if (strikeOut == null) return;
                    strikeoutRect.set(AppUtil.toRectF(strikeOut.getRect()));
                    caretRect.set(AppUtil.toRectF(curAnnot.getRect()));
                }

                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    mPdfViewCtrl.convertPdfRectToPageViewRect(caretRect, caretRect, pageIndex);

                    if (isReplace) {
                        mPdfViewCtrl.convertPdfRectToPageViewRect(strikeoutRect, strikeoutRect, pageIndex);
                        caretRect.union(strikeoutRect);
                    }
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(caretRect, caretRect, pageIndex);
                    mAnnotationMenu.update(caretRect);
                    mDocViewerRectF.set(caretRect);
                    if (mIsEditProperty) {
                        RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mDocViewerRectF);
                        mAnnotationProperty.update(rectF);
                    }
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }


    @Override
    public RectF getAnnotBBox(Annot annot) {
        try {
            if (annot != null) {
                return AppUtil.toRectF(annot.getRect());
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Annot getCaretAnnot(Annot annot) {
        try {
            if (annot.getType() == Annot.e_Caret) {
                return annot;
            }
            Annot caret = null;
            if (annot.getType() == Annot.e_StrikeOut)
                caret = AppAnnotUtil.createAnnot(((Markup) annot).getGroupHeader(), Annot.e_StrikeOut);
            return caret;
        } catch (PDFException e) {
            e.printStackTrace();
            return null;
        }
    }

    private float LineWidth2PageView(int pageIndex, float lineWidth) {
        RectF rectF = new RectF(0, 0, lineWidth, lineWidth);
        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
        return Math.abs(rectF.width());
    }

    public void onColorValueChanged(final int color) {
        final Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null) {
            return;
        }
        try {
            if (color != annot.getBorderColor()) {
                modifyAnnot(annot.getPage().getIndex(), annot, color, (int) (((Markup) annot).getOpacity() * 255f + 0.5f), annot.getContent(), false, true);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onOpacityValueChanged(final int opacity) {
        Annot annot = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        try {
            if (annot != null && AppDmUtil.opacity100To255(opacity) != (int) (((Markup) annot).getOpacity() * 255f)) {
                modifyAnnot(annot.getPage().getIndex(), annot, (int) annot.getBorderColor(), AppDmUtil.opacity100To255(opacity), annot.getContent(), false, true);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void showDialog(final Annot annot) {
        if (mPdfViewCtrl.getUIExtensionsManager() == null) return;
        Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return;
        final View contentView = View.inflate(mContext, R.layout.rd_note_dialog_edit, null);
        final TextView contentTitle = (TextView) contentView.findViewById(R.id.rd_note_dialog_edit_title);
        final EditText contentEditText = (EditText) contentView.findViewById(R.id.rd_note_dialog_edit);
        final Button cancelButton = (Button) contentView.findViewById(R.id.rd_note_dialog_edit_cancel);
        final Button applayButton = (Button) contentView.findViewById(R.id.rd_note_dialog_edit_ok);
        final Dialog contentDialog = new Dialog(context, R.style.rv_dialog_style);

        contentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentDialog.setContentView(contentView, new ViewGroup.LayoutParams(AppDisplay.getInstance(mContext).getUITextEditDialogWidth(), ViewGroup.LayoutParams.WRAP_CONTENT));
        contentEditText.setMaxLines(10);

        contentDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        contentDialog.getWindow().setBackgroundDrawableResource(R.drawable.dlg_title_bg_4circle_corner_white);

        if (AppAnnotUtil.isReplaceCaret(annot)) {
            contentTitle.setText(mContext.getApplicationContext().getString(R.string.fx_string_replacetext));
        } else {
            contentTitle.setText(mContext.getApplicationContext().getString(R.string.fx_string_inserttext));
        }

        contentEditText.setEnabled(true);
        try {
            String content = annot.getContent();
            if (content == null) {
                content = "";
            }
            contentEditText.setText(content);
            contentEditText.setSelection(content.length());
            applayButton.setEnabled(false);
            applayButton.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));

        } catch (PDFException e) {
            e.printStackTrace();
        }

        contentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                try {
                    if (!contentEditText.getText().toString().equals(annot.getContent())) {
                        applayButton.setEnabled(true);
                        applayButton.setTextColor(mContext.getResources().getColor(R.color.dlg_bt_text_selector));
                    } else {
                        applayButton.setEnabled(false);
                        applayButton.setTextColor(mContext.getResources().getColor(R.color.ux_bg_color_dialog_button_disabled));
                    }
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                          int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {

            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                contentDialog.dismiss();
            }
        });
        applayButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    int pageIndex = annot.getPage().getIndex();

                    if (!contentEditText.getText().toString().equals(annot.getContent())) {
                        modifyAnnot(pageIndex, annot, (int) annot.getBorderColor(), (int) (((Markup) annot).getOpacity() * 255f), contentEditText.getText().toString(), true, true);
                    }
                    contentDialog.dismiss();
                } catch (PDFException e) {
                    e.printStackTrace();
                }
            }
        });
        contentDialog.show();
        if (((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot() &&
                !(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot))) {
            AppUtil.showSoftInput(contentEditText);
        } else {
            contentEditText.setFocusable(false);
            contentEditText.setLongClickable(false);
            if (Build.VERSION.SDK_INT > 11) {
                contentEditText.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        return false;
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {

                    }
                });
            } else {
                contentEditText.setEnabled(false);
            }
        }
    }
}