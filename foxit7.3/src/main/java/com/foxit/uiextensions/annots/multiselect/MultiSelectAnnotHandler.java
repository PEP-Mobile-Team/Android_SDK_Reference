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
package com.foxit.uiextensions.annots.multiselect;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DrawFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Markup;
import com.foxit.sdk.pdf.annots.MarkupArray;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.util.ArrayList;
import java.util.HashMap;

public class MultiSelectAnnotHandler implements AnnotHandler {

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private UIExtensionsManager mUiExtensionsManager;
    private MultiSelectManager mMultiSelectManager;

    private ArrayList<Annot> mGroupAnnots = new ArrayList<>();
    private ArrayList<Annot> mActualSelectGroupAnnots = new ArrayList<>();

    private AnnotMenu mAnnotationMenu;
    private ArrayList<Integer> mMenuItems;

    private int mLastPageIndex = -1;
    private boolean mTouchCaptured = false;
    private PointF mDownPoint = new PointF(0, 0);// whether moving point
    private PointF mLastPoint = new PointF(0, 0);
    private RectF mInvalidateRect = new RectF(0, 0, 0, 0);

    private Paint mPaint;
    private Paint mFrmPaint;// outline
    private Paint mCtlPtPaint;

    private float mThickness = 5.0f;
    private int mControlPtEx = 5;// Refresh the scope expansion width
    private float mCtlPtLineWidth = 5;
    private float mCtlPtRadius = 10;

    private int mSelectAreaColor = 0x2da5da;
    private int mSelectAreaBound = 5;

    private int mMoveState;

    MultiSelectAnnotHandler(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = (UIExtensionsManager) pdfViewCtrl.getUIExtensionsManager();
        mMultiSelectManager = new MultiSelectManager(pdfViewCtrl);

        mMenuItems = new ArrayList<Integer>();
        mAnnotationMenu = new AnnotMenuImpl(mContext, mPdfViewCtrl);

        initPaint();
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(mSelectAreaColor);
        mPaint.setStrokeWidth(mSelectAreaBound);
        mPaint.setAlpha(255);

        PathEffect effect = AppAnnotUtil.getAnnotBBoxPathEffect();
        mFrmPaint = new Paint();
        mFrmPaint.setPathEffect(effect);
        mFrmPaint.setStyle(Paint.Style.STROKE);
        mFrmPaint.setAntiAlias(true);
        mFrmPaint.setColor(mSelectAreaColor);
        mFrmPaint.setAlpha(255);
        mFrmPaint.setStrokeWidth(mCtlPtLineWidth);

        mCtlPtPaint = new Paint();
        mCtlPtPaint.setStrokeWidth(mCtlPtLineWidth);
    }

    @Override
    public int getType() {
        return TYPE_MULTI_SELECT;
    }

    @Override
    public boolean annotCanAnswer(Annot annot) {
        return true;
    }

    @Override
    public RectF getAnnotBBox(Annot annot) {
        return MultiSelectUtils.getGroupRectF(mPdfViewCtrl, annot);
    }

    @Override
    public boolean isHitAnnot(Annot annot, PointF point) {
        RectF rectF = getAnnotBBox(annot);
        try {
            int pageIndex = annot.getPage().getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(rectF, rectF, pageIndex);
            rectF.inset(-10, 10);
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return rectF.contains(point.x, point.y);
    }

    private RectF mGroupAnnotsRect = new RectF();
    private RectF mLastGroupAnnotsRect = new RectF();

    private boolean mCanDelete = true;

    @Override
    public void onAnnotSelected(Annot annot, boolean reRender) {
        try {
            mGroupAnnots.clear();
            mActualSelectGroupAnnots.clear();
            mGroupAnnotsRect.setEmpty();
            mCanDelete = true;

            int pageIndex = annot.getPage().getIndex();
            Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
            MarkupArray markupArray = ((Markup) annot).getGroupElements();
            long arrSize = markupArray.getSize();
            for (long i = 0; i < arrSize; i++) {
                Annot groupAnnot = AppAnnotUtil.createAnnot(markupArray.getAt(i));

                if (mUiExtensionsManager.isLoadAnnotModule(groupAnnot)) {
                    RectF pvRect = AppUtil.toRectF(groupAnnot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                    if (mGroupAnnotsRect.isEmpty())
                        mGroupAnnotsRect.set(pvRect);
                    else
                        mGroupAnnotsRect.union(pvRect);
                    if (mCanDelete)
                        mCanDelete = !(AppAnnotUtil.isReadOnly(groupAnnot) || AppAnnotUtil.isLocked(groupAnnot));
                    mGroupAnnots.add(groupAnnot);
                }
                mActualSelectGroupAnnots.add(groupAnnot);
            }
            mPdfViewCtrl.convertPageViewRectToPdfRect(mGroupAnnotsRect, mGroupAnnotsRect, pageIndex);

            mMoveState = MultiSelectUtils.getMoveState(mGroupAnnots, mCanDelete);
            mLastPageIndex = pageIndex;

            prepareAnnotMenu(mGroupAnnots, pageIndex);
            RectF menuRect = new RectF(mGroupAnnotsRect);
            mPdfViewCtrl.convertPdfRectToPageViewRect(menuRect, menuRect, pageIndex);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(menuRect, menuRect, pageIndex);
            mAnnotationMenu.show(menuRect);

            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                RectF invalidateRect = new RectF(mGroupAnnotsRect);
                mPdfViewCtrl.convertPdfRectToPageViewRect(invalidateRect, invalidateRect, pageIndex);
                mPdfViewCtrl.refresh(pageIndex, AppDmUtil.rectFToRect(invalidateRect));
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void prepareAnnotMenu(final ArrayList<Annot> annots, final int pageIndex) {
        mMenuItems.clear();
        if (mUiExtensionsManager.getDocumentManager().canAddAnnot()) {
            mMenuItems.add(AnnotMenu.AM_BT_FLATTEN);
            if (MultiSelectUtils.isGroupSupportReply(mActualSelectGroupAnnots))
                mMenuItems.add(AnnotMenu.AM_BT_REPLY);
            mMenuItems.add(AnnotMenu.AM_BT_UNGROUP);
            if (mCanDelete)
                mMenuItems.add(AnnotMenu.AM_BT_DELETE);
        }

        mAnnotationMenu.setMenuItems(mMenuItems);
        mAnnotationMenu.setListener(new AnnotMenu.ClickListener() {
            @Override
            public void onAMClick(int btType) {
                if (btType == AnnotMenu.AM_BT_DELETE) {
                    deleteAnnots(pageIndex, annots, mGroupAnnotsRect, true, null);
                } else if (btType == AnnotMenu.AM_BT_FLATTEN) {
                    mUiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                    flattenAnnots(pageIndex, annots, mGroupAnnotsRect, null);
                } else if (btType == AnnotMenu.AM_BT_UNGROUP) {
                    mUiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                    unGroupAnnots(pageIndex, mActualSelectGroupAnnots, true, null);
                } else if (btType == AnnotMenu.AM_BT_REPLY) {
                    mUiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                    Annot annot = annots.get(0);
                    try {
                        annot = AppAnnotUtil.createAnnot(((Markup) annots.get(0)).getGroupHeader());
                    } catch (PDFException e) {
                        e.printStackTrace();
                    }
                    UIAnnotReply.replyToAnnot(mPdfViewCtrl, mUiExtensionsManager.getRootView(), annot, true);
                }
            }
        });
    }

    @Override
    public void onAnnotDeselected(Annot annot, boolean reRender) {
        RectF rect = new RectF(mGroupAnnotsRect);
        mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, mLastPageIndex);
        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(rect, rect, mLastPageIndex);
        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(rect));
        mLastPageIndex = -1;
        mCanDelete = true;
        mMoveState = MultiSelectConstants.STATE_NONE;
        if (mAnnotationMenu != null) {
            mAnnotationMenu.setListener(null);
            if (mAnnotationMenu.isShowing()) {
                mAnnotationMenu.dismiss();
            }
        }
    }

    @Override
    public void addAnnot(int pageIndex, AnnotContent content, boolean addUndo, Event.Callback result) {
    }

    @Override
    public void modifyAnnot(Annot annot, AnnotContent content, boolean addUndo, Event.Callback result) {
        mMultiSelectManager.modifyAnnot(annot, content, addUndo, result);
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
        try {
            int pageIndex = annot.getPage().getIndex();
            Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
            ArrayList<Annot> annots = new ArrayList<>();
            MarkupArray markupArray = ((Markup) annot).getGroupElements();
            long size = markupArray.getSize();
            RectF rectF = new RectF();
            for (long i = 0; i < size; i++) {
                Annot groupAnnot = AppAnnotUtil.createAnnot(markupArray.getAt(i));
                RectF pvRect = AppUtil.toRectF(groupAnnot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                if (i == 0)
                    rectF.set(pvRect);
                else
                    rectF.union(pvRect);
                annots.add(groupAnnot);
            }
            mPdfViewCtrl.convertPageViewRectToPdfRect(rectF, rectF, pageIndex);
            deleteAnnots(annot.getPage().getIndex(), annots, rectF, addUndo, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void deleteAnnots(final int pageIndex,
                              final ArrayList<Annot> annots,
                              final RectF annotsRectF,
                              final boolean addUndo,
                              final Event.Callback callback) {
        for (int i = 0; i < annots.size(); i++) {
            if (AppAnnotUtil.isSameAnnot(annots.get(i), mUiExtensionsManager.getDocumentManager().getCurrentAnnot())) {
                mUiExtensionsManager.getDocumentManager().setCurrentAnnot(null, false);
                break;
            }
        }

        HashMap<String, ArrayList<String>> groupMaps = new HashMap<>();
        ArrayList<String> uniqueIDs = new ArrayList<>();
        for (Annot annot: annots) {
            uniqueIDs.add(AppAnnotUtil.getAnnotUniqueID(annot));
        }
        groupMaps.put(AppAnnotUtil.getAnnotUniqueID(annots.get(0)), uniqueIDs);
        mMultiSelectManager.deleteAnnots(pageIndex, annots, groupMaps, annotsRectF, addUndo, callback);
    }

    public void flattenAnnot(Annot annot, final Event.Callback callback) {
        try {
            int pageIndex = annot.getPage().getIndex();
            ArrayList<Annot> annots = new ArrayList<>();
            MarkupArray markupArray = ((Markup) annot).getGroupElements();
            long size = markupArray.getSize();
            RectF rectF = new RectF();
            Matrix matrix = mPdfViewCtrl.getDisplayMatrix(pageIndex);
            for (long i = 0; i < size; i++) {
                Annot groupAnnot = AppAnnotUtil.createAnnot(markupArray.getAt(i));
                if (groupAnnot == null) continue;
                RectF pvRect = AppUtil.toRectF(groupAnnot.getDeviceRect(AppUtil.toMatrix2D(matrix)));
                if (i == 0)
                    rectF.set(pvRect);
                else
                    rectF.union(pvRect);
                annots.add(groupAnnot);
            }
            mPdfViewCtrl.convertPageViewRectToPdfRect(rectF, rectF, pageIndex);
            flattenAnnots(annot.getPage().getIndex(), annots, rectF, callback);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void flattenAnnots(final int pageIndex,
                               final ArrayList<Annot> annots,
                               final RectF annotsRectF,
                               final Event.Callback callback) {
        mMultiSelectManager.flattenAnnots(pageIndex, annots, annotsRectF, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                if (MultiSelectConstants.FLATTEN_OK == event.mType) {
                    if (callback != null)
                        callback.result(null, success);
                }
            }
        });
    }

    private void unGroupAnnots(final int pageIndex,
                               ArrayList<Annot> annots,
                               final boolean addUndo,
                               final Event.Callback callback) {
        HashMap<String, ArrayList<String>> groupMaps = new HashMap<>();
        ArrayList<String> uniqueIDs = new ArrayList<>();
        for (Annot annot: annots) {
            uniqueIDs.add(AppAnnotUtil.getAnnotUniqueID(annot));
        }
        groupMaps.put(AppAnnotUtil.getAnnotUniqueID(annots.get(0)), uniqueIDs);
        mMultiSelectManager.groupAnnots(pageIndex, MultiSelectConstants.OPER_UNGROUP_ANNOTS, annots, groupMaps, addUndo, callback);
    }

    private void moveAnnots(final int pageIndex,
                            ArrayList<Annot> annots,
                            final RectF oldRect,
                            final RectF newRect,
                            final boolean addUndo, Event.Callback callback) {
        mMultiSelectManager.moveAnnots(pageIndex, annots, oldRect, newRect, addUndo, callback);
    }

    /*
     *  LT     T     RT
     *   1-----2-----3
     *   |	         |
     *   |	         |
     * L 8           4 R
     *   |           |
     *   |           |
     *   7-----6-----5
     *   LB    B     RB
     *   */

    public static final int CTR_NONE = -1;
    public static final int CTR_LT = 1;
    public static final int CTR_T = 2;
    public static final int CTR_RT = 3;
    public static final int CTR_R = 4;
    public static final int CTR_RB = 5;
    public static final int CTR_B = 6;
    public static final int CTR_LB = 7;
    public static final int CTR_L = 8;
    private int mCurrentCtr = CTR_NONE;

    public static final int OPER_DEFAULT = -1;
    public static final int OPER_SCALE_LT = 1;// old:start at 0
    public static final int OPER_SCALE_T = 2;
    public static final int OPER_SCALE_RT = 3;
    public static final int OPER_SCALE_R = 4;
    public static final int OPER_SCALE_RB = 5;
    public static final int OPER_SCALE_B = 6;
    public static final int OPER_SCALE_LB = 7;
    public static final int OPER_SCALE_L = 8;
    public static final int OPER_TRANSLATE = 9;
    private int mLastOper = OPER_DEFAULT;

    private float mCtlPtTouchExt = 20;
    private float mCtlPtDeltyXY = 20;// Additional refresh range

    private PointF mDocViewerPt = new PointF(0, 0);
    private RectF mPageViewRect = new RectF(0, 0, 0, 0);

    private RectF mPageDrawRect = new RectF();
    private RectF mAnnotMenuRect = new RectF(0, 0, 0, 0);

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent motionEvent, Annot annot) {
        try {
            // in pageView evX and evY
            PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);
            float evX = point.x;
            float evY = point.y;
            RectF selectRect = new RectF(MultiSelectUtils.getGroupRectF(mPdfViewCtrl, annot));
            mPdfViewCtrl.convertPdfRectToPageViewRect(selectRect, selectRect, pageIndex);

            int action = motionEvent.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == mUiExtensionsManager.getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()
                            && mGroupAnnots.size() > 1) {
                        mThickness = thicknessOnPageView(pageIndex, mSelectAreaBound);
                        mPageViewRect.set(selectRect);
                        mPageViewRect.inset(mThickness / 2f, mThickness / 2f);

                        if (mMoveState == MultiSelectConstants.STATE_DRAG_MOVE) {
                            mCurrentCtr = isTouchControlPoint(selectRect, evX, evY);
                        }

                        mDownPoint.set(evX, evY);
                        mLastPoint.set(evX, evY);
                        mDocViewerPt.set(motionEvent.getX(), motionEvent.getY());

                        if (mCurrentCtr == CTR_LT) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_LT;
                            return true;
                        } else if (mCurrentCtr == CTR_T) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_T;
                            return true;
                        } else if (mCurrentCtr == CTR_RT) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_RT;
                            return true;
                        } else if (mCurrentCtr == CTR_R) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_R;
                            return true;
                        } else if (mCurrentCtr == CTR_RB) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_RB;
                            return true;
                        } else if (mCurrentCtr == CTR_B) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_B;
                            return true;
                        } else if (mCurrentCtr == CTR_LB) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_LB;
                            return true;
                        } else if (mCurrentCtr == CTR_L) {
                            mTouchCaptured = true;
                            mLastOper = OPER_SCALE_L;
                            return true;
                        } else if (isHitSelectRect(selectRect, point)) {
                            mTouchCaptured = true;
                            mLastOper = OPER_TRANSLATE;
                            return true;
                        }
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (mMoveState != MultiSelectConstants.STATE_NONE
                            && annot == mUiExtensionsManager.getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()
                            && mTouchCaptured
                            && mGroupAnnots.size() > 1
                            && mUiExtensionsManager.getDocumentManager().canAddAnnot()) {
                        if (evX != mLastPoint.x && evY != mLastPoint.y) {
                            float deltaXY = mCtlPtLineWidth + mCtlPtRadius * 2 + 2;// Judging border value
                            switch (mLastOper) {
                                case OPER_TRANSLATE: {
                                    mInvalidateRect.set(selectRect);
                                    mAnnotMenuRect.set(selectRect);
                                    mInvalidateRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                    mAnnotMenuRect.offset(evX - mDownPoint.x, evY - mDownPoint.y);
                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                    mInvalidateRect.union(mAnnotMenuRect);

                                    mInvalidateRect.inset(-deltaXY - mCtlPtDeltyXY, -deltaXY - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotationMenu.isShowing()) {
                                        mAnnotationMenu.dismiss();
                                        mAnnotationMenu.update(mAnnotMenuRect);
                                    }

                                    mLastPoint.set(evX, evY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    break;
                                }
                                case OPER_SCALE_LT: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(evX, evY, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_T: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, evY, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RT: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {

                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.y, mLastPoint.x, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, evY, evX, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);

                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }
                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_R: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, evX, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_RB: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mLastPoint.y);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, evX, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_B: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, mPageViewRect.right, evY);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_LB: {
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
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_SCALE_L: {
                                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(evX, mPageViewRect.top, mPageViewRect.right, mPageViewRect.bottom);
                                        mInvalidateRect.sort();
                                        mAnnotMenuRect.sort();
                                        mInvalidateRect.union(mAnnotMenuRect);
                                        mInvalidateRect.inset(-mThickness - mCtlPtDeltyXY, -mThickness - mCtlPtDeltyXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                        mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                        PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                        if (mAnnotationMenu.isShowing()) {
                                            mAnnotationMenu.dismiss();
                                            mAnnotationMenu.update(mAnnotMenuRect);
                                        }

                                        mLastPoint.set(evX, evY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
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
                    boolean ret = false;
                    if (mTouchCaptured
                            && mGroupAnnots.size() > 1
                            && annot == mUiExtensionsManager.getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()) {
                        RectF pageViewRect = new RectF(selectRect);
                        pageViewRect.inset(mThickness / 2, mThickness / 2);
                        switch (mLastOper) {
                            case OPER_TRANSLATE: {
                                mPageDrawRect.set(pageViewRect);
                                mPageDrawRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                break;
                            }
                            case OPER_SCALE_LT: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, mLastPoint.y, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_T: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, mLastPoint.y, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_RT: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, mLastPoint.y, mLastPoint.x, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_R: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, pageViewRect.bottom);
                                }
                                break;
                            }
                            case OPER_SCALE_RB: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, mLastPoint.x, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_B: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(pageViewRect.left, pageViewRect.top, pageViewRect.right, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_LB: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, mLastPoint.y);
                                }
                                break;
                            }
                            case OPER_SCALE_L: {
                                if (!mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                                    mPageDrawRect.set(mLastPoint.x, pageViewRect.top, pageViewRect.right, pageViewRect.bottom);
                                }
                                break;
                            }
                            default:
                                break;
                        }

                        RectF viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
                        viewDrawBox.inset(-thicknessOnPageView(pageIndex, mSelectAreaBound) / 2, -thicknessOnPageView(pageIndex, mSelectAreaBound) / 2);
                        MultiSelectUtils.normalize(viewDrawBox);

                        if (mLastOper != OPER_DEFAULT && !mDownPoint.equals(mLastPoint.x, mLastPoint.y)) {
                            RectF bboxRect = new RectF(viewDrawBox);
                            mLastGroupAnnotsRect.set(selectRect);
                            moveAnnots(pageIndex, mGroupAnnots, mLastGroupAnnotsRect, bboxRect, true, null);
                            mPdfViewCtrl.convertPageViewRectToPdfRect(bboxRect, mGroupAnnotsRect, pageIndex);
                        }
                        mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
                        if (mAnnotationMenu.isShowing()) {
                            mAnnotationMenu.update(viewDrawBox);
                        } else {
                            mAnnotationMenu.show(viewDrawBox);
                        }
                        ret = true;
                    }

                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    mLastOper = OPER_DEFAULT;
                    mCurrentCtr = CTR_NONE;
                    return ret;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
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

    private RectF mPageViewThickness = new RectF(0, 0, 0, 0);

    private float thicknessOnPageView(int pageIndex, float thickness) {
        mPageViewThickness.set(0, 0, thickness, thickness);
        mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewThickness, mPageViewThickness, pageIndex);
        return Math.abs(mPageViewThickness.width());
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
     *   1-----2-----3
     *   |	         |
     *   |	         |
     *   8           4
     *   |           |
     *   |           |
     *   7-----6-----5
     *   */
    private RectF mMapBounds = new RectF();

    private PointF[] calculateControlPoints(RectF rect) {
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

    private boolean isHitSelectRect(RectF rectF, PointF point) {
        return rectF.contains(point.x, point.y);
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return onSingleTapOrLongPress(pageIndex, motionEvent, annot);
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return onSingleTapOrLongPress(pageIndex, motionEvent, annot);
    }

    private boolean onSingleTapOrLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        PointF pageViewPt = AppAnnotUtil.getPageViewPoint(mPdfViewCtrl, pageIndex, motionEvent);
        try {
            if (annot == mUiExtensionsManager.getDocumentManager().getCurrentAnnot()) {
                if (pageIndex == annot.getPage().getIndex() && isHitAnnot(annot, pageViewPt)) {
                    return true;
                } else {
                    mUiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
                    return true;
                }
            } else {
                mUiExtensionsManager.getDocumentManager().setCurrentAnnot(annot);
                return true;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean shouldViewCtrlDraw(Annot annot) {
        return true;
    }

    private RectF mViewDrawRect = new RectF(0, 0, 0, 0);
    private RectF mDocViewerBBox = new RectF(0, 0, 0, 0);

    public void onDrawForControls(Canvas canvas) {
        Annot curAnnot = mUiExtensionsManager.getDocumentManager().getCurrentAnnot();
        if (curAnnot != null
                && mGroupAnnots.size() > 1
                && mUiExtensionsManager.getCurrentAnnotHandler() == this) {

            int[] visiblePages = mPdfViewCtrl.getVisiblePages();
            boolean isPageVisible = false;
            for (int visibleIndex : visiblePages) {
                if (visibleIndex == mLastPageIndex) {
                    isPageVisible = true;
                    break;
                }
            }
            if (isPageVisible) {
                float thickness = thicknessOnPageView(mLastPageIndex, mSelectAreaBound);
                RectF selectAnnotsRect = new RectF(mGroupAnnotsRect);
                mPdfViewCtrl.convertPdfRectToPageViewRect(selectAnnotsRect, selectAnnotsRect, mLastPageIndex);
                mViewDrawRect.set(selectAnnotsRect);
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
                    mDocViewerBBox.set(selectAnnotsRect);
                    float dx = mLastPoint.x - mDownPoint.x;
                    float dy = mLastPoint.y - mDownPoint.y;

                    mDocViewerBBox.offset(dx, dy);
                }

                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mDocViewerBBox, mDocViewerBBox, mLastPageIndex);
                mAnnotationMenu.update(mDocViewerBBox);
                if (mAnnotationMenu.isShowing())
                    mAnnotationMenu.update(mDocViewerBBox);
                else
                    mAnnotationMenu.show(mDocViewerBBox);
            } else {
                if (mAnnotationMenu.isShowing())
                    mAnnotationMenu.dismiss();
            }
        }
    }

    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        Annot annot = mUiExtensionsManager.getDocumentManager().getCurrentAnnot();
        if (annot == null
                || !AppAnnotUtil.isSupportAnnotGroup(annot)
                || !AppAnnotUtil.isGrouped(annot)
                || mUiExtensionsManager.getCurrentAnnotHandler() != this)
            return;

        try {
            int annotPageIndex = annot.getPage().getIndex();
            if (annotPageIndex != pageIndex)
                return;
        } catch (PDFException e) {
            e.printStackTrace();
        }

        if (mGroupAnnotsRect.left >= mGroupAnnotsRect.right || mGroupAnnotsRect.top <= mGroupAnnotsRect.bottom)
            return;
        RectF selectRect = new RectF(mGroupAnnotsRect);
        mPdfViewCtrl.convertPdfRectToPageViewRect(selectRect, selectRect, pageIndex);
        if (mMoveState == MultiSelectConstants.STATE_DRAG_MOVE) {
            onDraw_Drag_Move(pageIndex, canvas);
        } else { //if (mState == STATE_MOVE || mState == STATE_NONE)
            mBBoxInOnDraw.set(selectRect);
            if (mLastOper == OPER_DEFAULT) {
                float deltaXY = mCtlPtLineWidth;// Judging border value
                MultiSelectUtils.normalize(mPdfViewCtrl, pageIndex, mBBoxInOnDraw, deltaXY);
            }
            if (mMoveState == MultiSelectConstants.STATE_MOVE) {
                float dx = mLastPoint.x - mDownPoint.x;
                float dy = mLastPoint.y - mDownPoint.y;

                mBBoxInOnDraw.offset(dx, dy);
            }

            canvas.save();
            canvas.drawRect(mBBoxInOnDraw, mPaint);
            canvas.restore();
        }


    }

    private RectF mBBoxInOnDraw = new RectF();
    private RectF mViewDrawRectInOnDraw = new RectF();
    private DrawFilter mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private void onDraw_Drag_Move(int pageIndex, Canvas canvas) {
        if (mGroupAnnots.size() == 0) return;

        canvas.save();
        canvas.setDrawFilter(mDrawFilter);
        RectF selectRect = new RectF(mGroupAnnotsRect);
        mPdfViewCtrl.convertPdfRectToPageViewRect(selectRect, selectRect, pageIndex);
        mViewDrawRectInOnDraw.set(selectRect);
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
        if (mLastOper == OPER_TRANSLATE || mLastOper == OPER_DEFAULT) {// TRANSLATE or DEFAULT
            mBBoxInOnDraw.set(selectRect);
            if (mLastOper == OPER_DEFAULT) {
                float deltaXY = mCtlPtLineWidth + mCtlPtRadius * 2 + 2;// Judging border value
                MultiSelectUtils.normalize(mPdfViewCtrl, pageIndex, mBBoxInOnDraw, deltaXY);
            }
            float dx = mLastPoint.x - mDownPoint.x;
            float dy = mLastPoint.y - mDownPoint.y;

            mBBoxInOnDraw.offset(dx, dy);
        }
        if (mGroupAnnots.size() > 1) {
            drawControlPoints(canvas, mBBoxInOnDraw, mSelectAreaColor);
            // add Control Imaginary
            drawControlImaginary(canvas, mBBoxInOnDraw);
        }
        canvas.restore();
    }

    private void drawControlPoints(Canvas canvas, RectF rectBBox, int color) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        for (PointF ctlPt : ctlPts) {
            mCtlPtPaint.setColor(Color.WHITE);
            mCtlPtPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
            mCtlPtPaint.setColor(color);
            mCtlPtPaint.setAlpha(255);
            mCtlPtPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
        }
    }

    private Path mImaginaryPath = new Path();

    private void drawControlImaginary(Canvas canvas, RectF rectBBox) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
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

    private void pathAddLine(Path path, float start_x, float start_y, float end_x, float end_y) {
        path.moveTo(start_x, start_y);
        path.lineTo(end_x, end_y);
    }

}
