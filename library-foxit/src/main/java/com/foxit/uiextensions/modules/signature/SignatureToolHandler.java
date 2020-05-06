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
package com.foxit.uiextensions.modules.signature;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Progressive;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.dialog.FxProgressDialog;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UISaveAsDialog;
import com.foxit.uiextensions.controls.propertybar.AnnotMenu;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.AnnotMenuImpl;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.home.local.LocalModule;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.security.digitalsignature.DigitalSignatureModule;
import com.foxit.uiextensions.security.digitalsignature.DigitalSignatureUtil;
import com.foxit.uiextensions.security.digitalsignature.IDigitalSignatureCreateCallBack;
import com.foxit.uiextensions.utils.AppAnnotUtil;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.UIToast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class SignatureToolHandler implements ToolHandler {

	/*
     *  0---1---2
	 *  |       |
	 *  7       3
	 *  |       |
	 *  6---5---4
	 */

    private static final int CTR_NONE = -1;
    private static final int CTR_LEFT_TOP = 0;
    private static final int CTR_MID_TOP = 1;
    private static final int CTR_RIGHT_TOP = 2;
    private static final int CTR_RIGHT_MID = 3;
    private static final int CTR_RIGHT_BOTTOM = 4;
    private static final int CTR_MID_BOTTOM = 5;
    private static final int CTR_LEFT_BOTTOM = 6;
    private static final int CTR_LEFT_MID = 7;
    private int mCurrentCtr = CTR_NONE;

    private static final int OPER_DEFAULT = -1;
    private static final int OPER_TRANSLATE = 0;
    private static final int OPER_SCALE = 1;
    private int mLastOper = OPER_DEFAULT;

    private float mFrmLineWidth = 1;
    private float mCtlPtLineWidth = 4;
    private float mCtlPtRadius = 5;
    private float mCtlPtTouchExt = 20;

    private Paint mFrmPaint;
    private Paint mCtlPtPaint;
    private Paint mBitmapPaint;
    private boolean mTouchCaptured;
    private PointF mLastPoint;
    private RectF mBbox;

    private Context mContext;
    private ViewGroup mParent;
    private PDFViewCtrl mPdfViewCtrl;
    private AnnotMenu mAnnotMenu;
    private SignatureMixListPopup mMixListPopup;
    private ArrayList<Integer> mMenuItems;
    private AppDisplay mDisplay;
    private PaintFlagsDrawFilter mPaintFilter;
    private Matrix mMatrix;
    private RectF mFrameRectF;
    private RectF mDesRect;
    private int mPageIndex = -1;
    private PointF mDownPoint;
    private boolean mIsSignEditing;
    private Bitmap mBitmap;
    private Rect mBitmapRect;
    private RectF mBBoxTmp;
    private String mDsgPath;

    private int mColor;


    private float mDiameter;
    private PropertyBar mPropertyBar;

    private boolean mDefaultAdd = false;
    private boolean mIsShowAnnotMenu = true ;

    public SignatureToolHandler(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mParent = parent;
        mPdfViewCtrl = pdfViewCtrl;
        mDisplay = AppDisplay.getInstance(mContext);
        init();
        mPropertyBar = new PropertyBarImpl(mContext, mPdfViewCtrl);
        mMixListPopup = new SignatureMixListPopup(mContext, mParent, mPdfViewCtrl, mInkCallback, false);
    }

    private void init() {
        mLastPoint = new PointF();
        mFrmPaint = new Paint();
        mFrmPaint.setPathEffect(AppAnnotUtil.getAnnotBBoxPathEffect());
        mFrmPaint.setStyle(Style.STROKE);
        mFrmPaint.setAntiAlias(true);

        mPaintFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        mMatrix = new Matrix();
        mFrameRectF = new RectF();
        mDesRect = new RectF();
        mBbox = new RectF();
        mBBoxTmp = new RectF();
        mBitmapRect = new Rect();

        mCtlPtPaint = new Paint();
        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setFilterBitmap(true);

        mAnnotMenu = new AnnotMenuImpl(mContext, mPdfViewCtrl);
        mMenuItems = new ArrayList<Integer>();
        mMenuItems.add(0, AnnotMenu.AM_BT_SIGNATURE);
        mMenuItems.add(1, AnnotMenu.AM_BT_DELETE);

        mCtlPtRadius = dp2px(5);

    }

    @Override
    public String getType() {
        return ToolHandler.TH_TYPE_SIGNATURE;
    }

    @Override
    public void onActivate() {
        mIsSignEditing = false;
        mIsSigning = false;
        mPageIndex = -1;
        showSignDialog(false);
    }

    @Override
    public void onDeactivate() {
        mIsSignEditing = false;
        mIsSigning = false;
        if (mFragment != null && mFragment.isAttached()) {
            if (mPdfViewCtrl.getUIExtensionsManager() != null) {
                Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                if (context != null) {
                    try {
                        FragmentManager manager = ((FragmentActivity) context).getSupportFragmentManager();
                        manager.popBackStack();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        mAnnotMenu.dismiss();
        mBbox.setEmpty();
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        mBitmap = null;
        mDsgPath = null;
    }

    public void addSignature(int pageIndex, PointF downPoint, boolean isFromTs) {
        if (AppUtil.isFastDoubleClick()) return;
        mPageIndex = pageIndex;
        mDownPoint = new PointF(downPoint.x, downPoint.y);

        mPdfViewCtrl.convertPageViewPtToPdfPt(mDownPoint, mDownPoint, mPageIndex);
        showSignDialog(isFromTs);
    }


    private RectF mTouchTmpRectF = new RectF();

    @Override
    public boolean onTouchEvent(final int pageIndex, MotionEvent motionEvent) {

        if (mIsSigning) return true;
        int action = motionEvent.getActionMasked();
        PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
        PointF point = new PointF();
        mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, point, pageIndex);
        float evX = point.x;
        float evY = point.y;
        switch (action) {

            case MotionEvent.ACTION_DOWN:
                /*if (!mIsSignEditing) {

                } else */if (mPageIndex == pageIndex) {
                    mPdfViewCtrl.convertPageViewPtToDisplayViewPt(point, point, pageIndex);
                    mBBoxTmp.set(mBbox);

                    mPdfViewCtrl.convertPdfRectToPageViewRect(mBBoxTmp, mBBoxTmp, mPageIndex);
                    mCurrentCtr = isTouchControlPoint(mBBoxTmp, evX, evY, 5);

                    if (mCurrentCtr != CTR_NONE) {

                        mLastOper = OPER_SCALE;
                        mTouchCaptured = true;
                        mLastPoint.set(evX, evY);
                        mAnnotMenu.dismiss();
                        return true;
                    } else if (mBBoxTmp.contains(evX, evY)) {

                        mLastOper = OPER_TRANSLATE;
                        mTouchCaptured = true;
                        mLastPoint.set(evX, evY);
                        mAnnotMenu.dismiss();
                        return true;
                    }
                }

                return false;
            case MotionEvent.ACTION_MOVE:
                if (mTouchCaptured) {
                    if (evX != mLastPoint.x && evY != mLastPoint.y) {
                        mBBoxTmp.set(mBbox);

                        mPdfViewCtrl.convertPdfRectToPageViewRect(mBBoxTmp, mBBoxTmp, mPageIndex);
                        switch (mLastOper) {
                            case OPER_TRANSLATE: {
                                float dx = evX - mLastPoint.x;
                                float dy = evY - mLastPoint.y;
                                mBBoxTmp.offset(dx, dy);

                                float thickness = 5;
                                float adjustx = 0;
                                float adjusty = 0;
                                float deltaXY = thickness / 2 + mCtlPtRadius + 3;

                                if (mBBoxTmp.left < deltaXY) {
                                    adjustx = -mBBoxTmp.left + deltaXY;
                                }
                                if (mBBoxTmp.top < deltaXY) {
                                    adjusty = -mBBoxTmp.top + deltaXY;
                                }
                                if (mBBoxTmp.right > mPdfViewCtrl.getPageViewWidth(pageIndex) - deltaXY) {
                                    adjustx = mPdfViewCtrl.getPageViewWidth(pageIndex) - mBBoxTmp.right - deltaXY;
                                }
                                if (mBBoxTmp.bottom > mPdfViewCtrl.getPageViewHeight(pageIndex) - deltaXY) {
                                    adjusty = mPdfViewCtrl.getPageViewHeight(pageIndex) - mBBoxTmp.bottom - deltaXY;
                                }

                                mBBoxTmp.offset(adjustx, adjusty);
                                mBbox.set(mBBoxTmp);

                                mBBoxTmp.offset(-(adjustx + dx), -(adjusty + dy));
                                mBBoxTmp.union(mBbox);
                                mBBoxTmp.inset(-deltaXY, -deltaXY);

                                mPdfViewCtrl.convertPageViewRectToPdfRect(mBbox, mBbox, mPageIndex);
                                mPdfViewCtrl.invalidate();
                                mLastPoint.offset(dx + adjustx, dy + adjusty);
                                break;
                            }
                            case OPER_SCALE: {
                                float dx = evX - mLastPoint.x;
                                float dy = evY - mLastPoint.y;
                                if (mBBoxTmp.width() - Math.abs(dx) < 30) {
                                    boolean isBreak = false;
                                    switch (mCurrentCtr) {
                                        case CTR_LEFT_BOTTOM:
                                        case CTR_LEFT_MID:
                                        case CTR_LEFT_TOP:
                                            if (dx > 0) isBreak = true;
                                            break;
                                        case CTR_RIGHT_TOP:
                                        case CTR_RIGHT_MID:
                                        case CTR_RIGHT_BOTTOM:
                                            if (dx < 0) isBreak = true;
                                            break;
                                        case CTR_MID_TOP:
                                            break;
                                        case CTR_MID_BOTTOM:
                                            break;
                                        default:
                                            break;
                                    }
                                    if (isBreak) {
                                        dx = 0;
                                    }
                                }
                                if (mBBoxTmp.height() - Math.abs(dy) < 30) {
                                    boolean isBreak = false;
                                    switch (mCurrentCtr) {
                                        case CTR_LEFT_TOP:
                                        case CTR_MID_TOP:
                                        case CTR_RIGHT_TOP:
                                            if (dy > 0) isBreak = true;
                                            break;
                                        case CTR_RIGHT_BOTTOM:
                                        case CTR_MID_BOTTOM:
                                        case CTR_LEFT_BOTTOM:
                                            if (dy < 0) isBreak = true;
                                            break;
                                        case CTR_LEFT_MID:
                                            break;
                                        case CTR_RIGHT_MID:
                                            break;
                                        default:
                                            break;
                                    }
                                    if (isBreak) {
                                        dy = 0;
                                    }
                                }
                                mTouchTmpRectF.set(mBBoxTmp);
                                calculateScaleMatrix(mMatrix, mCurrentCtr, mBBoxTmp, dx, dy);
                                mMatrix.mapRect(mBBoxTmp);
                                float thickness = 5;
                                float deltaXY = thickness / 2 + mCtlPtRadius + 3;
                                PointF adjustXY = adjustScalePointF(pageIndex, mBBoxTmp, deltaXY);
                                mLastPoint.offset(dx + adjustXY.x, dy + adjustXY.y);
                                mBbox.set(mBBoxTmp);

                                mPdfViewCtrl.convertPageViewRectToPdfRect(mBbox, mBbox, mPageIndex);
                                mTouchTmpRectF.union(mBBoxTmp);
                                mTouchTmpRectF.inset(-deltaXY, -deltaXY);

                                mPdfViewCtrl.invalidate();
                                break;
                            }
                            default:
                                return false;
                        }
                    }
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mTouchCaptured) {
                    mBBoxTmp.set(mBbox);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(mBBoxTmp, mBBoxTmp, mPageIndex);
                    mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mBBoxTmp, mBBoxTmp, mPageIndex);
                    mAnnotMenu.show(mBBoxTmp);
                }
                mTouchCaptured = false;
                mLastPoint.set(0, 0);
                mLastOper = OPER_DEFAULT;
                mCurrentCtr = CTR_NONE;
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onLongPress(int pageIndex, MotionEvent motionEvent) {
        if (!mIsSignEditing) {
            PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
            PointF point = new PointF();
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, point, pageIndex);
            addSignature(pageIndex, point, false);
            return true;
        }
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(int pageIndex, MotionEvent motionEvent) {

        if (!mIsSignEditing) {
            PointF devPt = new PointF(motionEvent.getX(), motionEvent.getY());
            PointF point = new PointF();
            mPdfViewCtrl.convertDisplayViewPtToPageViewPt(devPt, point, pageIndex);
            addSignature(pageIndex, point, false);

            return true;
        }
        return false;
    }

    @Override
    public boolean isContinueAddAnnot() {
        return true;
    }

    @Override
    public void setContinueAddAnnot(boolean continueAddAnnot) {
    }


    @Override
    public void onDraw(int pageIndex, Canvas canvas) {
        if (mPageIndex != pageIndex) return;
        if (mBitmap == null) return;
        mDesRect.set(mBbox);
        mPdfViewCtrl.convertPdfRectToPageViewRect(mDesRect, mDesRect, mPageIndex);

        if (mDesRect.isEmpty()) return;
        canvas.setDrawFilter(mPaintFilter);
        mFrameRectF.set(mDesRect);
        float thickness = 5;
        mFrameRectF.inset(-thickness * 0.5f, -thickness * 0.5f);
        canvas.save();
        drawFrame(canvas, mFrameRectF);
        mBitmapPaint.setColor(0xFF000000);
        canvas.drawBitmap(mBitmap, mBitmapRect, mDesRect, mBitmapPaint);
        int color = 0xFF3D97FC;
        drawFrame(canvas, mFrameRectF, color, 5);
        drawControlPoints(canvas, mFrameRectF, color, 5);
        canvas.restore();
    }

    private void drawFrame(Canvas canvas, RectF rectF, int color, float thickness) {
        mFrmPaint.setColor(color);
        mFrmPaint.setStrokeWidth(mFrmLineWidth);
        canvas.drawRect(rectF, mFrmPaint);
    }

    private void drawFrame(Canvas canvas, RectF rectF) {
        mBitmapPaint.setColor(0x523BC1FD);

        canvas.drawRect(rectF, mBitmapPaint);
    }

    private void drawControlPoints(Canvas canvas, RectF rectF, int color, float thickness) {
        float[] ctlPts = calculateControlPoints(rectF);
        mCtlPtPaint.setStrokeWidth(mCtlPtLineWidth);
        for (int i = 0; i < ctlPts.length; i += 2) {
            mCtlPtPaint.setColor(Color.WHITE);
            mCtlPtPaint.setStyle(Style.FILL);
            canvas.drawCircle(ctlPts[i], ctlPts[i + 1], mCtlPtRadius, mCtlPtPaint);
            mCtlPtPaint.setColor(color);
            mCtlPtPaint.setStyle(Style.STROKE);
            canvas.drawCircle(ctlPts[i], ctlPts[i + 1], mCtlPtRadius, mCtlPtPaint);
        }
    }


    public void onDrawForControls(Canvas canvas) {
        if (mIsSignEditing) {
            mBBoxTmp.set(mBbox);
            if (mPdfViewCtrl.isPageVisible(mPageIndex)) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(mBBoxTmp, mBBoxTmp, mPageIndex);
                mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mBBoxTmp, mBBoxTmp, mPageIndex);
                mAnnotMenu.update(mBBoxTmp);
            }
        }
    }


    private SignatureFragment mFragment;

    private void showSignDialog(boolean isFromTs) {
        HashMap<String, Object> map = SignatureDataUtil.getRecentData(mContext);
        if (map != null && map.get("rect") != null && map.get("bitmap") != null) {
            Object dsgPathObj = map.get("dsgPath");
            if (dsgPathObj != null && !AppUtil.isEmpty((String) dsgPathObj)) {
                if (true) {
                    mInkCallback.onSuccess(false, (Bitmap) map.get("bitmap"), (Rect) map.get("rect"), (Integer) map.get("color"), (String) dsgPathObj);
                } else {
                    applyRecentNormalSignData(isFromTs);
                }
            } else {
                mInkCallback.onSuccess(false, (Bitmap) map.get("bitmap"), (Rect) map.get("rect"), (Integer) map.get("color"), null);
            }
            return;
        }

        if (isFromTs) {
            return;
        }
        showDrawViewFragment();
    }

    private void showDrawViewFragment() {
        if (mPdfViewCtrl.getUIExtensionsManager() == null) {
            return;
        }
        Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) {
            return;
        }
        FragmentActivity act = ((FragmentActivity) context);
        mFragment = (SignatureFragment) act.getSupportFragmentManager().findFragmentByTag("InkSignFragment");
        if (mFragment == null) {
            mFragment = new SignatureFragment();
            mFragment.init(mContext, mParent, mPdfViewCtrl, false);
        }
        mFragment.setInkCallback(mInkCallback);
        AppDialogManager.getInstance().showAllowManager(mFragment, act.getSupportFragmentManager(), "InkSignFragment", null);
    }

    private void applyRecentNormalSignData(boolean isFromTs) {
        HashMap<String, Object> map = SignatureDataUtil.getRecentNormalSignData(mContext);
        if (map != null && map.get("rect") != null && map.get("bitmap") != null) {
            mInkCallback.onSuccess(false, (Bitmap) map.get("bitmap"), (Rect) map.get("rect"), (Integer) map.get("color"), null);
        } else {
            if (isFromTs) {
                return;
            }
            showDrawViewFragment();
        }
    }

    private PointF adjustScalePointF(int pageIndex, RectF rectF, float dxy) {
        float adjustx = 0;
        float adjusty = 0;
        int pageHeight = mPdfViewCtrl.getPageViewHeight(pageIndex);
        int pageWidth = mPdfViewCtrl.getPageViewWidth(pageIndex);
        switch (mCurrentCtr) {
            case CTR_LEFT_TOP:
                if (rectF.left < dxy) {
                    adjustx = -rectF.left + dxy;
                    rectF.left = dxy;
                }
                if (rectF.top < dxy) {
                    adjusty = -rectF.top + dxy;
                    rectF.top = dxy;
                }
                break;
            case CTR_MID_TOP:
                if (rectF.top < dxy) {
                    adjusty = -rectF.top + dxy;
                    rectF.top = dxy;
                }
                break;
            case CTR_RIGHT_TOP:
                if (rectF.top < dxy) {
                    adjusty = -rectF.top + dxy;
                    rectF.top = dxy;
                }
                if (rectF.right > pageWidth - dxy) {
                    adjustx = pageWidth - rectF.right - dxy;
                    rectF.right = pageWidth - dxy;
                }
                break;
            case CTR_RIGHT_MID:
                if (rectF.right > pageWidth - dxy) {
                    adjustx = pageWidth - rectF.right - dxy;
                    rectF.right = pageWidth - dxy;
                }
                break;
            case CTR_RIGHT_BOTTOM:
                if (rectF.right > pageWidth - dxy) {
                    adjustx = pageWidth - rectF.right - dxy;
                    rectF.right = pageWidth - dxy;
                }
                if (rectF.bottom > pageHeight - dxy) {
                    adjusty = pageHeight - rectF.bottom - dxy;
                    rectF.bottom = pageHeight - dxy;
                }
                break;
            case CTR_MID_BOTTOM:
                if (rectF.bottom > pageHeight - dxy) {
                    adjusty = pageHeight - rectF.bottom - dxy;
                    rectF.bottom = pageHeight - dxy;
                }
                break;
            case CTR_LEFT_BOTTOM:
                if (rectF.left < dxy) {
                    adjustx = -rectF.left + dxy;
                    rectF.left = dxy;
                }
                if (rectF.bottom > pageHeight - dxy) {
                    adjusty = pageHeight - rectF.bottom - dxy;
                    rectF.bottom = pageHeight - dxy;
                }
                break;
            case CTR_LEFT_MID:
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

    private RectF mFrameTmpRect = new RectF();
    private RectF mAreaTmpRect = new RectF();

    private int isTouchControlPoint(RectF rectF, float x, float y, float thickness) {
        mFrameTmpRect.set(rectF);
        mFrameTmpRect.inset(-thickness * 0.5f, -thickness * 0.5f);
        float[] ctlPts = calculateControlPoints(mFrameTmpRect);
        for (int i = 0; i < ctlPts.length / 2; i++) {
            mAreaTmpRect.set(ctlPts[i * 2], ctlPts[i * 2 + 1], ctlPts[i * 2], ctlPts[i * 2 + 1]);
            mAreaTmpRect.inset(-mCtlPtTouchExt, -mCtlPtTouchExt);
            if (mAreaTmpRect.contains(x, y)) {
                return i;
            }
        }
        return CTR_NONE;
    }

    private float[] calculateControlPoints(RectF rectF) {
        float l = rectF.left;
        float t = rectF.top;
        float r = rectF.right;
        float b = rectF.bottom;
        float[] ctlPts = {
                l, t,
                (l + r) / 2, t,
                r, t,
                r, (t + b) / 2,
                r, b,
                (l + r) / 2, b,
                l, b,
                l, (t + b) / 2,
        };
        return ctlPts;
    }

    private void calculateScaleMatrix(Matrix matrix, int ctl, RectF rectF, float dx, float dy) {
        matrix.reset();
        float[] ctlPts = calculateControlPoints(rectF);
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
        float scaleH = (px + dx - oppositeX) / (px - oppositeX);
        float scaleV = (py + dy - oppositeY) / (py - oppositeY);

        switch (ctl) {
            case CTR_LEFT_TOP:
            case CTR_RIGHT_TOP:
            case CTR_RIGHT_BOTTOM:
            case CTR_LEFT_BOTTOM:
                matrix.postScale(scaleH, scaleV, oppositeX, oppositeY);
                break;
            case CTR_RIGHT_MID:
            case CTR_LEFT_MID:
                matrix.postScale(scaleH, 1.0f, oppositeX, oppositeY);
                break;
            case CTR_MID_TOP:
            case CTR_MID_BOTTOM:
                matrix.postScale(1.0f, scaleV, oppositeX, oppositeY);
                break;
            case CTR_NONE:
            default:
                break;
        }
    }

    private int dp2px(int dp) {
        return mDisplay.dp2px(dp);
    }

    private UITextEditDialog mWillSignDialog;
    private boolean mIsSigning;


    public boolean onKeyBack() {
        mInkCallback.onBackPressed();
        return true;
    }

    public void reset() {
        mIsSignEditing = false;
        mIsSigning = false;
        if (mFragment != null && mFragment.isAttached()) {
            mInkCallback.onBackPressed();
        }
        if (mWillSignDialog != null && mWillSignDialog.getDialog().isShowing()) {
            try {
                mWillSignDialog.dismiss();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        mWillSignDialog = null;
    }

    private SignatureFragment.SignatureInkCallback mInkCallback = new SignatureFragment.SignatureInkCallback() {

        @Override
        public void onBackPressed() {
            if (mPdfViewCtrl.getUIExtensionsManager() == null) {
                return;
            }
            Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
            if (context == null) {
                return;
            }

            FragmentActivity act = ((FragmentActivity) context);
            SignatureFragment fragment = (SignatureFragment) act.getSupportFragmentManager().findFragmentByTag("InkSignFragment");

            if (fragment == null) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
            } else {
                fragment.dismiss();

                List<String> list = SignatureDataUtil.getRecentKeys(mContext);
                if (list != null && list.size() > 0) {
                    if (mBitmap != null && mBBoxTmp != null) {
                        mAnnotMenu.show(mBBoxTmp);
                    }
                } else {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                }
            }

        }

        @Override
        public void onSuccess(boolean isFromFragment, final Bitmap bitmap, final Rect rect, final int color, final String dsgPath) {
            if (isFromFragment) {
                if (mPdfViewCtrl.getUIExtensionsManager() == null) {
                    return;
                }
                Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
                if (context == null) {
                    return;
                }
                try {
                    FragmentActivity act = ((FragmentActivity) context);
                    SignatureFragment fragment = (SignatureFragment) act.getSupportFragmentManager().findFragmentByTag("InkSignFragment");
                    fragment.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showSignOnPage(bitmap, rect, color, dsgPath);
                    }
                }, 300);
            } else {
                showSignOnPage(bitmap, rect, color, dsgPath);
            }
        }

        private void showSignOnPage(Bitmap bitmap, Rect rect, int color, String dsgPath) {
            if (bitmap == null || rect.isEmpty()) {
                mIsSignEditing = false;
                return;
            } else if (bitmap.getWidth() < rect.width() || bitmap.getHeight() < rect.height()) {
                mIsSignEditing = false;
                return;
            }
            float scale = (float) rect.height() / rect.width();

            if (mPageIndex < 0) {
                return;
            }

            int vw = mPdfViewCtrl.getPageViewWidth(mPageIndex);
            int vh = mPdfViewCtrl.getPageViewHeight(mPageIndex);
            int cx, cy, rw, rh;

            if (scale >= 1) {
                rh = dp2px(150);
                rw = (int) (rh / scale);
                rw = rw > vw / 2 ? vw / 2 : rw;

                mPdfViewCtrl.convertPdfPtToPageViewPt(mDownPoint, mDownPoint, mPageIndex);
            } else {
                rw = dp2px(150);
                rh = (int) (rw * scale);
                if (scale > 1.0) {
                    rh = rh > vh / 2 ? vh / 2 : rh;
                }

                mPdfViewCtrl.convertPdfPtToPageViewPt(mDownPoint, mDownPoint, mPageIndex);
            }
            if (mBitmap != null) {
                mPdfViewCtrl.convertPdfRectToPageViewRect(mBbox, mBbox, mPageIndex);
                cx = (int) mBbox.centerX() - (rw / 2);
                cy = (int) mBbox.centerY() - (rh / 2);
                mBitmap.recycle();
                mBitmap = null;
            } else {
                cx = (int) mDownPoint.x - (rw / 2);
                cy = (int) mDownPoint.y - (rh / 2);
            }

            cx = cx > 0 ? cx : 0;
            cy = cy > 0 ? cy : 0;
            int offsetX;
            int offsetY;
            if ((cx + rw) > vw)
                offsetX = vw - rw - 5;
            else
                offsetX = cx;
            if ((cy + rh) > vh)
                offsetY = vh - rh - 5;
            else
                offsetY = cy;
            mBbox.set(offsetX, offsetY, rw + offsetX, rh + offsetY);
            mBitmapRect.set(0, 0, rect.width(), rect.height());
            int t = rect.top;
            int b = rect.bottom;
            int l = rect.left;
            int r = rect.right;
            int[] pixels = new int[rect.width() * rect.height()];
            bitmap.getPixels(pixels, 0, r - l, l, t, r - l, b - t);
            for (int i = 0; i < pixels.length; i++) {
                if (0xFFFFFFFF == pixels[i]) {
                    pixels[i] = 0x0;
                }
            }
            mBitmap = Bitmap.createBitmap(pixels, rect.width(), rect.height(), Config.ARGB_8888);
            bitmap.recycle();
            bitmap = null;
            mDsgPath = dsgPath;
            mIsSignEditing = true;
            mAnnotMenu.setMenuItems(mMenuItems);
            mAnnotMenu.setListener(mMenuListener);
            mAnnotMenu.setShowAlways(true);

            mPdfViewCtrl.convertPageViewRectToPdfRect(mBbox, mBbox, mPageIndex);
            mBBoxTmp.set(mBbox);
            mPdfViewCtrl.convertPdfRectToPageViewRect(mBBoxTmp, mBBoxTmp, mPageIndex);

            mFrameRectF.set(mBBoxTmp);
            mFrameRectF.inset(-5 * 0.5f, -5 * 0.5f);
            mPdfViewCtrl.invalidate();

            mPdfViewCtrl.convertPageViewRectToDisplayViewRect(mBBoxTmp, mBBoxTmp, mPageIndex);
            mAnnotMenu.show(mBBoxTmp);
        }
    };


    private AnnotMenu.ClickListener mMenuListener = new AnnotMenu.ClickListener() {

        @Override
        public void onAMClick(int id) {
            if (AppUtil.isFastDoubleClick()) return;
            mIsSigning = false;
            if (id == AnnotMenu.AM_BT_SIGNATURE) {
                doSign();
            } else if (AnnotMenu.AM_BT_CANCEL == id) {
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
            } else if (AnnotMenu.AM_BT_DELETE == id) {
                int pageIndex = mPageIndex;
                clearData();
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    mPdfViewCtrl.invalidate();
                }
            }
        }
    };

    private boolean doSign() {
        if (mBitmap == null) return false;
        mAnnotMenu.dismiss();
        if (mDefaultAdd || AppUtil.isEmpty(mDsgPath)) { // common sign
            sign2Doc();
            return true;
        }
        if (mWillSignDialog == null || mWillSignDialog.getDialog().getOwnerActivity() == null) {
            if (mPdfViewCtrl.getUIExtensionsManager() == null)  return false;
            Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
            if (context == null) return false;
            mWillSignDialog = new UITextEditDialog(context);
            mWillSignDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (AppUtil.isFastDoubleClick()) return;
                    mWillSignDialog.dismiss();
                    int pageIndex = mPageIndex;
                    clearData();
                    if (mPdfViewCtrl.isPageVisible(pageIndex))
                        mPdfViewCtrl.invalidate();

                }
            });
            mWillSignDialog.setOnCancelListener(new OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    int pageIndex = mPageIndex;
                    clearData();
                    if (mPdfViewCtrl.isPageVisible(pageIndex))
                        mPdfViewCtrl.invalidate();

                }
            });
            mWillSignDialog.getPromptTextView().setText(AppResource.getString(mContext.getApplicationContext(), R.string.rv_sign_dialog_description));
            mWillSignDialog.setTitle(AppResource.getString(mContext.getApplicationContext(), R.string.rv_sign_dialog_title));
            mWillSignDialog.getInputEditText().setVisibility(View.GONE);
        }
        mWillSignDialog.getOKButton().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) return;
                mWillSignDialog.dismiss();
                sign2Doc();
                mDefaultAdd = true;
            }
        });
        mWillSignDialog.show();
        return true;
    }

    private UISaveAsDialog mSaveAsDialog;

    private void sign2Doc() {
        Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        if (context == null) return;
        if (!AppUtil.isEmpty(mDsgPath)) {// do digital signature sign
            signDigitalSignature(context);
        } else {
            signCommonSignature(context);
        }
    }

    private void signDigitalSignature(@NonNull Context context) {
        final Module dsgModule = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_DIGITALSIGNATURE);
        if (dsgModule != null) {
            if (mPdfViewCtrl.getUIExtensionsManager() == null) {
                return;
            }
            mSaveAsDialog = new UISaveAsDialog(context, "sign.pdf", "pdf", new UISaveAsDialog.ISaveAsOnOKClickCallBack() {
                @Override
                public void onOkClick(final String newFilePath) {
                    String tmpPath = newFilePath;
                    final File file = new File(newFilePath);
                    if (file.exists()) {
                        tmpPath = newFilePath + "_tmp.pdf";
                    }
                    DigitalSignatureUtil dsgUtil = ((DigitalSignatureModule) dsgModule).getDigitalSignatureUtil();
                    final String finalTmpPath = tmpPath;
                    dsgUtil.addCertSignature(tmpPath, mDsgPath, mBitmap, mBbox, mPageIndex, new IDigitalSignatureCreateCallBack() {
                        @Override
                        public void onCreateFinish(boolean success) {
                            if (!success){
                                File file = new File(finalTmpPath);
                                file.delete();

                                RectF rect = new RectF(mBbox);

                                mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, mPageIndex);
                                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(rect));
                                clearData();
                                return;
                            }

                            File newFile = new File(newFilePath);
                            File file = new File(finalTmpPath);
                            file.renameTo(newFile);

                            if (!mPdfViewCtrl.isPageVisible(mPageIndex)) {
                                clearData();
                                return;
                            }

                            RectF rect = new RectF(mBbox);

                            mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, mPageIndex);
                            mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(rect));
                            clearData();
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().clearUndoRedo();
                            ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(false);
                            mPdfViewCtrl.openDoc(newFilePath, null);
                            if (((DigitalSignatureModule) dsgModule).getDocPathChangeListener() != null)
                                ((DigitalSignatureModule) dsgModule).getDocPathChangeListener().onDocPathChange(newFilePath);
                            ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                            updateThumbnail(newFilePath);
                        }
                    });
                }

                @Override
                public void onCancelClick() {
                    mWillSignDialog.dismiss();
                    int pageIndex = mPageIndex;
                    clearData();
                    if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                        mPdfViewCtrl.invalidate();
                    }
                }
            });
            mSaveAsDialog.showDialog();
        }
    }

    private void signCommonSignature(@NonNull Context context) {
        showProgressDialog();
        final PDFPage page = getPage(mPdfViewCtrl.getDoc(), mPageIndex);
        int viewRotation = mPdfViewCtrl.getViewRotation();
        SignatureSignEvent event = new SignatureSignEvent(page, mBitmap, mBbox, SignatureConstants.SG_EVENT_SIGN, viewRotation, null);
        SignaturePSITask task = new SignaturePSITask(event, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                dismissProgressDialog();
                if (!success || !mPdfViewCtrl.isPageVisible(mPageIndex)) {
                    clearData();
                    return;
                }

                parsePage(page, true);// reparse page,
                RectF rect = new RectF(mBbox);
                mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, mPageIndex);
                mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(rect));
                clearData();
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).changeState(ReadStateConfig.STATE_NORMAL);
                ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().setDocModified(true);
            }
        });

        mPdfViewCtrl.addTask(task);
    }

    private void signCommonSignatureToNewFile(@NonNull Context context) {
        mSaveAsDialog = new UISaveAsDialog(context, "sign.pdf", "pdf", new UISaveAsDialog.ISaveAsOnOKClickCallBack() {
            @Override
            public void onOkClick(final String newFilePath) {
                PDFPage page = getPage(mPdfViewCtrl.getDoc(), mPageIndex);
                int viewRotation = mPdfViewCtrl.getViewRotation();
                SignatureSignEvent event = new SignatureSignEvent(page, mBitmap, mBbox, SignatureConstants.SG_EVENT_SIGN, viewRotation, null);
                SignaturePSITask task = new SignaturePSITask(event, new Event.Callback() {
                    @Override
                    public void result(Event event, boolean success) {
                        if (!success || !mPdfViewCtrl.isPageVisible(mPageIndex)) {
                            clearData();
                            return;
                        }

                        String tmpPath = newFilePath;
                        final File file = new File(newFilePath);
                        if (file.exists()) {
                            tmpPath = newFilePath + "_tmp.pdf";
                        }

                        boolean success2 = false;
                        try {
                            if (mPdfViewCtrl.getDoc().isCDRM()) {
                                mPdfViewCtrl.getConnectedPDF().saveAs(tmpPath);
                                success2 = true;
                            } else {
                                Progressive progressive = mPdfViewCtrl.getDoc().startSaveAs(tmpPath, PDFDoc.e_SaveFlagNormal, null);
                                int state = Progressive.e_ToBeContinued;
                                while (state == Progressive.e_ToBeContinued) {
                                    state = progressive.resume();
                                }

                                success2 = (state == Progressive.e_Finished);
                            }
                        } catch (PDFException e) {
                            e.printStackTrace();
                        }
                        final String finalTmpPath = tmpPath;

                        if (!success2){
                            File tmpFile = new File(finalTmpPath);
                            tmpFile.delete();
                            return;
                        }

                        File newFile = new File(newFilePath);
                        File tmpFile = new File(finalTmpPath);
                        tmpFile.renameTo(newFile);

                        RectF rect = new RectF(mBbox);

                        mPdfViewCtrl.convertPdfRectToPageViewRect(rect, rect, mPageIndex);
                        mPdfViewCtrl.refresh(mPageIndex, AppDmUtil.rectFToRect(rect));
                        clearData();
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getDocumentManager().clearUndoRedo();
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).openDocument(newFilePath, null);
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                        updateThumbnail(newFilePath);
                    }
                });

                mPdfViewCtrl.addTask(task);
            }

            @Override
            public void onCancelClick() {
                mWillSignDialog.dismiss();
                int pageIndex = mPageIndex;
                clearData();
                if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                    mPdfViewCtrl.invalidate();
                }
            }
        });
        mSaveAsDialog.showDialog();
    }

    private FxProgressDialog mProgressDialog;
    private void showProgressDialog() {
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager();
        if (uiExtensionsManager.getAttachedActivity() == null) {
            UIToast.getInstance(mContext).show(AppResource.getString(mContext.getApplicationContext(), R.string.rv_unknown_error));
            return;
        }
        mProgressDialog = new FxProgressDialog(uiExtensionsManager.getAttachedActivity(),
                AppResource.getString(mContext.getApplicationContext(), R.string.fx_string_processing));

        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private void updateThumbnail(String path){
        LocalModule module = (LocalModule) ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager())
                .getModuleByName(Module.MODULE_NAME_LOCAL);
        if (module != null) {
            module.updateThumbnail(path);
        }
    }


    private void clearData() {
        mIsSignEditing = false;
        mIsSigning = false;
        if (mFragment != null && mFragment.isAttached()) {
            Context context = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
            if (context != null) {
                try {
                    FragmentManager manager = ((FragmentActivity) context).getSupportFragmentManager();
                    manager.popBackStack();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        mAnnotMenu.dismiss();
        mBbox.setEmpty();
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        mBitmap = null;
        mDsgPath = null;
        mPageIndex = -1;
    }

    public void showSignList(RectF rectF) {
        mAnnotMenu.dismiss();
        if (mDisplay.isPad()) {
            showMixListPopupPad(rectF);
        } else {
            showMixListPopupPhone();
        }
    }

    private void showMixListPopupPhone() {
        if (mMixListPopup == null) {
            mMixListPopup = new SignatureMixListPopup(mContext, mParent, mPdfViewCtrl, mInkCallback, false);
        }
        mMixListPopup.setSignatureListEvent(new SignatureMixListPopup.ISignatureListEvent() {
            @Override
            public void onSignatureListDismiss() {
                if (mBitmap != null){
                    mAnnotMenu.show(mBBoxTmp);
                } else {
                    if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() == null) {
                        int pageIndex = mPageIndex;
                        clearData();
                        if (mPdfViewCtrl.isPageVisible(pageIndex)) {
                            mPdfViewCtrl.invalidate();
                        }
                    }
                }
            }
        });
        mMixListPopup.show();
    }

    protected void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
        if (newWidth != oldWidth || newHeight != oldHeight) {
            if (!AppDisplay.getInstance(mContext).isPad()) {
                if (mMixListPopup.getPopWindow() != null && mMixListPopup.getPopWindow().isShowing()) {
                    mMixListPopup.update(newWidth, newHeight);
                }
            }

            if (mSaveAsDialog != null && mSaveAsDialog.isShowing()) {
                mSaveAsDialog.setHeight(mSaveAsDialog.getDialogHeight());
                mSaveAsDialog.showDialog();
            }
        }
    }

    private void showMixListPopupPad(RectF rectF) {
        final SignatureListPicker listPicker = new SignatureListPicker(mContext, mParent, mPdfViewCtrl, mInkCallback, false);
        listPicker.init(new SignatureListPicker.ISignListPickerDismissCallback() {
            @Override
            public void onDismiss(boolean isShowAnnotMenu) {
                mIsShowAnnotMenu = isShowAnnotMenu;
                mPropertyBar.dismiss();
            }
        });
        listPicker.loadData();
        mPropertyBar.setArrowVisible(true);
        mPropertyBar.reset(0);
        mPropertyBar.addContentView(listPicker.getRootView());
        listPicker.getRootView().getLayoutParams().height = mDisplay.dp2px(460);
        mPropertyBar.setDismissListener(new PropertyBar.DismissListener() {
            @Override
            public void onDismiss() {
                if (listPicker.getBaseItemsSize() == 0) {
                    ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(null);
                }
                listPicker.dismiss();
                if (mBitmap != null) {
                    if (mIsShowAnnotMenu){
                        mAnnotMenu.show(mBBoxTmp);
                    } else {
                        mAnnotMenu.dismiss();
                        mIsShowAnnotMenu = true;
                    }
                }
            }
        });
        mPropertyBar.show(rectF, true);
    }

    public PropertyBar getPropertyBar(){
        return mPropertyBar;
    }

    public void setColor(int color) {
        mColor = color;
    }

    public int getColor() {
        return mColor;
    }

    public float getDiameter() {
        return mDiameter;
    }

    public void setDiameter(float diameter) {
        this.mDiameter = diameter;
    }

    private PDFPage getPage(@NonNull PDFDoc pdfDoc, int pageIndex) {
        PDFPage page = null;
        try {
            page = pdfDoc.getPage(pageIndex);
            if (page.isEmpty()) return null;
            if (!page.isParsed() || parsePage(page, false)) return page;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean parsePage(@NonNull PDFPage page, boolean reParse) {
        try {
            Progressive progressive = page.startParse(PDFPage.e_ParsePageNormal, null, reParse);
            int state = Progressive.e_ToBeContinued;
            while (state == Progressive.e_ToBeContinued) {
                state = progressive.resume();
            }

            if (state != Progressive.e_Finished) return false;
            return true;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return false;
    }
}
