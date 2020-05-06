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
package com.foxit.uiextensions.annots.stamp;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Stamp;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;


public class StampAnnotHandler implements AnnotHandler {
    public AnnotMenu          mAnnotMenu;
    public ArrayList<Integer> mMenuItems;
    public Annot              mBitmapAnnot;
    public boolean            mIsModify;
    public Context            mContext;
    public Paint              mPaint;
    public Paint              mPaintOut;
    public int                mBBoxSpace;
    public boolean            mTouchCaptured  = false;
    public PointF             mDownPoint;
    public PointF             mLastPoint;
    public RectF              tempUndoBBox;
    public float              mCtlPtLineWidth = 2;
    public float              mCtlPtRadius    = 5;////
    public float              mCtlPtTouchExt  = 20;
    public float              mCtlPtDeltyXY   = 20;

    public static final int CTR_NONE    = -1;
    public static final int CTR_LT      = 1;
    public static final int CTR_T       = 2;
    public static final int CTR_RT      = 3;
    public static final int CTR_R       = 4;
    public static final int CTR_RB      = 5;
    public static final int CTR_B       = 6;
    public static final int CTR_LB      = 7;
    public static final int CTR_L       = 8;
    public              int mCurrentCtr = CTR_NONE;

    public static final int OPER_DEFAULT   = -1;
    public static final int OPER_SCALE_LT  = 1;// old:start at 0
    public static final int OPER_SCALE_T   = 2;
    public static final int OPER_SCALE_RT  = 3;
    public static final int OPER_SCALE_R   = 4;
    public static final int OPER_SCALE_RB  = 5;
    public static final int OPER_SCALE_B   = 6;
    public static final int OPER_SCALE_LB  = 7;
    public static final int OPER_SCALE_L   = 8;
    public static final int OPER_TRANSLATE = 9;
    public              int mLastOper      = OPER_DEFAULT;

    public Paint mCtlPtPaint;
    public Paint mFrmPaint;

    public PointF mDocViewerPt  = new PointF(0, 0);
    public RectF  mPageViewRect = new RectF(0, 0, 0, 0);

    public RectF mPageDrawRect   = new RectF();
    public RectF mInvalidateRect = new RectF(0, 0, 0, 0);
    public RectF mAnnotMenuRect  = new RectF(0, 0, 0, 0);

    public float mThickness = 0f;

    public PDFViewCtrl mPdfViewCtrl;
    public ViewGroup   mParent;

    public StampAnnotHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);

        mPaintOut = new Paint();
        mPaintOut.setAntiAlias(true);
        mPaintOut.setStyle(Paint.Style.STROKE);
        mPaintOut.setPathEffect(AppAnnotUtil.getAnnotBBoxPathEffect());
        mPaintOut.setStrokeWidth(AppAnnotUtil.getInstance(context).getAnnotBBoxStrokeWidth());

        mDownPoint = new PointF();
        mLastPoint = new PointF();

        mMenuItems = new ArrayList<Integer>();

        mCtlPtPaint = new Paint();
        PathEffect effect = AppAnnotUtil.getAnnotBBoxPathEffect();
        mFrmPaint = new Paint();
        mFrmPaint.setPathEffect(effect);
        mFrmPaint.setStyle(Paint.Style.STROKE);
        mFrmPaint.setAntiAlias(true);
        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mBitmapAnnot = null;
    }


    public void setAnnotMenu(AnnotMenu annotMenu) {
        mAnnotMenu = annotMenu;
    }

    public AnnotMenu getAnnotMenu() {
        return mAnnotMenu;
    }

    @Override
    public int getType() {
        return Annot.e_Stamp;
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }


    @Override
    public RectF getAnnotBBox(Annot annot) {
        try {
            return new RectF(AppUtil.toRectF(annot.getRect()));
        } catch (PDFException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        RectF bbox = getAnnotBBox(annot);
        if (bbox == null) return false;
        try {
            mPdfViewCtrl.convertPdfRectToPageViewRect(bbox, bbox, annot.getPage().getIndex());
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return bbox.contains(point.x, point.y);
    }


    //    @Override
//    public void onAnnotSelected(final Annot annot, boolean reRender) {
//        try {
//            tempUndoBBox = AppUtil.toRectF(annot.getRect());
//            mBitmapAnnot = annot;
//            RectF _rect = new RectF(tempUndoBBox);
//            mPageViewRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
//
//            mAnnotMenu.dismiss();
//            mMenuItems.clear();
//            if (!((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
//                mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
//            } else {
//                mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
////                mMenuItems.add(AnnotMenu.AM_BT_REPLY);
////                mMenuItems.add(AnnotMenu.AM_BT_FLATTEN);
//                if (!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot))) {
//                    mMenuItems.add(AnnotMenu.AM_BT_DELETE);
//                }
//            }
//            mAnnotMenu.setMenuItems(mMenuItems);
//            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
//                @Override
//                public void onAMClick(int btType) {
//                    mAnnotMenu.dismiss();
//                    if (btType == AnnotMenu.AM_BT_COMMENT) {
//                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
//                        UIAnnotReply.showComments(mPdfViewCtrl, mParent, annot);
//                    } else if (btType == AnnotMenu.AM_BT_REPLY) {
//                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
//                        UIAnnotReply.replyToAnnot(mPdfViewCtrl, mParent, annot);
//                    } else if (btType == AnnotMenu.AM_BT_DELETE) {
//                        delAnnot(annot, true, null);
//                    } else if (AnnotMenu.AM_BT_FLATTEN == btType) {
//                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
//                        UIAnnotFlatten.flattenAnnot(mPdfViewCtrl, annot);
//                    }
//                }
//            });
//            RectF viewRect = new RectF(_rect);
//            final RectF modifyRectF = new RectF(viewRect);
//            final int pageIndex = annot.getPage().getIndex();
//            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
//            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewRect, viewRect, pageIndex);
//            mAnnotMenu.show(viewRect);
//
//            // change modify status
//            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
//                mPdfViewCtrl.convertPdfRectToPageViewRect(modifyRectF, modifyRectF, pageIndex);
//                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(modifyRectF));
//                if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
//                    mBitmapAnnot = annot;
//                }
//
//            } else {
//                mBitmapAnnot = annot;
//            }
//            mIsModify = false;
//        } catch (PDFException e) {
//            e.printStackTrace();
//        }
//    }

    public interface OnPepStampListener {
        void onOpen(Annot conent);
        void onDelete(Annot conent);
        void onModify(Annot annot,int pageIndex);
    }

    public void setOnPepStampListener(OnPepStampListener onPepStampListener) {
        this.onPepStampListener = onPepStampListener;
    }

    private OnPepStampListener onPepStampListener;

    @Override
    public void onAnnotSelected(final Annot annot, boolean reRender) {
        try {
            tempUndoBBox = AppUtil.toRectF(annot.getRect());
            mBitmapAnnot = annot;
            RectF _rect = new RectF(tempUndoBBox);
            mPageViewRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);

            mAnnotMenu.dismiss();
            mMenuItems.clear();
            if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
                mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
            } else {
                mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
//                mMenuItems.add(AnnotMenu.AM_BT_REPLY);
//                mMenuItems.add(AnnotMenu.AM_BT_FLATTEN);
                if (!(AppAnnotUtil.isLocked(annot) || AppAnnotUtil.isReadOnly(annot))) {
                    mMenuItems.add(AnnotMenu.AM_BT_DELETE);
                }
            }
            mAnnotMenu.setMenuItems(mMenuItems);
            mAnnotMenu.setListener(new AnnotMenu.ClickListener() {
                @Override
                public void onAMClick(int btType) {
                    mAnnotMenu.dismiss();
                    if (btType == AnnotMenu.AM_BT_COMMENT) {
                        //TODO 改动
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
//                        UIAnnotReply.showComments(mPdfViewCtrl, mParent, annot);
                        if (onPepStampListener != null) {
                            onPepStampListener.onOpen(annot);
                        }
                    } else if (btType == AnnotMenu.AM_BT_REPLY) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        UIAnnotReply.replyToAnnot(mPdfViewCtrl, mParent, annot);
                    } else if (btType == AnnotMenu.AM_BT_DELETE) {
                        delAnnot(annot, true, null);
                    } else if (AnnotMenu.AM_BT_FLATTEN == btType) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
                        UIAnnotFlatten.flattenAnnot(mPdfViewCtrl, annot);
                    }
                }
            });
            RectF       viewRect    = new RectF(_rect);
            final RectF modifyRectF = new RectF(viewRect);
            final int   pageIndex   = annot.getPage().getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewRect, viewRect, pageIndex);
            mAnnotMenu.show(viewRect);

            // change modify status
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(modifyRectF, modifyRectF, pageIndex);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(modifyRectF));
                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    mBitmapAnnot = annot;
                }

            } else {
                mBitmapAnnot = annot;
            }
            mIsModify = false;
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
        mAnnotMenu.dismiss();
        try {
            PDFPage page = annot.getPage();
            if (page != null) {
                final int pageIndex = page.getIndex();

                com.foxit.sdk.common.fxcrt.RectF _rectF   = annot.getRect();
                RectF                            pdfRect  = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                final RectF                      viewRect = new RectF(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                if (mIsModify && reRender) {
                    if (tempUndoBBox.equals(pdfRect)) {
                        modifyAnnot(pageIndex, annot, pdfRect, annot.getContent(), false, false, null);
                    } else {
                        modifyAnnot(pageIndex, annot, pdfRect, annot.getContent(), true, true, null);
                    }
                } else if (mIsModify) {
                    com.foxit.sdk.common.fxcrt.RectF rect = new com.foxit.sdk.common.fxcrt.RectF(tempUndoBBox.left, tempUndoBBox.bottom, tempUndoBBox.right, tempUndoBBox.top);
                    annot.move(rect);
                    annot.resetAppearanceStream();
                }
                mIsModify = false;
                if (mPdfViewCtrl.isPageVisible(pageIndex) && reRender) {
                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                    mBitmapAnnot = null;
                    return;
                }
            }
            mBitmapAnnot = null;
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result) {
        if (mToolHandler != null) {
            mToolHandler.addAnnot(pageIndex, content, addUndo, result);
        } else {
            if (result != null) {
                result.result(null, false);
            }
        }
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        try {
            PDFPage page      = annot.getPage();
            int     pageIndex = page.getIndex();

            com.foxit.sdk.common.fxcrt.RectF _rectF   = annot.getRect();
            RectF                            bbox     = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
            String                           contents = annot.getContent();
            tempUndoBBox = new RectF(bbox);

            if (content.getBBox() != null)
                bbox = content.getBBox();
            if (content.getContents() != null) {
                contents = content.getContents();
            }
            modifyAnnot(pageIndex, annot, bbox, contents, true, addUndo, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }


    public void modifyAnnot(final int pageIndex, final Annot annot, RectF bbox, String content, boolean isModifyJni, final boolean addUndo, final Event.Callback result) {

        final StampModifyUndoItem undoItem = new StampModifyUndoItem(mPdfViewCtrl);
        undoItem.setCurrentValue(annot);
        undoItem.mPageIndex = pageIndex;
        undoItem.mBBox = new RectF(bbox);
        undoItem.mContents = content;
        undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();

        undoItem.mUndoBox = new RectF(tempUndoBBox);
        try {
            undoItem.mUndoContent = annot.getContent();
        } catch (PDFException e) {
            e.printStackTrace();
        }
        undoItem.mRedoBox = new RectF(bbox);
        undoItem.mRedoContent = content;

        if (isModifyJni) {

            if (onPepStampListener != null) {
                onPepStampListener.onModify(annot,pageIndex);
            }

            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(true);
            StampEvent event = new StampEvent(EditAnnotEvent.EVENTTYPE_MODIFY, undoItem, (Stamp) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        if (addUndo) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        }
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(false);
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            RectF rectF = new RectF(0, 0, mPdfViewCtrl.getPageViewWidth(pageIndex), mPdfViewCtrl.getPageViewHeight(pageIndex));
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(rectF));
                        }
                    }

                    if (result != null) {
                        result.result(null, success);
                    }
                }
            });
            mPdfViewCtrl.addTask(task);
        }

        try {

            if (isModifyJni) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(annot.getPage(), annot);
            }

            mIsModify = true;

            // step 3: update pageview
            if (!isModifyJni) {
                com.foxit.sdk.common.fxcrt.RectF _rectF     = annot.getRect();
                RectF                            annotRectF = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());

                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                    annotRectF.inset(-thickness - mCtlPtRadius - mCtlPtDeltyXY, -thickness - mCtlPtRadius - mCtlPtDeltyXY);
                    mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    public RectF mThicknessRectF = new RectF();

    public float thicknessOnPageView(int pageIndex, float thickness) {
        mThicknessRectF.set(0, 0, thickness, thickness);
        mPdfViewCtrl.convertPdfRectToPageViewRect(mThicknessRectF, mThicknessRectF, pageIndex);
        return Math.abs(mThicknessRectF.width());
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
        delAnnot(annot, addUndo, result);
    }

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e, Annot annot) {

        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);

        float envX   = point.x;
        float envY   = point.y;
        int   action = e.getAction();
        try {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                        if (pageIndex == annot.getPage().getIndex()) {
                            com.foxit.sdk.common.fxcrt.RectF _rectF       = annot.getRect();
                            RectF                            pageViewBBox = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                            mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                            RectF pdfRect = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                            mPageViewRect.set(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                            mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
                            mPageViewRect.inset(mThickness / 2f, mThickness / 2f);
                            mCurrentCtr = isTouchControlPoint(pageViewBBox, envX, envY);
                            mDownPoint.set(envX, envY);
                            mLastPoint.set(envX, envY);

                            if (isHitAnnot(annot, point)) {
                                mTouchCaptured = true;
                                mLastOper = OPER_TRANSLATE;
                                return true;
                            }
                        }
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (mTouchCaptured && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()
                            && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
                        if (envX != mLastPoint.x && envY != mLastPoint.y) {
                            com.foxit.sdk.common.fxcrt.RectF _rectF       = annot.getRect();
                            RectF pageViewBBox = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                            mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                            // Judging border value
                            float deltaXY = mCtlPtLineWidth + mCtlPtRadius * 2 + 2;
                            switch (mLastOper) {
                                case OPER_TRANSLATE: {
                                    mInvalidateRect.set(pageViewBBox);
                                    mAnnotMenuRect.set(pageViewBBox);
                                    mInvalidateRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    mAnnotMenuRect.offset(envX - mDownPoint.x, envY - mDownPoint.y);
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
                                    mLastPoint.set(envX, envY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
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
                    if (mTouchCaptured && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() && pageIndex == annot.getPage().getIndex()) {
                        com.foxit.sdk.common.fxcrt.RectF _rectF       = annot.getRect();
                        RectF                            pageViewRect = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                        switch (mLastOper) {
                            case OPER_TRANSLATE: {
                                mPageDrawRect.set(pageViewRect);
                                mPageDrawRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                checkAnnotDeviation(pageIndex, annot, mPageDrawRect.centerX(), mPageDrawRect.centerY());

                                break;
                            }

                            default:
                                break;
                        }
                        if (mLastOper != OPER_DEFAULT && !mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                            RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
                            RectF bboxRect    = new RectF();
                            mPdfViewCtrl.convertPageViewRectToPdfRect(viewDrawBox, bboxRect, pageIndex);
                            modifyAnnot(pageIndex, annot, bboxRect, annot.getContent(), true, false, null);

                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);

                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.update(viewDrawBox);
                            } else {
                                mAnnotMenu.show(viewDrawBox);
                            }
                        } else {
                            RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
                            float _lineWidth  = annot.getBorderInfo().getWidth();
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
        } catch (PDFException e1) {
            e1.printStackTrace();
        }
        return false;
    }

    /**
     * 针对锚点不可重叠的需求,校验是否需要设置偏移
     *
     * @param pageIndex
     * @param currentAnnot
     * @param centerY
     */
    private void checkAnnotDeviation(int pageIndex, Annot currentAnnot, float centerX, float centerY) {
        try {
            PDFPage pdfPage = mPdfViewCtrl.getDoc().getPage(pageIndex);
            Rect    rectV   = mPdfViewCtrl.getPageViewRect(pageIndex);
            RectF   rectPDF = new RectF(rectV.left, rectV.top, rectV.right, rectV.bottom);
            mPdfViewCtrl.convertDisplayViewRectToPageViewRect(rectPDF, rectPDF, pageIndex);
            mPdfViewCtrl.convertPageViewRectToPdfRect(rectPDF, rectPDF, pageIndex);//获得pdf页面的rectf框
            int size = pdfPage.getAnnotCount();
            if (size > 0) {
                RectF rect = AppUtil.toRectF(currentAnnot.getRect());
                mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, pageIndex);
                PointF point = new PointF(centerX, centerY);
                mPdfViewCtrl.convertPageViewPtToPdfPt(point, point, pageIndex);
                RectF rect1 = OverlapUtils.getOverlappingRect(point, pdfPage, size);
                if (rect1 != null) {
                    point = OverlapUtils.getOverlapCenterPontF(rect1, pdfPage, rectPDF, 0);
                    point.set(point.x, point.y);
                    mPdfViewCtrl.convertPdfPtToPageViewPt(point, point, pageIndex);
                    mPageDrawRect.offset(point.x - centerX, point.y - centerY);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private static final float hight = (float) 20;//pdf中,锚点的固定高度大约是20

    /**
     * 获取重叠锚点的rectf
     *
     * @param pointF  新增或移动后的锚点中心点
     * @param pdfPage 当前页的pdfPage
     * @param size    当前页的锚点数量
     * @return
     */
    public static RectF getOverlappingRect(PointF pointF, PDFPage pdfPage, int size) {
        for (int i = 0; i < size; i++) {
            Annot annot;
            float rectTop;
            float rectBottom;
            float rectLeft;
            float rectRight;
            try {
                annot = pdfPage.getAnnot(i);
                int type = annot.getType();
                if (Annot.e_Line == type || Annot.e_Ink == type || Annot.e_Highlight == type) {
                    continue;
                }
                com.foxit.sdk.common.fxcrt.RectF rect1 = annot.getRect();
                RectF                            rect  = AppUtil.toRectF(rect1);
                rectTop = rect.top + hight / 2;
                rectBottom = rect.bottom - hight / 2;
                rectLeft = rect.left - hight / 2;
                rectRight = rect.right + hight / 2;
                if (rectLeft <= pointF.x && rectRight >= pointF.x && rectTop >= pointF.y && rectBottom <= pointF.y) {
                    return rect;
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
//    @Override
//    public boolean onTouchEvent(int pageIndex, MotionEvent e, Annot annot) {
//
//        PointF point = new PointF(e.getX(), e.getY());
//        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
//
//        float envX = point.x;
//        float envY = point.y;
//        int action = e.getAction();
//        try {
//            switch (action) {
//                case MotionEvent.ACTION_DOWN:
//                    if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
//                        if (pageIndex == annot.getPage().getIndex()) {
//                            com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
//                            RectF pageViewBBox = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
//                            mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
//                            RectF pdfRect = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
//                            mPageViewRect.set(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
//                            mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
//                            mPageViewRect.inset(mThickness / 2f, mThickness / 2f);
//                            mCurrentCtr = isTouchControlPoint(pageViewBBox, envX, envY);
//                            mDownPoint.set(envX, envY);
//                            mLastPoint.set(envX, envY);
//
//                            if (mCurrentCtr == CTR_LT) {
//                                mTouchCaptured = true;
//                                mLastOper = OPER_SCALE_LT;
//                                return true;
//                            } else if (mCurrentCtr == CTR_T) {
//                                mTouchCaptured = true;
//                                mLastOper = OPER_SCALE_T;
//                                return true;
//                            } else if (mCurrentCtr == CTR_RT) {
//                                mTouchCaptured = true;
//                                mLastOper = OPER_SCALE_RT;
//                                return true;
//                            } else if (mCurrentCtr == CTR_R) {
//                                mTouchCaptured = true;
//                                mLastOper = OPER_SCALE_R;
//                                return true;
//                            } else if (mCurrentCtr == CTR_RB) {
//                                mTouchCaptured = true;
//                                mLastOper = OPER_SCALE_RB;
//                                return true;
//                            } else if (mCurrentCtr == CTR_B) {
//                                mTouchCaptured = true;
//                                mLastOper = OPER_SCALE_B;
//                                return true;
//                            } else if (mCurrentCtr == CTR_LB) {
//                                mTouchCaptured = true;
//                                mLastOper = OPER_SCALE_LB;
//                                return true;
//                            } else if (mCurrentCtr == CTR_L) {
//                                mTouchCaptured = true;
//                                mLastOper = OPER_SCALE_L;
//                                return true;
//                            } else if (isHitAnnot(annot, point)) {
//                                mTouchCaptured = true;
//                                mLastOper = OPER_TRANSLATE;
//                                return true;
//                            }
//                        }
//                    }
//                    return false;
//                case MotionEvent.ACTION_MOVE:
//                    if (mTouchCaptured && annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
//                            && pageIndex == annot.getPage().getIndex()
//                            && ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
//                        if (envX != mLastPoint.x && envY != mLastPoint.y) {
//                            com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
//                            RectF pageViewBBox = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
//                            mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
//                            float deltaXY = mCtlPtLineWidth + mCtlPtRadius * 2 + 2;// Judging border value
//                            switch (mLastOper) {
//                                case OPER_TRANSLATE: {
//                                    mInvalidateRect.set(pageViewBBox);
//                                    mAnnotMenuRect.set(pageViewBBox);
//                                    mInvalidateRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
//                                    mAnnotMenuRect.offset(envX - mDownPoint.x, envY - mDownPoint.y);
//                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
//                                    mInvalidateRect.union(mAnnotMenuRect);
//                                    mInvalidateRect.inset(-deltaXY - mCtlPtDeltyXY, -deltaXY - mCtlPtDeltyXY);
//                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
//                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));
//
//                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
//                                    if (mAnnotMenu.isShowing()) {
//                                        mAnnotMenu.dismiss();
//                                        mAnnotMenu.update(mAnnotMenuRect);
//                                    }
//                                    mLastPoint.set(envX, envY);
//                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
//                                    break;
//                                }
//                                case OPER_SCALE_LT: {
//                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
//                                        mInvalidateRect.set(mLastPoint.x, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
//                                        mAnnotMenuRect.set(envX, envY, mPageViewRect.right, mPageViewRect.bottom);
//                                        mInvalidateRect.sort();
//                                        mAnnotMenuRect.sort();
//                                        mInvalidateRect.union(mAnnotMenuRect);
//                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
//                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));
//
//                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
//
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
//                                        if (mAnnotMenu.isShowing()) {
//                                            mAnnotMenu.dismiss();
//                                            mAnnotMenu.update(mAnnotMenuRect);
//                                        }
//                                        mLastPoint.set(envX, envY);
//                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
//                                    }
//                                    break;
//                                }
//                                case OPER_SCALE_T: {
//                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
//                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
//                                        mAnnotMenuRect.set(mPageViewRect.left, envY, mPageViewRect.right, mPageViewRect.bottom);
//                                        mInvalidateRect.sort();
//                                        mAnnotMenuRect.sort();
//                                        mInvalidateRect.union(mAnnotMenuRect);
//                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
//                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));
//
//                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
//
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
//                                        if (mAnnotMenu.isShowing()) {
//                                            mAnnotMenu.dismiss();
//                                            mAnnotMenu.update(mAnnotMenuRect);
//                                        }
//                                        mLastPoint.set(envX, envY);
//                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
//                                    }
//                                    break;
//                                }
//                                case OPER_SCALE_RT: {
//                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
//                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mLastPoint.x, mPageViewRect.bottom);
//                                        mAnnotMenuRect.set(mPageViewRect.left, envY, envX, mPageViewRect.bottom);
//                                        mInvalidateRect.sort();
//                                        mAnnotMenuRect.sort();
//                                        mInvalidateRect.union(mAnnotMenuRect);
//                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
//                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));
//
//                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
//
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
//                                        if (mAnnotMenu.isShowing()) {
//                                            mAnnotMenu.dismiss();
//                                            mAnnotMenu.update(mAnnotMenuRect);
//                                        }
//                                        mLastPoint.set(envX, envY);
//                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
//                                    }
//                                    break;
//                                }
//                                case OPER_SCALE_R: {
//                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
//                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mPageViewRect.bottom);
//                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, envX, mPageViewRect.bottom);
//                                        mInvalidateRect.sort();
//                                        mAnnotMenuRect.sort();
//                                        mInvalidateRect.union(mAnnotMenuRect);
//                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
//                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));
//
//                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
//
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
//                                        if (mAnnotMenu.isShowing()) {
//                                            mAnnotMenu.dismiss();
//                                            mAnnotMenu.update(mAnnotMenuRect);
//                                        }
//                                        mLastPoint.set(envX, envY);
//                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
//                                    }
//                                    break;
//                                }
//                                case OPER_SCALE_RB: {
//                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
//                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mLastPoint.y);
//                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, envX, envY);
//                                        mInvalidateRect.sort();
//                                        mAnnotMenuRect.sort();
//                                        mInvalidateRect.union(mAnnotMenuRect);
//                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
//                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));
//
//                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
//
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
//                                        if (mAnnotMenu.isShowing()) {
//                                            mAnnotMenu.dismiss();
//                                            mAnnotMenu.update(mAnnotMenuRect);
//                                        }
//
//                                        mLastPoint.set(envX, envY);
//                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
//                                    }
//                                    break;
//                                }
//                                case OPER_SCALE_B: {
//                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
//                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
//                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, envY);
//                                        mInvalidateRect.sort();
//                                        mAnnotMenuRect.sort();
//                                        mInvalidateRect.union(mAnnotMenuRect);
//                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
//                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));
//
//                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
//
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
//                                        if (mAnnotMenu.isShowing()) {
//                                            mAnnotMenu.dismiss();
//                                            mAnnotMenu.update(mAnnotMenuRect);
//                                        }
//                                        mLastPoint.set(envX, envY);
//                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
//                                    }
//                                    break;
//                                }
//                                case OPER_SCALE_LB: {
//                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
//                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
//                                        mAnnotMenuRect.set(envX, mPageViewRect.top, mPageViewRect.right, envY);
//                                        mInvalidateRect.sort();
//                                        mAnnotMenuRect.sort();
//                                        mInvalidateRect.union(mAnnotMenuRect);
//                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
//                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));
//
//                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
//
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
//                                        if (mAnnotMenu.isShowing()) {
//                                            mAnnotMenu.dismiss();
//                                            mAnnotMenu.update(mAnnotMenuRect);
//                                        }
//                                        mLastPoint.set(envX, envY);
//                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
//                                    }
//                                    break;
//                                }
//                                case OPER_SCALE_L: {
//                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
//                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
//                                        mAnnotMenuRect.set(envX, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
//                                        mInvalidateRect.sort();
//                                        mAnnotMenuRect.sort();
//                                        mInvalidateRect.union(mAnnotMenuRect);
//                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
//                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));
//
//                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
//
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
//                                        if (mAnnotMenu.isShowing()) {
//                                            mAnnotMenu.dismiss();
//                                            mAnnotMenu.update(mAnnotMenuRect);
//                                        }
//                                        mLastPoint.set(envX, envY);
//                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
//                                    }
//                                    break;
//                                }
//                                case OPER_DEFAULT:
//                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
//                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
//                                        mAnnotMenuRect.set(envX, mPageViewRect.top, mPageViewRect.right, envY);
//                                        mInvalidateRect.sort();
//                                        mAnnotMenuRect.sort();
//                                        mInvalidateRect.union(mAnnotMenuRect);
//                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
//                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));
//
//                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
//
//                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
//                                        if (mAnnotMenu.isShowing()) {
//                                            mAnnotMenu.dismiss();
//                                            mAnnotMenu.update(mAnnotMenuRect);
//                                        }
//                                        mLastPoint.set(envX, envY);
//                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
//                                    }
//                                    break;
//                                default:
//                                    break;
//                            }
//                        }
//                        return true;
//                    }
//                    return false;
//                case MotionEvent.ACTION_UP:
//                case MotionEvent.ACTION_CANCEL:
//                    if (mTouchCaptured && annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() && pageIndex == annot.getPage().getIndex()) {
//                        com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
//                        RectF pageViewRect = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
//                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
//                        pageViewRect.inset(mThickness / 2, mThickness / 2);
//                        switch (mLastOper) {
//                            case OPER_TRANSLATE: {
//                                mPageDrawRect.set(pageViewRect);
//                                mPageDrawRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
//                                break;
//                            }
//                            case OPER_SCALE_LT: {
//                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
//                                    mPageDrawRect.set(mLastPoint.x, mLastPoint.y, pageViewRect.right, pageViewRect.bottom);
//                                }
//                                break;
//                            }
//                            case OPER_SCALE_T: {
//                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
//                                    mPageDrawRect.set(pageViewRect.left, mLastPoint.y, pageViewRect.right, pageViewRect.bottom);
//                                }
//                                break;
//                            }
//                            case OPER_SCALE_RT: {
//                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
//                                    mPageDrawRect.set(pageViewRect.left, mLastPoint.y, mLastPoint.x, pageViewRect.bottom);
//                                }
//                                break;
//                            }
//                            case OPER_SCALE_R: {
//                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
//                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, pageViewRect.bottom);
//                                }
//                                break;
//                            }
//                            case OPER_SCALE_RB: {
//                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
//                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, mLastPoint.y);
//                                }
//                                break;
//                            }
//                            case OPER_SCALE_B: {
//                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
//                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, pageViewRect.right, mLastPoint.y);
//                                }
//                                break;
//                            }
//                            case OPER_SCALE_LB: {
//                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
//                                    mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, mLastPoint.y);
//                                }
//                                break;
//                            }
//                            case OPER_SCALE_L: {
//                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
//                                    mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, pageViewRect.bottom);
//                                }
//                                break;
//                            }
//                            default:
//                                break;
//                        }
//                        if (mLastOper != OPER_DEFAULT && !mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
//                            RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
//                            RectF bboxRect = new RectF();
//                            mPdfViewCtrl.convertPageViewRectToPdfRect(viewDrawBox, bboxRect, pageIndex);
//                            modifyAnnot(pageIndex, annot, bboxRect, annot.getContent(), true, false, null);
//
//                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
//
//                            if (mAnnotMenu.isShowing()) {
//                                mAnnotMenu.update(viewDrawBox);
//                            } else {
//                                mAnnotMenu.show(viewDrawBox);
//                            }
//                        } else {
//                            RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
//                            float _lineWidth = annot.getBorderInfo().getWidth();
//                            viewDrawBox.inset(-thicknessOnPageView(pageIndex, _lineWidth) / 2, -thicknessOnPageView(pageIndex, _lineWidth) / 2);
//                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
//                            if (mAnnotMenu.isShowing()) {
//                                mAnnotMenu.update(viewDrawBox);
//                            } else {
//                                mAnnotMenu.show(viewDrawBox);
//                            }
//                        }
//                        mTouchCaptured = false;
//                        mDownPoint.set(0, 0);
//                        mLastPoint.set(0, 0);
//                        mLastOper = OPER_DEFAULT;
//                        mCurrentCtr = CTR_NONE;
//                        return true;
//                    }
//                    mTouchCaptured = false;
//                    mDownPoint.set(0, 0);
//                    mLastPoint.set(0, 0);
//                    mLastOper = OPER_DEFAULT;
//                    mCurrentCtr = CTR_NONE;
//                    mTouchCaptured = false;
//                    return false;
//                default:
//            }
//        } catch (PDFException e1) {
//            e1.printStackTrace();
//        }
//        return false;
//    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        //返回true,过滤掉福昕自带的长按事件
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {

        try {
            PDFPage page = mPdfViewCtrl.getDoc().getPage(pageIndex);
            mDocViewerPt.set(motionEvent.getX(), motionEvent.getY());//display view

            PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);

            mThickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
            com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
            RectF                            _rect  = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
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

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        return true;
    }

    public RectF      mBBoxInOnDraw         = new RectF();
    public RectF      mViewDrawRectInOnDraw = new RectF();
    public DrawFilter mDrawFilter           = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    RectF mMapBounds = new RectF();

    public PointF[] calculateControlPoints(RectF rect) {
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

    public void drawControlPoints(Canvas canvas, RectF rectBBox, int color) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mCtlPtPaint.setStrokeWidth(mCtlPtLineWidth);
        for (PointF ctlPt : ctlPts) {
            mCtlPtPaint.setColor(Color.WHITE);
            mCtlPtPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
            int ctl = Color.parseColor("#179CD8");
            mCtlPtPaint.setColor(ctl);
            mCtlPtPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
        }
    }

    Path mImaginaryPath = new Path();

    public void pathAddLine(Path path, float start_x, float start_y, float end_x, float end_y) {
        path.moveTo(start_x, start_y);
        path.lineTo(end_x, end_y);

    }

    public void drawControlImaginary(Canvas canvas, RectF rectBBox, int color) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mFrmPaint.setStrokeWidth(mCtlPtLineWidth);
        int selectRect = Color.parseColor("#179CD8");
        mFrmPaint.setColor(selectRect);
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

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (annot == null || !(annot instanceof Stamp)) {
            return;
        }
        if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() != this)
            return;
        try {
            int annotPageIndex = annot.getPage().getIndex();
            if (AppAnnotUtil.equals(mBitmapAnnot, annot) && annotPageIndex == pageIndex) {
                canvas.save();
                canvas.setDrawFilter(mDrawFilter);
                RectF                            frameRectF = new RectF();
                com.foxit.sdk.common.fxcrt.RectF _rectF     = annot.getRect();
                RectF                            rect2      = AppUtil.toRectF(_rectF);
                float                            thickness  = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
                mPdfViewCtrl.convertPdfRectToPageViewRect(rect2, rect2, pageIndex);
                rect2.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setStrokeWidth(LineWidth2PageView(pageIndex, 0.6f));
                int color = Color.parseColor("#4EA984");
                mPaint.setColor(color);
                frameRectF.set(rect2.left - mBBoxSpace, rect2.top - mBBoxSpace, rect2.right + mBBoxSpace, rect2.bottom + mBBoxSpace);
                mPaintOut.setColor(color);
                RectF _rect = AppUtil.toRectF(_rectF);
                mViewDrawRectInOnDraw.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
                mPdfViewCtrl.convertPdfRectToPageViewRect(mViewDrawRectInOnDraw, mViewDrawRectInOnDraw, pageIndex);
                mViewDrawRectInOnDraw.inset(thickness / 2f, thickness / 2f);
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
                mBBoxInOnDraw.inset(-thickness / 2f, -thickness / 2f);
                if (mLastOper == OPER_TRANSLATE || mLastOper == OPER_DEFAULT) {// TRANSLATE or DEFAULT
                    mBBoxInOnDraw = AppUtil.toRectF(_rectF);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mBBoxInOnDraw, mBBoxInOnDraw, pageIndex);

                    float dx = mLastPoint.x - mDownPoint.x;
                    float dy = mLastPoint.y - mDownPoint.y;

                    mBBoxInOnDraw.offset(dx, dy);
                }
                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    drawControlPoints(canvas, mBBoxInOnDraw, (int) annot.getBorderColor());
                    // add Control Imaginary
                    drawControlImaginary(canvas, mBBoxInOnDraw, (int) annot.getBorderColor());
                }
                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    public StampToolHandler mToolHandler;

    public void setToolHandler(StampToolHandler toolHandler) {
        mToolHandler = toolHandler;
    }

//    public void delAnnot(final Annot annot, final boolean addUndo, final Event.Callback result) {
//        // step 1 : set current annot to null
//        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
//            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
//        }
//        try {
//            final PDFPage page = annot.getPage();
//            if (page == null) {
//                if (result != null) {
//                    result.result(null, false);
//                }
//                return;
//            }
//            final RectF               viewRect  = AppUtil.toRectF(annot.getRect());
//            final int                 pageIndex = page.getIndex();
//            final StampDeleteUndoItem undoItem  = new StampDeleteUndoItem(mPdfViewCtrl);
//
//            undoItem.setCurrentValue(annot);
//            undoItem.mPageIndex = pageIndex;
//            undoItem.mStampType = StampUntil.getStampTypeByName(undoItem.mSubject);
//            undoItem.mIconName = ((Stamp) annot).getIconName();
//            undoItem.mRotation = ((Stamp) annot).getRotation() / 90;
////            undoItem.mDsip = mToolHandler.mDsip;
//
//            if (undoItem.mStampType <= 17 && undoItem.mStampType != -1) {
//                undoItem.mBitmap = BitmapFactory.decodeResource(mContext.getResources(), mToolHandler.mStampIds[undoItem.mStampType]);
//            }
//
//            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
//            StampEvent event = new StampEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Stamp) annot, mPdfViewCtrl);
//            if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().isMultipleSelectAnnots()) {
//                if (result != null) {
//                    result.result(event, true);
//                }
//                return;
//            }
//            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
//                @Override
//                public void result(Event event, boolean success) {
//                    if (success) {
//                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
//                        if (addUndo) {
//                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
//                        }
//                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
//                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
//                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
//                        }
//                    }
//
//                    if (result != null) {
//                        result.result(null, success);
//                    }
//                }
//            });
//            mPdfViewCtrl.addTask(task);
//
//
//        } catch (PDFException e) {
//            e.printStackTrace();
//        }
//
//    }


    private void delAnnot(final Annot annot, final boolean addUndo, final Event.Callback result) {
        // step 1 : set current annot to null
        if (annot == ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null);
        }
        try {
            final PDFPage page = annot.getPage();
            if (page == null) {
                if (result != null) {
                    result.result(null, false);
                }
                return;
            }
            String s = null;
            try {
                s = annot.getContent();
            } catch (PDFException e) {
                e.printStackTrace();
            }
            final String ss = s;

            com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
            final RectF viewRect = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
            final int pageIndex = page.getIndex();

            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);

            final StampDeleteUndoItem undoItem = new StampDeleteUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = pageIndex;
            undoItem.mStampType = StampUntil.getStampTypeByName(undoItem.mSubject);
            undoItem.mIconName = undoItem.mSubject;
//            undoItem.mDsip = mToolHandler.mDsip;

            if (undoItem.mStampType <= 17 && undoItem.mStampType != -1) {
                undoItem.mBitmap = BitmapFactory.decodeResource(mContext.getResources(), mToolHandler.mStampIds[undoItem.mStampType]);
            }

            StampEvent event = new StampEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Stamp) annot, mPdfViewCtrl);
            EditAnnotTask task = new EditAnnotTask(event, new Event.Callback() {
                @Override
                public void result(Event event, boolean success) {
                    if (success) {
                        ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (addUndo) {
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        }
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                            mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                        }
                    }
                    if(!TextUtils.isEmpty(ss)) {
                        onPepStampListener.onDelete(annot);
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

    public float LineWidth2PageView(int pageIndex, float linewidth) {
        RectF rectF = new RectF(0, 0, linewidth, linewidth);
        mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
        return Math.abs(rectF.width());
    }

    public PointF mAdjustPointF = new PointF(0, 0);

    public PointF adjustScalePointF(int pageIndex, RectF rectF, float dxy) {
        float adjustx = 0;
        float adjusty = 0;
        if (mLastOper != OPER_TRANSLATE) {
            rectF.inset(-mThickness / 2f, -mThickness / 2f);
        }
        // must strong to int,In order to solve the conversion error (pageView to Doc)
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

    public int isTouchControlPoint(RectF rect, float x, float y) {
        PointF[] ctlPts = calculateControlPoints(rect);
        RectF    area   = new RectF();
        int      ret    = -1;
        for (int i = 0; i < ctlPts.length; i++) {
            area.set(ctlPts[i].x, ctlPts[i].y, ctlPts[i].x, ctlPts[i].y);
            area.inset(-mCtlPtTouchExt, -mCtlPtTouchExt);
            if (area.contains(x, y)) {
                ret = i + 1;
            }
        }
        return ret;
    }

    public RectF mViewDrawRect  = new RectF(0, 0, 0, 0);
    public RectF mDocViewerBBox = new RectF(0, 0, 0, 0);

    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot();
        if (curAnnot != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentAnnotHandler() == this) {
            try {
                int annotPageIndex = curAnnot.getPage().getIndex();
                if (mPdfViewCtrl.isPageVisible(annotPageIndex)) {
                    float                            thickness = thicknessOnPageView(annotPageIndex, curAnnot.getBorderInfo().getWidth());
                    com.foxit.sdk.common.fxcrt.RectF _rectF    = curAnnot.getRect();
                    RectF                            _rect     = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                    mViewDrawRect.set(_rect.left, _rect.top, _rect.right, _rect.bottom);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mViewDrawRect, mViewDrawRect, annotPageIndex);
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
                        mDocViewerBBox = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mDocViewerBBox, mDocViewerBBox, annotPageIndex);
                        float dx = mLastPoint.x - mDownPoint.x;
                        float dy = mLastPoint.y - mDownPoint.y;
                        mDocViewerBBox.offset(dx, dy);
                    }
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDocViewerBBox, mDocViewerBBox, annotPageIndex);
                    mAnnotMenu.update(mDocViewerBBox);
                }
            } catch (PDFException e) {
                e.printStackTrace();
            }
        }
    }
}
