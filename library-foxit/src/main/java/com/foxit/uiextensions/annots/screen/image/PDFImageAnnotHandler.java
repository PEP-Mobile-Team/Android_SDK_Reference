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
package com.foxit.uiextensions.annots.screen.image;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Screen;
import com.foxit.sdk.pdf.objects.PDFDictionary;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;

/*
     *   1-----------2
     *   |	         |
     *   |	         |
     *   |           |
     *   |           |
     *   |           |
     *   4-----------3
     *   */
public class PDFImageAnnotHandler implements AnnotHandler {

    private static final int CTR_NONE = -1;
    private static final int CTR_LT = 1;
    private static final int CTR_RT = 2;
    private static final int CTR_RB = 3;
    private static final int CTR_LB = 4;
    private int mCurrentCtr = CTR_NONE;

    private static final int OPER_DEFAULT = -1;
    private static final int OPER_SCALE_LT = 1;// old:start at 0
    private static final int OPER_SCALE_RT = 2;
    private static final int OPER_SCALE_RB = 3;
    private static final int OPER_SCALE_LB = 4;
    private static final int OPER_TRANSLATE = 5;
    private int mLastOper = OPER_DEFAULT;

    private float mCtlPtLineWidth = 2;
    private float mCtlPtRadius = 5;
    private float mCtlPtTouchExt = 20;
    private float mCtlPtDeltyXY = 20;// Additional refresh range

    private Paint mFrmPaint;// outline
    private Paint mCtlPtPaint;

    public Context mContext;
    public PDFViewCtrl mPdfViewCtrl;

    private boolean mTouchCaptured = false;
    private PointF mDownPoint;
    private PointF mLastPoint;

    private ArrayList<Integer> mMenuText;
    private AnnotMenu mAnnotMenu;

    private Annot mBitmapAnnot;

    private PropertyBar.PropertyChangeListener mPropertyChangeListener;
    private PropertyBar mPropertyBar;

    private int mTempLastRotation;
    private int mTempLastOpacity;
    private String mTempLastContent;
    private RectF mTempLastBBox = new RectF();
    private PDFDictionary mTempLastPDFDictionary;

    private boolean mIsEditProperty;
    private boolean mIsModify;

    private RectF mThicknessRectF = new RectF();
    private RectF mPageViewRect = new RectF(0, 0, 0, 0);
    private RectF mPageDrawRect = new RectF();
    private RectF mInvalidateRect = new RectF(0, 0, 0, 0);
    private RectF mAnnotMenuRect = new RectF(0, 0, 0, 0);
    private RectF mViewDrawRect = new RectF(0, 0, 0, 0);
    private RectF mDocViewerBBox = new RectF(0, 0, 0, 0);

    private float mThickness = 0f;

    public PDFImageAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        this.mContext = context;
        this.mPdfViewCtrl = pdfViewCtrl;

        mDownPoint = new PointF();
        mLastPoint = new PointF();

        PathEffect effect = AppAnnotUtil.getAnnotBBoxPathEffect();
        mFrmPaint = new Paint();
        mFrmPaint.setPathEffect(effect);
        mFrmPaint.setStyle(Paint.Style.STROKE);
        mFrmPaint.setAntiAlias(true);

        mCtlPtPaint = new Paint();

        mMenuText = new ArrayList<Integer>();

    }

    @Override
    public int getType() {
        return AnnotHandler.TYPE_SCREEN_IMAGE;
    }

    void setPropertyChangeListener(PropertyBar.PropertyChangeListener propertyChangeListener) {
        mPropertyChangeListener = propertyChangeListener;
    }

    void removePropertyBarListener() {
        mPropertyChangeListener = null;
    }

    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotMenu = annotMenu;
    }

    //热区
    public void drawActionRect(int pageIndex, Canvas canvas) {

    }

    protected AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    public void setPropertyBar(PropertyBar propertyBar) {
        mPropertyBar = propertyBar;
    }

    public PropertyBar getPropertyBar() {
        return mPropertyBar;
    }

    private float thicknessOnPageView(int pageIndex, float thickness) {
        mThicknessRectF.set(0, 0, thickness, thickness);
        mPdfViewCtrl.convertPdfRectToPageViewRect(mThicknessRectF, mThicknessRectF, pageIndex);
        return Math.abs(mThicknessRectF.width());
    }

    private void resetAnnotationMenuResource(Annot annot) {
        mMenuText.clear();

        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
            mMenuText.add(AnnotMenu.AM_BT_STYLE);
            mMenuText.add(AnnotMenu.AM_BT_FLATTEN);
            if (!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot))) {
                mMenuText.add(AnnotMenu.AM_BT_DELETE);
            }
        }
    }

    private void modifyAnnot(final int pageIndex, final Annot annot, RectF bbox, int opacity,
                             int rotation, String contents, boolean isModifyJni, final boolean addUndo,
                             final String fromType, final Event.Callback result) {
        try {
            final PDFImageModifyUndoItem undoItem = new PDFImageModifyUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = pageIndex;
            undoItem.mBBox = new RectF(bbox);
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mOpacity = opacity / 255f;
            undoItem.mContents = contents;
            undoItem.mRotation = rotation;
            undoItem.mPDFDictionary = ((Screen) annot).getMKDict();

            undoItem.mOldBBox = new RectF(mTempLastBBox);
            undoItem.mOldOpacity = mTempLastOpacity / 255f;
            undoItem.mOldContents = mTempLastContent;
            undoItem.mOldRotation = mTempLastRotation;
            undoItem.mOldPDFDictionary = mTempLastPDFDictionary;

            if (isModifyJni) {
                final RectF tempRectF = AppUtil.toRectF(annot.getRect());

                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(addUndo);
                PDFImageEvent event = new PDFImageEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Screen) annot, mPdfViewCtrl);

                EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (success) {
                            if (addUndo) {
                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                            }

                            if (fromType.equals("")) {
                                mIsModify = true;
                            }

                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(false);
                            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                                try {
                                    RectF annotRectF = AppUtil.toRectF(annot.getRect());
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                                    mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                                    annotRectF.union(tempRectF);
                                    annotRectF.inset(-AppAnnotUtil.getAnnotBBoxSpace() - 10, -AppAnnotUtil.getAnnotBBoxSpace() - 10);
                                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                                } catch (PDFException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        if (result != null) {
                            result.result(null, success);
                        }
                    }
                });
                mPdfViewCtrl.addTask(task);
            }
            if (!fromType.equals("")) {
                mIsModify = true;
                if (isModifyJni) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(annot.getPage(), annot);
                }

                if (!isModifyJni) {
                    Screen screenAnnot = (Screen) annot;
                    RectF tempRectF = AppUtil.toRectF(annot.getRect());

                    if (contents != null) {
                        screenAnnot.setContent(contents);
                    }
                    screenAnnot.setOpacity(opacity / 255f);
                    screenAnnot.setRotation(rotation);
                    screenAnnot.move(AppUtil.toFxRectF(bbox));
                    screenAnnot.setModifiedDateTime(AppDmUtil.currentDateToDocumentDate());
                    screenAnnot.resetAppearanceStream();

                    RectF annotRectF = AppUtil.toRectF(annot.getRect());

                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());

                        mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(tempRectF, tempRectF, pageIndex);
                        annotRectF.union(tempRectF);
                        annotRectF.inset(-thickness - mCtlPtRadius - mCtlPtDeltyXY, -thickness - mCtlPtRadius - mCtlPtDeltyXY);
                        mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                    }
                }
            }
        } catch (PDFException e) {
            if (e.getLastError() == Constants.e_ErrOutOfMemory) {
                mPdfViewCtrl.recoverForOOM();
            }
        }
    }

    private void deleteAnnot(final Annot annot, final boolean addUndo, final Event.Callback result) {
        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
        }

        try {
            final PDFPage page = annot.getPage();
            final RectF viewRect = AppUtil.toRectF(annot.getRect());
            final int pageIndex = page.getIndex();

            final PDFImageDeleteUndoItem undoItem = new PDFImageDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = pageIndex;

            undoItem.mPageIndex = pageIndex;
//            undoItem.mIntent = ((Screen) annot).getIntent();
            undoItem.mOpacity = ((Screen) annot).getOpacity();
            undoItem.mContents = annot.getContent();
//            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mBBox = AppUtil.toRectF(annot.getRect());
            undoItem.mAuthor = AppDmUtil.getAnnotAuthor();
            undoItem.mRotation = ((Screen) annot).getRotation();
            undoItem.mPDFDictionary = ((Screen) annot).getMKDict();

            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            PDFImageEvent event = new PDFImageEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Screen) annot, mPdfViewCtrl);
            if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
                if (result != null) {
                    result.result(event, true);
                }
                return;
            }
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (addUndo) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        }

                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
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

    protected void deleteAnnot(final Annot annot, final PDFImageDeleteUndoItem undoItem, final Event.Callback result) {
        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
        }

        try {
            final PDFPage page = annot.getPage();
            final int pageIndex = page.getIndex();
            final RectF annotRectF = AppUtil.toRectF(annot.getRect());
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            final PDFImageEvent deleteEvent = new PDFImageEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Screen) annot, mPdfViewCtrl);
            if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
                if (result != null) {
                    result.result(deleteEvent, true);
                }
                return;
            }
            EditAnnotTask task = new EditAnnotTask(deleteEvent, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            RectF deviceRectF = new RectF();
                            mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, deviceRectF, pageIndex);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(deviceRectF));
                        }
                    }
                    if (result != null) {
                        result.result(deleteEvent, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }

    @Override
    public RectF getAnnotBBox(Annot annot) {
        RectF rectF = null;
        try {
            rectF = AppUtil.toRectF(annot.getRect());
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return rectF;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        RectF bbox = getAnnotBBox(annot);
        if (bbox == null) return false;
        try {
            mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, annot.getPage().getIndex());
        } catch (PDFException e) {
            return false;
        }
        return bbox.contains(point.x, point.y);
    }

    @Override
    public void onAnnotSelected(Annot annot, boolean reRender) {
        mCtlPtRadius = AppDisplay.getInstance(mContext).dp2px(mCtlPtRadius);
        mCtlPtDeltyXY = AppDisplay.getInstance(mContext).dp2px(mCtlPtDeltyXY);

        try {
            mTempLastPDFDictionary = ((Screen) annot).getMKDict();
            mTempLastRotation = ((Screen) annot).getRotation();
            mTempLastOpacity = (int) (((Screen) annot).getOpacity() * 255f + 0.5f);
            mTempLastBBox = AppUtil.toRectF(annot.getRect());
            mTempLastContent = annot.getContent();
            if (mTempLastContent == null) {
                mTempLastContent = "";
            }
            RectF _rect = AppUtil.toRectF(annot.getRect());
            mPageViewRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
            prepareAnnotMenu(annot);
            RectF menuRect = new RectF(mPageViewRect);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(menuRect, menuRect, pageIndex);
            mAnnotMenu.show(menuRect);

            preparePropertyBar((Screen) annot);

            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(mPageViewRect));
                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    mBitmapAnnot = annot;
                }
            } else {
                mBitmapAnnot = annot;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean needInvalid) {
        mCtlPtRadius = 5;
        mCtlPtDeltyXY = 20;

        mAnnotMenu.setListener(null);
        mAnnotMenu.dismiss();
        if (mIsEditProperty) {
            mIsEditProperty = false;
            mPropertyBar.dismiss();
        }
        if (mPropertyBar.isShowing()) {
            mPropertyBar.dismiss();
        }

        PDFPage page = null;
        try {
            page = annot.getPage();

            if (needInvalid && mIsModify) {
                // must calculate BBox again
                if (mTempLastRotation == ((Screen) annot).getRotation()
                        && mTempLastBBox.equals(AppUtil.toRectF(annot.getRect()))
                        && mTempLastOpacity == (int) (((Screen) annot).getOpacity() * 255f)) {
                    modifyAnnot(page.getIndex(), annot, AppUtil.toRectF(annot.getRect()), (int) (((Screen) annot).getOpacity() * 255f),
                            ((Screen) annot).getRotation(), annot.getContent(), false, true,
                            PDFImageModule.MODULE_NAME_IMAGE, null);
                } else {
                    modifyAnnot(page.getIndex(), annot, AppUtil.toRectF(annot.getRect()), (int) (((Screen) annot).getOpacity() * 255f),
                            ((Screen) annot).getRotation(), annot.getContent(), true, true,
                            PDFImageModule.MODULE_NAME_IMAGE, null);
                }
            } else {
                ((Screen) annot).setRotation(mTempLastRotation);
                ((Screen) annot).setOpacity(mTempLastOpacity / 255f);
                annot.move(AppUtil.toFxRectF(mTempLastBBox));
                annot.resetAppearanceStream();
            }

            RectF pdfRect = AppUtil.toRectF(annot.getRect());
            RectF viewRect = new RectF(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);

            if (mPdfViewCtrl.isPageVisible(page.getIndex()) && needInvalid) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, page.getIndex());
                mPdfViewCtrl.refresh(page.getIndex(), AppDmUtil.rectFToRect(viewRect));
            }

            mBitmapAnnot = null;
            mIsModify = false;

        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAnnot(final int pageIndex, AnnotContent content, final boolean addUndo, final Event.Callback result) {
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        try {
            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();
            RectF bbox = AppUtil.toRectF(annot.getRect());
            int rotation = ((Screen) annot).getRotation();
            int opacity = (int) (((Screen) annot).getOpacity() * 255f);
            String contents = annot.getContent();

            if (content.getBBox() != null)
                bbox = content.getBBox();
            if (content.getOpacity() != 0)
                opacity = content.getOpacity();
            if (content.getContents() != null)
                contents = content.getContents();

            modifyAnnot(pageIndex, annot, bbox, opacity, rotation, contents, true, addUndo, "", result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
        deleteAnnot(annot, addUndo, result);
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e, Annot annot) {
        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
        float evX = point.x;
        float evY = point.y;

        int action = e.getAction();
        try {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()) {
                        mThickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
                        RectF pageViewBBox = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                        RectF pdfRect = AppUtil.toRectF(annot.getRect());
                        mPageViewRect.set(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
                        mPageViewRect.inset(mThickness / 2f, mThickness / 2f);

                        mCurrentCtr = isTouchControlPoint(pageViewBBox, evX, evY);

                        mDownPoint.set(evX, evY);
                        mLastPoint.set(evX, evY);

                        if (mCurrentCtr == CTR_LT) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_LT;
                            return true;
                        } else if (mCurrentCtr == CTR_RT) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_RT;
                            return true;
                        } else if (mCurrentCtr == CTR_RB) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_RB;
                            return true;
                        } else if (mCurrentCtr == CTR_LB) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_LB;
                            return true;
                        } else if (isHitAnnot(annot, point)) {
                            mTouchCaptured = true;
                            mLastOper = OPER_TRANSLATE;
                            return true;
                        }
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (pageIndex == annot.getPage().getIndex()
                            && mTouchCaptured
                            && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
                        if (evX != mLastPoint.x && evY != mLastPoint.y) {
                            RectF pageViewBBox = AppUtil.toRectF(annot.getRect());
                            mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                            float deltaXY = mCtlPtLineWidth + mCtlPtRadius * 2 + 2;// Judging border value
                            switch (mLastOper) {

                                case OPER_TRANSLATE: {
                                    mInvalidateRect.set(pageViewBBox);
                                    mAnnotMenuRect.set(pageViewBBox);
                                    mInvalidateRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    mAnnotMenuRect.offset(evX - mDownPoint.x, evY - mDownPoint.y);
                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                    mInvalidateRect.union(mAnnotMenuRect);

                                    mInvalidateRect.inset(-deltaXY - mCtlPtDeltyXY, -deltaXY - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotMenu.isShowing()) {
                                        mAnnotMenu.dismiss();
                                        mAnnotMenu.update(mAnnotMenuRect);
                                    }
                                    if (mIsEditProperty) {
                                        mPropertyBar.dismiss();
                                    }
                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    break;
                                }
                                case OPER_SCALE_LT: {

                                    float viewLeft = mPageViewRect.left;
                                    float viewTop = mPageViewRect.top;
                                    float viewRight = mPageViewRect.right;
                                    float viewBottom = mPageViewRect.bottom;

                                    float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                                    float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);
                                    float y = k * evX + b;

                                    float maxY = mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY;
                                    if (evX != mLastPoint.x && evY != mLastPoint.y && y > deltaXY && y < maxY) {
                                        mInvalidateRect.set(mLastPoint.x, mLastPoint.x * k + b, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(evX, evY, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        if (mIsEditProperty) {
                                            mPropertyBar.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RT: {
                                    float viewLeft = mPageViewRect.left;
                                    float viewTop = mPageViewRect.top;
                                    float viewRight = mPageViewRect.right;
                                    float viewBottom = mPageViewRect.bottom;

                                    float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                                    float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);
                                    float y = k * evX + b;

                                    float maxY = mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY;
                                    if (evX != mLastPoint.x && evY != mLastPoint.y && y > deltaXY && y< maxY) {
                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.x * k + b, mLastPoint.x, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, evY, evX, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        if (mIsEditProperty) {
                                            mPropertyBar.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RB: {
                                    float viewLeft = mPageViewRect.left;
                                    float viewTop = mPageViewRect.top;
                                    float viewRight = mPageViewRect.right;
                                    float viewBottom = mPageViewRect.bottom;

                                    float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                                    float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);
                                    float y = k * evX + b;

                                    if (evX != mLastPoint.x && evY != mLastPoint.y && (y + deltaXY) < mPdfViewCtrl.getPageViewHeight(pageIndex) && y > deltaXY) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mLastPoint.x * k + b);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, evX, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        if (mIsEditProperty) {
                                            mPropertyBar.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_LB: {
                                    float viewLeft = mPageViewRect.left;
                                    float viewTop = mPageViewRect.top;
                                    float viewRight = mPageViewRect.right;
                                    float viewBottom = mPageViewRect.bottom;

                                    float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                                    float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);
                                    float y = k * evX + b;

                                    if (evX != mLastPoint.x && evY != mLastPoint.y && (y + deltaXY) < mPdfViewCtrl.getPageViewHeight(pageIndex) && y > deltaXY) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mLastPoint.x * k + b);
                                        mAnnotMenuRect.set(evX, mPageViewRect.top, mPageViewRect.right, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        if (mIsEditProperty) {
                                            mPropertyBar.dismiss();
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_DEFAULT:
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
                                        if (mAnnotMenu.isShowing()) {
                                            mAnnotMenu.dismiss();
                                            mAnnotMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }


                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mTouchCaptured && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager()
                            .getCurrentAnnot() && pageIndex == annot.getPage().getIndex()) {
                        RectF pageViewRect = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                        pageViewRect.inset(mThickness / 2, mThickness / 2);

                        switch (mLastOper) {
                            case OPER_TRANSLATE: {
                                mPageDrawRect.set(pageViewRect);
                                mPageDrawRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                break;
                            }
                            case OPER_SCALE_LT: {
                                float viewLeft = mPageViewRect.left;
                                float viewTop = mPageViewRect.top;
                                float viewRight = mPageViewRect.right;
                                float viewBottom = mPageViewRect.bottom;

                                float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                                float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);

                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, k * mLastPoint.x + b, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_RT: {
                                float viewLeft = mPageViewRect.left;
                                float viewTop = mPageViewRect.top;
                                float viewRight = mPageViewRect.right;
                                float viewBottom = mPageViewRect.bottom;

                                float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                                float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);

                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, k * mLastPoint.x + b, mLastPoint.x, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_RB: {
                                float viewLeft = mPageViewRect.left;
                                float viewTop = mPageViewRect.top;
                                float viewRight = mPageViewRect.right;
                                float viewBottom = mPageViewRect.bottom;

                                float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                                float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);

                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, mLastPoint.x * k + b);
                                }
                                break;
                            }
                            case OPER_SCALE_LB: {
                                float viewLeft = mPageViewRect.left;
                                float viewTop = mPageViewRect.top;
                                float viewRight = mPageViewRect.right;
                                float viewBottom = mPageViewRect.bottom;

                                float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                                float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);

                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, mLastPoint.x * k + b);
                                }
                                break;
                            }
                            default:
                                break;
                        }
                        if (mLastOper != OPER_DEFAULT && !mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                            RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
                            RectF bboxRect = new RectF(viewDrawBox);
                            mPdfViewCtrl.convertPageViewRectToPdfRect(bboxRect, bboxRect, pageIndex);

                            modifyAnnot(pageIndex, annot, bboxRect, (int) (((Screen) annot).getOpacity() * 255f),
                                    ((Screen) annot).getRotation(), annot.getContent(), false, false,
                                    PDFImageModule.MODULE_NAME_IMAGE, null);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
                            if (!mIsEditProperty) {
                                if (mAnnotMenu.isShowing()) {
                                    mAnnotMenu.update(viewDrawBox);
                                } else {
                                    mAnnotMenu.show(viewDrawBox);
                                }
                            }
                        } else {
                            RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
                            float _lineWidth = annot.getBorderInfo().getWidth();
                            viewDrawBox.inset(-thicknessOnPageView(pageIndex, _lineWidth) / 2, -thicknessOnPageView(pageIndex, _lineWidth) / 2);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.update(viewDrawBox);
                            } else {
                                mAnnotMenu.show(viewDrawBox);
                            }
                        }

                        mTouchCaptured = false;
                        mDownPoint.set(0, 0);
                        mLastPoint.set(0, 0);
                        mLastOper = OPER_DEFAULT;
                        mCurrentCtr = CTR_NONE;
                        return true;
                    }

                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    mLastOper = OPER_DEFAULT;
                    mCurrentCtr = CTR_NONE;
                    mTouchCaptured = false;
                    return false;
                default:
            }
            return false;

        } catch (PDFException e1) {
            e1.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return onSingleTapOrLongPress(pageIndex, motionEvent, annot);
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        return true;
    }

    public boolean onSingleTapOrLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        try {
            PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);

            mThickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
            RectF _rect = AppUtil.toRectF(annot.getRect());
            mPageViewRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
            mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
            mPageViewRect.inset(mThickness / 2f, mThickness / 2f);
            if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {

                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, point)) {
                    return true;
                } else {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    return true;
                }
            } else {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(annot);
                return true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
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

    /*
     *   1-----------2
     *   |	         |
     *   |	         |
     *   |          |
     *   |           |
     *   |           |
     *   4-----------3
     *   */
    private RectF mMapBounds = new RectF();

    private PointF[] calculateControlPoints(RectF rect) {
        rect.sort();
        mMapBounds.set(rect);
        mMapBounds.inset(-mCtlPtRadius - mCtlPtLineWidth / 2f, -mCtlPtRadius - mCtlPtLineWidth / 2f);// control rect
        PointF p1 = new PointF(mMapBounds.left, mMapBounds.top);
        PointF p2 = new PointF(mMapBounds.right, mMapBounds.top);
        PointF p3 = new PointF(mMapBounds.right, mMapBounds.bottom);
        PointF p4 = new PointF(mMapBounds.left, mMapBounds.bottom);

        return new PointF[]{p1, p2, p3, p4};
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

    private RectF mBBoxInOnDraw = new RectF();
    private RectF mViewDrawRectInOnDraw = new RectF();
    private DrawFilter mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        drawActionRect(pageIndex, canvas);
        if (annot == null || !(annot instanceof Screen)) {
            return;
        }

        try {
            int annotPageIndex = annot.getPage().getIndex();
            if (AppAnnotUtil.equals(mBitmapAnnot, annot) && annotPageIndex == pageIndex) {
                canvas.save();
                canvas.setDrawFilter(mDrawFilter);
                RectF rect2 = AppUtil.toRectF(annot.getRect());
                float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
                mPdfViewCtrl.convertPdfRectToPageViewRect(rect2, rect2, pageIndex);
                rect2.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);

                RectF _rect = AppUtil.toRectF(annot.getRect());
                mViewDrawRectInOnDraw.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
                mPdfViewCtrl.convertPdfRectToPageViewRect(mViewDrawRectInOnDraw, mViewDrawRectInOnDraw, pageIndex);
                mViewDrawRectInOnDraw.inset(thickness / 2f, thickness / 2f);

                if (mLastOper == OPER_SCALE_LT) {// SCALE
                    float viewLeft = mViewDrawRectInOnDraw.left;
                    float viewTop = mViewDrawRectInOnDraw.top;
                    float viewRight = mViewDrawRectInOnDraw.right;
                    float viewBottom = mViewDrawRectInOnDraw.bottom;

                    float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                    float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);

                    mBBoxInOnDraw.set(mLastPoint.x, k * mLastPoint.x + b, mViewDrawRectInOnDraw.right, mViewDrawRectInOnDraw.bottom);
                } else if (mLastOper == OPER_SCALE_RT) {
                    float viewLeft = mViewDrawRectInOnDraw.left;
                    float viewTop = mViewDrawRectInOnDraw.top;
                    float viewRight = mViewDrawRectInOnDraw.right;
                    float viewBottom = mViewDrawRectInOnDraw.bottom;

                    float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                    float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);

                    mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mLastPoint.x * k + b, mLastPoint.x, mViewDrawRectInOnDraw.bottom);
                } else if (mLastOper == OPER_SCALE_RB) {
                    float viewLeft = mViewDrawRectInOnDraw.left;
                    float viewTop = mViewDrawRectInOnDraw.top;
                    float viewRight = mViewDrawRectInOnDraw.right;
                    float viewBottom = mViewDrawRectInOnDraw.bottom;

                    float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                    float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);

                    mBBoxInOnDraw.set(mViewDrawRectInOnDraw.left, mViewDrawRectInOnDraw.top, mLastPoint.x, mLastPoint.x * k + b);
                } else if (mLastOper == OPER_SCALE_LB) {
                    float viewLeft = mViewDrawRectInOnDraw.left;
                    float viewTop = mViewDrawRectInOnDraw.top;
                    float viewRight = mViewDrawRectInOnDraw.right;
                    float viewBottom = mViewDrawRectInOnDraw.bottom;

                    float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                    float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);

                    mBBoxInOnDraw.set(mLastPoint.x, mViewDrawRectInOnDraw.top, mViewDrawRectInOnDraw.right, mLastPoint.x * k + b);
                }
                mBBoxInOnDraw.inset(-thickness / 2f, -thickness / 2f);
                if (mLastOper == OPER_TRANSLATE || mLastOper == OPER_DEFAULT) {// TRANSLATE or DEFAULT
                    mBBoxInOnDraw = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mBBoxInOnDraw, mBBoxInOnDraw, pageIndex);
                    float dx = mLastPoint.x - mDownPoint.x;
                    float dy = mLastPoint.y - mDownPoint.y;

                    mBBoxInOnDraw.offset(dx, dy);
                }
                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    drawControlPoints(canvas, mBBoxInOnDraw);
                    // add Control Imaginary
                    drawControlImaginary(canvas, mBBoxInOnDraw);
                }

                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (curAnnot != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this) {

            try {
                int annotPageIndex = curAnnot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(annotPageIndex)) {
                    float thickness = thicknessOnPageView(annotPageIndex, curAnnot.getBorderInfo().getWidth());
                    RectF _rect = AppUtil.toRectF(curAnnot.getRect());
                    mViewDrawRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mViewDrawRect, mViewDrawRect, annotPageIndex);
                    mViewDrawRect.inset(thickness / 2f, thickness / 2f);

                    if (mLastOper == OPER_SCALE_LT) {
                        float viewLeft = mViewDrawRect.left;
                        float viewTop = mViewDrawRect.top;
                        float viewRight = mViewDrawRect.right;
                        float viewBottom = mViewDrawRect.bottom;

                        float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                        float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);

                        mDocViewerBBox.left = mLastPoint.x;
                        mDocViewerBBox.top = mLastPoint.x * k + b;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_RT) {
                        float viewLeft = mViewDrawRectInOnDraw.left;
                        float viewTop = mViewDrawRectInOnDraw.top;
                        float viewRight = mViewDrawRectInOnDraw.right;
                        float viewBottom = mViewDrawRectInOnDraw.bottom;

                        float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                        float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);

                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mLastPoint.x * k + b;
                        mDocViewerBBox.right = mLastPoint.x;
                        mDocViewerBBox.bottom = mViewDrawRect.bottom;
                    } else if (mLastOper == OPER_SCALE_RB) {
                        float viewLeft = mViewDrawRect.left;
                        float viewTop = mViewDrawRect.top;
                        float viewRight = mViewDrawRect.right;
                        float viewBottom = mViewDrawRect.bottom;

                        float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                        float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);

                        mDocViewerBBox.left = mViewDrawRect.left;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mLastPoint.x;
                        mDocViewerBBox.bottom = mLastPoint.x * k + b;
                    } else if (mLastOper == OPER_SCALE_LB) {
                        float viewLeft = mViewDrawRectInOnDraw.left;
                        float viewTop = mViewDrawRectInOnDraw.top;
                        float viewRight = mViewDrawRectInOnDraw.right;
                        float viewBottom = mViewDrawRectInOnDraw.bottom;

                        float k = (viewTop - viewBottom) / (viewRight - viewLeft);
                        float b = (viewBottom * viewRight - viewTop * viewLeft) / (viewRight - viewLeft);

                        mDocViewerBBox.left = mLastPoint.x;
                        mDocViewerBBox.top = mViewDrawRect.top;
                        mDocViewerBBox.right = mViewDrawRect.right;
                        mDocViewerBBox.bottom = mLastPoint.x * k + b;
                    }
                    mDocViewerBBox.inset(-thickness / 2f, -thickness / 2f);
                    if (mLastOper == OPER_TRANSLATE || mLastOper == OPER_DEFAULT) {
                        mDocViewerBBox = AppUtil.toRectF(curAnnot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mDocViewerBBox, mDocViewerBBox, annotPageIndex);

                        float dx = mLastPoint.x - mDownPoint.x;
                        float dy = mLastPoint.y - mDownPoint.y;

                        mDocViewerBBox.offset(dx, dy);
                    }

                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDocViewerBBox, mDocViewerBBox, annotPageIndex);
                    mAnnotMenu.update(mDocViewerBBox);
                    if (mPropertyBar.isShowing()) {
                        RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mDocViewerBBox);
                        mPropertyBar.update(rectF);
                    }

                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }

    private void drawControlPoints(Canvas canvas, RectF rectBBox) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mCtlPtPaint.setStrokeWidth(mCtlPtLineWidth);
        for (PointF ctlPt : ctlPts) {
            mCtlPtPaint.setColor(Color.WHITE);
            mCtlPtPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
            int color = Color.parseColor("#179CD8");
            mCtlPtPaint.setColor(color);
            mCtlPtPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
        }
    }

    private Path mImaginaryPath = new Path();

    private void drawControlImaginary(Canvas canvas, RectF rectBBox) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mFrmPaint.setStrokeWidth(mCtlPtLineWidth);
        int color = Color.parseColor("#179CD8");
        mFrmPaint.setColor(color);
        mImaginaryPath.reset();
        // set path
        pathAddLine(mImaginaryPath, ctlPts[0].x + mCtlPtRadius, ctlPts[0].y, ctlPts[1].x - mCtlPtRadius, ctlPts[1].y);
        pathAddLine(mImaginaryPath, ctlPts[1].x, ctlPts[1].y + mCtlPtRadius, ctlPts[2].x, ctlPts[2].y - mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[2].x - mCtlPtRadius, ctlPts[2].y, ctlPts[3].x + mCtlPtRadius, ctlPts[3].y);
        pathAddLine(mImaginaryPath, ctlPts[3].x, ctlPts[3].y - mCtlPtRadius, ctlPts[0].x, ctlPts[0].y + mCtlPtRadius);

        canvas.drawPath(mImaginaryPath, mFrmPaint);
    }

    private void pathAddLine(Path path, float start_x, float start_y, float end_x, float end_y) {
        path.moveTo(start_x, start_y);
        path.lineTo(end_x, end_y);
    }

    private void preparePropertyBar(Screen annot) {
        try {
            mPropertyBar.setEditable(!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot)));
            mPropertyBar.setProperty(PropertyBar.PROPERTY_ROTATION, annot.getRotation());
            int opacity = AppDmUtil.opacity255To100((int) (annot.getOpacity() * 255f + 0.5f));
            mPropertyBar.setProperty(PropertyBar.PROPERTY_OPACITY, opacity);
            mPropertyBar.setArrowVisible(false);
            mPropertyBar.reset(getSupportedProperties());

            mPropertyBar.setPropertyChangeListener(mPropertyChangeListener);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private long getSupportedProperties() {
        return PropertyBar.PROPERTY_ROTATION
                | PropertyBar.PROPERTY_OPACITY;
    }

    private void prepareAnnotMenu(final Annot annot) {
        resetAnnotationMenuResource(annot);

        mAnnotMenu.dismiss();
        mAnnotMenu.setMenuItems(mMenuText);

        mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
            @Override
            public void onAMClick(int btType) {
                if (btType == AnnotMenu.AM_BT_DELETE) {
                    if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                        deleteAnnot(annot, true, null);
                    }
                } else if (btType == AnnotMenu.AM_BT_STYLE) {

                    RectF rectF = AppUtil.toGlobalVisibleRectF(((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getRootView(), mDocViewerBBox);
                    mPropertyBar.show(rectF, false);
                    mAnnotMenu.dismiss();
                } else if (btType == AnnotMenu.AM_BT_FLATTEN) {
                    ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                    UIAnnotFlatten.flattenAnnot(mPdfViewCtrl, annot);
                }
            }
        });
    }

    protected void onOpacityValueChanged(int opacity) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        try {
            if (annot != null
                    && uiExtensionsManager.getCurrentAnnotHandler() == this
                    && AppDmUtil.opacity100To255(opacity) != (int) (((Screen) annot).getOpacity() * 255f)) {

                modifyAnnot(annot.getPage().getIndex(), annot, AppUtil.toRectF(annot.getRect()), AppDmUtil.opacity100To255(opacity),
                        ((Screen) annot).getRotation(), annot.getContent(), false, false,
                        PDFImageModule.MODULE_NAME_IMAGE, null);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    protected void onRotationValueChanged(int rotation) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        try {
            if (annot != null
                    && uiExtensionsManager.getCurrentAnnotHandler() == this
                    && rotation != ((Screen) annot).getRotation()) {

                modifyAnnot(annot.getPage().getIndex(), annot, AppUtil.toRectF(annot.getRect()), (int) (((Screen) annot).getOpacity() * 255f),
                        rotation, annot.getContent(), false, false,
                        PDFImageModule.MODULE_NAME_IMAGE, null);
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

}
