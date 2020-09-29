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
package com.foxit.uiextensions.annots.stamp;

import android.content.Context;
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
import android.widget.TextView;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Stamp;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.AnnotContent;
import com.foxit.uiextensions.annots.AnnotHandler;
import com.foxit.uiextensions.annots.common.EditAnnotEvent;
import com.foxit.uiextensions.annots.common.EditAnnotTask;
import com.foxit.uiextensions.annots.common.UIAnnotFlatten;
import com.foxit.uiextensions.annots.common.UIAnnotReply;
import com.foxit.uiextensions.annots.multiselect.GroupManager;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.math.BigDecimal;
import java.util.ArrayList;


public class StampAnnotHandler implements AnnotHandler {
    private static final int DEFAULT_ROTATE_LINE_LENGTH = 100;
    private static final int DEFAULT_ROTATE_LINE_WIDTH = 2;

    private AnnotMenu mAnnotMenu;
    private ArrayList<Integer> mMenuItems;
    private Annot mBitmapAnnot;
    private boolean mIsModify;
    private Context mContext;
    private int mBBoxSpace;
    private boolean mTouchCaptured = false;
    private PointF mDownPoint;
    private PointF mLastPoint;
    private PointF mRotateStartPoint;
    private PointF mRotateEndPoint;
    private RectF tempUndoBBox;
    private int tempRotate;
    private int mAnnotRotation;

    private float mCtlPtLineWidth = 2;
    private float mCtlPtRadius = 5;////
    private float mCtlPtTouchExt = 20;
    private float mCtlPtDeltyXY = 20;

    public static final int CTR_NONE = -1;
    public static final int CTR_LT = 1;
    public static final int CTR_RT = 2;
    public static final int CTR_RB = 3;
    public static final int CTR_LB = 4;
    public static final int CTR_ROTATE = 5;

    private int mCurrentCtr = CTR_NONE;

    public static final int OPER_DEFAULT = -1;
    public static final int OPER_SCALE_LT = 1;// old:start at 0
    public static final int OPER_SCALE_RT = 2;
    public static final int OPER_SCALE_RB = 3;
    public static final int OPER_SCALE_LB = 4;
    public static final int OPER_TRANSLATE = 5;
    public static final int OPER_ROTATE = 6;

    private int mLastOper = OPER_DEFAULT;

    private Paint mCtlPtPaint;
    private Paint mFrmPaint;
    private Paint mRotatePaint;

    private PointF mDocViewerPt = new PointF(0, 0);
    private RectF mPageViewRect = new RectF(0, 0, 0, 0);

    private RectF mPageDrawRect = new RectF();
    private RectF mInvalidateRect = new RectF(0, 0, 0, 0);
    private RectF mAnnotMenuRect = new RectF(0, 0, 0, 0);

    private float mThickness = 0f;

    private PDFViewCtrl mPdfViewCtrl;
    private ViewGroup mParent;
    private TextView mTvRotate;
    int mDefaultPaintColor; //ux_color_blue_ff179cd8

    public interface OnPepStampListener {
        void onOpen(Annot conent);
        void onDelete(Annot conent);
        void onModify(Annot annot,int pageIndex);
    }

    public void setOnPepStampListener(OnPepStampListener onPepStampListener) {
        this.onPepStampListener = onPepStampListener;
    }

    private OnPepStampListener onPepStampListener;

    public StampAnnotHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;

        mDownPoint = new PointF();
        mLastPoint = new PointF();
        mRotateStartPoint = new PointF();
        mRotateEndPoint = new PointF();

        mDefaultPaintColor = AppResource.getColor(context, R.color.ux_color_blue_ff179cd8, null);

        mMenuItems = new ArrayList<Integer>();

        mCtlPtPaint = new Paint();

        mRotatePaint = new Paint();
        mRotatePaint.setStyle(Paint.Style.STROKE);
        mRotatePaint.setColor(mDefaultPaintColor);
        mRotatePaint.setAntiAlias(true);
        mRotatePaint.setDither(true);
        mRotatePaint.setStrokeWidth(DEFAULT_ROTATE_LINE_WIDTH);

        PathEffect effect = AppAnnotUtil.getAnnotBBoxPathEffect();
        mFrmPaint = new Paint();
        mFrmPaint.setPathEffect(effect);
        mFrmPaint.setStyle(Paint.Style.STROKE);
        mFrmPaint.setAntiAlias(true);
        mBBoxSpace = AppAnnotUtil.getAnnotBBoxSpace();
        mBitmapAnnot = null;

        mCtlPtRadius = AppDisplay.getInstance(context).dp2px(5);
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

    @Override
    public void onAnnotSelected(final Annot annot, boolean reRender) {
        try {
            mAnnotRotation = ((Stamp) annot).getRotation();
            tempRotate = ((Stamp) annot).getRotation();
            tempUndoBBox = AppUtil.toRectF(annot.getRect());

            mBitmapAnnot = annot;
            RectF _rect = new RectF(tempUndoBBox);
            mPageViewRect.set(_rect);

            mAnnotMenu.dismiss();
            mMenuItems.clear();
            if (!((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
                mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
            } else {
                mMenuItems.add(AnnotMenu.AM_BT_COMMENT);
                mMenuItems.add(AnnotMenu.AM_BT_REPLY);
                mMenuItems.add(AnnotMenu.AM_BT_FLATTEN);
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
            RectF viewRect = new RectF(_rect);
            final RectF modifyRectF = new RectF(viewRect);
            final int pageIndex = annot.getPage().getIndex();
            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewRect, viewRect, pageIndex);
            mAnnotMenu.show(viewRect);

            // change modify status
            if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(modifyRectF, modifyRectF, pageIndex);
                refresh(pageIndex, AppDmUtil.rectFToRect(modifyRectF));
                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    mBitmapAnnot = annot;
                }

            } else {
                mBitmapAnnot = annot;
            }
            mIsModify = false;

            if (mTvRotate != null) {
                mParent.removeView(mTvRotate);
            }
            mTvRotate = new TextView(mContext);
            mTvRotate.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mTvRotate.setSingleLine(true);
            mTvRotate.setText("");
            mTvRotate.setTextColor(mDefaultPaintColor);
            float textSize = mContext.getResources().getDimension(R.dimen.ux_text_size_4sp);
            if (AppDisplay.getInstance(mContext).isPad()) {
                textSize = mContext.getResources().getDimension(R.dimen.ux_text_size_16sp);
            }
            mTvRotate.setTextSize(textSize);
            mParent.addView(mTvRotate);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void refresh(int pageIndex, Rect rect) {
        try {
            PDFPage page = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getPage(pageIndex, false);
            RectF pageRect = new RectF(0, 0, page.getWidth(), page.getHeight());
            RectF pageViewRect = new RectF();
            mPdfViewCtrl.convertPdfRectToPageViewRect(pageRect, pageViewRect, pageIndex);
            boolean intersect = rect.intersect((int) pageViewRect.left, (int) pageViewRect.top, (int) pageViewRect.right, (int) pageViewRect.bottom);
            if (intersect) {
                mPdfViewCtrl.refresh(pageIndex, rect);
            }
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

                RectF pdfRect = AppUtil.toRectF(annot.getRect());
                final RectF viewRect = new RectF(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                if (mIsModify && reRender) {
                    if (tempUndoBBox.equals(pdfRect) && tempRotate == ((Stamp) annot).getRotation()) {
                        modifyAnnot(pageIndex, annot, pdfRect, ((Stamp) annot).getRotation(), annot.getContent(), false, false, null);
                    } else {
                        modifyAnnot(pageIndex, annot, pdfRect, ((Stamp) annot).getRotation(), annot.getContent(), true, true, null);
                    }
                } else if (mIsModify) {
                    ((Stamp) annot).setRotation(tempRotate);
                    annot.move(AppUtil.toFxRectF(tempUndoBBox));
                }
                mIsModify = false;
                if (mPdfViewCtrl.isPageVisible(pageIndex) && reRender) {
                    mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                    refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
                    mBitmapAnnot = null;
                    if (mTvRotate != null)
                        mParent.removeView(mTvRotate);
                    return;
                }
            }
            mBitmapAnnot = null;
            if (mTvRotate != null)
                mParent.removeView(mTvRotate);
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
            PDFPage page = annot.getPage();
            int pageIndex = page.getIndex();

            RectF bbox = new RectF(AppUtil.toRectF(annot.getRect()));
            String contents = annot.getContent();
            tempUndoBBox = new RectF(bbox);
            int rotate = ((Stamp) annot).getRotation();
            tempRotate = rotate;

            if (content.getBBox() != null)
                bbox = content.getBBox();
            if (content.getContents() != null) {
                contents = content.getContents();
            }
            modifyAnnot(pageIndex, annot, bbox, rotate, contents, true, addUndo, result);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }


    public void modifyAnnot(final int pageIndex, final Annot annot, RectF bbox, int rotate, String content, boolean isModifyJni, final boolean addUndo, final Event.Callback result) {
        try {
            final StampModifyUndoItem undoItem = new StampModifyUndoItem(mPdfViewCtrl);
            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = pageIndex;
            undoItem.mBBox = new RectF(bbox);
            undoItem.mContents = content;
            undoItem.mRotation = rotate;
            undoItem.mModifiedDate = AppDmUtil.currentDateToDocumentDate();
            undoItem.mIconName = ((Stamp) annot).getSubject();

            undoItem.mUndoBox = new RectF(tempUndoBBox);
            undoItem.mUndoRotation = tempRotate;
            undoItem.mUndoContent = annot.getContent();
            undoItem.mUndoIconName = ((Stamp) annot).getSubject();

            undoItem.mRedoBox = new RectF(bbox);
            undoItem.mRedoContent = content;
            undoItem.mRedoRotation = rotate;
            undoItem.mRedoIconName = ((Stamp) annot).getSubject();

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

                            try {
                                mAnnotRotation = ((Stamp) annot).getRotation();

                                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setHasModifyTask(false);
                                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                                    RectF rectF = new RectF(0, 0, mPdfViewCtrl.getPageViewWidth(pageIndex), mPdfViewCtrl.getPageViewHeight(pageIndex));
                                    refresh(pageIndex, AppDmUtil.rectFToRect(rectF));
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


            if (isModifyJni) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotModified(annot.getPage(), annot);
            }

            mIsModify = true;
            // step 3: update pageview
            if (!isModifyJni) {
                RectF annotRectF = AppUtil.toRectF(annot.getRect());
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());

                    mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                    annotRectF.inset(-thickness - mCtlPtRadius - mCtlPtDeltyXY, -thickness - mCtlPtRadius - mCtlPtDeltyXY);
                    refresh(pageIndex, AppDmUtil.rectFToRect(annotRectF));
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private RectF mThicknessRectF = new RectF();

    private float thicknessOnPageView(int pageIndex, float thickness) {
        mThicknessRectF.set(0, 0, thickness, thickness);
        mPdfViewCtrl.convertPdfRectToPageViewRect(mThicknessRectF, mThicknessRectF, pageIndex);
        return Math.abs(mThicknessRectF.width());
    }

    @Override
    public void removeAnnot(Annot annot, boolean addUndo, Event.Callback result) {
        delAnnot(annot, addUndo, result);
    }

    private RectF mRotateInvalidateRect = new RectF();

    @Override
    public boolean onTouchEvent(int pageIndex, MotionEvent e, Annot annot) {

        PointF point = new PointF(e.getX(), e.getY());
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);

        float envX = point.x;
        float envY = point.y;
        int action = e.getAction();
        try {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()) {
                        RectF pageViewBBox = AppUtil.toRectF(annot.getRect());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                        RectF pdfRect = AppUtil.toRectF(annot.getRect());
                        mPageViewRect.set(pdfRect.left, pdfRect.top, pdfRect.right, pdfRect.bottom);
                        mPdfViewCtrl.convertPdfRectToPageViewRect(mPageViewRect, mPageViewRect, pageIndex);
                        mPageViewRect.inset(mThickness / 2f, mThickness / 2f);
                        int rotate = ((mPdfViewCtrl.getViewRotation() + mPdfViewCtrl.getDoc().getPage(pageIndex).getRotation()) * 90 + ((Stamp) annot).getRotation()) % 360;
                        mCurrentCtr = isTouchControlPoint(pageViewBBox, envX, envY, rotate, pageIndex);
                        mDownPoint.set(envX, envY);
                        mLastPoint.set(envX, envY);
                        mRotateEndPoint.set(envX, envY);

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
                        } else if (mCurrentCtr == CTR_ROTATE) {
                            mTouchCaptured = true;
                            mLastOper = OPER_ROTATE;

                            mRotateStartPoint.set(calculateStartRotatePoint(pageViewBBox, rotate, pageIndex));
                            mRotateStartCornerPoints = calculateRotateStartCornerPoints(pageViewBBox, rotate);

                            PointF cornerPoint = mRotateStartCornerPoints[0];
                            PointF centerPoint = new PointF(pageViewBBox.centerX(), pageViewBBox.centerY());
                            double rotateRadius = Math.sqrt(Math.pow(Math.abs(cornerPoint.x - centerPoint.x), 2) + Math.pow(Math.abs(cornerPoint.y - centerPoint.y), 2));
                            mRotateInvalidateRect.set((float) (centerPoint.x - rotateRadius), (float) (centerPoint.y - rotateRadius), (float) (centerPoint.x + rotateRadius), (float) (centerPoint.y + rotateRadius));
                            return true;
                        } else if (isHitAnnot(annot, point)) {
                            mTouchCaptured = true;
                            mLastOper = OPER_TRANSLATE;
                            return true;
                        }
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (mTouchCaptured && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()
                            && pageIndex == annot.getPage().getIndex()
                            && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().canAddAnnot()) {
                        if (envX != mLastPoint.x && envY != mLastPoint.y) {
                            RectF pageViewBBox = AppUtil.toRectF(annot.getRect());
                            mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewBBox, pageViewBBox, pageIndex);
                            float deltaXY = mCtlPtLineWidth + mCtlPtRadius * 2 + 2;// Judging border value
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
                                case OPER_ROTATE: {
                                    mAnnotMenuRect.set(pageViewBBox);

                                    mInvalidateRect.set(mRotateInvalidateRect);
                                    mInvalidateRect.union(mAnnotMenuRect);
                                    mInvalidateRect.union(mRotateEndPoint.x, mRotateEndPoint.y);
                                    mInvalidateRect.inset(-deltaXY - mCtlPtDeltyXY, -deltaXY - mCtlPtDeltyXY);
                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mInvalidateRect, mInvalidateRect, pageIndex);
                                    mPdfViewCtrl.invalidate(AppDmUtil.rectFToRect(mInvalidateRect));

                                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mAnnotMenuRect, mAnnotMenuRect, pageIndex);
                                    if (mAnnotMenu.isShowing()) {
                                        mAnnotMenu.dismiss();
//                                        mAnnotMenu.update(mAnnotMenuRect);
                                    }
                                    PointF adjustXY = adjustScalePointF(pageIndex, mAnnotMenuRect, deltaXY);
                                    mLastPoint.set(envX, envY);
                                    mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    mRotateEndPoint.set(envX, envY);
                                    break;
                                }
                                case OPER_SCALE_LT: {
                                    float viewLeft = mPageViewRect.left;
                                    float viewTop = mPageViewRect.top;
                                    float viewRight = mPageViewRect.right;
                                    float viewBottom = mPageViewRect.bottom;

                                    float k = (viewTop - viewBottom) / (viewLeft - viewRight);
                                    float b = (viewBottom * viewLeft - viewTop * viewRight) / (viewLeft - viewRight);
                                    float y = k * envX + b;

                                    float maxY = mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY;
                                    if (envX != mLastPoint.x && envY != mLastPoint.y && y > deltaXY && y < maxY) {
                                        mInvalidateRect.set(mLastPoint.x, mLastPoint.x * k + b, mPageViewRect.right, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(envX, envY, mPageViewRect.right, mPageViewRect.bottom);
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

                                        mLastPoint.set(envX, envY);
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
                                    float y = k * envX + b;

                                    float maxY = mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY;
                                    if (envX != mLastPoint.x && envY != mLastPoint.y && y > deltaXY && y < maxY) {
                                        mInvalidateRect.set(mPageViewRect.left, mLastPoint.x * k + b, mLastPoint.x, mPageViewRect.bottom);
                                        mAnnotMenuRect.set(mPageViewRect.left, envY, envX, mPageViewRect.bottom);
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

                                        mLastPoint.set(envX, envY);
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
                                    float y = k * envX + b;

                                    if (envX != mLastPoint.x && envY != mLastPoint.y && (y + deltaXY) < mPdfViewCtrl.getPageViewHeight(pageIndex) && y > deltaXY) {
                                        mInvalidateRect.set(mPageViewRect.left, mPageViewRect.top, mLastPoint.x, mLastPoint.x * k + b);
                                        mAnnotMenuRect.set(mPageViewRect.left, mPageViewRect.top, envX, envY);
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

                                        mLastPoint.set(envX, envY);
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
                                    float y = k * envX + b;

                                    if (envX != mLastPoint.x && envY != mLastPoint.y && (y + deltaXY) < mPdfViewCtrl.getPageViewHeight(pageIndex) && y > deltaXY) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mLastPoint.x * k + b);
                                        mAnnotMenuRect.set(envX, mPageViewRect.top, mPageViewRect.right, envY);
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

                                        mLastPoint.set(envX, envY);
                                        mLastPoint.offset(adjustXY.x, adjustXY.y);
                                    }
                                    break;
                                }
                                case OPER_DEFAULT:
                                    if (envX != mLastPoint.x && envY != mLastPoint.y) {
                                        mInvalidateRect.set(mLastPoint.x, mPageViewRect.top, mPageViewRect.right, mLastPoint.y);
                                        mAnnotMenuRect.set(envX, mPageViewRect.top, mPageViewRect.right, envY);
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
                                        mLastPoint.set(envX, envY);
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
                    if (mTouchCaptured && annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot() && pageIndex == annot.getPage().getIndex()) {
                        com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
                        RectF pageViewRect = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
                        mPdfViewCtrl.convertPdfRectToPageViewRect(pageViewRect, pageViewRect, pageIndex);
                        pageViewRect.inset(mThickness / 2, mThickness / 2);
                        switch (mLastOper) {
                            case OPER_TRANSLATE: {
                                mPageDrawRect.set(pageViewRect);
                                mPageDrawRect.offset(mLastPoint.x - mDownPoint.x, mLastPoint.y - mDownPoint.y);
                                break;
                            }
                            case OPER_ROTATE: {
                                if (mTvRotate != null)
                                    mTvRotate.setText("");
                                mPageDrawRect.set(pageViewRect);
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
                            int rotate;
                            RectF viewDrawBox = new RectF();
                            if (mLastOper == OPER_ROTATE && mRotateStartCornerPoints != null) {

                                PointF startPointF1 = mRotateStartCornerPoints[0];
                                PointF startPointF2 = mRotateStartCornerPoints[1];
                                PointF startPointF3 = mRotateStartCornerPoints[2];

                                double len1 = Math.sqrt(Math.pow(Math.abs(startPointF1.x - startPointF2.x), 2) + Math.pow(Math.abs(startPointF1.y - startPointF2.y), 2));
                                double len2 = Math.sqrt(Math.pow(Math.abs(startPointF2.x - startPointF3.x), 2) + Math.pow(Math.abs(startPointF2.y - startPointF3.y), 2));
                                double scale = len2 / len1;

                                int rotation = ((mPdfViewCtrl.getViewRotation() + mPdfViewCtrl.getDoc().getPage(pageIndex).getRotation()) * 90 + mAnnotRotation) % 360;
                                if (mRotateAngle <= 360 - rotation) {
                                    rotate = (int) (mRotateAngle + 0.5) + rotation;
                                } else {
                                    rotate = rotation - (360 - (int) (mRotateAngle + 0.5));
                                }

                                RectF rectF = new RectF(0, 0, 0, 0);
                                RectF annotRectF = AppUtil.toRectF(annot.getRect());
                                mPdfViewCtrl.convertPdfRectToPageViewRect(annotRectF, annotRectF, pageIndex);
                                double _width;
                                double _height;
                                double radians = Math.toRadians(rotate);
                                if (rotate >= 0 && rotate <= 90 || rotate >= 180 && rotate <= 270) {
                                    _width = len1 * ((Math.cos(radians)) + Math.sin(radians) * scale);
                                    _height = len1 * ((Math.sin(radians)) + Math.cos(radians) * scale);
                                } else {
                                    _width = len1 * (Math.sin(radians) * scale - (Math.cos(radians)));
                                    _height = len1 * ((Math.sin(radians)) - Math.cos(radians) * scale);
                                }

                                rectF.set(annotRectF.centerX() - (float) _width / 2,
                                        annotRectF.centerY() - (float) _height / 2,
                                        annotRectF.centerX() + (float) _width / 2,
                                        annotRectF.centerY() + (float) _height / 2);
                                viewDrawBox.set(rectF);
                                rotate = getRotation(rotate, pageIndex);
                            } else {
                                viewDrawBox = new RectF(mPageDrawRect.left, mPageDrawRect.top, mPageDrawRect.right, mPageDrawRect.bottom);
                                rotate = mAnnotRotation;
                            }
                            RectF bboxRect = new RectF();
                            mPdfViewCtrl.convertPageViewRectToPdfRect(viewDrawBox, bboxRect, pageIndex);

                            modifyAnnot(pageIndex, annot, bboxRect, rotate, annot.getContent(), true, false, null);
                            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(viewDrawBox, viewDrawBox, pageIndex);
                            if (mAnnotMenu.isShowing()) {
                                mAnnotMenu.update(viewDrawBox);
                            } else {
                                mAnnotMenu.show(viewDrawBox);
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
                        mRotateStartPoint.set(0, 0);
                        mRotateEndPoint.set(0, 0);
                        mRotateAngle = -1;
                        mRotateStartCornerPoints = null;
                        mRotateEndCornerPoints = null;
                        mLastOper = OPER_DEFAULT;
                        mCurrentCtr = CTR_NONE;
                        return true;
                    }
                    mTouchCaptured = false;
                    mDownPoint.set(0, 0);
                    mLastPoint.set(0, 0);
                    mRotateStartPoint.set(0, 0);
                    mRotateEndPoint.set(0, 0);
                    mRotateAngle = -1;
                    mRotateStartCornerPoints = null;
                    mRotateEndCornerPoints = null;
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

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return onSingleTapOrLongpress(pageIndex, motionEvent, annot);
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent, Annot annot) {
        return onSingleTapOrLongpress(pageIndex, motionEvent, annot);
    }

    private boolean onSingleTapOrLongpress(int pageIndex, MotionEvent motionEvent, Annot annot) {
        try {
            mDocViewerPt.set(motionEvent.getX(), motionEvent.getY());//display view

            PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(point, point, pageIndex);

            mThickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
            com.foxit.sdk.common.fxcrt.RectF _rectF = annot.getRect();
            RectF _rect = new RectF(_rectF.getLeft(), _rectF.getTop(), _rectF.getRight(), _rectF.getBottom());
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

    private RectF mBBoxInOnDraw = new RectF();
    private RectF mViewDrawRectInOnDraw = new RectF();
    private DrawFilter mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

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

    private void drawControlPoints(Canvas canvas, RectF rectBBox) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mCtlPtPaint.setStrokeWidth(mCtlPtLineWidth);
        for (PointF ctlPt : ctlPts) {
            mCtlPtPaint.setColor(Color.WHITE);
            mCtlPtPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
            mCtlPtPaint.setColor(mDefaultPaintColor);
            mCtlPtPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(ctlPt.x, ctlPt.y, mCtlPtRadius, mCtlPtPaint);
        }
    }

    private Path mImaginaryPath = new Path();

    private void pathAddLine(Path path, float start_x, float start_y, float end_x, float end_y) {
        path.moveTo(start_x, start_y);
        path.lineTo(end_x, end_y);
    }

    private void drawControlImaginary(Canvas canvas, RectF rectBBox) {
        PointF[] ctlPts = calculateControlPoints(rectBBox);
        mFrmPaint.setStrokeWidth(mCtlPtLineWidth);
        mFrmPaint.setColor(mDefaultPaintColor);
        mImaginaryPath.reset();
        // set path
        pathAddLine(mImaginaryPath, ctlPts[0].x + mCtlPtRadius, ctlPts[0].y, ctlPts[1].x - mCtlPtRadius, ctlPts[1].y);
        pathAddLine(mImaginaryPath, ctlPts[1].x, ctlPts[1].y + mCtlPtRadius, ctlPts[2].x, ctlPts[2].y - mCtlPtRadius);
        pathAddLine(mImaginaryPath, ctlPts[2].x - mCtlPtRadius, ctlPts[2].y, ctlPts[3].x + mCtlPtRadius, ctlPts[3].y);
        pathAddLine(mImaginaryPath, ctlPts[3].x, ctlPts[3].y - mCtlPtRadius, ctlPts[0].x, ctlPts[0].y + mCtlPtRadius);

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
                float thickness = thicknessOnPageView(pageIndex, annot.getBorderInfo().getWidth());
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
                } else if (mLastOper == OPER_ROTATE) {
                    mBBoxInOnDraw = AppUtil.toRectF(annot.getRect());
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mBBoxInOnDraw, mBBoxInOnDraw, pageIndex);
                }

                if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
                    //draw rotate point
                    drawControlPoints(canvas, mBBoxInOnDraw);
                    // add Control Imaginary
                    drawControlImaginary(canvas, mBBoxInOnDraw);

                    if (mLastOper == OPER_TRANSLATE) {
                        mRotateStartPointPath.reset();
                    } else {
//                    add rotate start point
                        int rotate = ((mPdfViewCtrl.getViewRotation() + mPdfViewCtrl.getDoc().getPage(pageIndex).getRotation()) * 90 + ((Stamp) annot).getRotation()) % 360;
                        drawRotateStartPointF(canvas, mBBoxInOnDraw, rotate, pageIndex);
                    }

                    if (mLastOper == OPER_ROTATE) {
                        //add rotate end point
                        drawRotateEndPointF(canvas, mBBoxInOnDraw);
                        // draw rotate rectangle
                        drawRotateEndCornerPath(canvas, mBBoxInOnDraw);
                        // draw rotate angle
                        drawRotateAngleText(canvas, mBBoxInOnDraw, pageIndex);
                    } else {
                        mRotateEndPointPath.reset();
                        mRotateEndCornerPath.reset();
                    }
                }
                canvas.restore();
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private StampToolHandler mToolHandler;

    public void setToolHandler(StampToolHandler toolHandler) {
        mToolHandler = toolHandler;
    }

    private void delAnnot(final Annot annot, final boolean addUndo, final Event.Callback result) {
        // step 1 : set current annot to null
        if (annot == ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().getCurrentAnnot()) {
            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setCurrentAnnot(null, false);
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

            final RectF viewRect = AppUtil.toRectF(annot.getRect());
            final int pageIndex = page.getIndex();
            final StampDeleteUndoItem undoItem = new StampDeleteUndoItem(mPdfViewCtrl);

            undoItem.setCurrentValue(annot);
            undoItem.mPageIndex = pageIndex;
            undoItem.mStampType = StampUntil.getStampTypeByName(undoItem.mSubject);
            undoItem.mIconName = ((Stamp) annot).getIconName();
            undoItem.mRotation = ((Stamp) annot).getRotation();
            if (AppAnnotUtil.isGrouped(annot))
                undoItem.mGroupNMList = GroupManager.getInstance().getGroupUniqueIDs(mPdfViewCtrl, annot);

            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotWillDelete(page, annot);
            StampEvent event = new StampEvent(EditAnnotEvent.EVENTTYPE_DELETE, undoItem, (Stamp) annot, mPdfViewCtrl);
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
                        if (undoItem.mGroupNMList.size() >= 2) {
                            ArrayList<String> newGroupList = new ArrayList<>(undoItem.mGroupNMList);
                            newGroupList.remove(undoItem.mNM);
                            if (newGroupList.size() >= 2)
                                GroupManager.getInstance().setAnnotGroup(mPdfViewCtrl, page, newGroupList);
                            else
                                GroupManager.getInstance().unGroup(page, newGroupList.get(0));
                        }

                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().onAnnotDeleted(page, annot);
                        if (addUndo) {
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().addUndoItem(undoItem);
                        }
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.convertPdfRectToPageViewRect(viewRect, viewRect, pageIndex);
                            refresh(pageIndex, AppDmUtil.rectFToRect(viewRect));
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

    private PointF mAdjustPointF = new PointF(0, 0);

    private PointF adjustScalePointF(int pageIndex, RectF rectF, float dxy) {
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

    private int isTouchControlPoint(RectF rect, float x, float y, int rotate, int pageIndex) {
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

        if (ret == -1) {
            PointF pointF = calculateStartRotatePoint(rect, rotate, pageIndex);
            area.set(pointF.x, pointF.y, pointF.x, pointF.y);
            area.inset(-mCtlPtTouchExt, -mCtlPtTouchExt);
            if (area.contains(x, y)) {
                ret = CTR_ROTATE;
            }
        }
        return ret;
    }

    private RectF mViewDrawRect = new RectF(0, 0, 0, 0);
    private RectF mDocViewerBBox = new RectF(0, 0, 0, 0);

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
                    if (mLastOper == OPER_TRANSLATE || mLastOper == OPER_ROTATE || mLastOper == OPER_DEFAULT) {
                        mDocViewerBBox = AppUtil.toRectF(curAnnot.getRect());
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

    private PointF calculateStartRotatePoint(RectF rectF, int rotate, int pageIndex) {
        PointF pointF = new PointF();

        float startLineLength = (float) calculateStartLineLength(rectF, rotate, pageIndex);
        PointF centerPointF = new PointF(rectF.centerX(), rectF.centerY());
        if (rotate == 0) {
            pointF.set(centerPointF.x, centerPointF.y - startLineLength);
        } else if (rotate < 90) {
            double degree = Math.toRadians(90 - rotate);
            double x = startLineLength * Math.cos(degree);
            double y = Math.sqrt(Math.pow(startLineLength, 2) - Math.pow(x, 2));
            pointF.set((float) (centerPointF.x + x), (float) (centerPointF.y - y));
        } else if (rotate == 90) {
            pointF.set(centerPointF.x + startLineLength, centerPointF.y);
        } else if (rotate < 180) {
            double degree = Math.toRadians(rotate - 90);
            double x = startLineLength * Math.cos(degree);
            double y = Math.sqrt(Math.pow(startLineLength, 2) - Math.pow(x, 2));
            pointF.set((float) (centerPointF.x + x), (float) (centerPointF.y + y));
        } else if (rotate == 180) {
            pointF.set(centerPointF.x, centerPointF.y + startLineLength);
        } else if (rotate < 270) {
            double degree = Math.toRadians(rotate - 180);
            double y = startLineLength * Math.cos(degree);
            double x = Math.sqrt(Math.pow(startLineLength, 2) - Math.pow(y, 2));
            pointF.set((float) (centerPointF.x - x), (float) (centerPointF.y + y));
        } else if (rotate == 270) {
            pointF.set(centerPointF.x - startLineLength, centerPointF.y);
        } else if (rotate < 360) {
            double degree = Math.toRadians(rotate - 270);
            double x = startLineLength * Math.cos(degree);
            double y = Math.sqrt(Math.pow(startLineLength, 2) - Math.pow(x, 2));
            pointF.set((float) (centerPointF.x - x), (float) (centerPointF.y - y));
        }
        return pointF;
    }

    private double calculateStartLineLength(RectF rectF, int rotate, int pageIndex) {
        int annotMenuPosition = mAnnotMenu.getShowingPosition();
        double startLineLength = DEFAULT_ROTATE_LINE_LENGTH;
        if (annotMenuPosition == AnnotMenu.SHOWING_CENTER) {
            return rectF.width() / 2;
        }

        float offset = mCtlPtRadius * 2;
        if (rotate >= 0 && rotate <= 45) {
            if (annotMenuPosition == AnnotMenu.SHOWING_TOP) {
                startLineLength = (rectF.height() / 2 - offset) / Math.cos(Math.toRadians(rotate));
            } else {
                double lavaLength = Math.abs(rectF.centerY() - offset) / Math.cos(Math.toRadians(rotate));
                if (lavaLength < DEFAULT_ROTATE_LINE_LENGTH) {
                    startLineLength = lavaLength;
                }
            }
        } else if (rotate >= 45 && rotate <= 90) {
            if (annotMenuPosition == AnnotMenu.SHOWING_RIGHT) {
                startLineLength = (rectF.width() / 2 - offset) / Math.sin(Math.toRadians(rotate));
            } else {
                double lavaLength = (mPdfViewCtrl.getPageViewWidth(pageIndex) - rectF.centerX() - offset) / Math.sin(Math.toRadians(rotate));
                if (lavaLength < DEFAULT_ROTATE_LINE_LENGTH) {
                    startLineLength = lavaLength;
                }
            }
        } else if (rotate >= 90 && rotate <= 135) {
            if (annotMenuPosition == AnnotMenu.SHOWING_RIGHT) {
                startLineLength = (rectF.width() / 2 - offset) / Math.cos(Math.toRadians(rotate - 90));
            } else {
                double lavaLength = (mPdfViewCtrl.getPageViewWidth(pageIndex) - rectF.centerX() - offset) / Math.cos(Math.toRadians(rotate - 90));
                if (lavaLength < DEFAULT_ROTATE_LINE_LENGTH) {
                    startLineLength = lavaLength;
                }
            }
        } else if (rotate >= 135 && rotate <= 180) {
            if (annotMenuPosition == AnnotMenu.SHOWING_BOTTOM) {
                startLineLength = (rectF.height() / 2 - offset) / Math.sin(Math.toRadians(rotate - 90));
            } else {
                double lavaLength = (mPdfViewCtrl.getPageViewHeight(pageIndex) - rectF.centerY() - offset) / Math.sin(Math.toRadians(rotate - 90));
                if (lavaLength < DEFAULT_ROTATE_LINE_LENGTH) {
                    startLineLength = lavaLength;
                }
            }
        } else if (rotate >= 180 && rotate <= 225) {
            if (annotMenuPosition == AnnotMenu.SHOWING_BOTTOM) {
                startLineLength = (rectF.height() / 2 - offset) / Math.cos(Math.toRadians(rotate - 180));
            } else {
                double lavaLength = (mPdfViewCtrl.getPageViewHeight(pageIndex) - rectF.centerY() - offset) / Math.cos(Math.toRadians(rotate - 180));
                if (lavaLength < DEFAULT_ROTATE_LINE_LENGTH) {
                    startLineLength = lavaLength;
                }
            }
        } else if (rotate >= 225 && rotate <= 270) {
            if (annotMenuPosition == AnnotMenu.SHOWING_LEFT) {
                startLineLength = (rectF.width() / 2 - offset) / Math.cos(Math.toRadians(270 - rotate));
            } else {
                double lavaLength = Math.abs(rectF.centerX() - offset) / Math.cos(Math.toRadians(270 - rotate));
                if (lavaLength < DEFAULT_ROTATE_LINE_LENGTH) {
                    startLineLength = lavaLength;
                }
            }
        } else if (rotate >= 270 && rotate <= 315) {
            if (annotMenuPosition == AnnotMenu.SHOWING_LEFT) {
                startLineLength = (rectF.width() / 2 - offset) / Math.cos(Math.toRadians(rotate - 270));
            } else {
                double lavaLength = (Math.abs(rectF.centerX()) - offset) / Math.cos(Math.toRadians(rotate - 270));
                if (lavaLength < DEFAULT_ROTATE_LINE_LENGTH) {
                    startLineLength = lavaLength;
                }
            }
        } else {
            if (annotMenuPosition == AnnotMenu.SHOWING_TOP) {
                startLineLength = (rectF.height() / 2 - offset) / Math.cos(Math.toRadians(rotate - 270));
            } else {
                double lavaLength = (Math.abs(rectF.centerX()) - offset) / Math.cos(Math.toRadians(rotate - 270));
                if (lavaLength < DEFAULT_ROTATE_LINE_LENGTH) {
                    startLineLength = lavaLength;
                }
            }

            if (annotMenuPosition == AnnotMenu.SHOWING_TOP) {
                startLineLength = (rectF.height() / 2 - offset) / Math.sin(Math.toRadians(rotate - 270));
            } else {
                double lavaLength = (Math.abs(rectF.centerY()) - offset) / Math.sin(Math.toRadians(rotate - 270));
                if (lavaLength < DEFAULT_ROTATE_LINE_LENGTH) {
                    startLineLength = lavaLength;
                }
            }
        }
        if (startLineLength < 30)
            startLineLength = 30;
        else if (startLineLength > DEFAULT_ROTATE_LINE_LENGTH)
            startLineLength = 100;
        return startLineLength;
    }

    private Path mRotateStartPointPath = new Path();
    private Path mRotateEndPointPath = new Path();
    private PointF[] mRotateStartCornerPoints = new PointF[4];
    private PointF[] mRotateEndCornerPoints = new PointF[4];

    private void drawRotateStartPointF(final Canvas canvas, final RectF rectBBox, int rotate, int pageIndex) {
        mRotateStartPointPath.reset();

        mRotatePaint.setStrokeWidth(mCtlPtLineWidth);
        mRotatePaint.setColor(mDefaultPaintColor);
        mRotatePaint.setStyle(Paint.Style.STROKE);
        final PointF rotatePoint = calculateStartRotatePoint(rectBBox, rotate, pageIndex);
        canvas.drawCircle(rotatePoint.x, rotatePoint.y, mCtlPtRadius, mRotatePaint);

        mRotatePaint.setStyle(Paint.Style.STROKE);
        PointF centerPointF = new PointF(rectBBox.centerX(), rectBBox.centerY());
        pathAddLine(mRotateStartPointPath, centerPointF.x, centerPointF.y, rotatePoint.x, rotatePoint.y);
        canvas.drawPath(mRotateStartPointPath, mRotatePaint);
    }

    private void drawRotateEndPointF(final Canvas canvas, final RectF rectBBox) {
        mRotateEndPointPath.reset();

        mRotatePaint.setStrokeWidth(mCtlPtLineWidth);
        mRotatePaint.setColor(mDefaultPaintColor);
        mRotatePaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(mRotateEndPoint.x, mRotateEndPoint.y, mCtlPtRadius, mRotatePaint);

        mRotatePaint.setStyle(Paint.Style.STROKE);
        PointF centerPointF = new PointF(rectBBox.centerX(), rectBBox.centerY());
        pathAddLine(mRotateEndPointPath, centerPointF.x, centerPointF.y, mRotateEndPoint.x, mRotateEndPoint.y);
        canvas.drawPath(mRotateEndPointPath, mRotatePaint);
    }

    private PointF[] calculateRotateStartCornerPoints(RectF rectF, int rotate) {
        PointF point1 = new PointF();
        PointF point2 = new PointF();
        PointF point3 = new PointF();
        PointF point4 = new PointF();
        if (rotate == 0) {
            point1.set(rectF.left, rectF.top);
            point2.set(rectF.right, rectF.top);
            point3.set(rectF.right, rectF.bottom);
            point4.set(rectF.left, rectF.bottom);
        } else if (rotate < 90) {
            double y;
            double x;
            if (rotate == 45) {
                x = rectF.width() / 4;
                y = rectF.height() / 4;
            } else {
                double degree = Math.toRadians(rotate);
                double tan = Math.tan(degree);
                y = (tan * rectF.width() - rectF.height()) / (Math.pow(tan, 2) - 1);
                x = tan * y;
            }
            point1.set((float) (rectF.left + x), rectF.top);
            point2.set(rectF.right, (float) (rectF.bottom - y));
            point3.set((float) (rectF.right - x), rectF.bottom);
            point4.set(rectF.left, (float) (rectF.top + y));
        } else if (rotate == 90) {
            point1.set(rectF.right, rectF.top);
            point2.set(rectF.right, rectF.bottom);
            point3.set(rectF.left, rectF.bottom);
            point4.set(rectF.left, rectF.top);
        } else if (rotate < 180) {
            double y;
            double x;
            rotate = rotate - 90;
            if (rotate == 45) {
                x = rectF.width() / 4 * 3;
                y = rectF.height() / 4 * 3;
            } else {
                double degree = Math.toRadians(rotate);
                double tan = Math.tan(degree);
                y = (tan * rectF.width() - rectF.height()) / (Math.pow(tan, 2) - 1);
                x = tan * y;
            }
            point1.set(rectF.right, (float) (rectF.bottom - y));
            point2.set((float) (rectF.right - x), rectF.bottom);
            point3.set(rectF.left, (float) (rectF.top + y));
            point4.set((float) (rectF.left + x), rectF.top);
        } else if (rotate == 180) {
            point1.set(rectF.right, rectF.bottom);
            point2.set(rectF.left, rectF.bottom);
            point3.set(rectF.left, rectF.top);
            point4.set(rectF.right, rectF.top);
        } else if (rotate < 270) {
            double y;
            double x;
            rotate = rotate - 180;
            if (rotate == 45) {
                x = rectF.width() / 4;
                y = rectF.height() / 4;
            } else {
                double degree = Math.toRadians(rotate);
                double tan = Math.tan(degree);
                y = (tan * rectF.width() - rectF.height()) / (Math.pow(tan, 2) - 1);
                x = tan * y;
            }
            point1.set((float) (rectF.right - x), rectF.bottom);
            point2.set(rectF.left, (float) (rectF.top + y));
            point3.set((float) (rectF.left + x), rectF.top);
            point4.set(rectF.right, (float) (rectF.bottom - y));
        } else if (rotate == 270) {
            point1.set(rectF.left, rectF.bottom);
            point2.set(rectF.left, rectF.top);
            point3.set(rectF.right, rectF.top);
            point4.set(rectF.right, rectF.bottom);
        } else if (rotate < 360) {
            double y;
            double x;
            rotate = rotate - 270;
            if (rotate == 45) {
                x = rectF.width() / 4 * 3;
                y = rectF.height() / 4 * 3;
            } else {
                double degree = Math.toRadians(rotate);
                double tan = Math.tan(degree);
                y = (tan * rectF.width() - rectF.height()) / (Math.pow(tan, 2) - 1);
                x = tan * y;
            }
            point1.set(rectF.left, (float) (rectF.top + y));
            point2.set((float) (rectF.left + x), rectF.top);
            point3.set(rectF.right, (float) (rectF.bottom - y));
            point4.set((float) (rectF.right - x), rectF.bottom);
        }
        return new PointF[]{point1, point2, point3, point4};
    }

    private double mRotateAngle;

    private PointF[] calculateRotateEndCornerPoints(RectF rectF) {
        float x_len = Math.abs(mRotateStartCornerPoints[0].x - rectF.centerX());
        float y_len = Math.abs(mRotateStartCornerPoints[0].y - rectF.centerY());
        double radius = Math.sqrt(x_len * x_len + y_len * y_len);
        double rotateAngle = calculateRotateAngle(new PointF(rectF.centerX(), rectF.centerY()), mRotateStartPoint, mRotateEndPoint);
        mRotateAngle = rotateAngle;

        PointF pointF1 = new PointF(mRotateStartCornerPoints[0].x, mRotateStartCornerPoints[0].y);
        PointF pointF2 = new PointF(mRotateStartCornerPoints[1].x, mRotateStartCornerPoints[1].y);
        PointF pointF3 = new PointF(mRotateStartCornerPoints[2].x, mRotateStartCornerPoints[2].y);
        PointF pointF4 = new PointF(mRotateStartCornerPoints[3].x, mRotateStartCornerPoints[3].y);

        PointF[] points = new PointF[]{pointF1, pointF2, pointF3, pointF4};
        swapPoints(points, new PointF(rectF.centerX(), rectF.centerY()), radius, rotateAngle);
        return points;
    }

    private void swapPoints(PointF[] pointFs, PointF center, double radius, double rotate) {
        for (PointF pointF : pointFs) {
            double degree = Math.toDegrees(Math.acos((pointF.x - center.x) / radius));

            if (pointF.y <= center.y) {
                double radians;
                if (degree - rotate >= 0) {
                    radians = Math.toRadians(degree - rotate);
                    pointF.x = (float) (radius * Math.cos(radians)) + center.x;
                    pointF.y = center.y - (float) (radius * Math.sin(radians));
                } else if (degree - rotate <= 0 && rotate <= 180) {
                    radians = Math.toRadians(rotate - degree);
                    pointF.x = (float) (radius * Math.cos(radians)) + center.x;
                    pointF.y = center.y + (float) (radius * Math.sin(radians));
                } else if (rotate > degree + 180) {
                    radians = Math.toRadians(360 - rotate + degree);
                    pointF.x = (float) (radius * Math.cos(radians)) + center.x;
                    pointF.y = center.y - (float) (radius * Math.sin(radians));
                } else {
                    radians = Math.toRadians(rotate - degree);
                    pointF.x = (float) (radius * Math.cos(radians)) + center.x;
                    pointF.y = center.y + (float) (radius * Math.sin(radians));
                }
            } else {
                double radians;
                if (180 - degree >= rotate) {
                    radians = Math.toRadians(degree + rotate);
                    pointF.x = (float) (radius * Math.cos(radians)) + center.x;
                    pointF.y = center.y + (float) (radius * Math.sin(radians));
                } else if (rotate > 180 - degree && rotate <= 180) {
                    radians = Math.toRadians(360 - degree - rotate);
                    pointF.x = (float) (radius * Math.cos(radians)) + center.x;
                    pointF.y = center.y - (float) (radius * Math.sin(radians));
                } else if (rotate <= 360 - degree && rotate >= 180) {
                    radians = Math.toRadians(360 - degree - rotate);
                    pointF.x = (float) (radius * Math.cos(radians)) + center.x;
                    pointF.y = center.y - (float) (radius * Math.sin(radians));
                } else {
                    radians = Math.toRadians(degree - (360 - rotate));
                    pointF.x = (float) (radius * Math.cos(radians)) + center.x;
                    pointF.y = center.y + (float) (radius * Math.sin(radians));
                }
            }
        }
    }

    private Path mRotateEndCornerPath = new Path();

    private void drawRotateEndCornerPath(final Canvas canvas, final RectF rectBBox) {
        mRotateEndCornerPath.reset();

        mRotateEndCornerPoints = calculateRotateEndCornerPoints(rectBBox);

        // set path
        pathAddLine(mRotateEndCornerPath, mRotateEndCornerPoints[0].x, mRotateEndCornerPoints[0].y, mRotateEndCornerPoints[1].x, mRotateEndCornerPoints[1].y);
        pathAddLine(mRotateEndCornerPath, mRotateEndCornerPoints[1].x, mRotateEndCornerPoints[1].y, mRotateEndCornerPoints[2].x, mRotateEndCornerPoints[2].y);
        pathAddLine(mRotateEndCornerPath, mRotateEndCornerPoints[2].x, mRotateEndCornerPoints[2].y, mRotateEndCornerPoints[3].x, mRotateEndCornerPoints[3].y);
        pathAddLine(mRotateEndCornerPath, mRotateEndCornerPoints[3].x, mRotateEndCornerPoints[3].y, mRotateEndCornerPoints[0].x, mRotateEndCornerPoints[0].y);
        canvas.drawPath(mRotateEndCornerPath, mRotatePaint);
    }

    private void drawRotateAngleText(final Canvas canvas, final RectF rectBBox, int pageIndex) {
        if (mTvRotate != null) {
            double rotate;
            if (mRotateAngle >= 0) {
                rotate = mRotateAngle;
            } else {
                rotate = calculateRotateAngle(new PointF(mBBoxInOnDraw.centerX(), mBBoxInOnDraw.centerY()), mRotateStartPoint, mRotateEndPoint);
            }
            BigDecimal bd = new BigDecimal(rotate).setScale(0, BigDecimal.ROUND_HALF_UP);

            int angle = bd.intValue();
            if (angle < 180)
                angle = -angle;
            else
                angle = 360 - angle;
            mTvRotate.setText(String.format("%d°", angle));

            PointF pointF = calculateAngleTextPosition(new PointF(rectBBox.centerX(), rectBBox.top), mTvRotate, pageIndex);
            mPdfViewCtrl.convertPageViewPtToDisplayViewPt(pointF, pointF, pageIndex);
            mTvRotate.setX(pointF.x);
            mTvRotate.setY(pointF.y);
        }
    }

    private PointF calculateAngleTextPosition(PointF pointF, TextView tvRotate, int pageIndex) {
        tvRotate.measure(0, 0);
        float fontWith = tvRotate.getWidth();
        float fontHeight = tvRotate.getHeight();

        PointF p = new PointF();
        p.x = pointF.x - fontWith / 2;
        p.y = pointF.y - fontHeight - 20;

        int space = 5;
        //the text on the left
        if ((pointF.x - fontWith / 2 - space) < 0) {
            p.x = fontWith / 2 + space;
        }
        //the text on the right
        if ((mPdfViewCtrl.getPageViewWidth(pageIndex) - pointF.x) - space < fontWith / 2) {
            p.x = pointF.x - (fontWith / 2 - (mPdfViewCtrl.getPageViewWidth(pageIndex) - pointF.x)) - space;
        }
        return p;
    }

    private double calculateRotateAngle(PointF center, PointF start, PointF end) {
        double startX = start.x - center.x;
        double startY = start.y - center.y;
        double endX = end.x - center.x;
        double endY = end.y - center.y;
        double bevel = (startX * endX) + (startY * endY);
        double startLen = Math.sqrt(startX * startX + startY * startY);
        double endLen = Math.sqrt(endX * endX + endY * endY);
        double cos = bevel / (startLen * endLen);
        double degree = Math.toDegrees(Math.acos(cos));

        float k = (start.y - center.y) / (start.x - center.x);
        float b = start.y - k * start.x;
        if (start.x > center.x && start.y < center.y) {
            if ((end.x * k + b - end.y) > 0) {
                degree = 360 - degree;
            }
        } else if (start.x < center.x && start.y < center.y) {
            if ((end.x * k + b - end.y) < 0) {
                degree = 360 - degree;
            }
        } else if (start.x < center.x && start.y > center.y) {
            if ((end.x * k + b - end.y) < 0) {
                degree = 360 - degree;
            }
        } else if (start.x > center.x && start.y > center.y) {
            if ((end.x * k + b - end.y) > 0) {
                degree = 360 - degree;
            }
        } else if (start.x == center.x) {
            if (start.y < center.y && end.x < center.x || start.y > center.y && end.x > center.x) {
                degree = 360 - degree;
            }
        } else if (start.y == center.y) {
            if (start.x < center.x && end.y > center.y || start.x > center.x && end.y < center.y) {
                degree = 360 - degree;
            }
        }
        return degree;
    }

    private int getRotation(int rotate, int pageIndex) {
        try {
            int delta = (mPdfViewCtrl.getViewRotation() + mPdfViewCtrl.getDoc().getPage(pageIndex).getRotation()) % 4;
            if (delta == 0) {
                return rotate;
            } else {
                int rotation = rotate + (4 - delta) * 90;
                if (rotation > 360) rotation -= 360;
                return rotation;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return rotate;
    }
}
